#ifndef __FRAMEBUFFER_DRAWING_AREA_RENDERER__
#define __FRAMEBUFFER_DRAWING_AREA_RENDERER__

#include "java/util/function/Consumer.h"
#include <stdint.h>

class ApplicationModel;
class Calligraphic2DBuffer;
class Camera;
class ColorRgb;
class DrawingAreaHost;
class DrawingAreaGizmoPresenter;
class FrameCaptureService;
class Framebuffer;
class CameraControlGizmo2DPainter;
class Gizmo2DDrawList;
class RotateGizmo;
class ScaleGizmo;
class SceneSelectionEditor;
class TranslateGizmo;
class Viewport;
class ProjectedSurfacesViewBuilder;

/**
CPU presentation of the complete viewport set on a `Framebuffer`, the
technology independent drawing surface of the framebuffer GUI: the same
views as `XlibDrawingAreaRenderer`, but drawn only with the operations every
framebuffer backend has (lines, filled rectangles, images and text), so it
works with Xlib, svgalib or any other backend.

Each viewport shows its background, its grid, the wireframes of surfaces and
the hidden line drawings of volumes (the bodies of the selection in red), or
the raytraced image of the scene in raytracing mode, and its title. Over
both, the manipulation gizmo of the interaction mode and the input gizmo
(numeric boxes) of the selected viewport are drawn as 2D primitives computed
by the gizmo painters of `vsdk/toolkit/render/gizmo` (hidden surfaces solved
with the painter's algorithm) and drawn by
`FramebufferGizmo2DDrawListRenderer`, as the Java `AwtDrawingAreaRenderer`
does. Over the selected viewport the camera control gizmo (ViewCube) is drawn
as a HUD at its upper right corner, with the hidden lines of its parts
removed by Appel's algorithm (see `CameraControlGizmo2DPainter`). Frame captures read the surface back with `Framebuffer::readImage`.
*/
class FramebufferDrawingAreaRenderer {
private:
    ApplicationModel* model;
    DrawingAreaHost* host;
    FrameCaptureService* frameCapture;
    int ditheringMode;
    java::Consumer<Viewport*> selectedListener;
    SceneSelectionEditor* selectionEditor;
    DrawingAreaGizmoPresenter* gizmoPresenter;
    Gizmo2DDrawList* gizmoDrawList;
    Gizmo2DDrawList* cameraControlDrawList;
    CameraControlGizmo2DPainter* cameraControlPainter;
    /// The solids of the views as visible polygons (see
    /// `ProjectedSurfacesViewBuilder`), in hidden surfaces render mode
    ProjectedSurfacesViewBuilder* surfacesBuilder;

    void drawLines(Framebuffer& surface, const Calligraphic2DBuffer& lines,
                   int x, int y, int width, int height, uint32_t color,
                   int lineWidth);
    void drawGrid(Framebuffer& surface, Camera* camera, int x, int y,
                  int width, int height);
    void drawView(Framebuffer& surface, Viewport* viewport, int x, int y,
                  int width, int height);
    void drawHiddenSurfaces(Framebuffer& surface, Viewport* viewport, int x,
                            int y, int width, int height);
    void drawTitle(Framebuffer& surface, Viewport* viewport, int x, int y);
    void drawGizmos(Framebuffer& surface, Viewport* viewport, int x, int y,
                    int width, int height);

    FramebufferDrawingAreaRenderer(const FramebufferDrawingAreaRenderer&);
    FramebufferDrawingAreaRenderer& operator=(
        const FramebufferDrawingAreaRenderer&);

public:
    /**
    @param model the model whose viewport set is drawn
    @param host services of the GUI (raytracing, messages)
    @param selected called with the selected viewport on each frame
    @param translationGizmo gizmo to draw in translation mode
    @param rotateGizmo gizmo to draw in rotation mode
    @param scaleGizmo gizmo to draw in scale mode
    */
    FramebufferDrawingAreaRenderer(
        ApplicationModel* model, DrawingAreaHost* host,
        const java::Consumer<Viewport*>& selected,
        TranslateGizmo* translationGizmo, RotateGizmo* rotateGizmo,
        ScaleGizmo* scaleGizmo);
    ~FramebufferDrawingAreaRenderer();

    /**
    Draws the viewport set on a rectangle of the surface.
    */
    void display(Framebuffer& surface, int x, int y, int width, int height);
    const char* nextDitheringMode(int bitsPerPixel);

    /**
    @return the packed 0xRRGGBB value of a color
    */
    static uint32_t packed(const ColorRgb& color);
};

#endif
