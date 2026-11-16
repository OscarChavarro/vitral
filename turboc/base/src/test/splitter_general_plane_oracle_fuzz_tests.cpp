// C++ port of Java's SplitterGeneralPlaneOracleFuzzTest
// (environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators).
#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "BooleanCompositeOracleFuzz.h"
#include "CompositeShapeOracle.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "PrismFixtures.h"
#include "SplitterOracleFuzz.h"
#include "java/util/ArrayList.txx"
#include "java/util/Random.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"

typedef PolyhedralBoundedSolidVolumeOracle Oracle;

/** First measurement (2026-11-16): 240 of 240. */
static const int MINIMUM_CORRECT = 240;

/** A value in the middle 80 % of [lo, hi]. */
static double
between(Random& random, double lo, double hi)
{
    return lo + (0.1 + 0.8 * random.nextDouble()) * (hi - lo);
}

/** Java `TreeMap<String, Integer>.merge(reason, 1, Integer::sum)`. */
static void
countReason(ArrayList<String>& reasons, ArrayList<int>& counts,
    const String& reason)
{
    long i;

    for ( i = 0; i < reasons.size(); i++ ) {
        if ( reasons[i] == reason ) {
            counts[i]++;
            return;
        }
    }
    for ( i = 0; i < reasons.size() && reasons[i] < reason;
          i++ ) {
    }
    reasons.add(i, reason);
    counts.add(i, 1);
}

static String
reasonsText(const ArrayList<String>& reasons, const ArrayList<int>& counts)
{
    String text = "{";
    char number[32];
    long i;

    for ( i = 0; i < reasons.size(); i++ ) {
        if ( i > 0 ) {
            text = text + ", ";
        }
        sprintf(number, "%d", counts[i]);
        text = text + reasons[i] + "=" + number;
    }
    return text + "}";
}

TEST(SplitterGeneralPlaneOracleFuzz, given_nonConvexSolidsAndGeneralPlanes_when_split_then_correctCountDoesNotRegress)
{
    Random random(77);
    ArrayList<String> failures;
    ArrayList<String> reasons;
    ArrayList<int> counts;
    int total = 0;
    int correct = 0;
    int s;
    long f;

    for ( s = 0; s < 30; s++ ) {
        String name;
        PolyhedralBoundedSolid* base;
        // Java evaluates the arguments from left to right
        double tx = random.nextDouble();
        double ty = random.nextDouble();
        double tz = random.nextDouble();
        double angle = 3.0 * random.nextDouble();
        double ax = random.nextGaussian();
        double ay = random.nextGaussian();
        double az = random.nextGaussian();
        Matrix4x4d motion = Oracle::translation(tx, ty, tz)
            .multiply(Oracle::rotation(angle, ax, ay, az));
        char number[32];
        if ( s % 3 == 0 ) {
            int points = 4 + random.nextInt(4);
            sprintf(number, "%d", points);
            name = String("STAR") + number;
            double inner = 0.4 + 0.1 * random.nextDouble();
            base = PrismFixtures::prism(PrismFixtures::starPolygon(points,
                1.0, inner), 1.0);
            PolyhedralBoundedSolidModeler::applyTransformation(base, motion);
        }
        else if ( s % 3 == 1 ) {
            int sides = 5 + random.nextInt(28);
            sprintf(number, "%d", sides);
            name = String("PRISM") + number;
            base = PrismFixtures::prism(
                PrismFixtures::regularPolygon(sides, 1.0), 1.0);
            PolyhedralBoundedSolidModeler::applyTransformation(base, motion);
        }
        else {
            CompositeShapeOracle::Shape shape = BooleanCompositeOracleFuzz
                ::randomShape(random, 1.0).moved(motion);
            name = shape.name;
            base = shape.toSolid();
        }
        double* minMax = base->getMinMax();
        int k;
        for ( k = 0; k < 8; k++ ) {
            double nx = random.nextGaussian();
            double ny = random.nextGaussian();
            double nz = random.nextGaussian();
            Vector3Dd n(nx, ny, nz);
            double px = between(random, minMax[0], minMax[3]);
            double py = between(random, minMax[1], minMax[4]);
            double pz = between(random, minMax[2], minMax[5]);
            Vector3Dd p(px, py, pz);
            if ( n.length() < 1.0e-3 ) {
                continue;
            }
            total++;
            String reason = SplitterOracleFuzz::check(base,
                InfinitePlane(n.multiply(1.0 / n.length()), p));
            if ( reason.length() == 0 ) {
                correct++;
            }
            else {
                char text[64];
                sprintf(text, " s=%d k=%d ", s, k);
                countReason(reasons, counts, reason);
                failures.add(name + text + reason);
            }
        }
        delete[] minMax;
        delete base;
    }

    printf("general-plane split fuzz: %d / %d %s\n", correct, total,
        reasonsText(reasons, counts).c_str());
    for ( f = 0; f < failures.size(); f++ ) {
        printf("  %s\n", failures[f].c_str());
    }
    // correct split results must not regress
    EXPECT_GE(correct, MINIMUM_CORRECT);
}

VITRAL_TEST_MAIN()
