#include <ctype.h>
#include <stdio.h>
#include <string.h>

#include <vga.h>

#include "java/io/File.h"
#include "java/io/FileReader.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/io/image/RGBColorPalettePersistence.h"
#include "vsdk/toolkit/media/RGBColorPalette.h"
#include "programs/vga/PaletteSet.h"
#include "programs/vga/PaletteStop.h"

PaletteSet::PaletteSet() : count(0), current(0) {}

int
PaletteSet::mix(int from, int to, int span, int t) {
    return (from * (span - t) + to * t) / span;
}

void
PaletteSet::makePalette(unsigned which, int *pal) {
    static const PaletteStop STOPS[] = {
        {0, 0, 0, 22}, {50, 0, 20, 50}, {90, 0, 40, 45}, {100, 10, 46, 12},
        {150, 38, 54, 10}, {190, 50, 40, 12}, {225, 42, 28, 18}, {255, 63, 63, 63}};
    for (unsigned i = 0; i < 256; i++) {
        int r = 0;
        int g = 0;
        int b = 0;
        int v = (int)(i * 63 / 255);
        switch (which) {
        case 0: r = g = b = v; break;
        case 1: r = v; break;
        case 2: g = v; break;
        case 3: b = v; break;
        case 4: {
            int s = 1;
            while (STOPS[s].index < (int)i) {
                s++;
            }
            int span = STOPS[s].index - STOPS[s - 1].index;
            int t = (int)i - STOPS[s - 1].index;
            r = mix(STOPS[s - 1].red, STOPS[s].red, span, t);
            g = mix(STOPS[s - 1].green, STOPS[s].green, span, t);
            b = mix(STOPS[s - 1].blue, STOPS[s].blue, span, t);
            break;
        }
        default: {
            int pos = (int)(i * 3 / 2);
            int f = pos % 64;
            switch (pos / 64) {
            case 0: r = 63; g = f; break;
            case 1: r = 63 - f; g = 63; break;
            case 2: g = 63; b = f; break;
            case 3: g = 63 - f; b = 63; break;
            case 4: r = f; b = 63; break;
            default: r = 63; b = 63 - f; break;
            }
        }
        }
        pal[i * 3] = r;
        pal[i * 3 + 1] = g;
        pal[i * 3 + 2] = b;
    }
}

int
PaletteSet::sameText(const char *a, const char *b) {
    for (; *a && *b; a++, b++) {
        if (tolower((unsigned char)*a) != tolower((unsigned char)*b)) {
            return 0;
        }
    }
    return *a == *b;
}

int
PaletteSet::compareText(const char *a, const char *b) {
    for (; *a && *b; a++, b++) {
        int d = tolower((unsigned char)*a) - tolower((unsigned char)*b);
        if (d) {
            return d;
        }
    }
    return tolower((unsigned char)*a) - tolower((unsigned char)*b);
}

int
PaletteSet::endsWithGpl(const char *name) {
    size_t n = strlen(name);
    return n > 4 && sameText(name + n - 4, ".gpl");
}

void
PaletteSet::scan() {
    count = 0;
    current = 0;
    java::File directory(PALETTE_DIR);
    java::ArrayList<java::String> names = directory.list();
    for (long i = 0; i < names.size() && count < MAX_FILES; i++) {
        const char *name = names[i].toCString();
        if (!endsWithGpl(name) || strlen(name) >= FILE_NAME) {
            continue;
        }
        int at = count++;
        while (at > 0 && compareText(file[at - 1], name) > 0) {
            strcpy(file[at], file[at - 1]);
            at--;
        }
        strcpy(file[at], name);
    }
}

void
PaletteSet::step(int delta) {
    unsigned all = total();
    current = (current + (delta > 0 ? 1 : all - 1)) % all;
}

void
PaletteSet::currentName(char *out, size_t size) const {
    static const char *const BUILT_IN[PALETTES] = {"grays", "reds", "greens", "blues", "topographic", "rainbow"};
    if (current < PALETTES) {
        snprintf(out, size, "%s", BUILT_IN[current]);
        return;
    }
    snprintf(out, size, "%s", file[current - PALETTES]);
    out[strlen(out) - 4] = 0;
}

int
PaletteSet::loadFile(const char *name, int *pal) {
    char path[192];
    snprintf(path, sizeof(path), "%s/%s", PALETTE_DIR, name);
    java::FileReader reader(path);
    RGBColorPalette *palette = RGBColorPalettePersistence::importGimpPalette(reader);
    reader.close();
    if (!palette) {
        return 0;
    }
    int size = palette->size();
    for (int i = 0; i < 256 * 3; i++) {
        pal[i] = 0;
    }
    for (int i = 0; i < 256 && i < size; i++) {
        const ColorRgb *c = palette->getColorAt(i);
        pal[i * 3] = (int)(c->r() * 63 + 0.5);
        pal[i * 3 + 1] = (int)(c->g() * 63 + 0.5);
        pal[i * 3 + 2] = (int)(c->b() * 63 + 0.5);
    }
    delete palette;
    return size > 0;
}

void
PaletteSet::apply() const {
    int pal[256 * 3];
    if (current < PALETTES || !loadFile(file[current - PALETTES], pal)) {
        makePalette(current < PALETTES ? current : 0, pal);
    }
    vga_setpalvec(0, 256, pal);
}

void
PaletteSet::printLine() const {
    char name[FILE_NAME];
    currentName(name, sizeof(name));
    printf("256-colour palette: %s (%u of %u: %d computed, %d from %s). Left / Right change it in a picture.",
           name, current + 1, total(), PALETTES, count, PALETTE_DIR);
}
