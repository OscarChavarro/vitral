package gui;

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

import model.Scene;
import model.ApplicationModel;
import model.DrawingArea;
import model.InteractionMode;
import model.selection.ScenePicker;
import model.selection.SceneSelectionEditor;
import gui.history.InteractionEditRecorder;
import gui.history.UndoRedoCommand;
import gui.history.UndoRedoInteractionTechnique;
import vsdk.toolkit.gui.viewport.ViewportInteractionTechniques;
import vsdk.toolkit.gui.viewport.ViewportSetInteractionTechniques;

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
translation, rotation and scale modes with a selection `-` belongs to the
input gizmo); in translation, rotation and scale modes, digits, `-`, `.`,
`TAB` and `BACKSPACE` (and `ENTER`, `ESC` while editing) edit the numeric
boxes of the gizmo (see `InputGizmo`), that show coordinates, angles in
degrees or scale factors; in rotation mode the mouse highlights (hover) and
chooses (click) the rings of the gizmo (see
`RotateGizmoInteractionTechnique`); `DELETE`
removes the selected things; `F10` requests a raytraced image; `T` / `B`
toggle a sample texture / bump map on the selected body; `h` shows the object
selector; `ESC` closes the application. Commands of the visual debug ray and
of the viewport set are processed by their own techniques.

Undo and redo (see `UndoRedoInteractionTechnique`): `Ctrl+Z` / `Ctrl+Y` over
the scene (creation, deletion and transformation of things), `Ctrl+Shift+Z`
/ `Ctrl+Shift+Y` over the view of the selected viewport (camera placement,
projection, display settings). Every mouse gesture (press to release), key
press, click and viewport menu command is recorded in the edition history of
the model by an `InteractionEditRecorder`.
*/
public class DrawingAreaInteractionTechniques
{
    private final DrawingArea drawingArea;
    private final ApplicationModel model;
    private final Scene scene;
    private final ViewportSet viewportSet;
    private final SceneSelectionEditor selectionEditor;
    private final ScenePicker scenePicker;
    private final ViewportInteractionTechniques interactionTechniques;
    private final ViewportSetInteractionTechniques viewportSetTechniques;
    private final VisualRayDebugController rayDebugController;
    private final SelectedBodyMapToggles mapToggles;
    private final TranslateGizmo translationGizmo;
    private final RotateGizmo rotateGizmo;
    private final ScaleGizmo scaleGizmo;
    private final DrawingAreaInteractionListener listener;
    private final InteractionEditRecorder editRecorder;
    private final UndoRedoInteractionTechnique undoRedoTechnique;

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
        scenePicker = new ScenePicker(scene);
        qualitySelection = scene.qualityTemplate;
        interactionTechniques = new ViewportInteractionTechniques(scene.camera, qualitySelection);
        viewportSetTechniques = new ViewportSetInteractionTechniques(viewportSet);
        rayDebugController = new VisualRayDebugController(model);
        mapToggles = new SelectedBodyMapToggles();
        translationGizmo = interactionTechniques.getTranslationGizmo();
        rotateGizmo = interactionTechniques.getRotateGizmo();
        scaleGizmo = interactionTechniques.getScaleGizmo();
        editRecorder = new InteractionEditRecorder(model.getEditHistory(), viewportSet);
        undoRedoTechnique = new UndoRedoInteractionTechnique(model.getEditHistory());
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

        // Transformation modes show what can be done only if there is
        // something selected to transform
        if ( selectionEditor.computeSelectionCentroid() == null ) {
            return PointerCursor.SELECT;
        }
        switch ( mode ) {
          case TRANSLATE:
            return PointerCursor.TRANSLATE;
          case ROTATE:
            return PointerCursor.ROTATE;
          case SCALE:
            return PointerCursor.SCALE;
          default:
            return PointerCursor.SELECT;
        }
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
        Viewport dragViewport = getGestureViewport();

        if ( dragViewport != null ) {
            return dragViewport;
        }
        return getViewportAtPointer(event);
    }

    /**
    @return the viewport of the gizmo gesture (drag of a translation handle or
    of a rotation ring) in course, or null if there is none
    */
    private Viewport getGestureViewport()
    {
        Viewport dragViewport = interactionTechniques.getTranslationDragViewport();

        if ( dragViewport != null ) {
            return dragViewport;
        }
        return interactionTechniques.getRotationDragViewport();
    }

    private boolean isTranslationGestureConfined()
    {
        return getGestureViewport() != null;
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
    Places the rotation gizmo over the first selected body, seen from the
    viewport, and lets the technique process the event.
    @return false if there is no viewport or selected body to process the event
    */
    private boolean processRotationEvent(MouseEvent event,
                                         Viewport viewport,
                                         Predicate<MouseEvent> technique)
    {
        SimpleBody body = selectionEditor.getFirstSelectedBody();

        if ( viewport == null || body == null ) {
            return false;
        }
        rotateGizmo.setCamera(viewport.getActiveCamera());
        rotateGizmo.setTransformationMatrix(
            SceneSelectionEditor.createRotationGizmoMatrix(body));
        if ( technique.test(toViewportEvent(event, viewport)) ) {
            listener.repaintRequested();
        }
        return true;
    }

    /**
    Places the scale gizmo over the first selected body (its frame: the same
    one the rotation gizmo uses, and its current scale factors), seen from the
    viewport, and lets the technique process the event.
    @return false if there is no viewport or selected body to process the event
    */
    private boolean processScaleEvent(MouseEvent event,
                                      Viewport viewport,
                                      Predicate<MouseEvent> technique)
    {
        SimpleBody body = selectionEditor.getFirstSelectedBody();

        if ( viewport == null || body == null ) {
            return false;
        }
        scaleGizmo.setCamera(viewport.getActiveCamera());
        scaleGizmo.setTransformationMatrix(
            SceneSelectionEditor.createRotationGizmoMatrix(body));
        scaleGizmo.setScale(body.getScale());
        if ( technique.test(toViewportEvent(event, viewport)) ) {
            listener.repaintRequested();
        }
        return true;
    }

    /**
    Discards the numbers typed in the boxes of the gizmos, so they show the
    real state of what the gizmos manipulate again.
    */
    private void cancelInputGizmoEditing()
    {
        interactionTechniques.getTranslationInputGizmo().cancelEditing();
        interactionTechniques.getRotationInputGizmo().cancelEditing();
        interactionTechniques.getScaleInputGizmo().cancelEditing();
    }

    /**
    Makes the first selected body take the orientation of the rotation gizmo.
    @param body first selected body
    */
    private void applyRotationGizmoToBody(SimpleBody body)
    {
        body.setRotation(new Matrix4x4d(rotateGizmo.getTransformationMatrix()).withoutTranslation());
    }

    /**
    Makes the first selected body take the scale factors of the scale gizmo.
    @param body first selected body
    */
    private void applyScaleGizmoToBody(SimpleBody body)
    {
        body.setScale(scaleGizmo.getScale());
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
        // Everything the gesture changes, up to its release, is one operation
        editRecorder.beginGesture(sceneOperationName(mode));
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

            if ( mode == InteractionMode.ROTATE ) {
                // The ring under the pointer is found again, as the press
                // could come without a previous movement
                gizmoGrabbed = processRotationEvent(event, mouseView,
                    e -> interactionTechniques.processRotationMousePressedEvent(e, mouseView)) &&
                    interactionTechniques.getRotationTechnique().isActive();
            }
            else if ( mode == InteractionMode.SCALE ) {
                // The handle under the pointer is found again, as the press
                // could come without a previous movement
                gizmoGrabbed = processScaleEvent(event, mouseView,
                    e -> interactionTechniques.processScaleMousePressedEvent(e, mouseView)) &&
                    interactionTechniques.getScaleTechnique().isActive();
            }

            if ( !gizmoGrabbed ) {
                // Numbers typed belong to the previous selection
                cancelInputGizmoEditing();
                model.setVisualDebugRay(scenePicker.selectObjectWithMouse(
                    viewportEvent.getX(), viewportEvent.getY(), composite));
            }

            Vector3Dd centroid = selectionEditor.computeSelectionCentroid();

            if ( centroid != null ) {
                translationGizmo.setCamera(mouseView.getActiveCamera());
                translationGizmo.setTransformationMatrix(
                    SceneSelectionEditor.createTranslationGizmoMatrix(centroid));
                // Only the translation gizmo has mouse interaction (a gesture
                // started in other mode would never be ended)
                if ( mode == InteractionMode.TRANSLATE ) {
                    interactionTechniques.processTranslationMousePressedEvent(
                        viewportEvent, mouseView);
                }
            }

            reportObjectSelection();

            // The selection may have changed what the pointer can do
            updateModeCursor(event);
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
        else if ( mode == InteractionMode.ROTATE ) {
            processRotationEvent(event, mouseView,
                interactionTechniques::processRotationMouseReleasedEvent);
        }
        else if ( mode == InteractionMode.SCALE ) {
            processScaleEvent(event, mouseView,
                interactionTechniques::processScaleMouseReleasedEvent);
        }
        editRecorder.endGesture();
    }

    public void processMouseClickedEvent(MouseEvent event)
    {
        InteractionMode mode = drawingArea.getInteractionMode();

        editRecorder.record(sceneOperationName(mode),
            () -> processMouseClickedEventWithoutRecording(event));
    }

    private void processMouseClickedEventWithoutRecording(MouseEvent event)
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
        else if ( mode == InteractionMode.ROTATE ) {
            processRotationEvent(event, mouseView,
                interactionTechniques::processRotationMouseClickedEvent);
        }
        else if ( mode == InteractionMode.SCALE ) {
            processScaleEvent(event, mouseView,
                interactionTechniques::processScaleMouseClickedEvent);
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
        else if ( mode == InteractionMode.ROTATE ) {
            processRotationEvent(event, mouseView,
                interactionTechniques::processRotationMouseMovedEvent);
        }
        else if ( mode == InteractionMode.SCALE ) {
            processScaleEvent(event, mouseView,
                interactionTechniques::processScaleMouseMovedEvent);
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
        else if ( mode == InteractionMode.ROTATE ) {
            SimpleBody body = selectionEditor.getFirstSelectedBody();

            // What the gizmo turns is oriented as the gizmo is
            processRotationEvent(event, mouseView, e -> {
                boolean changed = interactionTechniques.processRotationMouseDraggedEvent(e);

                if ( changed && body != null ) {
                    applyRotationGizmoToBody(body);
                }
                return changed;
            });
        }
        else if ( mode == InteractionMode.SCALE ) {
            SimpleBody body = selectionEditor.getFirstSelectedBody();

            processScaleEvent(event, mouseView, e -> {
                boolean changed = interactionTechniques.processScaleMouseDraggedEvent(e);

                if ( changed && body != null ) {
                    applyScaleGizmoToBody(body);
                }
                return changed;
            });
        }
    }

    /**
    WARNING: It is not working... check pending
    @param event
    */
    public void processMouseWheelEvent(MouseEvent event)
    {
        InteractionMode mode = drawingArea.getInteractionMode();

        editRecorder.beginAction();
        try {
            if ( mode == InteractionMode.CAMERA &&
                 interactionTechniques.processCameraMouseWheelEvent(event) ) {
                listener.repaintRequested();
            }
        }
        finally {
            editRecorder.endAction(sceneOperationName(mode), true);
        }
    }

    //= Keyboard ==========================================================

    /**
    Processes a key press: undo/redo chords go to the history, any other key
    is processed recording what it changes.
    @param event key press, with the key of Ctrl chords in its keycode
    */
    public void processKeyPressedEvent(KeyEvent event)
    {
        InteractionMode mode = drawingArea.getInteractionMode();
        UndoRedoCommand command = UndoRedoInteractionTechnique.recognize(event);

        if ( command != null ) {
            processUndoRedoCommand(command);
            return;
        }
        if ( isControlLetterChord(event) ) {
            // Other Ctrl+letter chords are not commands of the drawing area:
            // the letters alone are, so they must not be taken as them
            return;
        }

        editRecorder.beginAction();
        try {
            processEditionKeyPressedEvent(event);
        }
        finally {
            editRecorder.endAction(keyOperationName(mode, event), true);
        }
    }

    private void processEditionKeyPressedEvent(KeyEvent event)
    {
        InteractionMode mode = drawingArea.getInteractionMode();

        if ( (mode == InteractionMode.TRANSLATE || mode == InteractionMode.ROTATE ||
              mode == InteractionMode.SCALE) &&
             processInputGizmoKeyPressedEvent(mode, event) ) {
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
    Lets the input gizmo of the gizmo of the interaction mode use a key.
    @param mode translation, rotation or scale mode
    @param event key press
    @return true if the input gizmo used the key, so it must not be processed as any
    other command
    */
    private boolean processInputGizmoKeyPressedEvent(InteractionMode mode, KeyEvent event)
    {
        if ( mode == InteractionMode.ROTATE ) {
            return processRotationInputGizmoKeyPressedEvent(event);
        }
        if ( mode == InteractionMode.SCALE ) {
            return processScaleInputGizmoKeyPressedEvent(event);
        }
        return processTranslationInputGizmoKeyPressedEvent(event);
    }

    /**
    Lets the input gizmo of the rotation gizmo use a key. If the user accepts
    what was typed, the first selected body takes the orientation the angles
    say.
    @return true if the input gizmo used the key
    */
    private boolean processRotationInputGizmoKeyPressedEvent(KeyEvent event)
    {
        SimpleBody body = selectionEditor.getFirstSelectedBody();

        if ( body == null ) {
            return false;
        }
        rotateGizmo.setTransformationMatrix(
            SceneSelectionEditor.createRotationGizmoMatrix(body));
        if ( !interactionTechniques.isRotationInputGizmoKey(event) ) {
            return false;
        }
        if ( interactionTechniques.processRotateKeyPressedEvent(event) ) {
            applyRotationGizmoToBody(body);
        }
        return true;
    }

    /**
    Lets the input gizmo of the scale gizmo use a key. If the user accepts
    what was typed, the first selected body takes the scale factors the
    numbers say.
    @return true if the input gizmo used the key
    */
    private boolean processScaleInputGizmoKeyPressedEvent(KeyEvent event)
    {
        SimpleBody body = selectionEditor.getFirstSelectedBody();

        if ( body == null ) {
            return false;
        }
        scaleGizmo.setTransformationMatrix(
            SceneSelectionEditor.createRotationGizmoMatrix(body));
        scaleGizmo.setScale(body.getScale());
        if ( !interactionTechniques.isScaleInputGizmoKey(event) ) {
            return false;
        }
        if ( interactionTechniques.processScaleKeyPressedEvent(event) ) {
            body.setScale(scaleGizmo.getScale());
        }
        return true;
    }

    /**
    Lets the input gizmo of the translation gizmo use a key. If the user
    accepts what was typed, the selection is moved so the gizmo is exactly
    where the numbers say.
    @return true if the input gizmo used the key
    */
    private boolean processTranslationInputGizmoKeyPressedEvent(KeyEvent event)
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
            if ( event.unicodeId == KeyEvent.KEY_NONE ) {
                if ( event.keycode == KeyEvent.KEY_LEFT ) {
                    cancelInputGizmoEditing();
                    selectionEditor.selectPrevious();
                    reportObjectSelection();
                }
                else if ( event.keycode == KeyEvent.KEY_RIGHT ) {
                    cancelInputGizmoEditing();
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
                rotateGizmo.setTransformationMatrix(
                    SceneSelectionEditor.createRotationGizmoMatrix(body));
                if ( interactionTechniques.processRotateKeyPressedEvent(event) ) {
                    applyRotationGizmoToBody(body);
                }
            }
            break;
          case SCALE:
            body = selectionEditor.getFirstSelectedBody();
            if ( body != null ) {
                scaleGizmo.setTransformationMatrix(
                    SceneSelectionEditor.createRotationGizmoMatrix(body));
                scaleGizmo.setScale(body.getScale());
                if ( interactionTechniques.processScaleKeyPressedEvent(event) ) {
                    body.setScale(scaleGizmo.getScale());
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
            cancelInputGizmoEditing();
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
        if ( event.unicodeId == KeyEvent.KEY_NONE ) {
            return;
        }

        // Visual debug ray control
        if ( rayDebugController.processKeyPressedEvent(event) ) {
            return;
        }

        SimpleBody body;

        switch ( event.unicodeId ) {
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
        cancelInputGizmoEditing();
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
        InteractionMode mode = drawingArea.getInteractionMode();

        if ( UndoRedoInteractionTechnique.recognize(event) != null ||
             isControlLetterChord(event) ) {
            return;
        }
        editRecorder.beginAction();
        try {
            if ( mode == InteractionMode.CAMERA &&
                 interactionTechniques.processCameraKeyReleasedEvent(event) ) {
                listener.repaintRequested();
            }
        }
        finally {
            editRecorder.endAction(sceneOperationName(mode), true);
        }
    }

    //= Viewport commands =================================================

    /**
    Processes one of the standard commands of the viewport set (projection
    location or render mode, i.e. chosen in the menu of a viewport) over a
    viewport, recording the change of its view.
    @param command the id of the command, starting with `IDV_`
    @param viewport viewport to change
    @return true if the command was a viewport set one and was processed
    */
    public boolean processViewportCommand(String command, Viewport viewport)
    {
        boolean processed;

        editRecorder.beginAction();
        try {
            processed = viewportSetTechniques.processCommand(command, viewport);
        }
        finally {
            editRecorder.endAction(sceneOperationName(drawingArea.getInteractionMode()), false);
        }
        if ( processed && viewport == viewportSet.getSelectedViewport() ) {
            activateViewport(viewport);
        }
        listener.repaintRequested();
        return processed;
    }

    //= Undo / redo =======================================================

    /**
    Undoes or redoes an operation of the scene history or of the view history
    of the selected viewport. A gesture whose release was lost is finished
    (recorded) first, so it is what gets undone.
    @param command the command to execute
    */
    public void processUndoRedoCommand(UndoRedoCommand command)
    {
        Viewport selectedViewport = viewportSet.getSelectedViewport();
        UndoRedoInteractionTechnique.Result result;

        editRecorder.endGesture();
        // Numbers typed in the gizmo boxes belong to the state before
        cancelInputGizmoEditing();
        result = undoRedoTechnique.execute(command, selectedViewport);

        if ( result.isDone() ) {
            if ( command.isViewportCommand() ) {
                // The viewport may be showing other camera now
                activateViewport(selectedViewport);
            }
            else {
                listener.selectionChanged();
            }
        }
        listener.statusMessageRequested(result.message());
        listener.cursorRequested(getModeCursor());
        listener.repaintRequested();
    }

    /**
    @return true if the key press is a Ctrl chord with a letter
    */
    private static boolean isControlLetterChord(KeyEvent event)
    {
        return (event.modifierMask & KeyEvent.MASK_CTRL) != 0 &&
            event.keycode >= KeyEvent.KEY_A && event.keycode <= KeyEvent.KEY_z;
    }

    /**
    @return name of the operation over the scene done in a mode
    */
    private static String sceneOperationName(InteractionMode mode)
    {
        switch ( mode ) {
          case TRANSLATE:
            return "Translation";
          case ROTATE:
            return "Rotation";
          case SCALE:
            return "Scale";
          default:
            return "Scene edition";
        }
    }

    /**
    @return name of the operation over the scene done by a key in a mode
    */
    private static String keyOperationName(InteractionMode mode, KeyEvent event)
    {
        if ( event.keycode == KeyEvent.KEY_DELETE ) {
            return "Deletion";
        }
        return sceneOperationName(mode);
    }


    //= Selection feedback ================================================

    private void reportObjectSelection()
    {
        listener.statusMessageRequested(selectionEditor.describeSelection());
        listener.selectionChanged();
    }
}
