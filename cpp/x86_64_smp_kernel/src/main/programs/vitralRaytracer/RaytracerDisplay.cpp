#include "programs/vitralRaytracer/RaytracerDisplay.h"
#include "programs/vitralRaytracer/RaytracerConstant.h"
#include "programs/vitralRaytracer/RaytracerPalette.h"
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <vga.h>

static double
elapsedSeconds(const timespec &started, const timespec &finished) {
    return (double)(finished.tv_sec - started.tv_sec) + (double)(finished.tv_nsec - started.tv_nsec) / 1000000000.0;
}

void
RaytracerDisplay::drawTrueColor(const uint8_t *rgb, int width, int height) {
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

void
RaytracerDisplay::drawText(const uint8_t *rgb, int width, int height) {
    static const char RAMP[] = " .:-=+*#%@";
    const int steps = (int)sizeof(RAMP) - 1;
    for (int row = 0; row < TEXT_ROWS; row++) {
        printf("\033[%d;1H", row + 1);
        int bold = -1;
        for (int column = 0; column < TEXT_COLUMNS; column++) {
            if (row == TEXT_ROWS - 1 && column == TEXT_COLUMNS - 1) {
                break;
            }
            int x0 = column * width / TEXT_COLUMNS;
            int x1 = (column + 1) * width / TEXT_COLUMNS;
            int y0 = row * height / TEXT_ROWS;
            int y1 = (row + 1) * height / TEXT_ROWS;
            long sum = 0;
            long count = 0;
            for (int y = y0; y < y1; y++) {
                for (int x = x0; x < x1; x++) {
                    const uint8_t *p = rgb + ((unsigned)y * (unsigned)width + (unsigned)x) * 3;
                    sum += 299 * p[0] + 587 * p[1] + 114 * p[2];
                    count++;
                }
            }
            int level = count ? (int)(sum / count * (2 * steps) / 255001) : 0;
            int bright = level >= steps;
            if (bright != bold) {
                fputs(bright ? "\033[1m" : "\033[0m", stdout);
                bold = bright;
            }
            putchar(RAMP[bright ? level - steps : level]);
        }
    }
    fputs("\033[0m", stdout);
    fflush(stdout);
}

int
RaytracerDisplay::showImage(const uint8_t *rgb, int width, int height, int text, const Dithering::Palette *standard,
                            RaytracerColorMode colorMode, Shown *shown, double *vgaSeconds) {
    *vgaSeconds = 0;
    shown->palette = *standard;
    shown->dithered = 0;
    shown->custom = 0;
    if (text) {
        timespec started;
        clock_gettime(CLOCK_MONOTONIC, &started);
        drawText(rgb, width, height);
        timespec finished;
        clock_gettime(CLOCK_MONOTONIC, &finished);
        *vgaSeconds = elapsedSeconds(started, finished);
        return 1;
    }
    if (standard->count > 256) {
        timespec started;
        clock_gettime(CLOCK_MONOTONIC, &started);
        drawTrueColor(rgb, width, height);
        timespec finished;
        clock_gettime(CLOCK_MONOTONIC, &finished);
        *vgaSeconds = elapsedSeconds(started, finished);
        return 1;
    }
    unsigned pixels = (unsigned)width * (unsigned)height;
    uint8_t *indices = (uint8_t *)malloc(pixels);
    int ok = indices != 0;
    if (ok) {
        Dithering::Palette optimized;
        const Dithering::Palette *palette = standard;
        if (RaytracerPalette::usesOptimizedPalette(colorMode) && standard->count == 256) {
            if (Dithering::optimizePalette(rgb, (unsigned)width, (unsigned)height, 256, &optimized)) {
                palette = &optimized;
            }
        }
        ok = Dithering::convert(RaytracerPalette::methodOf(colorMode), rgb, (unsigned)width, (unsigned)height,
                                *palette, indices);
        if (ok) {
            shown->palette = *palette;
            shown->dithered = 1;
            shown->custom = palette == &optimized;
            timespec started;
            clock_gettime(CLOCK_MONOTONIC, &started);
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
            timespec finished;
            clock_gettime(CLOCK_MONOTONIC, &finished);
            *vgaSeconds = elapsedSeconds(started, finished);
        }
    }
    free(indices);
    return ok;
}
