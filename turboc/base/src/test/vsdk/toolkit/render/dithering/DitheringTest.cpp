#include <stdio.h>
#include "vsdk/toolkit/render/dithering/Dithering.h"

static unsigned char pixels[47 * 19 * 3];
static unsigned char indices[47 * 19];

static unsigned long
hashBytes(const unsigned char *data, unsigned count)
{
    unsigned long hash = 2166136261UL;
    unsigned i;
    for (i = 0; i < count; i++) hash = ((hash ^ data[i]) * 16777619UL) & 0xffffffffUL;
    return hash;
}

int main()
{
    const unsigned counts[3] = {2, 16, 256};
    const unsigned long expected[3][4] = {
        {0x82e89bfeUL, 0x20ce637dUL, 0xe6e355b9UL, 0x6476640cUL},
        {0x91e3e123UL, 0x48fded21UL, 0x99eab687UL, 0xd011121cUL},
        {0x4a6012d3UL, 0x919e026dUL, 0x738c2aecUL, 0x4bc57a28UL}
    };
    const Dithering::Method methods[3] = {Dithering::Nearest, Dithering::Ordered, Dithering::FloydSteinberg};
    unsigned long i;
    unsigned t, m;
    for (i = 0; i < sizeof(pixels); i++) pixels[i] = (unsigned char)((i * 73UL + i / 7UL * 19UL + 11UL) % 256UL);
    for (t = 0; t < 3; t++) {
        Dithering::Palette palette;
        if (!Dithering::optimizePalette(pixels, 47, 19, counts[t], &palette)) return 1;
        unsigned long hash = 2166136261UL;
        for (i = 0; i < counts[t] * 3U; i++) hash = ((hash ^ (unsigned)palette.rgb[i]) * 16777619UL) & 0xffffffffUL;
        if (hash != expected[t][0]) return 2;
        for (m = 0; m < 3; m++) {
            if (!Dithering::convert(methods[m], pixels, 47, 19, palette, indices)) return 3;
            if (hashBytes(indices, sizeof(indices)) != expected[t][m + 1]) return 4;
        }
    }
    Dithering::orderedRgb565(pixels, 47, 19);
    if (hashBytes(pixels, sizeof(pixels)) != 0x4292ca03UL) return 5;
    puts("Dithering matches the C++ reference.");
    return 0;
}
