#ifndef VITRAL_XLIB_FRAMEBUFFER_H
#define VITRAL_XLIB_FRAMEBUFFER_H

#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <map>
#include <stdint.h>
#include "vsdk/toolkit/gui/framebuffer/Framebuffer.h"

// Framebuffer on one top-level X11 window, drawn with Xlib only (no toolkit): XDrawPoint(), XDrawLine(),
// XFillRectangle(), XDrawRectangle(), XFillPolygon(), XPutImage() and XDrawString() with a core fixed width font
// such as the X11R6 "10x20", "9x15" or "fixed".
//
// Everything is drawn on a back buffer pixmap of the window's size, and the parts that changed are copied to the
// window when the outermost endDrawing() returns and before the input waits for events (see flush()), so the
// window never shows a half drawn menu and exposures are repaired without the application.
//
// Native pixel values are X pixel values. They are read from, and written to, a client side copy of the back buffer
// (one XGetImage() per snapshot instead of one round trip per pixel), which is sent back with XPutImage() before
// the next drawing operation.
//
// Colors: TrueColor and DirectColor visuals get exact colors from their masks; other visuals with more than one
// bit get the colors allocated with XAllocColor() on first use (the nearest one when the colormap is full);
// monochrome visuals show black and white.
class XlibFramebuffer : public Framebuffer {
public:
    static const char *const DEFAULT_FONTS[];

    XlibFramebuffer();
    ~XlibFramebuffer();
    XlibFramebuffer(const XlibFramebuffer &) = delete;
    XlibFramebuffer &operator=(const XlibFramebuffer &) = delete;

    // Connects to the display (null for $DISPLAY), loads the font (null for the first of DEFAULT_FONTS found) and
    // maps the window. The geometry is an X geometry specification, WIDTHxHEIGHT+X+Y with every part optional;
    // without it the window is as large as the work area of the desktop and asks the window manager to maximize
    // it. Returns false, with a message on the standard error, when the window cannot be created.
    bool open(const char *displayName, const char *geometry, const char *fontName, const char *title);
    void close();
    bool isOpen() const { return display != nullptr; }
    // Waits until the window manager maps the window, so that its first size is known. Returns false if the
    // connection is lost.
    bool waitMapped();

    Display *getDisplay() const { return display; }
    Window getWindow() const { return window; }
    const char *getFontName() const { return fontName; }
    // The pixel value for a 0xRRGGBB color.
    unsigned long native(uint32_t color);

    // For the input: the window has a new size; returns true when it changed and the back buffer was replaced.
    bool resize(int width, int height);
    // For the input: copies a rectangle of the back buffer to the window.
    void expose(int x, int y, int width, int height);
    // Copies what changed since the last flush to the window and sends the requests to the server.
    void flush();

    int width() const override { return surfaceWidth; }
    int height() const override { return surfaceHeight; }
    int bitsPerPixel() const override { return depthBits; }
    int fontWidth() const override { return advance; }
    int fontHeight() const override { return lineHeight; }
    int textWidth(const char *text, int byteCount) const override;
    void setClip(int x, int y, int width, int height) override;
    void resetClip() override;
    void pixel(int x, int y, uint32_t color) override;
    uint32_t nativePixel(int x, int y) const override;
    void putNativePixel(int x, int y, uint32_t value) override;
    void line(int x0, int y0, int x1, int y1, uint32_t color) override;
    void fillRectangle(int x, int y, int width, int height, uint32_t color) override;
    void rectangle(int x, int y, int width, int height, uint32_t color) override;
    void fillPolygon(const int *xy, int count, uint32_t color) override;
    void putImage(int x, int y, int width, int height, const uint32_t *rgb) override;
    bool readImage(int x, int y, int width, int height, uint32_t *rgb) const override;
    void text(int x, int y, const char *text, uint32_t color) override;
    void beginDrawing() override;
    void endDrawing() override;

private:
    Display *display;
    int screen;
    Window window;
    Visual *visual;
    Colormap colormap;
    int depth;
    int depthBits;
    bool trueColor;
    unsigned long redMask;
    unsigned long greenMask;
    unsigned long blueMask;
    Pixmap back;
    GC gc;
    XFontStruct *font;
    char fontName[64];
    int advance;
    int lineHeight;
    int surfaceWidth;
    int surfaceHeight;
    int clipLeft;
    int clipTop;
    int clipRight;
    int clipBottom;
    unsigned long foreground;
    int drawing;
    bool mapped;
    std::map<uint32_t, unsigned long> allocated;
    // The client side copy of the back buffer: valid when it matches the back buffer once the pending rectangle
    // of pixels written to it is sent.
    mutable XImage *mirror;
    mutable bool mirrorValid;
    int pendingLeft;
    int pendingTop;
    int pendingRight;
    int pendingBottom;
    // The part of the back buffer not yet copied to the window.
    int damageLeft;
    int damageTop;
    int damageRight;
    int damageBottom;

    bool loadFont(const char *name);
    void maximizedArea(int &x, int &y, int &width, int &height) const;
    void createBackBuffer(int width, int height);
    void setForeground(uint32_t color);
    void beforeDrawing();
    void damage(int x, int y, int width, int height);
    void damageAll() { damage(0, 0, surfaceWidth, surfaceHeight); }
    bool fetchMirror() const;
    void sendPending();
    uint32_t rgbOf(unsigned long value) const;
    static int channelShift(unsigned long mask);
    static unsigned long channel(uint32_t value, unsigned long mask);
    static unsigned decode(const char *&text);
};

#endif
