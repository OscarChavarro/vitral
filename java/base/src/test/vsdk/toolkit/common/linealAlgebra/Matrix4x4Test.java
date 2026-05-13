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
        Matrix4x4 translation, Vector3D point, Vector3D expected)
    {
        Vector3D moved = translation.multiply(point);
        assertThat(moved.epsilonEquals(expected, 1.0e-9)).isTrue();
    }

    private static Stream<Arguments> translationCases()
    {
        return Stream.of(
            Arguments.of(new Matrix4x4().translation(5.0, -2.0, 1.5),
                new Vector3D(1.0, 2.0, 3.0), new Vector3D(6.0, 0.0, 4.5)),
            Arguments.of(new Matrix4x4().translation(0.0, 0.0, 0.0),
                new Vector3D(1.0e-12, -1.0e-12, 0.0), new Vector3D(1.0e-12, -1.0e-12, 0.0))
        );
    }

    @ParameterizedTest
    @MethodSource("axisRotationCases")
    void given_axisRotation_when_axisIsDegenerateOrValid_then_itBehavesAsExpected(
        double angle, Vector3D axis, Vector3D input, Vector3D expected)
    {
        Matrix4x4 rotation = new Matrix4x4().axisRotation(angle, axis);
        Vector3D result = rotation.multiply(input);
        assertThat(result.epsilonEquals(expected, 1.0e-8)).isTrue();
    }

    private static Stream<Arguments> axisRotationCases()
    {
        return Stream.of(
            Arguments.of(Math.PI / 2.0, new Vector3D(0.0, 0.0, 1.0),
                new Vector3D(1.0, 0.0, 0.0), new Vector3D(0.0, 1.0, 0.0)),
            Arguments.of(Math.PI / 3.0, new Vector3D(0.0, 0.0, 0.0),
                new Vector3D(2.0, -1.0, 0.5), new Vector3D(2.0, -1.0, 0.5))
        );
    }

    @Test
    void given_rotationMatrix_when_convertingToQuaternionAndBack_then_rotationStaysEquivalent()
    {
        Matrix4x4 rotation = new Matrix4x4().axisRotation(Math.PI / 3.0, 0.0, 0.0, 1.0);

        Quaternion q = rotation.exportToQuaternion().normalized();
        Matrix4x4 rebuilt = new Matrix4x4().importFromQuaternion(q);

        Vector3D ref = new Vector3D(1.0, 0.0, 0.0);
        Vector3D r1 = rotation.multiply(ref);
        Vector3D r2 = rebuilt.multiply(ref);

        assertThat(r2.epsilonEquals(r1, 1.0e-6)).isTrue();
    }

    @Test
    void given_affineMatrix_when_inverting_then_productIsIdentity()
    {
        Matrix4x4 matrix = new Matrix4x4()
            .scale(2.0, 3.0, 4.0)
            .multiply(new Matrix4x4().translation(5.0, -1.0, 2.0));

        Matrix4x4 inverse = matrix.invert();
        Matrix4x4 identity = matrix.multiply(inverse);

        assertThat(identity.epsilonEquals(new Matrix4x4(), 1.0e-6)).isTrue();
    }

    @Test
    void given_matrix_when_withoutTranslation_then_translationIsRemovedOnly()
    {
        Matrix4x4 matrix = new Matrix4x4().translation(3.0, -2.0, 5.0)
            .multiply(new Matrix4x4().scale(2.0, 2.0, 2.0));

        Matrix4x4 noTranslation = matrix.withoutTranslation();

        assertThat(noTranslation.extractTranslation().epsilonEquals(new Vector3D(0.0, 0.0, 0.0), EPS)).isTrue();
    }

    @Test
    void given_closeMatrices_when_epsilonEquals_then_itDependsOnTolerance()
    {
        Matrix4x4 a = new Matrix4x4();
        Matrix4x4 b = a.withVal(0, 1, 1.0e-7);

        assertThat(a.epsilonEquals(b, 1.0e-6)).isTrue();
        assertThat(a.epsilonEquals(b, 1.0e-9)).isFalse();
        assertThatThrownBy(() -> a.epsilonEquals(b, -1.0))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
