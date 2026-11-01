#ifndef VITRAL_FRAMEBUFFER_THEME_H
#define VITRAL_FRAMEBUFFER_THEME_H

#include "vsdk/toolkit/common/VSDK.h"

class Framebuffer;

// Colors (0xRRGGBB) and metrics of the framebuffer GUI. Shaded themes draw Motif style three-dimensional shadows;
// flat themes draw one pixel outlines and show the armed menu entry in inverse video.
class FramebufferTheme {
public:
    unsigned long desktop;
    unsigned long face;
    unsigned long light;
    unsigned long shadow;
    unsigned long outline;
    unsigned long activeFace;
    unsigned long activeLight;
    unsigned long activeShadow;
    unsigned long text;
    unsigned long disabledText;
    unsigned long armedFace;
    unsigned long armedText;
    BOOLEAN shaded;
    int shadowThickness;

    // DECwindows on OSF/1: tan faces on a slate blue desktop.
    static FramebufferTheme decwindows(int shadowThickness);
    // White background and black lines and text.
    static FramebufferTheme monochrome();
    // The theme the surface can show: monochrome in 1 bit per pixel, DECwindows otherwise.
    static FramebufferTheme forSurface(const Framebuffer &surface);
    // The colors of the theme, so that palette backends can reserve their entries before drawing.
    int colors(unsigned long *out, int size) const;
};

#endif
