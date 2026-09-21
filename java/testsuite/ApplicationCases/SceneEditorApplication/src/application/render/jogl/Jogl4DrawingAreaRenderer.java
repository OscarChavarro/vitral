package application.render.jogl;

import java.util.function.Consumer;

// JOGL classes
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

// VSDK classes
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.gui.gizmo.RotateGizmo;
import vsdk.toolkit.gui.gizmo.ScaleGizmo;
import vsdk.toolkit.gui.gizmo.TranslateGizmo;
import vsdk.toolkit.gui.viewport.Viewport;
import vsdk.toolkit.gui.viewport.ViewportSet;
import vsdk.toolkit.render.jogl.Jogl4BackgroundRenderer;
import vsdk.toolkit.render.jogl.Jogl4ImageRenderer;
import vsdk.toolkit.render.jogl.Jogl4Renderer;
import vsdk.toolkit.render.jogl.gizmo.Jogl4RotateGizmoRenderer;
import vsdk.toolkit.render.jogl.gizmo.Jogl4ScaleGizmoRenderer;
import vsdk.toolkit.render.jogl.gizmo.Jogl4TranslateGizmoRenderer;
import vsdk.toolkit.render.jogl.viewport.Jogl4LabelImageProvider;
import vsdk.toolkit.render.jogl.viewport.Jogl4ViewportWindow;
import vsdk.toolkit.render.jogl.viewport.JoglViewportSetRenderer;

// Application classes
import application.framework.Scene;
import application.model.ApplicationModel;
import application.model.DrawingArea;
import application.model.InteractionMode;
import application.model.SceneSelectionEditor;

/**
Draws the scene in the viewports of a `DrawingArea`, using OpenGL 4: the
viewports, the manipulation gizmos and the visual debug ray. It does not
depend on the GUI technology hosting the drawing surface (see
`Jogl4DrawingAreaHost`).
*/
public class Jogl4DrawingAreaRenderer implements GLEventListener
{
    private final Scene theScene;
    private final ApplicationModel model;
    private final DrawingArea drawingArea;
    private final ViewportSet viewportSet;
    private final Jogl4DrawingAreaHost host;
    private final SceneSelectionEditor selectionEditor;

    private final TranslateGizmo translationGizmo;
    private final RotateGizmo rotateGizmo;
    private final ScaleGizmo scaleGizmo;
    private boolean translationGizmoDrawn;

    private final Jogl4VisualRayDebugRenderer rayDebugRenderer;
    private final Jogl4FrameCaptureService frameCapture;
    private final Jogl4ProjectedViewsDebugger projectedViewsDebugger;
    private final JoglViewportSetRenderer viewportSetRenderer;

    /**
    @param model application model
    @param host services from the GUI technology hosting the surface
    @param labelImageProvider creates the images for the texts of viewports
    @param selectedViewportListener notified with the viewport about to be
    drawn when it is the selected one (or the only one shown), so interaction
    techniques can work over its camera
    @param translationGizmo gizmo to draw in translation mode
    @param rotateGizmo gizmo to draw in rotation mode
    @param scaleGizmo gizmo to draw in scale mode
    */
    public Jogl4DrawingAreaRenderer(ApplicationModel model,
                                    Jogl4DrawingAreaHost host,
                                    Jogl4LabelImageProvider labelImageProvider,
                                    Consumer<Viewport> selectedViewportListener,
                                    TranslateGizmo translationGizmo,
                                    RotateGizmo rotateGizmo,
                                    ScaleGizmo scaleGizmo)
    {
        this.model = model;
        this.theScene = model.getScene();
        this.drawingArea = model.getDrawingArea();
        this.viewportSet = drawingArea.getViewportSet();
        this.host = host;
        this.translationGizmo = translationGizmo;
        this.rotateGizmo = rotateGizmo;
        this.scaleGizmo = scaleGizmo;
        translationGizmoDrawn = false;

        selectionEditor = new SceneSelectionEditor(theScene);
        rayDebugRenderer = new Jogl4VisualRayDebugRenderer(model);
        frameCapture = new Jogl4FrameCaptureService(model, host);
        projectedViewsDebugger = new Jogl4ProjectedViewsDebugger(model, host);

        viewportSetRenderer = new JoglViewportSetRenderer(
            viewportSet,
            labelImageProvider,
            new JoglViewportSetRenderer.ViewRenderer() {
                @Override
                public void configureView(Jogl4ViewportWindow view) {
                    selectedViewportListener.accept(view.getViewport());
                }

                @Override
                public void drawView(GL4 gl, Jogl4ViewportWindow view) {
                    theScene.activeCamera = view.getCamera();
                    theScene.qualityTemplate = view.getRendererConfiguration();
                    Jogl4DrawingAreaRenderer.this.drawView(gl, view);
                }
            });
    }

    private void drawGizmos(GL4 gl)
    {
        // Pending: Turn off scene light and turn on gizmo specific lighting
        translationGizmoDrawn = false;

        translationGizmo.setCamera(theScene.activeCamera);
        // Size and line width follow the resolution of the screen
        translationGizmo.applyScale(viewportSet.getElementScaler());

        gl.glClear(GL.GL_DEPTH_BUFFER_BIT);

        SimpleBody selectedBody = selectionEditor.getFirstSelectedBody();
        InteractionMode mode = drawingArea.getInteractionMode();

        if ( drawingArea.shouldDrawTranslationGizmo() ) {
            Vector3Dd centroid = selectionEditor.computeSelectionCentroid();

            if ( centroid != null ) {
                translationGizmo.setTransformationMatrix(
                    SceneSelectionEditor.createTranslationGizmoMatrix(centroid));

                Jogl4TranslateGizmoRenderer.draw(gl, translationGizmo, theScene.activeCamera);
                translationGizmoDrawn = true;
            }
        }
        else if ( mode == InteractionMode.ROTATE ) {
            if ( selectedBody != null ) {
                rotateGizmo.setTransformationMatrix(selectedBody.getRotation());
                Jogl4RotateGizmoRenderer.draw(gl, rotateGizmo,
                    selectedBody.getPosition(), theScene.activeCamera);
            }
        }
        else if ( mode == InteractionMode.SCALE && selectedBody != null ) {
            scaleGizmo.setTransformationMatrix(selectedBody.getRotation());
            Jogl4ScaleGizmoRenderer.draw(gl, scaleGizmo,
                selectedBody.getPosition(), theScene.activeCamera);
        }
        gl.glEnable(GL.GL_DEPTH_TEST);
    }

    private void drawView(GL4 gl, Jogl4ViewportWindow view)
    {
        if ( !view.isActive() ) {
            return;
        }

        if ( view.getRenderMode() == Jogl4ViewportWindow.RENDER_MODE_ZBUFFER ) {
            Jogl4SceneRenderer.draw(gl, theScene, host.getModifyPanel());
        }
        else {
            theScene.activateSelectedBackground();
            Jogl4BackgroundRenderer.draw(gl,
                theScene.scene.getBackgrounds().get(theScene.scene.getActiveBackgroundIndex()));
            model.setRaytracedImageWidth(view.getViewportSizeX());
            model.setRaytracedImageHeight(view.getViewportSizeY());
            host.raytraceImage();
            // The ray-traced image changes on each frame: its texture is
            // created again
            Jogl4ImageRenderer.unload(gl, model.getRaytracedImage());
            Jogl4ImageRenderer.draw(gl, model.getRaytracedImage());
        }

        //-----------------------------------------------------------------
        rayDebugRenderer.draw(gl);

        view.drawGrid(gl);

        //-----------------------------------------------------------------
        // Note that gizmo information will not be reported, as they damage
        // the z-buffer...
        frameCapture.copyZBufferIfNeeded(gl);

        // Must be the last to draw
        drawGizmos(gl);

        frameCapture.copyColorBufferIfNeeded(gl, view.isSelected());

        view.drawReferenceBase(gl);
        if ( translationGizmoDrawn ) {
            view.drawLabelsForTranslateGizmo(gl, translationGizmo);
        }
    }

    @Override
    public void display(GLAutoDrawable drawable)
    {
        GL4 gl = drawable.getGL().getGL4();

        // Text size and viewport state follow the component showing the surface
        host.beforeFrame();
        projectedViewsDebugger.debugIfNeeded(gl);
        drawingArea.updateSurfaceSize(drawable.getSurfaceWidth(),
            drawable.getSurfaceHeight());

        //-----------------------------------------------------------------
        gl.glViewport(0, 0, viewportSet.getSizeXInPixels(), viewportSet.getSizeYInPixels());
        gl.glClearColor(0.77f, 0.77f, 0.77f, 1.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL.GL_DEPTH_TEST);

        viewportSetRenderer.draw(gl, host.isFullScreenGuiMode());

        frameCapture.exportPendingFrame(gl, viewportSetRenderer);
    }

    @Override
    public void init(GLAutoDrawable drawable)
    {
        // A new OpenGL context (i.e. the GUI was rebuilt when changing the
        // language) does not have the textures created by the previous one
        viewportSetRenderer.invalidateGlResources();
    }

    @Override
    public void dispose(GLAutoDrawable drawable)
    {
        viewportSetRenderer.disposeGlResources(drawable.getGL().getGL4());
        Jogl4Renderer.disposeAll(drawable.getGL().getGL4());
    }

    @Override
    public void reshape(GLAutoDrawable drawable,
                        int x,
                        int y,
                        int width,
                        int height)
    {
        host.beforeFrame();
        drawingArea.updateSurfaceSize(width, height);
    }
}
