// C++ port of Java's RigidMotionInvarianceGateTest (@Tag("slow"))
// (environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators).
#include <math.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "BooleanOracleFuzz.h"
#include "CsgSampleCorpus.h"
#include "CsgSampleCorpusFixtures.h"
#include "PolyhedralBoundedSolidTestFixtures.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "PolyhedralTestFailures.h"
#include "RigidMotionInvarianceGate.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/Cpp98Support.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"

typedef PolyhedralBoundedSolidModeler Modeler;

/**
Stage A measured 24; Stage D raised it to 31. With the fallbacks retired
(every answer from the Ch. 15 pipeline) all 36 cases are invariant; the
same exception on both sides (CROSS_PAIR A-B, rejected as non-manifold)
counts as invariant.
*/
static const int MINIMUM_INVARIANT = 36;

/**
C++ port note: Java's `Double.NaN` volume of a rejected case is the flag
`rejected` (the legacy targets lack a portable NaN).
*/
class RigidMotionOutcome
{
public:
    String summary;
    double volume;
    BOOLEAN rejected;
    String path;
    BOOLEAN strictValid;

    RigidMotionOutcome() : volume(0.0), rejected(FALSE), path("null"),
        strictValid(FALSE)
    {
    }
};

static String
str(long value)
{
    return Cpp98Support::toString(value);
}

static RigidMotionOutcome
run(CsgSampleCorpus sample, int op, const Matrix4x4d* motion)
{
    ArrayList<PolyhedralBoundedSolid*> operands =
        CsgSampleCorpusFixtures::createPair(sample);
    PolyhedralBoundedSolid* a;
    PolyhedralBoundedSolid* b;
    PolyhedralBoundedSolid* result = NULL;
    RigidMotionOutcome outcome;
    String failureName;
    String failureMessage;

    if ( motion != NULL ) {
        Modeler::applyTransformation(operands[0], *motion);
        Modeler::applyTransformation(operands[1], *motion);
    }
    a = op == BooleanOracleFuzz::B_MINUS_A ? operands[1] : operands[0];
    b = op == BooleanOracleFuzz::B_MINUS_A ? operands[0] : operands[1];
    try {
        result = Modeler::setOp(a, b,
            op == BooleanOracleFuzz::B_MINUS_A ? (int)Modeler::SUBTRACT : op,
            FALSE, TRUE, FALSE);
        PolyhedralBoundedSolidTopologySummary topology =
            PolyhedralBoundedSolidTopologySummary::from(result);
        outcome.summary = String("F=") + str(topology.getFaceCount()) +
            " E=" + str(topology.getEdgeCount()) +
            " V=" + str(topology.getVertexCount()) +
            " S=" + str(topology.getShellCount());
        outcome.volume = PolyhedralBoundedSolidVolumeOracle::solidVolume(
            result);
        String message;
        outcome.strictValid =
            PolyhedralBoundedSolidValidationEngine::validateStrict(result,
                &message);
    }
    PBS_CATCH_THROWABLE(failureName, failureMessage)
    if ( failureName.length() > 0 ) {
        outcome.summary = String("exception ") + failureName;
        outcome.rejected = TRUE;
    }
    delete result;
    if ( Modeler::getLastSetOpResultPath() != NULL ) {
        outcome.path = *Modeler::getLastSetOpResultPath();
    }
    else {
        outcome.path = "null";
    }
    PolyhedralBoundedSolidTestFixtures::dispose(operands);
    return outcome;
}

static String
volumeText(const RigidMotionOutcome& outcome)
{
    char text[40];

    if ( outcome.rejected ) {
        return "NaN";
    }
    sprintf(text, "%.17g", outcome.volume);
    return text;
}

static String
booleanText(BOOLEAN value)
{
    if ( value ) {
        return String("true");
    }
    return String("false");
}

TEST(RigidMotionInvarianceGate, given_referencePairs_when_rotatedRigidly_then_resultsAreInvariant)
{
    VITRAL_SLOW_TEST();
    Matrix4x4d genericRotation = rigidMotionGenericRotation();
    ArrayList<String> report;
    int invariant = 0;
    int total = 0;
    int sampleIndex;
    long i;

    for ( sampleIndex = 0; sampleIndex < CSG_SAMPLE_CORPUS_COUNT;
          sampleIndex++ ) {
        CsgSampleCorpus sample = (CsgSampleCorpus)sampleIndex;
        int k;
        for ( k = 0; k < BooleanOracleFuzz::OPERATION_COUNT; k++ ) {
            int op = BooleanOracleFuzz::OPERATIONS[k];
            RigidMotionOutcome plain = run(sample, op, NULL);
            RigidMotionOutcome rotated = run(sample, op, &genericRotation);
            BOOLEAN bothRejected = plain.rejected && rotated.rejected;
            double scale = fabs(plain.volume) > 1.0 ? fabs(plain.volume) :
                1.0;
            BOOLEAN same = plain.summary == rotated.summary &&
                plain.strictValid == rotated.strictValid &&
                (bothRejected ||
                 (!plain.rejected && !rotated.rejected &&
                  fabs(plain.volume - rotated.volume) <= 1.0e-6 * scale));
            total++;
            if ( same ) {
                invariant++;
            }
            String head = "  DIFF ";
            if ( same ) {
                head = "  same ";
            }
            report.add(head + csgSampleCorpusName(sample) + " " +
                BooleanOracleFuzz::opName(op) + ": plain[" +
                plain.summary + " vol=" + volumeText(plain) + " strict=" +
                booleanText(plain.strictValid) + " path=" + plain.path +
                "] rotated[" + rotated.summary + " vol=" +
                volumeText(rotated) + " strict=" +
                booleanText(rotated.strictValid) + " path=" + rotated.path +
                "]");
        }
    }
    printf("Rigid-motion invariance: %d / %d\n", invariant, total);
    for ( i = 0; i < report.size(); i++ ) {
        printf("%s\n", report[i].c_str());
    }

    // rigid-motion invariant reference cases (ratchet)
    EXPECT_GE(invariant, MINIMUM_INVARIANT);
}

VITRAL_TEST_MAIN()
