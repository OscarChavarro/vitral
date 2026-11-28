#ifndef __COLOR_CUBE__
#define __COLOR_CUBE__

#include "vsdk/toolkit/common/VSDK.h"

/**
A uniform RGB color cube: the palette of indexed color displays (i.e. the
8-bit PseudoColor visuals of X11) where any RGB color has to be shown with
a fixed set of colors. Each channel is divided in a number of levels
spread over [0, 255]; cube cells are numbered with blue varying fastest:
`index = (red * greenLevels + green) * blueLevels + blue`.

The default cube has 5 x 9 x 5 = 225 cells, the dither palette of Mesa
up to 3.0 on 8-bit visuals: a client that allocates the same colors shares
its read-only colormap cells with the OpenGL rendering.

The colors are mapped to the nearest cell (`indexOf`) or with the 8x8
ordered dithering of `Dithering` (`indexOfDithered`), which keeps the
average color of image areas.
*/
class ColorCube {
public:
    enum { MAX_LEVELS = 16 };

    /**
    Builds the default 5 x 9 x 5 cube.
    */
    ColorCube();

    /**
    @param redLevels number of red levels, in [2, MAX_LEVELS]
    @param greenLevels number of green levels, in [2, MAX_LEVELS]
    @param blueLevels number of blue levels, in [2, MAX_LEVELS]
    Out of range numbers of levels are clamped.
    */
    ColorCube(int redLevels, int greenLevels, int blueLevels);

    int getRedLevels() const;
    int getGreenLevels() const;
    int getBlueLevels() const;

    /**
    @return the number of cells of the cube
    */
    int getSize() const;

    /**
    @param r red, in [0, 255]
    @param g green, in [0, 255]
    @param b blue, in [0, 255]
    @return the index of the nearest cell
    */
    int indexOf(int r, int g, int b) const;

    /**
    @param r red, in [0, 255]
    @param g green, in [0, 255]
    @param b blue, in [0, 255]
    @param x column of the pixel, which selects the dithering threshold
    @param y row of the pixel, which selects the dithering threshold
    @return the index of a cell around the color, chosen with the 8x8
    ordered dithering matrix
    */
    int indexOfDithered(int r, int g, int b, unsigned x, unsigned y) const;

    /**
    @param index index of a cell, in [0, getSize())
    @return the red of the cell, in [0, 255]
    */
    int getRed(int index) const;
    int getGreen(int index) const;
    int getBlue(int index) const;

    /**
    @param index index of a cell, in [0, getSize())
    @return the red level of the cell, in [0, getRedLevels())
    */
    int getRedLevel(int index) const;
    int getGreenLevel(int index) const;
    int getBlueLevel(int index) const;

private:
    int redLevels;
    int greenLevels;
    int blueLevels;

    static int clampLevels(int levels);
    static int nearestLevel(int value, int levels);
    static int ditheredLevel(int value, int levels, int threshold);
    static int levelValue(int level, int levels);
};

#endif
