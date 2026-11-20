#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/render/hiddenSurface/HiddenSurfaceRenderer.h"
#include "VitralTest.h"
#include "java/lang/Double.h"
#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
#include "java/util/Random.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector4Dd.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "vsdk/toolkit/environment/geometry/element/Vertex2D.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/_Polygon2DContour.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/light/AmbientLight.h"
#include "vsdk/toolkit/environment/light/DirectionalLight.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/light/PointLight.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"

/**
Checks the hidden surface removal of [WEIL1977] against ray casting: at
sample points of the view, the visible polygon covering the point must
belong to the body first hit by the ray of that point, and the visible
polygons must not overlap. With shadows ([WEIL1977].218), the lights given
for that polygon must be the ones whose shadow rays from the hit point reach
them unblocked.

C++ port note: translation of the Java HiddenSurfaceRendererTest (JUnit and
AssertJ), with the bodies, lights and visible polygons deleted at the end of
each case.
*/

static const int VIEW_SIZE = 64;
/// Samples closer than this (in normalized device coordinates) to an
/// edge of the output are ambiguous and are not checked
static const double EDGE_TOLERANCE = 1.0e-3;

static ArrayList<SimpleBody*> createRandomScene(Random& random, int count);
static void assertVisibilityMatchesRayCasting(ArrayList<SimpleBody*>& bodies,
    Camera* camera, int allowedMismatches);
static void assertVisibilityMatchesRayCasting(ArrayList<SimpleBody*>& bodies,
    Camera* camera, int allowedMismatches, int stride);
static void assertShadowsMatchRayCasting(ArrayList<SimpleBody*>& bodies,
    Camera* camera, ArrayList<Light*>& lights, int stride);
static BOOLEAN firstHit(ArrayList<SimpleBody*>& bodies, const Ray& ray,
    Vector3Dd& nearestPoint);
static BOOLEAN isInShadow(ArrayList<SimpleBody*>& bodies,
    const Vector3Dd& point, Light* light);
static Camera* createCamera();
static SimpleBody* createBox(const Vector3Dd& position, const Vector3Dd& size);
static SimpleBody* createRotatedBox(const Vector3Dd& position,
    const Vector3Dd& size, double yawDegrees, double tilt);
static BOOLEAN contains(Polygon2D* polygon, double x, double y);
static BOOLEAN isNearAnEdge(
    ArrayList<HiddenSurfaceRenderer::VisiblePolygon*>& visible,
    double x, double y);
static double distanceToSegment(double x, double y, const Vertex2D& a,
    const Vertex2D& b);
static void deleteBodies(ArrayList<SimpleBody*>& bodies);
static void deleteLights(ArrayList<Light*>& lights);
static void deleteVisible(
    ArrayList<HiddenSurfaceRenderer::VisiblePolygon*>& visible);

TEST(HiddenSurfaceRendererTest, given_boxPartiallyInFrontOfAnother_when_removingHiddenSurfaces_then_visibleBodiesMatchRayCasting)
{
    ArrayList<SimpleBody*> bodies;
    Camera* camera = createCamera();
    bodies.add(createBox(Vector3Dd(0.0, 0.0, 0.0), Vector3Dd(1.0, 1.0, 1.0)));
    bodies.add(createBox(Vector3Dd(0.4, 0.5, 0.3), Vector3Dd(1.0, 1.0, 1.0)));

    assertVisibilityMatchesRayCasting(bodies, camera, 0);
    delete camera;
    deleteBodies(bodies);
}

TEST(HiddenSurfaceRendererTest, given_penetratingBoxes_when_removingHiddenSurfaces_then_visibleBodiesMatchRayCasting)
{
    ArrayList<SimpleBody*> bodies;
    Camera* camera = createCamera();
    bodies.add(createBox(Vector3Dd(0.0, 0.0, 0.0), Vector3Dd(1.6, 0.4, 0.4)));
    bodies.add(createBox(Vector3Dd(0.1, 0.1, 0.0), Vector3Dd(0.4, 1.6, 0.6)));

    assertVisibilityMatchesRayCasting(bodies, camera, 0);
    delete camera;
    deleteBodies(bodies);
}

TEST(HiddenSurfaceRendererTest, given_cyclicallyOverlappingBars_when_removingHiddenSurfaces_then_visibleBodiesMatchRayCasting)
{
    // Three bars, each one over the next one at one end and under it at
    // the other ([WEIL1977].216, figure 4b)
    ArrayList<SimpleBody*> bodies;
    Camera* camera;
    bodies.add(createBox(Vector3Dd(0.0, -0.6, 0.0), Vector3Dd(1.8, 0.3, 0.1)));
    bodies.add(createRotatedBox(Vector3Dd(0.0, 0.0, 0.0),
        Vector3Dd(1.8, 0.3, 0.1), 120.0, 0.12));
    bodies.add(createRotatedBox(Vector3Dd(0.0, 0.0, 0.0),
        Vector3Dd(1.8, 0.3, 0.1), 240.0, -0.12));

    camera = new Camera();
    camera->setPosition(Vector3Dd(0.0, 0.0, 5.0));
    camera->setRotation(Matrix4x4d().eulerAnglesRotation(0.0,
        Math::toRadians(-90.0), 0.0));
    camera->updateVectors();
    camera->updateViewportResize(VIEW_SIZE, VIEW_SIZE);

    assertVisibilityMatchesRayCasting(bodies, camera, 0);
    delete camera;
    deleteBodies(bodies);
}

TEST(HiddenSurfaceRendererTest, given_randomPenetratingBoxesAndSpheres_when_removingHiddenSurfaces_then_visibleBodiesMatchRayCasting)
{
    // Many faces sharing edges and vertices, and pieces inscribed in
    // their clip polygons: the degenerate cases of the polygon clipper
    Random random(1);
    ArrayList<SimpleBody*> bodies = createRandomScene(random, 8);
    Camera* camera = createCamera();

    assertVisibilityMatchesRayCasting(bodies, camera, 0, 2);
    delete camera;
    deleteBodies(bodies);
}

TEST(HiddenSurfaceRendererTest, given_randomSceneSeenByOrthogonalCamera_when_removingHiddenSurfaces_then_visibleBodiesMatchRayCasting)
{
    Random random(2);
    ArrayList<SimpleBody*> bodies = createRandomScene(random, 8);
    Camera* camera = createCamera();
    camera->setProjectionMode(Camera::PROJECTION_MODE_ORTHOGONAL);
    camera->updateViewportResize(VIEW_SIZE, VIEW_SIZE);

    assertVisibilityMatchesRayCasting(bodies, camera, 0, 2);
    delete camera;
    deleteBodies(bodies);
}

TEST(HiddenSurfaceRendererTest, given_boxOverFloorAndPointLight_when_castingShadows_then_litAreasMatchShadowRays)
{
    ArrayList<SimpleBody*> bodies;
    ArrayList<Light*> lights;
    Camera* camera = createCamera();
    bodies.add(createBox(Vector3Dd(0.0, 0.0, -0.6), Vector3Dd(3.0, 3.0, 0.2)));
    bodies.add(createBox(Vector3Dd(0.2, 0.1, 0.1), Vector3Dd(0.6, 0.6, 0.6)));
    lights.add(new AmbientLight(ColorRgb(0.1, 0.1, 0.1)));
    lights.add(new PointLight(Vector3Dd(-1.5, -1.0, 3.0), ColorRgb(1, 1, 1)));

    assertShadowsMatchRayCasting(bodies, camera, lights, 1);
    delete camera;
    deleteLights(lights);
    deleteBodies(bodies);
}

TEST(HiddenSurfaceRendererTest, given_lightAmongSolidsAndDirectionalLight_when_castingShadows_then_litAreasMatchShadowRays)
{
    // The first light is inside the bounding sphere of the scene, so it
    // is seen with the six views of a cube
    Random random(3);
    ArrayList<SimpleBody*> bodies = createRandomScene(random, 6);
    ArrayList<Light*> lights;
    Camera* camera = createCamera();
    bodies.add(createBox(Vector3Dd(0.0, 0.0, -1.6), Vector3Dd(4.0, 4.0, 0.2)));
    lights.add(new PointLight(Vector3Dd(0.3, -0.2, 1.4), ColorRgb(1, 1, 1)));
    lights.add(new DirectionalLight(Vector3Dd(0.4, 0.3, -1.0), ColorRgb(1, 1, 1)));

    assertShadowsMatchRayCasting(bodies, camera, lights, 3);
    delete camera;
    deleteLights(lights);
    deleteBodies(bodies);
}

static ArrayList<SimpleBody*>
createRandomScene(Random& random, int count)
{
    ArrayList<SimpleBody*> bodies;
    int i;

    for ( i = 0; i < count; i++ ) {
        SimpleBody* body = new SimpleBody();
        Matrix4x4d rotation;
        double yaw;
        double pitch;
        double roll;
        double x;
        double y;
        double z;
        if ( i % 2 == 0 ) {
            Sphere sphere(0.3 + 0.4 * random.nextDouble());
            body->setGeometry(sphere.exportToPolyhedralBoundedSolid());
        }
        else {
            double dx = 0.2 + random.nextDouble();
            double dy = 0.2 + random.nextDouble();
            double dz = 0.2 + random.nextDouble();
            Vector3Dd size(dx, dy, dz);
            Box box(size);
            body->setGeometry(box.exportToPolyhedralBoundedSolid());
        }
        // C++ port note: the arguments are taken in the order Java
        // evaluates them
        yaw = random.nextDouble() * 6;
        pitch = random.nextDouble() * 6;
        roll = random.nextDouble() * 6;
        rotation = Matrix4x4d().eulerAnglesRotation(yaw, pitch, roll);
        x = random.nextGaussian() * 0.7;
        y = random.nextGaussian() * 0.7;
        z = random.nextGaussian() * 0.7;
        body->setPosition(Vector3Dd(x, y, z));
        body->setRotation(rotation);
        body->setRotationInverse(rotation.inverse());
        bodies.add(body);
    }
    return bodies;
}

static void
assertVisibilityMatchesRayCasting(ArrayList<SimpleBody*>& bodies,
    Camera* camera, int allowedMismatches)
{
    assertVisibilityMatchesRayCasting(bodies, camera, allowedMismatches, 1);
}

/**
@param stride distance, in pixels, between the sampled pixels (the ray
casting of spheres is slow)
*/
static void
assertVisibilityMatchesRayCasting(ArrayList<SimpleBody*>& bodies,
    Camera* camera, int allowedMismatches, int stride)
{
    ArrayList<HiddenSurfaceRenderer::VisiblePolygon*> visible =
        HiddenSurfaceRenderer::executeWeilerAthertonAlgorithm(bodies, camera);
    Matrix4x4d projection = camera->calculateProjectionMatrix();
    long int checked = 0;
    long int mismatches = 0;
    long int overlaps = 0;
    int x;
    int y;
    long int i;

    EXPECT_FALSE(visible.isEmpty());
    for ( y = 0; y < VIEW_SIZE; y += stride ) {
        for ( x = 0; x < VIEW_SIZE; x += stride ) {
            Ray ray = camera->generateRay(x, y);
            SimpleBody* nearest = NULL;
            Vector3Dd nearestPoint;
            double nearestT = Double::MAX_VALUE;
            Vector4Dd ndc;
            ArrayList<HiddenSurfaceRenderer::VisiblePolygon*> covering;
            for ( i = 0; i < bodies.size(); i++ ) {
                SimpleBody* body = bodies[i];
                RayHit hit(RayHit::DETAIL_POINT, TRUE);
                if ( body->doIntersectionFirstHit(Ray(ray), &hit) &&
                     hit.getRay() != NULL &&
                     hit.getRay()->getT() < nearestT ) {
                    nearestT = hit.getRay()->getT();
                    nearest = body;
                    nearestPoint = ray.getOrigin().add(
                        ray.getDirection().multiply(nearestT));
                }
            }
            if ( nearest == NULL ) {
                continue;
            }
            ndc = projection.multiply(Vector4Dd(nearestPoint)).dividedByW();
            if ( isNearAnEdge(visible, ndc.x(), ndc.y()) ) {
                continue;
            }
            for ( i = 0; i < visible.size(); i++ ) {
                if ( contains(visible[i]->area, ndc.x(), ndc.y()) ) {
                    covering.add(visible[i]);
                }
            }
            checked++;
            if ( covering.size() > 1 ) {
                overlaps++;
            }
            if ( covering.size() != 1 || covering[0]->body != nearest ) {
                mismatches++;
            }
        }
    }
    EXPECT_GT(checked, 100);
    EXPECT_EQ(overlaps, 0);
    EXPECT_LE(mismatches, allowedMismatches);
    deleteVisible(visible);
}

/**
@param stride distance, in pixels, between the sampled pixels
*/
static void
assertShadowsMatchRayCasting(ArrayList<SimpleBody*>& bodies, Camera* camera,
    ArrayList<Light*>& lights, int stride)
{
    ArrayList<HiddenSurfaceRenderer::VisiblePolygon*> visible =
        HiddenSurfaceRenderer::executeWeilerAthertonAlgorithm(bodies, camera,
            lights);
    Matrix4x4d projection = camera->calculateProjectionMatrix();
    long int checked = 0;
    long int lit = 0;
    long int shadowed = 0;
    long int mismatches = 0;
    BOOLEAN missingCovering = FALSE;
    int x;
    int y;
    long int i;

    EXPECT_FALSE(visible.isEmpty());
    for ( y = 0; y < VIEW_SIZE && !missingCovering; y += stride ) {
        for ( x = 0; x < VIEW_SIZE; x += stride ) {
            Ray ray = camera->generateRay(x, y);
            Vector3Dd point;
            Vector4Dd ndc;
            HiddenSurfaceRenderer::VisiblePolygon* covering = NULL;
            if ( !firstHit(bodies, ray, point) ) {
                continue;
            }
            ndc = projection.multiply(Vector4Dd(point)).dividedByW();
            if ( isNearAnEdge(visible, ndc.x(), ndc.y()) ) {
                continue;
            }
            for ( i = 0; i < visible.size(); i++ ) {
                if ( contains(visible[i]->area, ndc.x(), ndc.y()) ) {
                    covering = visible[i];
                }
            }
            // C++ port note: the failed assertion leaves the case after
            // the visible polygons are deleted
            EXPECT_TRUE(covering != NULL);
            if ( covering == NULL ) {
                missingCovering = TRUE;
                break;
            }
            checked++;
            for ( i = 0; i < lights.size(); i++ ) {
                Light* light = lights[i];
                BOOLEAN expected = light->isAmbient() ||
                    !isInShadow(bodies, point, light);
                if ( expected ) {
                    lit++;
                }
                else {
                    shadowed++;
                }
                if ( (covering->lights->contains(light) ? TRUE : FALSE) !=
                     (expected ? TRUE : FALSE) ) {
                    mismatches++;
                }
            }
        }
    }
    if ( !missingCovering ) {
        EXPECT_GT(checked, 100);
        EXPECT_GT(lit, 20);
        EXPECT_GT(shadowed, 20);
        EXPECT_EQ(mismatches, 0);
    }
    deleteVisible(visible);
}

/**
@param nearestPoint receives the first point of the bodies hit by the ray
@return false if the ray hits nothing (the Java version returns null)
*/
static BOOLEAN
firstHit(ArrayList<SimpleBody*>& bodies, const Ray& ray,
         Vector3Dd& nearestPoint)
{
    BOOLEAN found = FALSE;
    double nearestT = Double::MAX_VALUE;
    long int i;

    for ( i = 0; i < bodies.size(); i++ ) {
        RayHit hit(RayHit::DETAIL_POINT, TRUE);
        if ( bodies[i]->doIntersectionFirstHit(Ray(ray), &hit) &&
             hit.getRay() != NULL && hit.getRay()->getT() < nearestT ) {
            nearestT = hit.getRay()->getT();
            nearestPoint = ray.getOrigin().add(
                ray.getDirection().multiply(nearestT));
            found = TRUE;
        }
    }
    return found;
}

/**
@return true if the shadow ray from a surface point toward a light is
blocked by a body (the body of the point included: a point facing away
from the light is in its own shadow)
*/
static BOOLEAN
isInShadow(ArrayList<SimpleBody*>& bodies, const Vector3Dd& point,
           Light* light)
{
    Vector3Dd direction;
    double maxShadowDistance;
    double offset = 1.0e-6;
    long int i;

    light->getDirectionAndDistance(point, &direction, &maxShadowDistance);
    Ray shadowRay(point.add(direction.multiply(offset)), direction);
    for ( i = 0; i < bodies.size(); i++ ) {
        RayHit hit(RayHit::DETAIL_POINT, TRUE);
        if ( bodies[i]->doIntersectionFirstHit(Ray(shadowRay), &hit) &&
             hit.getRay() != NULL && hit.getRay()->getT() > 0.0 &&
             hit.getRay()->getT() < maxShadowDistance - offset ) {
            return TRUE;
        }
    }
    return FALSE;
}

static Camera*
createCamera()
{
    Camera* camera = new Camera();
    camera->setPosition(Vector3Dd(3.0, -2.0, 2.5));
    camera->setRotation(Matrix4x4d().eulerAnglesRotation(
        Math::toRadians(146.0), Math::toRadians(-35.0), 0.0));
    camera->updateVectors();
    camera->updateViewportResize(VIEW_SIZE, VIEW_SIZE);
    return camera;
}

static SimpleBody*
createBox(const Vector3Dd& position, const Vector3Dd& size)
{
    return createRotatedBox(position, size, 0.0, 0.0);
}

/**
@param yawDegrees rotation around the z axis
@param tilt rotation (radians) around the x axis, tilting the bar
*/
static SimpleBody*
createRotatedBox(const Vector3Dd& position, const Vector3Dd& size,
                 double yawDegrees, double tilt)
{
    SimpleBody* body = new SimpleBody();
    Box box(size);
    Matrix4x4d rotation = Matrix4x4d().axisRotation(
        Math::toRadians(yawDegrees), 0.0, 0.0, 1.0).multiply(
        Matrix4x4d().axisRotation(tilt, 1.0, 0.0, 0.0));
    body->setGeometry(box.exportToPolyhedralBoundedSolid());
    body->setPosition(position);
    body->setRotation(rotation);
    body->setRotationInverse(rotation.inverse());
    return body;
}

static BOOLEAN
contains(Polygon2D* polygon, double x, double y)
{
    BOOLEAN inside = FALSE;
    long int i;
    long int j;
    long int k;

    for ( k = 0; k < polygon->loops.size(); k++ ) {
        _Polygon2DContour* loop = polygon->loops[k];
        long int n = loop->vertices.size();
        for ( i = 0, j = n - 1; i < n; j = i++ ) {
            const Vertex2D& a = loop->vertices[i];
            const Vertex2D& b = loop->vertices[j];
            if ( (a.y > y) != (b.y > y) &&
                 x < (b.x - a.x) * (y - a.y) / (b.y - a.y) + a.x ) {
                inside = !inside;
            }
        }
    }
    return inside;
}

static BOOLEAN
isNearAnEdge(ArrayList<HiddenSurfaceRenderer::VisiblePolygon*>& visible,
             double x, double y)
{
    long int p;
    long int k;
    long int i;

    for ( p = 0; p < visible.size(); p++ ) {
        Polygon2D* area = visible[p]->area;
        for ( k = 0; k < area->loops.size(); k++ ) {
            _Polygon2DContour* loop = area->loops[k];
            long int n = loop->vertices.size();
            for ( i = 0; i < n; i++ ) {
                const Vertex2D& a = loop->vertices[i];
                const Vertex2D& b = loop->vertices[(i + 1) % n];
                if ( distanceToSegment(x, y, a, b) < EDGE_TOLERANCE ) {
                    return TRUE;
                }
            }
        }
    }
    return FALSE;
}

static double
distanceToSegment(double x, double y, const Vertex2D& a, const Vertex2D& b)
{
    double dx = b.x - a.x;
    double dy = b.y - a.y;
    double length2 = dx * dx + dy * dy;
    double t = length2 > 0.0 ?
        Math::max(0.0, Math::min(1.0, ((x - a.x) * dx + (y - a.y) * dy) / length2)) : 0.0;
    double px = a.x + t * dx - x;
    double py = a.y + t * dy - y;
    return Math::sqrt(px * px + py * py);
}

static void
deleteBodies(ArrayList<SimpleBody*>& bodies)
{
    long int i;

    for ( i = 0; i < bodies.size(); i++ ) {
        delete bodies[i];
    }
    bodies.clear();
}

static void
deleteLights(ArrayList<Light*>& lights)
{
    long int i;

    for ( i = 0; i < lights.size(); i++ ) {
        delete lights[i];
    }
    lights.clear();
}

static void
deleteVisible(ArrayList<HiddenSurfaceRenderer::VisiblePolygon*>& visible)
{
    long int i;

    for ( i = 0; i < visible.size(); i++ ) {
        delete visible[i];
    }
    visible.clear();
}

VITRAL_TEST_MAIN()
