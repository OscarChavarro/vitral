// C++ port of Java's BooleanVolumeIdentityFuzzGateTest
// (environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators).
#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "BooleanVolumeIdentityFuzz.h"
#include "java/util/ArrayList.txx"

typedef BooleanVolumeIdentityFuzz Fuzz;

/**
First measurement (2026-11-16): convex 16/16, star and convex 13/16,
stars 11/16. After plan-stage11 Phase F: 16, 15, 16.
*/
static int
minimumOk(Fuzz::Family family)
{
    static const int MINIMUM_OK[Fuzz::FAMILY_COUNT] = {
        16,   // CONVEX_PRISMS
        16,   // STAR_AND_CONVEX
        16 }; // STAR_PRISMS
    return MINIMUM_OK[(int)family];
}

/**
C++ port note: the Java test parameterized over the families is one case
per family.
*/
static void
checkFamily(Fuzz::Family family)
{
    Fuzz::Summary summary = Fuzz::runFamily(family);
    long i;

    printf("%s: %d / %d %s\n", Fuzz::familyName(family), summary.correct,
        summary.total, summary.byOutcomeText().c_str());
    for ( i = 0; i < summary.failures.size(); i++ ) {
        printf("  %s\n", summary.failures[i].c_str());
    }

    // results that break a volume identity are silent errors
    EXPECT_FALSE(summary.containsKey("IDENTITY"));
    EXPECT_FALSE(summary.containsKey("RANGE"));
    EXPECT_FALSE(summary.containsKey("OPERAND"));
    // correct cases in the family
    EXPECT_GE(summary.correct, minimumOk(family));
}

TEST(BooleanVolumeIdentityFuzzGate, CONVEX_PRISMS)
{
    checkFamily(Fuzz::CONVEX_PRISMS);
}

TEST(BooleanVolumeIdentityFuzzGate, STAR_AND_CONVEX)
{
    checkFamily(Fuzz::STAR_AND_CONVEX);
}

TEST(BooleanVolumeIdentityFuzzGate, STAR_PRISMS)
{
    checkFamily(Fuzz::STAR_PRISMS);
}

VITRAL_TEST_MAIN()
