#ifndef __XLIB_COLOR_PALETTE__
#define __XLIB_COLOR_PALETTE__

#include <X11/Xlib.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/render/dithering/ColorCube.h"

/**
Converts RGB colors to the pixel values of an X11 visual, and back, for
the 2D Xlib drawing of the toolkit (images, icons, gizmos, CPU renderers).

TrueColor (and DirectColor) visuals pack the channels with the masks of
the visual. Indexed visuals (PseudoColor, StaticColor, GrayScale,
StaticGray: no masks, i.e. the 8-bit displays of Tru64 CDE) map the color
to a `ColorCube` (5 x 9 x 5, the colors of the dither palette of Mesa, so
the cells are shared with the OpenGL rendering) whose cells are allocated
read-only on demand with `XAllocColor`; when the colormap is full, the
nearest color the colormap already has is used.

There is one palette for each display and visual, found with `forVisual`.
Its colormap is the one given with `setColormap` (the colormap of the
shell of the application), or the default colormap of the screen.
Release the palettes of a colormap (`releaseColormap`) before freeing it,
and all the palettes of a display (`releaseDisplay`) before closing it.
*/
class XlibColorPalette {
public:
    /**
    @return TRUE if the visual has no channel masks (a colormap index per
    pixel)
    */
    static BOOLEAN isIndexed(const Visual* visual);

    /**
    Sets the colormap whose colors are allocated for a visual (the colormap
    of the windows that use it). Without it, the default colormap is used.
    */
    static void setColormap(Display* display, const Visual* visual,
                            Colormap colormap);

    /**
    Forgets the palettes of a colormap (before `XFreeColormap`).
    */
    static void releaseColormap(Display* display, Colormap colormap);

    /**
    Forgets the palettes of a display (before closing it).
    */
    static void releaseDisplay(Display* display);

    /**
    @return the palette of a visual of a display, created on its first use
    */
    static XlibColorPalette* forVisual(Display* display, const Visual* visual);

    /**
    @param r red, in [0, 255]
    @param g green, in [0, 255]
    @param b blue, in [0, 255]
    @return the pixel value of the color (the nearest one available on
    indexed visuals)
    */
    unsigned long rgbToPixel(int r, int g, int b);

    /**
    As `rgbToPixel`, with ordered dithering on indexed visuals: for images,
    whose areas keep their average color.
    @param x column of the pixel
    @param y row of the pixel
    */
    unsigned long rgbToPixel(int r, int g, int b, int x, int y);

    /**
    Reads the colors of the colormap again (one request): call it before
    converting pixels read from the screen with `pixelToRgb`, as other
    clients may have allocated colors since the last time.
    */
    void refreshColors();

    /**
    @param pixel pixel value of the visual
    @param rgb out: red, green and blue, in [0, 255]
    */
    void pixelToRgb(unsigned long pixel, int rgb[3]);

    const Visual* getVisual() const;
    Colormap getColormap() const;

private:
    enum { MAX_PALETTES = 8, MAX_CELLS = 256 };

    Display* display;
    const Visual* visual;
    Colormap colormap;
    BOOLEAN indexed;
    ColorCube cube;
    /// Pixel of each cube cell; -1 while not allocated
    long cubePixels[MAX_CELLS];
    /// Colors of the colormap cells (indexed visuals), read by refreshColors
    XColor cells[MAX_CELLS];
    int numberOfCells;
    BOOLEAN cellsRead;

    static XlibColorPalette* palettes[MAX_PALETTES];
    static int numberOfPalettes;

    XlibColorPalette(Display* display, const Visual* visual,
                     Colormap colormap);
    unsigned long cubePixel(int index);
    unsigned long nearestCell(int r, int g, int b);
    static Colormap defaultColormapOf(Display* display, const Visual* visual);
    static void removePalette(int index);

    XlibColorPalette(const XlibColorPalette&);
    XlibColorPalette& operator=(const XlibColorPalette&);
};

#endif
