#include "RasterizerDisplay.h"
#include "RasterizerExamples.h"
#include "RasterizerPalette.h"
#include <stdlib.h>
#include <vga.h>
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/RGBPixel.h"

void
RasterizerDisplay::drawTrueColor(const uint8_t *rgb, int width, int height) {
    long last = -1;
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            const uint8_t *p = rgb + ((unsigned)y * (unsigned)width + (unsigned)x) * 3;
            long key = (long)p[0] << 16 | (long)p[1] << 8 | p[2];
            if (key != last) {
                vga_setrgbcolor(p[0], p[1], p[2]);
                last = key;
            }
            vga_drawpixel(x, y);
        }
    }
}

int
RasterizerDisplay::showExample(int which, int width, int height, const Dithering::Palette *standard, ColorMode colorMode,
                               RasterizerShown *shown) {
    shown->palette = *standard;
    shown->dithered = 0;
    shown->custom = 0;
    RGBImageUncompressed img;
    if (!img.init(width, height)) {
        return 0;
    }
    RasterizerExamples::draw(which, &img, width, height);
    unsigned pixels = (unsigned)width * (unsigned)height;
    uint8_t *rgb = (uint8_t *)malloc(pixels * 3);
    uint8_t *indices = (uint8_t *)malloc(pixels);
    int ok = rgb && indices;
    if (ok) {
        RGBPixel p;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.getPixelRgb(x, y, &p);
                uint8_t *dst = rgb + ((unsigned)y * (unsigned)width + (unsigned)x) * 3;
                dst[0] = (uint8_t)p.r;
                dst[1] = (uint8_t)p.g;
                dst[2] = (uint8_t)p.b;
            }
        }
        if (standard->count > 256) {
            drawTrueColor(rgb, width, height);
            free(rgb);
            free(indices);
            return 1;
        }
        Dithering::Palette optimized;
        const Dithering::Palette *palette = standard;
        if (RasterizerPalette::usesOptimizedPalette(colorMode) && standard->count == 256) {
            if (Dithering::optimizePalette(rgb, (unsigned)width, (unsigned)height, 256, &optimized)) {
                palette = &optimized;
            }
        }
        ok = Dithering::convert(RasterizerPalette::methodOf(colorMode), rgb, (unsigned)width, (unsigned)height, *palette, indices);
        if (ok) {
            shown->palette = *palette;
            shown->dithered = 1;
            shown->custom = palette == &optimized;
            if (palette->count == 256) {
                vga_setpalvec(0, 256, (int *)palette->rgb);
            }
            int last = -1;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int color = indices[(unsigned)y * (unsigned)width + (unsigned)x];
                    if (color != last) {
                        vga_setcolor(color);
                        last = color;
                    }
                    vga_drawpixel(x, y);
                }
            }
        }
    }
    free(rgb);
    free(indices);
    return ok;
}

