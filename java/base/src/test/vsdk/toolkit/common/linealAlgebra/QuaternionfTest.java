package vsdk.toolkit.common.linealAlgebra;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuaternionfTest
{
    private static final float EPS = 1.0e-5f;

    @Test
    void given_copyFactory_when_copyingQuaternion_then_itKeepsValues()
    {
        Quaternionf source = new Quaternionf(new Vector3Df(1.0f, 2.0f, 3.0f), 4.0f);

        Quaternionf copy = Quaternionf.copyOf(source);

        assertThat(copy).isNotSameAs(source);
        assertThat(copy.direction().epsilonEquals(source.direction(), EPS)).isTrue();
        assertThat(copy.magnitude()).isEqualTo(source.magnitude());
    }

    @ParameterizedTest
    @MethodSource("rotationCases")
    void given_unitQuaternions_when_rotatingVector_then_matchesExpected(
        Quaternionf q, Vector3Df input, Vector3Df expected)
    {
        Vector3Df rotated = q.rotate(input);
        assertThat(rotated.epsilonEquals(expected, 1.0e-4f)).isTrue();
    }

    private static Stream<Arguments> rotationCases()
    {
        float half90 = (float) (Math.PI / 4.0);
        Quaternionf rotZ90 = new Quaternionf(new Vector3Df(0.0f, 0.0f, (float) Math.sin(half90)), (float) Math.cos(half90));
        Quaternionf identity = new Quaternionf(new Vector3Df(0.0f, 0.0f, 0.0f), 1.0f);

        return Stream.of(
            Arguments.of(rotZ90, new Vector3Df(1.0f, 0.0f, 0.0f), new Vector3Df(0.0f, 1.0f, 0.0f)),
            Arguments.of(identity, new Vector3Df(2.0f, -1.0f, 0.5f), new Vector3Df(2.0f, -1.0f, 0.5f))
        );
    }

    @Test
    void given_nonZeroQuaternion_when_normalizingAndConjugating_then_normAndSignAreConsistent()
    {
        Quaternionf q = new Quaternionf(new Vector3Df(0.0f, 3.0f, 4.0f), 12.0f);

        Quaternionf normalized = q.normalized();
        Quaternionf conjugated = q.conjugated();

        assertThat(normalized.length()).isCloseTo(1.0f, org.assertj.core.data.Offset.offset(EPS));
        assertThat(conjugated.direction().epsilonEquals(q.direction().multiply(-1.0f), EPS)).isTrue();
        assertThat(conjugated.magnitude()).isEqualTo(q.magnitude());
    }

    @Test
    void given_zeroQuaternion_when_normalized_then_itReturnsSameInstanceState()
    {
        Quaternionf q = new Quaternionf(new Vector3Df(0.0f, 0.0f, 0.0f), 0.0f);
        Quaternionf normalized = q.normalized();
        assertThat(normalized.epsilonEquals(q, 0.0f)).isTrue();
    }

    @Test
    void given_closeQuaternions_when_epsilonEquals_then_itDependsOnTolerance()
    {
        Quaternionf a = new Quaternionf(new Vector3Df(0.0f, 0.5f, 0.0f), 0.8660254f);
        Quaternionf b = new Quaternionf(new Vector3Df(0.0f, 0.5001f, 0.0f), 0.8660254f);

        assertThat(a.epsilonEquals(b, 1.0e-3f)).isTrue();
        assertThat(a.epsilonEquals(b, 1.0e-6f)).isFalse();
        assertThatThrownBy(() -> a.epsilonEquals(b, -1.0f))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
