package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler;

import static org.assertj.core.api.Assertions.assertThat;

/**
Systematic boolean regression matrix based on reference object pairs and
single-object reference solids.

<p>Traceability: `doc/references/optimizationsOverMANT1988.md`,
section "Code improvement impact matrix".</p>
 */
class BooleansFromReferenceObjectPairsTest
{
    private PolyhedralBoundedSolid operandA;
    private PolyhedralBoundedSolid operandB;

    @BeforeEach
    void setUpOperands()
    {
        operandA = null;
        operandB = null;
    }

    @ParameterizedTest(name = "{0} + {1}")
    @MethodSource("legacyPassingReferencePairs")
    void given_referencePair_when_runningLegacyPassingBoolean_then_topologySummaryMatchesReference(
        CsgSampleCorpus sample,
        ReferenceBooleanOperation operation,
        PolyhedralBoundedSolid inputOperandA,
        PolyhedralBoundedSolid inputOperandB,
        TopologicalSummary expected)
    {
        operandA = inputOperandA;
        operandB = inputOperandB;

        PolyhedralBoundedSolid result = runBooleanOperation(operation);
        TopologicalSummary actual = TopologicalSummary.from(result);

        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void given_csgLampShell_when_buildingReferenceSolid_then_topologySummaryMatchesReference()
    {
        PolyhedralBoundedSolid result = createCsgLampShellReference();
        TopologicalSummary actual = TopologicalSummary.from(result);
        TopologicalSummary expected = expectedLampShellSummary();

        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void given_featuredObject_when_buildingReferenceSolid_then_topologySummaryMatchesReference()
    {
        PolyhedralBoundedSolid result =
            SimpleTestGeometryLibrary.createTestObjectAPPE1967_3();
        TopologicalSummary actual = TopologicalSummary.from(result);
        TopologicalSummary expected = expectedFeaturedObjectSummary();

        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
        assertThat(actual).isEqualTo(expected);
    }

    @Disabled("`CSG_KURLANDER_BOWL` is marked as failing (❌) in the current legacy matrix and remains pending explicit revalidation")
    @Test
    void given_csgKurlanderBowl_when_buildingReferenceSolid_then_topologySummaryMatchesReference()
    {
        PolyhedralBoundedSolid result = createCsgKurlanderBowlReference();
        TopologicalSummary actual = TopologicalSummary.from(result);
        TopologicalSummary expected = expectedKurlanderBowlSummary();

        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
        assertThat(actual).isEqualTo(expected);
    }

    @Disabled("Utility snapshot for refreshing hardcoded expected summaries after intentional baseline updates")
    @Test
    void dumpReferenceSummariesForBaselineRefresh()
    {
        legacyPassingReferencePairs().forEach(arguments -> {
            Object[] args = arguments.get();
            CsgSampleCorpus sample = (CsgSampleCorpus)args[0];
            ReferenceBooleanOperation op = (ReferenceBooleanOperation)args[1];
            operandA = (PolyhedralBoundedSolid)args[2];
            operandB = (PolyhedralBoundedSolid)args[3];
            PolyhedralBoundedSolid result = runBooleanOperation(op);
            TopologicalSummary summary = TopologicalSummary.from(result);
            System.out.println(sample.name() + " + " + op.label + " => " +
                summary.toLiteral());
        });

        PolyhedralBoundedSolid lamp = createCsgLampShellReference();
        PolyhedralBoundedSolid featured =
            SimpleTestGeometryLibrary.createTestObjectAPPE1967_3();
        PolyhedralBoundedSolid kurlander = createCsgKurlanderBowlReference();

        System.out.println("CSG_LAMP_SHELL => " +
            TopologicalSummary.from(lamp).toLiteral());
        System.out.println("FEATURED_OBJECT => " +
            TopologicalSummary.from(featured).toLiteral());
        System.out.println("CSG_KURLANDER_BOWL => " +
            TopologicalSummary.from(kurlander).toLiteral());
    }

    private PolyhedralBoundedSolid
    runBooleanOperation(ReferenceBooleanOperation operation)
    {
        switch ( operation ) {
            case UNION:
                return PolyhedralBoundedSolidModeler.setOp(
                    operandA, operandB, PolyhedralBoundedSolidModeler.UNION,
                    false);
            case INTERSECTION:
                return PolyhedralBoundedSolidModeler.setOp(
                    operandA, operandB,
                    PolyhedralBoundedSolidModeler.INTERSECTION, false);
            case DIFFERENCE_A_MINUS_B:
                return PolyhedralBoundedSolidModeler.setOp(
                    operandA, operandB, PolyhedralBoundedSolidModeler.SUBTRACT,
                    false);
            case DIFFERENCE_B_MINUS_A:
            default:
                return PolyhedralBoundedSolidModeler.setOp(
                    operandB, operandA, PolyhedralBoundedSolidModeler.SUBTRACT,
                    false);
        }
    }

    private static Stream<Arguments> legacyPassingReferencePairs()
    {
        return Stream.of(
            pairCase(CsgSampleCorpus.MANT1986_2,
                ReferenceBooleanOperation.UNION, expectedMANT1986_2Union()),
            pairCase(CsgSampleCorpus.MANT1986_2,
                ReferenceBooleanOperation.INTERSECTION,
                expectedMANT1986_2Intersection()),
            pairCase(CsgSampleCorpus.MANT1986_2,
                ReferenceBooleanOperation.DIFFERENCE_A_MINUS_B,
                expectedMANT1986_2DifferenceAB()),
            pairCase(CsgSampleCorpus.MANT1986_2,
                ReferenceBooleanOperation.DIFFERENCE_B_MINUS_A,
                expectedMANT1986_2DifferenceBA()),

            pairCase(CsgSampleCorpus.STACKED_BLOCKS,
                ReferenceBooleanOperation.UNION,
                expectedSTACKED_BLOCKSUnion()),
            pairCase(CsgSampleCorpus.STACKED_BLOCKS,
                ReferenceBooleanOperation.INTERSECTION,
                expectedSTACKED_BLOCKSIntersection()),
            pairCase(CsgSampleCorpus.STACKED_BLOCKS,
                ReferenceBooleanOperation.DIFFERENCE_A_MINUS_B,
                expectedSTACKED_BLOCKSDifferenceAB()),
            pairCase(CsgSampleCorpus.STACKED_BLOCKS,
                ReferenceBooleanOperation.DIFFERENCE_B_MINUS_A,
                expectedSTACKED_BLOCKSDifferenceBA()),

            pairCase(CsgSampleCorpus.MOON_BLOCK,
                ReferenceBooleanOperation.UNION, expectedMOON_BLOCKUnion()),
            pairCase(CsgSampleCorpus.MOON_BLOCK,
                ReferenceBooleanOperation.INTERSECTION,
                expectedMOON_BLOCKIntersection()),
            pairCase(CsgSampleCorpus.MOON_BLOCK,
                ReferenceBooleanOperation.DIFFERENCE_A_MINUS_B,
                expectedMOON_BLOCKDifferenceAB()),
            pairCase(CsgSampleCorpus.MOON_BLOCK,
                ReferenceBooleanOperation.DIFFERENCE_B_MINUS_A,
                expectedMOON_BLOCKDifferenceBA()),

            pairCase(CsgSampleCorpus.CROSS_PAIR,
                ReferenceBooleanOperation.UNION, expectedCROSS_PAIRUnion()),

            pairCase(CsgSampleCorpus.HOLLOW_BRICK,
                ReferenceBooleanOperation.UNION, expectedHOLLOW_BRICKUnion()),
            pairCase(CsgSampleCorpus.HOLLOW_BRICK,
                ReferenceBooleanOperation.INTERSECTION,
                expectedHOLLOW_BRICKIntersection()),
            pairCase(CsgSampleCorpus.HOLLOW_BRICK,
                ReferenceBooleanOperation.DIFFERENCE_A_MINUS_B,
                expectedHOLLOW_BRICKDifferenceAB()),
            pairCase(CsgSampleCorpus.HOLLOW_BRICK,
                ReferenceBooleanOperation.DIFFERENCE_B_MINUS_A,
                expectedHOLLOW_BRICKDifferenceBA()),

            pairCase(CsgSampleCorpus.MANT1988_15_1,
                ReferenceBooleanOperation.UNION, expectedMANT1988_15_1Union()),
            pairCase(CsgSampleCorpus.MANT1988_15_1,
                ReferenceBooleanOperation.INTERSECTION,
                expectedMANT1988_15_1Intersection()),
            pairCase(CsgSampleCorpus.MANT1988_15_1,
                ReferenceBooleanOperation.DIFFERENCE_A_MINUS_B,
                expectedMANT1988_15_1DifferenceAB())
        );
    }

    private static Arguments pairCase(CsgSampleCorpus sample,
                                      ReferenceBooleanOperation operation,
                                      TopologicalSummary expected)
    {
        PolyhedralBoundedSolid[] operands = CsgSampleCorpusFixtures
            .createPair(sample);
        return Arguments.of(sample, operation, operands[0], operands[1],
            expected);
    }

    // -----------------------------------------------------------------
    // Expected summaries (to be filled from baseline snapshot)
    // -----------------------------------------------------------------

    private static TopologicalSummary expectedMANT1986_2Union() { return TopologicalSummary.of(1, 12, 30, 20, 12, 0, 2, new int[] {12}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 6, 6, 6, 6, 6, 6}, new long[] {0L, -180000L, 0L, 1240000L, 500000L, 1020000L}); }
    private static TopologicalSummary expectedMANT1986_2Intersection() { return TopologicalSummary.of(1, 6, 12, 8, 6, 0, 2, new int[] {6}, new int[] {1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4}, new long[] {240000L, 0L, 420000L, 1000000L, 320000L, 600000L}); }
    private static TopologicalSummary expectedMANT1986_2DifferenceAB() { return TopologicalSummary.of(1, 9, 21, 14, 9, 0, 2, new int[] {9}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 6, 6, 6}, new long[] {0L, 0L, 0L, 1000000L, 500000L, 600000L}); }
    private static TopologicalSummary expectedMANT1986_2DifferenceBA() { return TopologicalSummary.of(1, 9, 21, 14, 9, 0, 2, new int[] {9}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 6, 6, 6}, new long[] {240000L, -180000L, 420000L, 1240000L, 320000L, 1020000L}); }
    private static TopologicalSummary expectedSTACKED_BLOCKSUnion() { return TopologicalSummary.of(1, 14, 32, 20, 14, 0, 2, new int[] {14}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 6, 6, 6, 6}, new long[] {0L, 0L, 0L, 1000000L, 1000000L, 600000L}); }
    private static TopologicalSummary expectedSTACKED_BLOCKSIntersection() { return TopologicalSummary.of(1, 2, 4, 4, 2, 0, 2, new int[] {2}, new int[] {1, 1}, new int[] {4, 4}, new long[] {250000L, 250000L, 300000L, 750000L, 750000L, 300000L}); }
    private static TopologicalSummary expectedSTACKED_BLOCKSDifferenceAB() { return TopologicalSummary.of(1, 6, 12, 8, 6, 0, 2, new int[] {6}, new int[] {1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4}, new long[] {0L, 250000L, 0L, 1000000L, 750000L, 300000L}); }
    private static TopologicalSummary expectedSTACKED_BLOCKSDifferenceBA() { return TopologicalSummary.of(1, 6, 12, 8, 6, 0, 2, new int[] {6}, new int[] {1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4}, new long[] {250000L, 0L, 300000L, 750000L, 1000000L, 600000L}); }
    private static TopologicalSummary expectedMOON_BLOCKUnion() { return TopologicalSummary.of(1, 76, 222, 148, 76, 0, 2, new int[] {76}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 8, 8, 36, 36, 38, 38}, new long[] {50000L, 50000L, -450000L, 1325000L, 1050000L, 1550000L}); }
    private static TopologicalSummary expectedMOON_BLOCKIntersection() { return TopologicalSummary.of(1, 34, 96, 64, 34, 0, 2, new int[] {34}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 32, 32}, new long[] {325000L, 71175L, 50000L, 1050000L, 1028825L, 1050000L}); }
    private static TopologicalSummary expectedMOON_BLOCKDifferenceAB() { return TopologicalSummary.of(1, 40, 114, 76, 40, 0, 2, new int[] {40}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 38, 38}, new long[] {50000L, 50000L, 50000L, 687500L, 1050000L, 1050000L}); }
    private static TopologicalSummary expectedMOON_BLOCKDifferenceBA() { return TopologicalSummary.of(1, 70, 204, 136, 70, 0, 2, new int[] {70}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 8, 8, 32, 32, 36, 36}, new long[] {325000L, 50000L, -450000L, 1325000L, 1050000L, 1550000L}); }
    private static TopologicalSummary expectedCROSS_PAIRUnion() { return TopologicalSummary.of(1, 12, 27, 17, 12, 0, 2, new int[] {12}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 4, 4, 4, 6, 6, 6}, new long[] {0L, 0L, 0L, 1000000L, 1000000L, 1000000L}); }
    private static TopologicalSummary expectedHOLLOW_BRICKUnion() { return TopologicalSummary.of(1, 10, 24, 16, 12, 2, 2, new int[] {10}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 2, 2}, new int[] {4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4}, new long[] {0L, 0L, 0L, 1000000L, 1000000L, 200000L}); }
    private static TopologicalSummary expectedHOLLOW_BRICKIntersection() { return TopologicalSummary.of(2, 12, 24, 16, 12, 0, 4, new int[] {6, 6}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4}, new long[] {0L, 0L, 0L, 1000000L, 1000000L, 200000L}); }
    private static TopologicalSummary expectedHOLLOW_BRICKDifferenceAB() { return TopologicalSummary.of(1, 8, 18, 12, 8, 0, 2, new int[] {8}, new int[] {1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 6, 6}, new long[] {0L, 200000L, 0L, 800000L, 1000000L, 200000L}); }
    private static TopologicalSummary expectedHOLLOW_BRICKDifferenceBA() { return TopologicalSummary.of(1, 8, 18, 12, 8, 0, 2, new int[] {8}, new int[] {1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 6, 6}, new long[] {200000L, 0L, 0L, 1000000L, 800000L, 200000L}); }
    private static TopologicalSummary expectedMANT1988_15_1Union() { return TopologicalSummary.of(1, 10, 24, 16, 10, 0, 2, new int[] {10}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {3, 3, 4, 4, 4, 4, 6, 6, 6, 8}, new long[] {0L, 0L, 0L, 1000000L, 1000000L, 1000000L}); }
    private static TopologicalSummary expectedMANT1988_15_1Intersection() { return TopologicalSummary.of(1, 10, 24, 16, 10, 0, 2, new int[] {10}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 6, 6, 6, 6}, new long[] {0L, 0L, 0L, 1000000L, 1000000L, 1000000L}); }
    private static TopologicalSummary expectedMANT1988_15_1DifferenceAB() { return TopologicalSummary.of(2, 10, 18, 12, 10, 0, 4, new int[] {5, 5}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {3, 3, 3, 3, 4, 4, 4, 4, 4, 4}, new long[] {0L, 0L, 583333L, 333333L, 1000000L, 1000000L}); }
    private static TopologicalSummary expectedLampShellSummary() { return TopologicalSummary.of(1, 13, 24, 14, 14, 1, 3, new int[] {13}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2}, new int[] {3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4}, new long[] {300000L, 116987L, 50000L, 1050000L, 983013L, 850000L}); }
    private static TopologicalSummary expectedFeaturedObjectSummary() { return TopologicalSummary.of(2, 32, 84, 54, 34, 2, 2, new int[] {16, 16}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2}, new int[] {4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 6, 6, 8, 8, 8, 8, 10, 10}, new long[] {0L, 0L, 0L, 1000000L, 1000000L, 1000000L}); }
    private static TopologicalSummary expectedKurlanderBowlSummary() { return TopologicalSummary.placeholder("CSG_KURLANDER_BOWL"); }

    // -----------------------------------------------------------------
    // Local builders for reference single objects
    // -----------------------------------------------------------------

    private static PolyhedralBoundedSolid createCsgLampShellReference()
    {
        double outerRadius = 0.5;
        double innerRadius = 0.45;
        int subdivisionCircumference = 3;
        int subdivisionHeight = 1;

        PolyhedralBoundedSolid outerSphere = createSphere(outerRadius,
            subdivisionCircumference, subdivisionHeight);
        PolyhedralBoundedSolid innerSphere = createSphere(innerRadius,
            subdivisionCircumference, subdivisionHeight);
        PolyhedralBoundedSolid sphericalShell = PolyhedralBoundedSolidModeler
            .setOp(outerSphere, innerSphere,
                PolyhedralBoundedSolidModeler.SUBTRACT, false);

        vsdk.toolkit.environment.geometry.volume.Box clipCubeGeometry =
            new vsdk.toolkit.environment.geometry.volume.Box(
                new Vector3D(1.4, 1.4, 1.05));
        PolyhedralBoundedSolid clipCube = clipCubeGeometry
            .exportToPolyhedralBoundedSolid();
        Matrix4x4 cubeMove = new Matrix4x4();
        cubeMove = cubeMove.translation(0.55, 0.55, 0.325);
        clipCube.applyTransformation(cubeMove);

        return PolyhedralBoundedSolidModeler.setOp(
            sphericalShell, clipCube,
            PolyhedralBoundedSolidModeler.INTERSECTION, false);
    }

    private static PolyhedralBoundedSolid createSphere(double radius,
                                                       int subdivisionsC,
                                                       int subdivisionsH)
    {
        Matrix4x4 move = new Matrix4x4();
        move = move.translation(0.55, 0.55, 0.55);
        Sphere sphere = new Sphere(radius);
        PolyhedralBoundedSolid solid = sphere.exportToPolyhedralBoundedSolid(
            subdivisionsC, subdivisionsH);
        solid.applyTransformation(move);
        return solid;
    }

    private static PolyhedralBoundedSolid createCsgKurlanderBowlReference()
    {
        return KurlanderBowlBuilder.create();
    }

    private enum ReferenceBooleanOperation
    {
        UNION("UNION"),
        INTERSECTION("INTERSECTION"),
        DIFFERENCE_A_MINUS_B("A-B"),
        DIFFERENCE_B_MINUS_A("B-A");

        private final String label;

        ReferenceBooleanOperation(String label)
        {
            this.label = label;
        }
    }

    private static final class TopologicalSummary
    {
        private final String placeholderLabel;
        private final int shellCount;
        private final int faceCount;
        private final int edgeCount;
        private final int vertexCount;
        private final int loopCount;
        private final int multiLoopFaceCount;
        private final int eulerCharacteristic;
        private final int[] shellFaceCountsSorted;
        private final int[] loopsPerFaceSorted;
        private final int[] verticesPerLoopSorted;
        private final long[] minMaxMicrounits;

        private TopologicalSummary(String placeholderLabel)
        {
            this.placeholderLabel = placeholderLabel;
            shellCount = -1;
            faceCount = -1;
            edgeCount = -1;
            vertexCount = -1;
            loopCount = -1;
            multiLoopFaceCount = -1;
            eulerCharacteristic = -1;
            shellFaceCountsSorted = new int[0];
            loopsPerFaceSorted = new int[0];
            verticesPerLoopSorted = new int[0];
            minMaxMicrounits = new long[0];
        }

        private TopologicalSummary(int shellCount,
                                   int faceCount,
                                   int edgeCount,
                                   int vertexCount,
                                   int loopCount,
                                   int multiLoopFaceCount,
                                   int eulerCharacteristic,
                                   int[] shellFaceCountsSorted,
                                   int[] loopsPerFaceSorted,
                                   int[] verticesPerLoopSorted,
                                   long[] minMaxMicrounits)
        {
            placeholderLabel = null;
            this.shellCount = shellCount;
            this.faceCount = faceCount;
            this.edgeCount = edgeCount;
            this.vertexCount = vertexCount;
            this.loopCount = loopCount;
            this.multiLoopFaceCount = multiLoopFaceCount;
            this.eulerCharacteristic = eulerCharacteristic;
            this.shellFaceCountsSorted = shellFaceCountsSorted;
            this.loopsPerFaceSorted = loopsPerFaceSorted;
            this.verticesPerLoopSorted = verticesPerLoopSorted;
            this.minMaxMicrounits = minMaxMicrounits;
        }

        static TopologicalSummary placeholder(String label)
        {
            return new TopologicalSummary(label);
        }

        static TopologicalSummary of(int shellCount,
                                     int faceCount,
                                     int edgeCount,
                                     int vertexCount,
                                     int loopCount,
                                     int multiLoopFaceCount,
                                     int eulerCharacteristic,
                                     int[] shellFaceCountsSorted,
                                     int[] loopsPerFaceSorted,
                                     int[] verticesPerLoopSorted,
                                     long[] minMaxMicrounits)
        {
            return new TopologicalSummary(shellCount, faceCount, edgeCount,
                vertexCount, loopCount, multiLoopFaceCount,
                eulerCharacteristic, shellFaceCountsSorted, loopsPerFaceSorted,
                verticesPerLoopSorted, minMaxMicrounits);
        }

        static TopologicalSummary from(PolyhedralBoundedSolid solid)
        {
            int faceCount = solid.polygonsList.size();
            int edgeCount = solid.edgesList.size();
            int vertexCount = solid.verticesList.size();
            int eulerCharacteristic = vertexCount - edgeCount + faceCount;
            ArrayList<Integer> loopsPerFace = new ArrayList<Integer>();
            ArrayList<Integer> verticesPerLoop = new ArrayList<Integer>();
            int loopCount = 0;
            int multiLoopFaceCount = 0;

            int i;
            int j;
            for ( i = 0; i < solid.polygonsList.size(); i++ ) {
                _PolyhedralBoundedSolidFace face = solid.polygonsList.get(i);
                int loopsInFace = face.boundariesList.size();
                loopsPerFace.add(Integer.valueOf(loopsInFace));
                loopCount += loopsInFace;
                if ( loopsInFace > 1 ) {
                    multiLoopFaceCount++;
                }
                for ( j = 0; j < loopsInFace; j++ ) {
                    verticesPerLoop.add(Integer.valueOf(
                        face.boundariesList.get(j).halfEdgesList.size()));
                }
            }

            Collections.sort(loopsPerFace);
            Collections.sort(verticesPerLoop);

            int[] shellFaceCountsSorted = computeShellFaceCounts(solid);
            int shellCount = shellFaceCountsSorted.length;

            return new TopologicalSummary(
                shellCount,
                faceCount,
                edgeCount,
                vertexCount,
                loopCount,
                multiLoopFaceCount,
                eulerCharacteristic,
                shellFaceCountsSorted,
                toIntArray(loopsPerFace),
                toIntArray(verticesPerLoop),
                toMinMaxMicrounits(solid.getMinMax())
            );
        }

        String toLiteral()
        {
            return "TopologicalSummary.of(" +
                shellCount + ", " +
                faceCount + ", " +
                edgeCount + ", " +
                vertexCount + ", " +
                loopCount + ", " +
                multiLoopFaceCount + ", " +
                eulerCharacteristic + ", " +
                intArrayLiteral(shellFaceCountsSorted) + ", " +
                intArrayLiteral(loopsPerFaceSorted) + ", " +
                intArrayLiteral(verticesPerLoopSorted) + ", " +
                longArrayLiteral(minMaxMicrounits) + ")";
        }

        private static String intArrayLiteral(int[] values)
        {
            return "new int[] {" + joinInts(values) + "}";
        }

        private static String longArrayLiteral(long[] values)
        {
            return "new long[] {" + joinLongs(values) + "}";
        }

        private static String joinInts(int[] values)
        {
            StringBuilder out = new StringBuilder();
            int i;
            for ( i = 0; i < values.length; i++ ) {
                if ( i > 0 ) {
                    out.append(", ");
                }
                out.append(values[i]);
            }
            return out.toString();
        }

        private static String joinLongs(long[] values)
        {
            StringBuilder out = new StringBuilder();
            int i;
            for ( i = 0; i < values.length; i++ ) {
                if ( i > 0 ) {
                    out.append(", ");
                }
                out.append(values[i]).append("L");
            }
            return out.toString();
        }

        private static int[] toIntArray(ArrayList<Integer> values)
        {
            int[] out = new int[values.size()];
            int i;
            for ( i = 0; i < values.size(); i++ ) {
                out[i] = values.get(i).intValue();
            }
            return out;
        }

        private static long[] toMinMaxMicrounits(double[] minMax)
        {
            long[] out = new long[minMax.length];
            int i;
            for ( i = 0; i < minMax.length; i++ ) {
                out[i] = Math.round(minMax[i] * 1000000.0);
            }
            return out;
        }

        private static int[] computeShellFaceCounts(PolyhedralBoundedSolid solid)
        {
            int faceCount = solid.polygonsList.size();
            if ( faceCount == 0 ) {
                return new int[0];
            }

            DisjointSet dsu = new DisjointSet(faceCount);
            int i;
            for ( i = 0; i < solid.edgesList.size(); i++ ) {
                _PolyhedralBoundedSolidEdge edge = solid.edgesList.get(i);
                if ( edge.leftHalf == null || edge.rightHalf == null ) {
                    continue;
                }
                _PolyhedralBoundedSolidFace faceA =
                    edge.leftHalf.parentLoop.parentFace;
                _PolyhedralBoundedSolidFace faceB =
                    edge.rightHalf.parentLoop.parentFace;
                if ( faceA == null || faceB == null ) {
                    continue;
                }
                int indexA = faceIndexOf(solid, faceA);
                int indexB = faceIndexOf(solid, faceB);
                if ( indexA >= 0 && indexB >= 0 ) {
                    dsu.union(indexA, indexB);
                }
            }

            ArrayList<Integer> componentSizes = new ArrayList<Integer>();
            boolean[] rootSeen = new boolean[faceCount];
            int[] rootCounts = new int[faceCount];
            for ( i = 0; i < faceCount; i++ ) {
                int root = dsu.find(i);
                rootCounts[root]++;
            }
            for ( i = 0; i < faceCount; i++ ) {
                if ( rootCounts[i] > 0 && !rootSeen[i] ) {
                    componentSizes.add(Integer.valueOf(rootCounts[i]));
                    rootSeen[i] = true;
                }
            }
            Collections.sort(componentSizes);
            return toIntArray(componentSizes);
        }

        private static int faceIndexOf(PolyhedralBoundedSolid solid,
                                       _PolyhedralBoundedSolidFace face)
        {
            int i;
            for ( i = 0; i < solid.polygonsList.size(); i++ ) {
                if ( solid.polygonsList.get(i) == face ) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public boolean equals(Object other)
        {
            if ( this == other ) {
                return true;
            }
            if ( !(other instanceof TopologicalSummary) ) {
                return false;
            }

            TopologicalSummary that = (TopologicalSummary)other;

            if ( this.placeholderLabel != null || that.placeholderLabel != null ) {
                throw new IllegalStateException(
                    "Missing hardcoded reference summary: " +
                    (this.placeholderLabel != null ? this.placeholderLabel
                        : that.placeholderLabel));
            }

            return shellCount == that.shellCount &&
                faceCount == that.faceCount &&
                edgeCount == that.edgeCount &&
                vertexCount == that.vertexCount &&
                loopCount == that.loopCount &&
                multiLoopFaceCount == that.multiLoopFaceCount &&
                eulerCharacteristic == that.eulerCharacteristic &&
                Arrays.equals(shellFaceCountsSorted, that.shellFaceCountsSorted) &&
                Arrays.equals(loopsPerFaceSorted, that.loopsPerFaceSorted) &&
                Arrays.equals(verticesPerLoopSorted, that.verticesPerLoopSorted) &&
                Arrays.equals(minMaxMicrounits, that.minMaxMicrounits);
        }

        @Override
        public int hashCode()
        {
            if ( placeholderLabel != null ) {
                return placeholderLabel.hashCode();
            }

            int result = shellCount;
            result = 31 * result + faceCount;
            result = 31 * result + edgeCount;
            result = 31 * result + vertexCount;
            result = 31 * result + loopCount;
            result = 31 * result + multiLoopFaceCount;
            result = 31 * result + eulerCharacteristic;
            result = 31 * result + Arrays.hashCode(shellFaceCountsSorted);
            result = 31 * result + Arrays.hashCode(loopsPerFaceSorted);
            result = 31 * result + Arrays.hashCode(verticesPerLoopSorted);
            result = 31 * result + Arrays.hashCode(minMaxMicrounits);
            return result;
        }

        @Override
        public String toString()
        {
            if ( placeholderLabel != null ) {
                return "TopologicalSummary{placeholder=" + placeholderLabel + "}";
            }

            return "TopologicalSummary{" +
                "shellCount=" + shellCount +
                ", faceCount=" + faceCount +
                ", edgeCount=" + edgeCount +
                ", vertexCount=" + vertexCount +
                ", loopCount=" + loopCount +
                ", multiLoopFaceCount=" + multiLoopFaceCount +
                ", eulerCharacteristic=" + eulerCharacteristic +
                ", shellFaceCountsSorted=" + Arrays.toString(shellFaceCountsSorted) +
                ", loopsPerFaceSorted=" + Arrays.toString(loopsPerFaceSorted) +
                ", verticesPerLoopSorted=" + Arrays.toString(verticesPerLoopSorted) +
                ", minMaxMicrounits=" + Arrays.toString(minMaxMicrounits) +
                '}';
        }
    }

    private static final class DisjointSet
    {
        private final int[] parent;
        private final int[] rank;

        private DisjointSet(int size)
        {
            parent = new int[size];
            rank = new int[size];
            int i;
            for ( i = 0; i < size; i++ ) {
                parent[i] = i;
                rank[i] = 0;
            }
        }

        private int find(int x)
        {
            if ( parent[x] != x ) {
                parent[x] = find(parent[x]);
            }
            return parent[x];
        }

        private void union(int a, int b)
        {
            int rootA = find(a);
            int rootB = find(b);
            if ( rootA == rootB ) {
                return;
            }
            if ( rank[rootA] < rank[rootB] ) {
                parent[rootA] = rootB;
            }
            else if ( rank[rootA] > rank[rootB] ) {
                parent[rootB] = rootA;
            }
            else {
                parent[rootB] = rootA;
                rank[rootA]++;
            }
        }
    }

    private static final class KurlanderBowlBuilder
    {
        private static final int CYLINDER_SIDES = 30;
        private static final double OBJECT_SCALE = 0.1;

        private KurlanderBowlBuilder()
        {
        }

        private static double s(double value)
        {
            return value * OBJECT_SCALE;
        }

        private static PolyhedralBoundedSolid booleanOp(
            PolyhedralBoundedSolid a, PolyhedralBoundedSolid b, int op)
        {
            return PolyhedralBoundedSolidModeler.setOp(a, b, op, false);
        }

        private static PolyhedralBoundedSolid createSphere(double radius,
                                                           Vector3D center)
        {
            PolyhedralBoundedSolid solid = new Sphere(radius)
                .exportToPolyhedralBoundedSolid();
            Matrix4x4 t = new Matrix4x4();
            t = t.translation(center);
            solid.applyTransformation(t);
            return solid;
        }

        private static PolyhedralBoundedSolid createCylinder(double radius,
                                                             double height,
                                                             Vector3D translation)
        {
            PolyhedralBoundedSolid solid = PolyhedralBoundedSolidModeler
                .createCircularLamina(0.0, 0.0, radius, 0.0, CYLINDER_SIDES);
            Matrix4x4 sweep = new Matrix4x4();
            sweep = sweep.translation(0.0, 0.0, height);
            PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
                solid, solid.findFace(1), sweep);

            Matrix4x4 move = new Matrix4x4();
            move = move.translation(translation);
            solid.applyTransformation(move);
            return solid;
        }

        private static PolyhedralBoundedSolid createExtrudedPolygon(
            Vector3D[] points, double thickness)
        {
            PolyhedralBoundedSolid solid = new PolyhedralBoundedSolid();
            int i;
            solid.mvfs(points[0], 1, 1);
            for ( i = 1; i < points.length; i++ ) {
                solid.smev(1, i, i + 1, points[i]);
            }
            solid.smef(1, points.length, 1, 2);

            Matrix4x4 t = new Matrix4x4();
            t = t.translation(0.0, 0.0, thickness);
            PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
                solid, solid.findFace(1), t);
            return solid;
        }

        private static PolyhedralBoundedSolid createStar()
        {
            int n = 10;
            double outerR = s(2.0);
            double innerR = s(0.77);
            double start = Math.toRadians(-90.0);
            Vector3D[] points = new Vector3D[n];
            int i;

            for ( i = 0; i < n; i++ ) {
                double a = start + i * Math.PI / 5.0;
                double r = (i % 2 == 0) ? outerR : innerR;
                points[i] = new Vector3D(r * Math.cos(a), r * Math.sin(a), 0.0);
            }

            return createExtrudedPolygon(points, s(5.5));
        }

        private static PolyhedralBoundedSolid createMoon()
        {
            PolyhedralBoundedSolid a = createCylinder(
                s(1.5), s(5.0), new Vector3D(0, 0, 0));
            PolyhedralBoundedSolid b = createCylinder(
                s(1.5), s(5.0), new Vector3D(s(1.1), 0, s(0.6)));
            return booleanOp(a, b, PolyhedralBoundedSolidModeler.SUBTRACT);
        }

        private static PolyhedralBoundedSolid placeMotif(
            PolyhedralBoundedSolid motif, double z, double azimuthDeg)
        {
            Matrix4x4 t = new Matrix4x4();
            Matrix4x4 ry = new Matrix4x4();
            Matrix4x4 rz = new Matrix4x4();
            Matrix4x4 m;

            t = t.translation(s(6.0), 0.0, s(z));
            ry = ry.axisRotation(Math.toRadians(90.0), 0, 1, 0);
            rz = rz.axisRotation(Math.toRadians(azimuthDeg), 0, 0, 1);
            m = rz.multiply(ry.multiply(t));
            motif.applyTransformation(m);
            return motif;
        }

        private static PolyhedralBoundedSolid create()
        {
            PolyhedralBoundedSolid outer = createSphere(
                s(10.0), new Vector3D(0, 0, s(10.0)));
            PolyhedralBoundedSolid inner = createSphere(
                s(9.5), new Vector3D(0, 0, s(10.0)));
            PolyhedralBoundedSolid shell = booleanOp(
                outer, inner, PolyhedralBoundedSolidModeler.SUBTRACT);

            int i;
            for ( i = 1; i <= 4; i++ ) {
                double base = -90.0 * i;
                shell = booleanOp(shell,
                    placeMotif(createMoon(), 4.0, base),
                    PolyhedralBoundedSolidModeler.SUBTRACT);
                shell = booleanOp(shell,
                    placeMotif(createMoon(), 14.0, base),
                    PolyhedralBoundedSolidModeler.SUBTRACT);
                shell = booleanOp(shell,
                    placeMotif(createMoon(), 11.5, base - 22.5),
                    PolyhedralBoundedSolidModeler.SUBTRACT);
                shell = booleanOp(shell,
                    placeMotif(createMoon(), 9.0, base - 45.0),
                    PolyhedralBoundedSolidModeler.SUBTRACT);
                shell = booleanOp(shell,
                    placeMotif(createMoon(), 6.5, base - 67.5),
                    PolyhedralBoundedSolidModeler.SUBTRACT);
            }

            for ( i = 1; i <= 4; i++ ) {
                double base = -90.0 * i;
                shell = booleanOp(shell,
                    placeMotif(createStar(), 9.0, base),
                    PolyhedralBoundedSolidModeler.SUBTRACT);
                shell = booleanOp(shell,
                    placeMotif(createStar(), 6.5, base - 22.5),
                    PolyhedralBoundedSolidModeler.SUBTRACT);
                shell = booleanOp(shell,
                    placeMotif(createStar(), 14.0, base - 45.0),
                    PolyhedralBoundedSolidModeler.SUBTRACT);
                shell = booleanOp(shell,
                    placeMotif(createStar(), 4.0, base - 45.0),
                    PolyhedralBoundedSolidModeler.SUBTRACT);
                shell = booleanOp(shell,
                    placeMotif(createStar(), 11.5, base - 67.5),
                    PolyhedralBoundedSolidModeler.SUBTRACT);
            }

            PolyhedralBoundedSolid guide = createCylinder(
                s(10.5), s(16.5), new Vector3D(0, 0, 0));
            return booleanOp(shell, guide,
                PolyhedralBoundedSolidModeler.INTERSECTION);
        }
    }
}
