// C++ port of Java's NearDegenerateBooleanFuzzGateTest
// (environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators).
#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "NearDegenerateBooleanFuzz.h"
#include "java/util/ArrayList.txx"

typedef NearDegenerateBooleanFuzz Fuzz;

/**
First measurement (2026-11-16): 363 of 432 answered, 69 rejected. With
the operands aligned inside the tolerance band: 422 answered. The 10
left are the unions of EDGE_CONTACT at 0, 0.1 and 1 tolerances, which
are edge contacts and are rejected by decision (B-04).
*/
struct NearDegenerateRow
{
    Fuzz::Contact contact;
    BOOLEAN rotated;
    int minimumAnswered;
};

static const NearDegenerateRow ROWS[] = {
    { Fuzz::STACKED_PARTIAL, FALSE, 36 },
    { Fuzz::STACKED_PARTIAL, TRUE, 36 },
    { Fuzz::STACKED_SAME_FOOTPRINT, FALSE, 36 },
    { Fuzz::STACKED_SAME_FOOTPRINT, TRUE, 36 },
    { Fuzz::SIDE_BY_SIDE, FALSE, 36 },
    { Fuzz::SIDE_BY_SIDE, TRUE, 36 },
    { Fuzz::FLUSH_FACE_CROSSING, FALSE, 36 },
    { Fuzz::FLUSH_FACE_CROSSING, TRUE, 36 },
    { Fuzz::INNER_TANGENT, FALSE, 36 },
    { Fuzz::INNER_TANGENT, TRUE, 36 },
    { Fuzz::EDGE_CONTACT, FALSE, 31 },
    { Fuzz::EDGE_CONTACT, TRUE, 31 } };

/**
C++ port note: the Java parameterized test is a loop over its rows, each
row named as JUnit names it ("{0} rotated={1}").
*/
TEST(NearDegenerateBooleanFuzzGate, given_contactDisplacedByTolerances_when_fourOperations_then_neverAWrongSolid)
{
    int r;

    for ( r = 0; r < (int)(sizeof(ROWS) / sizeof(ROWS[0])); r++ ) {
        const NearDegenerateRow& row = ROWS[r];
        Fuzz::Summary summary = Fuzz::run(row.contact, row.rotated);
        long i;

        printf("[%s rotated=%s]\n", Fuzz::contactName(row.contact),
            row.rotated ? "true" : "false");
        printf("%s%s: ok %d rejected %d wrong %d of %d\n",
            Fuzz::contactName(row.contact), row.rotated ? " rotated" : "",
            summary.answered, summary.rejected, summary.wrong,
            summary.total);
        for ( i = 0; i < summary.details.size(); i++ ) {
            printf("  %s\n", summary.details[i].c_str());
        }

        // wrong solids returned without an error
        EXPECT_EQ(0, summary.wrong);
        // answered operations
        EXPECT_GE(summary.answered, row.minimumAnswered);
    }
}

VITRAL_TEST_MAIN()
