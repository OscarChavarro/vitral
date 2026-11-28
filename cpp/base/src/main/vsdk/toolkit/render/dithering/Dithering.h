#ifndef __DITHERING__
#define __DITHERING__
#include <stdint.h>

class Dithering {
public:
    static constexpr unsigned MAX_COLORS = 256;

    class Palette {
    public:
        unsigned count;
        int rgb[MAX_COLORS * 3];
    };

    enum class Method { Nearest, Ordered, FloydSteinberg };

    /**
    @return the threshold of the 8x8 ordered (Bayer) dithering matrix at a
    pixel, in [0, 63]; the one of orderedRgb565 and of the Ordered method
    */
    static int orderedThreshold(unsigned x, unsigned y);

    static void orderedRgb565(uint8_t *rgb, unsigned width, unsigned height);

    static bool optimizePalette(const uint8_t *rgb, unsigned width, unsigned height, unsigned colors, Palette *out);

    static bool convert(Method method, const uint8_t *rgb, unsigned width, unsigned height, const Palette &palette,
                        uint8_t *indices);

private:
    class Finder;
    static constexpr unsigned BAYER_SIZE = 8;

    static void convertNearest(Finder &finder, const uint8_t *rgb, unsigned width, unsigned height, uint8_t *indices);
    static void convertOrdered(Finder &finder, const uint8_t *rgb, unsigned width, unsigned height,
                               unsigned spread, uint8_t *indices);
    static bool convertFloydSteinberg(Finder &finder, const uint8_t *rgb, unsigned width, unsigned height,
                                      uint8_t *indices);
    static int bayer(unsigned x, unsigned y);
    static unsigned spreadOf(unsigned colors);
    static int clamp(int v) { return v < 0 ? 0 : v > 255 ? 255 : v; }
};

#endif
