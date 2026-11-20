package vsdk.toolkit.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.linealAlgebra.Vector4Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.geometry.element.Vertex2D;
import vsdk.toolkit.environment.geometry.surface.polygon.Polygon2D;
import vsdk.toolkit.environment.geometry.surface.polygon._Polygon2DContour;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.light.AmbientLight;
import vsdk.toolkit.environment.light.DirectionalLight;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.light.PointLight;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.render.hiddenSurface.HiddenSurfaceRenderer;
import vsdk.toolkit.render.hiddenSurface.HiddenSurfaceRenderer.VisiblePolygon;

import static org.assertj.core.api.Assertions.assertThat;

/**
Checks the hidden surface removal of [WEIL1977] against ray casting: at
sample points of the view, the visible polygon covering the point must
belong to the body first hit by the ray of that point, and the visible
polygons must not overlap. With shadows ([WEIL1977].218), the lights given
for that polygon must be the ones whose shadow rays from the hit point reach
them unblocked.
*/
class HiddenSurfaceRendererTest
{
    private static final int VIEW_SIZE = 64;
    /// Samples closer than this (in normalized device coordinates) to an
    /// edge of the output are ambiguous and are not checked
    private static final double EDGE_TOLERANCE = 1.0e-3;

    @Test
    void given_boxPartiallyInFrontOfAnother_when_removingHiddenSurfaces_then_visibleBodiesMatchRayCasting()
    {
        List<SimpleBody> bodies = new ArrayList<SimpleBody>();
        bodies.add(createBox(new Vector3Dd(0.0, 0.0, 0.0), new Vector3Dd(1.0, 1.0, 1.0)));
        bodies.add(createBox(new Vector3Dd(0.4, 0.5, 0.3), new Vector3Dd(1.0, 1.0, 1.0)));

        assertVisibilityMatchesRayCasting(bodies, createCamera(), 0);
    }

    @Test
    void given_penetratingBoxes_when_removingHiddenSurfaces_then_visibleBodiesMatchRayCasting()
    {
        List<SimpleBody> bodies = new ArrayList<SimpleBody>();
        bodies.add(createBox(new Vector3Dd(0.0, 0.0, 0.0), new Vector3Dd(1.6, 0.4, 0.4)));
        bodies.add(createBox(new Vector3Dd(0.1, 0.1, 0.0), new Vector3Dd(0.4, 1.6, 0.6)));

        assertVisibilityMatchesRayCasting(bodies, createCamera(), 0);
    }

    @Test
    void given_cyclicallyOverlappingBars_when_removingHiddenSurfaces_then_visibleBodiesMatchRayCasting()
    {
        // Three bars, each one over the next one at one end and under it at
        // the other ([WEIL1977].216, figure 4b)
        List<SimpleBody> bodies = new ArrayList<SimpleBody>();
        bodies.add(createBox(new Vector3Dd(0.0, -0.6, 0.0), new Vector3Dd(1.8, 0.3, 0.1)));
        bodies.add(createRotatedBox(new Vector3Dd(0.0, 0.0, 0.0),
            new Vector3Dd(1.8, 0.3, 0.1), 120.0, 0.12));
        bodies.add(createRotatedBox(new Vector3Dd(0.0, 0.0, 0.0),
            new Vector3Dd(1.8, 0.3, 0.1), 240.0, -0.12));

        Camera camera = new Camera();
        camera.setPosition(new Vector3Dd(0.0, 0.0, 5.0));
        camera.setRotation(new Matrix4x4d().eulerAnglesRotation(0.0,
            Math.toRadians(-90.0), 0.0));
        camera.updateVectors();
        camera.updateViewportResize(VIEW_SIZE, VIEW_SIZE);

        assertVisibilityMatchesRayCasting(bodies, camera, 0);
    }

    @Test
    void given_randomPenetratingBoxesAndSpheres_when_removingHiddenSurfaces_then_visibleBodiesMatchRayCasting()
    {
        // Many faces sharing edges and vertices, and pieces inscribed in
        // their clip polygons: the degenerate cases of the polygon clipper
        List<SimpleBody> bodies = createRandomScene(new Random(1), 8);

        assertVisibilityMatchesRayCasting(bodies, createCamera(), 0, 2);
    }

    @Test
    void given_randomSceneSeenByOrthogonalCamera_when_removingHiddenSurfaces_then_visibleBodiesMatchRayCasting()
    {
        List<SimpleBody> bodies = createRandomScene(new Random(2), 8);
        Camera camera = createCamera();
        camera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        camera.updateViewportResize(VIEW_SIZE, VIEW_SIZE);

        assertVisibilityMatchesRayCasting(bodies, camera, 0, 2);
    }

    @Test
    void given_boxOverFloorAndPointLight_when_castingShadows_then_litAreasMatchShadowRays()
    {
        List<SimpleBody> bodies = new ArrayList<SimpleBody>();
        bodies.add(createBox(new Vector3Dd(0.0, 0.0, -0.6), new Vector3Dd(3.0, 3.0, 0.2)));
        bodies.add(createBox(new Vector3Dd(0.2, 0.1, 0.1), new Vector3Dd(0.6, 0.6, 0.6)));
        List<Light> lights = new ArrayList<Light>();
        lights.add(new AmbientLight(new ColorRgb(0.1, 0.1, 0.1)));
        lights.add(new PointLight(new Vector3Dd(-1.5, -1.0, 3.0), new ColorRgb(1, 1, 1)));

        assertShadowsMatchRayCasting(bodies, createCamera(), lights, 1);
    }

    @Test
    void given_lightAmongSolidsAndDirectionalLight_when_castingShadows_then_litAreasMatchShadowRays()
    {
        // The first light is inside the bounding sphere of the scene, so it
        // is seen with the six views of a cube
        List<SimpleBody> bodies = createRandomScene(new Random(3), 6);
        bodies.add(createBox(new Vector3Dd(0.0, 0.0, -1.6), new Vector3Dd(4.0, 4.0, 0.2)));
        List<Light> lights = new ArrayList<Light>();
        lights.add(new PointLight(new Vector3Dd(0.3, -0.2, 1.4), new ColorRgb(1, 1, 1)));
        lights.add(new DirectionalLight(new Vector3Dd(0.4, 0.3, -1.0), new ColorRgb(1, 1, 1)));

        assertShadowsMatchRayCasting(bodies, createCamera(), lights, 3);
    }

    private static List<SimpleBody> createRandomScene(Random random, int count)
    {
        List<SimpleBody> bodies = new ArrayList<SimpleBody>();
        for ( int i = 0; i < count; i++ ) {
            SimpleBody body = new SimpleBody();
            if ( i % 2 == 0 ) {
                body.setGeometry(new Sphere(0.3 + 0.4 * random.nextDouble())
                    .exportToPolyhedralBoundedSolid());
            }
            else {
                body.setGeometry(new Box(new Vector3Dd(0.2 + random.nextDouble(),
                    0.2 + random.nextDouble(), 0.2 + random.nextDouble()))
                    .exportToPolyhedralBoundedSolid());
            }
            Matrix4x4d rotation = new Matrix4x4d().eulerAnglesRotation(
                random.nextDouble() * 6, random.nextDouble() * 6,
                random.nextDouble() * 6);
            body.setPosition(new Vector3Dd(random.nextGaussian() * 0.7,
                random.nextGaussian() * 0.7, random.nextGaussian() * 0.7));
            body.setRotation(rotation);
            body.setRotationInverse(rotation.inverse());
            bodies.add(body);
        }
        return bodies;
    }

    private static void assertVisibilityMatchesRayCasting(List<SimpleBody> bodies,
                                                          Camera camera,
                                                          int allowedMismatches)
    {
        assertVisibilityMatchesRayCasting(bodies, camera, allowedMismatches, 1);
    }

    /**
    @param stride distance, in pixels, between the sampled pixels (the ray
    casting of spheres is slow)
    */
    private static void assertVisibilityMatchesRayCasting(List<SimpleBody> bodies,
                                                          Camera camera,
                                                          int allowedMismatches,
                                                          int stride)
    {
        List<VisiblePolygon> visible =
            HiddenSurfaceRenderer.executeWeilerAthertonAlgorithm(bodies, camera);
        Matrix4x4d projection = camera.calculateProjectionMatrix();
        int checked = 0;
        int mismatches = 0;
        int overlaps = 0;

        assertThat(visible).isNotEmpty();
        for ( int y = 0; y < VIEW_SIZE; y += stride ) {
            for ( int x = 0; x < VIEW_SIZE; x += stride ) {
                Ray ray = camera.generateRay(x, y);
                SimpleBody nearest = null;
                Vector3Dd nearestPoint = null;
                double nearestT = Double.MAX_VALUE;
                for ( SimpleBody body : bodies ) {
                    RayHit hit = new RayHit(RayHit.DETAIL_POINT, true);
                    if ( body.doIntersectionFirstHit(new Ray(ray), hit) &&
                         hit.getRay() != null &&
                         hit.getRay().getT() < nearestT ) {
                        nearestT = hit.getRay().getT();
                        nearest = body;
                        nearestPoint = ray.getOrigin().add(
                            ray.getDirection().multiply(nearestT));
                    }
                }
                if ( nearest == null ) {
                    continue;
                }
                Vector4Dd ndc = projection.multiply(new Vector4Dd(nearestPoint)).dividedByW();
                if ( isNearAnEdge(visible, ndc.x(), ndc.y()) ) {
                    continue;
                }
                List<VisiblePolygon> covering = new ArrayList<VisiblePolygon>();
                for ( VisiblePolygon polygon : visible ) {
                    if ( contains(polygon.area, ndc.x(), ndc.y()) ) {
                        covering.add(polygon);
                    }
                }
                checked++;
                if ( covering.size() > 1 ) {
                    overlaps++;
                }
                if ( covering.size() != 1 || covering.get(0).body != nearest ) {
                    mismatches++;
                }
            }
        }
        assertThat(checked).isGreaterThan(100);
        assertThat(overlaps).isZero();
        assertThat(mismatches).isLessThanOrEqualTo(allowedMismatches);
    }

    /**
    @param stride distance, in pixels, between the sampled pixels
    */
    private static void assertShadowsMatchRayCasting(List<SimpleBody> bodies,
                                                     Camera camera,
                                                     List<Light> lights,
                                                     int stride)
    {
        List<VisiblePolygon> visible =
            HiddenSurfaceRenderer.executeWeilerAthertonAlgorithm(bodies, camera,
                lights);
        Matrix4x4d projection = camera.calculateProjectionMatrix();
        int checked = 0;
        int lit = 0;
        int shadowed = 0;
        int mismatches = 0;

        assertThat(visible).isNotEmpty();
        for ( int y = 0; y < VIEW_SIZE; y += stride ) {
            for ( int x = 0; x < VIEW_SIZE; x += stride ) {
                Ray ray = camera.generateRay(x, y);
                Vector3Dd point = firstHit(bodies, ray);
                if ( point == null ) {
                    continue;
                }
                Vector4Dd ndc = projection.multiply(new Vector4Dd(point)).dividedByW();
                if ( isNearAnEdge(visible, ndc.x(), ndc.y()) ) {
                    continue;
                }
                VisiblePolygon covering = null;
                for ( VisiblePolygon polygon : visible ) {
                    if ( contains(polygon.area, ndc.x(), ndc.y()) ) {
                        covering = polygon;
                    }
                }
                assertThat(covering).isNotNull();
                checked++;
                for ( Light light : lights ) {
                    boolean expected = light.isAmbient() || !isInShadow(bodies, point, light);
                    if ( expected ) {
                        lit++;
                    }
                    else {
                        shadowed++;
                    }
                    if ( covering.lights.contains(light) != expected ) {
                        mismatches++;
                    }
                }
            }
        }
        assertThat(checked).isGreaterThan(100);
        assertThat(lit).isGreaterThan(20);
        assertThat(shadowed).isGreaterThan(20);
        assertThat(mismatches).isZero();
    }

    private static Vector3Dd firstHit(List<SimpleBody> bodies, Ray ray)
    {
        Vector3Dd nearestPoint = null;
        double nearestT = Double.MAX_VALUE;
        for ( SimpleBody body : bodies ) {
            RayHit hit = new RayHit(RayHit.DETAIL_POINT, true);
            if ( body.doIntersectionFirstHit(new Ray(ray), hit) &&
                 hit.getRay() != null && hit.getRay().getT() < nearestT ) {
                nearestT = hit.getRay().getT();
                nearestPoint = ray.getOrigin().add(
                    ray.getDirection().multiply(nearestT));
            }
        }
        return nearestPoint;
    }

    /**
    @return true if the shadow ray from a surface point toward a light is
    blocked by a body (the body of the point included: a point facing away
    from the light is in its own shadow)
    */
    private static boolean isInShadow(List<SimpleBody> bodies, Vector3Dd point,
                                      Light light)
    {
        Light.LightDirection toLight = light.getDirectionAndDistance(point);
        double offset = 1.0e-6;
        Ray shadowRay = new Ray(point.add(toLight.direction().multiply(offset)),
            toLight.direction());
        for ( SimpleBody body : bodies ) {
            RayHit hit = new RayHit(RayHit.DETAIL_POINT, true);
            if ( body.doIntersectionFirstHit(new Ray(shadowRay), hit) &&
                 hit.getRay() != null && hit.getRay().getT() > 0.0 &&
                 hit.getRay().getT() < toLight.maxShadowDistance() - offset ) {
                return true;
            }
        }
        return false;
    }

    private static Camera createCamera()
    {
        Camera camera = new Camera();
        camera.setPosition(new Vector3Dd(3.0, -2.0, 2.5));
        camera.setRotation(new Matrix4x4d().eulerAnglesRotation(
            Math.toRadians(146.0), Math.toRadians(-35.0), 0.0));
        camera.updateVectors();
        camera.updateViewportResize(VIEW_SIZE, VIEW_SIZE);
        return camera;
    }

    private static SimpleBody createBox(Vector3Dd position, Vector3Dd size)
    {
        return createRotatedBox(position, size, 0.0, 0.0);
    }

    /**
    @param yawDegrees rotation around the z axis
    @param tilt rotation (radians) around the x axis, tilting the bar
    */
    private static SimpleBody createRotatedBox(Vector3Dd position, Vector3Dd size,
                                               double yawDegrees, double tilt)
    {
        SimpleBody body = new SimpleBody();
        Box box = new Box(size);
        Matrix4x4d rotation = new Matrix4x4d().axisRotation(
            Math.toRadians(yawDegrees), 0.0, 0.0, 1.0).multiply(
            new Matrix4x4d().axisRotation(tilt, 1.0, 0.0, 0.0));
        body.setGeometry(box.exportToPolyhedralBoundedSolid());
        body.setPosition(position);
        body.setRotation(rotation);
        body.setRotationInverse(rotation.inverse());
        return body;
    }

    private static boolean contains(Polygon2D polygon, double x, double y)
    {
        boolean inside = false;
        for ( _Polygon2DContour loop : polygon.loops ) {
            int n = loop.vertices.size();
            for ( int i = 0, j = n - 1; i < n; j = i++ ) {
                Vertex2D a = loop.vertices.get(i);
                Vertex2D b = loop.vertices.get(j);
                if ( (a.y > y) != (b.y > y) &&
                     x < (b.x - a.x) * (y - a.y) / (b.y - a.y) + a.x ) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }

    private static boolean isNearAnEdge(List<VisiblePolygon> visible,
                                        double x, double y)
    {
        for ( VisiblePolygon polygon : visible ) {
            for ( _Polygon2DContour loop : polygon.area.loops ) {
                int n = loop.vertices.size();
                for ( int i = 0; i < n; i++ ) {
                    Vertex2D a = loop.vertices.get(i);
                    Vertex2D b = loop.vertices.get((i + 1) % n);
                    if ( distanceToSegment(x, y, a, b) < EDGE_TOLERANCE ) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static double distanceToSegment(double x, double y, Vertex2D a,
                                            Vertex2D b)
    {
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double length2 = dx * dx + dy * dy;
        double t = length2 > 0.0 ?
            Math.max(0.0, Math.min(1.0, ((x - a.x) * dx + (y - a.y) * dy) / length2)) : 0.0;
        double px = a.x + t * dx - x;
        double py = a.y + t * dy - y;
        return Math.sqrt(px * px + py * py);
    }
}
