package application.render.jogl;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.media.IndexedColorImageUncompressed;
import vsdk.toolkit.media.NormalMap;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.gui.viewport.ViewportSet;
import vsdk.toolkit.render.jogl.Jogl4FrameBufferReader;
import vsdk.toolkit.render.jogl.viewport.Jogl4ViewportWindow;
import vsdk.toolkit.render.jogl.viewport.JoglViewportSetRenderer;

import application.model.ApplicationModel;
import application.model.DrawingArea;

/**
Captures the content of the frame buffers when the user requested it through
the `DrawingArea`: the color buffer, the depth buffer, and the export of the
frame (or of the selected viewport) to image files. Captured images are left
in the application model and presented through the `Jogl4DrawingAreaHost`.
*/
public class Jogl4FrameCaptureService
{
    private final ApplicationModel model;
    private final DrawingArea drawingArea;
    private final ViewportSet viewportSet;
    private final Jogl4DrawingAreaHost host;

    public Jogl4FrameCaptureService(ApplicationModel model,
                                    Jogl4DrawingAreaHost host)
    {
        this.model = model;
        this.drawingArea = model.getDrawingArea();
        this.viewportSet = drawingArea.getViewportSet();
        this.host = host;
    }

    /**
    Captures the color buffer, if it was requested and the view being drawn is
    the selected one.
    @param gl
    @param selectedView true if the view being drawn is the selected one
    */
    public void copyColorBufferIfNeeded(GL4 gl, boolean selectedView)
    {
        if ( drawingArea.isColorCaptureRequested() && selectedView ) {
            model.setZbufferImage(Jogl4FrameBufferReader.readColor(gl));
            host.showImage(model.getZbufferImage());
            host.showStatusMessage("ZBuffer Color Image obtained!");
            drawingArea.setColorCaptureRequested(false);
        }
    }

    /**
    Captures the depth buffer, if it was requested.
    @param gl
    */
    public void copyZBufferIfNeeded(GL4 gl)
    {
        if ( !drawingArea.isDepthCaptureRequested() ) {
            return;
        }

        if ( drawingArea.isContoursRequested() ) {
            IndexedColorImageUncompressed zbuffer;
            NormalMap nm;
            zbuffer = Jogl4FrameBufferReader.readDepth(gl).exportIndexedColorImage();
            nm = new NormalMap();
            nm.importBumpMap(zbuffer, new Vector3Dd(1, 1, 0.1));
            model.setZbufferImage(nm.exportToRgbImageGradient());
        }
        else {
            model.setZbufferImage(
                Jogl4FrameBufferReader.readDepth(gl).exportRGBImage(
                    model.getPalette()));
        }

        host.showImage(model.getZbufferImage());
        host.showStatusMessage("ZBuffer depth map obtained!");
        drawingArea.setDepthCaptureRequested(false);
        drawingArea.setContoursRequested(false);
    }

    /**
    Exports to files the frame just drawn, if it was requested.
    @param gl
    @param viewportSetRenderer the renderer of the viewport set, to find the
    area of the selected viewport
    */
    public void exportPendingFrame(GL4 gl, JoglViewportSetRenderer viewportSetRenderer)
    {
        if ( drawingArea.getPendingViewportExportFile() == null &&
             drawingArea.getPendingWorkspaceExportFile() == null ) {
            return;
        }

        int width = viewportSet.getSizeXInPixels();
        int height = viewportSet.getSizeYInPixels();
        gl.glViewport(0, 0, width, height);
        RGBImageUncompressed workspace = Jogl4FrameBufferReader.readColor(gl);

        if ( drawingArea.getPendingViewportExportFile() != null ) {
            Jogl4ViewportWindow selected = viewportSetRenderer.getSelectedWindow();
            RGBImageUncompressed viewport = cropImage(workspace,
                selected.getViewportStartX(), selected.getViewportStartY(),
                selected.getViewportSizeX(), selected.getViewportSizeY());
            if ( drawingArea.isPendingViewportExportJpg() ) {
                ImagePersistence.exportJPG(drawingArea.getPendingViewportExportFile(), viewport);
            }
            else {
                ImagePersistence.exportPNG(drawingArea.getPendingViewportExportFile(), viewport);
            }
            drawingArea.clearPendingViewportExport();
        }

        if ( drawingArea.getPendingWorkspaceExportFile() != null ) {
            ImagePersistence.exportJPG(drawingArea.getPendingWorkspaceExportFile(), workspace);
            drawingArea.clearPendingWorkspaceExport();
        }
    }

    private RGBImageUncompressed cropImage(RGBImageUncompressed source,
                                           int startX, int startY,
                                           int width, int height)
    {
        int sourceWidth = source.getXSize();
        int sourceHeight = source.getYSize();
        int x0 = Math.max(0, startX);
        int y0 = Math.max(0, startY);
        int x1 = Math.min(sourceWidth, startX + width);
        int y1 = Math.min(sourceHeight, startY + height);
        RGBImageUncompressed result = new RGBImageUncompressed();
        result.init(Math.max(0, x1 - x0), Math.max(0, y1 - y0));
        for ( int y = y0; y < y1; y++ ) {
            for ( int x = x0; x < x1; x++ ) {
                RGBPixel pixel = source.getPixel(x, y);
                result.putPixel(x - x0, y - y0, pixel);
            }
        }
        return result;
    }
}
