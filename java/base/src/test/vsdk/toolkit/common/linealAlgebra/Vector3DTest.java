package vsdk.toolkit.common.linealAlgebra;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import vsdk.toolkit.common.VSDK;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Vector3DTest
{
    private static final double EPS = 1.0e-9;

    @ParameterizedTest
    @MethodSource("crossCases")
    void given_vectors_when_crossProduct_then_resultIsExpected(
        Vector3Dd a, Vector3Dd b, Vector3Dd expected)
    {
        Vector3Dd cross = a.crossProduct(b);

        assertThat(cross.epsilonEquals(expected, 1.0e-9)).isTrue();
        assertThat(cross.dotProduct(a)).isCloseTo(0.0, org.assertj.core.data.Offset.offset(1.0e-9));
        assertThat(cross.dotProduct(b)).isCloseTo(0.0, org.assertj.core.data.Offset.offset(1.0e-9));
    }

    private static Stream<Arguments> crossCases()
    {
        return Stream.of(
            Arguments.of(new Vector3Dd(1.0, 0.0, 0.0), new Vector3Dd(0.0, 1.0, 0.0),
                new Vector3Dd(0.0, 0.0, 1.0)),
            Arguments.of(new Vector3Dd(0.0, 0.0, 0.0), new Vector3Dd(1.0, 2.0, 3.0),
                new Vector3Dd(0.0, 0.0, 0.0)),
            Arguments.of(new Vector3Dd(1.0e-12, 0.0, 0.0), new Vector3Dd(0.0, 1.0e-12, 0.0),
                new Vector3Dd(0.0, 0.0, 1.0e-24))
        );
    }

    @ParameterizedTest
    @MethodSource("normalizeCases")
    void given_vector_when_normalized_then_handlesZerosAndSmallValues(
        Vector3Dd input, Vector3Dd expected, double tolerance)
    {
        Vector3Dd normalized = input.normalized();
        assertThat(normalized.epsilonEquals(expected, tolerance)).isTrue();
    }

    private static Stream<Arguments> normalizeCases()
    {
        return Stream.of(
            Arguments.of(new Vector3Dd(3.0, 4.0, 12.0), new Vector3Dd(3.0, 4.0, 12.0).multiply(1.0 / 13.0), 1.0e-12),
            Arguments.of(new Vector3Dd(0.0, 0.0, 0.0), new Vector3Dd(0.0, 0.0, 0.0), 0.0),
            Arguments.of(new Vector3Dd(VSDK.EPSILON / 2.0, 0.0, 0.0), new Vector3Dd(VSDK.EPSILON / 2.0, 0.0, 0.0), 0.0)
        );
    }

    @Test
    void given_sphericalAngles_when_roundTripping_then_coordinatesMatch()
    {
        Vector3Dd original = new Vector3Dd(2.0, 2.0, 1.0);

        double r = original.length();
        double theta = original.obtainSphericalThetaAngle();
        double phi = original.obtainSphericalPhiAngle();
        Vector3Dd rebuilt = Vector3Dd.fromSpherical(r, theta, phi);

        assertThat(rebuilt.epsilonEquals(original, 1.0e-6)).isTrue();
    }

    @Test
    void given_closeVectors_when_epsilonEquals_then_itDependsOnTolerance()
    {
        Vector3Dd a = new Vector3Dd(1.0, 2.0, 3.0);
        Vector3Dd b = new Vector3Dd(1.0 + 1.0e-7, 2.0, 3.0 - 1.0e-7);

        assertThat(a.epsilonEquals(b, 1.0e-6)).isTrue();
        assertThat(a.epsilonEquals(b, 1.0e-9)).isFalse();
        assertThatThrownBy(() -> a.epsilonEquals(b, -1.0))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
