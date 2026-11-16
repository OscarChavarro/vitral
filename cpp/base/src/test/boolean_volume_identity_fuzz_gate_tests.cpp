#include <cstdio>
#include <gtest/gtest.h>
#include "java/util/ArrayList.txx"
#include "java/util/TreeMap.txx"
#include "BooleanVolumeIdentityFuzz.h"

/*
Gate over `BooleanVolumeIdentityFuzz` (plan-stage11 Phase B): prisms of N
sides and star prisms in general position, checked with the volume
identities of the four operations.

Two conditions per family:
- No silent error, ever: a returned set of results that breaks an
  identity, or an intersection outside [0, min(|A|, |B|)], fails the
  test whatever the ratchet says.
- Ratchet on the count of correct cases: raise it when a fix lands,
  never lower it. The cases below the total are typed rejections of the
  pipeline on non-convex faces (plan-stage11 Phase F).

Traceability: [MANT1988] Sec. 13.3 (volume) and Ch. 15.
C++ counterpart of Java's `BooleanVolumeIdentityFuzzGateTest`.
*/
namespace {

typedef BooleanVolumeIdentityFuzz Fuzz;

/** First measurement (2026-11-16): convex 16/16, star and convex 13/16, stars 11/16. After plan-stage11 Phase F: 16, 15, 16. */
int minimumOk(Fuzz::Family family)
{
    switch ( family ) {
        case Fuzz::CONVEX_PRISMS: return 16;
        case Fuzz::STAR_AND_CONVEX: return 16;
        case Fuzz::STAR_PRISMS:
        default: return 16;
    }
}

class BooleanVolumeIdentityFuzzGateTest :
    public ::testing::TestWithParam<Fuzz::Family>
{
};

}

TEST_P(BooleanVolumeIdentityFuzzGateTest, SeededPrismsFourOperationsVolumeIdentitiesHold) {
    Fuzz::Family family = GetParam();
    Fuzz::Summary summary = Fuzz::runFamily(family);
    printf("%s: %d / %d %s\n", Fuzz::familyName(family), summary.correct,
        summary.total, summary.byOutcomeText().c_str());
    for ( long i = 0; i < summary.failures.size(); i++ ) {
        printf("  %s\n", summary.failures[i].c_str());
    }
    EXPECT_FALSE(summary.byOutcome.containsKey("IDENTITY") ||
        summary.byOutcome.containsKey("RANGE") ||
        summary.byOutcome.containsKey("OPERAND"))
        << "results that break a volume identity are silent errors";
    EXPECT_GE(summary.correct, minimumOk(family))
        << "correct cases in family " << Fuzz::familyName(family);
}

INSTANTIATE_TEST_SUITE_P(Families, BooleanVolumeIdentityFuzzGateTest,
    ::testing::ValuesIn(BooleanVolumeIdentityFuzz::FAMILIES),
    [](const ::testing::TestParamInfo<BooleanVolumeIdentityFuzz::Family>& info) {
        return std::string(BooleanVolumeIdentityFuzz::familyName(info.param));
    });
