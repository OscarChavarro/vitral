#ifndef VITRAL_BGI_MODES_H
#define VITRAL_BGI_MODES_H

#include "vsdk/toolkit/common/VSDK.h"

// The graphics modes of the Borland Graphics Interface that a program can step through, as VgaModes does with the
// svgalib modes in the cpp port. A mode is the number of a row of a fixed table of driver files and driver modes:
// CGA.BGI (CGA and MCGA, 2 colours) and EGAVGA.BGI from Turbo C++, and the SuperVGA drivers of Jordan Hargraphix
// (SVGA16.BGI, SVGA256.BGI, SVGA64K.BGI and SVGATC.BGI, https://github.com/jharg93/SvgaBGI), which reach the VESA
// modes. The drivers are loaded from a folder (C:\TURBOC\BGI by default, or the BGIPATH environment variable).
// probe() keeps the modes that the machine can set, with the size that the table says.
class BgiModes {
public:
    enum { MAX_MODES = 40, MAX_TEXT = 40, ROW_WIDTH = 72 };
    enum Change { ChangeNone, ChangeDone, ChangeFailed };

    class Screen {
    public:
        int mode;
        int width;
        int height;
        long colors;
        int bitsPerPixel;
    };

    BgiModes(const char *driverPath);
    // Leaves the text mode back if the program did not call leave() (any return path).
    ~BgiModes();

    // Sets every mode of the table once and keeps those that work, in the order of VgaModes: by depth, then size.
    void probe();
    int count() const { return total; }
    int at(int index) const { return modes[index]; }
    int indexOf(int mode) const;
    // Drops the modes whose bitsPerPixel() is not one of the given depths, keeping the order of the others.
    void keepDepths(const int *depths, int count);

    static void header(char *out, int size);
    static void row(int mode, char *out, int size);
    void print(const char *title) const;

    static int tableSize();
    static const char *name(int mode);
    static int bitsPerPixel(int mode);
    static int width(int mode);
    static int height(int mode);
    // Driver file and driver mode, as "SVGA256 2".
    static void origin(int mode, char *out, int size);
    // A mode number, or a name such as G640x480x256 or 640x480x256, ignoring case.
    static int lookup(const char *text);

    BOOLEAN enter(int mode);
    const Screen &screen() const { return shown; }
    int current() const { return shown.mode; }
    BOOLEAN inGraphics() const { return graphics; }
    Change step(int step);
    // Back to the text mode.
    void leave();

private:
    char path[80];
    int modes[MAX_MODES];
    int total;
    Screen shown;
    BOOLEAN graphics;
    int loadedDriver;

    BgiModes(const BgiModes &other);
    BgiModes &operator=(const BgiModes &other);

    int driverNumber(int mode);
    BOOLEAN set(int mode);
    static long area(int mode);
};

#endif
