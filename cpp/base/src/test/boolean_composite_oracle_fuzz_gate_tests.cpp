#include <cstdio>
#include <gtest/gtest.h>
#include "java/util/ArrayList.txx"
#include "BooleanCompositeOracleFuzz.h"

/*
Ratchet gate over the seeded non-convex oracle fuzz (Fable5.2Analysis.md
Section 8, pending item 2): L, U, stepped and genus-1 operands, rotated,
scaled to 10^-3 and 10^3, and chains of three operations, checked against
the exact volume of `CompositeShapeOracle`. Every family must keep at
least its recorded number of correct, strict-valid results. Raise a minimum
when a fix lands; never lower one.

The three rotated families replay the same configurations at three
sizes, so their counts must stay equal: a difference means a tolerance that
does not follow the model scale.

Traceability: [MANT1988] Sec. 13.3 (volume as integral property) and
Ch. 15 (set operations).
C++ counterpart of Java's `BooleanCompositeOracleFuzzGateTest`.
*/
namespace {

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
int minimumOk(Fuzz::Family family)
{
    switch ( family ) {
        case Fuzz::NON_CONVEX_AXIS_ALIGNED: return 72;
        case Fuzz::NON_CONVEX_ROTATED: return 72;
        case Fuzz::NON_CONVEX_ROTATED_SCALE_1E_MINUS_3: return 72;
        case Fuzz::NON_CONVEX_ROTATED_SCALE_1E_PLUS_3: return 72;
        case Fuzz::CHAINS: return 36;
        case Fuzz::NON_CONVEX_COPLANAR_BOTTOMS:
        default: return 72;
    }
}

class BooleanCompositeOracleFuzzGateTest :
    public ::testing::TestWithParam<Fuzz::Family>
{
};

}

TEST_P(BooleanCompositeOracleFuzzGateTest, SeededNonConvexOperandsCorrectCountDoesNotRegress) {
    Fuzz::Family family = GetParam();
    Fuzz::FamilySummary summary = Fuzz::runFamily(family);
    printf("%s: %d / %d correct %s\n", Fuzz::familyName(family),
        summary.correct, summary.total, summary.byOutcomeText().c_str());
    for ( long i = 0; i < summary.failures.size(); i++ ) {
        printf("  %s\n", summary.failures[i].toString().c_str());
    }
    EXPECT_EQ(0, summary.byOutcome[Fuzz::WRONG_VOLUME])
        << "solids returned with a wrong volume in family "
        << Fuzz::familyName(family)
        << " (a silent error, whatever the ratchet says)";
    EXPECT_GE(summary.correct, minimumOk(family))
        << "correct results in family " << Fuzz::familyName(family)
        << " must not regress below the ratchet";
}

INSTANTIATE_TEST_SUITE_P(Families, BooleanCompositeOracleFuzzGateTest,
    ::testing::ValuesIn(BooleanCompositeOracleFuzz::FAMILIES),
    [](const ::testing::TestParamInfo<BooleanCompositeOracleFuzz::Family>& info) {
        return std::string(BooleanCompositeOracleFuzz::familyName(info.param));
    });
