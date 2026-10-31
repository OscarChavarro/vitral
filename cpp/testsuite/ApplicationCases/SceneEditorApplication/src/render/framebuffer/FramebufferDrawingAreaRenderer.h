#ifndef __FRAMEBUFFER_DRAWING_AREA_RENDERER__
#define __FRAMEBUFFER_DRAWING_AREA_RENDERER__

#include "java/util/function/Consumer.h"
#include <stdint.h>

class ApplicationModel;
class Calligraphic2DBuffer;
class Camera;
class ColorRgb;
class DrawingAreaHost;
class FrameCaptureService;
class Framebuffer;
class Viewport;

/**
CPU presentation of the complete viewport set on a `Framebuffer`, the
technology independent drawing surface of the framebuffer GUI: the same
views as `XlibDrawingAreaRenderer`, but drawn only with the operations every
framebuffer backend has (lines, filled rectangles, images and text), so it
works with Xlib, svgalib or any other backend.

Each viewport shows its background, its grid, the wireframes of surfaces and
the hidden line drawings of volumes (the bodies of the selection in red), or
the raytraced image of the scene in raytracing mode, and its title. Frame
captures read the surface back with `Framebuffer::readImage`.
*/
class FramebufferDrawingAreaRenderer {
private:
    ApplicationModel* model;
    DrawingAreaHost* host;
    FrameCaptureService* frameCapture;
    java::Consumer<Viewport*> selectedListener;

    void drawLines(Framebuffer& surface, const Calligraphic2DBuffer& lines,
                   int x, int y, int width, int height, uint32_t color,
                   int lineWidth);
    void drawGrid(Framebuffer& surface, Camera* camera, int x, int y,
                  int width, int height);
    void drawView(Framebuffer& surface, Viewport* viewport, int x, int y,
                  int width, int height);
    void drawTitle(Framebuffer& surface, Viewport* viewport, int x, int y);

    FramebufferDrawingAreaRenderer(const FramebufferDrawingAreaRenderer&);
    FramebufferDrawingAreaRenderer& operator=(
        const FramebufferDrawingAreaRenderer&);

public:
    /**
    @param model the model whose viewport set is drawn
    @param host services of the GUI (raytracing, messages)
    @param selected called with the selected viewport on each frame
    */
    FramebufferDrawingAreaRenderer(
        ApplicationModel* model, DrawingAreaHost* host,
        const java::Consumer<Viewport*>& selected);
    ~FramebufferDrawingAreaRenderer();

    /**
    Draws the viewport set on a rectangle of the surface.
    */
    void display(Framebuffer& surface, int x, int y, int width, int height);

    /**
    @return the packed 0xRRGGBB value of a color
    */
    static uint32_t packed(const ColorRgb& color);
};

#endif
