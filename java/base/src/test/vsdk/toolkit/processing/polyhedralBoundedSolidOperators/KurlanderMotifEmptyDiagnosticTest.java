package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

/**
Diagnostic: why do certain Kurlander motifs produce empty SUBTRACT results?
Not a regression guard — prints findings and passes unconditionally.
Delete once the root cause is documented and fixed.
*/
class KurlanderMotifEmptyDiagnosticTest
{
    // single EMPTY case for pipeline trace (keep trace output manageable)
    private static final int[] EMPTY_MOTIFS = { 24 };
    // representative OK motif for comparison
    private static final int[] OK_MOTIFS = { 21 };

    @Test
    void diagnose_emptyMotifCases_printBboxAndPreflightInfo()
    {
        System.out.println("=== EMPTY MOTIF DIAGNOSTIC ===");
        System.out.println("Format: motif | label | bowlBbox | motifBbox | overlap | "
            + "bowlF | motifV | bowlV | result_faces");

        printMotifGroup("EMPTY", EMPTY_MOTIFS);
        printMotifGroup("OK   ", OK_MOTIFS);

        System.out.println("=== END DIAGNOSTIC ===");
        // always pass — this is a diagnostic, not a regression guard
    }

    private static void printMotifGroup(String groupLabel, int[] indices)
    {
        for ( int motif : indices ) {
            System.setProperty("vsdk.setop.tracePipelineSummary", "true");
            try {
                PolyhedralBoundedSolid[] operands =
                    CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(motif);
                PolyhedralBoundedSolid bowl = operands[0];
                PolyhedralBoundedSolid motifSolid = operands[1];

                double[] bb = bowl.getMinMax();
                double[] mb = motifSolid.getMinMax();
                boolean overlap = bboxOverlap(bb, mb);

                PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
                    bowl, motifSolid, PolyhedralBoundedSolidModeler.SUBTRACT, false);

                int resultFaces = result == null ? -1 : result.getPolygonsList().size();
                boolean resultValid = resultFaces > 0 &&
                    PolyhedralBoundedSolidValidationEngine.validateIntermediate(result);

                System.out.printf("[%s] motif=%2d (%s)%n"
                    + "      bowl bbox=[%.3f..%.3f, %.3f..%.3f, %.3f..%.3f] F=%d V=%d%n"
                    + "      motif bbox=[%.3f..%.3f, %.3f..%.3f, %.3f..%.3f] F=%d V=%d%n"
                    + "      bboxOverlap=%b  resultFaces=%d  resultValid=%b%n",
                    groupLabel, motif, CsgKurlanderBowlFixture.describeSingleMotif(motif),
                    bb[0], bb[3], bb[1], bb[4], bb[2], bb[5],
                    bowl.getPolygonsList().size(), bowl.getVerticesList().size(),
                    mb[0], mb[3], mb[1], mb[4], mb[2], mb[5],
                    motifSolid.getPolygonsList().size(), motifSolid.getVerticesList().size(),
                    overlap, resultFaces, resultValid);
            }
            catch ( Throwable t ) {
                System.out.printf("[%s] motif=%2d EXCEPTION: %s%n",
                    groupLabel, motif, t.getMessage());
            }
            finally {
                System.clearProperty("vsdk.setop.tracePipelineSummary");
            }
        }
    }

    private static boolean bboxOverlap(double[] a, double[] b)
    {
        // a/b are [minX, minY, minZ, maxX, maxY, maxZ]
        return a[3] > b[0] && b[3] > a[0]
            && a[4] > b[1] && b[4] > a[1]
            && a[5] > b[2] && b[5] > a[2];
    }
}
