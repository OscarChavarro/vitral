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
    @Test
    void given_vector_when_lengthsAndDistances_then_resultsAreExact()
    {
        final Vector3Dd a = new Vector3Dd(1.0, -2.0, 2.0);
        assertThat(a.lengthSquared()).isEqualTo(9.0);
        assertThat(a.manhattanLength()).isEqualTo(5.0);
        final Vector3Dd b = new Vector3Dd(4.0, 2.0, 2.0);
        assertThat(a.distance(b)).isEqualTo(5.0);
        assertThat(a.distanceSquared(b)).isEqualTo(25.0);
        assertThat(a.distance(b)).isEqualTo(b.subtract(a).length());
    }

    @Test
    void given_smallDivisor_when_multiplyByInverse_then_vectorIsKept()
    {
        final Vector3Dd v = new Vector3Dd(2.0, 4.0, -6.0);
        final Vector3Dd scaled = v.multiplyByInverse(2.0, 1e-6);
        assertThat(scaled.x()).isEqualTo(1.0);
        assertThat(scaled.y()).isEqualTo(2.0);
        assertThat(scaled.z()).isEqualTo(-3.0);
        assertThat(v.multiplyByInverse(1e-7, 1e-6)).isEqualTo(v);
        assertThat(v.multiplyByInverse(-1e-7, 1e-6)).isEqualTo(v);
        // The factor is computed once: x * (1 / s), not x / s
        final double s = 3.0;
        assertThat(new Vector3Dd(1.0, 0.0, 0.0).multiplyByInverse(s, 1e-6).x()).isEqualTo((1.0 / s) * 1.0);
    }

    @Test
    void given_epsilon_when_normalized_then_tinyVectorIsKept()
    {
        final Vector3Dd n = new Vector3Dd(3.0, 4.0, 12.0).normalized(1e-6);
        final double factor = 1.0 / 13.0;
        assertThat(n.x()).isEqualTo(factor * 3.0);
        assertThat(n.y()).isEqualTo(factor * 4.0);
        assertThat(n.z()).isEqualTo(factor * 12.0);
        final Vector3Dd tiny = new Vector3Dd(1e-8, 0.0, 0.0);
        assertThat(tiny.normalized(1e-6)).isEqualTo(tiny);
    }

    @Test
    void given_vectors_when_dominantAxis_then_tiesGoToFirstAxis()
    {
        assertThat(new Vector3Dd(-5.0, 1.0, 2.0).dominantAxis()).isEqualTo(0);
        assertThat(new Vector3Dd(1.0, -5.0, 2.0).dominantAxis()).isEqualTo(1);
        assertThat(new Vector3Dd(1.0, 2.0, -5.0).dominantAxis()).isEqualTo(2);
        assertThat(new Vector3Dd(3.0, -3.0, 3.0).dominantAxis()).isEqualTo(0);
        assertThat(new Vector3Dd(1.0, 3.0, -3.0).dominantAxis()).isEqualTo(1);
    }

    @Test
    void given_vector_when_component_then_matchesNamedAccessors()
    {
        final Vector3Dd value = new Vector3Dd(1.5, -2.0, 3.25);
        assertThat(value.component(0)).isEqualTo(value.x());
        assertThat(value.component(1)).isEqualTo(value.y());
        assertThat(value.component(2)).isEqualTo(value.z());
    }
}
