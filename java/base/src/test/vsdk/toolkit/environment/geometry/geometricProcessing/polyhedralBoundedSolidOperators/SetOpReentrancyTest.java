package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.List;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidVolumeOracle.OrientedBox;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologySummary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;

/**
Reentrancy (Fable5.2Analysis.md Section 8 and pending item 9): a boolean
that fails in the middle of the pipeline must leave nothing behind that
changes the next one. The connect stage keeps its working state in the
connector instance, so an unrelated boolean after a failure gives the same
answer as on a fresh JVM.
 */
class SetOpReentrancyTest
{
    @Test
    void given_failedBoolean_when_unrelatedBooleanFollows_then_sameResult()
    {
        PolyhedralBoundedSolid reference = unrelatedUnion();
        PolyhedralBoundedSolidTopologySummary expected =
            PolyhedralBoundedSolidTopologySummary.from(reference);
        double expectedVolume =
            PolyhedralBoundedSolidVolumeOracle.solidVolume(reference);

        failingBoolean();
        PolyhedralBoundedSolid after = unrelatedUnion();

        assertThat(PolyhedralBoundedSolidTopologySummary.from(after))
            .hasToString(expected.toString());
        assertThat(PolyhedralBoundedSolidVolumeOracle.solidVolume(after))
            .isCloseTo(expectedVolume, offset(1.0e-9));
    }

    /**
    Case 1 of the rotated non-convex fuzz, B − A: an open defect of the
    connect stage (the pipeline loses the solid), so it throws after the
    connector has run.
    */
    private static void failingBoolean()
    {
        List<CompositeShapeOracle.Shape> shapes = BooleanCompositeOracleFuzz
            .shapesOf(BooleanCompositeOracleFuzz.Family.NON_CONVEX_ROTATED,
                1);
        PolyhedralBoundedSolid a = shapes.get(0).toSolid();
        PolyhedralBoundedSolid b = shapes.get(1).toSolid();

        assertThatThrownBy(() -> PolyhedralBoundedSolidModeler.setOp(b, a,
            PolyhedralBoundedSolidModeler.SUBTRACT))
            .isInstanceOf(PolyhedralBoundedSolidOperationException.class);
    }

    /** A rotated box pair, answered by the normal pipeline. */
    private static PolyhedralBoundedSolid unrelatedUnion()
    {
        PolyhedralBoundedSolid a = new OrientedBox(2, 2, 1,
            PolyhedralBoundedSolidVolumeOracle.translation(0, 0, 0))
            .toSolid();
        PolyhedralBoundedSolid b = new OrientedBox(1, 1, 1,
            PolyhedralBoundedSolidVolumeOracle.translation(1.0, 0.1, 0.2)
                .multiply(PolyhedralBoundedSolidVolumeOracle.rotation(0.5,
                    0, 0, 1)))
            .toSolid();
        return PolyhedralBoundedSolidModeler.setOp(a, b,
            PolyhedralBoundedSolidModeler.UNION);
    }
}
