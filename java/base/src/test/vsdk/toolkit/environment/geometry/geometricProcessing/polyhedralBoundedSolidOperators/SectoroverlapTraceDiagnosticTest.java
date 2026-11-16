package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;

/**
§7.3.1 diagnostic harness — captures every {@code sectoroverlap}
invocation during a boolean operation, plus the connector's
{@code endsa}/{@code endsb} survivors, and prints a structured
report.

<p>This is the input for §7.3.2 (deciding which of the three
correction alternatives to apply). The report should answer:</p>
<ul>
<li>How many times is {@code sectoroverlap} called for each pending
case?</li>
<li>Which calls hit {@code |a2-b1| ≈ 0} (boundary-ray contact)?</li>
<li>What is the decision (true/false) for each?</li>
<li>For MANT1988_15_1 INT/SUB, which calls correlate with the 4
loose survivors that end up unmatched?</li>
</ul>
 */
class SectoroverlapTraceDiagnosticTest
{
    @Test
    void trace_mant1988_15_1_intersection() throws Exception
    {
        runDiagnostic("MANT1988_15_1 + INTERSECTION",
            SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_1(),
            PolyhedralBoundedSolidModeler.INTERSECTION);
    }

    @Test
    void trace_mant1988_15_1_subtract() throws Exception
    {
        runDiagnostic("MANT1988_15_1 + SUBTRACT",
            SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_1(),
            PolyhedralBoundedSolidModeler.SUBTRACT);
    }

    @Test
    void trace_mant1988_15_1_union_for_comparison() throws Exception
    {
        // UNION passes (looseA=0) — provides a control trace.
        runDiagnostic("MANT1988_15_1 + UNION (CONTROL — passes)",
            SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_1(),
            PolyhedralBoundedSolidModeler.UNION);
    }

    @Test
    void trace_hollow_brick_intersection_for_comparison() throws Exception
    {
        // HOLLOW_BRICK INTERSECTION passes (looseA=0) — second control.
        runDiagnostic("HOLLOW_BRICK + INTERSECTION (CONTROL — passes)",
            CsgSampleCorpusFixtures.createPair(CsgSampleCorpus.HOLLOW_BRICK),
            PolyhedralBoundedSolidModeler.INTERSECTION);
    }

    private static void runDiagnostic(String label,
                                      PolyhedralBoundedSolid[] pair,
                                      int op) throws Exception
    {
        _PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .enableSectoroverlapTrace();
        try {
            PolyhedralBoundedSolidModeler.setOp(pair[0], pair[1], op, false);
        } finally {
            ArrayList<
                _PolyhedralBoundedSolidSetGeometricPredicateProcessor.SectoroverlapTraceEntry>
                trace =
                _PolyhedralBoundedSolidSetGeometricPredicateProcessor
                    .getSectoroverlapTrace();
            _PolyhedralBoundedSolidSetGeometricPredicateProcessor
                .disableSectoroverlapTrace();

            int looseA = _PolyhedralBoundedSolidSetNullEdgesConnector
                .getLastLooseACount();
            int looseB = _PolyhedralBoundedSolidSetNullEdgesConnector
                .getLastLooseBCount();

            System.out.println("\n========== " + label + " ==========");
            System.out.println("looseA=" + looseA + " looseB=" + looseB);
            System.out.println("sectoroverlap calls: " +
                (trace != null ? trace.size() : 0));

            if ( trace != null ) {
                int trueCount = 0;
                int boundaryRayCount = 0;
                int boundaryRayTrue = 0;
                for ( _PolyhedralBoundedSolidSetGeometricPredicateProcessor.SectoroverlapTraceEntry e : trace ) {
                    if ( e.decision ) trueCount++;
                    if ( e.boundaryRayContact ) {
                        boundaryRayCount++;
                        if ( e.decision ) boundaryRayTrue++;
                    }
                }
                System.out.println("  decisions: " + trueCount + " TRUE / " +
                    (trace.size() - trueCount) + " FALSE");
                System.out.println("  boundary-ray-contact calls (|a2-b1|<1e-12 " +
                    "or |b2-a1|<1e-12): " + boundaryRayCount +
                    " (of which " + boundaryRayTrue + " returned TRUE)");

                if ( !trace.isEmpty() ) {
                    System.out.println("\n  --- all entries ---");
                    System.out.printf(
                        "  %-3s | %-4s | %-4s | %-9s | %-9s | %-8s %-8s | "
                        + "%-8s %-8s | %-5s | %s%n",
                        "i", "fA", "fB", "vA(F->T)", "vB(F->T)",
                        "a1", "a2", "b1", "b2", "BRC", "dec");
                    for ( _PolyhedralBoundedSolidSetGeometricPredicateProcessor.SectoroverlapTraceEntry e : trace ) {
                        System.out.printf(
                            "  %3d | %4d | %4d | %3d->%-3d | %3d->%-3d | "
                            + "%+.5f %+.5f | %+.5f %+.5f | %-5s | %s%n",
                            e.callIndex, e.faceA, e.faceB,
                            e.vertexAFrom, e.vertexATo,
                            e.vertexBFrom, e.vertexBTo,
                            e.a1, e.a2, e.b1, e.b2,
                            e.boundaryRayContact ? "YES" : "no",
                            e.decision ? "T" : "F");
                    }
                }
            }

            System.out.println("================================\n");
        }
    }

}
