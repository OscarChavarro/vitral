// C++ port of Java's InteriorOverlapPredicateTest
// (environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators).
#include <math.h>
#include <float.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "PrismFixtures.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "java/util/Random.h"
#include "vsdk/toolkit/common/Cpp98Support.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidSetNonIntersectingClassifier.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.h"

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

typedef PolyhedralBoundedSolidVolumeOracle Oracle;
typedef _PolyhedralBoundedSolidSetNonIntersectingClassifier Classifier;

static String
str(double value)
{
    return Cpp98Support::toString(value);
}

static String
str(long value)
{
    return Cpp98Support::toString(value);
}

static String
booleanText(BOOLEAN value)
{
    if ( value ) {
        return String("true");
    }
    return String("false");
}

/** One pair: two boxes given by their corners, and the truth. */
class InteriorOverlapCase
{
public:
    String name;
    double boxA[6];
    double boxB[6];
    BOOLEAN overlap;

    InteriorOverlapCase() : overlap(FALSE)
    {
        int i;
        for ( i = 0; i < 6; i++ ) {
            boxA[i] = 0.0;
            boxB[i] = 0.0;
        }
    }

    InteriorOverlapCase(const String& inName, const double* a,
        const double* b, BOOLEAN inOverlap) :
        name(inName), overlap(inOverlap)
    {
        int i;
        for ( i = 0; i < 6; i++ ) {
            boxA[i] = a[i];
            boxB[i] = b[i];
        }
    }

    BOOLEAN operator==(const InteriorOverlapCase& other) const
    {
        return this == &other;
    }
};

inline BOOLEAN
vsdkEquals(const InteriorOverlapCase &a, const InteriorOverlapCase &b)
{
    return a == b;
}

static void
addCase(ArrayList<InteriorOverlapCase>& cases, const String& name,
    const double* a, double x0, double y0, double z0, double x1, double y1,
    double z1, BOOLEAN overlap)
{
    double b[6];

    b[0] = x0;
    b[1] = y0;
    b[2] = z0;
    b[3] = x1;
    b[4] = y1;
    b[5] = z1;
    cases.add(InteriorOverlapCase(name, a, b, overlap));
}

static ArrayList<InteriorOverlapCase>
cases()
{
    ArrayList<InteriorOverlapCase> cases;
    static const double unit[6] = {0, 0, 0, 1, 1, 1};
    static const double thicknesses[4] = {0.5, 0.1, 1.0e-2, 1.0e-3};
    int k;

    for ( k = 0; k < 4; k++ ) {
        double t = thicknesses[k];
        // Same cross-section, shifted along x: only the faces across x
        // differ, every other face is coplanar with one of the other.
        addCase(cases, String("shifted, overlap ") + str(t), unit,
            1 - t, 0, 0, 2 - t, 1, 1, TRUE);
        // Coplanar top and bottom, overlapping in a thin corner column.
        addCase(cases, String("corner column ") + str(t), unit,
            1 - t, 1 - t, 0, 2, 2, 1, TRUE);
        // A thin plate inside A against its top face.
        addCase(cases, String("inner plate ") + str(t), unit,
            0.2, 0.2, 1 - t, 0.8, 0.8, 1, TRUE);
        // A thin plate through A, flush with two of its faces.
        addCase(cases, String("through plate ") + str(t), unit,
            0, 0.5, 0, 1, 0.5 + t, 1, TRUE);
        // B contains A and shares five of its faces.
        addCase(cases, String("cover, margin ") + str(t), unit,
            0, 0, 0, 1 + t, 1, 1, TRUE);
    }
    addCase(cases, "identical", unit, 0, 0, 0, 1, 1, 1, TRUE);
    addCase(cases, "face contact", unit, 1, 0, 0, 2, 1, 1, FALSE);
    addCase(cases, "partial face contact", unit,
        1, 0.3, 0.2, 2, 1.4, 0.7, FALSE);
    addCase(cases, "stacked, partial", unit,
        0.4, 0.3, 1, 1.4, 1.3, 1.6, FALSE);
    addCase(cases, "edge contact", unit, 1, 0.1, 1, 1.9, 0.9, 1.8, FALSE);
    addCase(cases, "vertex contact", unit, 1, 1, 1, 2, 2, 2, FALSE);
    addCase(cases, "apart", unit, 1.5, 0, 0, 2.5, 1, 1, FALSE);
    addCase(cases, "thin plate on top, outside", unit,
        0.2, 0.2, 1, 0.8, 0.8, 1.001, FALSE);
    return cases;
}

/** C++ port note: Java's null motion is `motion == NULL`. */
static PolyhedralBoundedSolid*
solid(const double* box, const Matrix4x4d* motion)
{
    Matrix4x4d place = Oracle::translation(
        (box[0] + box[3]) / 2.0, (box[1] + box[4]) / 2.0,
        (box[2] + box[5]) / 2.0);
    Matrix4x4d transform = place;

    if ( motion != NULL ) {
        transform = motion->multiply(place);
    }
    return Oracle::OrientedBox(box[3] - box[0], box[4] - box[1],
        box[5] - box[2], transform).toSolid();
}

static ArrayList<String>
wrongAnswers(const Matrix4x4d* motion)
{
    ArrayList<String> wrong;
    ArrayList<InteriorOverlapCase> all = cases();
    long i;

    for ( i = 0; i < all.size(); i++ ) {
        const InteriorOverlapCase& c = all[i];
        int order;
        for ( order = 0; order < 2; order++ ) {
            PolyhedralBoundedSolid* a = solid(c.boxA, motion);
            PolyhedralBoundedSolid* b = solid(c.boxB, motion);
            BOOLEAN answer;
            if ( order == 0 ) {
                answer = Classifier::interiorsOverlap(a, b);
            }
            else {
                answer = Classifier::interiorsOverlap(b, a);
            }
            if ( answer != c.overlap ) {
                String orderText = " (B,A)";
                if ( order == 0 ) {
                    orderText = " (A,B)";
                }
                wrong.add(c.name + orderText + " answered " +
                    booleanText(answer));
            }
            delete a;
            delete b;
        }
    }
    return wrong;
}

static void
printAll(const ArrayList<String>& lines)
{
    long i;

    for ( i = 0; i < lines.size(); i++ ) {
        printf("%s\n", lines[i].c_str());
    }
}

/**
Java `translation(nextDouble x3).multiply(rotation(3 nextDouble,
nextGaussian x3))`, with the draws in Java's left-to-right order.
*/
static Matrix4x4d
randomMotion(Random& random)
{
    double tx = random.nextDouble();
    double ty = random.nextDouble();
    double tz = random.nextDouble();
    double angle = 3.0 * random.nextDouble();
    double ax = random.nextGaussian();
    double ay = random.nextGaussian();
    double az = random.nextGaussian();

    return Oracle::translation(tx, ty, tz).multiply(
        Oracle::rotation(angle, ax, ay, az));
}

TEST(InteriorOverlapPredicate, given_axisAlignedPairs_when_interiorsOverlapIsAsked_then_answerIsExact)
{
    ArrayList<String> wrong = wrongAnswers(NULL);

    printAll(wrong);
    EXPECT_TRUE(wrong.isEmpty());
}

TEST(InteriorOverlapPredicate, given_rotatedPairs_when_interiorsOverlapIsAsked_then_answerIsExact)
{
    Random random(31);
    ArrayList<String> wrong;
    long i;

    for ( i = 0; i < 12; i++ ) {
        Matrix4x4d motion = randomMotion(random);
        ArrayList<String> w = wrongAnswers(&motion);
        long j;
        for ( j = 0; j < w.size(); j++ ) {
            wrong.add(String("motion ") + str(i) + ": " + w[j]);
        }
    }
    printAll(wrong);
    EXPECT_TRUE(wrong.isEmpty());
}

static PrismPolygon
turned(const PrismPolygon& xy, double angle, double dx, double dy)
{
    PrismPolygon out;
    double c = cos(angle);
    double s = sin(angle);
    long i;

    for ( i = 0; i < xy.size(); i++ ) {
        out.add(Vector2Dd(c * xy[i].x - s * xy[i].y + dx,
            s * xy[i].x + c * xy[i].y + dy));
    }
    return out;
}

/** Farthest reach of a polygon along a direction. */
static double
support(const PrismPolygon& xy, double direction)
{
    double best = -DBL_MAX;
    long i;

    for ( i = 0; i < xy.size(); i++ ) {
        double value = xy[i].x * cos(direction) + xy[i].y * sin(direction);
        if ( value > best ) {
            best = value;
        }
    }
    return best;
}

static double
side(const Vector2Dd& from, const Vector2Dd& to, const Vector2Dd& p)
{
    return (to.x - from.x) * (p.y - from.y) -
        (to.y - from.y) * (p.x - from.x);
}

/** Area of the intersection of two convex polygons (Sutherland-Hodgman). */
static double
intersectionArea(const PrismPolygon& subject, const PrismPolygon& clip)
{
    PrismPolygon polygon = subject;
    long i;

    for ( i = 0; i < clip.size() && !polygon.isEmpty(); i++ ) {
        Vector2Dd c0 = clip[i];
        Vector2Dd c1 = clip[(i + 1) % clip.size()];
        PrismPolygon next;
        long j;
        for ( j = 0; j < polygon.size(); j++ ) {
            Vector2Dd p = polygon[j];
            Vector2Dd q = polygon[(j + 1) % polygon.size()];
            double sp = side(c0, c1, p);
            double sq = side(c0, c1, q);
            if ( sp >= 0 ) {
                next.add(p);
            }
            if ( (sp > 0 && sq < 0) || (sp < 0 && sq > 0) ) {
                double t = sp / (sp - sq);
                next.add(Vector2Dd(p.x + t * (q.x - p.x),
                    p.y + t * (q.y - p.y)));
            }
        }
        polygon = next;
    }
    if ( polygon.size() < 3 ) {
        return 0.0;
    }
    return fabs(PrismFixtures::polygonArea(polygon));
}

/**
Prisms over convex polygons with the same bottom and top planes. No
edge of one crosses a face of the other in its interior (the side
edges end on the shared planes), so nothing but the overlap test can
tell a thin overlap from a contact. The truth is the area of the
intersection of the two base polygons.
*/
TEST(InteriorOverlapPredicate, given_prismsWithCoplanarEnds_when_interiorsOverlapIsAsked_then_answerIsExact)
{
    static const double depths[7] = {0.5, 0.05, 5.0e-3, 5.0e-4,
        -5.0e-4, -5.0e-3, -0.05};
    Random random(57);
    ArrayList<String> wrong;
    long i;

    for ( i = 0; i < 60; i++ ) {
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
            reach * cos(direction), reach * sin(direction));
        BOOLEAN truth = intersectionArea(baseA, baseB) > 1.0e-12;
        BOOLEAN hasMotion = i % 2 != 0;
        Matrix4x4d motion;
        if ( hasMotion ) {
            motion = randomMotion(random);
        }
        int order;
        for ( order = 0; order < 2; order++ ) {
            PolyhedralBoundedSolid* a = PrismFixtures::prism(baseA, 1.0);
            PolyhedralBoundedSolid* b = PrismFixtures::prism(baseB, 1.0);
            BOOLEAN answer;
            if ( hasMotion ) {
                PolyhedralBoundedSolidModeler::applyTransformation(a,
                    motion);
                PolyhedralBoundedSolidModeler::applyTransformation(b,
                    motion);
            }
            if ( order == 0 ) {
                answer = Classifier::interiorsOverlap(a, b);
            }
            else {
                answer = Classifier::interiorsOverlap(b, a);
            }
            if ( answer != truth ) {
                String text = String("#") + str(i) + " " +
                    str((long)sidesA) + "/" + str((long)sidesB) +
                    " sides, depth " + str(depth);
                if ( hasMotion ) {
                    text = text + " rotated";
                }
                if ( order == 0 ) {
                    text = text + " (A,B)";
                }
                else {
                    text = text + " (B,A)";
                }
                wrong.add(text + " answered " + booleanText(answer) +
                    ", area " + str(intersectionArea(baseA, baseB)));
            }
            delete a;
            delete b;
        }
    }
    printAll(wrong);
    EXPECT_TRUE(wrong.isEmpty());
}

/**
Inside the tolerance band the pair is a contact: a box that enters
another by half a tolerance does not overlap it, and one that enters by
twenty tolerances does.
*/
TEST(InteriorOverlapPredicate, given_overlapAroundTheTolerance_when_interiorsOverlapIsAsked_then_bandIsAContact)
{
    static const double unit[6] = {0, 0, 0, 1, 1, 1};
    static const double right[6] = {1, 0, 0, 2, 1, 1};
    PolyhedralBoundedSolid* a = solid(unit, NULL);
    PolyhedralBoundedSolid* b = solid(right, NULL);
    double tolerance = PolyhedralBoundedSolidNumericPolicy::forSolids(a, b)
        .bigEpsilon();
    double half[6] = {1, 0, 0, 2, 1, 1};
    double twenty[6] = {1, 0, 0, 2, 1, 1};
    PolyhedralBoundedSolid* c;
    PolyhedralBoundedSolid* d;

    delete a;
    delete b;
    half[0] = 1 - 0.5 * tolerance;
    twenty[0] = 1 - 20 * tolerance;

    c = solid(unit, NULL);
    d = solid(half, NULL);
    EXPECT_FALSE(Classifier::interiorsOverlap(c, d));
    delete c;
    delete d;
    c = solid(unit, NULL);
    d = solid(twenty, NULL);
    EXPECT_TRUE(Classifier::interiorsOverlap(c, d));
    delete c;
    delete d;
}

VITRAL_TEST_MAIN()
