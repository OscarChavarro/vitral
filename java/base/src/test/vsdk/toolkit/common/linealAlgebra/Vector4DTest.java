package vsdk.toolkit.common.linealAlgebra;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Vector4DTest
{
    private static final double EPS = 1.0e-9;

    @ParameterizedTest
    @MethodSource("divideByWCases")
    void given_homogeneousVector_when_dividingByW_then_handlesEdgeCases(
        Vector4D input, Vector4D expected)
    {
        Vector4D result = input.dividedByW();
        assertThat(result.epsilonEquals(expected, 1.0e-12)).isTrue();
    }

    private static Stream<Arguments> divideByWCases()
    {
        return Stream.of(
            Arguments.of(new Vector4D(4.0, 6.0, 8.0, 2.0), new Vector4D(2.0, 3.0, 4.0, 1.0)),
            Arguments.of(new Vector4D(1.0, 2.0, 3.0, 0.0), new Vector4D(1.0, 2.0, 3.0, 0.0)),
            Arguments.of(new Vector4D(1.0, 2.0, 3.0, 1.0e-12), new Vector4D(1.0, 2.0, 3.0, 1.0e-12))
        );
    }

    @Test
    void given_vectors_when_addAndScale_then_componentsMatch()
    {
        Vector4D a = new Vector4D(1.0, 2.0, 3.0, 1.0);
        Vector4D b = new Vector4D(-1.0, 4.0, 0.5, 2.0);

        Vector4D sum = a.add(b);
        Vector4D scaled = sum.multiply(2.0);

        assertThat(scaled.epsilonEquals(new Vector4D(0.0, 12.0, 7.0, 6.0), EPS)).isTrue();
    }

    @Test
    void given_closeVectors_when_epsilonEquals_then_itDependsOnTolerance()
    {
        Vector4D a = new Vector4D(1.0, 2.0, 3.0, 1.0);
        Vector4D b = new Vector4D(1.0, 2.0 + 1.0e-7, 3.0, 1.0);

        assertThat(a.epsilonEquals(b, 1.0e-6)).isTrue();
        assertThat(a.epsilonEquals(b, 1.0e-9)).isFalse();
        assertThatThrownBy(() -> a.epsilonEquals(b, -1.0))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
