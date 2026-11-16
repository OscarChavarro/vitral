// C++ port of Java's SetOpConnectRingInvariantTest
// (environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators).
#include <math.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "BooleanOracleFuzz.h"
#include "CsgSampleCorpus.h"
#include "CsgSampleCorpusFixtures.h"
#include "PolyhedralBoundedSolidTestFixtures.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/NonManifoldBooleanResultException.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/_PolyhedralBoundedSolidSetOperator.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"

typedef PolyhedralBoundedSolidVolumeOracle Oracle;
typedef PolyhedralBoundedSolidVolumeOracle::OrientedBox OrientedBox;
typedef PolyhedralBoundedSolidModeler Modeler;
typedef _PolyhedralBoundedSolidSetOperator SetOperator;

/** Java `List.toString()`. */
static String
listText(const ArrayList<String>& list)
{
    String text = "[";
    long i;

    for ( i = 0; i < list.size(); i++ ) {
        if ( i > 0 ) {
            text = text + ", ";
        }
        text = text + list[i];
    }
    return text + "]";
}

static void
assertViolationsEmpty(const String& label)
{
    const ArrayList<String>* violations =
        SetOperator::getLastConnectInvariantViolations();
    if ( violations != NULL ) {
        if ( !violations->isEmpty() ) {
            printf("%s: %s\n", label.c_str(), listText(*violations).c_str());
        }
        EXPECT_TRUE(violations->isEmpty());
    }
}

static void
checkCorpusCase(CsgSampleCorpus sample, int op)
{
    ArrayList<PolyhedralBoundedSolid*> p =
        CsgSampleCorpusFixtures::createPair(sample);
    BOOLEAN bMinusA = op == BooleanOracleFuzz::B_MINUS_A;
    PolyhedralBoundedSolid* result = NULL;

    try {
        result = Modeler::setOp(bMinusA ? p[1] : p[0],
            bMinusA ? p[0] : p[1],
            bMinusA ? (int)Modeler::SUBTRACT : op, FALSE, TRUE, FALSE);
    }
#ifndef VSDK_WITHOUT_EXCEPTIONS
    catch ( const NonManifoldBooleanResultException& ) {
        // Rejected after finish (CROSS_PAIR A-B: two bars touching
        // along an edge); connect ran and its invariant still applies.
    }
#endif
    delete result;

    assertViolationsEmpty(String(csgSampleCorpusName(sample)) + " " +
        BooleanOracleFuzz::opName(op));
    PolyhedralBoundedSolidTestFixtures::dispose(p);
}

/**
C++ port note: the parameterized tests are loops over their Java rows.
*/
TEST(SetOpConnectRingInvariant, given_referencePair_when_setOpReachesConnect_then_ringInvariantHolds)
{
    int sampleIndex;
    int k;

    for ( sampleIndex = 0; sampleIndex < CSG_SAMPLE_CORPUS_COUNT;
          sampleIndex++ ) {
        for ( k = 0; k < BooleanOracleFuzz::OPERATION_COUNT; k++ ) {
            checkCorpusCase((CsgSampleCorpus)sampleIndex,
                BooleanOracleFuzz::OPERATIONS[k]);
        }
    }
}

static void
checkSectionRing(const char* name, const BooleanOracleFuzz::BoxPair& pair,
    int op)
{
    BooleanOracleFuzz::CaseResult result = BooleanOracleFuzz::run(
        BooleanOracleFuzz::B_ROTATED_ABOUT_RANDOM_AXIS, 0, pair, op);

    printf("%s op=%d\n", name, op);
    // reached connect
    EXPECT_TRUE(SetOperator::getLastConnectInvariantViolations() != NULL);
    assertViolationsEmpty(String(name) + " " + BooleanOracleFuzz::opName(op));
    if ( result.outcome != BooleanOracleFuzz::OK ) {
        printf("%s\n", result.toString().c_str());
    }
    EXPECT_EQ((int)BooleanOracleFuzz::OK, (int)result.outcome);
}

TEST(SetOpConnectRingInvariant, given_sectionRingInsideAFace_when_setOp_then_ringInvariantHolds)
{
    BooleanOracleFuzz::BoxPair peg(
        OrientedBox(2, 2, 2, Oracle::translation(0, 0, 0)),
        OrientedBox(0.5, 0.5, 1.5, Oracle::translation(0.1, 0.2, 1.0)));
    BooleanOracleFuzz::BoxPair pegThrough(
        OrientedBox(2, 2, 2, Oracle::translation(0, 0, 0)),
        OrientedBox(0.5, 0.5, 3.0, Oracle::translation(0.1, 0.2, 0.05)
            .multiply(Oracle::rotation(0.3, 0, 0, 1))));
    BooleanOracleFuzz::BoxPair corner(
        OrientedBox(4, 4, 1, Oracle::translation(0.05, 0.03, 1.2)),
        OrientedBox(1, 1, 1, Oracle::rotation(0.37, 0, 0, 1).multiply(
            Oracle::rotation(acos(1.0 / sqrt(3.0)), 1, -1, 0))));
    int k;

    for ( k = 0; k < BooleanOracleFuzz::OPERATION_COUNT; k++ ) {
        int op = BooleanOracleFuzz::OPERATIONS[k];
        checkSectionRing("peg-ending-inside-block", peg, op);
        checkSectionRing("peg-through-block", pegThrough, op);
        checkSectionRing("cube-corner-through-slab-face", corner, op);
    }
}

TEST(SetOpConnectRingInvariant, given_nonCoplanarFuzzFamilies_when_setOp_then_ringInvariantHolds)
{
    ArrayList<String> report;
    int familyIndex;
    long r;

    for ( familyIndex = 0; familyIndex < BooleanOracleFuzz::FAMILY_COUNT;
          familyIndex++ ) {
        BooleanOracleFuzz::Family family =
            (BooleanOracleFuzz::Family)familyIndex;
        if ( family == BooleanOracleFuzz::INTEGER_GRID ||
             family == BooleanOracleFuzz::COPLANAR_TOP_BOTTOM_ROTATED_45 ) {
            // Coplanar contacts: Stage D (defects B-02, B-05).
            continue;
        }
        ArrayList<BooleanOracleFuzz::BoxPair> pairs =
            BooleanOracleFuzz::pairs(family);
        long i;
        for ( i = 0; i < pairs.size(); i++ ) {
            int k;
            for ( k = 0; k < BooleanOracleFuzz::OPERATION_COUNT; k++ ) {
                int op = BooleanOracleFuzz::OPERATIONS[k];
                BooleanOracleFuzz::run(family, (int)i, pairs[i], op);
                const ArrayList<String>* v =
                    SetOperator::getLastConnectInvariantViolations();
                if ( v != NULL && !v->isEmpty() ) {
                    char number[32];
                    sprintf(number, "%ld", i);
                    report.add(String(BooleanOracleFuzz::familyName(family)) +
                        "#" + number + " " + BooleanOracleFuzz::opName(op) +
                        " " + listText(*v));
                }
            }
        }
    }
    for ( r = 0; r < report.size(); r++ ) {
        printf("%s\n", report[r].c_str());
    }
    EXPECT_TRUE(report.isEmpty());
}

VITRAL_TEST_MAIN()
