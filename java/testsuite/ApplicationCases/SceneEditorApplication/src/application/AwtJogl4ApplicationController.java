package application;

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

import gui.awt.AwtDrawingAreaController;
import gui.awt.AwtDrawingAreaFeedback;
import gui.DrawingAreaInteractionTechniques;
import model.ApplicationModel;
import model.DrawingArea;
import model.RenderTechnology;
import render.awt.AwtDrawingAreaRenderer;
import render.awt.AwtViewportSetCanvas;
import render.jogl.Jogl4DrawingAreaRenderer;

/**
Composition root of the drawing area of the editor: it creates the canvas of
the render technology selected in the `DrawingArea` (an OpenGL canvas, or an
AWT canvas drawn only with 2D operations) and connects the drawing area model
with its renderer, its interaction techniques and the AWT adapters, and offers
the operations that need the canvas.
*/
public class AwtJogl4ApplicationController
{
    private final ApplicationModel model;
    private Component canvas;
    /// Technology of the current canvas
    private RenderTechnology canvasTechnology;
    private AwtDrawingAreaController awtController;
    private AwtDrawingAreaFeedback awtFeedback;

    public AwtJogl4ApplicationController(ApplicationModel model)
    {
        this.model = model;
    }

    private void createDrawingArea(AwtJogl4SceneEditorApplication application)
    {
        DrawingArea drawingArea = model.getDrawingArea();
        DrawingAreaInteractionTechniques techniques;

        canvasTechnology = drawingArea.getRenderTechnology();
        if ( canvasTechnology == RenderTechnology.AWT ) {
            techniques = createAwtCanvas(application);
        }
        else {
            techniques = createJogl4Canvas(application);
        }
        canvas.setMinimumSize(new Dimension(8, 8));

        //-----------------------------------------------------------------
        awtController = new AwtDrawingAreaController(canvas, drawingArea,
            techniques, awtFeedback);
    }

    /**
    Creates the interaction techniques, reporting to the feedback of the
    current canvas.
    */
    private DrawingAreaInteractionTechniques createTechniques(
        AwtJogl4SceneEditorApplication application)
    {
        awtFeedback = new AwtDrawingAreaFeedback(application, canvas);
        DrawingAreaInteractionTechniques techniques =
            new DrawingAreaInteractionTechniques(model, awtFeedback);
        // While dragging the gizmo, the cursor wraps around its viewport
        techniques.setCursorWrapEnabled(awtFeedback.isCursorWarpAvailable());
        return techniques;
    }

    private DrawingAreaInteractionTechniques createAwtCanvas(
        AwtJogl4SceneEditorApplication application)
    {
        AwtViewportSetCanvas awtCanvas = new AwtViewportSetCanvas();
        canvas = awtCanvas;
        DrawingAreaInteractionTechniques techniques = createTechniques(application);

        awtCanvas.setRenderer(new AwtDrawingAreaRenderer(model, awtFeedback,
            techniques::activateViewport));
        return techniques;
    }

    private DrawingAreaInteractionTechniques createJogl4Canvas(
        AwtJogl4SceneEditorApplication application)
    {
        GLProfile profile = GLProfile.get(GLProfile.GL4);
        GLCapabilities capabilities = new GLCapabilities(profile);
        capabilities.setDepthBits(32);
        GLCanvas glCanvas = new GLCanvas(capabilities);
        canvas = glCanvas;
        DrawingAreaInteractionTechniques techniques = createTechniques(application);

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
        glCanvas.addGLEventListener(renderer);
        return techniques;
    }

    private void ensureDrawingArea(AwtJogl4SceneEditorApplication application)
    {
        if ( canvas != null &&
             canvasTechnology != model.getDrawingArea().getRenderTechnology() ) {
            // The previous canvas leaves with the window that contains it
            canvas = null;
            awtController = null;
            awtFeedback = null;
        }
        if ( canvas == null ) {
            createDrawingArea(application);
        }
    }

    /**
    Draws a frame now, so the requests of the frame (i.e. exports to files)
    are done on return.
    */
    private void displayNow()
    {
        if ( canvas instanceof GLCanvas ) {
            ((GLCanvas)canvas).display();
        }
        else if ( canvas instanceof AwtViewportSetCanvas ) {
            ((AwtViewportSetCanvas)canvas).display();
        }
    }

    /**
    @param application the application, needed to create the drawing area the
    first time
    @return the component presenting the drawing area
    */
    public Component getCanvas(AwtJogl4SceneEditorApplication application)
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
        displayNow();
    }

    /**
    Exports the selected viewport to a JPG file, in the next frame.
    @param file destination file
    */
    public void exportViewportJpg(File file)
    {
        model.getDrawingArea().requestViewportExport(file, true);
        displayNow();
    }

    /**
    Exports the whole viewport set area to a JPG file, in the next frame.
    @param file destination file
    */
    public void exportWorkspaceJpg(File file)
    {
        model.getDrawingArea().requestWorkspaceExport(file);
        displayNow();
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
    Delivers a synthetic key press to the canvas, optionally with the CTRL
    key down (see `AwtDrawingAreaController.injectKeyEvent`).
    @param key a single character or a key name
    @param shift true to press it with the SHIFT key down
    @param ctrl true to press it with the CTRL key down
    */
    public void injectKeyEvent(String key, boolean shift, boolean ctrl)
    {
        awtController.injectKeyEvent(key, shift, ctrl);
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
