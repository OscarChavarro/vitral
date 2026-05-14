package vsdk.toolkit.common.linealAlgebra;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Matrix4x4Test
{
    private static final double EPS = 1.0e-9;

    @ParameterizedTest
    @MethodSource("translationCases")
    void given_translationMatrix_when_multiplyingPoint_then_coordinatesAreTranslated(
        Matrix4x4d translation, Vector3Dd point, Vector3Dd expected)
    {
        Vector3Dd moved = translation.multiply(point);
        assertThat(moved.epsilonEquals(expected, 1.0e-9)).isTrue();
    }

    private static Stream<Arguments> translationCases()
    {
        return Stream.of(
            Arguments.of(new Matrix4x4d().translation(5.0, -2.0, 1.5),
                new Vector3Dd(1.0, 2.0, 3.0), new Vector3Dd(6.0, 0.0, 4.5)),
            Arguments.of(new Matrix4x4d().translation(0.0, 0.0, 0.0),
                new Vector3Dd(1.0e-12, -1.0e-12, 0.0), new Vector3Dd(1.0e-12, -1.0e-12, 0.0))
        );
    }

    @ParameterizedTest
    @MethodSource("axisRotationCases")
    void given_axisRotation_when_axisIsDegenerateOrValid_then_itBehavesAsExpected(
        double angle, Vector3Dd axis, Vector3Dd input, Vector3Dd expected)
    {
        Matrix4x4d rotation = new Matrix4x4d().axisRotation(angle, axis);
        Vector3Dd result = rotation.multiply(input);
        assertThat(result.epsilonEquals(expected, 1.0e-8)).isTrue();
    }

    private static Stream<Arguments> axisRotationCases()
    {
        return Stream.of(
            Arguments.of(Math.PI / 2.0, new Vector3Dd(0.0, 0.0, 1.0),
                new Vector3Dd(1.0, 0.0, 0.0), new Vector3Dd(0.0, 1.0, 0.0)),
            Arguments.of(Math.PI / 3.0, new Vector3Dd(0.0, 0.0, 0.0),
                new Vector3Dd(2.0, -1.0, 0.5), new Vector3Dd(2.0, -1.0, 0.5))
        );
    }

    @Test
    void given_rotationMatrix_when_convertingToQuaternionAndBack_then_rotationStaysEquivalent()
    {
        Matrix4x4d rotation = new Matrix4x4d().axisRotation(Math.PI / 3.0, 0.0, 0.0, 1.0);

        Quaterniond q = rotation.exportToQuaternion().normalized();
        Matrix4x4d rebuilt = new Matrix4x4d().importFromQuaternion(q);

        Vector3Dd ref = new Vector3Dd(1.0, 0.0, 0.0);
        Vector3Dd r1 = rotation.multiply(ref);
        Vector3Dd r2 = rebuilt.multiply(ref);

        assertThat(r2.epsilonEquals(r1, 1.0e-6)).isTrue();
    }

    @Test
    void given_affineMatrix_when_inverting_then_productIsIdentity()
    {
        Matrix4x4d matrix = new Matrix4x4d()
            .scale(2.0, 3.0, 4.0)
            .multiply(new Matrix4x4d().translation(5.0, -1.0, 2.0));

        Matrix4x4d inverse = matrix.invert();
        Matrix4x4d identity = matrix.multiply(inverse);

        assertThat(identity.epsilonEquals(new Matrix4x4d(), 1.0e-6)).isTrue();
    }

    @Test
    void given_matrix_when_withoutTranslation_then_translationIsRemovedOnly()
    {
        Matrix4x4d matrix = new Matrix4x4d().translation(3.0, -2.0, 5.0)
            .multiply(new Matrix4x4d().scale(2.0, 2.0, 2.0));

        Matrix4x4d noTranslation = matrix.withoutTranslation();

        assertThat(noTranslation.extractTranslation().epsilonEquals(new Vector3Dd(0.0, 0.0, 0.0), EPS)).isTrue();
    }

    @Test
    void given_closeMatrices_when_epsilonEquals_then_itDependsOnTolerance()
    {
        Matrix4x4d a = new Matrix4x4d();
        Matrix4x4d b = a.withVal(0, 1, 1.0e-7);

        assertThat(a.epsilonEquals(b, 1.0e-6)).isTrue();
        assertThat(a.epsilonEquals(b, 1.0e-9)).isFalse();
        assertThatThrownBy(() -> a.epsilonEquals(b, -1.0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void given_identityMatrix_when_multiplied_then_itActsAsNeutralElement()
    {
        Matrix4x4d identity = Matrix4x4d.identityMatrix();
        Matrix4x4d matrix = new Matrix4x4d().translation(2.0, -3.0, 1.0)
            .multiply(new Matrix4x4d().scale(1.5, 2.0, 0.5));

        assertThat(identity.multiply(matrix).epsilonEquals(matrix, EPS)).isTrue();
        assertThat(matrix.multiply(identity).epsilonEquals(matrix, EPS)).isTrue();
    }

    @Test
    void given_identityAndScale_when_checkingDeterminant_then_valuesAreExpected()
    {
        Matrix4x4d identity = Matrix4x4d.identityMatrix();
        Matrix4x4d scale = new Matrix4x4d().scale(2.0, 3.0, 4.0);

        assertThat(identity.determinant()).isCloseTo(1.0, org.assertj.core.data.Offset.offset(EPS));
        assertThat(scale.determinant()).isCloseTo(24.0, org.assertj.core.data.Offset.offset(1.0e-9));
    }

    @Test
    void given_anyMatrix_when_transposedTwice_then_originalIsRecovered()
    {
        Matrix4x4d matrix = new Matrix4x4d()
            .withVal(0, 1, 2.0)
            .withVal(1, 2, -3.0)
            .withVal(2, 3, 4.0)
            .withVal(3, 0, -1.0);

        Matrix4x4d rebuilt = matrix.transpose().transpose();

        assertThat(rebuilt.epsilonEquals(matrix, EPS)).isTrue();
    }
}
