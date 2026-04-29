package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CsgKurlanderBowlAllMotifsRegressionTest
{
    private static final double GEOMETRY_TOLERANCE = 1.0e-9;

    @Test
    void given_kurlanderShellAndFirstMoon_when_subtractingMoonFromShell_then_resultStaysValid()
    {
        PolyhedralBoundedSolid[] operands =
            CsgKurlanderBowlFixture.createShellAndFirstMoonOperands();
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            operands[0], operands[1], PolyhedralBoundedSolidModeler.SUBTRACT,
            false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPolygonsList().size()).isGreaterThan(0);
        assertThat(result.getEdgesList().size()).isGreaterThan(0);
        assertThat(result.getVerticesList().size()).isGreaterThan(0);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
    }

    @Test
    void given_kurlanderFirstStarPlacement_whenCreated_thenTopTipStaysUprightAgainstZ()
    {
        Matrix4x4 placement =
            CsgKurlanderBowlFixture.createStarPlacementTransformation(
                9.0, -90.0);
        Vector3D origin = placement.multiply(new Vector3D());
        Vector3D extrusionAxis = placement.multiply(
            new Vector3D(0.0, 0.0, 0.55)).subtract(origin);
        Vector3D topTip = placement.multiply(
            new Vector3D(0.0, -0.2, 0.0)).subtract(origin);

        assertVectorClose(origin, new Vector3D(0.0, -0.6, 0.9));
        assertVectorClose(extrusionAxis, new Vector3D(0.0, -0.55, 0.0));
        assertVectorClose(topTip, new Vector3D(0.0, 0.0, 0.2));
    }

    @Test
    void given_kurlanderFirstMoonOperand_whenCreated_thenMoonIsRolledAndInsetIntoBowl()
    {
        int firstMoonIndex = CsgKurlanderBowlFixture.getSingleMotifStarCount();
        PolyhedralBoundedSolid[] operands =
            CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(
                firstMoonIndex);
        PolyhedralBoundedSolid moon = operands[1];
        double[] minMax = moon.getMinMax();
        Matrix4x4 placement =
            CsgKurlanderBowlFixture.createMoonPlacementTransformation(
                4.0, -90.0);
        Vector3D origin = placement.multiply(new Vector3D());
        Vector3D cylinderAxis = placement.multiply(
            new Vector3D(0.0, 0.0, 0.5)).subtract(origin);
        Vector3D crescentOffset = placement.multiply(
            new Vector3D(0.11, 0.0, 0.06)).subtract(origin);

        assertThat(moon.getVerticesList().size()).isGreaterThan(0);
        assertThat(minMax[1]).isCloseTo(-1.04, within(GEOMETRY_TOLERANCE));
        assertThat(minMax[4]).isCloseTo(-0.54, within(GEOMETRY_TOLERANCE));
        assertVectorClose(origin, new Vector3D(0.0, -0.54, 0.4));
        assertVectorClose(cylinderAxis, new Vector3D(0.0, -0.5, 0.0));
        assertVectorClose(crescentOffset, new Vector3D(0.11, -0.06, 0.0));
    }

    private static void assertVectorClose(Vector3D actual, Vector3D expected)
    {
        assertThat(actual.x()).isCloseTo(expected.x(),
            within(GEOMETRY_TOLERANCE));
        assertThat(actual.y()).isCloseTo(expected.y(),
            within(GEOMETRY_TOLERANCE));
        assertThat(actual.z()).isCloseTo(expected.z(),
            within(GEOMETRY_TOLERANCE));
    }
}
