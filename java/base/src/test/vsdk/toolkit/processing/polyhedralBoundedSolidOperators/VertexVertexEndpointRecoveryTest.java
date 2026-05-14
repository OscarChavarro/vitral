package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import java.lang.reflect.Method;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

import static org.assertj.core.api.Assertions.assertThat;

/**
Acceptance tests for the V/V endpoint recovery loop in
{@link PolyhedralBoundedSolidSetOperator#separateEdgeSequence}, after the
§5.3 refactor in plan-csg-boolean-fix-stage2 replaced the legacy
"recoveryGuard > 16" magic ceiling with strict cycle detection over the
visited (from, to) pair history.

<p>The loop's progress invariant is: every iteration produces an unseen
{@code (from, to)} configuration. Because the half-edge population of
both loops is finite, the visited set cannot grow forever — the loop
either reaches the desired pairing (vertices coincide) or detects a
revisited configuration and reports
{@link PolyhedralBoundedSolidSetOperator.SeparateEdgeSequenceResult#FAILED_CYCLE_DETECTED}.</p>

<p>Mapping to Mäntylä: the recovery handles the cases A-E that program
[MANT1988].15.12 (extended in wMANT2008) needs to canonicalize before
LMEV is safe to execute (figure 15.13 of the book).</p>
 */
class VertexVertexEndpointRecoveryTest
{
    private static Method separateEdgeSequenceMethod;
    private static Class<?> resultEnum;

    @BeforeAll
    static void resolveReflectionHandles() throws Exception
    {
        separateEdgeSequenceMethod =
            PolyhedralBoundedSolidSetOperator.class.getDeclaredMethod(
                "separateEdgeSequence",
                vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid
                    .nodes._PolyhedralBoundedSolidHalfEdge.class,
                vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid
                    .nodes._PolyhedralBoundedSolidHalfEdge.class,
                int.class,
                PolyhedralBoundedSolid.class,
                PolyhedralBoundedSolid.class);
        separateEdgeSequenceMethod.setAccessible(true);

        resultEnum = Class.forName(
            "vsdk.toolkit.processing.polyhedralBoundedSolidOperators."
            + "PolyhedralBoundedSolidSetOperator$SeparateEdgeSequenceResult");
    }

    @Test
    void given_separateEdgeSequence_when_invokedWithNullFrom_then_returnsFailedNullInput()
        throws Exception
    {
        Object result = separateEdgeSequenceMethod.invoke(
            null, null, null, 0, null, null);

        assertThat(result.toString()).isEqualTo("FAILED_NULL_INPUT");
    }

    @Test
    void given_separateEdgeSequence_when_apiInspected_then_resultEnumExposesAllFailureModes()
    {
        // Regression guard: the §5.3 refactor must keep all five named
        // outcomes (OK + four failure modes) as part of the public diagnostic
        // surface so the caller can react to each cause specifically.
        Object[] constants = resultEnum.getEnumConstants();
        HashSet<String> names = new HashSet<String>();
        for ( Object c : constants ) {
            names.add(c.toString());
        }
        assertThat(names).containsExactlyInAnyOrder(
            "OK",
            "FAILED_NULL_INPUT",
            "FAILED_DIFFERENT_SOLIDS",
            "FAILED_CYCLE_DETECTED",
            "FAILED_NO_PAIRING_REACHED");
    }

    @Test
    void given_classicReferencePair_when_runningSetOpThatExercisesRecovery_then_brepRemainsValid()
    {
        // The MANT1988 §15.1 pair stresses the V/V endpoint pairing path
        // (multiple coincident vertices in the coplanar interface). Driving
        // it through INTERSECTION must complete without triggering the new
        // cycle-detection abort and produce a valid B-rep.
        PolyhedralBoundedSolid[] pair =
            SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_1();

        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            pair[0], pair[1],
            PolyhedralBoundedSolidModeler.INTERSECTION, false);

        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
        assertThat(result.getPolygonsList().size()).isGreaterThan(0);
    }

    @Test
    void given_classicReferencePair_when_runningSubtractThatExercisesRecovery_then_brepRemainsValid()
    {
        PolyhedralBoundedSolid[] pair =
            SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_1();

        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            pair[0], pair[1],
            PolyhedralBoundedSolidModeler.SUBTRACT, false);

        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
        assertThat(result.getPolygonsList().size()).isGreaterThan(0);
    }
}
