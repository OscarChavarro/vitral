package application.gui;

import java.util.ArrayList;
import java.util.function.Predicate;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.gui.KeyEvent;
import vsdk.toolkit.gui.MouseEvent;
import vsdk.toolkit.gui.gizmo.RotateGizmo;
import vsdk.toolkit.gui.gizmo.ScaleGizmo;
import vsdk.toolkit.gui.gizmo.TranslateGizmo;
import vsdk.toolkit.gui.gizmo.TranslateGizmoInteractionTechnique;
import vsdk.toolkit.gui.viewport.Viewport;
import vsdk.toolkit.gui.viewport.ViewportSet;

import application.framework.Scene;
import application.model.ApplicationModel;
import application.model.DrawingArea;
import application.model.InteractionMode;
import application.model.SceneSelectionEditor;
import framework.gui.ViewportInteractionTechniques;
import framework.gui.ViewportSetInteractionTechniques;

/**
Mouse and keyboard interaction techniques of the drawing area of the editor:
camera control, selection of things, gizmo manipulation, mode selection and
global commands. It processes only vitral events, so callers must convert
events from the GUI technology in use (i.e. with `AwtSystem`) before calling
it. Anything the user must see or the GUI technology must do (pointer shape,
messages, dialogs) is requested through the `DrawingAreaInteractionListener`.

Mouse events must have coordinates in canvas pixels (see `DrawingArea`).

Keyboard commands: `c`, `q`, `w`, `e`, `r` select the camera, selection,
translation, rotation and scale modes; `LEFT` / `RIGHT` select things
sequentially; `=` / `-` change the size of the translation gizmo (in
translation mode with a selection `-` belongs to the input gizmo); in
translation mode, digits, `-`, `.`, `TAB` and `BACKSPACE` (and `ENTER`, `ESC`
while editing) edit the numeric boxes of the gizmo (see `InputGizmo`); `DELETE`
removes the selected things; `F10` requests a raytraced image; `T` / `B`
toggle a sample texture / bump map on the selected body; `h` shows the object
selector; `ESC` closes the application. Commands of the visual debug ray and
of the viewport set are processed by their own techniques.
*/
public class DrawingAreaInteractionTechniques
{
    private final DrawingArea drawingArea;
    private final ApplicationModel model;
    private final Scene scene;
    private final ViewportSet viewportSet;
    private final SceneSelectionEditor selectionEditor;
    private final ViewportInteractionTechniques interactionTechniques;
    private final ViewportSetInteractionTechniques viewportSetTechniques;
    private final VisualRayDebugController rayDebugController;
    private final SelectedBodyMapToggles mapToggles;
    private final TranslateGizmo translationGizmo;
    private final RotateGizmo rotateGizmo;
    private final ScaleGizmo scaleGizmo;
    private final DrawingAreaInteractionListener listener;

    private RendererConfiguration qualitySelection;

    /**
    @param model application model, providing the scene to interact with
    @param listener who presents the requests derived from interaction
    */
    public DrawingAreaInteractionTechniques(ApplicationModel model,
                                            DrawingAreaInteractionListener listener)
    {
        this.model = model;
        this.drawingArea = model.getDrawingArea();
        this.viewportSet = drawingArea.getViewportSet();
        this.scene = model.getScene();
        this.listener = listener;

        selectionEditor = new SceneSelectionEditor(scene);
        qualitySelection = scene.qualityTemplate;
        interactionTechniques = new ViewportInteractionTechniques(scene.camera, qualitySelection);
        viewportSetTechniques = new ViewportSetInteractionTechniques(viewportSet);
        rayDebugController = new VisualRayDebugController(model);
        mapToggles = new SelectedBodyMapToggles();
        translationGizmo = interactionTechniques.getTranslationGizmo();
        rotateGizmo = interactionTechniques.getRotateGizmo();
        scaleGizmo = interactionTechniques.getScaleGizmo();
    }

    public ViewportSetInteractionTechniques getViewportSetTechniques()
    {
        return viewportSetTechniques;
    }

    public TranslateGizmo getTranslationGizmo()
    {
        return translationGizmo;
    }

    public RotateGizmo getRotateGizmo()
    {
        return rotateGizmo;
    }

    public ScaleGizmo getScaleGizmo()
    {
        return scaleGizmo;
    }

    /**
    @param enabled true if the caller is able to place the pointer when the
    translation gizmo technique requests it (see
    `DrawingAreaInteractionListener.cursorWarpRequested`)
    */
    public void setCursorWrapEnabled(boolean enabled)
    {
        interactionTechniques.setTranslationCursorWrapEnabled(enabled);
    }

    /**
    Makes the camera controllers work over the camera and configuration of
    the given viewport. Viewport selection is processed by the viewport set
    interaction techniques.
    @param viewport the viewport to work over, or null for none
    */
    public void activateViewport(Viewport viewport)
    {
        if ( viewport == null ) {
            return;
        }

        interactionTechniques.setCamera(viewport.getActiveCamera());
        interactionTechniques.setRendererConfiguration(viewport.getRendererConfiguration());
        qualitySelection = viewport.getRendererConfiguration();
    }

    //= Pointer shape =====================================================

    private PointerCursor getModeCursor()
    {
        InteractionMode mode = drawingArea.getInteractionMode();

        if ( mode == InteractionMode.CAMERA ) {
            return PointerCursor.CAMERA_ROTATE;
        }
        if ( mode == InteractionMode.TRANSLATE ) {
            return PointerCursor.CAMERA_TRANSLATE;
        }
        return PointerCursor.SELECT;
    }

    /**
    Requests the pointer shape for the pointer position: a normal pointer over
    the title of a viewport (feedback that it can be clicked), the given one
    elsewhere.
    */
    private void requestCursorForPointer(MouseEvent event, PointerCursor cursor)
    {
        PointerCursor wanted = cursor;

        if ( viewportSetTechniques.isPointerOverTitle(drawingArea.toSurfaceEvent(event)) ) {
            wanted = PointerCursor.VIEWPORT_TITLE;
        }
        listener.cursorRequested(wanted);
    }

    /**
    Requests the pointer shape for the current interaction mode, at the
    position of the event.
    @param event pointer position
    */
    public void updateModeCursor(MouseEvent event)
    {
        requestCursorForPointer(event, getModeCursor());
    }

    //= Mouse =============================================================

    private Viewport getViewportAtPointer(MouseEvent event)
    {
        return viewportSetTechniques.findViewportAt(drawingArea.toSurfaceEvent(event));
    }

    /**
    A translation gesture (drag of the gizmo) belongs to the viewport where it
    started, even if the cursor goes over another one.
    @return the viewport where the gesture in course started, or the one under
    the pointer if there is no gesture
    */
    private Viewport getInteractionViewportAtPointer(MouseEvent event)
    {
        Viewport dragViewport = interactionTechniques.getTranslationDragViewport();

        if ( dragViewport != null ) {
            return dragViewport;
        }
        return getViewportAtPointer(event);
    }

    private boolean isTranslationGestureConfined()
    {
        return interactionTechniques.getTranslationDragViewport() != null;
    }

    private MouseEvent toViewportEvent(MouseEvent event, Viewport viewport)
    {
        return viewportSetTechniques.toViewportEvent(drawingArea.toSurfaceEvent(event), viewport);
    }

    /**
    Places the pointer as requested by the translation technique when the
    cursor leaves the viewport of the gesture (infinite drag).
    @param viewport viewport of the gesture
    */
    private void wrapCursorIfRequested(Viewport viewport)
    {
        TranslateGizmoInteractionTechnique.CursorWarp warp =
            interactionTechniques.consumeTranslationCursorWarp();

        if ( warp == null || viewport == null ) {
            return;
        }
        listener.cursorWarpRequested(
            viewportSet.toSetX(viewport, warp.x()),
            viewportSet.toSetY(viewport, warp.y()));
    }

    /**
    Places the translation gizmo over the selection centroid, seen from the
    viewport, and lets the technique process the event. If the gizmo moves the
    selection, it is moved with it.
    @return false if there is no viewport to process the event
    */
    private boolean processTranslationEvent(MouseEvent event,
                                            Viewport viewport,
                                            Vector3Dd centroid,
                                            Predicate<MouseEvent> technique)
    {
        if ( viewport == null ) {
            return false;
        }
        translationGizmo.setCamera(viewport.getActiveCamera());
        translationGizmo.setTransformationMatrix(
            SceneSelectionEditor.createTranslationGizmoMatrix(centroid));
        if ( technique.test(toViewportEvent(event, viewport)) ) {
            selectionEditor.applyTranslationToSelectedObjects(centroid,
                translationGizmo.getPosition());
            listener.repaintRequested();
        }
        return true;
    }

    /**
    The pointer entered the drawing area.
    @param event
    */
    public void processMouseEnteredEvent(MouseEvent event)
    {
        // WARNING / TODO
        // There should be a cameraController.getFutureAction(e) that calculates
        // the proper icon for display ... here an Aquynza operation is
        // assumed and hard-coded
        updateModeCursor(event);
    }

    public void processMousePressedEvent(MouseEvent event)
    {
        Viewport mouseView = getViewportAtPointer(event);
        InteractionMode mode = drawingArea.getInteractionMode();

        if ( mouseView == null ) {
            return;
        }
        viewportSetTechniques.processMousePressedEvent(drawingArea.toSurfaceEvent(event));
        activateViewport(mouseView);

        //-----------------------------------------------------------------
        // WARNING / TODO
        // There should be a cameraController.getFutureAction(e) that calculates
        // the proper icon for display ... here an Aquynza operation is
        // assumed and hard-coded
        int m = event.getModifiers();
        PointerCursor cursor;

        if ( mode == InteractionMode.CAMERA &&
             (m & MouseEvent.BUTTON1_DOWN_MASK) != 0 ) {
            cursor = PointerCursor.CAMERA_ROTATE;
        }
        else if ( mode == InteractionMode.CAMERA &&
                  (m & MouseEvent.BUTTON2_DOWN_MASK) != 0 ) {
            cursor = PointerCursor.CAMERA_TRANSLATE;
        }
        else if ( mode == InteractionMode.CAMERA &&
                  (m & MouseEvent.BUTTON3_DOWN_MASK) != 0 ) {
            cursor = PointerCursor.CAMERA_ADVANCE;
        }
        else {
            cursor = getModeCursor();
        }
        requestCursorForPointer(event, cursor);

        //-----------------------------------------------------------------
        if ( mode == InteractionMode.CAMERA &&
             interactionTechniques.processCameraMousePressedEvent(event) ) {
        }
        else if ( mode != InteractionMode.CAMERA ) {
            boolean composite = (m & MouseEvent.CTRL_DOWN_MASK) != 0;
            MouseEvent viewportEvent = toViewportEvent(event, mouseView);

            scene.activeCamera = mouseView.getActiveCamera();

            // A press over a gizmo handle grabs the gizmo: the ray selection
            // is skipped, so the whole selected group is kept
            boolean gizmoGrabbed =
                mode == InteractionMode.TRANSLATE &&
                interactionTechniques.getTranslationTechnique().isActive() &&
                selectionEditor.computeSelectionCentroid() != null;

            if ( !gizmoGrabbed ) {
                // Numbers typed belong to the previous selection
                interactionTechniques.getTranslationInputGizmo().cancelEditing();
                model.setVisualDebugRay(scene.selectObjectWithMouse(
                    viewportEvent.getX(), viewportEvent.getY(), composite, model.getVisualDebugRay()));
            }

            Vector3Dd centroid = selectionEditor.computeSelectionCentroid();

            if ( centroid != null ) {
                translationGizmo.setCamera(mouseView.getActiveCamera());
                translationGizmo.setTransformationMatrix(
                    SceneSelectionEditor.createTranslationGizmoMatrix(centroid));
                if ( mode != InteractionMode.SELECT ) {
                    interactionTechniques.processTranslationMousePressedEvent(
                        viewportEvent, mouseView);
                }
            }

            reportObjectSelection();
        }
        listener.repaintRequested();
    }

    public void processMouseReleasedEvent(MouseEvent event)
    {
        Viewport mouseView = getInteractionViewportAtPointer(event);
        boolean confinedGesture = isTranslationGestureConfined();
        InteractionMode mode = drawingArea.getInteractionMode();

        if ( mouseView != null && !confinedGesture ) {
            viewportSetTechniques.processMouseReleasedEvent(drawingArea.toSurfaceEvent(event));
        }
        activateViewport(mouseView);

        // WARNING / TODO
        // There should be a cameraController.getFutureAction(e) that calculates
        // the proper icon for display ... here an Aquynza operation is
        // assumed and hard-coded

        Vector3Dd centroid = selectionEditor.computeSelectionCentroid();

        updateModeCursor(event);

        if ( mode == InteractionMode.CAMERA &&
             interactionTechniques.processCameraMouseReleasedEvent(event) ) {
            listener.repaintRequested();
        }
        else if ( mode == InteractionMode.TRANSLATE && centroid != null ) {
            processTranslationEvent(event, mouseView, centroid,
                interactionTechniques::processTranslationMouseReleasedEvent);
        }
    }

    public void processMouseClickedEvent(MouseEvent event)
    {
        Viewport mouseView = getViewportAtPointer(event);
        InteractionMode mode = drawingArea.getInteractionMode();

        if ( mouseView != null ) {
            viewportSetTechniques.processMouseClickedEvent(drawingArea.toSurfaceEvent(event));
        }
        activateViewport(mouseView);

        Vector3Dd centroid = selectionEditor.computeSelectionCentroid();

        if ( mode == InteractionMode.CAMERA &&
             interactionTechniques.processCameraMouseClickedEvent(event) ) {
            listener.repaintRequested();
        }
        else if ( mode == InteractionMode.TRANSLATE && centroid != null ) {
            processTranslationEvent(event, mouseView, centroid,
                interactionTechniques::processTranslationMouseClickedEvent);
        }
    }

    /**
    The pointer moved without buttons pressed.
    @param event
    */
    public void processMouseMovedEvent(MouseEvent event)
    {
        Viewport mouseView = getInteractionViewportAtPointer(event);
        boolean confinedGesture = isTranslationGestureConfined();
        InteractionMode mode = drawingArea.getInteractionMode();

        // Moved events can come while dragging the gizmo (i.e. when the cursor
        // is wrapped): the viewport of the gesture keeps on being the one used
        if ( mouseView != null && !confinedGesture ) {
            interactionTechniques.setCamera(mouseView.getActiveCamera());
        }

        //-----------------------------------------------------------------
        Vector3Dd centroid = selectionEditor.computeSelectionCentroid();

        if ( mode == InteractionMode.CAMERA &&
             interactionTechniques.processCameraMouseMovedEvent(event) ) {
            listener.repaintRequested();
        }
        else if ( mode == InteractionMode.TRANSLATE && centroid != null ) {
            processTranslationEvent(event, mouseView, centroid,
                interactionTechniques::processTranslationMouseMovedEvent);
        }
    }

    public void processMouseDraggedEvent(MouseEvent event)
    {
        Viewport mouseView = getInteractionViewportAtPointer(event);
        boolean confinedGesture = isTranslationGestureConfined();
        InteractionMode mode = drawingArea.getInteractionMode();

        // While dragging the gizmo, the pointer over other viewport does not
        // select it
        if ( mouseView != null && !confinedGesture ) {
            viewportSetTechniques.processMouseDraggedEvent(drawingArea.toSurfaceEvent(event));
        }
        activateViewport(mouseView);

        Vector3Dd centroid = selectionEditor.computeSelectionCentroid();

        if ( mode == InteractionMode.CAMERA &&
             interactionTechniques.processCameraMouseDraggedEvent(event) ) {
            listener.repaintRequested();
        }
        else if ( mode == InteractionMode.TRANSLATE && centroid != null ) {
            if ( processTranslationEvent(event, mouseView, centroid,
                     interactionTechniques::processTranslationMouseDraggedEvent) ) {
                wrapCursorIfRequested(mouseView);
            }
        }
    }

    /**
    WARNING: It is not working... check pending
    @param event
    */
    public void processMouseWheelEvent(MouseEvent event)
    {
        if ( drawingArea.getInteractionMode() == InteractionMode.CAMERA &&
             interactionTechniques.processCameraMouseWheelEvent(event) ) {
            listener.repaintRequested();
        }
    }

    //= Keyboard ==========================================================

    public void processKeyPressedEvent(KeyEvent event)
    {
        InteractionMode mode = drawingArea.getInteractionMode();

        if ( mode == InteractionMode.TRANSLATE &&
             processInputGizmoKeyPressedEvent(event) ) {
            listener.repaintRequested();
            return;
        }

        if ( mode == InteractionMode.CAMERA &&
             interactionTechniques.processCameraKeyPressedEvent(event) ) {
        }
        else {
            processModeKeyPressedEvent(mode, event);
        }

        processGlobalKeyPressedEvent(event);

        // Viewport set commands (selection, layout, per-viewport display)
        viewportSetTechniques.processKeyPressedEvent(event);

        processCharacterKeyPressedEvent(event);

        listener.cursorRequested(getModeCursor());
        listener.repaintRequested();
    }

    /**
    Lets the input gizmo of the translation gizmo use a key. If the user
    accepts what was typed, the selection is moved so the gizmo is exactly
    where the numbers say.
    @return true if the input gizmo used the key, so it must not be processed as any
    other command
    */
    private boolean processInputGizmoKeyPressedEvent(KeyEvent event)
    {
        Vector3Dd centroid = selectionEditor.computeSelectionCentroid();

        if ( centroid == null ) {
            return false;
        }
        translationGizmo.setTransformationMatrix(
            SceneSelectionEditor.createTranslationGizmoMatrix(centroid));
        if ( !interactionTechniques.isTranslationInputGizmoKey(event) ) {
            return false;
        }
        if ( interactionTechniques.processTranslationKeyPressedEvent(event) ) {
            selectionEditor.applyTranslationToSelectedObjects(centroid,
                translationGizmo.getPosition());
        }
        return true;
    }

    /**
    Keys with a meaning that depends on the interaction mode.
    */
    private void processModeKeyPressedEvent(InteractionMode mode, KeyEvent event)
    {
        SimpleBody body;

        switch ( mode ) {
          case SELECT:
            if ( event.unicode_id == KeyEvent.KEY_NONE ) {
                if ( event.keycode == KeyEvent.KEY_LEFT ) {
                    interactionTechniques.getTranslationInputGizmo().cancelEditing();
                    selectionEditor.selectPrevious();
                    reportObjectSelection();
                }
                else if ( event.keycode == KeyEvent.KEY_RIGHT ) {
                    interactionTechniques.getTranslationInputGizmo().cancelEditing();
                    selectionEditor.selectNext();
                    reportObjectSelection();
                }
            }
            break;
          case TRANSLATE:
            Vector3Dd centroid = selectionEditor.computeSelectionCentroid();

            if ( centroid != null ) {
                translationGizmo.setTransformationMatrix(
                    SceneSelectionEditor.createTranslationGizmoMatrix(centroid));
                if ( interactionTechniques.processTranslationKeyPressedEvent(event) ) {
                    selectionEditor.applyTranslationToSelectedObjects(centroid,
                        translationGizmo.getPosition());
                }
            }
            break;
          case ROTATE:
            body = selectionEditor.getFirstSelectedBody();
            if ( body != null ) {
                Matrix4x4d rotation = body.getRotation();

                rotateGizmo.setTransformationMatrix(rotation);
                if ( interactionTechniques.processRotateKeyPressedEvent(event) ) {
                    rotation = rotateGizmo.getTransformationMatrix();
                    body.setRotation(rotation);
                    body.setRotationInverse(new Matrix4x4d(rotation).invert());
                }
            }
            break;
          case SCALE:
            body = selectionEditor.getFirstSelectedBody();
            if ( body != null ) {
                Vector3Dd s = body.getScale();
                Matrix4x4d S = new Matrix4x4d();

                S = S.withVal(0, 0, s.x());
                S = S.withVal(1, 1, s.y());
                S = S.withVal(2, 2, s.z());
                scaleGizmo.setTransformationMatrix(S);
                if ( interactionTechniques.processScaleKeyPressedEvent(event) ) {
                    S = scaleGizmo.getTransformationMatrix();
                    body.setScale(new Vector3Dd(S.get(0, 0), S.get(1, 1), S.get(2, 2)));
                }
            }
            break;
          default:
            break;
        }
    }

    /**
    Keys with the same meaning in every interaction mode.
    */
    private void processGlobalKeyPressedEvent(KeyEvent event)
    {
        int apparentSize;

        switch ( event.keycode ) {
          case KeyEvent.KEY_ESC:
            listener.closeRequested();
            break;
          case KeyEvent.KEY_EQUALS:
            apparentSize = translationGizmo.getBaseApparentSizeInPixels();
            apparentSize += 10;
            if ( apparentSize > 300 ) apparentSize = 300;
            translationGizmo.setBaseApparentSizeInPixels(apparentSize);
            break;
          case KeyEvent.KEY_MINUS:
            apparentSize = translationGizmo.getBaseApparentSizeInPixels();
            apparentSize -= 20;
            if ( apparentSize < 20 ) apparentSize = 20;
            translationGizmo.setBaseApparentSizeInPixels(apparentSize);
            break;
          case KeyEvent.KEY_DELETE:
            interactionTechniques.getTranslationInputGizmo().cancelEditing();
            selectionEditor.deleteSelected();
            break;
          case KeyEvent.KEY_F10:
            listener.raytracingRequested();
            break;
          default:
            break;
        }

        if ( interactionTechniques.processQualityKeyPressedEvent(event) ) {
            System.out.println(qualitySelection);
        }
    }

    /**
    Keys with a character (letters, digits and symbols).
    */
    private void processCharacterKeyPressedEvent(KeyEvent event)
    {
        if ( event.unicode_id == KeyEvent.KEY_NONE ) {
            return;
        }

        // Visual debug ray control
        if ( rayDebugController.processKeyPressedEvent(event) ) {
            return;
        }

        SimpleBody body;

        switch ( event.unicode_id ) {
          case 'T':
            body = selectionEditor.getFirstSelectedBody();
            if ( body != null ) {
                mapToggles.toggleTexture(body);
            }
            break;
          case 'B':
            body = selectionEditor.getFirstSelectedBody();
            if ( body != null ) {
                mapToggles.toggleNormalMap(body);
            }
            break;
          case 'h':
            listener.selectorDialogRequested();
            printBodyNames();
            break;
          case 'c':
            switchMode(InteractionMode.CAMERA,
                "Camera mode interaction - drag mouse with different buttons over the scene to change current camera.");
            break;
          case 'q':
            switchMode(InteractionMode.SELECT,
                "Selection mode interaction - click mouse to select objects, LEFT/RIGHT arrow keys to select sequencialy.");
            break;
          case 'w':
            // Alt+w (maximize viewport) is a viewport set command
            if ( (event.modifierMask & KeyEvent.MASK_ALT) == 0 ) {
                switchMode(InteractionMode.TRANSLATE,
                    "Translation mode interaction - click mouse to select objects, X, Y, Z keys and gizmo to move it.");
            }
            break;
          case 'e':
            switchMode(InteractionMode.ROTATE,
                "Rotation mode interaction - click mouse to select objects, X, Y, Z keys and gizmo to rotate it.");
            break;
          case 'r':
            switchMode(InteractionMode.SCALE,
                "Scale mode interaction - click mouse to select objects, X, Y, Z/ARROWS keys and gizmo to scale it.");
            break;
          default:
            break;
        }
    }

    private void switchMode(InteractionMode mode, String message)
    {
        listener.statusMessageRequested(message);
        interactionTechniques.getTranslationInputGizmo().cancelEditing();
        drawingArea.switchInteractionMode(mode);
    }

    private void printBodyNames()
    {
        ArrayList<SimpleBody> bodies = scene.scene.getSimpleBodies();
        String name;
        int i;

        for ( i = 0; i < bodies.size(); i++ ) {
            System.out.println("Consultando cosa " + i + ":");
            name = bodies.get(i).getName();
            if ( name == null || name.equals("") ) {
                name = "Not named object";
            }
            System.out.println("Object: " + name);
        }
    }

    /**
    @param event key released event
    */
    public void processKeyReleasedEvent(KeyEvent event)
    {
        if ( drawingArea.getInteractionMode() == InteractionMode.CAMERA &&
             interactionTechniques.processCameraKeyReleasedEvent(event) ) {
            listener.repaintRequested();
        }
    }

    //= Selection feedback ================================================

    private void reportObjectSelection()
    {
        listener.statusMessageRequested(selectionEditor.describeSelection());
        listener.selectionChanged();
    }
}
