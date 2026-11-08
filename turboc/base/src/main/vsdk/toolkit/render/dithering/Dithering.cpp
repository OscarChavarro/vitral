#include <stdlib.h>
#include <string.h>
#include "vsdk/toolkit/render/dithering/Dithering.h"

class DitheringFinder {
public:
    unsigned count;
    int pal[256 * 3];
    unsigned short *cache[32];
    DitheringFinder(const Dithering::Palette &palette) : count(palette.count) {
        unsigned i;
        for (i = 0; i < count * 3; i++) {
            int v = palette.rgb[i] & 63;
            pal[i] = (v << 2) | (v >> 4);
        }
        for (i = 0; i < 32; i++) cache[i] = 0;
    }
    ~DitheringFinder() {
        unsigned i;
        for (i = 0; i < 32; i++) free(cache[i]);
    }
    unsigned nearest(int r, int g, int b) {
        unsigned key = ((unsigned)(r >> 3) << 10) | ((unsigned)(g >> 3) << 5) | (unsigned)(b >> 3);
        unsigned block = key >> 10, offset = key & 1023, best = 0, i;
        if (!cache[block]) {
            cache[block] = (unsigned short *)malloc(1024U * sizeof(unsigned short));
            if (cache[block]) memset(cache[block], 255, 1024U * sizeof(unsigned short));
        }
        if (cache[block] && cache[block][offset] != 0xffffU) return cache[block][offset];
        long distance = 2147483647L;
        for (i = 0; i < count; i++) {
            long dr = r - pal[i * 3], dg = g - pal[i * 3 + 1], db = b - pal[i * 3 + 2];
            long d = dr * dr + dg * dg + db * db;
            if (d < distance) { distance = d; best = i; }
        }
        if (cache[block]) cache[block][offset] = (unsigned short)best;
        return best;
    }
private:
    DitheringFinder(const DitheringFinder &);
    DitheringFinder &operator=(const DitheringFinder &);
};

static int ditheringBayer(unsigned x, unsigned y) {
    unsigned mixed = x ^ y, v = 0, bit;
    for (bit = 0; bit < 3; bit++) v = (v << 2) | (((y >> bit) & 1) << 1) | ((mixed >> bit) & 1);
    return (int)v;
}
static int ditheringClamp(int v) { return v < 0 ? 0 : v > 255 ? 255 : v; }

void Dithering::orderedRgb565(unsigned char DITHERING_HUGE *rgb, unsigned width, unsigned height) {
    unsigned x, y, c;
    if (!rgb) return;
    for (y = 0; y < height; y++) for (x = 0; x < width; x++) {
        unsigned long p = ((unsigned long)y * width + x) * 3UL;
        unsigned threshold = (unsigned)ditheringBayer(x % 8, y % 8);
        for (c = 0; c < 3; c++) {
            unsigned levels = c == 1 ? 63 : 31, scaled = (unsigned)rgb[p + c] * levels;
            unsigned value = scaled / 255;
            if ((unsigned long)(scaled % 255) * 64UL > (unsigned long)threshold * 255UL && value < levels) value++;
            rgb[p + c] = (unsigned char)((value * 255U + levels - 1) / levels);
        }
    }
}

BOOLEAN Dithering::convert(Method method, const unsigned char DITHERING_HUGE *rgb, unsigned width, unsigned height,
                            const Palette &palette, unsigned char DITHERING_HUGE *indices) {
    unsigned x, y, c, levels, spread;
    if (!rgb || !indices || !palette.count || palette.count > MAX_COLORS ||
        (method != Nearest && method != Ordered && method != FloydSteinberg)) return FALSE;
    DitheringFinder finder(palette);
    levels = 2;
    while ((unsigned long)levels * levels * levels < palette.count) levels++;
    spread = 255 / (levels - 1);
    if (method != FloydSteinberg) {
        for (y = 0; y < height; y++) for (x = 0; x < width; x++) {
            unsigned long i = (unsigned long)y * width + x, p = i * 3UL;
            int offset = method == Ordered ? (ditheringBayer(x % 8, y % 8) * 2 + 1 - 64) * (int)spread / 128 : 0;
            indices[i] = (unsigned char)finder.nearest(ditheringClamp(rgb[p] + offset), ditheringClamp(rgb[p + 1] + offset),
                                                       ditheringClamp(rgb[p + 2] + offset));
        }
        return TRUE;
    }
    /* Two separate rows keep each allocation within a real-mode segment. */
    if ((unsigned long)(width + 2UL) * 3UL * sizeof(int) > 65520UL) return FALSE;
    unsigned cells = (width + 2U) * 3U;
    int *current = (int *)calloc(cells, sizeof(int));
    int *next = (int *)calloc(cells, sizeof(int));
    if (!current || !next) { free(current); free(next); return FALSE; }
    int *first = current, *second = next;
    for (y = 0; y < height; y++) {
        int step = (y & 1) ? -1 : 1;
        memset(next, 0, cells * sizeof(int));
        for (unsigned i = 0; i < width; i++) {
            x = step == 1 ? i : width - 1 - i;
            unsigned long pos = (unsigned long)y * width + x, p = pos * 3UL;
            unsigned here = (x + 1) * 3;
            int wanted[3];
            for (c = 0; c < 3; c++) wanted[c] = ditheringClamp(rgb[p + c] + current[here + c] / 16);
            unsigned index = finder.nearest(wanted[0], wanted[1], wanted[2]);
            indices[pos] = (unsigned char)index;
            for (c = 0; c < 3; c++) {
                int error = wanted[c] - finder.pal[index * 3 + c];
                current[(int)here + step * 3 + c] += error * 7;
                next[(int)here - step * 3 + c] += error * 3;
                next[here + c] += error * 5;
                next[(int)here + step * 3 + c] += error;
            }
        }
        int *swap = current; current = next; next = swap;
    }
    free(first); free(second);
    return TRUE;
}

class DitheringEntry { public: unsigned short key; unsigned long count; };
class DitheringBox {
public:
    unsigned first, end, low[3], high[3], longest;
    double pixels, score;
};
static unsigned ditheringChannel(const DitheringEntry &e, unsigned c) { return (e.key >> (10 - c * 5)) & 31; }
static DitheringEntry &ditheringEntry(DitheringEntry **blocks, unsigned i) { return blocks[i >> 10][i & 1023]; }
static void ditheringMeasure(DitheringEntry **entries, DitheringBox &box) {
    unsigned i, c;
    box.pixels = 0;
    for (c = 0; c < 3; c++) { box.low[c] = 31; box.high[c] = 0; }
    for (i = box.first; i < box.end; i++) {
        DitheringEntry &e = ditheringEntry(entries, i); box.pixels += e.count;
        for (c = 0; c < 3; c++) {
            unsigned v = ditheringChannel(e, c);
            if (v < box.low[c]) box.low[c] = v;
            if (v > box.high[c]) box.high[c] = v;
        }
    }
    box.longest = 0;
    for (c = 1; c < 3; c++) if (box.high[c] - box.low[c] > box.high[box.longest] - box.low[box.longest]) box.longest = c;
    box.score = box.pixels * (box.high[box.longest] - box.low[box.longest]);
    if (box.score > 4294967295.0) box.score = 4294967295.0;
}

BOOLEAN Dithering::optimizePalette(const unsigned char DITHERING_HUGE *rgb, unsigned width, unsigned height,
                                    unsigned colors, Palette *out) {
    if (!rgb || !out || !colors || colors > MAX_COLORS) return FALSE;
    unsigned long *hist[32];
    DitheringEntry *entries[32];
    unsigned b, c, distinct = 0, boxCount = 0;
    for (b = 0; b < 32; b++) { hist[b] = 0; entries[b] = 0; }
    BOOLEAN ok = TRUE;
    unsigned long pixels = (unsigned long)width * height, p;
    for (p = 0; p < pixels; p++) {
        unsigned long pos = p * 3UL;
        unsigned key = ((unsigned)(rgb[pos] >> 3) << 10) | ((unsigned)(rgb[pos + 1] >> 3) << 5) | (rgb[pos + 2] >> 3);
        unsigned block = key >> 10;
        if (!hist[block]) hist[block] = (unsigned long *)calloc(1024U, sizeof(unsigned long));
        if (!hist[block]) { ok = FALSE; break; }
        hist[block][key & 1023]++;
    }
    for (b = 0; b < 32 && ok; b++) if (hist[b]) {
        for (unsigned k = 0; k < 1024; k++) if (hist[b][k]) {
            unsigned block = distinct >> 10;
            if (!entries[block]) entries[block] = (DitheringEntry *)malloc(1024U * sizeof(DitheringEntry));
            if (!entries[block]) { ok = FALSE; break; }
            DitheringEntry &e = ditheringEntry(entries, distinct++);
            e.key = (unsigned short)(b * 1024U + k); e.count = hist[b][k];
        }
    }
    for (b = 0; b < 32; b++) free(hist[b]);
    DitheringBox *boxes = ok ? (DitheringBox *)malloc(MAX_COLORS * sizeof(DitheringBox)) : 0;
    if (!boxes) ok = FALSE;
    if (ok && distinct) { boxes[0].first = 0; boxes[0].end = distinct; ditheringMeasure(entries, boxes[0]); boxCount = 1; }
    while (ok && boxCount && boxCount < colors) {
        unsigned best = 0;
        for (b = 1; b < boxCount; b++) if (boxes[b].score > boxes[best].score) best = b;
        DitheringBox &box = boxes[best];
        if (box.score == 0) break;
        c = box.longest;
        double perValue[32]; memset(perValue, 0, sizeof(perValue));
        unsigned i;
        for (i = box.first; i < box.end; i++) perValue[ditheringChannel(ditheringEntry(entries, i), c)] += ditheringEntry(entries, i).count;
        unsigned split = box.low[c]; double seen = 0;
        while (split < box.high[c] - 1 && (seen += perValue[split]) * 2 < box.pixels) split++;
        i = box.first; unsigned j = box.end;
        while (i < j) {
            if (ditheringChannel(ditheringEntry(entries, i), c) <= split) i++;
            else { j--; DitheringEntry tmp = ditheringEntry(entries, i); ditheringEntry(entries, i) = ditheringEntry(entries, j); ditheringEntry(entries, j) = tmp; }
        }
        DitheringBox &upper = boxes[boxCount++]; upper.first = i; upper.end = box.end; box.end = i;
        ditheringMeasure(entries, box); ditheringMeasure(entries, upper);
    }
    if (ok) {
        out->count = colors;
        for (b = 0; b < colors; b++) {
            double sum[3] = {0, 0, 0};
            if (b < boxCount) {
                DitheringBox &box = boxes[b];
                for (unsigned i = box.first; i < box.end; i++) for (c = 0; c < 3; c++) {
                    DitheringEntry &e = ditheringEntry(entries, i);
                    unsigned v = ditheringChannel(e, c); sum[c] += (double)((v << 3) | (v >> 2)) * e.count;
                }
                for (c = 0; c < 3; c++) out->rgb[b * 3 + c] = (int)(sum[c] / box.pixels) >> 2;
            } else for (c = 0; c < 3; c++) out->rgb[b * 3 + c] = boxCount ? out->rgb[c] : 0;
        }
    }
    free(boxes);
    for (b = 0; b < 32; b++) free(entries[b]);
    return ok;
}
