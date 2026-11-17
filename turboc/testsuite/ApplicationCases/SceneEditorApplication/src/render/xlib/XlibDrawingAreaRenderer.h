#ifndef __XLIB_DRAWING_AREA_RENDERER__
#define __XLIB_DRAWING_AREA_RENDERER__

#include <X11/Xlib.h>
#include "java/lang/Runnable.h"
#include "java/util/function/Consumer.h"
#include "java/util/function/Function.h"
#include "java/util/function/Supplier.h"

class ApplicationModel;
class BrepExportCache;
class CameraControlGizmo2DPainter;
class DrawingAreaGizmoPresenter;
class DrawingAreaHost;
class FrameCaptureService;
class Gizmo2DDrawList;
class RotateGizmo;
class ScaleGizmo;
class SceneSelectionEditor;
class TranslateGizmo;
class Viewport;
class XlibGizmo2DDrawListRenderer;

/**
CPU presentation of the complete viewport set, without a GL context. Over
the views, the manipulation gizmo of the interaction mode and the input
gizmo (numeric boxes) of the selected viewport are drawn as 2D primitives
computed by the gizmo painters of `vsdk/toolkit/render/gizmo` (hidden
surfaces solved with the painter's algorithm) and drawn by
`XlibGizmo2DDrawListRenderer`, as the Java `AwtDrawingAreaRenderer` does.

Over the selected viewport the camera control gizmo (ViewCube) is drawn as
a HUD at its upper right corner, with the hidden lines of its parts removed
by Appel's algorithm (see `CameraControlGizmo2DPainter`).
*/
class XlibDrawingAreaRenderer {
    ApplicationModel* model;
    DrawingAreaHost* host;
    FrameCaptureService* frameCapture;
    Consumer<Viewport*>* selectedListener;
    Display* fontDisplay;
    XFontSet titleFont;
    int titleFontSize;
    SceneSelectionEditor* selectionEditor;
    DrawingAreaGizmoPresenter* gizmoPresenter;
    Gizmo2DDrawList* gizmoDrawList;
    XlibGizmo2DDrawListRenderer* gizmoRenderer;
    Gizmo2DDrawList* cameraControlDrawList;
    CameraControlGizmo2DPainter* cameraControlPainter;
    /// Boundary representations of the volumes drawn in the views
    BrepExportCache* brepCache;

    void ensureTitleFont(Display* display, int size);
    void drawView(Display* display, Drawable frame, GC gc, Visual* visual,
                  int depth, Viewport* viewport, int x, int y, int width,
                  int height);
    void drawTitle(Display* display, Drawable frame, GC gc, Visual* visual,
                   Viewport* viewport, int x, int y);
    void drawGizmos(Display* display, Drawable frame, GC gc, Visual* visual,
                    Viewport* viewport, int x, int y, int width, int height);

public:
    /**
    @param model the model whose viewport set is drawn
    @param host services of the GUI (raytracing, messages)
    @param selected called with the selected viewport on each frame
    @param translationGizmo gizmo to draw in translation mode
    @param rotateGizmo gizmo to draw in rotation mode
    @param scaleGizmo gizmo to draw in scale mode
    */
    XlibDrawingAreaRenderer(ApplicationModel* model, DrawingAreaHost* host,
                            Consumer<Viewport*>* selected,
                            TranslateGizmo* translationGizmo,
                            RotateGizmo* rotateGizmo,
                            ScaleGizmo* scaleGizmo);
    ~XlibDrawingAreaRenderer();
    void display(Display* display, Window window, Visual* visual, int depth,
                 int width, int height);
};

#endif
