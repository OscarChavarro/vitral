package vsdk.toolkit.media;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
Exercises the raw pixel access of uncompressed images, whose memory order goes
from the bottom row to the top one, while pixel coordinates start on the top row.
 */
class RawImageAccessTest
{
    private static final byte[] TWO_ROWS_RGBA = {
        1, 2, 3, 4,    // Bottom row
        5, 6, 7, 8     // Top row
    };

    private static final byte[] TWO_ROWS_RGB = {
        1, 2, 3,       // Bottom row
        5, 6, 7        // Top row
    };

    @Test
    void given_rawRgbaPixels_when_set_then_theFirstRowIsTheBottomOne()
    {
        // Arrange
        RGBAImageUncompressed image = new RGBAImageUncompressed();

        // Act
        image.setRawImage(1, 2, TWO_ROWS_RGBA);

        // Assert
        assertThat(image.getXSize()).isEqualTo(1);
        assertThat(image.getYSize()).isEqualTo(2);
        assertThat(image.getPixel(0, 0).r).isEqualTo((byte)5);
        assertThat(image.getPixel(0, 1).r).isEqualTo((byte)1);
        assertThat(image.getRawImageDirectBuffer().isDirect()).isTrue();
    }

    @Test
    void given_rgbaImage_when_rawImageIsAsked_then_getsAnIndependentCopy()
    {
        // Arrange
        RGBAImageUncompressed image = new RGBAImageUncompressed();
        image.setRawImage(1, 2, TWO_ROWS_RGBA);

        // Act
        byte[] copy = image.getRawImage();
        copy[0] = 99;

        // Assert
        assertThat(image.getRawImage()).isEqualTo(TWO_ROWS_RGBA);
        assertThat(image.getRawImageSizeInBytes()).isEqualTo(8);
        assertThat(image.getRawImageDirectBuffer().position()).isZero();
    }

    @Test
    void given_rawRgbPixels_when_set_then_theFirstRowIsTheBottomOne()
    {
        // Arrange
        RGBImageUncompressed image = new RGBImageUncompressed();

        // Act
        image.setRawImage(1, 2, TWO_ROWS_RGB);

        // Assert
        assertThat(image.getPixel(0, 0).r).isEqualTo((byte)5);
        assertThat(image.getPixel(0, 1).r).isEqualTo((byte)1);
        assertThat(image.getRawImage()).isEqualTo(TWO_ROWS_RGB);
        assertThat(image.getRawImageSizeInBytes()).isEqualTo(6);
    }

    @Test
    void given_rawPixelsOfTheWrongSize_when_set_then_isRejected()
    {
        assertThatThrownBy(() -> new RGBAImageUncompressed().setRawImage(2, 2, TWO_ROWS_RGBA))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RGBImageUncompressed().setRawImage(2, 2, TWO_ROWS_RGB))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
