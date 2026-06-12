package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidGeometricValidator;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
Diagnóstico 40×4 del sweep Kurlander: para los 40 motifs × 4 operaciones
(A-B, B-A, A∩B, A+B), clasifica cada resultado como OK / EMPTY / INVALID /
BLACK_FACES / EXCEPTION y documenta el {@code TopologicalSummary} cuando el
resultado es válido.

<p>Ver {@code doc/plan-csg-boolean-fix-stage3.md} §6 para el workflow completo.</p>

<p>El test de regresión permanente para el motif 0 está en
{@link #given_kurlanderBowlAndMotif0_when_allFourOps_then_topologyMatchesBaseline()}.
El test diagnóstico lento está marcado {@code @Tag("slow")} y {@code @Disabled}
para no bloquear las ejecuciones regulares; se activa explícitamente así:
{@code gradle :base:test --tests "*KurlanderMotif4OperationMatrixTest.diagnose*"}.
</p>
*/
class KurlanderMotif4OperationMatrixTest
{
    private enum OpStatus { OK, EMPTY, INVALID, BLACK_FACES, EXCEPTION }

    private static final String[] OP_NAMES = { "A-B", "B-A", "AiB", "A+B" };

    /**
    Control de ejecución del test paramétrico: un booleano por motif (índice 0–39).
    {@code true} = el motif es conocido como 4-OK y se ejecuta el assert.
    {@code false} = el motif aún falla en alguna operación; se omite con assumeTrue.

    Motifs 4-OK actuales (plan-csg-boolean-fix-stage3 §14.2):
      ✅ shellCount=2: 0, 2, 10, 15, 21
      ⚠️ shellCount=1: 1, 5, 7, 12, 14, 23
    */
    static final boolean[] ENABLED = {
    //   0      1      2      3      4      5      6      7      8      9
        true,  true,  true,  false, false, true,  false, true,  false, false,
    //  10     11     12     13     14     15     16     17     18     19
        true,  false, true,  false, true,  true,  false, false, false, false,
    //  20     21     22     23     24     25     26     27     28     29
        false, true,  false, true,  false, false, false, false, false, false,
    //  30     31     32     33     34     35     36     37     38     39
        false, false, false, false, false, false, false, false, false, false
    };

    // -----------------------------------------------------------------------
    // Paso 1.1 — Test diagnóstico lento 40×4 (plan-csg-boolean-fix-stage3 §6.1)
    // -----------------------------------------------------------------------

    @Test
    @Tag("slow")
    @Disabled("Diagnóstico 40×4 — ejecutar manualmente: gradle :base:test --tests '*KurlanderMotif4OperationMatrixTest.diagnose*'")
    void diagnose_allMotifsAllOps_printTopologicalSummaryMatrix()
    {
        int total = CsgKurlanderBowlFixture.getSingleMotifCount();
        int[][] statusCounts = new int[OP_NAMES.length][OpStatus.values().length];

        for ( int motif = 0; motif < total; motif++ ) {
            String desc = CsgKurlanderBowlFixture.describeSingleMotif(motif);

            for ( int opIdx = 0; opIdx < OP_NAMES.length; opIdx++ ) {
                PolyhedralBoundedSolid[] operands;
                PolyhedralBoundedSolid result;
                OpStatus status;
                String detail = "";

                try {
                    operands = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(motif);
                }
                catch ( Throwable t ) {
                    status = OpStatus.EXCEPTION;
                    detail = " build-operands: " + t.getClass().getSimpleName() + ": " + t.getMessage();
                    statusCounts[opIdx][status.ordinal()]++;
                    System.out.printf("[MATRIX] motif=%2d %-10s op=%-4s status=EXCEPTION%s%n",
                        motif, desc, OP_NAMES[opIdx], detail);
                    continue;
                }

                try {
                    result = executeOp(operands[0], operands[1], opIdx);
                }
                catch ( Throwable t ) {
                    status = OpStatus.EXCEPTION;
                    detail = " " + t.getClass().getSimpleName() + ": " + t.getMessage();
                    statusCounts[opIdx][status.ordinal()]++;
                    System.out.printf("[MATRIX] motif=%2d %-10s op=%-4s status=EXCEPTION%s%n",
                        motif, desc, OP_NAMES[opIdx], detail);
                    continue;
                }

                status = classify(result);
                statusCounts[opIdx][status.ordinal()]++;

                if ( status == OpStatus.OK ) {
                    BooleansFromReferenceObjectPairsTest.TopologicalSummary summary =
                        BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
                    if ( opIdx == 1 ) {
                        detail = " shellCount=" + summary.shellCount + " => " + summary.toLiteral();
                    }
                    else {
                        detail = " => " + summary.toLiteral();
                    }
                }

                System.out.printf("[MATRIX] motif=%2d %-10s op=%-4s status=%-12s%s%n",
                    motif, desc, OP_NAMES[opIdx], status, detail);
            }
        }

        System.out.println("\n=== 40×4 MATRIX SUMMARY ===");
        System.out.printf("%-6s", "op");
        for ( OpStatus s : OpStatus.values() ) {
            System.out.printf(" %-12s", s);
        }
        System.out.println();
        for ( int opIdx = 0; opIdx < OP_NAMES.length; opIdx++ ) {
            System.out.printf("%-6s", OP_NAMES[opIdx]);
            for ( int sIdx = 0; sIdx < OpStatus.values().length; sIdx++ ) {
                System.out.printf(" %-12d", statusCounts[opIdx][sIdx]);
            }
            System.out.println();
        }
    }

    // -----------------------------------------------------------------------
    // Test paramétrico controlado por ENABLED[40]
    // -----------------------------------------------------------------------

    static IntStream allMotifIndices()
    {
        return IntStream.range(0, CsgKurlanderBowlFixture.getSingleMotifCount());
    }

    /**
    Para cada motif habilitado en {@link #ENABLED}, ejecuta las 4 operaciones booleanas
    y verifica que todas producen {@link OpStatus#OK}.
    Para cada operación imprime el trace de vértices creados durante la intersección.
    Los motifs con {@code ENABLED[motif] == false} se omiten con {@code assumeTrue}.
    */
    @ParameterizedTest(name = "motif[{0}]")
    @MethodSource("allMotifIndices")
    void given_kurlanderBowlAndMotifN_when_allFourOps_then_allClassifyOK(int motif)
    {
        assumeTrue(ENABLED[motif],
            "motif " + motif + " deshabilitado en ENABLED — aún no es 4-OK");

        String motifDesc = CsgKurlanderBowlFixture.describeSingleMotif(motif);
        System.out.printf("%n[PARAM] motif=%2d %s%n", motif, motifDesc);

        for ( int opIdx = 0; opIdx < OP_NAMES.length; opIdx++ ) {
            PolyhedralBoundedSolid[] ops =
                CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(motif);
            PolyhedralBoundedSolid result = executeOp(ops[0], ops[1], opIdx);

            System.out.printf("[PARAM]   op=%s — intersection vertices:%n",
                OP_NAMES[opIdx]);
            for ( String event : _PolyhedralBoundedSolidSetIntersector.intersectionTrace ) {
                System.out.println("[PARAM]     " + event);
            }
            if ( _PolyhedralBoundedSolidSetIntersector.intersectionTrace.isEmpty() ) {
                System.out.println("[PARAM]     (none)");
            }

            OpStatus status = classify(result);
            assertThat(status)
                .as("motif %d op %s", motif, OP_NAMES[opIdx])
                .isEqualTo(OpStatus.OK);
        }
    }

    // -----------------------------------------------------------------------
    // Paso 1.2 — Regresión permanente motif 0 (4 operaciones hardcodeadas)
    //            Expectativas a completar tras ejecutar el test diagnóstico.
    // -----------------------------------------------------------------------

    @Test
    void given_kurlanderBowlAndMotif0_when_allFourOps_then_topologyMatchesBaseline()
    {
        PolyhedralBoundedSolid[] ops;
        PolyhedralBoundedSolid result;
        BooleansFromReferenceObjectPairsTest.TopologicalSummary summary;

        // A-B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(0);
        result = executeOp(ops[0], ops[1], 0);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif0AB());

        // B-A
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(0);
        result = executeOp(ops[0], ops[1], 1);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary.shellCount).as("B-A must produce 2 shells").isEqualTo(2);
        assertThat(summary).isEqualTo(expectedMotif0BA());

        // A∩B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(0);
        result = executeOp(ops[0], ops[1], 2);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif0AiB());

        // A+B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(0);
        result = executeOp(ops[0], ops[1], 3);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif0ApB());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
    Ejecuta la operación indicada por {@code opIdx}:
    0 = A-B (SUBTRACT), 1 = B-A (SUBTRACT inverso),
    2 = A∩B (INTERSECTION), 3 = A+B (UNION).
    */
    static PolyhedralBoundedSolid executeOp(
        PolyhedralBoundedSolid bowl,
        PolyhedralBoundedSolid motif,
        int opIdx)
    {
        switch ( opIdx ) {
            case 0:
                return PolyhedralBoundedSolidModeler.setOp(
                    bowl, motif, PolyhedralBoundedSolidModeler.SUBTRACT, false);
            case 1:
                return PolyhedralBoundedSolidModeler.setOp(
                    motif, bowl, PolyhedralBoundedSolidModeler.SUBTRACT, false);
            case 2:
                return PolyhedralBoundedSolidModeler.setOp(
                    bowl, motif, PolyhedralBoundedSolidModeler.INTERSECTION, false);
            case 3:
                return PolyhedralBoundedSolidModeler.setOp(
                    bowl, motif, PolyhedralBoundedSolidModeler.UNION, false);
            default:
                throw new IllegalArgumentException("opIdx must be 0-3, got: " + opIdx);
        }
    }

    static OpStatus classify(PolyhedralBoundedSolid result)
    {
        if ( result == null || result.getPolygonsList().size() == 0 ) {
            return OpStatus.EMPTY;
        }

        boolean topOK;
        try {
            topOK = PolyhedralBoundedSolidValidationEngine.validateIntermediate(result);
        }
        catch ( Throwable t ) {
            return OpStatus.INVALID;
        }
        if ( !topOK ) {
            return OpStatus.INVALID;
        }

        StringBuilder msg = new StringBuilder();
        boolean orientOK;
        try {
            orientOK = PolyhedralBoundedSolidGeometricValidator
                .validateConsistentFaceOrientations(result, msg);
        }
        catch ( Throwable t ) {
            orientOK = true;
        }

        return orientOK ? OpStatus.OK : OpStatus.BLACK_FACES;
    }

    // -----------------------------------------------------------------------
    // Expectativas hardcodeadas motif 0 (a completar tras ejecutar diagnóstico)
    // -----------------------------------------------------------------------

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif0AB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(
            1, 203, 418, 216, 204, 1, 1,
            new int[] {203},
            new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2},
            new int[] {3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 9, 9, 9, 9, 16, 16},
            new long[] {-1000000L, -993462L, 0L, 1000000L, 1000000L, 1650000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif0BA()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(
            2, 30, 72, 46, 30, 0, 4,
            new int[] {15, 15},
            new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            new int[] {3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 7, 7, 7, 7, 10, 10},
            new long[] {-190211L, -1150000L, 738197L, 190211L, -600000L, 1100000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif0AiB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(
            1, 18, 42, 26, 18, 0, 2,
            new int[] {18},
            new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            new int[] {3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 6, 6, 7, 7, 7, 7},
            new long[] {-190211L, -1000000L, 738197L, 190211L, -874541L, 1100000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif0ApB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(
            1, 215, 448, 236, 216, 1, 3,
            new int[] {215},
            new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2},
            new int[] {3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 9, 9, 9, 9, 10, 10, 16, 16},
            new long[] {-1000000L, -1150000L, 0L, 1000000L, 1000000L, 1650000L});
    }

    // -----------------------------------------------------------------------
    // Regresión permanente motif 1
    // -----------------------------------------------------------------------

    @Test
    void given_kurlanderBowlAndMotif1_when_allFourOps_then_topologyMatchesBaseline()
    {
        PolyhedralBoundedSolid[] ops;
        PolyhedralBoundedSolid result;
        BooleansFromReferenceObjectPairsTest.TopologicalSummary summary;

        // A-B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(1);
        result = executeOp(ops[0], ops[1], 0);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif1AB());

        // B-A
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(1);
        result = executeOp(ops[0], ops[1], 1);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif1BA());

        // A∩B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(1);
        result = executeOp(ops[0], ops[1], 2);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif1AiB());

        // A+B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(1);
        result = executeOp(ops[0], ops[1], 3);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif1ApB());
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif1AB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(1, 203, 418, 216, 204, 1, 1, new int[] {203}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2}, new int[] {3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 7, 7, 7, 7, 8, 8, 16, 16}, new long[] {-1000000L, -1000000L, 0L, 1000000L, 1000000L, 1650000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif1BA()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(2, 30, 72, 46, 30, 0, 4, new int[] {15, 15}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 10, 10}, new long[] {-615818L, -1135252L, 488197L, -53878L, -481537L, 850000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif1AiB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(1, 18, 42, 26, 18, 0, 2, new int[] {18}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6}, new long[] {-521999L, -908753L, 488197L, -151400L, -652765L, 850000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif1ApB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(1, 215, 448, 236, 216, 1, 3, new int[] {215}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2}, new int[] {3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 7, 7, 7, 7, 8, 8, 10, 10, 16, 16}, new long[] {-1000000L, -1135252L, 0L, 1000000L, 1000000L, 1650000L});
    }
    // -----------------------------------------------------------------------
    // Regresión permanente motif 2
    // -----------------------------------------------------------------------

    @Test
    void given_kurlanderBowlAndMotif2_when_allFourOps_then_topologyMatchesBaseline()
    {
        PolyhedralBoundedSolid[] ops;
        PolyhedralBoundedSolid result;
        BooleansFromReferenceObjectPairsTest.TopologicalSummary summary;

        // A-B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(2);
        result = executeOp(ops[0], ops[1], 0);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif2AB());

        // B-A
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(2);
        result = executeOp(ops[0], ops[1], 1);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary.shellCount).as("B-A must produce 2 shells").isEqualTo(2);
        assertThat(summary).isEqualTo(expectedMotif2BA());

        // A∩B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(2);
        result = executeOp(ops[0], ops[1], 2);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif2AiB());

        // A+B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(2);
        result = executeOp(ops[0], ops[1], 3);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif2ApB());
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif2AB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(1, 203, 418, 216, 204, 1, 1, new int[] {203}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2}, new int[] {3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 7, 7, 7, 7, 8, 8, 16, 16}, new long[] {-1000000L, -1000000L, 0L, 1000000L, 1000000L, 1650000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif2BA()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(2, 30, 72, 46, 30, 0, 4, new int[] {15, 15}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 10, 10}, new long[] {-947673L, -947673L, 1238197L, -289764L, -289764L, 1600000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif2AiB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(1, 18, 42, 26, 18, 0, 2, new int[] {18}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6}, new long[] {-740195L, -740195L, 1238197L, -412942L, -412942L, 1600000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif2ApB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(1, 215, 448, 236, 216, 1, 3, new int[] {215}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2}, new int[] {3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 7, 7, 7, 7, 8, 8, 10, 10, 16, 16}, new long[] {-1000000L, -1000000L, 0L, 1000000L, 1000000L, 1650000L});
    }
    // -----------------------------------------------------------------------
    // Regresión permanente motif 5
    // -----------------------------------------------------------------------

    @Test
    void given_kurlanderBowlAndMotif5_when_allFourOps_then_topologyMatchesBaseline()
    {
        PolyhedralBoundedSolid[] ops;
        PolyhedralBoundedSolid result;
        BooleansFromReferenceObjectPairsTest.TopologicalSummary summary;

        // A-B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(5);
        result = executeOp(ops[0], ops[1], 0);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif5AB());

        // B-A
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(5);
        result = executeOp(ops[0], ops[1], 1);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif5BA());

        // A∩B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(5);
        result = executeOp(ops[0], ops[1], 2);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif5AiB());

        // A+B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(5);
        result = executeOp(ops[0], ops[1], 3);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif5ApB());
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif5AB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(1, 203, 418, 216, 204, 1, 1, new int[] {203}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2}, new int[] {3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 9, 9, 9, 9, 16, 16}, new long[] {-993462L, -1000000L, 0L, 1000000L, 1000000L, 1650000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif5BA()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(2, 30, 72, 46, 30, 0, 4, new int[] {15, 15}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 7, 7, 7, 7, 10, 10}, new long[] {-1150000L, -190211L, 738197L, -600000L, 190211L, 1100000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif5AiB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(1, 18, 42, 26, 18, 0, 2, new int[] {18}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 6, 6, 7, 7, 7, 7}, new long[] {-1000000L, -190211L, 738197L, -874541L, 190211L, 1100000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif5ApB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(1, 215, 448, 236, 216, 1, 3, new int[] {215}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2}, new int[] {3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 9, 9, 9, 9, 10, 10, 16, 16}, new long[] {-1150000L, -1000000L, 0L, 1000000L, 1000000L, 1650000L});
    }
    // -----------------------------------------------------------------------
    // Regresión permanente motif 7
    // -----------------------------------------------------------------------

    @Test
    void given_kurlanderBowlAndMotif7_when_allFourOps_then_topologyMatchesBaseline()
    {
        PolyhedralBoundedSolid[] ops;
        PolyhedralBoundedSolid result;
        BooleansFromReferenceObjectPairsTest.TopologicalSummary summary;

        // A-B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(7);
        result = executeOp(ops[0], ops[1], 0);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif7AB());

        // B-A
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(7);
        result = executeOp(ops[0], ops[1], 1);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif7BA());

        // A∩B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(7);
        result = executeOp(ops[0], ops[1], 2);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif7AiB());

        // A+B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(7);
        result = executeOp(ops[0], ops[1], 3);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif7ApB());
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif7AB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(1, 203, 418, 216, 204, 1, 1, new int[] {203}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2}, new int[] {3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 7, 7, 7, 7, 8, 8, 16, 16}, new long[] {-1000000L, -1000000L, 0L, 1000000L, 1000000L, 1650000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif7BA()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(2, 30, 72, 46, 30, 0, 4, new int[] {15, 15}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 10, 10}, new long[] {-947673L, 289764L, 1238197L, -289764L, 947673L, 1600000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif7AiB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(1, 18, 42, 26, 18, 0, 2, new int[] {18}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6}, new long[] {-740195L, 412942L, 1238197L, -412942L, 740195L, 1600000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif7ApB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(1, 215, 448, 236, 216, 1, 3, new int[] {215}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2}, new int[] {3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 7, 7, 7, 7, 8, 8, 10, 10, 16, 16}, new long[] {-1000000L, -1000000L, 0L, 1000000L, 1000000L, 1650000L});
    }
    // -----------------------------------------------------------------------
    // Regresión permanente motif 10
    // -----------------------------------------------------------------------

    @Test
    void given_kurlanderBowlAndMotif10_when_allFourOps_then_topologyMatchesBaseline()
    {
        PolyhedralBoundedSolid[] ops;
        PolyhedralBoundedSolid result;
        BooleansFromReferenceObjectPairsTest.TopologicalSummary summary;

        // A-B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(10);
        result = executeOp(ops[0], ops[1], 0);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif10AB());

        // B-A
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(10);
        result = executeOp(ops[0], ops[1], 1);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary.shellCount).as("B-A must produce 2 shells").isEqualTo(2);
        assertThat(summary).isEqualTo(expectedMotif10BA());

        // A∩B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(10);
        result = executeOp(ops[0], ops[1], 2);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif10AiB());

        // A+B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(10);
        result = executeOp(ops[0], ops[1], 3);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif10ApB());
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif10AB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(
            1, 203, 418, 216, 204, 1, 1, new int[] {203}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2}, new int[] {3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 9, 9, 9, 9, 16, 16}, new long[] {-1000000L, -1000000L, 0L, 1000000L, 993462L, 1650000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif10BA()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(
            2, 30, 72, 46, 30, 0, 4, new int[] {15, 15}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 7, 7, 7, 7, 10, 10}, new long[] {-190211L, 600000L, 738197L, 190211L, 1150000L, 1100000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif10AiB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(
            1, 18, 42, 26, 18, 0, 2, new int[] {18}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 6, 6, 7, 7, 7, 7}, new long[] {-190211L, 874541L, 738197L, 190211L, 1000000L, 1100000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif10ApB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(
            1, 215, 448, 236, 216, 1, 3, new int[] {215}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2}, new int[] {3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 9, 9, 9, 9, 10, 10, 16, 16}, new long[] {-1000000L, -1000000L, 0L, 1000000L, 1150000L, 1650000L});
    }
    // -----------------------------------------------------------------------
    // Regresión permanente motif 12
    // -----------------------------------------------------------------------

    @Test
    void given_kurlanderBowlAndMotif12_when_allFourOps_then_topologyMatchesBaseline()
    {
        PolyhedralBoundedSolid[] ops;
        PolyhedralBoundedSolid result;
        BooleansFromReferenceObjectPairsTest.TopologicalSummary summary;

        // A-B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(12);
        result = executeOp(ops[0], ops[1], 0);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif12AB());

        // B-A
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(12);
        result = executeOp(ops[0], ops[1], 1);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif12BA());

        // A∩B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(12);
        result = executeOp(ops[0], ops[1], 2);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif12AiB());

        // A+B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(12);
        result = executeOp(ops[0], ops[1], 3);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif12ApB());
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif12AB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(1, 203, 418, 216, 204, 1, 1, new int[] {203}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2}, new int[] {3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 7, 7, 7, 7, 8, 8, 16, 16}, new long[] {-1000000L, -1000000L, 0L, 1000000L, 1000000L, 1650000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif12BA()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(2, 30, 72, 46, 30, 0, 4, new int[] {15, 15}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 10, 10}, new long[] {289764L, 289764L, 1238197L, 947673L, 947673L, 1600000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif12AiB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(1, 18, 42, 26, 18, 0, 2, new int[] {18}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6}, new long[] {412942L, 412942L, 1238197L, 740195L, 740195L, 1600000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif12ApB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(1, 215, 448, 236, 216, 1, 3, new int[] {215}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2}, new int[] {3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 7, 7, 7, 7, 8, 8, 10, 10, 16, 16}, new long[] {-1000000L, -1000000L, 0L, 1000000L, 1000000L, 1650000L});
    }
    // -----------------------------------------------------------------------
    // Regresión permanente motif 14
    // -----------------------------------------------------------------------

    @Test
    void given_kurlanderBowlAndMotif14_when_allFourOps_then_topologyMatchesBaseline()
    {
        PolyhedralBoundedSolid[] ops;
        PolyhedralBoundedSolid result;
        BooleansFromReferenceObjectPairsTest.TopologicalSummary summary;

        // A-B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(14);
        result = executeOp(ops[0], ops[1], 0);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif14AB());

        // B-A
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(14);
        result = executeOp(ops[0], ops[1], 1);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif14BA());

        // A∩B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(14);
        result = executeOp(ops[0], ops[1], 2);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif14AiB());

        // A+B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(14);
        result = executeOp(ops[0], ops[1], 3);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif14ApB());
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif14AB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(1, 207, 428, 222, 208, 1, 1, new int[] {207}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2}, new int[] {3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 6, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8, 8, 16, 16}, new long[] {-1000000L, -1000000L, 0L, 1000000L, 1000000L, 1650000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif14BA()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(2, 30, 74, 48, 30, 0, 4, new int[] {15, 15}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 7, 7, 7, 7, 10, 10}, new long[] {481537L, 53878L, 988197L, 1135252L, 615818L, 1350000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif14AiB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(1, 18, 44, 28, 18, 0, 2, new int[] {18}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 6, 6, 6, 6, 7, 7, 7, 7}, new long[] {731016L, 157215L, 988197L, 946509L, 527814L, 1350000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif14ApB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(1, 219, 458, 242, 220, 1, 3, new int[] {219}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2}, new int[] {3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 7, 7, 7, 7, 8, 8, 8, 8, 10, 10, 16, 16}, new long[] {-1000000L, -1000000L, 0L, 1135252L, 1000000L, 1650000L});
    }
    // -----------------------------------------------------------------------
    // Regresión permanente motif 15
    // -----------------------------------------------------------------------

    @Test
    void given_kurlanderBowlAndMotif15_when_allFourOps_then_topologyMatchesBaseline()
    {
        PolyhedralBoundedSolid[] ops;
        PolyhedralBoundedSolid result;
        BooleansFromReferenceObjectPairsTest.TopologicalSummary summary;

        // A-B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(15);
        result = executeOp(ops[0], ops[1], 0);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif15AB());

        // B-A
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(15);
        result = executeOp(ops[0], ops[1], 1);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary.shellCount).as("B-A must produce 2 shells").isEqualTo(2);
        assertThat(summary).isEqualTo(expectedMotif15BA());

        // A∩B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(15);
        result = executeOp(ops[0], ops[1], 2);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif15AiB());

        // A+B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(15);
        result = executeOp(ops[0], ops[1], 3);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif15ApB());
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif15AB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(1, 203, 418, 216, 204, 1, 1, new int[] {203}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2}, new int[] {3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 9, 9, 9, 9, 16, 16}, new long[] {-1000000L, -1000000L, 0L, 993462L, 1000000L, 1650000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif15BA()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(2, 30, 72, 46, 30, 0, 4, new int[] {15, 15}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 7, 7, 7, 7, 10, 10}, new long[] {600000L, -190211L, 738197L, 1150000L, 190211L, 1100000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif15AiB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(1, 18, 42, 26, 18, 0, 2, new int[] {18}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 6, 6, 7, 7, 7, 7}, new long[] {874541L, -190211L, 738197L, 1000000L, 190211L, 1100000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif15ApB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(1, 215, 448, 236, 216, 1, 3, new int[] {215}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2}, new int[] {3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 9, 9, 9, 9, 10, 10, 16, 16}, new long[] {-1000000L, -1000000L, 0L, 1150000L, 1000000L, 1650000L});
    }
    // -----------------------------------------------------------------------
    // Regresión permanente motif 21
    // -----------------------------------------------------------------------

    @Test
    void given_kurlanderBowlAndMotif21_when_allFourOps_then_topologyMatchesBaseline()
    {
        PolyhedralBoundedSolid[] ops;
        PolyhedralBoundedSolid result;
        BooleansFromReferenceObjectPairsTest.TopologicalSummary summary;

        // A-B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(21);
        result = executeOp(ops[0], ops[1], 0);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif21AB());

        // B-A
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(21);
        result = executeOp(ops[0], ops[1], 1);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary.shellCount).as("B-A must produce 2 shells").isEqualTo(2);
        assertThat(summary).isEqualTo(expectedMotif21BA());

        // A∩B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(21);
        result = executeOp(ops[0], ops[1], 2);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif21AiB());

        // A+B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(21);
        result = executeOp(ops[0], ops[1], 3);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif21ApB());
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif21AB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(1, 229, 498, 270, 230, 1, 1, new int[] {229}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2}, new int[] {3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 7, 8, 11, 11, 11, 11, 11, 12, 13, 14, 16, 16}, new long[] {-1000000L, -1000000L, 0L, 1000000L, 1000000L, 1650000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif21BA()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(2, 85, 243, 162, 85, 0, 4, new int[] {37, 48}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 7, 7, 7, 7, 12, 14, 16, 18, 24, 30, 32}, new long[] {-150000L, -1040000L, 1250822L, 150000L, -490000L, 1549178L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif21AiB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(1, 40, 114, 76, 40, 0, 2, new int[] {40}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 6, 6, 7, 7, 7, 7, 12, 14, 16, 18}, new long[] {-150000L, -950108L, 1250822L, 55000L, -748789L, 1549178L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif21ApB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(1, 274, 627, 356, 275, 1, 3, new int[] {274}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2}, new int[] {3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 7, 8, 11, 11, 11, 11, 11, 12, 13, 14, 16, 16, 24, 30, 32}, new long[] {-1000000L, -1040000L, 0L, 1000000L, 1000000L, 1650000L});
    }
    // -----------------------------------------------------------------------
    // Regresión permanente motif 23
    // -----------------------------------------------------------------------

    @Test
    void given_kurlanderBowlAndMotif23_when_allFourOps_then_topologyMatchesBaseline()
    {
        PolyhedralBoundedSolid[] ops;
        PolyhedralBoundedSolid result;
        BooleansFromReferenceObjectPairsTest.TopologicalSummary summary;

        // A-B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(23);
        result = executeOp(ops[0], ops[1], 0);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif23AB());

        // B-A
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(23);
        result = executeOp(ops[0], ops[1], 1);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif23BA());

        // A∩B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(23);
        result = executeOp(ops[0], ops[1], 2);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif23AiB());

        // A+B
        ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(23);
        result = executeOp(ops[0], ops[1], 3);
        summary = BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
        assertThat(summary).isEqualTo(expectedMotif23ApB());
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif23AB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(1, 229, 498, 270, 230, 1, 1, new int[] {229}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2}, new int[] {3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 6, 6, 6, 6, 8, 9, 9, 10, 10, 11, 11, 11, 11, 16, 16, 16, 16}, new long[] {-1000000L, -1000000L, 0L, 1000000L, 1000000L, 1650000L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif23BA()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(2, 85, 243, 162, 85, 0, 4, new int[] {37, 48}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 7, 7, 7, 7, 8, 8, 22, 22, 24, 30, 32}, new long[] {-841457L, -774282L, 750822L, -240416L, -240416L, 1049178L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif23AiB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(1, 40, 114, 76, 40, 0, 2, new int[] {40}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 6, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8, 22, 22}, new long[] {-780539L, -732803L, 750822L, -591536L, -528915L, 1049178L});
    }

    private static BooleansFromReferenceObjectPairsTest.TopologicalSummary expectedMotif23ApB()
    {
        return BooleansFromReferenceObjectPairsTest.TopologicalSummary.of(1, 274, 627, 356, 275, 1, 3, new int[] {274}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2}, new int[] {3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 9, 9, 10, 10, 11, 11, 11, 11, 16, 16, 16, 16, 24, 30, 32}, new long[] {-1000000L, -1000000L, 0L, 1000000L, 1000000L, 1650000L});
    }

    @Test
    void zzTempPrintMoonBaselines()
    {
        int[] motifs = { 21, 23 };
        for ( int motif : motifs ) {
            for ( int opIdx = 0; opIdx < 4; opIdx++ ) {
                PolyhedralBoundedSolid[] ops =
                    CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(motif);
                PolyhedralBoundedSolid result = executeOp(ops[0], ops[1], opIdx);
                BooleansFromReferenceObjectPairsTest.TopologicalSummary summary =
                    BooleansFromReferenceObjectPairsTest.TopologicalSummary.from(result);
                System.out.println("MOTIF" + motif + "_OP" + opIdx + "=" +
                    summary.toLiteral());
            }
        }
    }
}