//= References:                                                             =
//= [WEIL1977] Weiler, Kevin. Atherton, Peter. "Hidden surface removal      =
//=          using polygon area sorting". SIGGRAPH proceedings, 1977.       =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

package render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.gui.viewport.Viewport;
import vsdk.toolkit.render.TraceWorkspace;
import vsdk.toolkit.render.hiddenSurface.HiddenSurfaceRenderer;
import vsdk.toolkit.render.shaders.FlatShader;

import model.Scene;
import model.history.CameraState;

/**
Builds the drawing of a viewport as projected 2D polygons, computed entirely
in the processor: the visible surfaces of the solids, solved by the hidden
surface removal of Weiler and Atherton [WEIL1977] (`HiddenSurfaceRenderer`),
whose output is a set of polygons that do not overlap on the view. The
result is a list of `ProjectedPolygon`s, so this class does not depend on the
2D technology that finally draws them (see `render.awt`), as
`ProjectedLinesViewBuilder` does for lines.

Only the bodies that are (or can be exported to) a `PolyhedralBoundedSolid`
are presented, as [WEIL1977] works on polygons. Each visible polygon is
filled with the color of its face (constant shading [FOLE1992].16.2.1, by
`FlatShader`) under the lights of the scene, evaluated at the center of the
face. With shadows (see `RendererConfiguration.isWithShadowsSet`, `F10`),
the visible polygons are divided by the shadows the solids cast, computed
by the same algorithm from each light [WEIL1977].218, and each part is
shaded only with the lights that reach it. As the polygons have no depth, the annotations of the
view (selection, lights, gizmos) are drawn over them, without depth test, as
over a raytraced image.

The polygons of a viewport are kept, and given again, while its camera, its
size, the shadows option and the content of the scene (see `DrawingArea.getContentGeneration`) do
not change: the hidden surface removal of a complex scene takes from tens to
hundreds of milliseconds, and the frame is drawn again when only overlays
change.
*/
public class ProjectedSurfacesViewBuilder
{
    private static final ColorRgb NO_LIGHT_COLOR = new ColorRgb(0.8, 0.8, 0.8);

    private static final class CachedView
    {
        CameraState cameraState;
        int width;
        int height;
        long generation;
        boolean withShadows;
        List<ProjectedPolygon> polygons;
    }

    private final BrepExportCache brepCache = new BrepExportCache();
    private final Map<Viewport, CachedView> cache =
        new WeakHashMap<Viewport, CachedView>();
    private final FlatShader shader = new FlatShader();
    private final TraceWorkspace workspace = new TraceWorkspace();

    /**
    Builds the visible polygons presenting the solids of the scene in a
    viewport.
    @param scene scene to present
    @param viewport viewport whose active camera is used; its camera must
    have its viewport size updated
    @param width width of the viewport, in pixels
    @param height height of the viewport, in pixels
    @param generation content generation of the scene (see
    `DrawingArea.getContentGeneration`)
    @param withShadows true to cast the shadows of the solids
    @return the polygons to fill, which do not overlap
    */
    public List<ProjectedPolygon> buildView(Scene scene, Viewport viewport,
                                            int width, int height,
                                            long generation,
                                            boolean withShadows)
    {
        Camera camera = viewport.getActiveCamera();
        CameraState cameraState = CameraState.capture(camera);
        CachedView cached = cache.get(viewport);

        if ( cached != null && cached.width == width &&
             cached.height == height && cached.generation == generation &&
             cached.withShadows == withShadows &&
             cached.cameraState.isSameState(cameraState) ) {
            return cached.polygons;
        }

        List<SimpleBody> solids = new ArrayList<SimpleBody>();
        for ( SimpleBody body : scene.scene.getSimpleBodies() ) {
            if ( scene.isHidden(body) || body.getGeometry() == null ) {
                continue;
            }
            PolyhedralBoundedSolid brep = brepCache.get(body.getGeometry());
            if ( brep != null ) {
                solids.add(brepBody(body, brep));
            }
        }

        List<ProjectedPolygon> polygons = new ArrayList<ProjectedPolygon>();
        List<Light> lights = scene.scene.getLights();
        boolean shadows = withShadows && lights != null && !lights.isEmpty();
        if ( !solids.isEmpty() ) {
            List<HiddenSurfaceRenderer.VisiblePolygon> visible = shadows ?
                HiddenSurfaceRenderer.executeWeilerAthertonAlgorithm(solids,
                    camera, lights) :
                HiddenSurfaceRenderer.executeWeilerAthertonAlgorithm(solids,
                    camera);
            // The color depends on the face and, with shadows, on the
            // lights reaching the polygon
            Map<List<Object>, ColorRgb> colors = new HashMap<List<Object>, ColorRgb>();
            for ( HiddenSurfaceRenderer.VisiblePolygon polygon : visible ) {
                List<Object> key = new ArrayList<Object>();
                key.add(polygon.face);
                key.add(polygon.body);
                if ( polygon.lights != null ) {
                    key.addAll(polygon.lights);
                }
                ColorRgb color = colors.get(key);
                if ( color == null ) {
                    color = shadeFace(polygon, scene, camera,
                        polygon.lights != null ? polygon.lights : lights);
                    colors.put(key, color);
                }
                polygons.add(new ProjectedPolygon(polygon.area, color));
            }
        }

        cached = new CachedView();
        cached.cameraState = cameraState;
        cached.width = width;
        cached.height = height;
        cached.generation = generation;
        cached.withShadows = withShadows;
        cached.polygons = Collections.unmodifiableList(polygons);
        cache.put(viewport, cached);
        return cached.polygons;
    }

    /**
    @param lights lights reaching the polygon
    @return the flat shaded color of the face of a visible polygon, under the
    given lights (without lights in the scene, under a light at the eye)
    */
    private ColorRgb shadeFace(HiddenSurfaceRenderer.VisiblePolygon polygon,
                               Scene scene, Camera camera, List<Light> lights)
    {
        SimpleMaterial material = polygon.body.getMaterial();
        Vector3Dd normal = polygon.worldPlane.getNormal();
        Vector3Dd center = faceCenter(polygon.face, polygon.body);
        Vector3Dd toEye;

        if ( material == null ) {
            material = scene.defaultMaterial();
        }
        if ( camera.getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL ) {
            toEye = camera.getFront().multiply(-1.0).normalized();
        }
        else {
            toEye = camera.getPosition().subtract(center).normalized();
        }
        if ( polygon.lights == null && (lights == null || lights.isEmpty()) ) {
            // Without lights, a light at the eye
            double factor = Math.abs(normal.dotProduct(toEye));
            ColorRgb diffuse = material.getDiffuse();
            return new ColorRgb(NO_LIGHT_COLOR.r() * diffuse.r() * factor,
                NO_LIGHT_COLOR.g() * diffuse.g() * factor,
                NO_LIGHT_COLOR.b() * diffuse.b() * factor);
        }

        RayHit hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
        hit.point = center;
        hit.normal = normal;
        ColorRgb color = shader.shadeLocal(hit, toEye.x(), toEye.y(), toEye.z(),
            lights, Collections.<SimpleBody>emptyList(), material,
            workspace).color();
        return new ColorRgb(clamp(color.r()), clamp(color.g()), clamp(color.b()));
    }

    /**
    @return the average of the vertices of a face, in world coordinates
    */
    private static Vector3Dd faceCenter(_PolyhedralBoundedSolidFace face,
                                        SimpleBody body)
    {
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        int n = 0;

        for ( int i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            if ( loop == null || loop.boundaryStartHalfEdge == null ) {
                continue;
            }
            _PolyhedralBoundedSolidHalfEdge he = loop.boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge heStart = he;
            do {
                if ( he == null || he.startingVertex == null ) {
                    break;
                }
                x += he.startingVertex.position.x();
                y += he.startingVertex.position.y();
                z += he.startingVertex.position.z();
                n++;
                he = he.next();
            } while ( he != heStart );
        }
        if ( n == 0 ) {
            return body.getPosition();
        }
        return body.getTransformationMatrix().multiply(
            new Vector3Dd(x / n, y / n, z / n));
    }

    private static double clamp(double value)
    {
        return Math.max(0.0, Math.min(1.0, value));
    }

    /**
    A body whose geometry is exported to a `PolyhedralBoundedSolid` (i.e. a
    `Box` or a `Sphere`) is replaced, for the hidden surface algorithm, by a
    body with the same transformation and material, and the exported
    boundary representation as geometry.
    */
    private static SimpleBody brepBody(SimpleBody body, PolyhedralBoundedSolid brep)
    {
        Geometry geometry = body.getGeometry();
        if ( geometry == brep ) {
            return body;
        }
        SimpleBody surrogate = new SimpleBody();
        surrogate.setName(body.getName());
        surrogate.setGeometry(brep);
        surrogate.setPosition(body.getPosition());
        surrogate.setRotation(body.getRotation());
        surrogate.setScale(body.getScale());
        surrogate.setMaterialReference(body.getMaterial());
        return surrogate;
    }
}
