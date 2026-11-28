package vsdk.toolkit.io.image;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.media.RGBImageFloatUncompressed;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.RGBPixel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
Exercises the high dynamic range float image, the RGBE (Radiance) persistence
and the stream versions of the PPM exporter, as the C++ port's
image_persistence_rgbe_tests.cpp does.
*/
class ImagePersistenceRgbeTest
{
    @TempDir
    Path temporaryDirectory;

    /// Deterministic pseudo random values in a wide dynamic range
    private static double wideRangeValue(int seed)
    {
        long hash = ((long)seed * 2654435761L) & 0xFFFFFFFFL;
        double unit = (double)(hash % 10007L) / 10007.0;
        return Math.scalb(unit, (int)(hash % 41L) - 20);
    }

    private static RGBImageFloatUncompressed wideRangeImage(int width, int height)
    {
        RGBImageFloatUncompressed image = new RGBImageFloatUncompressed();
        image.init(width, height);
        for ( int y = 0; y < height; y++ ) {
            for ( int x = 0; x < width; x++ ) {
                int i = 3 * (y * width + x);
                if ( x % 7 < 3 ) {
                    // Runs of equal pixels, for the run length encoding
                    image.putPixel(x, y, 0.25, 0.5, 2.0);
                }
                else {
                    image.putPixel(x, y, wideRangeValue(i), wideRangeValue(i + 1), wideRangeValue(i + 2));
                }
            }
        }
        return image;
    }

    private static void assertRgbeClose(RGBImageFloatUncompressed expected, RGBImageFloatUncompressed actual)
    {
        assertThat(actual.getXSize()).isEqualTo(expected.getXSize());
        assertThat(actual.getYSize()).isEqualTo(expected.getYSize());
        for ( int y = 0; y < expected.getYSize(); y++ ) {
            for ( int x = 0; x < expected.getXSize(); x++ ) {
                ColorRgb e = expected.getPixel(x, y);
                ColorRgb a = actual.getPixel(x, y);
                // The mantissas share the exponent of the largest component
                double largest = Math.max(e.getR(), Math.max(e.getG(), e.getB()));
                double tolerance = largest / 128.0;
                assertThat(a.getR()).as("pixel %d, %d", x, y).isCloseTo(e.getR(), within(tolerance));
                assertThat(a.getG()).as("pixel %d, %d", x, y).isCloseTo(e.getG(), within(tolerance));
                assertThat(a.getB()).as("pixel %d, %d", x, y).isCloseTo(e.getB(), within(tolerance));
            }
        }
    }

    private static int headerLength(byte[] bytes, int lines)
    {
        int end = 0;
        for ( int found = 0; end < bytes.length && found < lines; end++ ) {
            if ( bytes[end] == '\n' ) {
                found++;
            }
        }
        return end;
    }

    private static int[] unsigned(byte[] bytes, int offset, int length)
    {
        int[] values = new int[length];
        for ( int k = 0; k < length; k++ ) {
            values[k] = bytes[offset + k] & 0xFF;
        }
        return values;
    }

    private static byte[] concatenate(String header, int[] pixels)
    {
        byte[] head = header.getBytes(StandardCharsets.ISO_8859_1);
        byte[] bytes = Arrays.copyOf(head, head.length + pixels.length);
        for ( int k = 0; k < pixels.length; k++ ) {
            bytes[head.length + k] = (byte)pixels[k];
        }
        return bytes;
    }

    @Test
    void given_floatImage_when_accessed_then_keepsUnboundedComponentsAndClampsRgbPixels()
    {
        RGBImageFloatUncompressed image = new RGBImageFloatUncompressed();
        assertThat(image.init(2, 1)).isTrue();
        assertThat(image.getPixel(1, 0).getG()).isEqualTo(0.0);

        image.putPixel(0, 0, 1500.25, -3.0, 0.5);
        ColorRgb color = image.getPixel(0, 0);
        assertThat(color.getR()).isEqualTo(1500.25);
        assertThat(color.getG()).isEqualTo(-3.0);
        assertThat(color.getB()).isEqualTo(0.5);

        RGBPixel pixel = new RGBPixel();
        image.getPixelRgb(0, 0, pixel);
        assertThat(pixel.r & 0xFF).isEqualTo(255);
        assertThat(pixel.g & 0xFF).isEqualTo(0);
        assertThat(pixel.b & 0xFF).isEqualTo(127);

        pixel.r = (byte)255;
        pixel.g = 0;
        pixel.b = (byte)51;
        image.putPixelRgb(1, 0, pixel);
        assertThat(image.getPixel(1, 0).getR()).isEqualTo(1.0);
        assertThat(image.getPixel(1, 0).getB()).isCloseTo(0.2, within(1e-15));
    }

    @Test
    void given_narrowImage_when_exported_then_writesRadianceHeaderAndSharedExponentPixels() throws Exception
    {
        RGBImageFloatUncompressed image = new RGBImageFloatUncompressed();
        image.init(2, 1);
        image.putPixel(0, 0, 1.0, 0.5, 0.0);
        image.putPixel(1, 0, 0.0, 0.0, 0.0);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        assertThat(ImagePersistenceRgbe.exportRgbe(os, image)).isTrue();
        byte[] bytes = os.toByteArray();

        String expectedHeader =
            "#?RADIANCE\n" +
            "# Image generated by VitralSDK (https://github.com/OscarChavarro/vitral)\n" +
            "FORMAT=32-bit_rle_rgbe\n" +
            "\n" +
            "-Y 1 +X 2\n";
        int headerLength = expectedHeader.length();
        assertThat(bytes.length).isEqualTo(headerLength + 8);
        assertThat(new String(bytes, 0, headerLength(bytes, 5), StandardCharsets.US_ASCII)).isEqualTo(expectedHeader);

        // A scanline narrower than 8 pixels is flat: 1.0 = (127.99995 / 256) * 2^1
        assertThat(unsigned(bytes, headerLength, 8)).containsExactly(127, 63, 0, 129, 0, 0, 0, 0);
    }

    @Test
    void given_wideConstantImage_when_exported_then_runLengthEncodesScanlines() throws Exception
    {
        RGBImageFloatUncompressed image = new RGBImageFloatUncompressed();
        image.init(100, 2);
        for ( int y = 0; y < 2; y++ ) {
            for ( int x = 0; x < 100; x++ ) {
                image.putPixel(x, y, 1.0, 0.5, 0.0);
            }
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        assertThat(ImagePersistenceRgbe.exportRgbe(os, image)).isTrue();
        byte[] bytes = os.toByteArray();
        int header = headerLength(bytes, 5);
        // Per scanline: the 4 byte marker, and one run of 100 pixels (2 bytes) per component
        assertThat(bytes.length).isEqualTo(header + 2 * (4 + 4 * 2));
        assertThat(unsigned(bytes, header, 12)).containsExactly(2, 2, 0, 100, 228, 127, 228, 63, 228, 0, 228, 129);
    }

    @Test
    void given_encodedAndFlatScanlines_when_roundTripped_then_valuesAreKept() throws Exception
    {
        int[] widths = {5, 8, 37, 300};
        for ( int width : widths ) {
            RGBImageFloatUncompressed image = wideRangeImage(width, 3);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            assertThat(ImagePersistenceRgbe.exportRgbe(os, image)).isTrue();

            ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
            RGBImageFloatUncompressed imported = ImagePersistenceRgbe.importRgbe(is);
            assertThat(imported).as("width %d", width).isNotNull();
            assertRgbeClose(image, imported);
            assertThat(is.read()).as("width %d", width).isEqualTo(-1);
        }
    }

    @Test
    void given_blackAndNegativeComponents_when_exported_then_storesThemAsZero() throws Exception
    {
        RGBImageFloatUncompressed image = new RGBImageFloatUncompressed();
        image.init(3, 1);
        image.putPixel(0, 0, 0.0, 0.0, 0.0);
        image.putPixel(1, 0, -1.0, -2.0, -0.5);
        image.putPixel(2, 0, -1.0, 4.0, 0.0);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        assertThat(ImagePersistenceRgbe.exportRgbe(os, image)).isTrue();
        RGBImageFloatUncompressed imported =
            ImagePersistenceRgbe.importRgbe(new ByteArrayInputStream(os.toByteArray()));
        assertThat(imported).isNotNull();
        for ( int x = 0; x < 2; x++ ) {
            ColorRgb color = imported.getPixel(x, 0);
            assertThat(color.getR()).isEqualTo(0.0);
            assertThat(color.getG()).isEqualTo(0.0);
            assertThat(color.getB()).isEqualTo(0.0);
        }
        // As in Radiance, a zero mantissa decodes to half a step of the exponent
        ColorRgb mixed = imported.getPixel(2, 0);
        assertThat(mixed.getR()).isCloseTo(0.0, within(4.0 / 128.0));
        assertThat(mixed.getG()).isCloseTo(4.0, within(4.0 / 128.0));
        assertThat(mixed.getB()).isCloseTo(0.0, within(4.0 / 128.0));
    }

    @Test
    void given_infiniteAndTooLargeComponents_when_exported_then_saturates() throws Exception
    {
        RGBImageFloatUncompressed image = new RGBImageFloatUncompressed();
        image.init(2, 1);
        image.putPixel(0, 0, Double.POSITIVE_INFINITY, 1.0, -2.0);
        image.putPixel(1, 0, 1e300, 0.0, 0.0);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        assertThat(ImagePersistenceRgbe.exportRgbe(os, image)).isTrue();
        byte[] bytes = os.toByteArray();
        assertThat(bytes.length).isGreaterThanOrEqualTo(8);
        assertThat(unsigned(bytes, bytes.length - 8, 8)).containsExactly(255, 255, 255, 255, 255, 255, 255, 255);
    }

    @Test
    void given_oldRunLengthRepeats_when_imported_then_repeatsThePreviousPixel() throws Exception
    {
        // A pixel, then a repeat count of 3, then another pixel
        byte[] bytes = concatenate("#?RGBE\nFORMAT=32-bit_rle_rgbe\n\n-Y 1 +X 5\n",
            new int[] {128, 64, 32, 129, 1, 1, 1, 3, 0, 0, 0, 0});

        RGBImageFloatUncompressed imported = ImagePersistenceRgbe.importRgbe(new ByteArrayInputStream(bytes));
        assertThat(imported).isNotNull();
        for ( int x = 0; x < 4; x++ ) {
            assertThat(imported.getPixel(x, 0).getR()).as("pixel %d", x).isEqualTo(128.5 / 128.0);
            assertThat(imported.getPixel(x, 0).getB()).as("pixel %d", x).isEqualTo(32.5 / 128.0);
        }
        assertThat(imported.getPixel(4, 0).getR()).isEqualTo(0.0);
    }

    @Test
    void given_streamsThatAreNotRgbe_when_imported_then_returnsNull() throws Exception
    {
        byte[] ppm = concatenate("P6\n2 1\n255\n", new int[] {1, 2, 3, 4, 5, 6});
        assertThat(ImagePersistenceRgbe.importRgbe(new ByteArrayInputStream(ppm))).isNull();

        byte[] truncated = concatenate("#?RADIANCE\nFORMAT=32-bit_rle_rgbe\n\n-Y 2 +X 3\n", new int[] {1, 2});
        assertThat(ImagePersistenceRgbe.importRgbe(new ByteArrayInputStream(truncated))).isNull();
    }

    @Test
    void given_file_when_roundTrippedThroughFrontEnd_then_valuesAreKept()
    {
        RGBImageFloatUncompressed image = wideRangeImage(64, 4);
        File file = temporaryDirectory.resolve("vitral_rgbe_round_trip.hdr").toFile();
        assertThat(ImagePersistence.exportRGBE(file, image)).isTrue();

        RGBImageFloatUncompressed imported = ImagePersistence.importRGBE(file);
        assertThat(imported).isNotNull();
        assertRgbeClose(image, imported);
    }

    @Test
    void given_image_when_exportedAsPpmToStream_then_matchesExportToFile() throws Exception
    {
        RGBImageUncompressed image = new RGBImageUncompressed();
        image.init(3, 2);
        image.putPixel(0, 0, (byte)255, (byte)0, (byte)0);
        image.putPixel(2, 1, (byte)0, (byte)128, (byte)200);

        File file = temporaryDirectory.resolve("vitral_ppm_stream.ppm").toFile();
        assertThat(ImagePersistence.exportPPM(file, image)).isTrue();
        byte[] fileBytes = Files.readAllBytes(file.toPath());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        assertThat(ImagePersistence.exportPPM(os, image)).isTrue();
        byte[] bytes = os.toByteArray();
        assertThat(bytes).isEqualTo(fileBytes);

        // Header, then the rows from the top (y = 0)
        int header = headerLength(bytes, 4);
        assertThat(bytes.length).isEqualTo(header + 18);
        assertThat(bytes[header] & 0xFF).isEqualTo(255);
        assertThat(bytes[header + 9 + 6 + 1] & 0xFF).isEqualTo(128);
        assertThat(bytes[header + 9 + 6 + 2] & 0xFF).isEqualTo(200);
    }
}
