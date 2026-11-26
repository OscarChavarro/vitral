package vsdk.toolkit.common.color;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.VSDK;

import static org.assertj.core.api.Assertions.assertThat;

/**
Java-side coverage of the arithmetic of the immutable `ColorRgb`, mirroring
the C++ inline implementation of `cpp/base/src/main/vsdk/toolkit/common/color/ColorRgb.h`
(the C++ port has no dedicated test for it): every operation returns a new
color, leaves its operands untouched and evaluates its components in the
documented order.
*/
class ColorRgbTest
{
    private static void expectColor(ColorRgb c, double r, double g, double b)
    {
        assertThat(c.getR()).isEqualTo(r);
        assertThat(c.getG()).isEqualTo(g);
        assertThat(c.getB()).isEqualTo(b);
    }

    @Test
    void given_colors_when_combined_then_componentWiseResultsAndOperandsUntouched()
    {
        ColorRgb a = new ColorRgb(0.5, -0.25, 2.0);
        ColorRgb b = new ColorRgb(0.25, 0.5, -1.0);

        expectColor(a.add(b), 0.75, 0.25, 1.0);
        expectColor(a.subtract(b), 0.25, -0.75, 3.0);
        expectColor(a.addScaled(2.0, b), 0.5 + 2.0 * 0.25, -0.25 + 2.0 * 0.5,
            2.0 + 2.0 * -1.0);
        expectColor(a.multiply(2.0), 1.0, -0.5, 4.0);
        expectColor(a.multiply(b), 0.125, -0.125, -2.0);
        expectColor(a.absolute(), 0.5, 0.25, 2.0);
        expectColor(a.minimum(b), 0.25, -0.25, -1.0);
        expectColor(a.maximum(b), 0.5, 0.5, 2.0);
        expectColor(a, 0.5, -0.25, 2.0);
        expectColor(b, 0.25, 0.5, -1.0);
    }

    @Test
    void given_zeroDivisors_when_divided_then_componentsAreKept()
    {
        ColorRgb a = new ColorRgb(3.0, 6.0, -9.0);

        // scaleInverse multiplies by the reciprocal, computed once
        expectColor(a.scaleInverse(3.0), (1.0 / 3.0) * 3.0, (1.0 / 3.0) * 6.0,
            (1.0 / 3.0) * -9.0);
        expectColor(a.scaleInverse(0.0), 3.0, 6.0, -9.0);
        expectColor(a.divide(new ColorRgb(2.0, 0.0, -3.0)), 1.5, 6.0, 3.0);
        expectColor(a.divide(new ColorRgb()), 3.0, 6.0, -9.0);
    }

    @Test
    void given_color_when_reduced_then_scalarSummariesMatch()
    {
        ColorRgb c = new ColorRgb(-0.5, 0.25, 0.75);

        assertThat(c.sumAbsComponents()).isEqualTo(1.5);
        assertThat(c.maximumComponent()).isEqualTo(0.75);
        assertThat(new ColorRgb(0.9, 0.1, 0.3).maximumComponent()).isEqualTo(0.9);
        assertThat(new ColorRgb(0.1, 0.9, 0.3).maximumComponent()).isEqualTo(0.9);
        assertThat(c.average()).isEqualTo((-0.5 + 0.25 + 0.75) / 3.0);
    }

    @Test
    void given_nearlyBlackColors_when_tested_then_epsilonBoundIsStrict()
    {
        double e = VSDK.EPSILON;

        assertThat(new ColorRgb().isBlack()).isTrue();
        assertThat(new ColorRgb(0.5 * e, -0.5 * e, 0.0).isBlack()).isTrue();
        assertThat(new ColorRgb(e, 0.0, 0.0).isBlack()).isFalse();
        assertThat(new ColorRgb(0.0, -e, 0.0).isBlack()).isFalse();
        assertThat(new ColorRgb(0.0, 0.0, 2.0 * e).isBlack()).isFalse();
    }
}
