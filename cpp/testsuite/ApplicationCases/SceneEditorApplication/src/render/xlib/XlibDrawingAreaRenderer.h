#ifndef __XLIB_DRAWING_AREA_RENDERER__
#define __XLIB_DRAWING_AREA_RENDERER__

#include "java/util/function/Consumer.h"
#include <X11/Xlib.h>

class ApplicationModel;
class DrawingAreaHost;
class FrameCaptureService;
class Viewport;

/** CPU presentation of the complete viewport set, without a GL context. */
class XlibDrawingAreaRenderer {
    ApplicationModel* model;
    DrawingAreaHost* host;
    FrameCaptureService* frameCapture;
    java::Consumer<Viewport*> selectedListener;
    Display* fontDisplay;
    XFontSet titleFont;
    int titleFontSize;

    void ensureTitleFont(Display* display, int size);
    void drawView(Display* display, Drawable frame, GC gc, Visual* visual,
                  int depth, Viewport* viewport, int x, int y, int width,
                  int height);
    void drawTitle(Display* display, Drawable frame, GC gc, Visual* visual,
                   Viewport* viewport, int x, int y);

public:
    XlibDrawingAreaRenderer(ApplicationModel* model, DrawingAreaHost* host,
                            const java::Consumer<Viewport*>& selected);
    ~XlibDrawingAreaRenderer();
    void display(Display* display, Window window, Visual* visual, int depth,
                 int width, int height);
};

#endif
