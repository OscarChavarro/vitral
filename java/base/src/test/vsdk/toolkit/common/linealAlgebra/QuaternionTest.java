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
        Quaterniond source = new Quaterniond(new Vector3Dd(1.0, 2.0, 3.0), 4.0);

        Quaterniond copy = Quaterniond.copyOf(source);

        assertThat(copy).isNotSameAs(source);
        assertThat(copy.direction().epsilonEquals(source.direction(), EPS)).isTrue();
        assertThat(copy.magnitude()).isEqualTo(source.magnitude());
    }

    @ParameterizedTest
    @MethodSource("rotationCases")
    void given_unitQuaternions_when_rotatingVector_then_matchesExpected(
        Quaterniond q, Vector3Dd input, Vector3Dd expected)
    {
        Vector3Dd rotated = q.rotate(input);
        assertThat(rotated.epsilonEquals(expected, 1.0e-8)).isTrue();
    }

    private static Stream<Arguments> rotationCases()
    {
        double half90 = Math.PI / 4.0;
        Quaterniond rotZ90 = new Quaterniond(new Vector3Dd(0.0, 0.0, Math.sin(half90)), Math.cos(half90));
        Quaterniond identity = new Quaterniond(new Vector3Dd(0.0, 0.0, 0.0), 1.0);

        return Stream.of(
            Arguments.of(rotZ90, new Vector3Dd(1.0, 0.0, 0.0), new Vector3Dd(0.0, 1.0, 0.0)),
            Arguments.of(identity, new Vector3Dd(2.0, -1.0, 0.5), new Vector3Dd(2.0, -1.0, 0.5))
        );
    }

    @Test
    void given_nonZeroQuaternion_when_normalizingAndConjugating_then_normAndSignAreConsistent()
    {
        Quaterniond q = new Quaterniond(new Vector3Dd(0.0, 3.0, 4.0), 12.0);

        Quaterniond normalized = q.normalized();
        Quaterniond conjugated = q.conjugated();

        assertThat(normalized.length()).isCloseTo(1.0, org.assertj.core.data.Offset.offset(EPS));
        assertThat(conjugated.direction().epsilonEquals(q.direction().multiply(-1.0), EPS)).isTrue();
        assertThat(conjugated.magnitude()).isEqualTo(q.magnitude());
    }

    @Test
    void given_zeroQuaternion_when_normalized_then_itReturnsSameInstanceState()
    {
        Quaterniond q = new Quaterniond(new Vector3Dd(0.0, 0.0, 0.0), 0.0);
        Quaterniond normalized = q.normalized();
        assertThat(normalized.epsilonEquals(q, 0.0)).isTrue();
    }

    @Test
    void given_closeQuaternions_when_epsilonEquals_then_itDependsOnTolerance()
    {
        Quaterniond a = new Quaterniond(new Vector3Dd(0.0, 0.5, 0.0), 0.86602540378);
        Quaterniond b = new Quaterniond(new Vector3Dd(0.0, 0.5000001, 0.0), 0.86602540378);

        assertThat(a.epsilonEquals(b, 1.0e-6)).isTrue();
        assertThat(a.epsilonEquals(b, 1.0e-9)).isFalse();
        assertThatThrownBy(() -> a.epsilonEquals(b, -1.0))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
