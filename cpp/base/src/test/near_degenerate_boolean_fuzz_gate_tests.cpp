#include <ostream>
#include <cstdio>
#include <gtest/gtest.h>
#include "java/util/ArrayList.txx"
#include "java/util/TreeMap.txx"
#include "NearDegenerateBooleanFuzz.h"

/*
Gate over `NearDegenerateBooleanFuzz` (plan-stage11 Phase B): box pairs in
degenerate contact, displaced by -100 to 100 tolerances, as built and
rotated; 36 operations per row.

Two conditions per row:
- No wrong solid, ever. An operation either returns a strict-valid solid
  with the right volume or throws a typed exception. Before this gate,
  FLUSH_FACE_CROSSING rotated at 0.1 tolerances returned B - A with one
  of its two pieces missing: connect had left loose null edges on an open
  intersection curve and finish went on. That is now rejected.
- Ratchet on the count of answered operations: raise it when a fix
  lands, never lower it. Every row is at its total except EDGE_CONTACT,
  whose unions near contact are rejected by decision.

C++ counterpart of Java's `NearDegenerateBooleanFuzzGateTest`.
*/
namespace {

typedef NearDegenerateBooleanFuzz Fuzz;

struct Row
{
    Fuzz::Contact contact;
    bool rotated;
    int minimumAnswered;
};

/** Test name shown by gtest and ctest (instead of the raw bytes). */
void PrintTo(const Row& v, std::ostream* os)
{
    *os << NearDegenerateBooleanFuzz::contactName(v.contact) << (v.rotated ? " rotated" : "");
}

/**
First measurement (2026-11-16): 363 of 432 answered, 69 rejected. With
the operands aligned inside the tolerance band: 422 answered. The 10
left are the unions of EDGE_CONTACT at 0, 0.1 and 1 tolerances, which
are edge contacts and are rejected by decision (B-04).
*/
const Row ROWS[] = {
    { Fuzz::STACKED_PARTIAL, false, 36 },
    { Fuzz::STACKED_PARTIAL, true, 36 },
    { Fuzz::STACKED_SAME_FOOTPRINT, false, 36 },
    { Fuzz::STACKED_SAME_FOOTPRINT, true, 36 },
    { Fuzz::SIDE_BY_SIDE, false, 36 },
    { Fuzz::SIDE_BY_SIDE, true, 36 },
    { Fuzz::FLUSH_FACE_CROSSING, false, 36 },
    { Fuzz::FLUSH_FACE_CROSSING, true, 36 },
    { Fuzz::INNER_TANGENT, false, 36 },
    { Fuzz::INNER_TANGENT, true, 36 },
    { Fuzz::EDGE_CONTACT, false, 31 },
    { Fuzz::EDGE_CONTACT, true, 31 }
};

class NearDegenerateBooleanFuzzGateTest :
    public ::testing::TestWithParam<Row>
{
};

}

TEST_P(NearDegenerateBooleanFuzzGateTest, ContactDisplacedByTolerancesFourOperationsNeverAWrongSolid) {
    const Row& row = GetParam();
    Fuzz::Summary summary = Fuzz::run(row.contact, row.rotated);
    printf("%s%s: ok %d rejected %d wrong %d of %d\n",
        Fuzz::contactName(row.contact), row.rotated ? " rotated" : "",
        summary.answered, summary.rejected, summary.wrong, summary.total);
    for ( long i = 0; i < summary.details.size(); i++ ) {
        printf("  %s\n", summary.details[i].c_str());
    }
    EXPECT_EQ(0, summary.wrong)
        << "wrong solids returned without an error: "
        << summary.detailsText().c_str();
    EXPECT_GE(summary.answered, row.minimumAnswered)
        << "answered operations";
}

INSTANTIATE_TEST_SUITE_P(Rows, NearDegenerateBooleanFuzzGateTest,
    ::testing::ValuesIn(ROWS),
    [](const ::testing::TestParamInfo<Row>& info) {
        return std::string(NearDegenerateBooleanFuzz::contactName(
            info.param.contact)) + (info.param.rotated ? "_rotated" : "");
    });
