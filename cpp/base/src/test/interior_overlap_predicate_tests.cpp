#include <cfloat>
#include <cmath>
#include <gtest/gtest.h>
#include "java/util/ArrayList.txx"
#include "java/util/Random.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidSetNonIntersectingClassifier.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "PrismFixtures.h"

/*
"Do the interiors of A and B overlap?" (plan-stage11 Phase D). The
preflights of the set operator and the "result cannot be empty" guard of
`setOp` rest on this decision. It used to be taken by sampling: 27 probes
in the common bounding box and then a grid built from the vertex
coordinates. A thin overlap that falls between the samples was missed, and
more easily when the pair is not aligned with the axes.

Every pair here is built from boxes, so the truth is known: the
interiors overlap exactly when the boxes overlap with positive volume
before the rigid motion applied to both. The pairs are the hard ones for a
sampling test: overlaps as thin as 10^-3 of the size, with coplanar faces,
so that no edge of one crosses a face of the other in its interior.

The decision has a band: an overlap thinner than about 5 tolerances is
a contact for the kernel (the tolerance is 10^-5 of the size of the pair).
The last test pins that down.
C++ counterpart of Java's `InteriorOverlapPredicateTest`. A null Java
motion is a null pointer.
*/
namespace {

typedef _PolyhedralBoundedSolidSetNonIntersectingClassifier Classifier;
typedef PolyhedralBoundedSolidVolumeOracle Oracle;

class Box6
{
public:
    double v[6];
    Box6() { for ( int i = 0; i < 6; i++ ) v[i] = 0; }
    Box6(double x0, double y0, double z0, double x1, double y1, double z1)
    {
        v[0] = x0; v[1] = y0; v[2] = z0; v[3] = x1; v[4] = y1; v[5] = z1;
    }
    double operator[](int i) const { return v[i]; }
};

/** One pair: two boxes given by their corners, and the truth. */
class Case
{
public:
    java::String name;
    Box6 boxA;
    Box6 boxB;
    bool overlap;

    Case() : overlap(false) {}
    Case(const java::String& name, const Box6& a, const Box6& b,
        bool overlap) : name(name), boxA(a), boxB(b), overlap(overlap) {}
    bool operator==(const Case& other) const { return this == &other; }
};

java::ArrayList<Case> cases()
{
    java::ArrayList<Case> list;
    Box6 unit(0, 0, 0, 1, 1, 1);
    const double thicknesses[] = {0.5, 0.1, 1.0e-2, 1.0e-3};
    for ( double t : thicknesses ) {
        java::String ts = java::String::valueOf(t);
        // Same cross-section, shifted along x: only the faces across x
        // differ, every other face is coplanar with one of the other.
        list.add(Case(java::String("shifted, overlap ") + ts, unit,
            Box6(1 - t, 0, 0, 2 - t, 1, 1), true));
        // Coplanar top and bottom, overlapping in a thin corner column.
        list.add(Case(java::String("corner column ") + ts, unit,
            Box6(1 - t, 1 - t, 0, 2, 2, 1), true));
        // A thin plate inside A against its top face.
        list.add(Case(java::String("inner plate ") + ts, unit,
            Box6(0.2, 0.2, 1 - t, 0.8, 0.8, 1), true));
        // A thin plate through A, flush with two of its faces.
        list.add(Case(java::String("through plate ") + ts, unit,
            Box6(0, 0.5, 0, 1, 0.5 + t, 1), true));
        // B contains A and shares five of its faces.
        list.add(Case(java::String("cover, margin ") + ts, unit,
            Box6(0, 0, 0, 1 + t, 1, 1), true));
    }
    list.add(Case("identical", unit, unit, true));
    list.add(Case("face contact", unit, Box6(1, 0, 0, 2, 1, 1), false));
    list.add(Case("partial face contact", unit,
        Box6(1, 0.3, 0.2, 2, 1.4, 0.7), false));
    list.add(Case("stacked, partial", unit,
        Box6(0.4, 0.3, 1, 1.4, 1.3, 1.6), false));
    list.add(Case("edge contact", unit,
        Box6(1, 0.1, 1, 1.9, 0.9, 1.8), false));
    list.add(Case("vertex contact", unit, Box6(1, 1, 1, 2, 2, 2), false));
    list.add(Case("apart", unit, Box6(1.5, 0, 0, 2.5, 1, 1), false));
    list.add(Case("thin plate on top, outside", unit,
        Box6(0.2, 0.2, 1, 0.8, 0.8, 1.001), false));
    return list;
}

PolyhedralBoundedSolid* solid(const Box6& box, const Matrix4x4d* motion)
{
    Matrix4x4d place = Oracle::translation(
        (box[0] + box[3]) / 2.0, (box[1] + box[4]) / 2.0,
        (box[2] + box[5]) / 2.0);
    return Oracle::OrientedBox(box[3] - box[0], box[4] - box[1],
        box[5] - box[2], motion == nullptr ? place : motion->multiply(place))
        .toSolid();
}

java::ArrayList<java::String> wrongAnswers(const Matrix4x4d* motion)
{
    java::ArrayList<java::String> wrong;
    java::ArrayList<Case> all = cases();
    for ( long i = 0; i < all.size(); i++ ) {
        const Case& c = all[i];
        for ( int order = 0; order < 2; order++ ) {
            PolyhedralBoundedSolid* a = solid(c.boxA, motion);
            PolyhedralBoundedSolid* b = solid(c.boxB, motion);
            bool answer = order == 0 ?
                Classifier::interiorsOverlap(a, b) :
                Classifier::interiorsOverlap(b, a);
            if ( answer != c.overlap ) {
                wrong.add(c.name + (order == 0 ? " (A,B)" : " (B,A)") +
                    " answered " + (answer ? "true" : "false"));
            }
            delete a;
            delete b;
        }
    }
    return wrong;
}

java::String join(const java::ArrayList<java::String>& list)
{
    java::String text("[");
    for ( long i = 0; i < list.size(); i++ ) {
        if ( i > 0 ) {
            text += ", ";
        }
        text += list[i];
    }
    return text + "]";
}

PrismPolygon turned(const PrismPolygon& xy, double angle, double dx,
    double dy)
{
    PrismPolygon out;
    double c = std::cos(angle);
    double s = std::sin(angle);
    for ( long i = 0; i < xy.size(); i++ ) {
        out.add(PrismPoint(c * xy[i][0] - s * xy[i][1] + dx,
            s * xy[i][0] + c * xy[i][1] + dy));
    }
    return out;
}

/** Farthest reach of a polygon along a direction. */
double support(const PrismPolygon& xy, double direction)
{
    double best = -DBL_MAX;
    for ( long i = 0; i < xy.size(); i++ ) {
        best = std::fmax(best, xy[i][0] * std::cos(direction) +
            xy[i][1] * std::sin(direction));
    }
    return best;
}

double side(const PrismPoint& from, const PrismPoint& to,
    const PrismPoint& p)
{
    return (to[0] - from[0]) * (p[1] - from[1]) -
        (to[1] - from[1]) * (p[0] - from[0]);
}

/** Area of the intersection of two convex polygons (Sutherland-Hodgman). */
double intersectionArea(const PrismPolygon& subject, const PrismPolygon& clip)
{
    PrismPolygon polygon = subject;
    for ( long i = 0; i < clip.size() && !polygon.isEmpty(); i++ ) {
        const PrismPoint& c0 = clip[i];
        const PrismPoint& c1 = clip[(i + 1) % clip.size()];
        PrismPolygon next;
        for ( long j = 0; j < polygon.size(); j++ ) {
            const PrismPoint& p = polygon[j];
            const PrismPoint& q = polygon[(j + 1) % polygon.size()];
            double sp = side(c0, c1, p);
            double sq = side(c0, c1, q);
            if ( sp >= 0 ) {
                next.add(p);
            }
            if ( (sp > 0 && sq < 0) || (sp < 0 && sq > 0) ) {
                double t = sp / (sp - sq);
                next.add(PrismPoint(p[0] + t * (q[0] - p[0]),
                    p[1] + t * (q[1] - p[1])));
            }
        }
        polygon = next;
    }
    if ( polygon.size() < 3 ) {
        return 0.0;
    }
    return std::fabs(PrismFixtures::polygonArea(polygon));
}

}

TEST(InteriorOverlapPredicateTest, AxisAlignedPairsInteriorsOverlapAnswerIsExact) {
    java::ArrayList<java::String> wrong = wrongAnswers(nullptr);
    EXPECT_TRUE(wrong.isEmpty()) << join(wrong).c_str();
}

TEST(InteriorOverlapPredicateTest, RotatedPairsInteriorsOverlapAnswerIsExact) {
    java::Random random(31);
    java::ArrayList<java::String> wrong;
    for ( int i = 0; i < 12; i++ ) {
        // Java evaluates the arguments left to right
        double tx = random.nextDouble();
        double ty = random.nextDouble();
        double tz = random.nextDouble();
        double angle = 3.0 * random.nextDouble();
        double ax = random.nextGaussian();
        double ay = random.nextGaussian();
        double az = random.nextGaussian();
        Matrix4x4d motion = Oracle::translation(tx, ty, tz)
            .multiply(Oracle::rotation(angle, ax, ay, az));
        java::ArrayList<java::String> w = wrongAnswers(&motion);
        for ( long k = 0; k < w.size(); k++ ) {
            wrong.add(java::String("motion ") + java::String::valueOf(i) +
                ": " + w[k]);
        }
    }
    EXPECT_TRUE(wrong.isEmpty()) << join(wrong).c_str();
}

/*
Prisms over convex polygons with the same bottom and top planes. No
edge of one crosses a face of the other in its interior (the side
edges end on the shared planes), so nothing but the overlap test can
tell a thin overlap from a contact. The truth is the area of the
intersection of the two base polygons.
*/
TEST(InteriorOverlapPredicateTest, PrismsWithCoplanarEndsInteriorsOverlapAnswerIsExact) {
    java::Random random(57);
    java::ArrayList<java::String> wrong;
    const double depths[] = {0.5, 0.05, 5.0e-3, 5.0e-4, -5.0e-4, -5.0e-3,
        -0.05};
    for ( int i = 0; i < 60; i++ ) {
        int sidesA = 3 + random.nextInt(8);
        int sidesB = 3 + random.nextInt(8);
        PrismPolygon baseA = turned(PrismFixtures::regularPolygon(sidesA,
            1.0), 6.3 * random.nextDouble(), 0, 0);
        double direction = 6.3 * random.nextDouble();
        // Distance at which the polygons would overlap by `depth` if
        // they were discs: thin overlaps, exact contact never.
        double depth = depths[i % 7];
        PrismPolygon baseB0 = turned(PrismFixtures::regularPolygon(sidesB,
            1.0), 6.3 * random.nextDouble(), 0, 0);
        double reach = support(baseA, direction) +
            support(baseB0, direction + M_PI) - depth;
        PrismPolygon baseB = turned(baseB0, 0,
            reach * std::cos(direction), reach * std::sin(direction));
        bool truth = intersectionArea(baseA, baseB) > 1.0e-12;
        Matrix4x4d motionValue;
        Matrix4x4d* motion = nullptr;
        if ( i % 2 != 0 ) {
            double tx = random.nextDouble();
            double ty = random.nextDouble();
            double tz = random.nextDouble();
            double angle = 3.0 * random.nextDouble();
            double ax = random.nextGaussian();
            double ay = random.nextGaussian();
            double az = random.nextGaussian();
            motionValue = Oracle::translation(tx, ty, tz)
                .multiply(Oracle::rotation(angle, ax, ay, az));
            motion = &motionValue;
        }
        for ( int order = 0; order < 2; order++ ) {
            PolyhedralBoundedSolid* a = PrismFixtures::prism(baseA, 1.0);
            PolyhedralBoundedSolid* b = PrismFixtures::prism(baseB, 1.0);
            if ( motion != nullptr ) {
                PolyhedralBoundedSolidModeler::applyTransformation(a,
                    *motion);
                PolyhedralBoundedSolidModeler::applyTransformation(b,
                    *motion);
            }
            bool answer = order == 0 ?
                Classifier::interiorsOverlap(a, b) :
                Classifier::interiorsOverlap(b, a);
            if ( answer != truth ) {
                wrong.add(java::String("#") + java::String::valueOf(i) +
                    " " + java::String::valueOf(sidesA) + "/" +
                    java::String::valueOf(sidesB) + " sides, depth " +
                    java::String::valueOf(depth) +
                    (motion == nullptr ? "" : " rotated") +
                    (order == 0 ? " (A,B)" : " (B,A)") + " answered " +
                    (answer ? "true" : "false") + ", area " +
                    java::String::valueOf(intersectionArea(baseA, baseB)));
            }
            delete a;
            delete b;
        }
    }
    EXPECT_TRUE(wrong.isEmpty()) << join(wrong).c_str();
}

/*
Inside the tolerance band the pair is a contact: a box that enters
another by half a tolerance does not overlap it, and one that enters by
twenty tolerances does.
*/
TEST(InteriorOverlapPredicateTest, OverlapAroundTheToleranceBandIsAContact) {
    PolyhedralBoundedSolid* p = solid(Box6(0, 0, 0, 1, 1, 1), nullptr);
    PolyhedralBoundedSolid* q = solid(Box6(1, 0, 0, 2, 1, 1), nullptr);
    double tolerance = PolyhedralBoundedSolidNumericPolicy::forSolids(p, q)
        .bigEpsilon();
    delete p;
    delete q;
    PolyhedralBoundedSolid* a = solid(Box6(0, 0, 0, 1, 1, 1), nullptr);
    PolyhedralBoundedSolid* b = solid(
        Box6(1 - 0.5 * tolerance, 0, 0, 2, 1, 1), nullptr);
    EXPECT_FALSE(Classifier::interiorsOverlap(a, b));
    delete a;
    delete b;
    a = solid(Box6(0, 0, 0, 1, 1, 1), nullptr);
    b = solid(Box6(1 - 20 * tolerance, 0, 0, 2, 1, 1), nullptr);
    EXPECT_TRUE(Classifier::interiorsOverlap(a, b));
    delete a;
    delete b;
}
