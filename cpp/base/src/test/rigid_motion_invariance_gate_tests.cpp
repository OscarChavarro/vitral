#include <cmath>
#include <cstdio>
#include <limits>
#include <gtest/gtest.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "BooleanOracleFuzz.h"
#include "CsgSampleCorpusFixtures.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "RigidMotionInvarianceGate.h"
#include "ThrowableSimpleName.h"

/*
A boolean must commute with a rigid motion applied to both operands. This
gate runs every reference pair of CsgSampleCorpus under the four operations
twice, as built and after one fixed generic rotation, and compares
face/edge/vertex/shell counts and the divergence-theorem volume. It
measures how much of the green reference matrix depends on axis alignment
(Fable5.2Analysis.md Stage A step 3, defect B-05).

Ratchet: the number of invariant cases must not drop below
MINIMUM_INVARIANT. Raise it when a fix lands.

Slow (Java `@Tag("slow")`): built into `vitral_base_slow_tests`.

Traceability: [MANT1988] Ch. 15; Appendix B (regularised operations are
defined on point sets, hence invariant under isometries).
C++ counterpart of Java's `RigidMotionInvarianceGateTest`.
*/
namespace {

typedef PolyhedralBoundedSolidModeler Modeler;

/**
Stage A measured 24; Stage D raised it to 31. With the fallbacks retired
(every answer from the Ch. 15 pipeline) all 36 cases are invariant; the
same exception on both sides (CROSS_PAIR A-B, rejected as non-manifold)
counts as invariant.
*/
const int MINIMUM_INVARIANT = 36;

class Outcome
{
public:
    java::String summary;
    double volume;
    java::String path;
    bool strictValid;
    Outcome() : volume(0.0), path("null"), strictValid(false) {}
};

Outcome run(CsgSampleCorpus sample, int op, const Matrix4x4d* motion)
{
    java::ArrayList<PolyhedralBoundedSolid*> operands =
        CsgSampleCorpusFixtures::createPair(sample);
    PolyhedralBoundedSolid* a;
    PolyhedralBoundedSolid* b;
    Outcome outcome;

    if ( motion != nullptr ) {
        Modeler::applyTransformation(operands[0], *motion);
        Modeler::applyTransformation(operands[1], *motion);
    }
    a = op == BooleanOracleFuzz::B_MINUS_A ? operands[1] : operands[0];
    b = op == BooleanOracleFuzz::B_MINUS_A ? operands[0] : operands[1];
    try {
        PolyhedralBoundedSolid* result = Modeler::setOp(a, b,
            op == BooleanOracleFuzz::B_MINUS_A ?
                (int)Modeler::SUBTRACT : op, false, true, false);
        PolyhedralBoundedSolidTopologySummary topology =
            PolyhedralBoundedSolidTopologySummary::from(result);
        outcome.summary = java::String("F=") +
            java::String::valueOf(topology.getFaceCount()) +
            " E=" + java::String::valueOf(topology.getEdgeCount()) +
            " V=" + java::String::valueOf(topology.getVertexCount()) +
            " S=" + java::String::valueOf(topology.getShellCount());
        outcome.volume = PolyhedralBoundedSolidVolumeOracle::solidVolume(
            result);
        java::String message;
        outcome.strictValid = PolyhedralBoundedSolidValidationEngine
            ::validateStrict(result, &message);
        delete result;
    }
    catch ( const java::Throwable& t ) {
        outcome.summary = java::String("exception ") +
            throwableSimpleName(t);
        outcome.volume = std::numeric_limits<double>::quiet_NaN();
    }
    delete operands[0];
    delete operands[1];
    const java::String* path = Modeler::getLastSetOpResultPath();
    outcome.path = path == nullptr ? java::String("null") : *path;
    return outcome;
}

}

TEST(RigidMotionInvarianceGateTest, ReferencePairsRotatedRigidlyResultsAreInvariant) {
    java::ArrayList<java::String> report;
    int invariant = 0;
    int total = 0;
    Matrix4x4d rotation = rigidMotionGenericRotation();

    for ( int s = 0; s < CSG_SAMPLE_CORPUS_COUNT; s++ ) {
        CsgSampleCorpus sample = CSG_SAMPLE_CORPUS_VALUES[s];
        for ( int k = 0; k < 4; k++ ) {
            int op = BooleanOracleFuzz::OPERATIONS[k];
            Outcome plain = run(sample, op, nullptr);
            Outcome rotated = run(sample, op, &rotation);
            bool bothRejected = std::isnan(plain.volume) &&
                std::isnan(rotated.volume);
            bool same = plain.summary == rotated.summary &&
                plain.strictValid == rotated.strictValid &&
                (bothRejected ||
                 std::fabs(plain.volume - rotated.volume) <=
                     1.0e-6 * std::fmax(1.0, std::fabs(plain.volume)));
            total++;
            if ( same ) {
                invariant++;
            }
            report.add(java::String(same ? "  same " : "  DIFF ") +
                csgSampleCorpusName(sample) + " " +
                BooleanOracleFuzz::opName(op) + ": plain[" + plain.summary +
                " vol=" + java::String::valueOf(plain.volume) + " strict=" +
                (plain.strictValid ? "true" : "false") + " path=" +
                plain.path + "] rotated[" + rotated.summary + " vol=" +
                java::String::valueOf(rotated.volume) + " strict=" +
                (rotated.strictValid ? "true" : "false") + " path=" +
                rotated.path + "]");
        }
    }
    printf("Rigid-motion invariance: %d / %d\n", invariant, total);
    for ( long i = 0; i < report.size(); i++ ) {
        printf("%s\n", report[i].c_str());
    }
    EXPECT_GE(invariant, MINIMUM_INVARIANT)
        << "rigid-motion invariant reference cases (ratchet)";
}
