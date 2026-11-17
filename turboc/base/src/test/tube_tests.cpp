// C++ port of Java's TubeTest (environment/geometry/volume).
#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "java/lang/Math.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/element/RayCrossingQuery.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "vsdk/toolkit/environment/geometry/element/RaySolidClassification.h"
#include "vsdk/toolkit/environment/geometry/volume/Tube.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

/*
Exercises the `Tube` (axis along Z, base at z = 0): ray intersection, ray
classification, point containment, and its boundary representation built
with Euler operators, also for the washer (height 0) case.
*/

static const double TOLERANCE = 0.01;
static const double EPS = 1e-9;

static Ray
ray(double ox, double oy, double oz, double dx, double dy, double dz)
{
    return Ray(Vector3Dd(ox, oy, oz), Vector3Dd(dx, dy, dz));
}

static double
signum(double v)
{
    return v > 0 ? 1.0 : (v < 0 ? -1.0 : 0.0);
}

static Vector3Dd
faceCentroid(_PolyhedralBoundedSolidFace* face)
{
    _PolyhedralBoundedSolidHalfEdge* start =
        face->boundariesList.get(0)->boundaryStartHalfEdge;
    _PolyhedralBoundedSolidHalfEdge* he = start;
    Vector3Dd sum(0, 0, 0);
    int count = 0;
    do {
        sum = sum.add(he->startingVertex->position);
        count++;
        he = he->next();
    } while ( he != start );
    return sum.multiply(1.0 / count);
}

static void
assertValidBrep(PolyhedralBoundedSolid* solid, int expectedVertices,
    int expectedEdges, int expectedFaces)
{
    PolyhedralBoundedSolidTopologySummary topology =
        PolyhedralBoundedSolidTopologySummary::from(solid);

    EXPECT_EQ(expectedVertices, topology.getVertexCount());
    EXPECT_EQ(expectedEdges, topology.getEdgeCount());
    EXPECT_EQ(expectedFaces, topology.getFaceCount());
    EXPECT_EQ(1, topology.getShellCount());
    String msg;
    BOOLEAN valid = PolyhedralBoundedSolidValidationEngine::validateStrict(
        solid, &msg);
    if ( !valid ) {
        printf("%s\n", msg.c_str());
    }
    EXPECT_TRUE(valid);
}

TEST(TubeTest, given_tube_when_rayFromOutsideHitsExternalWall_then_normalPointsOut)
{
    Tube tube(1, 2, 3);
    RayHit hit(RayHit::DETAIL_ALL, TRUE);

    ASSERT_TRUE(tube.doIntersectionFirstHit(ray(-5, 0, 1, 1, 0, 0), &hit));
    EXPECT_NEAR(3, hit.getRay()->getT(), EPS);
    EXPECT_NEAR(-1, hit.normal.x(), EPS);
}

TEST(TubeTest, given_tube_when_rayStartsInHole_then_hitsInternalWallWithNormalToAxis)
{
    Tube tube(1, 2, 3);
    RayHit hit(RayHit::DETAIL_ALL, TRUE);

    ASSERT_TRUE(tube.doIntersectionFirstHit(ray(0, 0, 1, 1, 0, 0), &hit));
    EXPECT_NEAR(1, hit.getRay()->getT(), EPS);
    EXPECT_NEAR(-1, hit.normal.x(), EPS);
}

TEST(TubeTest, given_tube_when_verticalRayOverWallOrHole_then_capOrNothingIsHit)
{
    Tube tube(1, 2, 3);
    RayHit hit(RayHit::DETAIL_ALL, TRUE);
    RayHit miss1(RayHit::DETAIL_ALL, TRUE);
    RayHit miss2(RayHit::DETAIL_ALL, TRUE);

    ASSERT_TRUE(tube.doIntersectionFirstHit(ray(1.5, 0, 10, 0, 0, -1), &hit));
    EXPECT_NEAR(7, hit.getRay()->getT(), EPS);
    EXPECT_NEAR(1, hit.normal.z(), EPS);
    EXPECT_FALSE(tube.doIntersectionFirstHit(ray(0.5, 0, 10, 0, 0, -1), &miss1));
    EXPECT_FALSE(tube.doIntersectionFirstHit(ray(2.5, 0, 10, 0, 0, -1), &miss2));
}

TEST(TubeTest, given_solidCylinderTube_when_rayCrossesAxis_then_oneInterval)
{
    Tube tube(0, 2, 3);
    RaySolidClassification c;

    EXPECT_EQ(RaySolidQueryStatus::Supported, tube.classifyRay(
        ray(-5, 0, 1, 1, 0, 0), RayCrossingQuery(0, 100), c));
    ASSERT_EQ(2, c.boundaries.size());
    EXPECT_NEAR(3, c.boundaries[0].t, EPS);
    EXPECT_NEAR(7, c.boundaries[1].t, EPS);
}

TEST(TubeTest, given_tube_when_rayCrossesHole_then_twoInteriorIntervals)
{
    Tube tube(1, 2, 3);
    RaySolidClassification c;

    EXPECT_EQ(RaySolidQueryStatus::Supported, tube.classifyRay(
        ray(-5, 0, 1, 1, 0, 0), RayCrossingQuery(0, 100), c));
    EXPECT_FALSE(c.insideSegments[0]);
    ASSERT_EQ(4, c.boundaries.size());
    const double expected[4] = {3, 4, 6, 7};
    for ( int i = 0; i < 4; i++ ) {
        EXPECT_NEAR(expected[i], c.boundaries[i].t, EPS);
        EXPECT_EQ(i % 2 == 0, (int)c.insideSegments[i + 1]);
    }
}

TEST(TubeTest, given_tube_when_queryStartsInsideWall_then_startsInside)
{
    Tube tube(1, 2, 3);
    RaySolidClassification c;

    tube.classifyRay(ray(-5, 0, 1, 1, 0, 0), RayCrossingQuery(3.5, 100), c);
    EXPECT_TRUE(c.insideSegments[0]);
    ASSERT_EQ(3, c.boundaries.size());
    EXPECT_NEAR(4, c.boundaries[0].t, EPS);
}

TEST(TubeTest, given_tube_when_pointsClassified_then_wallHoleAndCapsAreRespected)
{
    Tube tube(1, 2, 3);

    EXPECT_EQ((int)Geometry::INSIDE,
        tube.doContainmentTest(Vector3Dd(1.5, 0, 1), TOLERANCE));
    EXPECT_EQ((int)Geometry::OUTSIDE,
        tube.doContainmentTest(Vector3Dd(0, 0.5, 1), TOLERANCE));
    EXPECT_EQ((int)Geometry::LIMIT,
        tube.doContainmentTest(Vector3Dd(0, 1.0, 1), TOLERANCE));
    EXPECT_EQ((int)Geometry::LIMIT,
        tube.doContainmentTest(Vector3Dd(2.0, 0, 1), TOLERANCE));
    EXPECT_EQ((int)Geometry::LIMIT,
        tube.doContainmentTest(Vector3Dd(1.5, 0, 3), TOLERANCE));
    EXPECT_EQ((int)Geometry::OUTSIDE,
        tube.doContainmentTest(Vector3Dd(2.5, 0, 1), TOLERANCE));
    EXPECT_EQ((int)Geometry::OUTSIDE,
        tube.doContainmentTest(Vector3Dd(1.5, 0, -0.5), TOLERANCE));
}

TEST(TubeTest, given_washer_when_raysAndPointsTested_then_onlyTheAnnulusIsHit)
{
    Tube washer(1, 2, 0);
    RayHit hit(RayHit::DETAIL_ALL, TRUE);
    RayHit miss(RayHit::DETAIL_ALL, TRUE);
    RaySolidClassification c;

    EXPECT_TRUE(washer.isWasher());
    ASSERT_TRUE(washer.doIntersectionFirstHit(ray(1.5, 0, 5, 0, 0, -1), &hit));
    EXPECT_NEAR(5, hit.getRay()->getT(), EPS);
    EXPECT_NEAR(1, hit.normal.z(), EPS);
    ASSERT_TRUE(washer.doIntersectionFirstHit(ray(1.5, 0, -5, 0, 0, 1), &hit));
    EXPECT_NEAR(-1, hit.normal.z(), EPS);
    EXPECT_FALSE(washer.doIntersectionFirstHit(ray(0.5, 0, 5, 0, 0, -1), &miss));
    EXPECT_EQ((int)Geometry::LIMIT,
        washer.doContainmentTest(Vector3Dd(1.5, 0, 0), TOLERANCE));
    EXPECT_EQ((int)Geometry::OUTSIDE,
        washer.doContainmentTest(Vector3Dd(0.5, 0, 0), TOLERANCE));
    washer.classifyRay(ray(1.5, 0, 5, 0, 0, -1), RayCrossingQuery(0, 100), c);
    EXPECT_TRUE(c.boundaries.isEmpty());
}

TEST(TubeTest, given_tube_when_exportedToBrep_then_isValidSolidOfGenusOne)
{
    int n = 12;
    Tube tube(1, 2, 3);
    PolyhedralBoundedSolid* solid = tube.exportToPolyhedralBoundedSolid(n);

    // Top and bottom annuli (one face each, with a ring) plus n external
    // and n internal side faces; V - E + F - (L - F) = 2 (S - H) with
    // one ring in each cap and genus 1
    assertValidBrep(solid, 4 * n, 6 * n, 2 * n + 2);
    for ( long i = 0; i < solid->getPolygonsList().size(); i++ ) {
        _PolyhedralBoundedSolidFace* face = solid->getPolygonsList().get(i);
        Vector3Dd normal = face->containingPlaneNormal();
        Vector3Dd p = face->boundariesList.get(0)->boundaryStartHalfEdge
            ->startingVertex->position;
        Vector3Dd centroid = faceCentroid(face);
        if ( Math::abs(normal.z()) > 0.5 ) {
            // Caps: outward along Z
            EXPECT_EQ(p.z() > 1.5 ? 1.0 : -1.0, signum(normal.z()));
        }
        else {
            // Walls: away from the axis outside, towards it inside
            double radial = normal.x() * centroid.x() + normal.y() * centroid.y();
            double rho = Math::sqrt(centroid.x() * centroid.x() +
                centroid.y() * centroid.y());
            EXPECT_EQ(rho > 1.5 ? 1.0 : -1.0, signum(radial));
        }
    }
    delete solid;
}

TEST(TubeTest, given_washer_when_exportedToBrep_then_isLaminaWithHole)
{
    int n = 12;
    Tube washer(1, 2, 0);
    PolyhedralBoundedSolid* solid = washer.exportToPolyhedralBoundedSolid(n);
    PolyhedralBoundedSolidTopologySummary topology =
        PolyhedralBoundedSolidTopologySummary::from(solid);

    EXPECT_EQ(2 * n, topology.getVertexCount());
    EXPECT_EQ(2 * n, topology.getEdgeCount());
    EXPECT_EQ(2, topology.getFaceCount());
    for ( long i = 0; i < solid->getPolygonsList().size(); i++ ) {
        EXPECT_EQ(2, solid->getPolygonsList().get(i)->boundariesList.size());
    }
    delete solid;
}

TEST(TubeTest, given_solidCylinderTube_when_exportedToBrep_then_isValid)
{
    int n = 12;
    Tube tube(0, 2, 3);
    PolyhedralBoundedSolid* solid = tube.exportToPolyhedralBoundedSolid(n);

    assertValidBrep(solid, 2 * n, 3 * n, n + 2);
    delete solid;
}

VITRAL_TEST_MAIN()
