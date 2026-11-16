package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologySummary;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

import static org.assertj.core.api.Assertions.assertThat;

/**
A boolean must commute with a rigid motion applied to both operands. This
gate runs every reference pair of {@link CsgSampleCorpus} under the four
operations twice, as built and after one fixed generic rotation, and
compares face/edge/vertex/shell counts and the divergence-theorem volume.
It measures how much of the green reference matrix depends on axis
alignment (Fable5.2Analysis.md Stage A step 3, defect B-05).

<p>Ratchet: the number of invariant cases must not drop below
{@link #MINIMUM_INVARIANT}. Raise it when a fix lands.</p>

<p>Traceability: [MANT1988] Ch. 15; Appendix B (regularised operations are
defined on point sets, hence invariant under isometries).</p>
 */
@Tag("slow")
class RigidMotionInvarianceGateTest
{
    /**
    Stage A measured 24; Stage D raised it to 31. With the fallbacks retired
    (every answer from the Ch. 15 pipeline) all 36 cases are invariant; the
    same exception on both sides (CROSS_PAIR A-B, rejected as non-manifold)
    counts as invariant.
    */
    private static final int MINIMUM_INVARIANT = 36;

    private static final int[] OPERATIONS = BooleanOracleFuzz.OPERATIONS;

    static final Matrix4x4d GENERIC_ROTATION =
        new Matrix4x4d().axisRotation(0.61, 0.3, 1.0, 0.2);

    static final class Outcome
    {
        String summary;
        double volume;
        String path;
        boolean strictValid;
    }

    static Outcome run(CsgSampleCorpus sample, int op, Matrix4x4d motion)
    {
        PolyhedralBoundedSolid[] operands =
            CsgSampleCorpusFixtures.createPair(sample);
        PolyhedralBoundedSolid a;
        PolyhedralBoundedSolid b;
        Outcome outcome = new Outcome();

        if ( motion != null ) {
            PolyhedralBoundedSolidModeler.applyTransformation(operands[0],
                motion);
            PolyhedralBoundedSolidModeler.applyTransformation(operands[1],
                motion);
        }
        a = op == BooleanOracleFuzz.B_MINUS_A ? operands[1] : operands[0];
        b = op == BooleanOracleFuzz.B_MINUS_A ? operands[0] : operands[1];
        try {
            PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler
                .setOp(a, b, op == BooleanOracleFuzz.B_MINUS_A ?
                    PolyhedralBoundedSolidModeler.SUBTRACT : op,
                    false, true, false);
            PolyhedralBoundedSolidTopologySummary topology =
                PolyhedralBoundedSolidTopologySummary.from(result);
            outcome.summary = "F=" + topology.getFaceCount() +
                " E=" + topology.getEdgeCount() +
                " V=" + topology.getVertexCount() +
                " S=" + topology.getShellCount();
            outcome.volume =
                PolyhedralBoundedSolidVolumeOracle.solidVolume(result);
            outcome.strictValid =
                PolyhedralBoundedSolidValidationEngine.validateStrict(
                    result, new StringBuilder());
        }
        catch ( Throwable t ) {
            outcome.summary = "exception " + t.getClass().getSimpleName();
            outcome.volume = Double.NaN;
        }
        outcome.path = PolyhedralBoundedSolidModeler.getLastSetOpResultPath();
        return outcome;
    }

    @Test
    void given_referencePairs_when_rotatedRigidly_then_resultsAreInvariant()
    {
        List<String> report = new ArrayList<>();
        int invariant = 0;
        int total = 0;

        for ( CsgSampleCorpus sample : CsgSampleCorpus.values() ) {
            for ( int op : OPERATIONS ) {
                Outcome plain = run(sample, op, null);
                Outcome rotated = run(sample, op, GENERIC_ROTATION);
                boolean bothRejected = Double.isNaN(plain.volume) &&
                    Double.isNaN(rotated.volume);
                boolean same = plain.summary.equals(rotated.summary) &&
                    plain.strictValid == rotated.strictValid &&
                    (bothRejected ||
                     Math.abs(plain.volume - rotated.volume) <=
                         1.0e-6 * Math.max(1.0, Math.abs(plain.volume)));
                total++;
                if ( same ) {
                    invariant++;
                }
                report.add((same ? "  same " : "  DIFF ") + sample + " " +
                    BooleanOracleFuzz.opName(op) + ": plain[" +
                    plain.summary + " vol=" + plain.volume + " strict=" +
                    plain.strictValid + " path=" + plain.path +
                    "] rotated[" + rotated.summary + " vol=" +
                    rotated.volume + " strict=" + rotated.strictValid +
                    " path=" + rotated.path + "]");
            }
        }
        System.out.println("Rigid-motion invariance: " + invariant + " / " +
            total);
        report.forEach(System.out::println);

        assertThat(invariant)
            .as("rigid-motion invariant reference cases (ratchet)")
            .isGreaterThanOrEqualTo(MINIMUM_INVARIANT);
    }
}
