// C++ port of Java's SetOpDeterminismTest
// (environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators).
#include <stdio.h>
#include <string.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "BooleanOracleFuzz.h"
#include "CsgSampleCorpus.h"
#include "CsgSampleCorpusFixtures.h"
#include "PolyhedralBoundedSolidTestFixtures.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "PolyhedralTestFailures.h"
#include "PrismFixtures.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/Cpp98Support.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

typedef PolyhedralBoundedSolidVolumeOracle Oracle;
typedef PolyhedralBoundedSolidModeler Modeler;

/**
Java `Double.doubleToLongBits(value)`: here the bytes of the double in
hexadecimal, an equally exact fingerprint.
*/
static String
bitsOf(double value)
{
    unsigned char bytes[sizeof(double)];
    char text[2 * sizeof(double) + 1];
    unsigned i;

    memcpy(bytes, &value, sizeof(double));
    for ( i = 0; i < sizeof(double); i++ ) {
        sprintf(text + 2 * i, "%02x", (unsigned)bytes[i]);
    }
    return text;
}

static String
run(PolyhedralBoundedSolid* a, PolyhedralBoundedSolid* b, int op)
{
    PolyhedralBoundedSolid* result = NULL;
    String failureName;
    String failureMessage;

    try {
        if ( op == BooleanOracleFuzz::B_MINUS_A ) {
            result = Modeler::setOp(b, a, Modeler::SUBTRACT);
        }
        else {
            result = Modeler::setOp(a, b, op);
        }
    }
    PBS_CATCH_OPERATION(failureName, failureMessage)
    if ( result == NULL ) {
        return String("THROWS ") + failureName;
    }
    String text = PolyhedralBoundedSolidTopologySummary::from(result)
        .toString();
    long i;
    for ( i = 0; i < result->getVerticesList().size(); i++ ) {
        Vector3Dd p = result->getVerticesList().get(i)->position;
        text = text + "\n" + bitsOf(p.x()) + " " + bitsOf(p.y()) + " " +
            bitsOf(p.z());
    }
    for ( i = 0; i < result->getPolygonsList().size(); i++ ) {
        text = text + "\n" + Cpp98Support::toString(
            result->getPolygonsList().get(i)->boundariesList.size());
    }
    delete result;
    return text;
}

static String
fingerprint(CsgSampleCorpus sample, int op)
{
    ArrayList<PolyhedralBoundedSolid*> pair =
        CsgSampleCorpusFixtures::createPair(sample);
    String text = run(pair[0], pair[1], op);

    PolyhedralBoundedSolidTestFixtures::dispose(pair);
    return text;
}

static String
prismFingerprint(int op)
{
    PolyhedralBoundedSolid* a = PrismFixtures::prism(
        PrismFixtures::starPolygon(5, 1.0, 0.45), 1.0);
    PolyhedralBoundedSolid* b = PrismFixtures::prism(
        PrismFixtures::regularPolygon(9, 0.8), 1.2);
    Modeler::applyTransformation(b,
        Oracle::translation(0.2, 0.1, 0.3)
            .multiply(Oracle::rotation(0.6, 1, 2, 3)));
    String text = run(a, b, op);

    delete a;
    delete b;
    return text;
}

TEST(SetOpDeterminism, given_referencePairs_when_eachOperationRunsTwice_then_resultsAreIdentical)
{
    ArrayList<String> differences;
    int sampleIndex;
    long i;

    for ( sampleIndex = 0; sampleIndex < CSG_SAMPLE_CORPUS_COUNT;
          sampleIndex++ ) {
        CsgSampleCorpus sample = (CsgSampleCorpus)sampleIndex;
        int k;
        for ( k = 0; k < BooleanOracleFuzz::OPERATION_COUNT; k++ ) {
            int op = BooleanOracleFuzz::OPERATIONS[k];
            String first = fingerprint(sample, op);
            // Unrelated work in between: other operations move the
            // global id counters and leave their own state behind.
            fingerprint((CsgSampleCorpus)(
                (sampleIndex + 1) % CSG_SAMPLE_CORPUS_COUNT), Modeler::UNION);
            String second = fingerprint(sample, op);
            if ( !(first == second) ) {
                differences.add(String(csgSampleCorpusName(sample)) + " " +
                    BooleanOracleFuzz::opName(op));
            }
        }
    }

    // operations whose result changed between two runs
    for ( i = 0; i < differences.size(); i++ ) {
        printf("%s\n", differences[i].c_str());
    }
    EXPECT_TRUE(differences.isEmpty());
}

TEST(SetOpDeterminism, given_starPrisms_when_eachOperationRunsTwice_then_resultsAreIdentical)
{
    ArrayList<String> differences;
    int k;
    long i;

    for ( k = 0; k < BooleanOracleFuzz::OPERATION_COUNT; k++ ) {
        int op = BooleanOracleFuzz::OPERATIONS[k];
        String first = prismFingerprint(op);
        String second = prismFingerprint(op);
        if ( !(first == second) ) {
            differences.add(BooleanOracleFuzz::opName(op));
        }
    }

    for ( i = 0; i < differences.size(); i++ ) {
        printf("%s\n", differences[i].c_str());
    }
    EXPECT_TRUE(differences.isEmpty());
}

VITRAL_TEST_MAIN()
