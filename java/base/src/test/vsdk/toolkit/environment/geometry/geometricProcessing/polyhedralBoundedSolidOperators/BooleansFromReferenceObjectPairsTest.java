package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologySummary;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler;

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
        // Arrange
        operandA = inputOperandA;
        operandB = inputOperandB;

        // Action
        PolyhedralBoundedSolid result = runBooleanOperation(operation);
        TopologicalSummary actual = TopologicalSummary.from(result);

        // Assert
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void given_csgLampShell_when_buildingReferenceSolid_then_topologySummaryMatchesReference()
    {
        // Arrange

        // Action
        PolyhedralBoundedSolid result = createCsgLampShellReference();
        TopologicalSummary actual = TopologicalSummary.from(result);
        TopologicalSummary expected = expectedLampShellSummary();

        // Assert
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void given_featuredObject_when_buildingReferenceSolid_then_topologySummaryMatchesReference()
    {
        // Arrange

        // Action
        PolyhedralBoundedSolid result =
            SimpleTestGeometryLibrary.createTestObjectAPPE1967_3();
        TopologicalSummary actual = TopologicalSummary.from(result);
        TopologicalSummary expected = expectedFeaturedObjectSummary();

        // Assert
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
        assertThat(actual).isEqualTo(expected);
    }

    /**
    Prints the summaries of every reference pair and single object as Java
    literals, to refresh the hardcoded expectations after an intentional
    baseline change. Not a test: run it with the test classpath.
    */
    public static void main(String[] args)
    {
        new BooleansFromReferenceObjectPairsTest()
            .dumpReferenceSummariesForBaselineRefresh();
    }

    private void dumpReferenceSummariesForBaselineRefresh()
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

        System.out.println("CSG_LAMP_SHELL => " +
            TopologicalSummary.from(lamp).toLiteral());
        System.out.println("FEATURED_OBJECT => " +
            TopologicalSummary.from(featured).toLiteral());
    }

    private PolyhedralBoundedSolid
    runBooleanOperation(ReferenceBooleanOperation operation)
    {
        switch ( operation ) {
            case UNION:
                return PolyhedralBoundedSolidModeler.setOp(
                    operandA, operandB, PolyhedralBoundedSolidModeler.UNION,
                    false, true, false);
            case INTERSECTION:
                return PolyhedralBoundedSolidModeler.setOp(
                    operandA, operandB,
                    PolyhedralBoundedSolidModeler.INTERSECTION,
                    false, true, false);
            case DIFFERENCE_A_MINUS_B:
                return PolyhedralBoundedSolidModeler.setOp(
                    operandA, operandB, PolyhedralBoundedSolidModeler.SUBTRACT,
                    false, true, false);
            case DIFFERENCE_B_MINUS_A:
            default:
                return PolyhedralBoundedSolidModeler.setOp(
                    operandB, operandA, PolyhedralBoundedSolidModeler.SUBTRACT,
                    false, true, false);
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

            pairCase(CsgSampleCorpus.MANT1988_6_13,
                ReferenceBooleanOperation.UNION,
                expectedMANT1988_6_13Union()),
            pairCase(CsgSampleCorpus.MANT1988_6_13,
                ReferenceBooleanOperation.INTERSECTION,
                expectedMANT1988_6_13Intersection()),
            pairCase(CsgSampleCorpus.MANT1988_6_13,
                ReferenceBooleanOperation.DIFFERENCE_A_MINUS_B,
                expectedMANT1988_6_13DifferenceAB()),
            pairCase(CsgSampleCorpus.MANT1988_6_13,
                ReferenceBooleanOperation.DIFFERENCE_B_MINUS_A,
                expectedMANT1988_6_13DifferenceBA()),

            pairCase(CsgSampleCorpus.MANT1988_15_2_HOLED,
                ReferenceBooleanOperation.UNION,
                expectedMANT1988_15_2HoledUnion()),
            pairCase(CsgSampleCorpus.MANT1988_15_2_HOLED,
                ReferenceBooleanOperation.INTERSECTION,
                expectedMANT1988_15_2HoledIntersection()),
            pairCase(CsgSampleCorpus.MANT1988_15_2_HOLED,
                ReferenceBooleanOperation.DIFFERENCE_A_MINUS_B,
                expectedMANT1988_15_2HoledDifferenceAB()),
            pairCase(CsgSampleCorpus.MANT1988_15_2_HOLED,
                ReferenceBooleanOperation.DIFFERENCE_B_MINUS_A,
                expectedMANT1988_15_2HoledDifferenceBA()),

            pairCase(CsgSampleCorpus.MANT1988_15_1,
                ReferenceBooleanOperation.UNION, expectedMANT1988_15_1Union()),
            pairCase(CsgSampleCorpus.MANT1988_15_1,
                ReferenceBooleanOperation.INTERSECTION,
                expectedMANT1988_15_1Intersection()),
            pairCase(CsgSampleCorpus.MANT1988_15_1,
                ReferenceBooleanOperation.DIFFERENCE_A_MINUS_B,
                expectedMANT1988_15_1DifferenceAB()),
            pairCase(CsgSampleCorpus.MANT1988_15_1,
                ReferenceBooleanOperation.DIFFERENCE_B_MINUS_A,
                expectedMANT1988_15_1DifferenceBA()),

            pairCase(CsgSampleCorpus.MANT1988_3,
                ReferenceBooleanOperation.UNION, expectedMANT1988_3Union()),
            pairCase(CsgSampleCorpus.MANT1988_3,
                ReferenceBooleanOperation.INTERSECTION,
                expectedMANT1988_3Intersection()),
            pairCase(CsgSampleCorpus.MANT1988_3,
                ReferenceBooleanOperation.DIFFERENCE_A_MINUS_B,
                expectedMANT1988_3DifferenceAB()),
            pairCase(CsgSampleCorpus.MANT1988_3,
                ReferenceBooleanOperation.DIFFERENCE_B_MINUS_A,
                expectedMANT1988_3DifferenceBA())
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
    private static TopologicalSummary expectedMANT1988_6_13Union() { return TopologicalSummary.of(1, 11, 27, 18, 11, 0, 2, new int[] {11}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 4, 5, 5, 8, 8}, new long[] {0L, 0L, 0L, 1000000L, 1000000L, 324324L}); }
    private static TopologicalSummary expectedMANT1988_6_13Intersection() { return TopologicalSummary.of(1, 12, 30, 20, 12, 0, 2, new int[] {12}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 8, 8}, new long[] {0L, 0L, 0L, 837838L, 1000000L, 324324L}); }
    private static TopologicalSummary expectedMANT1988_6_13DifferenceAB() { return TopologicalSummary.of(1, 7, 15, 10, 7, 0, 2, new int[] {7}, new int[] {1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 5, 5}, new long[] {0L, 243243L, 81081L, 837838L, 756757L, 324324L}); }
    private static TopologicalSummary expectedMANT1988_6_13DifferenceBA() { return TopologicalSummary.of(1, 12, 30, 20, 12, 0, 2, new int[] {12}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 8, 8}, new long[] {432432L, 0L, 0L, 1000000L, 1000000L, 324324L}); }
    private static TopologicalSummary expectedMANT1988_15_2HoledUnion() { return TopologicalSummary.of(1, 14, 30, 20, 16, 2, 4, new int[] {14}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2}, new int[] {3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4}, new long[] {0L, 0L, 0L, 775000L, 1000000L, 600000L}); }
    // Refreshed 2026-11-16 (Fable5.2Analysis.md B-01): the wedge pierces the
    // block through two faces; the old snapshot kept two zero-area ring
    // faces. Exact volume 0.04125 is asserted in PolyhedralBoundedSolidSetOperatorTest.
    private static TopologicalSummary expectedMANT1988_15_2HoledIntersection() { return TopologicalSummary.of(1, 5, 9, 6, 5, 0, 2, new int[] {5}, new int[] {1, 1, 1, 1, 1}, new int[] {3, 3, 4, 4, 4}, new long[] {137500L, 225000L, 250000L, 637500L, 775000L, 550000L}); }
    private static TopologicalSummary expectedMANT1988_15_2HoledDifferenceAB() { return TopologicalSummary.of(1, 9, 21, 14, 11, 2, 2, new int[] {9}, new int[] {1, 1, 1, 1, 1, 1, 1, 2, 2}, new int[] {3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4}, new long[] {137500L, 0L, 0L, 637500L, 1000000L, 600000L}); }
    // Refreshed 2026-11-16 (B-01): two prism stubs, no zero-area rings.
    private static TopologicalSummary expectedMANT1988_15_2HoledDifferenceBA() { return TopologicalSummary.of(2, 10, 18, 12, 10, 0, 4, new int[] {5, 5}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {3, 3, 3, 3, 4, 4, 4, 4, 4, 4}, new long[] {0L, 225000L, 250000L, 775000L, 775000L, 550000L}); }
    private static TopologicalSummary expectedMANT1988_15_1Union() { return TopologicalSummary.of(1, 10, 24, 16, 10, 0, 2, new int[] {10}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {3, 3, 4, 4, 4, 4, 6, 6, 6, 8}, new long[] {0L, 0L, 0L, 1000000L, 1000000L, 1000000L}); }
    private static TopologicalSummary expectedMANT1988_15_1Intersection() { return TopologicalSummary.of(1, 10, 24, 16, 10, 0, 2, new int[] {10}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 6, 6, 6, 6}, new long[] {0L, 0L, 0L, 1000000L, 1000000L, 1000000L}); }
    private static TopologicalSummary expectedMANT1988_15_1DifferenceAB() { return TopologicalSummary.of(2, 10, 18, 12, 10, 0, 4, new int[] {5, 5}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {3, 3, 3, 3, 4, 4, 4, 4, 4, 4}, new long[] {0L, 0L, 583333L, 333333L, 1000000L, 1000000L}); }
    private static TopologicalSummary expectedMANT1988_15_1DifferenceBA() { return TopologicalSummary.of(1, 8, 18, 12, 8, 0, 2, new int[] {8}, new int[] {1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 6, 6}, new long[] {333333L, 0L, 250000L, 1000000L, 1000000L, 1000000L}); }
    private static TopologicalSummary expectedMANT1988_3Union() { return TopologicalSummary.of(1, 14, 35, 23, 14, 0, 2, new int[] {14}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 4, 5, 5, 6, 6, 6, 6, 8}, new long[] {50000L, -160000L, 50000L, 970000L, 470000L, 770000L}); }
    private static TopologicalSummary expectedMANT1988_3Intersection() { return TopologicalSummary.of(1, 6, 12, 8, 6, 0, 2, new int[] {6}, new int[] {1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4}, new long[] {750000L, 50000L, 230000L, 970000L, 260000L, 410000L}); }
    private static TopologicalSummary expectedMANT1988_3DifferenceAB() { return TopologicalSummary.of(1, 11, 27, 18, 11, 0, 2, new int[] {11}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 4, 6, 6, 6, 8}, new long[] {50000L, 50000L, 50000L, 970000L, 470000L, 770000L}); }
    private static TopologicalSummary expectedMANT1988_3DifferenceBA() { return TopologicalSummary.of(1, 8, 18, 12, 8, 0, 2, new int[] {8}, new int[] {1, 1, 1, 1, 1, 1, 1, 1}, new int[] {4, 4, 4, 4, 4, 4, 6, 6}, new long[] {390000L, -160000L, 230000L, 970000L, 260000L, 410000L}); }
    private static TopologicalSummary expectedLampShellSummary() { return TopologicalSummary.of(1, 13, 24, 14, 14, 1, 3, new int[] {13}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2}, new int[] {3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4}, new long[] {300000L, 116987L, 50000L, 1050000L, 983013L, 850000L}); }
    private static TopologicalSummary expectedFeaturedObjectSummary() { return TopologicalSummary.of(2, 32, 84, 54, 34, 2, 2, new int[] {16, 16}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2}, new int[] {4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 6, 6, 8, 8, 8, 8, 10, 10}, new long[] {0L, 0L, 0L, 1000000L, 1000000L, 1000000L}); }

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
                new Vector3Dd(1.4, 1.4, 1.05));
        PolyhedralBoundedSolid clipCube = clipCubeGeometry
            .exportToPolyhedralBoundedSolid();
        Matrix4x4d cubeMove = new Matrix4x4d();
        cubeMove = cubeMove.translation(0.55, 0.55, 0.325);
        PolyhedralBoundedSolidModeler.applyTransformation(clipCube, cubeMove);

        return PolyhedralBoundedSolidModeler.setOp(
            sphericalShell, clipCube,
            PolyhedralBoundedSolidModeler.INTERSECTION, false);
    }

    private static PolyhedralBoundedSolid createSphere(double radius,
                                                       int subdivisionsC,
                                                       int subdivisionsH)
    {
        Matrix4x4d move = new Matrix4x4d();
        move = move.translation(0.55, 0.55, 0.55);
        Sphere sphere = new Sphere(radius);
        PolyhedralBoundedSolid solid = sphere.exportToPolyhedralBoundedSolid(
            subdivisionsC, subdivisionsH);
        PolyhedralBoundedSolidModeler.applyTransformation(solid, move);
        return solid;
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

    static final class TopologicalSummary
    {
        final int shellCount;
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
            int faceCount = solid.getPolygonsList().size();
            int edgeCount = solid.getEdgesList().size();
            int vertexCount = solid.getVerticesList().size();
            int eulerCharacteristic = vertexCount - edgeCount + faceCount;
            ArrayList<Integer> loopsPerFace = new ArrayList<Integer>();
            ArrayList<Integer> verticesPerLoop = new ArrayList<Integer>();
            int loopCount = 0;
            int multiLoopFaceCount = 0;

            int i;
            int j;
            for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
                _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
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
            PolyhedralBoundedSolidTopologySummary sharedSummary =
                PolyhedralBoundedSolidTopologySummary.from(solid);
            ArrayList<Integer> componentSizes = new ArrayList<Integer>();
            for ( int i = 0;
                  i < sharedSummary.getShells().size(); i++ ) {
                componentSizes.add(Integer.valueOf(
                    sharedSummary.getShells().get(i).getFaceCount()));
            }
            Collections.sort(componentSizes);
            return toIntArray(componentSizes);
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
}
