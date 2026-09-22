package render.jogl;

import java.awt.Component;
import java.awt.Dimension;
import java.io.File;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.gui.viewport.Viewport;
import vsdk.toolkit.media.RGBAImageUncompressed;
import vsdk.toolkit.render.jogl.viewport.Jogl4LabelImageProvider;

import application.SceneEditorApplication;
import gui.AwtDrawingAreaController;
import gui.AwtDrawingAreaFeedback;
import gui.DrawingAreaInteractionTechniques;
import model.ApplicationModel;
import model.DrawingArea;

/**
Composition root of the drawing area of the editor: it creates the OpenGL
canvas and connects the drawing area model with its renderer, its interaction
techniques and the AWT adapters, and offers the operations that need the
canvas.
*/
public class Jogl4ApplicationController
{
    private final ApplicationModel model;
    private GLCanvas canvas;
    private AwtDrawingAreaController awtController;
    private AwtDrawingAreaFeedback awtFeedback;

    public Jogl4ApplicationController(ApplicationModel model)
    {
        this.model = model;
    }

    private void createDrawingArea(SceneEditorApplication application)
    {
        DrawingArea drawingArea = model.getDrawingArea();

        //-----------------------------------------------------------------
        GLProfile profile = GLProfile.get(GLProfile.GL4);
        GLCapabilities capabilities = new GLCapabilities(profile);
        capabilities.setDepthBits(32);
        canvas = new GLCanvas(capabilities);
        canvas.setMinimumSize(new Dimension(8, 8));

        //-----------------------------------------------------------------
        awtFeedback = new AwtDrawingAreaFeedback(application, canvas);
        DrawingAreaInteractionTechniques techniques =
            new DrawingAreaInteractionTechniques(model, awtFeedback);
        // While dragging the gizmo, the cursor wraps around its viewport
        techniques.setCursorWrapEnabled(awtFeedback.isCursorWarpAvailable());

        //-----------------------------------------------------------------
        Jogl4DrawingAreaRenderer renderer = new Jogl4DrawingAreaRenderer(
            model,
            awtFeedback,
            new Jogl4LabelImageProvider() {
                @Override
                public RGBAImageUncompressed createLabelImage(String text, ColorRgb color, int fontSize) {
                    return AwtSystem.calculateLabelImage(text, color, fontSize);
                }
            },
            techniques::activateViewport,
            techniques.getTranslationGizmo(),
            techniques.getRotateGizmo(),
            techniques.getScaleGizmo());
        canvas.addGLEventListener(renderer);

        //-----------------------------------------------------------------
        awtController = new AwtDrawingAreaController(canvas, drawingArea,
            techniques, awtFeedback);
    }

    private void ensureDrawingArea(SceneEditorApplication application)
    {
        if ( canvas == null ) {
            createDrawingArea(application);
        }
    }

    /**
    @param application the application, needed to create the drawing area the
    first time
    @return the component presenting the drawing area
    */
    public Component getCanvas(SceneEditorApplication application)
    {
        ensureDrawingArea(application);
        return canvas;
    }

    /**
    @return true if the canvas of the drawing area was already created
    */
    public boolean isDrawingAreaCreated()
    {
        return canvas != null;
    }

    public void requestFocusInWindow()
    {
        if ( canvas != null ) {
            canvas.requestFocusInWindow();
        }
    }

    public void repaint()
    {
        if ( canvas != null ) {
            canvas.repaint();
        }
    }

    /**
    Notifies the modify panel of the currently selected target.
    */
    public void reportTargetToModifyPanel()
    {
        if ( awtFeedback != null ) {
            awtFeedback.reportTargetToModifyPanel();
        }
    }

    /**
    Exports the selected viewport to a PNG file, in the next frame.
    @param file destination file
    */
    public void exportViewportPng(File file)
    {
        model.getDrawingArea().requestViewportExport(file, false);
        canvas.display();
    }

    /**
    Exports the selected viewport to a JPG file, in the next frame.
    @param file destination file
    */
    public void exportViewportJpg(File file)
    {
        model.getDrawingArea().requestViewportExport(file, true);
        canvas.display();
    }

    /**
    Exports the whole viewport set area to a JPG file, in the next frame.
    @param file destination file
    */
    public void exportWorkspaceJpg(File file)
    {
        model.getDrawingArea().requestWorkspaceExport(file);
        canvas.display();
    }

    /**
    Delivers a synthetic mouse event to the canvas (see
    `AwtDrawingAreaController.injectMouseEvent`).
    @param type one of "move", "press", "drag", "release"
    @param x canvas (AWT) x coordinate
    @param y canvas (AWT) y coordinate
    @param button AWT button number (1 = left)
    */
    public void injectMouseEvent(String type, int x, int y, int button)
    {
        awtController.injectMouseEvent(type, x, y, button);
    }

    /**
    Delivers a synthetic key press to the canvas (see
    `AwtDrawingAreaController.injectKeyEvent`).
    @param key a single character or a key name (see `AwtDrawingAreaController.injectKeyEvent`)
    @param shift true to press it with the SHIFT key down
    */
    public void injectKeyEvent(String key, boolean shift)
    {
        awtController.injectKeyEvent(key, shift);
    }

    /**
    Projects a point of the scene to canvas (AWT) pixel coordinates.
    @param viewport viewport whose camera is used
    @param point point in world coordinates
    @return {x, y} in canvas pixels, or null if the point is behind the camera
    */
    public double[] projectToCanvas(Viewport viewport, Vector3Dd point)
    {
        return awtController.projectToCanvas(viewport, point);
    }
}
