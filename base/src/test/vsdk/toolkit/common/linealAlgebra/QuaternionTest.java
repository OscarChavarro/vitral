package vsdk.toolkit.common.linealAlgebra;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuaternionTest
{
    private static final double EPS = 1.0e-9;

    @Test
    void given_copyFactory_when_copyingQuaternion_then_itKeepsValues()
    {
        Quaternion source = new Quaternion(new Vector3D(1.0, 2.0, 3.0), 4.0);

        Quaternion copy = Quaternion.copyOf(source);

        assertThat(copy).isNotSameAs(source);
        assertThat(copy.direction().epsilonEquals(source.direction(), EPS)).isTrue();
        assertThat(copy.magnitude()).isEqualTo(source.magnitude());
    }

    @ParameterizedTest
    @MethodSource("rotationCases")
    void given_unitQuaternions_when_rotatingVector_then_matchesExpected(
        Quaternion q, Vector3D input, Vector3D expected)
    {
        Vector3D rotated = q.rotate(input);
        assertThat(rotated.epsilonEquals(expected, 1.0e-8)).isTrue();
    }

    private static Stream<Arguments> rotationCases()
    {
        double half90 = Math.PI / 4.0;
        Quaternion rotZ90 = new Quaternion(new Vector3D(0.0, 0.0, Math.sin(half90)), Math.cos(half90));
        Quaternion identity = new Quaternion(new Vector3D(0.0, 0.0, 0.0), 1.0);

        return Stream.of(
            Arguments.of(rotZ90, new Vector3D(1.0, 0.0, 0.0), new Vector3D(0.0, 1.0, 0.0)),
            Arguments.of(identity, new Vector3D(2.0, -1.0, 0.5), new Vector3D(2.0, -1.0, 0.5))
        );
    }

    @Test
    void given_nonZeroQuaternion_when_normalizingAndConjugating_then_normAndSignAreConsistent()
    {
        Quaternion q = new Quaternion(new Vector3D(0.0, 3.0, 4.0), 12.0);

        Quaternion normalized = q.normalized();
        Quaternion conjugated = q.conjugated();

        assertThat(normalized.length()).isCloseTo(1.0, org.assertj.core.data.Offset.offset(EPS));
        assertThat(conjugated.direction().epsilonEquals(q.direction().multiply(-1.0), EPS)).isTrue();
        assertThat(conjugated.magnitude()).isEqualTo(q.magnitude());
    }

    @Test
    void given_zeroQuaternion_when_normalized_then_itReturnsSameInstanceState()
    {
        Quaternion q = new Quaternion(new Vector3D(0.0, 0.0, 0.0), 0.0);
        Quaternion normalized = q.normalized();
        assertThat(normalized.epsilonEquals(q, 0.0)).isTrue();
    }

    @Test
    void given_closeQuaternions_when_epsilonEquals_then_itDependsOnTolerance()
    {
        Quaternion a = new Quaternion(new Vector3D(0.0, 0.5, 0.0), 0.86602540378);
        Quaternion b = new Quaternion(new Vector3D(0.0, 0.5000001, 0.0), 0.86602540378);

        assertThat(a.epsilonEquals(b, 1.0e-6)).isTrue();
        assertThat(a.epsilonEquals(b, 1.0e-9)).isFalse();
        assertThatThrownBy(() -> a.epsilonEquals(b, -1.0))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
