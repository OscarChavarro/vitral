#include <cstdio>
#include <gtest/gtest.h>
#include "java/util/ArrayList.txx"
#include "java/util/Random.h"
#include "java/util/TreeMap.txx"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "BooleanCompositeOracleFuzz.h"
#include "CompositeShapeOracle.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "PrismFixtures.h"
#include "SplitterOracleFuzzCheck.h"

/*
Seeded oracle fuzz for the splitter with planes in general position
(plan-stage11 Phase B). SplitterOracleFuzzTest cuts the reference corpus
with planes through its vertices; this one cuts solids the corpus does not
have, with planes through random interior points:
- star prisms (N reflex edges crossed at once);
- regular prisms of many sides;
- L, U, stepped and frame shapes (the frame has a through hole, genus 1),
  built with unions and differences of boxes and rotated.
The check is the one of SplitterOracleFuzzTest: the pieces conserve the
volume, each lies on its side, each is strict-valid and none is empty.
C++ counterpart of Java's `SplitterGeneralPlaneOracleFuzzTest`.
*/
namespace {

/** First measurement (2026-11-16): 240 of 240. */
const int MINIMUM_CORRECT = 240;

/** A value in the middle 80 % of [lo, hi]. */
double between(java::Random& random, double lo, double hi)
{
    return lo + (0.1 + 0.8 * random.nextDouble()) * (hi - lo);
}

java::String byReasonText(const java::TreeMap<java::String, int>& byReason)
{
    java::String text("{");
    for ( long i = 0; i < byReason.size(); i++ ) {
        if ( i > 0 ) {
            text += ", ";
        }
        text += byReason.getKey(i) + "=" +
            java::String::valueOf(byReason.getValue(i));
    }
    return text + "}";
}

}

TEST(SplitterGeneralPlaneOracleFuzzTest, NonConvexSolidsAndGeneralPlanesSplitCorrectCountDoesNotRegress) {
    java::Random random(77);
    java::ArrayList<java::String> failures;
    java::TreeMap<java::String, int> byReason;
    int total = 0;
    int correct = 0;

    for ( int s = 0; s < 30; s++ ) {
        java::String name;
        PolyhedralBoundedSolid* base;
        // Java evaluates the arguments left to right
        double tx = random.nextDouble();
        double ty = random.nextDouble();
        double tz = random.nextDouble();
        double angle = 3.0 * random.nextDouble();
        double ax = random.nextGaussian();
        double ay = random.nextGaussian();
        double az = random.nextGaussian();
        Matrix4x4d motion = PolyhedralBoundedSolidVolumeOracle
            ::translation(tx, ty, tz)
            .multiply(PolyhedralBoundedSolidVolumeOracle::rotation(
                angle, ax, ay, az));
        if ( s % 3 == 0 ) {
            int points = 4 + random.nextInt(4);
            name = java::String("STAR") + java::String::valueOf(points);
            base = PrismFixtures::prism(PrismFixtures::starPolygon(points,
                1.0, 0.4 + 0.1 * random.nextDouble()), 1.0);
            PolyhedralBoundedSolidModeler::applyTransformation(base, motion);
        }
        else if ( s % 3 == 1 ) {
            int sides = 5 + random.nextInt(28);
            name = java::String("PRISM") + java::String::valueOf(sides);
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
        for ( int k = 0; k < 8; k++ ) {
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
            java::String reason = splitterOracleCheck(base,
                InfinitePlane(n.multiply(1.0 / n.length()), p));
            if ( reason.length() == 0 ) {
                correct++;
            }
            else {
                byReason.put(reason, byReason.getOrDefault(reason, 0) + 1);
                failures.add(name + " s=" + java::String::valueOf(s) +
                    " k=" + java::String::valueOf(k) + " " + reason);
            }
        }
        delete[] minMax;
        delete base;
    }
    printf("general-plane split fuzz: %d / %d %s\n", correct, total,
        byReasonText(byReason).c_str());
    for ( long i = 0; i < failures.size(); i++ ) {
        printf("  %s\n", failures[i].c_str());
    }
    EXPECT_GE(correct, MINIMUM_CORRECT)
        << "correct split results must not regress";
}
