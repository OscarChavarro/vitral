#include "java/lang/String.h"
#include <gtest/gtest.h>

#include <cmath>
#include <cstdio>
#include <cstring>
#include <vector>

#include "java/io/File.h"
#include "java/io/FileInputStream.h"
#include "java/io/InputStream.h"
#include "java/io/OutputStream.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/io/image/ImagePersistenceRgbe.h"
#include "vsdk/toolkit/media/RGBImageFloatUncompressed.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/RGBPixel.h"

namespace {

class MemoryOutputStream : public java::OutputStream {
  public:
    std::vector<unsigned char> bytes;

    void write(int value) override { bytes.push_back(static_cast<unsigned char>(value)); }
    void write(const unsigned char *buffer, int offset, int length) override {
        bytes.insert(bytes.end(), buffer + offset, buffer + offset + length);
    }
    void flush() override {}
    void close() override {}
};

class MemoryInputStream : public java::InputStream {
  private:
    const std::vector<unsigned char> &bytes;
    size_t position;

  public:
    explicit MemoryInputStream(const std::vector<unsigned char> &inBytes) : bytes(inBytes), position(0) {}

    int read() override { return position < bytes.size() ? bytes[position++] : -1; }
    int read(unsigned char *buffer, int offset, int length) override {
        if ( position >= bytes.size() ) {
            return -1;
        }
        int n = 0;
        while ( n < length && position < bytes.size() ) {
            buffer[offset + n++] = bytes[position++];
        }
        return n;
    }
    void close() override {}
};

/// Deterministic pseudo random values in a wide dynamic range
double wideRangeValue(int seed) {
    const unsigned int hash = static_cast<unsigned int>(seed) * 2654435761u;
    const double unit = static_cast<double>(hash % 10007u) / 10007.0;
    return std::ldexp(unit, static_cast<int>(hash % 41u) - 20);
}

RGBImageFloatUncompressed *wideRangeImage(int width, int height) {
    RGBImageFloatUncompressed *image = new RGBImageFloatUncompressed();
    image->init(width, height);
    for ( int y = 0; y < height; y++ ) {
        for ( int x = 0; x < width; x++ ) {
            const int i = 3 * (y * width + x);
            if ( x % 7 < 3 ) {
                // Runs of equal pixels, for the run length encoding
                image->putPixel(x, y, 0.25, 0.5, 2.0);
            } else {
                image->putPixel(x, y, wideRangeValue(i), wideRangeValue(i + 1), wideRangeValue(i + 2));
            }
        }
    }
    return image;
}

void expectRgbeClose(const RGBImageFloatUncompressed &expected, const RGBImageFloatUncompressed &actual) {
    ASSERT_EQ(expected.getXSize(), actual.getXSize());
    ASSERT_EQ(expected.getYSize(), actual.getYSize());
    for ( int y = 0; y < expected.getYSize(); y++ ) {
        for ( int x = 0; x < expected.getXSize(); x++ ) {
            const ColorRgb e = expected.getPixel(x, y);
            const ColorRgb a = actual.getPixel(x, y);
            // The mantissas share the exponent of the largest component
            const double largest = std::fmax(e.getR(), std::fmax(e.getG(), e.getB()));
            const double tolerance = largest / 128.0;
            EXPECT_NEAR(e.getR(), a.getR(), tolerance) << "pixel " << x << ", " << y;
            EXPECT_NEAR(e.getG(), a.getG(), tolerance) << "pixel " << x << ", " << y;
            EXPECT_NEAR(e.getB(), a.getB(), tolerance) << "pixel " << x << ", " << y;
        }
    }
}

java::String headerOf(const std::vector<unsigned char> &bytes, int lines) {
    size_t end = 0;
    for ( int found = 0; end < bytes.size() && found < lines; end++ ) {
        if ( bytes[end] == '\n' ) {
            found++;
        }
    }
    return java::String(std::string(bytes.begin(), bytes.begin() + static_cast<long>(end)).c_str());
}

}

TEST(RGBImageFloatUncompressedTest, KeepsUnboundedComponentsAndClampsRgbPixels) {
    RGBImageFloatUncompressed image;
    ASSERT_TRUE(image.init(2, 1));
    EXPECT_EQ(0.0, image.getPixel(1, 0).getG());

    image.putPixel(0, 0, 1500.25, -3.0, 0.5);
    const ColorRgb color = image.getPixel(0, 0);
    EXPECT_EQ(1500.25, color.getR());
    EXPECT_EQ(-3.0, color.getG());
    EXPECT_EQ(0.5, color.getB());

    RGBPixel pixel;
    image.getPixelRgb(0, 0, &pixel);
    EXPECT_EQ(255, (unsigned char)pixel.r);
    EXPECT_EQ(0, (unsigned char)pixel.g);
    EXPECT_EQ(127, (unsigned char)pixel.b);

    pixel.r = (char)255;
    pixel.g = 0;
    pixel.b = (char)51;
    image.putPixelRgb(1, 0, &pixel);
    EXPECT_EQ(1.0, image.getPixel(1, 0).getR());
    EXPECT_DOUBLE_EQ(0.2, image.getPixel(1, 0).getB());
}

TEST(ImagePersistenceRgbeTest, WritesRadianceHeaderAndSharedExponentPixels) {
    RGBImageFloatUncompressed image;
    image.init(2, 1);
    image.putPixel(0, 0, 1.0, 0.5, 0.0);
    image.putPixel(1, 0, 0.0, 0.0, 0.0);

    MemoryOutputStream os;
    ASSERT_TRUE(ImagePersistenceRgbe::exportRgbe(os, &image));

    const char *expectedHeader =
        "#?RADIANCE\n"
        "# Image generated by VitralSDK (https://github.com/OscarChavarro/vitral)\n"
        "FORMAT=32-bit_rle_rgbe\n"
        "\n"
        "-Y 1 +X 2\n";
    const size_t headerLength = std::strlen(expectedHeader);
    ASSERT_EQ(headerLength + 8, os.bytes.size());
    EXPECT_STREQ(expectedHeader, headerOf(os.bytes, 5).toCString());

    // A scanline narrower than 8 pixels is flat: 1.0 = (127.99995 / 256) * 2^1
    const unsigned char expectedPixels[8] = {127, 63, 0, 129, 0, 0, 0, 0};
    EXPECT_EQ(0, std::memcmp(expectedPixels, &os.bytes[headerLength], 8));
}

TEST(ImagePersistenceRgbeTest, RunLengthEncodesWideConstantScanlines) {
    RGBImageFloatUncompressed image;
    image.init(100, 2);
    for ( int y = 0; y < 2; y++ ) {
        for ( int x = 0; x < 100; x++ ) {
            image.putPixel(x, y, 1.0, 0.5, 0.0);
        }
    }

    MemoryOutputStream os;
    ASSERT_TRUE(ImagePersistenceRgbe::exportRgbe(os, &image));
    const java::String header = headerOf(os.bytes, 5);
    // Per scanline: the 4 byte marker, and one run of 100 pixels (2 bytes) per component
    ASSERT_EQ(static_cast<size_t>(header.length()) + 2 * (4 + 4 * 2), os.bytes.size());
    const unsigned char expectedScanline[12] = {2, 2, 0, 100, 228, 127, 228, 63, 228, 0, 228, 129};
    EXPECT_EQ(0, std::memcmp(expectedScanline, &os.bytes[header.length()], 12));
}

TEST(ImagePersistenceRgbeTest, RoundTripsEncodedAndFlatScanlines) {
    const int widths[] = {5, 8, 37, 300};
    for ( int width : widths ) {
        RGBImageFloatUncompressed *image = wideRangeImage(width, 3);
        MemoryOutputStream os;
        ASSERT_TRUE(ImagePersistenceRgbe::exportRgbe(os, image));

        MemoryInputStream is(os.bytes);
        RGBImageFloatUncompressed *imported = ImagePersistenceRgbe::importRgbe(is);
        ASSERT_NE(nullptr, imported) << "width " << width;
        expectRgbeClose(*image, *imported);
        EXPECT_EQ(-1, is.read()) << "width " << width;
        delete imported;
        delete image;
    }
}

TEST(ImagePersistenceRgbeTest, StoresBlackAndNegativeComponentsAsZero) {
    RGBImageFloatUncompressed image;
    image.init(3, 1);
    image.putPixel(0, 0, 0.0, 0.0, 0.0);
    image.putPixel(1, 0, -1.0, -2.0, -0.5);
    image.putPixel(2, 0, -1.0, 4.0, 0.0);

    MemoryOutputStream os;
    ASSERT_TRUE(ImagePersistenceRgbe::exportRgbe(os, &image));
    MemoryInputStream is(os.bytes);
    RGBImageFloatUncompressed *imported = ImagePersistenceRgbe::importRgbe(is);
    ASSERT_NE(nullptr, imported);
    for ( int x = 0; x < 2; x++ ) {
        const ColorRgb color = imported->getPixel(x, 0);
        EXPECT_EQ(0.0, color.getR());
        EXPECT_EQ(0.0, color.getG());
        EXPECT_EQ(0.0, color.getB());
    }
    // As in Radiance, a zero mantissa decodes to half a step of the exponent
    const ColorRgb mixed = imported->getPixel(2, 0);
    EXPECT_NEAR(0.0, mixed.getR(), 4.0 / 128.0);
    EXPECT_NEAR(4.0, mixed.getG(), 4.0 / 128.0);
    EXPECT_NEAR(0.0, mixed.getB(), 4.0 / 128.0);
    delete imported;
}

TEST(ImagePersistenceRgbeTest, SaturatesInfiniteAndTooLargeComponents) {
    RGBImageFloatUncompressed image;
    image.init(2, 1);
    image.putPixel(0, 0, HUGE_VAL, 1.0, -2.0);
    image.putPixel(1, 0, 1e300, 0.0, 0.0);

    MemoryOutputStream os;
    ASSERT_TRUE(ImagePersistenceRgbe::exportRgbe(os, &image));
    const unsigned char saturated[8] = {255, 255, 255, 255, 255, 255, 255, 255};
    ASSERT_GE(os.bytes.size(), 8u);
    EXPECT_EQ(0, std::memcmp(saturated, &os.bytes[os.bytes.size() - 8], 8));
}

TEST(ImagePersistenceRgbeTest, ReadsOldRunLengthRepeats) {
    const char *header = "#?RGBE\nFORMAT=32-bit_rle_rgbe\n\n-Y 1 +X 5\n";
    std::vector<unsigned char> bytes(header, header + std::strlen(header));
    // A pixel, then a repeat count of 3, then another pixel
    const unsigned char pixels[12] = {128, 64, 32, 129, 1, 1, 1, 3, 0, 0, 0, 0};
    bytes.insert(bytes.end(), pixels, pixels + 12);

    MemoryInputStream is(bytes);
    RGBImageFloatUncompressed *imported = ImagePersistenceRgbe::importRgbe(is);
    ASSERT_NE(nullptr, imported);
    for ( int x = 0; x < 4; x++ ) {
        EXPECT_DOUBLE_EQ(128.5 / 128.0, imported->getPixel(x, 0).getR()) << "pixel " << x;
        EXPECT_DOUBLE_EQ(32.5 / 128.0, imported->getPixel(x, 0).getB()) << "pixel " << x;
    }
    EXPECT_EQ(0.0, imported->getPixel(4, 0).getR());
    delete imported;
}

TEST(ImagePersistenceRgbeTest, RejectsStreamsThatAreNotRgbe) {
    const char *ppm = "P6\n2 1\n255\n\x01\x02\x03\x04\x05\x06";
    std::vector<unsigned char> bytes(ppm, ppm + std::strlen(ppm));
    MemoryInputStream is(bytes);
    EXPECT_EQ(nullptr, ImagePersistenceRgbe::importRgbe(is));

    const char *truncated = "#?RADIANCE\nFORMAT=32-bit_rle_rgbe\n\n-Y 2 +X 3\n\x01\x02";
    std::vector<unsigned char> truncatedBytes(truncated, truncated + std::strlen(truncated));
    MemoryInputStream truncatedStream(truncatedBytes);
    EXPECT_EQ(nullptr, ImagePersistenceRgbe::importRgbe(truncatedStream));
}

TEST(ImagePersistenceRgbeTest, FrontEndRoundTripsFiles) {
    RGBImageFloatUncompressed *image = wideRangeImage(64, 4);
    java::String path = java::String(testing::TempDir().c_str()) + "vitral_rgbe_round_trip.hdr";
    java::File file(path.c_str());
    ASSERT_TRUE(ImagePersistence::exportRGBE(file, image));

    RGBImageFloatUncompressed *imported = ImagePersistence::importRGBE(file);
    ASSERT_NE(nullptr, imported);
    expectRgbeClose(*image, *imported);
    delete imported;
    delete image;
    std::remove(path.c_str());
}

TEST(ImagePersistenceTest, ExportPpmToStreamMatchesExportToFile) {
    RGBImageUncompressed image;
    image.init(3, 2);
    image.putPixel(0, 0, (char)255, 0, 0);
    image.putPixel(2, 1, 0, (char)128, (char)200);

    java::String path = java::String(testing::TempDir().c_str()) + "vitral_ppm_stream.ppm";
    java::File file(path.c_str());
    ASSERT_TRUE(ImagePersistence::exportPPM(file, &image));
    std::vector<unsigned char> fileBytes;
    {
        java::FileInputStream fis(path.c_str());
        for ( int c = fis.read(); c >= 0; c = fis.read() ) {
            fileBytes.push_back(static_cast<unsigned char>(c));
        }
    }
    std::remove(path.c_str());

    MemoryOutputStream os;
    ASSERT_TRUE(ImagePersistence::exportPPM(os, &image));
    EXPECT_EQ(fileBytes, os.bytes);

    // Header, then the rows from the top (y = 0)
    const java::String header = headerOf(os.bytes, 4);
    ASSERT_EQ(static_cast<size_t>(header.length()) + 18, os.bytes.size());
    EXPECT_EQ(255, os.bytes[header.length()]);
    EXPECT_EQ(128, os.bytes[header.length() + 9 + 6 + 1]);
    EXPECT_EQ(200, os.bytes[header.length() + 9 + 6 + 2]);
}

TEST(ImagePersistenceTest, ExportPngToStreamMatchesExportToFileAndReadsBack) {
    RGBImageUncompressed image;
    image.init(5, 3);
    for ( int y = 0; y < 3; y++ ) {
        for ( int x = 0; x < 5; x++ ) {
            image.putPixel(x, y, (char)(40 * x), (char)(100 * y), (char)(x * y + 7));
        }
    }

    java::String path = java::String(testing::TempDir().c_str()) + "vitral_png_stream.png";
    java::File file(path.c_str());
    if ( !ImagePersistence::exportPNG(file, &image) ) {
        GTEST_SKIP() << "Built without PNG support";
    }
    std::vector<unsigned char> fileBytes;
    {
        java::FileInputStream fis(path.c_str());
        for ( int c = fis.read(); c >= 0; c = fis.read() ) {
            fileBytes.push_back(static_cast<unsigned char>(c));
        }
    }

    MemoryOutputStream os;
    ASSERT_TRUE(ImagePersistence::exportPNG(os, &image));
    EXPECT_EQ(fileBytes, os.bytes);
    const unsigned char signature[8] = {137, 'P', 'N', 'G', '\r', '\n', 26, '\n'};
    ASSERT_GE(os.bytes.size(), 8u);
    EXPECT_EQ(0, std::memcmp(signature, os.bytes.data(), 8));

    RGBImageUncompressed *imported = ImagePersistence::importRGB(file);
    ASSERT_NE(nullptr, imported);
    ASSERT_EQ(5, imported->getXSize());
    ASSERT_EQ(3, imported->getYSize());
    RGBPixel expected;
    RGBPixel actual;
    for ( int y = 0; y < 3; y++ ) {
        for ( int x = 0; x < 5; x++ ) {
            image.getPixelRgb(x, y, &expected);
            imported->getPixelRgb(x, y, &actual);
            EXPECT_EQ(expected.r, actual.r) << "pixel " << x << ", " << y;
            EXPECT_EQ(expected.g, actual.g) << "pixel " << x << ", " << y;
            EXPECT_EQ(expected.b, actual.b) << "pixel " << x << ", " << y;
        }
    }
    delete imported;
    std::remove(path.c_str());
}
