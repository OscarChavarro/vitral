#include <cmath>
#include <cstdlib>
#include <gtest/gtest.h>

#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/EntityControlAccessor.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector2Dd.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/element/GeometryBounds.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/element/RayBoundary.h"
#include "vsdk/toolkit/environment/geometry/element/RayCrossingQuery.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "vsdk/toolkit/environment/geometry/element/RaySolidClassification.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "vsdk/toolkit/environment/geometry/volume/Prism.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/csg/CsgBody.h"

/*
Exercises the parametric `Prism` (base polygon on z = 0, swept along +Z):
ray intersection and [ROTH1982] ray classification against convex and
concave bases (compared with `Box` for a rectangular base), CSG with the
Roth evaluator, point containment, the boundary representation and the
parameters exposed to generic editors.
*/
namespace {

const double EPS = 1e-9;

Ray ray(double ox, double oy, double oz, double dx, double dy, double dz)
{
    return Ray(Vector3Dd(ox, oy, oz), Vector3Dd(dx, dy, dz));
}

java::ArrayList<Vector2Dd> square(double half)
{
    java::ArrayList<Vector2Dd> polygon;
    polygon.add(Vector2Dd(-half, -half));
    polygon.add(Vector2Dd(half, -half));
    polygon.add(Vector2Dd(half, half));
    polygon.add(Vector2Dd(-half, half));
    return polygon;
}

/** "U" shape: x in [0, 3], y in [0, 2], with the notch x in [1, 2], y > 1 */
java::ArrayList<Vector2Dd> uShape()
{
    java::ArrayList<Vector2Dd> polygon;
    polygon.add(Vector2Dd(0, 0));
    polygon.add(Vector2Dd(3, 0));
    polygon.add(Vector2Dd(3, 2));
    polygon.add(Vector2Dd(2, 2));
    polygon.add(Vector2Dd(2, 1));
    polygon.add(Vector2Dd(1, 1));
    polygon.add(Vector2Dd(1, 2));
    polygon.add(Vector2Dd(0, 2));
    return polygon;
}

java::ArrayList<Vector2Dd> star(int points, double outer, double inner)
{
    java::ArrayList<Vector2Dd> polygon;
    for ( int i = 0; i < 2 * points; i++ ) {
        double a = M_PI * i / points;
        double r = i % 2 == 0 ? outer : inner;
        polygon.add(Vector2Dd(r * std::cos(a), r * std::sin(a)));
    }
    return polygon;
}

double random(double low, double high)
{
    return low + (high - low) * (std::rand() / (double)RAND_MAX);
}

}

TEST(PrismTest, given_squarePrism_when_randomRaysClassified_then_matchesBox)
{
    Prism prism(square(1.0), 2.0);
    Box box(Vector3Dd(-1, -1, 0), Vector3Dd(1, 1, 2));
    std::srand(12345);
    int compared = 0;

    for ( int i = 0; i < 2000; i++ ) {
        Vector3Dd origin(random(-4, 4), random(-4, 4), random(-3, 5));
        Vector3Dd direction(random(-1, 1), random(-1, 1), random(-1, 1));
        if ( direction.length() < 0.1 ) {
            continue;
        }
        Ray r(origin, direction);
        RayCrossingQuery query(0.0, 100.0);
        RaySolidClassification fromPrism;
        RaySolidClassification fromBox;
        ASSERT_EQ(prism.classifyRay(r, query, fromPrism),
            RaySolidQueryStatus::Supported);
        ASSERT_EQ(box.classifyRay(r, query, fromBox),
            RaySolidQueryStatus::Supported);
        ASSERT_EQ(fromPrism.boundaries.size(), fromBox.boundaries.size());
        ASSERT_EQ(fromPrism.insideSegments.size(), fromBox.insideSegments.size());
        for ( long j = 0; j < fromBox.boundaries.size(); j++ ) {
            EXPECT_NEAR(fromPrism.boundaries[j].t, fromBox.boundaries[j].t, 1e-9);
        }
        for ( long j = 0; j < fromBox.insideSegments.size(); j++ ) {
            EXPECT_EQ(fromPrism.insideSegments[j], fromBox.insideSegments[j]);
        }

        RayHit prismHit(RayHit::DETAIL_ALL, true);
        RayHit boxHit(RayHit::DETAIL_ALL, true);
        RayFirstHitStatus prismStatus = prism.queryFirstHit(r, query, &prismHit);
        RayFirstHitStatus boxStatus = box.queryFirstHit(r, query, &boxHit);
        ASSERT_EQ(prismStatus, boxStatus);
        if ( prismStatus == RayFirstHitStatus::Hit ) {
            EXPECT_NEAR(prismHit.getHitDistance(), boxHit.getHitDistance(), 1e-9);
            EXPECT_NEAR(prismHit.normal.dotProduct(boxHit.normal), 1.0, 1e-9);
            compared++;
        }
    }
    EXPECT_GT(compared, 100);
}

TEST(PrismTest, given_concavePrism_when_rayCrossesNotch_then_twoInteriorIntervals)
{
    Prism prism(uShape(), 1.0);
    // Along x at y = 1.5, z = 0.5: inside [0,1] and [2,3]
    RaySolidClassification result;
    ASSERT_EQ(prism.classifyRay(ray(-1, 1.5, 0.5, 1, 0, 0),
        RayCrossingQuery(-10, 10), result), RaySolidQueryStatus::Supported);
    ASSERT_EQ(result.boundaries.size(), 4);
    EXPECT_NEAR(result.boundaries[0].t, 1.0, EPS);
    EXPECT_NEAR(result.boundaries[1].t, 2.0, EPS);
    EXPECT_NEAR(result.boundaries[2].t, 3.0, EPS);
    EXPECT_NEAR(result.boundaries[3].t, 4.0, EPS);
    EXPECT_FALSE(result.insideSegments[0]);
    EXPECT_TRUE(result.insideSegments[1]);
    EXPECT_FALSE(result.insideSegments[2]);
    EXPECT_TRUE(result.insideSegments[3]);
    EXPECT_FALSE(result.insideSegments[4]);

    // The first hit from inside the notch is the inner wall of the right arm
    RayHit hit(RayHit::DETAIL_ALL, true);
    ASSERT_EQ(prism.queryFirstHit(ray(1.5, 1.5, 0.5, 1, 0, 0),
        RayCrossingQuery(0, 10), &hit), RayFirstHitStatus::Hit);
    EXPECT_NEAR(hit.getHitDistance(), 0.5, EPS);
    EXPECT_NEAR(hit.normal.x(), -1.0, EPS);
}

TEST(PrismTest, given_concavePrism_when_rayPassesThroughVertices_then_intervalsAreConsistent)
{
    Prism prism(uShape(), 1.0);
    RaySolidClassification result;

    // Line x + y = 2: enters at the convex corner (0,2), goes through the
    // reflex corner (1,1) without leaving the base, and leaves at (2,0)
    ASSERT_EQ(prism.classifyRay(ray(-1, 3, 0.5, 1, -1, 0),
        RayCrossingQuery(-10, 10), result), RaySolidQueryStatus::Supported);
    ASSERT_EQ(result.boundaries.size(), 2);
    // (Ray normalizes its direction: t is a distance)
    EXPECT_NEAR(result.boundaries[0].t, 1.0 * std::sqrt(2.0), EPS);
    EXPECT_NEAR(result.boundaries[1].t, 3.0 * std::sqrt(2.0), EPS);

    // Line y = x: inside from the corner (0,0) to the reflex corner (1,1),
    // where it enters the notch; it touches the corner (2,2) of the right arm
    ASSERT_EQ(prism.classifyRay(ray(-1, -1, 0.5, 1, 1, 0),
        RayCrossingQuery(-10, 10), result), RaySolidQueryStatus::Supported);
    ASSERT_EQ(result.boundaries.size(), 2);
    EXPECT_NEAR(result.boundaries[0].t, 1.0 * std::sqrt(2.0), EPS);
    EXPECT_NEAR(result.boundaries[1].t, 2.0 * std::sqrt(2.0), EPS);

    // Line touching the convex corner (3,0) only: no interior
    ASSERT_EQ(prism.classifyRay(ray(2, -1, 0.5, 1, 1, 0),
        RayCrossingQuery(-10, 10), result), RaySolidQueryStatus::Supported);
    EXPECT_EQ(result.boundaries.size(), 0);
    ASSERT_EQ(result.insideSegments.size(), 1);
    EXPECT_FALSE(result.insideSegments[0]);
}

TEST(PrismTest, given_prism_when_rayRunsAlongAFace_then_isIndeterminate)
{
    Prism prism(square(1.0), 2.0);
    RaySolidClassification result;
    // On the wall x = 1
    EXPECT_EQ(prism.classifyRay(ray(1, -5, 1, 0, 1, 0),
        RayCrossingQuery(0, 10), result), RaySolidQueryStatus::Indeterminate);
    // On the top cap plane, over the base
    EXPECT_EQ(prism.classifyRay(ray(-5, 0, 2, 1, 0, 0),
        RayCrossingQuery(0, 10), result), RaySolidQueryStatus::Indeterminate);
    // On the plane of the top cap, but away from the base: a miss
    EXPECT_EQ(prism.classifyRay(ray(-5, 3, 2, 1, 0, 0),
        RayCrossingQuery(0, 10), result), RaySolidQueryStatus::Supported);
    EXPECT_EQ(result.boundaries.size(), 0);
}

TEST(PrismTest, given_prism_when_verticalRays_then_capsOrNothingAreHit)
{
    Prism prism(uShape(), 1.0);
    RayHit hit(RayHit::DETAIL_ALL, true);

    ASSERT_EQ(prism.queryFirstHit(ray(0.5, 1.5, 5, 0, 0, -1),
        RayCrossingQuery(0, 10), &hit), RayFirstHitStatus::Hit);
    EXPECT_NEAR(hit.getHitDistance(), 4.0, EPS);
    EXPECT_NEAR(hit.normal.z(), 1.0, EPS);
    // Over the notch
    EXPECT_EQ(prism.queryFirstHit(ray(1.5, 1.5, 5, 0, 0, -1),
        RayCrossingQuery(0, 10), &hit), RayFirstHitStatus::Miss);
    // From below, the bottom cap
    ASSERT_EQ(prism.queryFirstHit(ray(2.5, 0.5, -1, 0, 0, 1),
        RayCrossingQuery(0, 10), &hit), RayFirstHitStatus::Hit);
    EXPECT_NEAR(hit.getHitDistance(), 1.0, EPS);
    EXPECT_NEAR(hit.normal.z(), -1.0, EPS);
}

TEST(PrismTest, given_clockwiseBase_when_constructed_then_isStoredCounterclockwiseWithOutwardWalls)
{
    java::ArrayList<Vector2Dd> clockwise;
    java::ArrayList<Vector2Dd> counterclockwise = square(1.0);
    for ( long i = counterclockwise.size() - 1; i >= 0; i-- ) {
        clockwise.add(counterclockwise[i]);
    }
    Prism prism(clockwise, 1.0);
    ASSERT_TRUE(prism.hasValidParameters());

    RayHit hit(RayHit::DETAIL_ALL, true);
    ASSERT_TRUE(prism.doIntersectionFirstHit(ray(5, 0, 0.5, -1, 0, 0), &hit));
    EXPECT_NEAR(hit.getHitDistance(), 4.0, EPS);
    EXPECT_NEAR(hit.normal.x(), 1.0, EPS);
}

TEST(PrismTest, given_invalidBases_when_constructed_then_haveNoValidParameters)
{
    java::ArrayList<Vector2Dd> bowtie;
    bowtie.add(Vector2Dd(0, 0));
    bowtie.add(Vector2Dd(1, 1));
    bowtie.add(Vector2Dd(1, 0));
    bowtie.add(Vector2Dd(0, 1));
    EXPECT_FALSE(Prism(bowtie, 1.0).hasValidParameters());

    java::ArrayList<Vector2Dd> collinear;
    collinear.add(Vector2Dd(0, 0));
    collinear.add(Vector2Dd(1, 0));
    collinear.add(Vector2Dd(2, 0));
    EXPECT_FALSE(Prism(collinear, 1.0).hasValidParameters());

    EXPECT_FALSE(Prism(square(1.0), 0.0).hasValidParameters());
    RayHit hit(RayHit::DETAIL_ALL, true);
    EXPECT_EQ(Prism(bowtie, 1.0).queryFirstHit(ray(-5, 0.5, 0.5, 1, 0, 0),
        RayCrossingQuery(0, 10), &hit), RayFirstHitStatus::InvalidInput);
}

TEST(PrismTest, given_concavePrism_when_pointsClassified_then_notchAndCapsAreRespected)
{
    Prism prism(uShape(), 1.0);
    const double tol = 0.01;
    EXPECT_EQ(prism.doContainmentTest(Vector3Dd(0.5, 1.5, 0.5), tol), Geometry::INSIDE);
    EXPECT_EQ(prism.doContainmentTest(Vector3Dd(1.5, 1.5, 0.5), tol), Geometry::OUTSIDE);
    EXPECT_EQ(prism.doContainmentTest(Vector3Dd(1.0, 1.5, 0.5), tol), Geometry::LIMIT);
    EXPECT_EQ(prism.doContainmentTest(Vector3Dd(2.5, 0.5, 1.0), tol), Geometry::LIMIT);
    EXPECT_EQ(prism.doContainmentTest(Vector3Dd(2.5, 0.5, 1.5), tol), Geometry::OUTSIDE);
    EXPECT_EQ(prism.classifyPoint(Vector3Dd(2.5, 0.5, 0.5), tol), GeometryContainment::Inside);

    GeometryBounds bounds;
    prism.getBounds(bounds);
    ASSERT_EQ(bounds.getKind(), GeometryBounds::Kind::Finite);
    EXPECT_EQ(bounds.getMinimum().x(), 0.0);
    EXPECT_EQ(bounds.getMaximum().x(), 3.0);
    EXPECT_EQ(bounds.getMaximum().y(), 2.0);
    EXPECT_EQ(bounds.getMaximum().z(), 1.0);
}

TEST(PrismTest, given_prismsAsCsgOperands_when_rothEvaluated_then_differenceHasTheNotch)
{
    // A square slab minus a smaller square prism through it
    CsgBody body(CsgOperation::DIFFERENCE, CsgBodyEvaluator::ROTH);
    SimpleBody* outer = new SimpleBody();
    outer->setGeometry(new Prism(square(2.0), 1.0));
    SimpleBody* inner = new SimpleBody();
    inner->setGeometry(new Prism(4, 1.0, 3.0));
    inner->setPosition(Vector3Dd(0, 0, -1));
    ASSERT_TRUE(body.addLeaf(0, outer));
    ASSERT_TRUE(body.addLeaf(0, inner));

    // Along x at z = 0.5, y = 0: the inner prism is a diamond of
    // circumradius 1, so the solid is [-2, -1] U [1, 2]
    RaySolidClassification result;
    ASSERT_EQ(body.classifyRay(ray(-5, 0, 0.5, 1, 0, 0),
        RayCrossingQuery(0, 20), result), RaySolidQueryStatus::Supported);
    ASSERT_EQ(result.boundaries.size(), 4);
    EXPECT_NEAR(result.boundaries[0].t, 3.0, EPS);
    EXPECT_NEAR(result.boundaries[1].t, 4.0, EPS);
    EXPECT_NEAR(result.boundaries[2].t, 6.0, EPS);
    EXPECT_NEAR(result.boundaries[3].t, 7.0, EPS);

    // From the hole, the first hit is the diamond wall, facing the hole
    RayHit hit(RayHit::DETAIL_POINT | RayHit::DETAIL_NORMAL, false);
    ASSERT_TRUE(body.doIntersectionFirstHit(ray(0, 0, 0.5, 1, 0, 0), &hit));
    EXPECT_NEAR(hit.getHitDistance(), 1.0, EPS);
    EXPECT_LT(hit.normal.x(), 0.0);
}

TEST(PrismTest, given_starPrism_when_exportedToBrep_then_isValidSolid)
{
    int points = 5;
    Prism prism(star(points, 2.0, 0.8), 1.5);
    PolyhedralBoundedSolid* solid = prism.exportToPolyhedralBoundedSolid();
    int n = 2 * points;

    PolyhedralBoundedSolidTopologySummary topology =
        PolyhedralBoundedSolidTopologySummary::from(solid);
    EXPECT_EQ(topology.getVertexCount(), 2 * n);
    EXPECT_EQ(topology.getEdgeCount(), 3 * n);
    EXPECT_EQ(topology.getFaceCount(), n + 2);
    EXPECT_EQ(topology.getShellCount(), 1);
    java::String msg;
    EXPECT_TRUE(PolyhedralBoundedSolidValidationEngine::validateStrict(solid,
        &msg)) << msg.c_str();
    delete solid;
}

TEST(PrismTest, given_concaveBase_when_triangulated_then_trianglesCoverTheBase)
{
    Prism prism(star(6, 2.0, 0.7), 1.0);
    java::ArrayList<int> triangles;
    ASSERT_EQ(prism.triangulateBase(triangles), 10);
    ASSERT_EQ(triangles.size(), 30);

    double polygonArea = 0;
    const java::ArrayList<Vector2Dd>& base = prism.getBase();
    for ( long i = 0; i < base.size(); i++ ) {
        const Vector2Dd& a = base[i];
        const Vector2Dd& b = base[(i + 1) % base.size()];
        polygonArea += 0.5 * (a.x * b.y - b.x * a.y);
    }
    double trianglesArea = 0;
    for ( long i = 0; i < triangles.size(); i += 3 ) {
        const Vector2Dd& a = base[triangles[i]];
        const Vector2Dd& b = base[triangles[i + 1]];
        const Vector2Dd& c = base[triangles[i + 2]];
        double area = 0.5 * ((b.x - a.x) * (c.y - a.y) - (c.x - a.x) * (b.y - a.y));
        EXPECT_GT(area, 0.0);
        trianglesArea += area;
    }
    EXPECT_NEAR(trianglesArea, polygonArea, 1e-12);
}

TEST(PrismTest, given_prism_when_editedThroughControlAccessors_then_parametersChange)
{
    Prism prism(6, 1.0, 2.0);
    const java::ArrayList<java::String>& specifications =
        prism.getControlSpecifications();
    ASSERT_EQ(specifications.size(), 3);

    const EntityControlAccessor* sides = prism.getControlAccessor("numberOfSides");
    const EntityControlAccessor* radius = prism.getControlAccessor("circumradius");
    const EntityControlAccessor* height = prism.getControlAccessor("height");
    ASSERT_NE(sides, nullptr);
    ASSERT_NE(radius, nullptr);
    ASSERT_NE(height, nullptr);
    EXPECT_STREQ(sides->getType(), "int");

    java::String text;
    ASSERT_TRUE(sides->readValue(prism, text));
    EXPECT_EQ(text, java::String("6"));
    ASSERT_TRUE(sides->writeValue(prism, "8"));
    EXPECT_EQ(prism.getNumberOfSides(), 8);
    EXPECT_NEAR(prism.getCircumradius(), 1.0, EPS);
    ASSERT_TRUE(radius->writeValue(prism, "2.5"));
    EXPECT_NEAR(prism.getCircumradius(), 2.5, EPS);
    EXPECT_EQ(prism.getNumberOfSides(), 8);
    ASSERT_TRUE(height->writeValue(prism, "4"));
    EXPECT_EQ(prism.getHeight(), 4.0);

    Geometry* copy = prism.copyGeometry();
    ASSERT_NE(copy, nullptr);
    EXPECT_EQ(dynamic_cast<Prism*>(copy)->getNumberOfSides(), 8);
    delete copy;
}
