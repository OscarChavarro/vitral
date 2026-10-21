#ifndef __PALETTE_SET__
#define __PALETTE_SET__

#ifndef VGA_PALETTE_DIR
#error "VGA_PALETTE_DIR (directory of the palette files) is defined by CMake"
#endif

#include <stddef.h>

#include "programs/vga/VgaConstant.h"

class PaletteSet {
  public:
    PaletteSet();

    void scan();
    unsigned total() const { return PALETTES + (unsigned)count; }
    void step(int delta);
    void currentName(char *out, size_t size) const;
    void apply() const;
    void printLine() const;

  private:
    static constexpr const char *PALETTE_DIR = VGA_PALETTE_DIR;

    int count;
    char file[MAX_FILES][FILE_NAME];
    unsigned current;

    static int mix(int from, int to, int span, int t);
    static void makePalette(unsigned which, int *pal);
    static int sameText(const char *a, const char *b);
    static int compareText(const char *a, const char *b);
    static int endsWithGpl(const char *name);
    static int loadFile(const char *name, int *pal);
};

#endif
