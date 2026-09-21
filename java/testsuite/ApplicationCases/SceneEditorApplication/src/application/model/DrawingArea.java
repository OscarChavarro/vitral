package application.model;

import java.io.File;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.gui.MouseEvent;
import vsdk.toolkit.gui.viewport.Viewport;
import vsdk.toolkit.gui.viewport.ViewportSet;

/**
State of the drawing area of the editor: the area where a `ViewportSet` is
presented and manipulated. It is a plain model, independent of the GUI and
rendering technologies in use.

Two coordinate systems are related here: the canvas (logical pixels of the
GUI component, where the pointer is reported) and the surface (physical pixels
of the drawing surface, which differ from the canvas ones in high density
displays). The viewport set is always expressed in surface pixels.
*/
public class DrawingArea
{
    private final ViewportSet viewportSet;

    private InteractionMode interactionMode;
    private InteractionMode lastInteractionMode;

    private boolean colorCaptureRequested;
    private boolean depthCaptureRequested;
    private boolean contoursRequested;
    private boolean projectedViewsDebugRequested;

    private File pendingViewportExportFile;
    private boolean pendingViewportExportJpg;
    private File pendingWorkspaceExportFile;

    private int canvasWidth;
    private int canvasHeight;

    /**
    @param viewportSet the viewport set presented by this drawing area
    */
    public DrawingArea(ViewportSet viewportSet)
    {
        this.viewportSet = viewportSet;

        // As in 3ds Max, the application starts in selection mode
        interactionMode = InteractionMode.SELECT;
        lastInteractionMode = InteractionMode.SELECT;
        colorCaptureRequested = false;
        depthCaptureRequested = false;
        contoursRequested = false;
        projectedViewsDebugRequested = false;
        pendingViewportExportFile = null;
        pendingViewportExportJpg = false;
        pendingWorkspaceExportFile = null;
        canvasWidth = 0;
        canvasHeight = 0;
    }

    public ViewportSet getViewportSet()
    {
        return viewportSet;
    }

    //= Interaction mode ==================================================

    public InteractionMode getInteractionMode()
    {
        return interactionMode;
    }

    public void setInteractionMode(InteractionMode interactionMode)
    {
        this.interactionMode = interactionMode;
    }

    public InteractionMode getLastInteractionMode()
    {
        return lastInteractionMode;
    }

    /**
    Changes the interaction mode, remembering the previous one (the camera
    mode is transient over a translation, for example).
    @param interactionMode the new mode
    */
    public void switchInteractionMode(InteractionMode interactionMode)
    {
        lastInteractionMode = this.interactionMode;
        this.interactionMode = interactionMode;
    }

    /**
    @return true if the translation gizmo is to be shown. The selection mode
    (as in 3ds Max) only marks the selected objects with the selection
    corners: it shows no gizmo
    */
    public boolean shouldDrawTranslationGizmo()
    {
        return interactionMode == InteractionMode.TRANSLATE ||
            (interactionMode == InteractionMode.CAMERA &&
             lastInteractionMode == InteractionMode.TRANSLATE);
    }

    //= Pending requests to the renderer ==================================

    public boolean isColorCaptureRequested()
    {
        return colorCaptureRequested;
    }

    public void setColorCaptureRequested(boolean colorCaptureRequested)
    {
        this.colorCaptureRequested = colorCaptureRequested;
    }

    public boolean isDepthCaptureRequested()
    {
        return depthCaptureRequested;
    }

    public void setDepthCaptureRequested(boolean depthCaptureRequested)
    {
        this.depthCaptureRequested = depthCaptureRequested;
    }

    /**
    @return true if the requested depth capture must be presented as a
    contours (normal map) image
    */
    public boolean isContoursRequested()
    {
        return contoursRequested;
    }

    public void setContoursRequested(boolean contoursRequested)
    {
        this.contoursRequested = contoursRequested;
    }

    public boolean isProjectedViewsDebugRequested()
    {
        return projectedViewsDebugRequested;
    }

    public void setProjectedViewsDebugRequested(boolean projectedViewsDebugRequested)
    {
        this.projectedViewsDebugRequested = projectedViewsDebugRequested;
    }

    /**
    Requests the export of the selected viewport, to be done in the next frame.
    @param file destination file
    @param jpg true for a JPG file, false for a PNG one
    */
    public void requestViewportExport(File file, boolean jpg)
    {
        pendingViewportExportFile = file;
        pendingViewportExportJpg = jpg;
    }

    /**
    Requests the export of the whole viewport set area as a JPG, to be done in
    the next frame.
    @param file destination file
    */
    public void requestWorkspaceExport(File file)
    {
        pendingWorkspaceExportFile = file;
    }

    public File getPendingViewportExportFile()
    {
        return pendingViewportExportFile;
    }

    public boolean isPendingViewportExportJpg()
    {
        return pendingViewportExportJpg;
    }

    public File getPendingWorkspaceExportFile()
    {
        return pendingWorkspaceExportFile;
    }

    public void clearPendingViewportExport()
    {
        pendingViewportExportFile = null;
    }

    public void clearPendingWorkspaceExport()
    {
        pendingWorkspaceExportFile = null;
    }

    //= Viewport set operations ===========================================

    public void toggleSelectedViewportGrid()
    {
        Viewport selected = viewportSet.getSelectedViewport();

        if ( selected != null ) {
            selected.toggleGrid();
        }
    }

    public void addViewport()
    {
        viewportSet.addViewport(new Viewport());
    }

    /**
    Removes the last viewport, if there is more than one.
    */
    public void removeLastViewport()
    {
        if ( viewportSet.getViewportCount() > 1 ) {
            viewportSet.removeViewport(viewportSet.getViewportCount() - 1);
        }
    }

    //= Canvas / surface relation =========================================

    public int getCanvasWidth()
    {
        return canvasWidth;
    }

    public int getCanvasHeight()
    {
        return canvasHeight;
    }

    /**
    Records the size of the canvas, in logical pixels. Empty sizes (i.e. from
    a component not yet laid out) are ignored.
    @param width
    @param height
    */
    public void updateCanvasSize(int width, int height)
    {
        if ( width <= 0 || height <= 0 ) {
            return;
        }
        canvasWidth = width;
        canvasHeight = height;
    }

    /**
    Records the size of the canvas, in logical pixels, as reported by the
    last resize of the drawing surface.
    @param width
    @param height
    */
    public void setCanvasSize(int width, int height)
    {
        canvasWidth = width;
        canvasHeight = height;
    }

    /**
    Makes the viewport set match the size of the drawing surface.
    @param surfaceWidth width in physical pixels
    @param surfaceHeight height in physical pixels
    */
    public void updateSurfaceSize(int surfaceWidth, int surfaceHeight)
    {
        if ( surfaceWidth <= 0 || surfaceHeight <= 0 ) {
            return;
        }
        if ( surfaceWidth == viewportSet.getSizeXInPixels() &&
             surfaceHeight == viewportSet.getSizeYInPixels() ) {
            return;
        }
        viewportSet.resize(surfaceWidth, surfaceHeight);
    }

    public int scaleXToSurface(int x)
    {
        if ( canvasWidth <= 0 || viewportSet.getSizeXInPixels() <= 0 ) {
            return x;
        }
        return (int)Math.round(((double)x * (double)viewportSet.getSizeXInPixels()) /
            (double)canvasWidth);
    }

    public int scaleYToSurface(int y)
    {
        if ( canvasHeight <= 0 || viewportSet.getSizeYInPixels() <= 0 ) {
            return y;
        }
        return (int)Math.round(((double)y * (double)viewportSet.getSizeYInPixels()) /
            (double)canvasHeight);
    }

    public int scaleXToCanvas(int x)
    {
        if ( canvasWidth <= 0 || viewportSet.getSizeXInPixels() <= 0 ) {
            return x;
        }
        return (int)Math.round(((double)x * (double)canvasWidth) /
            (double)viewportSet.getSizeXInPixels());
    }

    public int scaleYToCanvas(int y)
    {
        if ( canvasHeight <= 0 || viewportSet.getSizeYInPixels() <= 0 ) {
            return y;
        }
        return (int)Math.round(((double)y * (double)canvasHeight) /
            (double)viewportSet.getSizeYInPixels());
    }

    /**
    @param canvasEvent mouse event with coordinates in canvas pixels
    @return a copy of the event, with coordinates in surface pixels
    */
    public MouseEvent toSurfaceEvent(MouseEvent canvasEvent)
    {
        MouseEvent surfaceEvent = new MouseEvent();

        surfaceEvent.setX(scaleXToSurface(canvasEvent.getX()));
        surfaceEvent.setY(scaleYToSurface(canvasEvent.getY()));
        surfaceEvent.setButton(canvasEvent.getButton());
        surfaceEvent.setModifiers(canvasEvent.getModifiers());
        surfaceEvent.setClicks(canvasEvent.getClicks());
        return surfaceEvent;
    }

    /**
    Projects a point of the scene to canvas pixel coordinates using the
    active camera of a viewport.
    @param viewport viewport whose camera is used
    @param point point in world coordinates
    @return {x, y} in canvas pixels, or null if the point is behind the camera
    */
    public double[] projectToCanvas(Viewport viewport, Vector3Dd point)
    {
        Camera camera = viewport.getActiveCamera();
        camera.updateVectors();

        Vector3Dd d = point.subtract(camera.getPosition());
        Vector3Dd front = camera.getFront().normalized();
        Vector3Dd right = camera.getLeft().normalized().multiply(-1);
        Vector3Dd up = camera.getUp().normalized();
        double depth = d.dotProduct(front);

        if ( depth <= 0 ) {
            return null;
        }
        double u = d.dotProduct(right) / depth * 0.5 /
            camera.getRightWithScale().length();
        double v = d.dotProduct(up) / depth * 0.5 /
            camera.getUpWithScale().length();
        double w = camera.getViewportXSize();
        double h = camera.getViewportYSize();
        double surfaceX = viewport.getPixelStartX() + u * w + w / 2.0;
        double surfaceY = viewport.getPixelStartY() + h / 2.0 - 1 - v * h;

        return new double[] {
            surfaceX * canvasWidth / viewportSet.getSizeXInPixels(),
            surfaceY * canvasHeight / viewportSet.getSizeYInPixels()
        };
    }
}
