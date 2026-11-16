#include <gtest/gtest.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/NonManifoldBooleanResultException.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.h"
#include "CsgSampleCorpusFixtures.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"

/*
Reentrancy (Fable5.2Analysis.md Section 8 and pending item 9): a boolean
that fails in the middle of the pipeline must leave nothing behind that
changes the next one. The connect stage keeps its working state in the
connector instance, so an unrelated boolean after a failure gives the same
answer as in a fresh process.
C++ counterpart of Java's `SetOpReentrancyTest`.
*/
namespace {

typedef PolyhedralBoundedSolidModeler Modeler;
typedef PolyhedralBoundedSolidVolumeOracle Oracle;

/*
CROSS_PAIR A - B: the two pieces left of A touch along an edge, so the
result is rejected as non-manifold, by decision, after generate, classify,
connect and finish have all run.
*/
void failingBoolean()
{
    java::ArrayList<PolyhedralBoundedSolid*> pair =
        CsgSampleCorpusFixtures::createPair(CROSS_PAIR);
    EXPECT_THROW(Modeler::setOp(pair[0], pair[1], Modeler::SUBTRACT),
        NonManifoldBooleanResultException);
    delete pair[0];
    delete pair[1];
}

/** A rotated box pair, answered by the normal pipeline. */
PolyhedralBoundedSolid* unrelatedUnion()
{
    PolyhedralBoundedSolid* a = Oracle::OrientedBox(2, 2, 1,
        Oracle::translation(0, 0, 0)).toSolid();
    PolyhedralBoundedSolid* b = Oracle::OrientedBox(1, 1, 1,
        Oracle::translation(1.0, 0.1, 0.2)
            .multiply(Oracle::rotation(0.5, 0, 0, 1))).toSolid();
    PolyhedralBoundedSolid* result = Modeler::setOp(a, b, Modeler::UNION);
    delete a;
    delete b;
    return result;
}

}

TEST(SetOpReentrancyTest, FailedBooleanUnrelatedBooleanFollowsSameResult) {
    PolyhedralBoundedSolid* reference = unrelatedUnion();
    java::String expected = PolyhedralBoundedSolidTopologySummary::from(
        reference).toString();
    double expectedVolume = Oracle::solidVolume(reference);
    delete reference;
    failingBoolean();
    PolyhedralBoundedSolid* after = unrelatedUnion();
    EXPECT_STREQ(expected.c_str(), PolyhedralBoundedSolidTopologySummary
        ::from(after).toString().c_str());
    EXPECT_NEAR(expectedVolume, Oracle::solidVolume(after), 1.0e-9);
    delete after;
}
