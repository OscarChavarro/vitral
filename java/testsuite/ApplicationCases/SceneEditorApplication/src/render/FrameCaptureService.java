package render;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.gui.viewport.Viewport;
import vsdk.toolkit.gui.viewport.ViewportSet;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.media.IndexedColorImageUncompressed;
import vsdk.toolkit.media.NormalMap;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.RGBPixel;

import model.ApplicationModel;
import model.DrawingArea;

/**
Captures the content of the frame buffers when the user requested it through
the `DrawingArea`: the color buffer, the depth buffer, and the export of the
frame (or of the selected viewport) to image files. Captured images are left
in the application model and presented through the `DrawingAreaHost`. The
buffers are read through a `FrameBufferSource`, so this class does not depend
on the rendering technology.
*/
public class FrameCaptureService
{
    private final ApplicationModel model;
    private final DrawingArea drawingArea;
    private final ViewportSet viewportSet;
    private final DrawingAreaHost host;

    public FrameCaptureService(ApplicationModel model,
                               DrawingAreaHost host)
    {
        this.model = model;
        this.drawingArea = model.getDrawingArea();
        this.viewportSet = drawingArea.getViewportSet();
        this.host = host;
    }

    /**
    Captures the color buffer, if it was requested and the view being drawn is
    the selected one.
    @param source frame buffer of the view being drawn
    @param selectedView true if the view being drawn is the selected one
    */
    public void copyColorBufferIfNeeded(FrameBufferSource source, boolean selectedView)
    {
        if ( drawingArea.isColorCaptureRequested() && selectedView ) {
            model.setZbufferImage(source.readColor());
            host.showImage(model.getZbufferImage());
            host.showStatusMessage("ZBuffer Color Image obtained!");
            drawingArea.setColorCaptureRequested(false);
        }
    }

    /**
    Captures the depth buffer, if it was requested.
    @param source frame buffer of the view being drawn
    */
    public void copyZBufferIfNeeded(FrameBufferSource source)
    {
        if ( !drawingArea.isDepthCaptureRequested() ) {
            return;
        }

        if ( drawingArea.isContoursRequested() ) {
            IndexedColorImageUncompressed zbuffer;
            NormalMap nm;
            zbuffer = source.readDepth().exportIndexedColorImage();
            nm = new NormalMap();
            nm.importBumpMap(zbuffer, new Vector3Dd(1, 1, 0.1));
            model.setZbufferImage(nm.exportToRgbImageGradient());
        }
        else {
            model.setZbufferImage(
                source.readDepth().exportRGBImage(model.getPalette()));
        }

        host.showImage(model.getZbufferImage());
        host.showStatusMessage("ZBuffer depth map obtained!");
        drawingArea.setDepthCaptureRequested(false);
        drawingArea.setContoursRequested(false);
    }

    /**
    @return true if the user requested to export the next frame to files
    */
    public boolean isFrameExportPending()
    {
        return drawingArea.getPendingViewportExportFile() != null ||
            drawingArea.getPendingWorkspaceExportFile() != null;
    }

    /**
    Exports to files the frame just drawn, if it was requested.
    @param source frame buffer, set to the whole viewport set area
    */
    public void exportPendingFrame(FrameBufferSource source)
    {
        if ( !isFrameExportPending() ) {
            return;
        }

        RGBImageUncompressed workspace = source.readColor();

        if ( drawingArea.getPendingViewportExportFile() != null ) {
            Viewport selected = viewportSet.getSelectedViewport();
            if ( selected != null ) {
                RGBImageUncompressed viewport = cropImage(workspace,
                    selected.getPixelStartX(), selected.getPixelStartY(),
                    selected.getPixelSizeX(), selected.getPixelSizeY());
                if ( drawingArea.isPendingViewportExportJpg() ) {
                    ImagePersistence.exportJPG(drawingArea.getPendingViewportExportFile(), viewport);
                }
                else {
                    ImagePersistence.exportPNG(drawingArea.getPendingViewportExportFile(), viewport);
                }
            }
            else {
                host.showStatusMessage("ERROR: there is no selected viewport to export");
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
