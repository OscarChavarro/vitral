package render;

import java.util.ArrayList;
import java.util.List;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.geometry.volume.Arrow;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;

import model.Scene;
import model.ApplicationModel;

/**
Builds the geometry presenting the visual debug ray of the application model:
the ray, its normal at the hit point and its successive reflections. It does
not draw: renderers of each technology draw the `RenderPrimitive`s it builds
(see `buildPrimitives`).
*/
public class VisualRayDebugGeometry
{
    private final ApplicationModel model;
    private final Scene scene;
    private final RendererConfiguration qualitySelectionVisualDebug;
    private final SimpleMaterial visualDebugMaterial;

    public VisualRayDebugGeometry(ApplicationModel model)
    {
        this.model = model;
        this.scene = model.getScene();

        qualitySelectionVisualDebug = new RendererConfiguration();
        qualitySelectionVisualDebug.setShadingType(
            RendererConfiguration.SHADING_TYPE_GOURAUD);
        visualDebugMaterial = scene.defaultMaterial();
    }

    /**
    @return the rendering configuration for the primitives
    */
    public RendererConfiguration getRendererConfiguration()
    {
        return qualitySelectionVisualDebug;
    }

    /**
    @return the primitives presenting the visual debug ray, or an empty list if
    it is not enabled in the application model
    */
    public List<RenderPrimitive> buildPrimitives()
    {
        List<RenderPrimitive> primitives = new ArrayList<>();

        if ( model.isWithVisualDebugRay() ) {
            addRay(primitives, model.getVisualDebugRay(), model.getVisualDebugRayLevels());
        }
        return primitives;
    }

    private void addSegment(List<RenderPrimitive> primitives, Vector3Dd start, Vector3Dd end,
        boolean follow, double w, double tip, SimpleMaterial segmentMaterial)
    {
        double l;
        Vector3Dd diff = end.subtract(start);
        l = diff.length();

        //-----------------------------------------------------------------
        Geometry a;

        if ( l > tip ) {
            a = new Arrow(l - tip, tip, w/2, w);
        }
        else {
            a = new Cone(w/2, w/2, l);
        }
        Matrix4x4d R = new Matrix4x4d();
        double yaw, pitch;
        yaw = diff.obtainSphericalThetaAngle();
        pitch = diff.obtainSphericalPhiAngle();
        R = R.eulerAnglesRotation(Math.toRadians(180)+yaw, pitch, 0);

        primitives.add(new RenderPrimitive(a,
            new Matrix4x4d().translation(start).multiply(R), segmentMaterial));

        //-----------------------------------------------------------------
        if ( follow ) {
            Sphere s = new Sphere(0.025);
            Vector3Dd p;
            double offset = 0.1;
            int i;
            diff = diff.normalized();
            for ( i = 0; i < 3; i++, offset += 0.1 ) {
                p = end.add(diff.multiply(offset));
                primitives.add(new RenderPrimitive(s,
                    new Matrix4x4d().translation(p), segmentMaterial));
            }
        }
    }

    private void addRay(List<RenderPrimitive> primitives, Ray ray, int level)
    {
        if ( level < 0 ) {
            return;
        }

        //-----------------------------------------------------------------
        Vector3Dd p;
        Vector3Dd d = new Vector3Dd(ray.getDirection());
        d = d.normalized();
        RayHit info;

        info = new RayHit();

        //-----------------------------------------------------------------
        SimpleMaterial rayOriginMaterial = visualDebugMaterial.withDiffuse(new ColorRgb(0.9, 0.5, 0.0));
        primitives.add(new RenderPrimitive(new Sphere(0.05),
            new Matrix4x4d().translation(ray.getOrigin()), rayOriginMaterial));

        //-----------------------------------------------------------------
        if ( scene.doIntersectionFirstHit(ray, info) ) {
            d = d.multiply(ray.getT());
            p = ray.getOrigin().add(d);

            addSegment(primitives, ray.getOrigin(), p, false, 0.07, 0.4, rayOriginMaterial);
            if ( level >= 1 ) {
                // Draw normal
                SimpleMaterial normalMaterial = visualDebugMaterial.withDiffuse(new ColorRgb(0.9, 0.9, 0.5));
                addSegment(primitives, p, p.add(info.n.multiply(0.5)), false, 0.05, 0.2, normalMaterial);
            }
            // Reflection ray
            Vector3Dd dd = ray.getDirection().multiply(-1);
            dd = dd.normalized();
            Vector3Dd h = info.n.multiply(dd.dotProduct(info.n)).subtract(dd);
            Ray subray = new Ray(p, dd.add(h.multiply(2)));
            subray = subray.withOrigin(
                subray.getOrigin().add(subray.getDirection().multiply(VSDK.EPSILON*10.0)));
            addRay(primitives, subray, level-1);
        }
        else {
            d = d.multiply(1.4);
            p = ray.getOrigin().add(d);
            addSegment(primitives, ray.getOrigin(), p, true, 0.07, 0.4, rayOriginMaterial);
        }
    }
}
