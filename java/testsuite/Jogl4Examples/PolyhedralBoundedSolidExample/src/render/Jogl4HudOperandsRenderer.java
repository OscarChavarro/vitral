package render;

import com.jogamp.opengl.GL4;

import models.DebuggerModel;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.render.jogl.Jogl4SimpleMaterialRenderer;
import vsdk.toolkit.render.jogl.polyhedralBoundedSolid.Jogl4PolyhedralBoundedSolidRenderer;

public class Jogl4HudOperandsRenderer
{
    private static final double HUD_INSET_DEPTH = 2.8;

    private final DebuggerModel model;
    private final SimpleMaterial csgOperandMaterialA;
    private final SimpleMaterial csgOperandMaterialB;

    public Jogl4HudOperandsRenderer(DebuggerModel model)
    {
        this.model = model;
        this.csgOperandMaterialA = createInsetMaterial(1.0, 0.502, 0.502);
        this.csgOperandMaterialB = createInsetMaterial(0.502, 1.0, 0.502);
    }

    private static SimpleMaterial createInsetMaterial(double r, double g, double b)
    {
        SimpleMaterial m = new SimpleMaterial();
        m = m.withAmbient(new ColorRgb(0.2 * r, 0.2 * g, 0.2 * b));
        m = m.withDiffuse(new ColorRgb(r, g, b));
        m = m.withSpecular(new ColorRgb(1.0, 1.0, 1.0));
        m = m.withDoubleSided(false);
        m = m.withPhongExponent(100);
        return m;
    }

    private static Vector3Dd solidCenter(PolyhedralBoundedSolid solid)
    {
        double[] minMax;

        if ( solid == null ) {
            return new Vector3Dd(0, 0, 0);
        }
        minMax = solid.getMinMax();
        if ( minMax == null || minMax.length < 6 ) {
            return new Vector3Dd(0, 0, 0);
        }
        return new Vector3Dd(
            (minMax[0] + minMax[3]) / 2.0,
            (minMax[1] + minMax[4]) / 2.0,
            (minMax[2] + minMax[5]) / 2.0);
    }

    private static double solidMaxExtent(PolyhedralBoundedSolid solid)
    {
        double[] minMax;
        double ex;
        double ey;
        double ez;

        if ( solid == null ) {
            return 1.0;
        }
        minMax = solid.getMinMax();
        if ( minMax == null || minMax.length < 6 ) {
            return 1.0;
        }
        ex = Math.abs(minMax[0] - minMax[3]);
        ey = Math.abs(minMax[1] - minMax[4]);
        ez = Math.abs(minMax[2] - minMax[5]);
        return Math.max(ex, Math.max(ey, ez));
    }

    private static Vector3Dd cameraRelativeAnchor(Camera camera,
        double ndcX,
        double ndcY,
        double depth)
    {
        Vector3Dd eye = camera.getPosition();
        Vector3Dd front = camera.getFront().normalized();
        Vector3Dd up = camera.getUp().normalized();
        Vector3Dd right = camera.getLeft().multiply(-1).normalized();
        double viewportY = Math.max(camera.getViewportYSize(), 1e-9);
        double aspect = camera.getViewportXSize() / viewportY;
        double offsetX;
        double offsetY;
        double safeDepth = Math.max(depth, 1e-9);

        if ( camera.getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL ) {
            double zoom = Math.max(camera.getOrthogonalZoom(), 1e-9);
            offsetX = ndcX * (aspect / zoom);
            offsetY = ndcY * (1.0 / zoom);
        }
        else {
            double halfHeight = safeDepth *
                Math.tan(Math.toRadians(camera.getFov() / 2.0));
            double halfWidth = halfHeight * aspect;
            offsetX = ndcX * halfWidth;
            offsetY = ndcY * halfHeight;
        }

        return eye.add(front.multiply(safeDepth))
                  .add(right.multiply(offsetX))
                  .add(up.multiply(offsetY));
    }

    private void drawInsetSolid(GL4 gl,
        PolyhedralBoundedSolid solid,
        SimpleMaterial material,
        Vector3Dd anchorPoint,
        double mainSolidExtent)
    {
        Vector3Dd center;
        double extent;
        double scale;

        if ( solid == null ) {
            return;
        }
        center = solidCenter(solid);
        extent = solidMaxExtent(solid);
        if ( extent < 1e-12 ) {
            extent = 1.0;
        }
        if ( mainSolidExtent < 1e-12 ) {
            mainSolidExtent = 1.0;
        }
        scale = 0.75 * (mainSolidExtent / extent);

        Matrix4x4d modelMatrix = new Matrix4x4d()
            .translation(anchorPoint)
            .multiply(new Matrix4x4d().scale(scale, scale, scale)
                .multiply(new Matrix4x4d().translation(-center.x(), -center.y(),
                    -center.z())));
        Jogl4SimpleMaterialRenderer.activate(gl, material);
        Jogl4PolyhedralBoundedSolidRenderer.draw(gl, solid, model.getCamera(),
            model.getQuality(), modelMatrix);
    }

    public void draw(GL4 gl, int viewportWidth, int viewportHeight)
    {
        PolyhedralBoundedSolid operandA = model.getCsgPreviewOperandA();
        PolyhedralBoundedSolid operandB = model.getCsgPreviewOperandB();
        PolyhedralBoundedSolid mainSolid = model.getSolid();
        Camera camera = model.getCamera();
        double mainExtent;
        Vector3Dd leftAnchor;
        Vector3Dd rightAnchor;

        if ( operandA == null || operandB == null || mainSolid == null ) {
            return;
        }
        if ( viewportWidth <= 0 || viewportHeight <= 0 ) {
            return;
        }
        mainExtent = solidMaxExtent(mainSolid);

        leftAnchor = cameraRelativeAnchor(camera, -0.76, -0.5,
            HUD_INSET_DEPTH);
        rightAnchor = cameraRelativeAnchor(camera, 0.76, -0.5,
            HUD_INSET_DEPTH);

        drawInsetSolid(gl, operandA, csgOperandMaterialA, leftAnchor, mainExtent);
        drawInsetSolid(gl, operandB, csgOperandMaterialB, rightAnchor, mainExtent);
        Jogl4SimpleMaterialRenderer.activate(gl, model.getMaterial());
    }
}
