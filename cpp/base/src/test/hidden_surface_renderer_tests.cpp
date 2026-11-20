#include <cmath>
#include <gtest/gtest.h>

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
#include "vsdk/toolkit/render/hiddenSurface/HiddenSurfaceRenderer.h"

/**
Checks the hidden surface removal of [WEIL1977] against ray casting: at
sample points of the view, the visible polygon covering the point must
belong to the body first hit by the ray of that point, and the visible
polygons must not overlap. With shadows ([WEIL1977].218), the lights given
for that polygon must be the ones whose shadow rays from the hit point reach
them unblocked.

C++ port of the Java `HiddenSurfaceRendererTest`. The bodies, lights and
visible polygons are freed by the fixture.
*/
class HiddenSurfaceRendererTest : public ::testing::Test
{
protected:
    typedef HiddenSurfaceRenderer::VisiblePolygon VisiblePolygon;

    static const int VIEW_SIZE = 64;
    /// Samples closer than this (in normalized device coordinates) to an
    /// edge of the output are ambiguous and are not checked
    static constexpr double EDGE_TOLERANCE = 1.0e-3;

    java::ArrayList<SimpleBody*> bodies;
    java::ArrayList<Light*> lights;

    void TearDown() override
    {
        for ( long i = 0; i < bodies.size(); i++ ) {
            delete bodies[i];
        }
        for ( long i = 0; i < lights.size(); i++ ) {
            delete lights[i];
        }
    }

    static void deleteAll(java::ArrayList<VisiblePolygon*>& visible)
    {
        for ( long i = 0; i < visible.size(); i++ ) {
            delete visible[i];
        }
        visible.clear();
    }

    static void createRandomScene(java::Random& random, int count,
                                  java::ArrayList<SimpleBody*>& bodies)
    {
        for ( int i = 0; i < count; i++ ) {
            SimpleBody* body = new SimpleBody();
            if ( i % 2 == 0 ) {
                Sphere sphere(0.3 + 0.4 * random.nextDouble());
                body->setGeometry(sphere.exportToPolyhedralBoundedSolid());
            }
            else {
                // C++ port: Java evaluates the arguments from left to right
                double dx = 0.2 + random.nextDouble();
                double dy = 0.2 + random.nextDouble();
                double dz = 0.2 + random.nextDouble();
                Box box(Vector3Dd(dx, dy, dz));
                body->setGeometry(box.exportToPolyhedralBoundedSolid());
            }
            double yaw = random.nextDouble() * 6;
            double pitch = random.nextDouble() * 6;
            double roll = random.nextDouble() * 6;
            Matrix4x4d rotation = Matrix4x4d().eulerAnglesRotation(yaw, pitch,
                roll);
            double x = random.nextGaussian() * 0.7;
            double y = random.nextGaussian() * 0.7;
            double z = random.nextGaussian() * 0.7;
            body->setPosition(Vector3Dd(x, y, z));
            body->setRotation(rotation);
            body->setRotationInverse(rotation.inverse());
            bodies.add(body);
        }
    }

    static void assertVisibilityMatchesRayCasting(
        java::ArrayList<SimpleBody*>& bodies, const Camera& camera,
        int allowedMismatches)
    {
        assertVisibilityMatchesRayCasting(bodies, camera, allowedMismatches, 1);
    }

    /**
    @param stride distance, in pixels, between the sampled pixels (the ray
    casting of spheres is slow)
    */
    static void assertVisibilityMatchesRayCasting(
        java::ArrayList<SimpleBody*>& bodies, const Camera& camera,
        int allowedMismatches, int stride)
    {
        java::ArrayList<VisiblePolygon*> visible =
            HiddenSurfaceRenderer::executeWeilerAthertonAlgorithm(bodies, camera);
        Matrix4x4d projection = camera.calculateProjectionMatrix();
        int checked = 0;
        int mismatches = 0;
        int overlaps = 0;

        EXPECT_FALSE(visible.isEmpty());
        for ( int y = 0; y < VIEW_SIZE; y += stride ) {
            for ( int x = 0; x < VIEW_SIZE; x += stride ) {
                Ray ray = camera.generateRay(x, y);
                SimpleBody* nearest = nullptr;
                Vector3Dd nearestPoint;
                double nearestT = java::Double::MAX_VALUE;
                for ( long b = 0; b < bodies.size(); b++ ) {
                    SimpleBody* body = bodies[b];
                    RayHit hit(RayHit::DETAIL_POINT, true);
                    if ( body->doIntersectionFirstHit(Ray(ray), &hit) &&
                         hit.getRay() != nullptr &&
                         hit.getRay()->getT() < nearestT ) {
                        nearestT = hit.getRay()->getT();
                        nearest = body;
                        nearestPoint = ray.getOrigin().add(
                            ray.getDirection().multiply(nearestT));
                    }
                }
                if ( nearest == nullptr ) {
                    continue;
                }
                Vector4Dd ndc = projection.multiply(Vector4Dd(nearestPoint)).dividedByW();
                if ( isNearAnEdge(visible, ndc.x(), ndc.y()) ) {
                    continue;
                }
                java::ArrayList<VisiblePolygon*> covering;
                for ( long i = 0; i < visible.size(); i++ ) {
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
        deleteAll(visible);
        EXPECT_GT(checked, 100);
        EXPECT_EQ(overlaps, 0);
        EXPECT_LE(mismatches, allowedMismatches);
    }

    /**
    @param stride distance, in pixels, between the sampled pixels
    */
    static void assertShadowsMatchRayCasting(
        java::ArrayList<SimpleBody*>& bodies, const Camera& camera,
        const java::ArrayList<Light*>& lights, int stride)
    {
        java::ArrayList<VisiblePolygon*> visible =
            HiddenSurfaceRenderer::executeWeilerAthertonAlgorithm(bodies, camera,
                lights);
        Matrix4x4d projection = camera.calculateProjectionMatrix();
        int checked = 0;
        int lit = 0;
        int shadowed = 0;
        int mismatches = 0;

        EXPECT_FALSE(visible.isEmpty());
        for ( int y = 0; y < VIEW_SIZE; y += stride ) {
            for ( int x = 0; x < VIEW_SIZE; x += stride ) {
                Ray ray = camera.generateRay(x, y);
                Vector3Dd point;
                if ( !firstHit(bodies, ray, point) ) {
                    continue;
                }
                Vector4Dd ndc = projection.multiply(Vector4Dd(point)).dividedByW();
                if ( isNearAnEdge(visible, ndc.x(), ndc.y()) ) {
                    continue;
                }
                VisiblePolygon* covering = nullptr;
                for ( long i = 0; i < visible.size(); i++ ) {
                    if ( contains(visible[i]->area, ndc.x(), ndc.y()) ) {
                        covering = visible[i];
                    }
                }
                EXPECT_NE(covering, nullptr);
                if ( covering == nullptr ) {
                    continue;
                }
                checked++;
                for ( long l = 0; l < lights.size(); l++ ) {
                    Light* light = lights[l];
                    bool expected = light->isAmbient() || !isInShadow(bodies, point, light);
                    if ( expected ) {
                        lit++;
                    }
                    else {
                        shadowed++;
                    }
                    if ( covering->lights->contains(light) != expected ) {
                        mismatches++;
                    }
                }
            }
        }
        deleteAll(visible);
        EXPECT_GT(checked, 100);
        EXPECT_GT(lit, 20);
        EXPECT_GT(shadowed, 20);
        EXPECT_EQ(mismatches, 0);
    }

    /**
    C++ port: returns false where Java returns null, and the point in `out`.
    */
    static bool firstHit(java::ArrayList<SimpleBody*>& bodies, const Ray& ray,
                         Vector3Dd& out)
    {
        bool found = false;
        double nearestT = java::Double::MAX_VALUE;
        for ( long b = 0; b < bodies.size(); b++ ) {
            RayHit hit(RayHit::DETAIL_POINT, true);
            if ( bodies[b]->doIntersectionFirstHit(Ray(ray), &hit) &&
                 hit.getRay() != nullptr && hit.getRay()->getT() < nearestT ) {
                nearestT = hit.getRay()->getT();
                out = ray.getOrigin().add(ray.getDirection().multiply(nearestT));
                found = true;
            }
        }
        return found;
    }

    /**
    @return true if the shadow ray from a surface point toward a light is
    blocked by a body (the body of the point included: a point facing away
    from the light is in its own shadow)
    */
    static bool isInShadow(java::ArrayList<SimpleBody*>& bodies,
                           const Vector3Dd& point, Light* light)
    {
        Vector3Dd direction;
        double maxShadowDistance;
        light->getDirectionAndDistance(point, &direction, &maxShadowDistance);
        double offset = 1.0e-6;
        Ray shadowRay(point.add(direction.multiply(offset)), direction);
        for ( long b = 0; b < bodies.size(); b++ ) {
            RayHit hit(RayHit::DETAIL_POINT, true);
            if ( bodies[b]->doIntersectionFirstHit(Ray(shadowRay), &hit) &&
                 hit.getRay() != nullptr && hit.getRay()->getT() > 0.0 &&
                 hit.getRay()->getT() < maxShadowDistance - offset ) {
                return true;
            }
        }
        return false;
    }

    static Camera createCamera()
    {
        Camera camera;
        camera.setPosition(Vector3Dd(3.0, -2.0, 2.5));
        camera.setRotation(Matrix4x4d().eulerAnglesRotation(
            java::Math::toRadians(146.0), java::Math::toRadians(-35.0), 0.0));
        camera.updateVectors();
        camera.updateViewportResize(VIEW_SIZE, VIEW_SIZE);
        return camera;
    }

    static SimpleBody* createBox(const Vector3Dd& position, const Vector3Dd& size)
    {
        return createRotatedBox(position, size, 0.0, 0.0);
    }

    /**
    @param yawDegrees rotation around the z axis
    @param tilt rotation (radians) around the x axis, tilting the bar
    */
    static SimpleBody* createRotatedBox(const Vector3Dd& position,
                                        const Vector3Dd& size,
                                        double yawDegrees, double tilt)
    {
        SimpleBody* body = new SimpleBody();
        Box box(size);
        Matrix4x4d rotation = Matrix4x4d().axisRotation(
            java::Math::toRadians(yawDegrees), 0.0, 0.0, 1.0).multiply(
            Matrix4x4d().axisRotation(tilt, 1.0, 0.0, 0.0));
        body->setGeometry(box.exportToPolyhedralBoundedSolid());
        body->setPosition(position);
        body->setRotation(rotation);
        body->setRotationInverse(rotation.inverse());
        return body;
    }

    static bool contains(Polygon2D* polygon, double x, double y)
    {
        bool inside = false;
        for ( long l = 0; l < polygon->loops.size(); l++ ) {
            _Polygon2DContour* loop = polygon->loops[l];
            long n = loop->vertices.size();
            for ( long i = 0, j = n - 1; i < n; j = i++ ) {
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

    static bool isNearAnEdge(const java::ArrayList<VisiblePolygon*>& visible,
                             double x, double y)
    {
        for ( long p = 0; p < visible.size(); p++ ) {
            Polygon2D* area = visible[p]->area;
            for ( long l = 0; l < area->loops.size(); l++ ) {
                _Polygon2DContour* loop = area->loops[l];
                long n = loop->vertices.size();
                for ( long i = 0; i < n; i++ ) {
                    const Vertex2D& a = loop->vertices[i];
                    const Vertex2D& b = loop->vertices[(i + 1) % n];
                    if ( distanceToSegment(x, y, a, b) < EDGE_TOLERANCE ) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    static double distanceToSegment(double x, double y, const Vertex2D& a,
                                    const Vertex2D& b)
    {
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double length2 = dx * dx + dy * dy;
        double t = length2 > 0.0 ?
            java::Math::max(0.0, java::Math::min(1.0, ((x - a.x) * dx + (y - a.y) * dy) / length2)) : 0.0;
        double px = a.x + t * dx - x;
        double py = a.y + t * dy - y;
        return std::sqrt(px * px + py * py);
    }
};

TEST_F(HiddenSurfaceRendererTest,
       given_boxPartiallyInFrontOfAnother_when_removingHiddenSurfaces_then_visibleBodiesMatchRayCasting)
{
    bodies.add(createBox(Vector3Dd(0.0, 0.0, 0.0), Vector3Dd(1.0, 1.0, 1.0)));
    bodies.add(createBox(Vector3Dd(0.4, 0.5, 0.3), Vector3Dd(1.0, 1.0, 1.0)));

    assertVisibilityMatchesRayCasting(bodies, createCamera(), 0);
}

TEST_F(HiddenSurfaceRendererTest,
       given_penetratingBoxes_when_removingHiddenSurfaces_then_visibleBodiesMatchRayCasting)
{
    bodies.add(createBox(Vector3Dd(0.0, 0.0, 0.0), Vector3Dd(1.6, 0.4, 0.4)));
    bodies.add(createBox(Vector3Dd(0.1, 0.1, 0.0), Vector3Dd(0.4, 1.6, 0.6)));

    assertVisibilityMatchesRayCasting(bodies, createCamera(), 0);
}

TEST_F(HiddenSurfaceRendererTest,
       given_cyclicallyOverlappingBars_when_removingHiddenSurfaces_then_visibleBodiesMatchRayCasting)
{
    // Three bars, each one over the next one at one end and under it at
    // the other ([WEIL1977].216, figure 4b)
    bodies.add(createBox(Vector3Dd(0.0, -0.6, 0.0), Vector3Dd(1.8, 0.3, 0.1)));
    bodies.add(createRotatedBox(Vector3Dd(0.0, 0.0, 0.0),
        Vector3Dd(1.8, 0.3, 0.1), 120.0, 0.12));
    bodies.add(createRotatedBox(Vector3Dd(0.0, 0.0, 0.0),
        Vector3Dd(1.8, 0.3, 0.1), 240.0, -0.12));

    Camera camera;
    camera.setPosition(Vector3Dd(0.0, 0.0, 5.0));
    camera.setRotation(Matrix4x4d().eulerAnglesRotation(0.0,
        java::Math::toRadians(-90.0), 0.0));
    camera.updateVectors();
    camera.updateViewportResize(VIEW_SIZE, VIEW_SIZE);

    assertVisibilityMatchesRayCasting(bodies, camera, 0);
}

TEST_F(HiddenSurfaceRendererTest,
       given_randomPenetratingBoxesAndSpheres_when_removingHiddenSurfaces_then_visibleBodiesMatchRayCasting)
{
    // Many faces sharing edges and vertices, and pieces inscribed in
    // their clip polygons: the degenerate cases of the polygon clipper
    java::Random random(1);
    createRandomScene(random, 8, bodies);

    assertVisibilityMatchesRayCasting(bodies, createCamera(), 0, 2);
}

TEST_F(HiddenSurfaceRendererTest,
       given_randomSceneSeenByOrthogonalCamera_when_removingHiddenSurfaces_then_visibleBodiesMatchRayCasting)
{
    java::Random random(2);
    createRandomScene(random, 8, bodies);
    Camera camera = createCamera();
    camera.setProjectionMode(Camera::PROJECTION_MODE_ORTHOGONAL);
    camera.updateViewportResize(VIEW_SIZE, VIEW_SIZE);

    assertVisibilityMatchesRayCasting(bodies, camera, 0, 2);
}

TEST_F(HiddenSurfaceRendererTest,
       given_boxOverFloorAndPointLight_when_castingShadows_then_litAreasMatchShadowRays)
{
    bodies.add(createBox(Vector3Dd(0.0, 0.0, -0.6), Vector3Dd(3.0, 3.0, 0.2)));
    bodies.add(createBox(Vector3Dd(0.2, 0.1, 0.1), Vector3Dd(0.6, 0.6, 0.6)));
    lights.add(new AmbientLight(ColorRgb(0.1, 0.1, 0.1)));
    lights.add(new PointLight(Vector3Dd(-1.5, -1.0, 3.0), ColorRgb(1, 1, 1)));

    assertShadowsMatchRayCasting(bodies, createCamera(), lights, 1);
}

TEST_F(HiddenSurfaceRendererTest,
       given_lightAmongSolidsAndDirectionalLight_when_castingShadows_then_litAreasMatchShadowRays)
{
    // The first light is inside the bounding sphere of the scene, so it
    // is seen with the six views of a cube
    java::Random random(3);
    createRandomScene(random, 6, bodies);
    bodies.add(createBox(Vector3Dd(0.0, 0.0, -1.6), Vector3Dd(4.0, 4.0, 0.2)));
    lights.add(new PointLight(Vector3Dd(0.3, -0.2, 1.4), ColorRgb(1, 1, 1)));
    lights.add(new DirectionalLight(Vector3Dd(0.4, 0.3, -1.0), ColorRgb(1, 1, 1)));

    assertShadowsMatchRayCasting(bodies, createCamera(), lights, 3);
}
