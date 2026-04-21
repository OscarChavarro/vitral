package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

/**
Diagnostic trace harness for a boolean case whose classification/connect
behavior is under active investigation.

<p>Traceability: [MANT1988] Ch. 15.5-15.8, covering setopgenerate,
classify, connect, and finish phases.</p>
 */
class PolyhedralBoundedSolidSetOperatorCrossPairDiagnosticTest
{
    private static final String TRACE_PIPELINE_SUMMARY_PROPERTY =
        "vsdk.setop.tracePipelineSummary";

    @Test
    void given_crossPair_when_runningBooleanDiagnostics_then_printsClassificationConnectAndFinishState()
    {
        runCase("UNION", false, PolyhedralBoundedSolidModeler.UNION);
        runCase("INTERSECTION", false, PolyhedralBoundedSolidModeler.INTERSECTION);
        runCase("SUBTRACT_AB", false, PolyhedralBoundedSolidModeler.SUBTRACT);
        runCase("SUBTRACT_BA", true, PolyhedralBoundedSolidModeler.SUBTRACT);
    }

    private static void runCase(String label, boolean swapOperands, int op)
    {
        String oldPipelineTrace = System.getProperty(TRACE_PIPELINE_SUMMARY_PROPERTY);
        PolyhedralBoundedSolid[] pair = CsgSampleCorpusFixtures.createPair(
            CsgSampleCorpus.CROSS_PAIR);
        PolyhedralBoundedSolid solidA = swapOperands ? pair[1] : pair[0];
        PolyhedralBoundedSolid solidB = swapOperands ? pair[0] : pair[1];

        try {
            System.setProperty(TRACE_PIPELINE_SUMMARY_PROPERTY, "true");
            System.out.println("=== CROSS_PAIR " + label + " ===");
            printSolid("inputA", solidA);
            printSolid("inputB", solidB);

            PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
                solidA, solidB, op, false);

            printSolid("result", result);
            printSolid("mutatedA", solidA);
            printSolid("mutatedB", solidB);
        }
        finally {
            if ( oldPipelineTrace == null ) {
                System.clearProperty(TRACE_PIPELINE_SUMMARY_PROPERTY);
            }
            else {
                System.setProperty(TRACE_PIPELINE_SUMMARY_PROPERTY, oldPipelineTrace);
            }
        }
    }

    private static void printSolid(String label, PolyhedralBoundedSolid solid)
    {
        boolean intermediate = PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(solid);
        boolean strict = PolyhedralBoundedSolidValidationEngine
            .validateStrict(solid);
        double[] minMax = solid.getMinMax();

        System.out.println(
            "  " + label +
            " faces=" + solid.polygonsList.size() +
            " edges=" + solid.edgesList.size() +
            " vertices=" + solid.verticesList.size() +
            " intermediate=" + intermediate +
            " strict=" + strict +
            " minmax=[" +
            minMax[0] + "," + minMax[1] + "," + minMax[2] + "," +
            minMax[3] + "," + minMax[4] + "," + minMax[5] + "]");
    }
}
