package vsdk.toolkit.common.linealAlgebra;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import vsdk.toolkit.common.VSDK;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
}
