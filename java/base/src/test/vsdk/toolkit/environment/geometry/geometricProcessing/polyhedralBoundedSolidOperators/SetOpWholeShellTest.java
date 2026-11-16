package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidVolumeOracle.OrientedBox;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologySummary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
Operands with several shells, one of which the other operand does not cross
(plan-stage11 Phase F). The finish stage moves to the result the parts that
are connected to a cut; a shell without any cut was left behind without an
error. The composite oracle found it in a chain: (A n B) u C lost a piece
of A n B that C did not touch.

<p>A is two separate unit cubes, P at the origin and Q five units away. B
crosses P only (or lies inside it). The expected volumes follow from the
boxes; the rotated run repeats them away from the axes.</p>
 */
class SetOpWholeShellTest
{
    private static final double BOX = 1.0;

    @Test
    void given_twoPieceSolidCrossedInOnePiece_when_setOp_then_theOtherPieceIsKept()
    {
        check(false);
        check(true);
    }

    /** B sticks out of P: half of it (0.108 of 0.216) is inside. */
    private static void check(boolean rotated)
    {
        double inside = 0.6 * 0.6 * 0.3;
        double b = 0.6 * 0.6 * 0.6;

        assertVolume(rotated, PolyhedralBoundedSolidModeler.UNION, false,
            2 * BOX + b - inside, 2);
        assertVolume(rotated, PolyhedralBoundedSolidModeler.INTERSECTION,
            false, inside, 1);
        assertVolume(rotated, PolyhedralBoundedSolidModeler.SUBTRACT, false,
            2 * BOX - inside, 2);
        assertVolume(rotated, PolyhedralBoundedSolidModeler.SUBTRACT, true,
            b - inside, 1);
    }

    /**
    The void left by a box removed from inside P is a shell of its own. A
    tool that crosses only the outside of P must not lose it.
    */
    @Test
    void given_solidWithACavity_when_toolCrossesTheOuterShellOnly_then_theCavityIsKept()
    {
        PolyhedralBoundedSolid hollow = PolyhedralBoundedSolidModeler.setOp(
            box(0, 0, 0, 2, 2, 2).toSolid(),
            box(0.5, 0.5, 0.5, 1.5, 1.5, 1.5).toSolid(),
            PolyhedralBoundedSolidModeler.SUBTRACT);
        assertThat(PolyhedralBoundedSolidTopologySummary.from(hollow)
            .getShellCount()).isEqualTo(2);
        PolyhedralBoundedSolid tool = box(1.7, 0.8, 0.8, 2.3, 1.2, 1.2)
            .toSolid();

        PolyhedralBoundedSolid cut = PolyhedralBoundedSolidModeler.setOp(
            hollow, tool, PolyhedralBoundedSolidModeler.SUBTRACT);

        assertThat(PolyhedralBoundedSolidVolumeOracle.solidVolume(cut))
            .isCloseTo(8.0 - 1.0 - 0.3 * 0.4 * 0.4, offset(1.0e-9));
        assertThat(PolyhedralBoundedSolidTopologySummary.from(cut)
            .getShellCount()).isEqualTo(2);
    }

    private static void assertVolume(boolean rotated, int op,
        boolean swapped, double expected, int shells)
    {
        PolyhedralBoundedSolid a = twoCubes(rotated);
        PolyhedralBoundedSolid b = place(box(0.2, 0.2, 0.7, 0.8, 0.8, 1.3),
            rotated).toSolid();
        PolyhedralBoundedSolid result = swapped ?
            PolyhedralBoundedSolidModeler.setOp(b, a, op) :
            PolyhedralBoundedSolidModeler.setOp(a, b, op);
        String label = (rotated ? "rotated " : "") +
            BooleanOracleFuzz.opName(swapped ? BooleanOracleFuzz.B_MINUS_A :
                op);

        assertThat(PolyhedralBoundedSolidVolumeOracle.solidVolume(result))
            .as(label + " volume").isCloseTo(expected, offset(1.0e-9));
        assertThat(PolyhedralBoundedSolidTopologySummary.from(result)
            .getShellCount()).as(label + " shells").isEqualTo(shells);
    }

    private static PolyhedralBoundedSolid twoCubes(boolean rotated)
    {
        PolyhedralBoundedSolid both = PolyhedralBoundedSolidModeler.setOp(
            place(box(0, 0, 0, 1, 1, 1), rotated).toSolid(),
            place(box(5, 0, 0, 6, 1, 1), rotated).toSolid(),
            PolyhedralBoundedSolidModeler.UNION);

        assertThat(PolyhedralBoundedSolidTopologySummary.from(both)
            .getShellCount()).isEqualTo(2);
        return both;
    }

    private static OrientedBox place(OrientedBox box, boolean rotated)
    {
        if ( !rotated ) {
            return box;
        }
        return new OrientedBox(box.size[0], box.size[1], box.size[2],
            PolyhedralBoundedSolidVolumeOracle.translation(0.3, -0.2, 0.5)
                .multiply(PolyhedralBoundedSolidVolumeOracle.rotation(0.83,
                    1, 2, -1.5)).multiply(box.transform));
    }

    private static OrientedBox box(double x0, double y0, double z0,
        double x1, double y1, double z1)
    {
        return new OrientedBox(x1 - x0, y1 - y0, z1 - z0,
            PolyhedralBoundedSolidVolumeOracle.translation(
                (x0 + x1) / 2.0, (y0 + y1) / 2.0, (z0 + z1) / 2.0));
    }
}
