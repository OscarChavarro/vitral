package vsdk.toolkit.common.linealAlgebra;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Matrix4x4fTest
{
    private static final float EPS = 1.0e-5f;

    @ParameterizedTest
    @MethodSource("translationCases")
    void given_translationMatrix_when_multiplyingPoint_then_coordinatesAreTranslated(
        Matrix4x4f translation, Vector3Df point, Vector3Df expected)
    {
        Vector3Df moved = translation.multiply(point);
        assertThat(moved.epsilonEquals(expected, 1.0e-4f)).isTrue();
    }

    private static Stream<Arguments> translationCases()
    {
        return Stream.of(
            Arguments.of(new Matrix4x4f().translation(5.0f, -2.0f, 1.5f),
                new Vector3Df(1.0f, 2.0f, 3.0f), new Vector3Df(6.0f, 0.0f, 4.5f)),
            Arguments.of(new Matrix4x4f().translation(0.0f, 0.0f, 0.0f),
                new Vector3Df(1.0e-5f, -1.0e-5f, 0.0f), new Vector3Df(1.0e-5f, -1.0e-5f, 0.0f))
        );
    }

    @ParameterizedTest
    @MethodSource("axisRotationCases")
    void given_axisRotation_when_axisIsDegenerateOrValid_then_itBehavesAsExpected(
        float angle, Vector3Df axis, Vector3Df input, Vector3Df expected)
    {
        Matrix4x4f rotation = new Matrix4x4f().axisRotation(angle, axis);
        Vector3Df result = rotation.multiply(input);
        assertThat(result.epsilonEquals(expected, 1.0e-4f)).isTrue();
    }

    private static Stream<Arguments> axisRotationCases()
    {
        return Stream.of(
            Arguments.of((float) (Math.PI / 2.0), new Vector3Df(0.0f, 0.0f, 1.0f),
                new Vector3Df(1.0f, 0.0f, 0.0f), new Vector3Df(0.0f, 1.0f, 0.0f)),
            Arguments.of((float) (Math.PI / 3.0), new Vector3Df(0.0f, 0.0f, 0.0f),
                new Vector3Df(2.0f, -1.0f, 0.5f), new Vector3Df(2.0f, -1.0f, 0.5f))
        );
    }

    @Test
    void given_rotationMatrix_when_convertingToQuaternionAndBack_then_rotationStaysEquivalent()
    {
        Matrix4x4f rotation = new Matrix4x4f().axisRotation((float) (Math.PI / 3.0), 0.0f, 0.0f, 1.0f);

        Quaterniond q = rotation.exportToQuaternion().normalized();
        Matrix4x4f rebuilt = new Matrix4x4f().importFromQuaternion(q);

        Vector3Df ref = new Vector3Df(1.0f, 0.0f, 0.0f);
        Vector3Df r1 = rotation.multiply(ref);
        Vector3Df r2 = rebuilt.multiply(ref);

        assertThat(r2.epsilonEquals(r1, 1.0e-4f)).isTrue();
    }

    @Test
    void given_affineMatrix_when_inverting_then_productIsIdentity()
    {
        Matrix4x4f matrix = new Matrix4x4f()
            .scale(2.0f, 3.0f, 4.0f)
            .multiply(new Matrix4x4f().translation(5.0f, -1.0f, 2.0f));

        Matrix4x4f inverse = matrix.invert();
        Matrix4x4f identity = matrix.multiply(inverse);

        assertThat(identity.epsilonEquals(new Matrix4x4f(), 1.0e-4f)).isTrue();
    }

    @Test
    void given_matrix_when_withoutTranslation_then_translationIsRemovedOnly()
    {
        Matrix4x4f matrix = new Matrix4x4f().translation(3.0f, -2.0f, 5.0f)
            .multiply(new Matrix4x4f().scale(2.0f, 2.0f, 2.0f));

        Matrix4x4f noTranslation = matrix.withoutTranslation();

        assertThat(noTranslation.extractTranslation().epsilonEquals(new Vector3Df(0.0f, 0.0f, 0.0f), EPS)).isTrue();
    }

    @Test
    void given_closeMatrices_when_epsilonEquals_then_itDependsOnTolerance()
    {
        Matrix4x4f a = new Matrix4x4f();
        Matrix4x4f b = a.withVal(0, 1, 1.0e-4f);

        assertThat(a.epsilonEquals(b, 1.0e-3f)).isTrue();
        assertThat(a.epsilonEquals(b, 1.0e-6f)).isFalse();
        assertThatThrownBy(() -> a.epsilonEquals(b, -1.0f))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void given_identityMatrix_when_multiplied_then_itActsAsNeutralElement()
    {
        Matrix4x4f identity = Matrix4x4f.identityMatrix();
        Matrix4x4f matrix = new Matrix4x4f().translation(2.0f, -3.0f, 1.0f)
            .multiply(new Matrix4x4f().scale(1.5f, 2.0f, 0.5f));

        assertThat(identity.multiply(matrix).epsilonEquals(matrix, EPS)).isTrue();
        assertThat(matrix.multiply(identity).epsilonEquals(matrix, EPS)).isTrue();
    }

    @Test
    void given_identityAndScale_when_checkingDeterminant_then_valuesAreExpected()
    {
        Matrix4x4f identity = Matrix4x4f.identityMatrix();
        Matrix4x4f scale = new Matrix4x4f().scale(2.0f, 3.0f, 4.0f);

        assertThat(identity.determinant()).isCloseTo(1.0f, org.assertj.core.data.Offset.offset(EPS));
        assertThat(scale.determinant()).isCloseTo(24.0f, org.assertj.core.data.Offset.offset(1.0e-3f));
    }

    @Test
    void given_anyMatrix_when_transposedTwice_then_originalIsRecovered()
    {
        Matrix4x4f matrix = new Matrix4x4f()
            .withVal(0, 1, 2.0f)
            .withVal(1, 2, -3.0f)
            .withVal(2, 3, 4.0f)
            .withVal(3, 0, -1.0f);

        Matrix4x4f rebuilt = matrix.transpose().transpose();

        assertThat(rebuilt.epsilonEquals(matrix, EPS)).isTrue();
    }
}
