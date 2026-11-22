#ifndef __XLIB_DRAWING_AREA_RENDERER__
#define __XLIB_DRAWING_AREA_RENDERER__

#include "java/util/function/Consumer.h"
#include <X11/Xlib.h>

class ApplicationModel;
class DrawingAreaGizmoPresenter;
class DrawingAreaHost;
class FrameCaptureService;
class CameraControlGizmo2DPainter;
class Gizmo2DDrawList;
class RotateGizmo;
class ScaleGizmo;
class SceneSelectionEditor;
class TranslateGizmo;
class Viewport;
class XlibGizmo2DDrawListRenderer;
class ProjectedLinesViewBuilder;
class ProjectedSurfacesViewBuilder;

/**
CPU presentation of the complete viewport set, without a GL context, in the
render modes Xlib offers (see `RenderTechnology::XLIB`): hidden lines,
hidden surfaces (the visible polygons of Weiler and Atherton, filled, or
only their contours when the rendering configuration turns the surfaces off
and the lines on) or raytracing. Over
the views, the manipulation gizmo of the interaction mode and the input
gizmo (numeric boxes) of the selected viewport are drawn as 2D primitives
computed by the gizmo painters of `vsdk/toolkit/render/gizmo` (hidden
surfaces solved with the painter's algorithm) and drawn by
`XlibGizmo2DDrawListRenderer`, as the Java `AwtDrawingAreaRenderer` does.
Over the selected viewport the camera control gizmo (ViewCube) is drawn as a
HUD at its upper right corner, with the hidden lines of its parts removed by
Appel's algorithm (see `CameraControlGizmo2DPainter`).
*/
class XlibDrawingAreaRenderer {
    ApplicationModel* model;
    DrawingAreaHost* host;
    FrameCaptureService* frameCapture;
    java::Consumer<Viewport*> selectedListener;
    Display* fontDisplay;
    XFontSet titleFont;
    int titleFontSize;
    SceneSelectionEditor* selectionEditor;
    DrawingAreaGizmoPresenter* gizmoPresenter;
    Gizmo2DDrawList* gizmoDrawList;
    Gizmo2DDrawList* cameraControlDrawList;
    CameraControlGizmo2DPainter* cameraControlPainter;
    XlibGizmo2DDrawListRenderer* gizmoRenderer;
    /// The bodies of the views as projected lines (see `ProjectedLinesViewBuilder`)
    ProjectedLinesViewBuilder* linesBuilder;
    /// The solids of the views as visible polygons (see
    /// `ProjectedSurfacesViewBuilder`), in hidden surfaces render mode
    ProjectedSurfacesViewBuilder* surfacesBuilder;

    void ensureTitleFont(Display* display, int size);
    void drawView(Display* display, Drawable frame, GC gc, Visual* visual,
                  int depth, Viewport* viewport, int x, int y, int width,
                  int height);
    void drawHiddenSurfaces(Display* display, Drawable frame, GC gc,
                            Visual* visual, Viewport* viewport, int x, int y,
                            int width, int height);
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
                            const java::Consumer<Viewport*>& selected,
                            TranslateGizmo* translationGizmo,
                            RotateGizmo* rotateGizmo,
                            ScaleGizmo* scaleGizmo);
    ~XlibDrawingAreaRenderer();
    void display(Display* display, Window window, Visual* visual, int depth,
                 int width, int height);
};

#endif
