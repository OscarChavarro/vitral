package vsdk.toolkit.common.color;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
Java-side coverage of `Cie` (the C++ port has no dedicated test for it),
checking the properties of [WARD1996] Sec. 6.1.2 and [MART1991] that the
C++ implementation documents: both transforms are inverse of each other,
RGB white maps to the white point chromaticity with Y = 1, and the gray
factors are the classic 0.299 / 0.587 / 0.114 only for the NTSC primaries
with the Illuminant C white point.
*/
class CieTest
{
    private static final double EPS = 1e-9;

    @Test
    void given_defaultPrimaries_when_constructed_then_nominalCrtPrimariesAreKept()
    {
        Cie cie = new Cie();

        assertThat(cie.getRedX()).isEqualTo(0.640);
        assertThat(cie.getRedY()).isEqualTo(0.330);
        assertThat(cie.getGreenX()).isEqualTo(0.290);
        assertThat(cie.getGreenY()).isEqualTo(0.600);
        assertThat(cie.getBlueX()).isEqualTo(0.150);
        assertThat(cie.getBlueY()).isEqualTo(0.060);
        assertThat(cie.getWhiteX()).isEqualTo(0.3333333333);
        assertThat(cie.getWhiteY()).isEqualTo(0.3333333333);
        assertThat(cie.getLuminousEfficacy()).isEqualTo(183.07);
    }

    @Test
    void given_rgb_when_transformedToXyzAndBack_then_isRecovered()
    {
        Cie cie = new Cie();
        double[] rgb = {0.2, 0.7, 0.4};
        double[] xyz = new double[3];
        double[] back = new double[3];

        cie.transformColorFromRGB2XYZ(rgb, xyz);
        cie.transformColorFromXYZ2RGB(xyz, back);
        for ( int i = 0; i < 3; i++ ) {
            assertThat(back[i]).isCloseTo(rgb[i], within(EPS));
        }
        // The Y of the color is its gray value
        assertThat(xyz[1]).isCloseTo(cie.spectrumGray(0.2, 0.7, 0.4), within(EPS));
    }

    @Test
    void given_white_when_transformed_then_hasWhitePointChromaticityAndUnitLuminance()
    {
        Cie cie = new Cie(0.670, 0.330, 0.210, 0.710, 0.140, 0.080,
            0.3101, 0.3162);
        double[] white = {1.0, 1.0, 1.0};
        double[] xyz = new double[3];

        cie.transformColorFromRGB2XYZ(white, xyz);
        double sum = xyz[0] + xyz[1] + xyz[2];
        assertThat(xyz[1]).isCloseTo(1.0, within(EPS));
        assertThat(xyz[0] / sum).isCloseTo(0.3101, within(EPS));
        assertThat(xyz[1] / sum).isCloseTo(0.3162, within(EPS));
        assertThat(cie.spectrumGray(1.0, 1.0, 1.0)).isCloseTo(1.0, within(EPS));
        assertThat(cie.spectrumLuminance(1.0, 1.0, 1.0))
            .isCloseTo(183.07, within(1e-7));
    }

    @Test
    void given_ntscPrimariesWithIlluminantC_when_grayFactorsComputed_then_areTheClassicWeights()
    {
        Cie ntsc = new Cie(0.670, 0.330, 0.210, 0.710, 0.140, 0.080,
            0.3101, 0.3162);

        assertThat(ntsc.spectrumGray(1, 0, 0)).isCloseTo(0.299, within(1e-3));
        assertThat(ntsc.spectrumGray(0, 1, 0)).isCloseTo(0.587, within(1e-3));
        assertThat(ntsc.spectrumGray(0, 0, 1)).isCloseTo(0.114, within(1e-3));
    }
}
