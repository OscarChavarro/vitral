package vsdk.toolkit.common.linealAlgebra;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import vsdk.toolkit.common.VSDK;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vsdk.toolkit.GTestAssertions.*;

class Vector3DfTest
{
    private static final float EPS = 1.0e-5f;

    @ParameterizedTest
    @MethodSource("crossCases")
    void given_vectors_when_crossProduct_then_resultIsExpected(
        Vector3Df a, Vector3Df b, Vector3Df expected)
    {
        Vector3Df cross = a.crossProduct(b);

        assertThat(cross.epsilonEquals(expected, 1.0e-5f)).isTrue();
        assertThat(cross.dotProduct(a)).isCloseTo(0.0f, org.assertj.core.data.Offset.offset(1.0e-5f));
        assertThat(cross.dotProduct(b)).isCloseTo(0.0f, org.assertj.core.data.Offset.offset(1.0e-5f));
    }

    private static Stream<Arguments> crossCases()
    {
        return Stream.of(
            Arguments.of(new Vector3Df(1.0f, 0.0f, 0.0f), new Vector3Df(0.0f, 1.0f, 0.0f),
                new Vector3Df(0.0f, 0.0f, 1.0f)),
            Arguments.of(new Vector3Df(0.0f, 0.0f, 0.0f), new Vector3Df(1.0f, 2.0f, 3.0f),
                new Vector3Df(0.0f, 0.0f, 0.0f)),
            Arguments.of(new Vector3Df(1.0e-5f, 0.0f, 0.0f), new Vector3Df(0.0f, 1.0e-5f, 0.0f),
                new Vector3Df(0.0f, 0.0f, 1.0e-10f))
        );
    }

    @ParameterizedTest
    @MethodSource("normalizeCases")
    void given_vector_when_normalized_then_handlesZerosAndSmallValues(
        Vector3Df input, Vector3Df expected, float tolerance)
    {
        Vector3Df normalized = input.normalized();
        assertThat(normalized.epsilonEquals(expected, tolerance)).isTrue();
    }

    private static Stream<Arguments> normalizeCases()
    {
        return Stream.of(
            Arguments.of(new Vector3Df(3.0f, 4.0f, 12.0f), new Vector3Df(3.0f, 4.0f, 12.0f).multiply(1.0f / 13.0f), 1.0e-5f),
            Arguments.of(new Vector3Df(0.0f, 0.0f, 0.0f), new Vector3Df(0.0f, 0.0f, 0.0f), 0.0f),
            Arguments.of(new Vector3Df((float) VSDK.EPSILON / 2.0f, 0.0f, 0.0f), new Vector3Df((float) VSDK.EPSILON / 2.0f, 0.0f, 0.0f), 0.0f)
        );
    }

    @Test
    void given_closeVectors_when_epsilonEquals_then_itDependsOnTolerance()
    {
        Vector3Df a = new Vector3Df(1.0f, 2.0f, 3.0f);
        Vector3Df b = new Vector3Df(1.0f + 1.0e-4f, 2.0f, 3.0f - 1.0e-4f);

        assertThat(a.epsilonEquals(b, 1.0e-3f)).isTrue();
        assertThat(a.epsilonEquals(b, 1.0e-6f)).isFalse();
        assertThatThrownBy(() -> a.epsilonEquals(b, -1.0f))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // Ports of the Vector3DfTest cases of `cpp/base/src/test/vector_tests.cpp`;
    // each test method is named `<cpp suite>_<cpp test>`.

    @Test
    void Vector3DfTest_LengthsAndDistances()
    {
        final Vector3Df a = new Vector3Df(1.0f, -2.0f, 2.0f);
        expectEq(a.lengthSquared(), 9.0f);
        expectEq(a.manhattanLength(), 5.0f);
        final Vector3Df b = new Vector3Df(4.0f, 2.0f, 2.0f);
        expectEq(a.distance(b), 5.0f);
        expectEq(a.distanceSquared(b), 25.0f);
        expectEq(a.distance(b), b.subtract(a).length());
    }

    @Test
    void Vector3DfTest_MultiplyByInverseKeepsSmallDivisors()
    {
        final Vector3Df v = new Vector3Df(2.0f, 4.0f, -6.0f);
        final Vector3Df scaled = v.multiplyByInverse(2.0f, 1e-6f);
        expectEq(scaled.x(), 1.0f);
        expectEq(scaled.y(), 2.0f);
        expectEq(scaled.z(), -3.0f);
        expectTrue(v.multiplyByInverse(1e-7f, 1e-6f).equals(v));
        expectTrue(v.multiplyByInverse(-1e-7f, 1e-6f).equals(v));
        // The factor is computed once: x * (1 / s), not x / s
        final float s = 3.0f;
        expectEq(new Vector3Df(1.0f, 0.0f, 0.0f).multiplyByInverse(s, 1e-6f).x(), (1.0f / s) * 1.0f);
    }

    @Test
    void Vector3DfTest_NormalizedWithEpsilon()
    {
        final Vector3Df n = new Vector3Df(3.0f, 4.0f, 12.0f).normalized(1e-6f);
        final float factor = 1.0f / 13.0f;
        expectEq(n.x(), factor * 3.0f);
        expectEq(n.y(), factor * 4.0f);
        expectEq(n.z(), factor * 12.0f);
        final Vector3Df tiny = new Vector3Df(1e-8f, 0.0f, 0.0f);
        expectTrue(tiny.normalized(1e-6f).equals(tiny));
    }

    @Test
    void Vector3DfTest_DominantAxis()
    {
        expectEq(new Vector3Df(-5.0f, 1.0f, 2.0f).dominantAxis(), 0);
        expectEq(new Vector3Df(1.0f, -5.0f, 2.0f).dominantAxis(), 1);
        expectEq(new Vector3Df(1.0f, 2.0f, -5.0f).dominantAxis(), 2);
        expectEq(new Vector3Df(3.0f, -3.0f, 3.0f).dominantAxis(), 0);
        expectEq(new Vector3Df(1.0f, 3.0f, -3.0f).dominantAxis(), 1);
    }

    @Test
    void Vector3DfTest_AssignmentReplacesValue()
    {
        Vector3Df v = new Vector3Df(1.0f, 2.0f, 3.0f);
        v = v.withY(5.0f);
        expectTrue(v.equals(new Vector3Df(1.0f, 5.0f, 3.0f)));
    }
    @Test
    void indexedComponentsMatchNamedAccessors()
    {
        Vector3Df vector = new Vector3Df(1.5f, -2.0f, 3.25f);
        expectEq(vector.component(0), vector.x());
        expectEq(vector.component(1), vector.y());
        expectEq(vector.component(2), vector.z());
    }
}
