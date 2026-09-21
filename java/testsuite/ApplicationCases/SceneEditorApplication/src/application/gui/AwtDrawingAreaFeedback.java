package application.gui;

// AWT/Swing classes
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import javax.swing.JLabel;

// VSDK classes
import vsdk.toolkit.media.RGBImageUncompressed;

// Application classes
import application.SceneEditorApplication;
import application.model.DrawingArea;
import application.render.jogl.Jogl4DrawingAreaHost;
import framework.gui.AwtCursorWarper;
import framework.gui.AwtViewportElementScaler;

/**
Presents in the AWT/Swing GUI what the drawing area asks for: pointer shapes,
status messages, image and selector windows, the modify panel, and the
application level commands (raytracing, close, full screen). It also
supplies the rendering classes with the services they need from the GUI
technology hosting the drawing surface.
*/
public class AwtDrawingAreaFeedback implements DrawingAreaInteractionListener,
    Jogl4DrawingAreaHost
{
    private static final String CURSORS_FOLDER = "./etc/cursors/";

    private final SceneEditorApplication application;
    private final DrawingArea drawingArea;
    private final Component canvas;
    private final AwtCursorWarper cursorWarper = new AwtCursorWarper();
    private final AwtViewportElementScaler elementScaler;

    private final Cursor camrotateCursor;
    private final Cursor camtranslateCursor;
    private final Cursor camadvanceCursor;
    private final Cursor selectCursor;
    private final Cursor titleCursor;

    /**
    @param application the application whose GUI is used
    @param canvas the component presenting the drawing surface
    */
    public AwtDrawingAreaFeedback(SceneEditorApplication application,
                                  Component canvas)
    {
        this.application = application;
        this.drawingArea = application.getApplicationModel().getDrawingArea();
        this.canvas = canvas;

        camrotateCursor = createCursor("cursor_camrotate.gif", "CameraRotation");
        camtranslateCursor = createCursor("cursor_camtranslate.gif", "CameraTranslation");
        camadvanceCursor = createCursor("cursor_camadvance.gif", "CameraAdvance");
        selectCursor = new Cursor(Cursor.DEFAULT_CURSOR);
        titleCursor = new Cursor(Cursor.DEFAULT_CURSOR);

        elementScaler = new AwtViewportElementScaler(
            drawingArea.getViewportSet().getElementScaler());
        elementScaler.updateFromDefaultScreen();
    }

    private Cursor createCursor(String filename, String name)
    {
        Toolkit awtToolkit = Toolkit.getDefaultToolkit();
        java.awt.Image image = awtToolkit.getImage(CURSORS_FOLDER + filename);

        return awtToolkit.createCustomCursor(image, new Point(16, 16), name);
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
        int firstThingSelected =
            application.getApplicationModel().getScene().selectedThings.firstSelected();

        if ( awtModel.isModifyPanelSelected() && firstThingSelected >= 0 ) {
            awtModel.getModifyPanel().notifyTargetBeginEdit(
                application.getApplicationModel().getScene().scene.getSimpleBodies().get(firstThingSelected)
            );
        }
        else {
            awtModel.getModifyPanel().notifyTargetEndEdit();
        }
    }

    //= DrawingAreaInteractionListener ====================================

    @Override
    public void cursorRequested(PointerCursor cursor)
    {
        Cursor wanted;

        switch ( cursor ) {
          case CAMERA_ROTATE:
            wanted = camrotateCursor;
            break;
          case CAMERA_TRANSLATE:
            wanted = camtranslateCursor;
            break;
          case CAMERA_ADVANCE:
            wanted = camadvanceCursor;
            break;
          case VIEWPORT_TITLE:
            wanted = titleCursor;
            break;
          default:
            wanted = selectCursor;
            break;
        }
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

        setStatusText(awtModel.getGui().getMessage("IDM_COMPUTING_RAYTRACING"));
        application.doRaytracingImage();
        showImage(application.getApplicationModel().getRaytracedImage());
    }

    @Override
    public void selectorDialogRequested()
    {
        AwtApplicationModel awtModel = application.getAwtModel();

        if ( awtModel.getSelectorDialog() == null ) {
            awtModel.setSelectorDialog(new SwingSelectorDialog());
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
        application.getAwtModel().toggleFullScreenGuiMode();
        application.destroyGUI();
        application.createGUI();
    }

    //= Jogl4DrawingAreaHost ==============================================

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
        return application.getAwtModel().isFullScreenGuiMode();
    }

    @Override
    public ModifyPanel getModifyPanel()
    {
        return application.getAwtModel().getModifyPanel();
    }

    @Override
    public void raytraceImage()
    {
        application.doRaytracingImage();
    }

    @Override
    public void showImage(RGBImageUncompressed image)
    {
        AwtApplicationModel awtModel = application.getAwtModel();

        if ( awtModel.getImageControlWindow() == null ) {
            awtModel.setImageControlWindow(new SwingImageControlWindow(
                image,
                awtModel.getGui(),
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
