#include <cmath>
#include <gtest/gtest.h>

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
with Euler operators, also for the washer (height 0) case. C++ counterpart
of Java's `TubeTest`.
*/
namespace {

const double TOLERANCE = 0.01;
const double EPS = 1e-9;

Ray ray(double ox, double oy, double oz, double dx, double dy, double dz)
{
    return Ray(Vector3Dd(ox, oy, oz), Vector3Dd(dx, dy, dz));
}

RayHit fullHit()
{
    return RayHit(RayHit::DETAIL_ALL, true);
}

double signum(double v)
{
    return v > 0 ? 1.0 : (v < 0 ? -1.0 : 0.0);
}

Vector3Dd faceCentroid(_PolyhedralBoundedSolidFace* face)
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

void assertValidBrep(PolyhedralBoundedSolid* solid, int expectedVertices,
    int expectedEdges, int expectedFaces)
{
    PolyhedralBoundedSolidTopologySummary topology =
        PolyhedralBoundedSolidTopologySummary::from(solid);

    EXPECT_EQ(topology.getVertexCount(), expectedVertices);
    EXPECT_EQ(topology.getEdgeCount(), expectedEdges);
    EXPECT_EQ(topology.getFaceCount(), expectedFaces);
    EXPECT_EQ(topology.getShellCount(), 1);
    java::String msg;
    EXPECT_TRUE(PolyhedralBoundedSolidValidationEngine::validateStrict(solid,
        &msg)) << msg.c_str();
}

}

TEST(TubeTest, given_tube_when_rayFromOutsideHitsExternalWall_then_normalPointsOut)
{
    Tube tube(1, 2, 3);
    RayHit hit = fullHit();

    ASSERT_TRUE(tube.doIntersectionFirstHit(ray(-5, 0, 1, 1, 0, 0), &hit));
    EXPECT_NEAR(hit.getRay()->getT(), 3, EPS);
    EXPECT_NEAR(hit.normal.x(), -1, EPS);
}

TEST(TubeTest, given_tube_when_rayStartsInHole_then_hitsInternalWallWithNormalToAxis)
{
    Tube tube(1, 2, 3);
    RayHit hit = fullHit();

    ASSERT_TRUE(tube.doIntersectionFirstHit(ray(0, 0, 1, 1, 0, 0), &hit));
    EXPECT_NEAR(hit.getRay()->getT(), 1, EPS);
    EXPECT_NEAR(hit.normal.x(), -1, EPS);
}

TEST(TubeTest, given_tube_when_verticalRayOverWallOrHole_then_capOrNothingIsHit)
{
    Tube tube(1, 2, 3);
    RayHit hit = fullHit();
    RayHit miss1 = fullHit();
    RayHit miss2 = fullHit();

    ASSERT_TRUE(tube.doIntersectionFirstHit(ray(1.5, 0, 10, 0, 0, -1), &hit));
    EXPECT_NEAR(hit.getRay()->getT(), 7, EPS);
    EXPECT_NEAR(hit.normal.z(), 1, EPS);
    EXPECT_FALSE(tube.doIntersectionFirstHit(ray(0.5, 0, 10, 0, 0, -1), &miss1));
    EXPECT_FALSE(tube.doIntersectionFirstHit(ray(2.5, 0, 10, 0, 0, -1), &miss2));
}

TEST(TubeTest, given_solidCylinderTube_when_rayCrossesAxis_then_oneInterval)
{
    Tube tube(0, 2, 3);
    RaySolidClassification c;

    EXPECT_EQ(tube.classifyRay(ray(-5, 0, 1, 1, 0, 0),
        RayCrossingQuery(0, 100), c), RaySolidQueryStatus::Supported);
    ASSERT_EQ(c.boundaries.size(), 2);
    EXPECT_NEAR(c.boundaries.get(0).t, 3, EPS);
    EXPECT_NEAR(c.boundaries.get(1).t, 7, EPS);
}

TEST(TubeTest, given_tube_when_rayCrossesHole_then_twoInteriorIntervals)
{
    Tube tube(1, 2, 3);
    RaySolidClassification c;

    EXPECT_EQ(tube.classifyRay(ray(-5, 0, 1, 1, 0, 0),
        RayCrossingQuery(0, 100), c), RaySolidQueryStatus::Supported);
    EXPECT_FALSE(c.insideSegments.get(0));
    ASSERT_EQ(c.boundaries.size(), 4);
    double expected[] = {3, 4, 6, 7};
    for ( int i = 0; i < 4; i++ ) {
        EXPECT_NEAR(c.boundaries.get(i).t, expected[i], EPS);
        EXPECT_EQ(c.insideSegments.get(i + 1), i % 2 == 0);
    }
}

TEST(TubeTest, given_tube_when_queryStartsInsideWall_then_startsInside)
{
    Tube tube(1, 2, 3);
    RaySolidClassification c;

    tube.classifyRay(ray(-5, 0, 1, 1, 0, 0), RayCrossingQuery(3.5, 100), c);
    EXPECT_TRUE(c.insideSegments.get(0));
    ASSERT_EQ(c.boundaries.size(), 3);
    EXPECT_NEAR(c.boundaries.get(0).t, 4, EPS);
}

TEST(TubeTest, given_tube_when_pointsClassified_then_wallHoleAndCapsAreRespected)
{
    Tube tube(1, 2, 3);

    EXPECT_EQ(tube.doContainmentTest(Vector3Dd(1.5, 0, 1), TOLERANCE),
        Geometry::INSIDE);
    EXPECT_EQ(tube.doContainmentTest(Vector3Dd(0, 0.5, 1), TOLERANCE),
        Geometry::OUTSIDE);
    EXPECT_EQ(tube.doContainmentTest(Vector3Dd(0, 1.0, 1), TOLERANCE),
        Geometry::LIMIT);
    EXPECT_EQ(tube.doContainmentTest(Vector3Dd(2.0, 0, 1), TOLERANCE),
        Geometry::LIMIT);
    EXPECT_EQ(tube.doContainmentTest(Vector3Dd(1.5, 0, 3), TOLERANCE),
        Geometry::LIMIT);
    EXPECT_EQ(tube.doContainmentTest(Vector3Dd(2.5, 0, 1), TOLERANCE),
        Geometry::OUTSIDE);
    EXPECT_EQ(tube.doContainmentTest(Vector3Dd(1.5, 0, -0.5), TOLERANCE),
        Geometry::OUTSIDE);
}

TEST(TubeTest, given_washer_when_raysAndPointsTested_then_onlyTheAnnulusIsHit)
{
    Tube washer(1, 2, 0);
    RayHit hit = fullHit();
    RayHit miss = fullHit();
    RaySolidClassification c;

    EXPECT_TRUE(washer.isWasher());
    ASSERT_TRUE(washer.doIntersectionFirstHit(ray(1.5, 0, 5, 0, 0, -1), &hit));
    EXPECT_NEAR(hit.getRay()->getT(), 5, EPS);
    EXPECT_NEAR(hit.normal.z(), 1, EPS);
    ASSERT_TRUE(washer.doIntersectionFirstHit(ray(1.5, 0, -5, 0, 0, 1), &hit));
    EXPECT_NEAR(hit.normal.z(), -1, EPS);
    EXPECT_FALSE(washer.doIntersectionFirstHit(ray(0.5, 0, 5, 0, 0, -1), &miss));
    EXPECT_EQ(washer.doContainmentTest(Vector3Dd(1.5, 0, 0), TOLERANCE),
        Geometry::LIMIT);
    EXPECT_EQ(washer.doContainmentTest(Vector3Dd(0.5, 0, 0), TOLERANCE),
        Geometry::OUTSIDE);
    washer.classifyRay(ray(1.5, 0, 5, 0, 0, -1), RayCrossingQuery(0, 100), c);
    EXPECT_EQ(c.boundaries.size(), 0);
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
        if ( std::fabs(normal.z()) > 0.5 ) {
            // Caps: outward along Z
            EXPECT_EQ(signum(normal.z()), p.z() > 1.5 ? 1.0 : -1.0);
        }
        else {
            // Walls: away from the axis outside, towards it inside
            double radial = normal.x() * centroid.x() + normal.y() * centroid.y();
            double rho = std::hypot(centroid.x(), centroid.y());
            EXPECT_EQ(signum(radial), rho > 1.5 ? 1.0 : -1.0);
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

    EXPECT_EQ(topology.getVertexCount(), 2 * n);
    EXPECT_EQ(topology.getEdgeCount(), 2 * n);
    EXPECT_EQ(topology.getFaceCount(), 2);
    for ( long i = 0; i < solid->getPolygonsList().size(); i++ ) {
        EXPECT_EQ(solid->getPolygonsList().get(i)->boundariesList.size(), 2);
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
