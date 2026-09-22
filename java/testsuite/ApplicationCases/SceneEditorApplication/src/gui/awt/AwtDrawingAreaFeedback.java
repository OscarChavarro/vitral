package gui.awt;

// AWT/Swing classes
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.JLabel;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.logging.Logger;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.media.RGBImageUncompressed;

// Application classes
import model.DrawingArea;
import model.selection.SceneSelectionEditor;
import render.BodyEditFeedbackProvider;
import render.DrawingAreaHost;
import gui.DrawingAreaInteractionListener;
import gui.PointerCursor;

/**
Presents in the AWT/Swing GUI what the drawing area asks for: pointer shapes,
status messages, image and selector windows, the modify panel, and the
application level commands (raytracing, close, full screen). It also
supplies the rendering classes with the services they need from the GUI
technology hosting the drawing surface.
*/
public class AwtDrawingAreaFeedback implements DrawingAreaInteractionListener,
    DrawingAreaHost
{
    private final AwtApplicationHost application;
    private final DrawingArea drawingArea;
    private final Component canvas;
    private final AwtCursorWarper cursorWarper = new AwtCursorWarper();
    private final AwtViewportElementScaler elementScaler;
    private final SceneSelectionEditor selectionEditor;

    private final Map<PointerCursor, Cursor> cursors;

    /**
    @param application the application whose GUI is used
    @param canvas the component presenting the drawing surface
    */
    public AwtDrawingAreaFeedback(AwtApplicationHost application,
                                  Component canvas)
    {
        this.application = application;
        this.drawingArea = application.getApplicationModel().getDrawingArea();
        this.canvas = canvas;
        this.selectionEditor = new SceneSelectionEditor(
            application.getApplicationModel().getScene());

        cursors = new EnumMap<>(PointerCursor.class);
        for ( PointerCursor cursor : PointerCursor.values() ) {
            cursors.put(cursor, createCursor(cursor));
        }

        elementScaler = new AwtViewportElementScaler(
            drawingArea.getViewportSet().getElementScaler());
        elementScaler.updateFromDefaultScreen();
    }

    /**
    Loads the image of a pointer (with transparency, hot spot at its center).
    @param cursor the pointer to present
    @return the custom cursor, or the default cursor if the pointer has no
    image or it cannot be read
    */
    private Cursor createCursor(PointerCursor cursor)
    {
        String filename = cursor.getImagePath();

        if ( filename == null ) {
            return new Cursor(Cursor.DEFAULT_CURSOR);
        }
        try {
            BufferedImage image = ImageIO.read(new File(filename));

            if ( image != null ) {
                Point hotSpot = new Point(image.getWidth() / 2, image.getHeight() / 2);

                return Toolkit.getDefaultToolkit().createCustomCursor(image, hotSpot,
                    cursor.getDisplayName());
            }
        }
        catch ( IOException | IllegalArgumentException e ) {
            Logger.reportMessage(this, VSDK.WARNING, "createCursor",
                "Cannot load cursor image " + filename + ": " + e.getMessage());
            return new Cursor(Cursor.DEFAULT_CURSOR);
        }
        Logger.reportMessage(this, VSDK.WARNING, "createCursor",
            "Unsupported cursor image format: " + filename);
        return new Cursor(Cursor.DEFAULT_CURSOR);
    }

    /**
    @return true if the pointer can be placed by the application (needed for
    the infinite drag of gizmos)
    */
    public boolean isCursorWarpAvailable()
    {
        return cursorWarper.isAvailable();
    }

    private void setStatusText(String message)
    {
        JLabel statusMessage = application.getAwtModel().getStatusMessage();

        if ( statusMessage != null ) {
            statusMessage.setText(message);
        }
    }

    /**
    Notifies the modify panel of the target it must edit: the first selected
    body, or none.
    */
    public void reportTargetToModifyPanel()
    {
        AwtApplicationModel awtModel = application.getAwtModel();
        SimpleBody target = null;

        if ( application.getApplicationModel().getGuiState().isModifyPanelSelected() ) {
            target = selectionEditor.getFirstSelectedBody();
        }
        if ( target != null ) {
            awtModel.getModifyPanel().notifyTargetBeginEdit(target);
        }
        else {
            awtModel.getModifyPanel().notifyTargetEndEdit();
        }
    }

    //= DrawingAreaInteractionListener ====================================

    @Override
    public void cursorRequested(PointerCursor cursor)
    {
        Cursor wanted = cursors.get(cursor);

        if ( canvas.getCursor() != wanted ) {
            canvas.setCursor(wanted);
        }
    }

    @Override
    public void cursorWarpRequested(int surfaceX, int surfaceY)
    {
        cursorWarper.warp(canvas, drawingArea.scaleXToCanvas(surfaceX),
            drawingArea.scaleYToCanvas(surfaceY));
    }

    @Override
    public void repaintRequested()
    {
        canvas.repaint();
    }

    @Override
    public void statusMessageRequested(String message)
    {
        setStatusText(message);
    }

    @Override
    public void selectionChanged()
    {
        reportTargetToModifyPanel();
    }

    @Override
    public void raytracingRequested()
    {
        AwtApplicationModel awtModel = application.getAwtModel();

        setStatusText(application.getApplicationModel().getI18nContext().getMessage("IDM_COMPUTING_RAYTRACING"));
        application.doRaytracingImage();
        showImage(application.getApplicationModel().getRaytracedImage());
    }

    @Override
    public void selectorDialogRequested()
    {
        AwtApplicationModel awtModel = application.getAwtModel();

        if ( awtModel.getSelectorDialog() == null ) {
            awtModel.setSelectorDialog(new AwtSelectorDialog());
        }
        awtModel.getSelectorDialog().setVisible(true);
        awtModel.getSelectorDialog().repaint();
    }

    @Override
    public void closeRequested()
    {
        application.closeApplication();
    }

    @Override
    public void fullScreenGuiToggleRequested()
    {
        application.getApplicationModel().getGuiState().toggleFullScreenGuiMode();
        application.destroyGUI();
        application.createGUI();
    }

    //= DrawingAreaHost ===================================================

    @Override
    public void beforeFrame()
    {
        // Text size follows the resolution of the screen showing the canvas
        elementScaler.updateFromComponent(canvas);
        drawingArea.updateCanvasSize(canvas.getWidth(), canvas.getHeight());
    }

    @Override
    public boolean isFullScreenGuiMode()
    {
        return application.getApplicationModel().getGuiState().isFullScreenGuiMode();
    }

    @Override
    public BodyEditFeedbackProvider getBodyEditFeedbackProvider()
    {
        return application.getAwtModel().getModifyPanel();
    }

    @Override
    public void raytraceImage()
    {
        application.doViewportRaytracingImage();
    }

    @Override
    public void showImage(RGBImageUncompressed image)
    {
        AwtApplicationModel awtModel = application.getAwtModel();

        if ( awtModel.getImageControlWindow() == null ) {
            awtModel.setImageControlWindow(new AwtImageControlWindow(
                image,
                application.getApplicationModel().getI18nContext(),
                awtModel.getExecutorPanel()));
        }
        else {
            awtModel.getImageControlWindow().setImage(image);
        }
        awtModel.getImageControlWindow().redrawImage();
    }

    @Override
    public void showStatusMessage(String message)
    {
        setStatusText(message);
    }
}
