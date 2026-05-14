package vsdk.toolkit.common.linealAlgebra;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Vector2DfTest
{
    private static final float EPS = 1.0e-6f;

    @ParameterizedTest
    @MethodSource("addCases")
    void given_twoVectors_when_adding_then_componentsMatch(
        Vector2Df a, Vector2Df b, Vector2Df expected)
    {
        Vector2Df c = a.add(b);
        assertThat(c.epsilonEquals(expected, EPS)).isTrue();
    }

    private static Stream<Arguments> addCases()
    {
        return Stream.of(
            Arguments.of(new Vector2Df(1.5f, -2.0f), new Vector2Df(-0.5f, 3.0f),
                new Vector2Df(1.0f, 1.0f)),
            Arguments.of(new Vector2Df(0.0f, 0.0f), new Vector2Df(0.0f, 0.0f),
                new Vector2Df(0.0f, 0.0f)),
            Arguments.of(new Vector2Df(1.0e-6f, -1.0e-6f), new Vector2Df(-1.0e-6f, 1.0e-6f),
                new Vector2Df(0.0f, 0.0f))
        );
    }

    @ParameterizedTest
    @MethodSource("scaleCases")
    void given_vector_when_scaling_then_componentsAndLengthScale(
        Vector2Df v, float scalar, Vector2Df expected)
    {
        Vector2Df scaled = v.multiply(scalar);

        assertThat(scaled.epsilonEquals(expected, 1.0e-5f)).isTrue();
        assertThat(scaled.length()).isCloseTo(
            Math.abs(scalar) * v.length(),
            org.assertj.core.data.Offset.offset(1.0e-5f));
    }

    private static Stream<Arguments> scaleCases()
    {
        return Stream.of(
            Arguments.of(new Vector2Df(2.0f, -3.0f), 2.5f, new Vector2Df(5.0f, -7.5f)),
            Arguments.of(new Vector2Df(0.0f, 0.0f), 9.0f, new Vector2Df(0.0f, 0.0f)),
            Arguments.of(new Vector2Df(4.0f, -8.0f), 0.0f, new Vector2Df(0.0f, 0.0f))
        );
    }

    @Test
    void given_vector_when_usingWithMethods_then_originalRemainsImmutable()
    {
        Vector2Df v = new Vector2Df(3.0f, 4.0f);

        Vector2Df withX = v.withX(10.0f);
        Vector2Df withY = v.withY(-1.0f);

        assertThat(withX.epsilonEquals(new Vector2Df(10.0f, 4.0f), EPS)).isTrue();
        assertThat(withY.epsilonEquals(new Vector2Df(3.0f, -1.0f), EPS)).isTrue();
        assertThat(v.epsilonEquals(new Vector2Df(3.0f, 4.0f), EPS)).isTrue();
    }

    @Test
    void given_closeVectors_when_epsilonEquals_then_itDependsOnTolerance()
    {
        Vector2Df a = new Vector2Df(1.0f, 2.0f);
        Vector2Df b = new Vector2Df(1.0f + 1.0e-4f, 2.0f - 1.0e-4f);

        assertThat(a.epsilonEquals(b, 1.0e-3f)).isTrue();
        assertThat(a.epsilonEquals(b, 1.0e-6f)).isFalse();
        assertThatThrownBy(() -> a.epsilonEquals(b, -1.0f))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
