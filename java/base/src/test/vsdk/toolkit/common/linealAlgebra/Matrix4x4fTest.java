package vsdk.toolkit.common.linealAlgebra;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vsdk.toolkit.GTestAssertions.*;

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

    // Ports of the Matrix4x4fTest cases of `cpp/base/src/test/matrix4x4_tests.cpp`;
    // each test method is named `<cpp suite>_<cpp test>`.

    @Test
    void Matrix4x4fTest_StoresFloatsExactly()
    {
        // 0.1f is not a double-rounded value: it must come back bit for bit
        final float[][] values = {
            {0.1f, 0.2f, 0.3f, 0.4f},
            {1.0f / 3.0f, 2.0f, 3.0f, 4.0f},
            {5.0f, 6.0f, 7.0f, 8.0f},
            {9.0f, 10.0f, 11.0f, 12.0f}
        };
        final Matrix4x4f m = new Matrix4x4f(values);
        for ( int i = 0; i < 4; i++ ) {
            for ( int j = 0; j < 4; j++ ) {
                expectEq(m.get(i, j), values[i][j]);
            }
        }
        expectEq(m.withVal(2, 1, 0.7f).get(2, 1), 0.7f);
    }

    @Test
    void Matrix4x4fTest_ProductsComputeInFloatFromTheFirstTerm()
    {
        final float[][] a = {
            {0.1f, 0.2f, 0.3f, 0.4f},
            {0.5f, 0.6f, 0.7f, 0.8f},
            {0.9f, 1.1f, 1.2f, 1.3f},
            {1.4f, 1.5f, 1.6f, 1.7f}
        };
        final float[][] b = {
            {1.7f, 0.3f, 0.11f, 2.0f},
            {0.13f, 1.9f, 0.4f, 0.5f},
            {0.6f, 0.23f, 1.3f, 0.7f},
            {0.8f, 0.9f, 0.31f, 1.0f}
        };
        final Matrix4x4f p = new Matrix4x4f(a).multiply(new Matrix4x4f(b));
        for ( int i = 0; i < 4; i++ ) {
            for ( int j = 0; j < 4; j++ ) {
                final float expected = a[i][0] * b[0][j] + a[i][1] * b[1][j] + a[i][2] * b[2][j] + a[i][3] * b[3][j];
                expectEq(p.get(i, j), expected);
            }
        }
        final Vector4Df v = new Matrix4x4f(a).multiply(new Vector4Df(0.3f, -1.1f, 2.5f, 1.0f));
        expectEq(v.x(), a[0][0] * 0.3f + a[0][1] * -1.1f + a[0][2] * 2.5f + a[0][3] * 1.0f);
        expectEq(v.w(), a[3][0] * 0.3f + a[3][1] * -1.1f + a[3][2] * 2.5f + a[3][3] * 1.0f);
    }

    @Test
    void Matrix4x4fTest_RotationProjectionAndInverse()
    {
        final Matrix4x4f r = new Matrix4x4f().axisRotation((float) (Math.PI / 2.0), 0, 0, 1);
        expectTrue(r.multiply(new Vector3Df(1, 0, 0)).epsilonEquals(new Vector3Df(0, 1, 0), 1e-6f));
        expectTrue(r.multiply(r.transpose()).epsilonEquals(new Matrix4x4f(), 1e-6f));

        final Matrix4x4f o = new Matrix4x4f().orthogonalProjection(-2, 2, -1, 1, 1, 10);
        final Matrix4x4d od = new Matrix4x4d().orthogonalProjection(-2, 2, -1, 1, 1, 10);
        for ( int i = 0; i < 4; i++ ) {
            for ( int j = 0; j < 4; j++ ) {
                expectNear(o.get(i, j), od.get(i, j), 1e-6);
            }
        }

        final Matrix4x4f m = new Matrix4x4f().axisRotation(0.3f, 1, 2, 3).multiply(new Matrix4x4f().translation(1, -2, 0.5f));
        expectTrue(m.multiply(m.invert()).epsilonEquals(new Matrix4x4f(), 1e-5f));
        expectNear(m.determinant(), 1.0f, 1e-5f);
        expectTrue(m.withoutTranslation().extractTranslation().equals(new Vector3Df(0, 0, 0)));
    }

    @Test
    void Matrix4x4fTest_QuaternionRoundTrip()
    {
        final Matrix4x4f r = new Matrix4x4f().axisRotation(0.7f, 0.0f, 1.0f, 0.0f);
        expectTrue(r.importFromQuaternion(r.exportToQuaternion()).epsilonEquals(r, 1e-5f));
    }
}
