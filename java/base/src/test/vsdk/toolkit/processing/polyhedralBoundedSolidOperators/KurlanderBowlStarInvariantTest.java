package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidGeometricValidator;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

import static org.assertj.core.api.Assertions.assertThat;

/**
Protected invariant (see {@code KurlanderBowlFixPlan.md} §4-ter): every Kurlander
star motif (indices 0..19), subtracted from the bowl, must produce a valid,
non-empty, correctly-oriented solid.

<p>This locks in the result of Step 2 (commit {@code 56be7fb6}): the connect
phase now preserves the classifier emission order for all-singleton null-edge
sets, so all 20 stars close cleanly ({@code looseA == 0}). Any future change to
the boolean pipeline must keep this green.</p>

<p>The moon motifs (indices 20..39) are <b>not</b> covered here on purpose; they
are the open investigation of §4-bis. Their status is tracked by the {@code
ENABLED[]}/{@code assumeTrue} mechanism in {@link KurlanderMotif4OperationMatrixTest}.</p>
*/
@Tag("slow")
class KurlanderBowlStarInvariantTest
{
    @ParameterizedTest(name = "STAR motif {0}")
    @ValueSource(ints = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
        10, 11, 12, 13, 14, 15, 16, 17, 18, 19
    })
    void given_kurlanderBowlAndStar_when_subtracting_then_resultIsValidNonEmptyAndOriented(
        int motif)
    {
        PolyhedralBoundedSolid[] operands =
            CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(motif);
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            operands[0], operands[1], PolyhedralBoundedSolidModeler.SUBTRACT,
            false);

        assertThat(result)
            .as("star %d: A-B must not be null", motif)
            .isNotNull();
        assertThat(result.getPolygonsList().size())
            .as("star %d: A-B must be non-empty (object must not disappear)", motif)
            .isGreaterThan(0);
        assertThat(PolyhedralBoundedSolidValidationEngine.validateIntermediate(
                result))
            .as("star %d: A-B must pass validateIntermediate", motif)
            .isTrue();

        StringBuilder orientationMessage = new StringBuilder();
        boolean orientationOk =
            PolyhedralBoundedSolidGeometricValidator
                .validateConsistentFaceOrientations(result, orientationMessage);
        assertThat(orientationOk)
            .as("star %d: A-B must have no inverted (black) faces. %s",
                motif, orientationMessage.toString())
            .isTrue();
    }
}
