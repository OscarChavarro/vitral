package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidVolumeOracle.OrientedBox;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
The one decision on whether two faces coincide
({@link _PolyhedralBoundedSolidSetOperandAligner}): a face of B parallel to
a face of A, next to it and closer than two tolerances is put on its plane
by translating B, before any stage of the set operation runs.

<p>The cases: inside the band B moves by exactly the gap; outside the band
and at exact contact nothing moves; two gaps along two directions are both
closed; two different gaps along one direction cannot be, and nothing
moves; the caller's operands are never touched by `setOp`.</p>
 */
class SetOperandAlignerTest
{
    private static final double[] UNIT = {0, 0, 0, 1, 1, 1};

    @Test
    void given_faceInsideTheBand_when_aligned_then_bMovesByTheGap()
    {
        double tolerance = tolerance();

        for ( double gap : new double[] {-1.5, -0.3, 0.4, 1.7} ) {
            PolyhedralBoundedSolid a = box(UNIT);
            PolyhedralBoundedSolid b = box(new double[] {0.4, 0.3,
                1 + gap * tolerance, 1.4, 1.3, 1.6 + gap * tolerance});

            Vector3Dd moved = _PolyhedralBoundedSolidSetOperandAligner
                .align(a, b);

            assertThat(moved).as("gap of " + gap + " tolerances")
                .isNotNull();
            assertThat(moved.x()).isCloseTo(0.0, offset(1.0e-15));
            assertThat(moved.y()).isCloseTo(0.0, offset(1.0e-15));
            assertThat(moved.z()).isCloseTo(-gap * tolerance,
                offset(1.0e-9 * tolerance));
            assertThat(b.getMinMax()[2]).isCloseTo(1.0, offset(1.0e-15));
        }
    }

    @Test
    void given_faceOutsideTheBandOrInContact_when_aligned_then_nothingMoves()
    {
        double tolerance = tolerance();

        for ( double gap : new double[] {-10, -3, 0, 3, 10} ) {
            PolyhedralBoundedSolid b = box(new double[] {0.4, 0.3,
                1 + gap * tolerance, 1.4, 1.3, 1.6 + gap * tolerance});
            double before = b.getMinMax()[2];

            assertThat(_PolyhedralBoundedSolidSetOperandAligner.align(
                box(UNIT), b)).as("gap of " + gap + " tolerances").isNull();
            assertThat(b.getMinMax()[2]).isEqualTo(before);
        }
    }

    /** B sits in a corner of an L of two boxes: two gaps, two directions. */
    @Test
    void given_gapsAlongTwoDirections_when_aligned_then_bothAreClosed()
    {
        double tolerance = tolerance();
        PolyhedralBoundedSolid a = box(UNIT);
        PolyhedralBoundedSolid b = box(new double[] {
            1 + 0.7 * tolerance, 0.2, 0.5 - 1.2 * tolerance,
            1.8 + 0.7 * tolerance, 0.9, 1 - 1.2 * tolerance});

        Vector3Dd moved = _PolyhedralBoundedSolidSetOperandAligner.align(a,
            b);

        assertThat(moved).isNotNull();
        assertThat(moved.x()).isCloseTo(-0.7 * tolerance,
            offset(1.0e-9 * tolerance));
        assertThat(moved.z()).isCloseTo(1.2 * tolerance,
            offset(1.0e-9 * tolerance));
        assertThat(moved.y()).isCloseTo(0.0, offset(1.0e-15));
    }

    /**
    B stands between the top of A and the bottom of a second piece of A,
    a tolerance short of each on different amounts: no translation closes
    both gaps, so B stays where it is.
    */
    @Test
    void given_twoGapsAlongOneDirection_when_aligned_then_nothingMoves()
    {
        double tolerance = tolerance();
        PolyhedralBoundedSolid a = PolyhedralBoundedSolidModeler.setOp(
            box(UNIT), box(new double[] {0, 0, 2, 1, 1, 3}),
            PolyhedralBoundedSolidModeler.UNION);
        PolyhedralBoundedSolid b = box(new double[] {0.2, 0.2,
            1 + 0.5 * tolerance, 0.8, 0.8, 2 - 1.5 * tolerance});
        double before = b.getMinMax()[2];

        assertThat(_PolyhedralBoundedSolidSetOperandAligner.align(a, b))
            .isNull();
        assertThat(b.getMinMax()[2]).isEqualTo(before);
    }

    @Test
    void given_operandsInsideTheBand_when_setOp_then_answeredAndOperandsUntouched()
    {
        double tolerance = tolerance();
        PolyhedralBoundedSolid a = box(UNIT);
        PolyhedralBoundedSolid b = box(new double[] {0.4, 0.3,
            1 - 0.5 * tolerance, 1.4, 1.3, 1.6 - 0.5 * tolerance});
        double bottomOfB = b.getMinMax()[2];

        PolyhedralBoundedSolid union = PolyhedralBoundedSolidModeler.setOp(a,
            b, PolyhedralBoundedSolidModeler.UNION);

        assertThat(PolyhedralBoundedSolidVolumeOracle.solidVolume(union))
            .isCloseTo(1.0 + 0.6, offset(2.0 * tolerance));
        assertThat(b.getMinMax()[2]).isEqualTo(bottomOfB);
    }

    /**
    B inside A with one face on a face of A: A - B is A with a pocket open
    to the outside (11 faces), not A with a cavity. The containment
    decision used to answer it with a cavity, which strict validation
    rejected.
    */
    @Test
    void given_boxInsideTouchingAFace_when_subtracted_then_resultIsAPocket()
    {
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            box(UNIT), box(new double[] {0.2, 0.3, 0.6, 0.7, 0.8, 1}),
            PolyhedralBoundedSolidModeler.SUBTRACT);

        assertThat(PolyhedralBoundedSolidVolumeOracle.solidVolume(result))
            .isCloseTo(1.0 - 0.5 * 0.5 * 0.4, offset(1.0e-12));
        assertThat(result.getPolygonsList().size()).isEqualTo(11);
        assertThat(PolyhedralBoundedSolidModeler.getLastSetOpResultPath())
            .isEqualTo("normal-pipeline");
    }

    private static double tolerance()
    {
        return PolyhedralBoundedSolidNumericPolicy.forSolids(box(UNIT),
            box(new double[] {0.4, 0.3, 1, 1.4, 1.3, 1.6})).bigEpsilon();
    }

    private static PolyhedralBoundedSolid box(double[] corners)
    {
        return new OrientedBox(corners[3] - corners[0],
            corners[4] - corners[1], corners[5] - corners[2],
            PolyhedralBoundedSolidVolumeOracle.translation(
                (corners[0] + corners[3]) / 2.0,
                (corners[1] + corners[4]) / 2.0,
                (corners[2] + corners[5]) / 2.0)).toSolid();
    }
}
