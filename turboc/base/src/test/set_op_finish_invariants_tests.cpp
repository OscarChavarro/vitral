// C++ port of Java's SetOpFinishInvariantsTest
// (environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators).
#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "PolyhedralBoundedSolidTestFixtures.h"
#include "PolyhedralTestFailures.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetFinisher.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"

typedef PolyhedralBoundedSolidModeler Modeler;
typedef PolyhedralBoundedSolidTestFixtures Fixtures;

enum { BASELINE_FIXTURE_COUNT = 5 };

/**
Java's `baselineFixtures()` rows: the operands of row `row` go to `solidA`
and `solidB`, and `pairs` keeps every pair made (to delete them). As in
Java, the UNION and SUBTRACT rows of MANT1986_2 take A and B from two
different pairs.
*/
static const char*
baselineFixture(int row, ArrayList<PolyhedralBoundedSolid*>& pairs,
    PolyhedralBoundedSolid*& solidA, PolyhedralBoundedSolid*& solidB,
    int& op)
{
    ArrayList<PolyhedralBoundedSolid*> first;
    ArrayList<PolyhedralBoundedSolid*> second;
    const char* label;

    switch ( row ) {
      case 0:
      case 2:
        first = Fixtures::createMant1986_2Pair();
        second = Fixtures::createMant1986_2Pair();
        solidA = first[0];
        solidB = second[1];
        if ( row == 0 ) {
            op = Modeler::UNION;
            label = "MANT1986_2 UNION";
        }
        else {
            op = Modeler::SUBTRACT;
            label = "MANT1986_2 SUBTRACT";
        }
        break;
      case 1:
        first = Fixtures::createMant1986_2Pair();
        solidA = first[0];
        solidB = first[1];
        op = Modeler::INTERSECTION;
        label = "MANT1986_2 INTERSECTION";
        break;
      case 3:
        first = Fixtures::createMant1988_15_2Pair(0);
        solidA = first[0];
        solidB = first[1];
        op = Modeler::UNION;
        label = "MANT1988_15_2 UNION";
        break;
      default:
        first = Fixtures::createMant1988_6_13Pair();
        solidA = first[0];
        solidB = first[1];
        op = Modeler::SUBTRACT;
        label = "MANT1988_6_13 SUBTRACT";
        break;
    }
    pairs.addAll(first);
    pairs.addAll(second);
    return label;
}

/**
C++ port note: the parameterized tests are loops over the rows, and
`assertThatCode(...).doesNotThrowAnyException()` is a catch that fails the
case.
*/
TEST(SetOpFinishInvariants, given_baselineFixture_when_setopRuns_then_nullFacesArePairedByIndex)
{
    int row;

    for ( row = 0; row < BASELINE_FIXTURE_COUNT; row++ ) {
        ArrayList<PolyhedralBoundedSolid*> pairs;
        PolyhedralBoundedSolid* solidA;
        PolyhedralBoundedSolid* solidB;
        PolyhedralBoundedSolid* result = NULL;
        int op;
        const char* label = baselineFixture(row, pairs, solidA, solidB, op);
        String failureName;
        String failureMessage;

        printf("9.1 null faces paired by index: %s op=%d\n", label, op);
        try {
            result = Modeler::setOp(solidA, solidB, op, FALSE, TRUE, FALSE);
        }
        PBS_CATCH_THROWABLE(failureName, failureMessage)
        // finish must pair every null face by index
        if ( failureName.length() > 0 ) {
            printf("[%s] %s: %s\n", label, failureName.c_str(),
                failureMessage.c_str());
        }
        EXPECT_EQ(0, failureName.length());
        delete result;
        Fixtures::dispose(pairs);
    }
}

TEST(SetOpFinishInvariants, given_baselineFixture_when_setopRuns_then_noNonPlanarFaceTriangulated)
{
    int row;

    for ( row = 0; row < BASELINE_FIXTURE_COUNT; row++ ) {
        ArrayList<PolyhedralBoundedSolid*> pairs;
        PolyhedralBoundedSolid* solidA;
        PolyhedralBoundedSolid* solidB;
        int op;
        const char* label = baselineFixture(row, pairs, solidA, solidB, op);

        printf("9.2 no triangulation: %s op=%d\n", label, op);
        PolyhedralBoundedSolid* result = Modeler::setOp(solidA, solidB, op,
            FALSE, TRUE, FALSE);

        // triangulateNonPlanarFaces must split 0 faces in clean baseline
        EXPECT_EQ(0,
            _PolyhedralBoundedSolidSetFinisher::getLastTriangulatedFaceCount());
        delete result;
        Fixtures::dispose(pairs);
    }
}

VITRAL_TEST_MAIN()
