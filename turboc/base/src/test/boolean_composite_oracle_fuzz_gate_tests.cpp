// C++ port of Java's BooleanCompositeOracleFuzzGateTest
// (environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators).
#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "BooleanCompositeOracleFuzz.h"
#include "java/util/ArrayList.txx"

typedef BooleanCompositeOracleFuzz Fuzz;

/**
Ratchet: raise only. First measurement (2026-11-16), after the
scale-aware tolerances and the ring placement of join: the rotated
families 47 of 72, the chains 23 of 36 and the coplanar bottoms 46 of
72. In general position a non-manifold rejection counts as a failure.
After plan-stage11 Phase F (connect joins curve neighbours only; lmekr
keeps the outer loop first; finish keeps whole shells): rotated 65,
chains 31, coplanar bottoms 50, and the 72 axis-aligned ones are
answered, none rejected. No family returns a wrong volume any more,
and that is now a condition of its own: see the test. With rings
redistributed on both solids for every operation: rotated 72 of 72 at
the three scales, chains 36 of 36, coplanar bottoms 54; 72 once
contact nodes are removed before connect. Every family is complete.
*/
static int
minimumOk(Fuzz::Family family)
{
    static const int MINIMUM_OK[Fuzz::FAMILY_COUNT] = {
        72,   // NON_CONVEX_AXIS_ALIGNED
        72,   // NON_CONVEX_ROTATED
        72,   // NON_CONVEX_ROTATED_SCALE_1E_MINUS_3
        72,   // NON_CONVEX_ROTATED_SCALE_1E_PLUS_3
        36,   // CHAINS
        72 }; // NON_CONVEX_COPLANAR_BOTTOMS
    return MINIMUM_OK[(int)family];
}

static void
checkFamily(Fuzz::Family family)
{
    Fuzz::FamilySummary summary = Fuzz::runFamily(family);
    long i;

    printf("%s: %d / %d correct %s\n", Fuzz::familyName(family),
        summary.correct, summary.total, summary.byOutcomeText().c_str());
    for ( i = 0; i < summary.failures.size(); i++ ) {
        printf("  %s\n", summary.failures[i].toString().c_str());
    }

    // solids returned with a wrong volume in the family (a silent error,
    // whatever the ratchet says)
    EXPECT_EQ(0, summary.byOutcome[Fuzz::WRONG_VOLUME]);
    // correct results in the family must not regress below the ratchet
    EXPECT_GE(summary.correct, minimumOk(family));
}

/**
C++ port note: the Java test parameterized over the families is one case
per family.
*/
TEST(BooleanCompositeOracleFuzzGate, NON_CONVEX_AXIS_ALIGNED)
{
    checkFamily(Fuzz::NON_CONVEX_AXIS_ALIGNED);
}

TEST(BooleanCompositeOracleFuzzGate, NON_CONVEX_ROTATED)
{
    checkFamily(Fuzz::NON_CONVEX_ROTATED);
}

TEST(BooleanCompositeOracleFuzzGate, NON_CONVEX_ROTATED_SCALE_1E_MINUS_3)
{
    checkFamily(Fuzz::NON_CONVEX_ROTATED_SCALE_1E_MINUS_3);
}

TEST(BooleanCompositeOracleFuzzGate, NON_CONVEX_ROTATED_SCALE_1E_PLUS_3)
{
    checkFamily(Fuzz::NON_CONVEX_ROTATED_SCALE_1E_PLUS_3);
}

TEST(BooleanCompositeOracleFuzzGate, CHAINS)
{
    checkFamily(Fuzz::CHAINS);
}

TEST(BooleanCompositeOracleFuzzGate, NON_CONVEX_COPLANAR_BOTTOMS)
{
    checkFamily(Fuzz::NON_CONVEX_COPLANAR_BOTTOMS);
}

VITRAL_TEST_MAIN()
