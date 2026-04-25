package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

import static org.assertj.core.api.Assertions.assertThat;

class CsgKurlanderBowlFirstStarRegressionTest
{
    @Test
    void given_kurlanderBowlAndFirstStar_when_subtractingStarFromBowl_then_resultStaysNonEmptyAndIntermediateValid()
    {
        PolyhedralBoundedSolid[] operands =
            CsgKurlanderBowlFixture.createBowlAndFirstStarOperands();
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            operands[0], operands[1], PolyhedralBoundedSolidModeler.SUBTRACT,
            false);

        assertThat(result).isNotNull();
        assertThat(result.getPolygonsList().size()).isGreaterThan(0);
        assertThat(result.getEdgesList().size()).isGreaterThan(0);
        assertThat(result.getVerticesList().size()).isGreaterThan(0);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
    }
}
