#include <cstdio>
#include <gtest/gtest.h>
#include "java/util/ArrayList.txx"
#include "BooleanOracleFuzz.h"

/*
Ratchet gate over the seeded box-pair oracle fuzz (Fable5.2Analysis.md
Section 1 and Stage A step 2). Every family must keep at least its recorded
number of correct, strict-valid results. Raise a minimum when a fix lands;
never lower one.

Slow (Java `@Tag("slow")`): built into `vitral_base_slow_tests`, see
`cpp/scripts/testAllWithSlowCases.sh`. The per-family failures are printed
so a regression names the cases that broke.

Traceability: [MANT1988] Sec. 13.3 (volume as integral property) and
Ch. 15 (set operations).
C++ counterpart of Java's `BooleanOracleFuzzGateTest`.
*/
namespace {

typedef BooleanOracleFuzz Fuzz;

/**
Ratchet: raise only. Stage A (2026-11-16) measured 48/41/38/38/304/16;
the B-01 ring fix (Stage C) raised the three rotated families to 48 and
the coplanar family to 18. Stage D (vertex/face neighbourhood, witness
containment, face-contact union) raised coplanar to 48 and the grid to
306. The 14 grid unions touching only along an edge (B-04) are now
rejected as non-manifold, which counts as correct: grid 320.
*/
int minimumOk(Fuzz::Family family)
{
    switch ( family ) {
        case Fuzz::AXIS_ALIGNED_GENERAL_POSITION: return 48;
        case Fuzz::B_ROTATED_ABOUT_Z: return 48;
        case Fuzz::B_ROTATED_ABOUT_RANDOM_AXIS: return 48;
        case Fuzz::RANDOM_AXIS_TRANSLATED_TO_1000: return 48;
        case Fuzz::INTEGER_GRID: return 320;
        case Fuzz::COPLANAR_TOP_BOTTOM_ROTATED_45:
        default: return 48;
    }
}

class BooleanOracleFuzzGateTest :
    public ::testing::TestWithParam<Fuzz::Family>
{
};

}

TEST_P(BooleanOracleFuzzGateTest, SeededBoxPairsCorrectCountDoesNotRegress) {
    Fuzz::Family family = GetParam();
    Fuzz::FamilySummary summary = Fuzz::runFamily(family);
    printf("%s: %d / %d correct %s\n", Fuzz::familyName(family),
        summary.correct, summary.total, summary.byOutcomeText().c_str());
    for ( long i = 0; i < summary.failures.size(); i++ ) {
        printf("  %s\n", summary.failures[i].toString().c_str());
    }
    EXPECT_GE(summary.correct, minimumOk(family))
        << "correct results in family " << Fuzz::familyName(family)
        << " must not regress below the ratchet";
}

INSTANTIATE_TEST_SUITE_P(Families, BooleanOracleFuzzGateTest,
    ::testing::ValuesIn(BooleanOracleFuzz::FAMILIES),
    [](const ::testing::TestParamInfo<BooleanOracleFuzz::Family>& info) {
        return std::string(BooleanOracleFuzz::familyName(info.param));
    });
