package render.awt;

// AWT classes
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.logging.Logger;
import vsdk.toolkit.gui.viewport.Viewport;
import vsdk.toolkit.gui.viewport.ViewportSet;
import vsdk.toolkit.render.awt.AwtRGBImageUncompressedRenderer;

// Application classes
import model.ApplicationModel;
import model.DrawingArea;
import model.Scene;
import render.DrawingAreaHost;
import render.FrameCaptureService;
import render.ProjectedLineLayer;
import render.ProjectedLinesViewBuilder;

/**
Draws the scene in the viewports of a `DrawingArea` using only the 2D
operations of AWT, the counterpart of `Jogl4DrawingAreaRenderer` for a
drawing surface without any 3D API. As AWT knows nothing about cameras or
depth, every viewport is computed in the processor, in one of the render
modes AWT offers (see `model.RenderTechnology.AWT`):
  - Hidden lines: the scene is projected as lines by `ProjectedLinesViewBuilder`
    (wireframe, or hidden line removal for the solids when the rendering
    configuration asks for surfaces) and drawn by
    `AwtProjectedLineLayerRenderer`.
  - Raytracing: the raytraced image is drawn as is, and the editor
    annotations (selection corners, lights...) are drawn over it as lines,
    without depth test.

The frame is composed in an off-screen image with one pixel for each
physical pixel of the screen (the "surface" of `DrawingArea`), which is also
the frame buffer read by `FrameCaptureService`.

Not presented yet in this technology: manipulation gizmos, the visual debug
ray, the depth buffer captures and the projected views debugger.
*/
public class AwtDrawingAreaRenderer
{
    private static final Color SET_BACKGROUND = new Color(0.77f, 0.77f, 0.77f);

    private final ApplicationModel model;
    private final Scene theScene;
    private final DrawingArea drawingArea;
    private final ViewportSet viewportSet;
    private final DrawingAreaHost host;
    private final FrameCaptureService frameCapture;
    private final ProjectedLinesViewBuilder linesBuilder;
    private final AwtViewportSetRenderer viewportSetRenderer;
    private BufferedImage frame;

    /**
    @param model application model
    @param host services from the GUI technology hosting the surface
    @param selectedViewportListener notified with the viewport about to be
    drawn when it is the selected one (or the only one shown), so interaction
    techniques can work over its camera
    */
    public AwtDrawingAreaRenderer(ApplicationModel model, DrawingAreaHost host,
                                  Consumer<Viewport> selectedViewportListener)
    {
        this.model = model;
        this.theScene = model.getScene();
        this.drawingArea = model.getDrawingArea();
        this.viewportSet = drawingArea.getViewportSet();
        this.host = host;
        this.frameCapture = new FrameCaptureService(model, host);
        this.linesBuilder = new ProjectedLinesViewBuilder();
        this.frame = null;

        viewportSetRenderer = new AwtViewportSetRenderer(viewportSet,
            new AwtViewportSetRenderer.ViewRenderer() {
                @Override
                public void configureView(Viewport viewport) {
                    selectedViewportListener.accept(viewport);
                }

                @Override
                public void drawView(Graphics2D g, Viewport viewport, int x,
                                     int y, int width, int height) {
                    AwtDrawingAreaRenderer.this.drawView(g, viewport, x, y,
                        width, height);
                }
            });
    }

    /**
    Draws a frame.
    @param target graphics of the component presenting the drawing area
    @param canvasWidth width of the component, in logical pixels
    @param canvasHeight height of the component, in logical pixels
    @param pixelScale physical pixels for each logical pixel (i.e. 2 on high
    density displays)
    */
    public void display(Graphics2D target, int canvasWidth, int canvasHeight,
                        double pixelScale)
    {
        if ( canvasWidth <= 0 || canvasHeight <= 0 ) {
            return;
        }
        host.beforeFrame();
        reportUnsupportedRequests();
        drawingArea.updateSurfaceSize(
            (int)Math.round(canvasWidth * pixelScale),
            (int)Math.round(canvasHeight * pixelScale));
        viewportSet.enforceAvailableRenderModes();

        int width = viewportSet.getSizeXInPixels();
        int height = viewportSet.getSizeYInPixels();
        if ( frame == null || frame.getWidth() != width || frame.getHeight() != height ) {
            frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        }

        Graphics2D g = frame.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);
            g.setColor(SET_BACKGROUND);
            g.fillRect(0, 0, width, height);
            viewportSetRenderer.draw(g, host.isFullScreenGuiMode());
        }
        finally {
            g.dispose();
        }

        if ( frameCapture.isFrameExportPending() ) {
            frameCapture.exportPendingFrame(
                new AwtFrameBufferSource(frame, 0, 0, width, height));
        }

        target.drawImage(frame, 0, 0, canvasWidth, canvasHeight, null);
    }

    private void drawView(Graphics2D g, Viewport viewport, int x, int y,
                          int width, int height)
    {
        List<ProjectedLineLayer> layers;
        boolean raytraced = viewport.getRenderMode() == Viewport.RENDER_MODE_RAYTRACING;

        theScene.activeCamera = viewport.getActiveCamera();
        theScene.qualityTemplate = viewport.getRendererConfiguration();

        if ( raytraced ) {
            theScene.activateSelectedBackground();
            model.setRaytracedImageWidth(width);
            model.setRaytracedImageHeight(height);
            host.raytraceImage();
            AwtRGBImageUncompressedRenderer.draw(g, model.getRaytracedImage(), x, y);
        }
        else {
            g.setColor(AwtProjectedLineLayerRenderer.toAwtColor(
                theScene.simpleBackground.colorInDireccion(
                    theScene.activeCamera.getFront())));
            g.fillRect(x, y, width, height);
        }

        try {
            layers = linesBuilder.buildView(theScene, viewport,
                host.getBodyEditFeedbackProvider(), !raytraced);
        }
        catch ( RuntimeException e ) {
            // A failure of the line algorithms over some body must not break
            // the presentation of the whole set
            Logger.reportMessage(this, VSDK.WARNING, "drawView",
                "Can not compute the lines of viewport \"" +
                viewportSet.getTitleFor(viewport) + "\": " + e);
            layers = new ArrayList<ProjectedLineLayer>();
        }
        AwtProjectedLineLayerRenderer.draw(g, layers, x, y, width, height,
            viewportSet.getElementScaler().getScale());

        frameCapture.copyColorBufferIfNeeded(
            new AwtFrameBufferSource(frame, x, y, width, height),
            viewportSet.isSelected(viewport));
    }

    /**
    Captures that need a depth buffer or a 3D API are not available in AWT:
    they are reported and dropped, instead of being left pending.
    */
    private void reportUnsupportedRequests()
    {
        if ( drawingArea.isDepthCaptureRequested() ) {
            drawingArea.setDepthCaptureRequested(false);
            drawingArea.setContoursRequested(false);
            host.showStatusMessage("Depth buffer captures are not available when rendering with AWT");
        }
        if ( drawingArea.isProjectedViewsDebugRequested() ) {
            drawingArea.setProjectedViewsDebugRequested(false);
            host.showStatusMessage("Projected views debugging is not available when rendering with AWT");
        }
    }
}
