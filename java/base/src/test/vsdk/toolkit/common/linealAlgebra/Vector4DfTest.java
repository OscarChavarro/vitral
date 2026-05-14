package vsdk.toolkit.common.linealAlgebra;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Vector4DfTest
{
    private static final float EPS = 1.0e-5f;

    @ParameterizedTest
    @MethodSource("divideByWCases")
    void given_homogeneousVector_when_dividingByW_then_handlesEdgeCases(
        Vector4Df input, Vector4Df expected)
    {
        Vector4Df result = input.dividedByW();
        assertThat(result.epsilonEquals(expected, 1.0e-5f)).isTrue();
    }

    private static Stream<Arguments> divideByWCases()
    {
        return Stream.of(
            Arguments.of(new Vector4Df(4.0f, 6.0f, 8.0f, 2.0f), new Vector4Df(2.0f, 3.0f, 4.0f, 1.0f)),
            Arguments.of(new Vector4Df(1.0f, 2.0f, 3.0f, 0.0f), new Vector4Df(1.0f, 2.0f, 3.0f, 0.0f)),
            Arguments.of(new Vector4Df(1.0f, 2.0f, 3.0f, 1.0e-8f), new Vector4Df(1.0f, 2.0f, 3.0f, 1.0e-8f))
        );
    }

    @Test
    void given_vectors_when_addAndScale_then_componentsMatch()
    {
        Vector4Df a = new Vector4Df(1.0f, 2.0f, 3.0f, 1.0f);
        Vector4Df b = new Vector4Df(-1.0f, 4.0f, 0.5f, 2.0f);

        Vector4Df sum = a.add(b);
        Vector4Df scaled = sum.multiply(2.0f);

        assertThat(scaled.epsilonEquals(new Vector4Df(0.0f, 12.0f, 7.0f, 6.0f), EPS)).isTrue();
    }

    @Test
    void given_closeVectors_when_epsilonEquals_then_itDependsOnTolerance()
    {
        Vector4Df a = new Vector4Df(1.0f, 2.0f, 3.0f, 1.0f);
        Vector4Df b = new Vector4Df(1.0f, 2.0f + 1.0e-4f, 3.0f, 1.0f);

        assertThat(a.epsilonEquals(b, 1.0e-3f)).isTrue();
        assertThat(a.epsilonEquals(b, 1.0e-6f)).isFalse();
        assertThatThrownBy(() -> a.epsilonEquals(b, -1.0f))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
