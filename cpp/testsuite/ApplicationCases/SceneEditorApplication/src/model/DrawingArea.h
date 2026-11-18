#ifndef __DRAWING_AREA__
#define __DRAWING_AREA__

#include "java/io/File.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/gui/MouseEvent.h"
#include "model/InteractionMode.h"
#include "model/RenderTechnology.h"

class Viewport;
class ViewportSet;

/**
State of the drawing area of the editor: the area where a `ViewportSet` is
presented and manipulated. It is a plain model, independent of the GUI and
rendering technologies in use.

Two coordinate systems are related here: the canvas (logical pixels of the
GUI component, where the pointer is reported) and the surface (physical
pixels of the drawing surface, which differ from the canvas ones in high
density displays). The viewport set is always expressed in surface pixels.
The viewport set is referenced, not owned.
*/
class DrawingArea {
private:
    ViewportSet* viewportSet;
    RenderTechnology::Value renderTechnology;

    InteractionMode interactionMode;
    InteractionMode lastInteractionMode;

    bool colorCaptureRequested;
    bool depthCaptureRequested;
    bool contoursRequested;
    bool projectedViewsDebugRequested;

    bool hasPendingViewportExport;
    java::File pendingViewportExportFile;
    bool pendingViewportExportJpg;
    bool hasPendingWorkspaceExport;
    java::File pendingWorkspaceExportFile;

    int canvasWidth;
    int canvasHeight;

    long long contentGeneration;
    bool interactiveGestureInCourse;

public:
    /**
    @param viewportSet the viewport set presented by this drawing area
    */
    explicit DrawingArea(ViewportSet* viewportSet);

    ViewportSet* getViewportSet() const;

    //= Render technology =================================================

    /**
    @return the technology presenting the viewport set
    */
    RenderTechnology::Value getRenderTechnology() const;

    /**
    Changes the technology presenting the viewport set, which changes the
    render modes its viewports can use. The application must replace the
    drawing surface accordingly.
    */
    void setRenderTechnology(RenderTechnology::Value renderTechnology);

    //= Frame invalidation ================================================

    /**
    @return a number that changes each time the content of the scene
    (bodies, lights, materials, backgrounds, rendering configuration...) may
    have changed. A frame computed for an equal generation, with the same
    camera and size, can be presented again (i.e. a raytraced image).
    Changes of the cameras alone do not change it.
    */
    long long getContentGeneration() const;

    /**
    The content of the scene may have changed: frames computed before can not
    be presented again. Any request to draw again that is not known to change
    only the cameras or overlays must call this.
    */
    void markContentChanged();

    /**
    @return true while the user is doing a continuous gesture over the
    drawing area (dragging with a button pressed, or an animation of the
    cameras is in course): expensive render modes (raytracing) may draw
    approximated frames, and the exact one when it ends
    */
    bool isInteractiveGestureInCourse() const;

    void setInteractiveGestureInCourse(bool inCourse);

    //= Interaction mode ==================================================
    InteractionMode getInteractionMode() const;
    void setInteractionMode(InteractionMode interactionMode);
    InteractionMode getLastInteractionMode() const;

    /**
    Changes the interaction mode, remembering the previous one (the camera
    mode is transient over a translation, for example).
    */
    void switchInteractionMode(InteractionMode interactionMode);

    /**
    @return true if the translation gizmo is to be shown. The selection mode
    (as in 3ds Max) only marks the selected objects with the selection
    corners: it shows no gizmo
    */
    bool shouldDrawTranslationGizmo() const;

    //= Pending requests to the renderer ==================================
    bool isColorCaptureRequested() const;
    void setColorCaptureRequested(bool colorCaptureRequested);
    bool isDepthCaptureRequested() const;
    void setDepthCaptureRequested(bool depthCaptureRequested);

    /**
    @return true if the requested depth capture must be presented as a
    contours (normal map) image
    */
    bool isContoursRequested() const;
    void setContoursRequested(bool contoursRequested);
    bool isProjectedViewsDebugRequested() const;
    void setProjectedViewsDebugRequested(bool projectedViewsDebugRequested);

    /**
    Requests the export of the selected viewport, to be done in the next
    frame.
    @param file destination file
    @param jpg true for a JPG file, false for a PNG one
    */
    void requestViewportExport(const java::File& file, bool jpg);

    /**
    Requests the export of the whole viewport set area as a JPG, to be done
    in the next frame.
    */
    void requestWorkspaceExport(const java::File& file);

    /**
    @return the destination of the pending viewport export, or null if there
    is none
    */
    const java::File* getPendingViewportExportFile() const;
    bool isPendingViewportExportJpg() const;

    /**
    @return the destination of the pending workspace export, or null if
    there is none
    */
    const java::File* getPendingWorkspaceExportFile() const;
    void clearPendingViewportExport();
    void clearPendingWorkspaceExport();

    //= Viewport set operations ===========================================
    void toggleSelectedViewportGrid();
    void addViewport();

    /**
    Removes (and deletes) the last viewport, if there is more than one.
    */
    void removeLastViewport();

    //= Canvas / surface relation =========================================
    int getCanvasWidth() const;
    int getCanvasHeight() const;

    /**
    Records the size of the canvas, in logical pixels. Empty sizes (i.e.
    from a component not yet laid out) are ignored.
    */
    void updateCanvasSize(int width, int height);

    /**
    Records the size of the canvas, in logical pixels, as reported by the
    last resize of the drawing surface.
    */
    void setCanvasSize(int width, int height);

    /**
    Makes the viewport set match the size of the drawing surface.
    @param surfaceWidth width in physical pixels
    @param surfaceHeight height in physical pixels
    */
    void updateSurfaceSize(int surfaceWidth, int surfaceHeight);
    int scaleXToSurface(int x) const;
    int scaleYToSurface(int y) const;
    int scaleXToCanvas(int x) const;
    int scaleYToCanvas(int y) const;

    /**
    @param canvasEvent mouse event with coordinates in canvas pixels
    @return a copy of the event, with coordinates in surface pixels
    */
    MouseEvent toSurfaceEvent(const MouseEvent& canvasEvent) const;

    /**
    Projects a point of the scene to canvas pixel coordinates using the
    active camera of a viewport.
    @param viewport viewport whose camera is used
    @param point point in world coordinates
    @param outCanvas {x, y} in canvas pixels
    @return false if the point is behind the camera (the Java version
    returns null)
    */
    bool projectToCanvas(Viewport* viewport, const Vector3Dd& point,
                         double outCanvas[2]) const;
};

#endif
