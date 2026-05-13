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
        Vector3D a, Vector3D b, Vector3D expected)
    {
        Vector3D cross = a.crossProduct(b);

        assertThat(cross.epsilonEquals(expected, 1.0e-9)).isTrue();
        assertThat(cross.dotProduct(a)).isCloseTo(0.0, org.assertj.core.data.Offset.offset(1.0e-9));
        assertThat(cross.dotProduct(b)).isCloseTo(0.0, org.assertj.core.data.Offset.offset(1.0e-9));
    }

    private static Stream<Arguments> crossCases()
    {
        return Stream.of(
            Arguments.of(new Vector3D(1.0, 0.0, 0.0), new Vector3D(0.0, 1.0, 0.0),
                new Vector3D(0.0, 0.0, 1.0)),
            Arguments.of(new Vector3D(0.0, 0.0, 0.0), new Vector3D(1.0, 2.0, 3.0),
                new Vector3D(0.0, 0.0, 0.0)),
            Arguments.of(new Vector3D(1.0e-12, 0.0, 0.0), new Vector3D(0.0, 1.0e-12, 0.0),
                new Vector3D(0.0, 0.0, 1.0e-24))
        );
    }

    @ParameterizedTest
    @MethodSource("normalizeCases")
    void given_vector_when_normalized_then_handlesZerosAndSmallValues(
        Vector3D input, Vector3D expected, double tolerance)
    {
        Vector3D normalized = input.normalized();
        assertThat(normalized.epsilonEquals(expected, tolerance)).isTrue();
    }

    private static Stream<Arguments> normalizeCases()
    {
        return Stream.of(
            Arguments.of(new Vector3D(3.0, 4.0, 12.0), new Vector3D(3.0, 4.0, 12.0).multiply(1.0 / 13.0), 1.0e-12),
            Arguments.of(new Vector3D(0.0, 0.0, 0.0), new Vector3D(0.0, 0.0, 0.0), 0.0),
            Arguments.of(new Vector3D(VSDK.EPSILON / 2.0, 0.0, 0.0), new Vector3D(VSDK.EPSILON / 2.0, 0.0, 0.0), 0.0)
        );
    }

    @Test
    void given_sphericalAngles_when_roundTripping_then_coordinatesMatch()
    {
        Vector3D original = new Vector3D(2.0, 2.0, 1.0);

        double r = original.length();
        double theta = original.obtainSphericalThetaAngle();
        double phi = original.obtainSphericalPhiAngle();
        Vector3D rebuilt = Vector3D.fromSpherical(r, theta, phi);

        assertThat(rebuilt.epsilonEquals(original, 1.0e-6)).isTrue();
    }

    @Test
    void given_closeVectors_when_epsilonEquals_then_itDependsOnTolerance()
    {
        Vector3D a = new Vector3D(1.0, 2.0, 3.0);
        Vector3D b = new Vector3D(1.0 + 1.0e-7, 2.0, 3.0 - 1.0e-7);

        assertThat(a.epsilonEquals(b, 1.0e-6)).isTrue();
        assertThat(a.epsilonEquals(b, 1.0e-9)).isFalse();
        assertThatThrownBy(() -> a.epsilonEquals(b, -1.0))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
