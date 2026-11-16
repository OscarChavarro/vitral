package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidVolumeOracle.OrientedBox;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidStructuralCopy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologySummary;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;
import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidVolumeOracle.translation;

/**
Public contract of the set operations (Fable5.2Analysis.md Stage F):

<ul>
  <li>F.1 setOp leaves its operands intact; setOpDestructive consumes
      them, as [MANT1988] Sec. 15.8 does.</li>
  <li>F.2 failures are typed, all under
      {@link PolyhedralBoundedSolidOperationException} (still an
      {@link IllegalStateException}).</li>
  <li>F.4 a right empty result is an empty solid that
      {@link PolyhedralBoundedSolidModeler#isEmptyResult} recognizes; an
      empty result the operands disprove is a
      {@link SolidOperationFailedException}, never a silent empty solid.</li>
</ul>
 */
class SetOpContractTest
{
    static Stream<Arguments> corpusOperations()
    {
        Stream.Builder<Arguments> rows = Stream.builder();
        for ( CsgSampleCorpus sample : CsgSampleCorpus.values() ) {
            for ( int op : new int[] {PolyhedralBoundedSolidModeler.UNION,
                      PolyhedralBoundedSolidModeler.INTERSECTION,
                      PolyhedralBoundedSolidModeler.SUBTRACT} ) {
                rows.add(Arguments.of(sample, op));
            }
        }
        return rows.build();
    }

    @ParameterizedTest(name = "{0} op={1}")
    @MethodSource("corpusOperations")
    void given_referencePair_when_setOp_then_operandsAreUnchanged(
        CsgSampleCorpus sample, int op)
    {
        PolyhedralBoundedSolid[] pair = CsgSampleCorpusFixtures.createPair(
            sample);
        String beforeA = PolyhedralBoundedSolidTopologySummary.from(pair[0])
            .toString();
        String beforeB = PolyhedralBoundedSolidTopologySummary.from(pair[1])
            .toString();
        double volumeA = PolyhedralBoundedSolidVolumeOracle.solidVolume(
            pair[0]);
        double volumeB = PolyhedralBoundedSolidVolumeOracle.solidVolume(
            pair[1]);

        try {
            PolyhedralBoundedSolidModeler.setOp(pair[0], pair[1], op, false,
                true, false);
        }
        catch ( PolyhedralBoundedSolidOperationException e ) {
            // The contract holds after a failure as well.
        }

        assertThat(PolyhedralBoundedSolidTopologySummary.from(pair[0]))
            .hasToString(beforeA);
        assertThat(PolyhedralBoundedSolidTopologySummary.from(pair[1]))
            .hasToString(beforeB);
        assertThat(PolyhedralBoundedSolidVolumeOracle.solidVolume(pair[0]))
            .isEqualTo(volumeA);
        assertThat(PolyhedralBoundedSolidVolumeOracle.solidVolume(pair[1]))
            .isEqualTo(volumeB);
    }

    @Test
    void given_operands_when_setOpDestructive_then_operandsAreConsumed()
    {
        PolyhedralBoundedSolid a = new OrientedBox(2, 2, 2,
            translation(0, 0, 0)).toSolid();
        PolyhedralBoundedSolid b = new OrientedBox(2, 2, 2,
            translation(1, 1, 1)).toSolid();

        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler
            .setOpDestructive(a, b, PolyhedralBoundedSolidModeler.UNION,
                false, true, true);

        assertThat(PolyhedralBoundedSolidVolumeOracle.solidVolume(result))
            .isCloseTo(15.0, offset(1.0e-9));
        assertThat(PolyhedralBoundedSolidVolumeOracle.solidVolume(a))
            .as("operand A after a destructive union")
            .isNotCloseTo(8.0, offset(1.0e-9));
    }

    @Test
    void given_sameSolidTwice_when_subtracting_then_resultIsTheEmptySolid()
    {
        PolyhedralBoundedSolid a = new OrientedBox(1, 2, 3,
            translation(0.5, 0, 0)).toSolid();
        PolyhedralBoundedSolid b = PolyhedralBoundedSolidStructuralCopy.copy(a);

        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            a, b, PolyhedralBoundedSolidModeler.SUBTRACT);

        assertThat(PolyhedralBoundedSolidModeler.isEmptyResult(result))
            .isTrue();
    }

    @ParameterizedTest(name = "op={0}")
    @ValueSource(ints = {PolyhedralBoundedSolidModeler.INTERSECTION,
        PolyhedralBoundedSolidModeler.SUBTRACT})
    void given_disjointOrContainedOperands_when_emptyByDefinition_then_emptySolid(
        int op)
    {
        PolyhedralBoundedSolid a = op == PolyhedralBoundedSolidModeler.SUBTRACT
            ? new OrientedBox(1, 1, 1, translation(0, 0, 0)).toSolid()
            : new OrientedBox(1, 1, 1, translation(5, 0, 0)).toSolid();
        PolyhedralBoundedSolid b = new OrientedBox(3, 3, 3,
            translation(0, 0, 0)).toSolid();

        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            a, b, op);

        assertThat(PolyhedralBoundedSolidModeler.isEmptyResult(result))
            .isTrue();
    }

    /**
    The rotated cross pair used to come back as an empty solid without any
    report (Stage D notes). Union and intersection give the solid; the
    difference leaves two bars that touch only along an edge, a non-manifold
    result that is rejected (policy of B-04).
    */
    @ParameterizedTest(name = "op={0}")
    @ValueSource(ints = {PolyhedralBoundedSolidModeler.UNION,
        PolyhedralBoundedSolidModeler.INTERSECTION,
        PolyhedralBoundedSolidModeler.SUBTRACT})
    void given_rotatedCrossPair_when_setOp_then_neverASilentEmptySolid(int op)
    {
        PolyhedralBoundedSolid[] pair = CsgSampleCorpusFixtures.createPair(
            CsgSampleCorpus.CROSS_PAIR);
        PolyhedralBoundedSolidModeler.applyTransformation(pair[0],
            RigidMotionInvarianceGateTest.GENERIC_ROTATION);
        PolyhedralBoundedSolidModeler.applyTransformation(pair[1],
            RigidMotionInvarianceGateTest.GENERIC_ROTATION);

        if ( op == PolyhedralBoundedSolidModeler.SUBTRACT ) {
            assertThatThrownBy(() -> PolyhedralBoundedSolidModeler
                .setOp(pair[0], pair[1], op, false, true, false))
                .isInstanceOf(NonManifoldBooleanResultException.class);
            return;
        }
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler
            .setOp(pair[0], pair[1], op, false, true, false);
        assertThat(PolyhedralBoundedSolidModeler.isEmptyResult(result))
            .as("silent empty result").isFalse();
        assertThat(PolyhedralBoundedSolidValidationEngine.validateStrict(
            result, new StringBuilder())).isTrue();
    }

    @Test
    void given_typedFailures_then_theyShareTheBaseAndStayIllegalState()
    {
        assertThat(new NonManifoldBooleanResultException("x"))
            .isInstanceOf(PolyhedralBoundedSolidOperationException.class)
            .isInstanceOf(IllegalStateException.class);
        assertThat(new InvalidSolidResultException("x"))
            .isInstanceOf(PolyhedralBoundedSolidOperationException.class);
        assertThat(new SolidOperationFailedException("x", new RuntimeException()))
            .isInstanceOf(PolyhedralBoundedSolidOperationException.class)
            .hasCauseInstanceOf(RuntimeException.class);
        assertThat(new InvalidOperandException("x"))
            .isInstanceOf(PolyhedralBoundedSolidOperationException.class);
    }

    /**
    An operand that is not a valid boundary model stops the operation before
    the pipeline runs: a cube with one vertex lifted has non-planar faces.
    */
    @Test
    void given_operandWithNonPlanarFaces_when_setOp_then_invalidOperand()
    {
        PolyhedralBoundedSolid warped = new OrientedBox(1, 1, 1,
            translation(0, 0, 0)).toSolid();
        PolyhedralBoundedSolid box = new OrientedBox(1, 1, 1,
            translation(0.3, 0.2, 0.1)).toSolid();

        warped.getVerticesList().get(0).position =
            warped.getVerticesList().get(0).position.add(
                new Vector3Dd(0.1, 0.2, 0.3));

        assertThatThrownBy(() -> PolyhedralBoundedSolidModeler.setOp(
            warped, box, PolyhedralBoundedSolidModeler.UNION))
            .isInstanceOf(InvalidOperandException.class);
    }
}
