// C++ port of Java's SetOpReentrancyTest
// (environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators).
#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "CsgSampleCorpus.h"
#include "CsgSampleCorpusFixtures.h"
#include "PolyhedralBoundedSolidTestFixtures.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/NonManifoldBooleanResultException.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.h"

typedef PolyhedralBoundedSolidVolumeOracle Oracle;
typedef PolyhedralBoundedSolidModeler Modeler;

/**
CROSS_PAIR A - B: the two pieces left of A touch along an edge, so the
result is rejected as non-manifold, by decision, after generate,
classify, connect and finish have all run. (This test used three
defects in turn as its failing boolean, a rotated non-convex pair, one
with coplanar bottoms and one inside the tolerance band; plan-stage11
fixed each of them. A rejection by decision does not go away.)
*/
static void
failingBoolean()
{
    ArrayList<PolyhedralBoundedSolid*> pair =
        CsgSampleCorpusFixtures::createPair(CROSS_PAIR);

    EXPECT_THROW(Modeler::setOp(pair[0], pair[1], Modeler::SUBTRACT),
        NonManifoldBooleanResultException);
    PolyhedralBoundedSolidTestFixtures::dispose(pair);
}

/** A rotated box pair, answered by the normal pipeline. */
static PolyhedralBoundedSolid*
unrelatedUnion()
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

TEST(SetOpReentrancy, given_failedBoolean_when_unrelatedBooleanFollows_then_sameResult)
{
    PolyhedralBoundedSolid* reference = unrelatedUnion();
    String expected =
        PolyhedralBoundedSolidTopologySummary::from(reference).toString();
    double expectedVolume = Oracle::solidVolume(reference);

    failingBoolean();
    PolyhedralBoundedSolid* after = unrelatedUnion();

    EXPECT_TRUE(PolyhedralBoundedSolidTopologySummary::from(after)
        .toString() == expected);
    EXPECT_NEAR(expectedVolume, Oracle::solidVolume(after), 1.0e-9);
    delete reference;
    delete after;
}

VITRAL_TEST_MAIN()
