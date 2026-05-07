package vsdk.toolkit.common.linealAlgebra;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Vector2DTest
{
    private static final double EPS = 1.0e-9;

    @ParameterizedTest
    @MethodSource("addCases")
    void given_twoVectors_when_adding_then_componentsMatch(
        Vector2D a, Vector2D b, Vector2D expected)
    {
        Vector2D c = a.add(b);
        assertThat(c.epsilonEquals(expected, EPS)).isTrue();
    }

    private static Stream<Arguments> addCases()
    {
        return Stream.of(
            Arguments.of(new Vector2D(1.5, -2.0), new Vector2D(-0.5, 3.0),
                new Vector2D(1.0, 1.0)),
            Arguments.of(new Vector2D(0.0, 0.0), new Vector2D(0.0, 0.0),
                new Vector2D(0.0, 0.0)),
            Arguments.of(new Vector2D(1.0e-12, -1.0e-12), new Vector2D(-1.0e-12, 1.0e-12),
                new Vector2D(0.0, 0.0))
        );
    }

    @ParameterizedTest
    @MethodSource("scaleCases")
    void given_vector_when_scaling_then_componentsAndLengthScale(
        Vector2D v, double scalar, Vector2D expected)
    {
        Vector2D scaled = v.multiply(scalar);

        assertThat(scaled.epsilonEquals(expected, 1.0e-12)).isTrue();
        assertThat(scaled.length()).isCloseTo(
            Math.abs(scalar) * v.length(),
            org.assertj.core.data.Offset.offset(1.0e-12));
    }

    private static Stream<Arguments> scaleCases()
    {
        return Stream.of(
            Arguments.of(new Vector2D(2.0, -3.0), 2.5, new Vector2D(5.0, -7.5)),
            Arguments.of(new Vector2D(0.0, 0.0), 9.0, new Vector2D(0.0, 0.0)),
            Arguments.of(new Vector2D(4.0, -8.0), 0.0, new Vector2D(0.0, 0.0))
        );
    }

    @Test
    void given_vector_when_usingWithMethods_then_originalRemainsImmutable()
    {
        Vector2D v = new Vector2D(3.0, 4.0);

        Vector2D withX = v.withX(10.0);
        Vector2D withY = v.withY(-1.0);

        assertThat(withX.epsilonEquals(new Vector2D(10.0, 4.0), EPS)).isTrue();
        assertThat(withY.epsilonEquals(new Vector2D(3.0, -1.0), EPS)).isTrue();
        assertThat(v.epsilonEquals(new Vector2D(3.0, 4.0), EPS)).isTrue();
    }

    @Test
    void given_closeVectors_when_epsilonEquals_then_itDependsOnTolerance()
    {
        Vector2D a = new Vector2D(1.0, 2.0);
        Vector2D b = new Vector2D(1.0 + 1.0e-7, 2.0 - 1.0e-7);

        assertThat(a.epsilonEquals(b, 1.0e-6)).isTrue();
        assertThat(a.epsilonEquals(b, 1.0e-9)).isFalse();
        assertThatThrownBy(() -> a.epsilonEquals(b, -1.0))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
