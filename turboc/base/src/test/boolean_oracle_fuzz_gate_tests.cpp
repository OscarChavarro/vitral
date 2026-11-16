// C++ port of Java's BooleanOracleFuzzGateTest (@Tag("slow"))
// (environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators).
#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "BooleanOracleFuzz.h"
#include "java/util/ArrayList.txx"

/**
Ratchet: raise only. Stage A (2026-11-16) measured 48/41/38/38/304/16;
the B-01 ring fix (Stage C) raised the three rotated families to 48 and
the coplanar family to 18. Stage D (vertex/face neighbourhood, witness
containment, face-contact union) raised coplanar to 48 and the grid to
306. The 14 grid unions touching only along an edge (B-04) are now
rejected as non-manifold, which counts as correct: grid 320.
*/
static int
minimumOk(BooleanOracleFuzz::Family family)
{
    static const int MINIMUM_OK[BooleanOracleFuzz::FAMILY_COUNT] = {
        48,   // AXIS_ALIGNED_GENERAL_POSITION
        48,   // B_ROTATED_ABOUT_Z
        48,   // B_ROTATED_ABOUT_RANDOM_AXIS
        48,   // RANDOM_AXIS_TRANSLATED_TO_1000
        320,  // INTEGER_GRID
        48 }; // COPLANAR_TOP_BOTTOM_ROTATED_45
    return MINIMUM_OK[(int)family];
}

static void
checkFamily(BooleanOracleFuzz::Family family)
{
    BooleanOracleFuzz::FamilySummary summary =
        BooleanOracleFuzz::runFamily(family);
    long i;

    printf("%s: %d / %d correct %s\n", BooleanOracleFuzz::familyName(family),
        summary.correct, summary.total, summary.byOutcomeText().c_str());
    for ( i = 0; i < summary.failures.size(); i++ ) {
        printf("  %s\n", summary.failures[i].toString().c_str());
    }

    // correct results in the family must not regress below the ratchet
    EXPECT_GE(summary.correct, minimumOk(family));
}

/**
C++ port note: the Java test parameterized over the families is one case
per family.
*/
TEST(BooleanOracleFuzzGate, AXIS_ALIGNED_GENERAL_POSITION)
{
    VITRAL_SLOW_TEST();
    checkFamily(BooleanOracleFuzz::AXIS_ALIGNED_GENERAL_POSITION);
}

TEST(BooleanOracleFuzzGate, B_ROTATED_ABOUT_Z)
{
    VITRAL_SLOW_TEST();
    checkFamily(BooleanOracleFuzz::B_ROTATED_ABOUT_Z);
}

TEST(BooleanOracleFuzzGate, B_ROTATED_ABOUT_RANDOM_AXIS)
{
    VITRAL_SLOW_TEST();
    checkFamily(BooleanOracleFuzz::B_ROTATED_ABOUT_RANDOM_AXIS);
}

TEST(BooleanOracleFuzzGate, RANDOM_AXIS_TRANSLATED_TO_1000)
{
    VITRAL_SLOW_TEST();
    checkFamily(BooleanOracleFuzz::RANDOM_AXIS_TRANSLATED_TO_1000);
}

TEST(BooleanOracleFuzzGate, INTEGER_GRID)
{
    VITRAL_SLOW_TEST();
    checkFamily(BooleanOracleFuzz::INTEGER_GRID);
}

TEST(BooleanOracleFuzzGate, COPLANAR_TOP_BOTTOM_ROTATED_45)
{
    VITRAL_SLOW_TEST();
    checkFamily(BooleanOracleFuzz::COPLANAR_TOP_BOTTOM_ROTATED_45);
}

VITRAL_TEST_MAIN()
