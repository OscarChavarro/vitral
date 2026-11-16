package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTestFixtures;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolyhedralBoundedSolidStrictSetOpTest
{
    @Test
    void given_defaultOverloadAndExplicitTrue_when_run_then_resultsAreEquivalent()
    {
        PolyhedralBoundedSolidValidationEngine
            .resetStrictValidationInvocationCount();
        PolyhedralBoundedSolid[] oldOperands =
            PolyhedralBoundedSolidTestFixtures.createDisjointBoxPair();
        PolyhedralBoundedSolid[] explicitTrueOperands =
            PolyhedralBoundedSolidTestFixtures.createDisjointBoxPair();

        PolyhedralBoundedSolid oldResult =
            PolyhedralBoundedSolidModeler.setOp(
                oldOperands[0], oldOperands[1],
                PolyhedralBoundedSolidModeler.UNION, false, true);
        PolyhedralBoundedSolid explicitTrueResult =
            PolyhedralBoundedSolidModeler.setOp(
                explicitTrueOperands[0], explicitTrueOperands[1],
                PolyhedralBoundedSolidModeler.UNION, false, true, true);

        assertEquivalentShape(oldResult, explicitTrueResult);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .getStrictValidationInvocationCount()).isEqualTo(2L);
    }

    @Test
    void given_validDisjointPreflight_when_strictEnabled_then_resultReturns()
    {
        PolyhedralBoundedSolidValidationEngine
            .resetStrictValidationInvocationCount();
        PolyhedralBoundedSolid[] operands =
            PolyhedralBoundedSolidTestFixtures.createDisjointBoxPair();

        PolyhedralBoundedSolid result =
            PolyhedralBoundedSolidModeler.setOp(
                operands[0], operands[1],
                PolyhedralBoundedSolidModeler.UNION, false, true, true);

        assertThat(result).isNotNull();
        assertThat(result.getPolygonsList().size()).isEqualTo(12);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .getStrictValidationInvocationCount()).isEqualTo(1L);
    }

    @Test
    void given_currentPseudomanifoldResult_when_strictEnabled_then_itFailsAtSetOpGateway()
    {
        PolyhedralBoundedSolid[] operands =
            PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(0);

        assertThatThrownBy(() -> PolyhedralBoundedSolidModeler.setOp(
            operands[0], operands[1],
            PolyhedralBoundedSolidModeler.UNION, false, true, true))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Strict boolean result validation failed")
            .hasMessageContaining("op=UNION")
            .hasMessageContaining("path=")
            .hasMessageContaining("operandA={faces=")
            .hasMessageContaining("result={faces=")
            .hasMessageContaining("TopologySummary{")
            .hasMessageContaining("adjustedEuler=");
    }

    private static void assertEquivalentShape(
        PolyhedralBoundedSolid first,
        PolyhedralBoundedSolid second)
    {
        assertThat(first.getPolygonsList().size())
            .isEqualTo(second.getPolygonsList().size());
        assertThat(first.getEdgesList().size())
            .isEqualTo(second.getEdgesList().size());
        assertThat(first.getVerticesList().size())
            .isEqualTo(second.getVerticesList().size());
        assertThat(first.getMinMax()).containsExactly(second.getMinMax());
    }
}
