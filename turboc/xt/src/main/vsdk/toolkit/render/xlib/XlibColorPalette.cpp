// First: the X11R6 headers (i.e. Debian 1.1) do not include it themselves
#include <X11/Xlib.h>
#include <X11/Xutil.h>

#include "vsdk/toolkit/render/xlib/XlibColorPalette.h"

XlibColorPalette* XlibColorPalette::palettes[MAX_PALETTES];
int XlibColorPalette::numberOfPalettes = 0;

static int shiftOf(unsigned long mask)
{
    int shift = 0;
    while ( mask != 0 && (mask & 1) == 0 ) {
        mask >>= 1;
        shift++;
    }
    return shift;
}

/**
@return the bits of a channel of a TrueColor pixel for an 8 bit value
*/
static unsigned long toChannel(int value, unsigned long mask)
{
    unsigned long bits = mask >> shiftOf(mask);
    value = value < 0 ? 0 : value > 255 ? 255 : value;
    return ((((unsigned long)(value)) * bits / 255) << shiftOf(mask)) &
        mask;
}

/**
@return the 8 bit value of a channel of a TrueColor pixel
*/
static int fromChannel(unsigned long pixel, unsigned long mask)
{
    unsigned long bits = mask >> shiftOf(mask);
    if ( bits == 0 ) {
        return 0;
    }
    return (int)(((pixel & mask) >> shiftOf(mask)) * 255 / bits);
}

BOOLEAN XlibColorPalette::isIndexed(const Visual* visual)
{
    return visual != NULL && visual->red_mask == 0 &&
        visual->green_mask == 0 && visual->blue_mask == 0;
}

XlibColorPalette::XlibColorPalette(Display* d, const Visual* v, Colormap c)
    : display(d), visual(v), colormap(c), indexed(isIndexed(v)), cube(),
      numberOfCells(0), cellsRead(FALSE)
{
    int i;
    for ( i = 0; i < MAX_CELLS; i++ ) {
        cubePixels[i] = -1;
    }
    numberOfCells = v != NULL ? v->map_entries : 0;
    if ( numberOfCells > MAX_CELLS ) {
        numberOfCells = MAX_CELLS;
    }
}

Colormap XlibColorPalette::defaultColormapOf(Display* display,
                                             const Visual* visual)
{
    int screen;
    for ( screen = 0; screen < ScreenCount(display); screen++ ) {
        if ( DefaultVisual(display, screen) == visual ) {
            return DefaultColormap(display, screen);
        }
    }
    return DefaultColormap(display, DefaultScreen(display));
}

void XlibColorPalette::removePalette(int index)
{
    delete palettes[index];
    for ( ; index < numberOfPalettes - 1; index++ ) {
        palettes[index] = palettes[index + 1];
    }
    numberOfPalettes--;
}

void XlibColorPalette::setColormap(Display* display, const Visual* visual,
                                   Colormap colormap)
{
    int i;
    for ( i = 0; i < numberOfPalettes; i++ ) {
        if ( palettes[i]->display == display &&
             palettes[i]->visual == visual ) {
            if ( palettes[i]->colormap == colormap ) {
                return;
            }
            removePalette(i);
            break;
        }
    }
    if ( numberOfPalettes == MAX_PALETTES ) {
        removePalette(0);
    }
    palettes[numberOfPalettes++] =
        new XlibColorPalette(display, visual, colormap);
}

void XlibColorPalette::releaseColormap(Display* display, Colormap colormap)
{
    int i = 0;
    while ( i < numberOfPalettes ) {
        if ( palettes[i]->display == display &&
             palettes[i]->colormap == colormap ) {
            removePalette(i);
        }
        else {
            i++;
        }
    }
}

void XlibColorPalette::releaseDisplay(Display* display)
{
    int i = 0;
    while ( i < numberOfPalettes ) {
        if ( palettes[i]->display == display ) {
            removePalette(i);
        }
        else {
            i++;
        }
    }
}

XlibColorPalette* XlibColorPalette::forVisual(Display* display,
                                              const Visual* visual)
{
    int i;
    for ( i = 0; i < numberOfPalettes; i++ ) {
        if ( palettes[i]->display == display &&
             palettes[i]->visual == visual ) {
            return palettes[i];
        }
    }
    setColormap(display, visual, defaultColormapOf(display, visual));
    return palettes[numberOfPalettes - 1];
}

const Visual* XlibColorPalette::getVisual() const
{
    return visual;
}

Colormap XlibColorPalette::getColormap() const
{
    return colormap;
}

void XlibColorPalette::refreshColors()
{
    if ( !indexed || numberOfCells <= 0 ) {
        return;
    }
    int i;
    for ( i = 0; i < numberOfCells; i++ ) {
        cells[i].pixel = (unsigned long)i;
    }
    XQueryColors(display, colormap, cells, numberOfCells);
    cellsRead = TRUE;
}

unsigned long XlibColorPalette::nearestCell(int r, int g, int b)
{
    if ( !cellsRead ) {
        refreshColors();
    }
    int i;
    int best = 0;
    long bestDistance = -1;
    for ( i = 0; i < numberOfCells; i++ ) {
        long dr = (long)(cells[i].red >> 8) - r;
        long dg = (long)(cells[i].green >> 8) - g;
        long db = (long)(cells[i].blue >> 8) - b;
        // Weighted as the sensitivity of the eye to each channel
        long distance = 3 * dr * dr + 4 * dg * dg + 2 * db * db;
        if ( bestDistance < 0 || distance < bestDistance ) {
            bestDistance = distance;
            best = i;
        }
    }
    return cells[best].pixel;
}

/**
@return the pixel of a cell of the cube, allocated on its first use with
the exact 16 bit levels of the Mesa dither palette (shared cells), or the
nearest color of the colormap when it is full
*/
unsigned long XlibColorPalette::cubePixel(int index)
{
    if ( cubePixels[index] >= 0 ) {
        return (unsigned long)cubePixels[index];
    }
    XColor color;
    color.red = (unsigned short)((long)cube.getRedLevel(index) * 65535L /
                                 (cube.getRedLevels() - 1));
    color.green = (unsigned short)((long)cube.getGreenLevel(index) *
                                   65535L / (cube.getGreenLevels() - 1));
    color.blue = (unsigned short)((long)cube.getBlueLevel(index) * 65535L /
                                  (cube.getBlueLevels() - 1));
    color.flags = DoRed | DoGreen | DoBlue;
    if ( XAllocColor(display, colormap, &color) ) {
        cubePixels[index] = (long)color.pixel;
        // The cell may have been free when the colors were read
        if ( cellsRead && color.pixel < (unsigned long)numberOfCells ) {
            cells[color.pixel] = color;
        }
    }
    else {
        cubePixels[index] = (long)nearestCell(cube.getRed(index),
                                              cube.getGreen(index),
                                              cube.getBlue(index));
    }
    return (unsigned long)cubePixels[index];
}

unsigned long XlibColorPalette::rgbToPixel(int r, int g, int b)
{
    if ( visual == NULL ) {
        return 0;
    }
    if ( !indexed ) {
        return toChannel(r, visual->red_mask) |
            toChannel(g, visual->green_mask) |
            toChannel(b, visual->blue_mask);
    }
    return cubePixel(cube.indexOf(r, g, b));
}

unsigned long XlibColorPalette::rgbToPixel(int r, int g, int b, int x, int y)
{
    if ( !indexed ) {
        return rgbToPixel(r, g, b);
    }
    return cubePixel(cube.indexOfDithered(r, g, b, (unsigned)x,
                                          (unsigned)y));
}

void XlibColorPalette::pixelToRgb(unsigned long pixel, int rgb[3])
{
    if ( visual == NULL ) {
        rgb[0] = rgb[1] = rgb[2] = 0;
        return;
    }
    if ( !indexed ) {
        rgb[0] = fromChannel(pixel, visual->red_mask);
        rgb[1] = fromChannel(pixel, visual->green_mask);
        rgb[2] = fromChannel(pixel, visual->blue_mask);
        return;
    }
    if ( !cellsRead ) {
        refreshColors();
    }
    if ( pixel >= (unsigned long)numberOfCells ) {
        rgb[0] = rgb[1] = rgb[2] = 0;
        return;
    }
    rgb[0] = cells[pixel].red >> 8;
    rgb[1] = cells[pixel].green >> 8;
    rgb[2] = cells[pixel].blue >> 8;
}
