#ifndef __DITHERING__
#define __DITHERING__

#include "vsdk/toolkit/common/VSDK.h"

#ifdef __TURBOC__
#define DITHERING_HUGE huge
#else
#define DITHERING_HUGE
#endif

// RGB buffers contain packed 8-bit channels; palette channels use the VGA range 0..63.
// optimizePalette uses 5-bit color bins and convert writes one palette index per pixel.
// Real-mode buffers use huge pointers so that image data can cross segment boundaries.
class Dithering {
public:
    enum { MAX_COLORS = 256 };
    class Palette {
    public:
        unsigned count;
        int rgb[MAX_COLORS * 3];
    };
    enum Method { Nearest, Ordered, FloydSteinberg };
    static void orderedRgb565(unsigned char DITHERING_HUGE *rgb, unsigned width, unsigned height);
    static BOOLEAN optimizePalette(const unsigned char DITHERING_HUGE *rgb, unsigned width, unsigned height,
                                   unsigned colors, Palette *out);
    static BOOLEAN convert(Method method, const unsigned char DITHERING_HUGE *rgb, unsigned width, unsigned height,
                           const Palette &palette, unsigned char DITHERING_HUGE *indices);
};

#endif
