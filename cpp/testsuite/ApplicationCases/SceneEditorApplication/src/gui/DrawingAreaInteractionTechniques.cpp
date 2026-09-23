#include "java/lang/System.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/gui/gizmo/InputGizmo.h"
#include "vsdk/toolkit/gui/gizmo/RotateGizmo.h"
#include "vsdk/toolkit/gui/gizmo/RotateGizmoInteractionTechnique.h"
#include "vsdk/toolkit/gui/gizmo/ScaleGizmo.h"
#include "vsdk/toolkit/gui/gizmo/ScaleGizmoInteractionTechnique.h"
#include "vsdk/toolkit/gui/gizmo/TranslateGizmo.h"
#include "vsdk/toolkit/gui/gizmo/TranslateGizmoInteractionTechnique.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/gui/viewport/ViewportInteractionTechniques.h"
#include "vsdk/toolkit/gui/viewport/ViewportSet.h"
#include "vsdk/toolkit/gui/viewport/ViewportSetInteractionTechniques.h"
#include "model/ApplicationModel.h"
#include "model/DrawingArea.h"
#include "model/Scene.h"
#include "model/selection/ScenePicker.h"
#include "model/selection/SceneSelectionEditor.h"
#include "gui/DrawingAreaInteractionListener.h"
#include "gui/DrawingAreaInteractionTechniques.h"
#include "gui/SelectedBodyMapToggles.h"
#include "gui/VisualRayDebugController.h"
#include "gui/history/InteractionEditRecorder.h"
#include "gui/history/UndoRedoInteractionTechnique.h"

DrawingAreaInteractionTechniques::DrawingAreaInteractionTechniques(
    ApplicationModel* model, DrawingAreaInteractionListener* listener)
{
    this->model = model;
    this->drawingArea = model->getDrawingArea();
    this->viewportSet = drawingArea->getViewportSet();
    this->scene = model->getScene();
    this->listener = listener;

    selectionEditor = new SceneSelectionEditor(scene);
    scenePicker = new ScenePicker(scene);
    qualitySelection = scene->qualityTemplate;
    interactionTechniques = new ViewportInteractionTechniques(scene->camera,
                                                              qualitySelection);
    viewportSetTechniques = new ViewportSetInteractionTechniques(viewportSet);
    rayDebugController = new VisualRayDebugController(model);
    mapToggles = new SelectedBodyMapToggles();
    translationGizmo = interactionTechniques->getTranslationGizmo();
    rotateGizmo = interactionTechniques->getRotateGizmo();
    scaleGizmo = interactionTechniques->getScaleGizmo();
    editRecorder = new InteractionEditRecorder(model->getEditHistory(),
                                               viewportSet);
    undoRedoTechnique = new UndoRedoInteractionTechnique(
        model->getEditHistory());
}

DrawingAreaInteractionTechniques::~DrawingAreaInteractionTechniques()
{
    delete undoRedoTechnique;
    delete editRecorder;
    delete mapToggles;
    delete rayDebugController;
    delete viewportSetTechniques;
    delete interactionTechniques;
    delete scenePicker;
    delete selectionEditor;
}

ViewportSetInteractionTechniques*
DrawingAreaInteractionTechniques::getViewportSetTechniques() const
{
    return viewportSetTechniques;
}

TranslateGizmo* DrawingAreaInteractionTechniques::getTranslationGizmo() const
{
    return translationGizmo;
}

RotateGizmo* DrawingAreaInteractionTechniques::getRotateGizmo() const
{
    return rotateGizmo;
}

ScaleGizmo* DrawingAreaInteractionTechniques::getScaleGizmo() const
{
    return scaleGizmo;
}

void DrawingAreaInteractionTechniques::setCursorWrapEnabled(bool enabled)
{
    interactionTechniques->setTranslationCursorWrapEnabled(enabled);
}

void DrawingAreaInteractionTechniques::activateViewport(Viewport* viewport)
{
    if ( viewport == nullptr ) {
        return;
    }

    interactionTechniques->setCamera(viewport->getActiveCamera());
    interactionTechniques->setRendererConfiguration(
        viewport->getRendererConfiguration());
    qualitySelection = viewport->getRendererConfiguration();
}

//= Pointer shape =========================================================

PointerCursor::Value DrawingAreaInteractionTechniques::getModeCursor()
{
    InteractionMode mode = drawingArea->getInteractionMode();

    if ( mode == InteractionMode::CAMERA ) {
        return PointerCursor::CAMERA_ROTATE;
    }

    // Transformation modes show what can be done only if there is
    // something selected to transform
    Vector3Dd centroid;
    if ( !selectionEditor->computeSelectionCentroid(&centroid) ) {
        return PointerCursor::SELECT;
    }
    switch ( mode ) {
      case InteractionMode::TRANSLATE:
        return PointerCursor::TRANSLATE;
      case InteractionMode::ROTATE:
        return PointerCursor::ROTATE;
      case InteractionMode::SCALE:
        return PointerCursor::SCALE;
      default:
        return PointerCursor::SELECT;
    }
}

void DrawingAreaInteractionTechniques::requestCursorForPointer(
    const MouseEvent& event, PointerCursor::Value cursor)
{
    // A normal pointer over the title of a viewport (feedback that it can
    // be clicked), the given one elsewhere
    PointerCursor::Value wanted = cursor;

    if ( viewportSetTechniques->isPointerOverTitle(
             drawingArea->toSurfaceEvent(event)) ) {
        wanted = PointerCursor::VIEWPORT_TITLE;
    }
    listener->cursorRequested(wanted);
}

void DrawingAreaInteractionTechniques::updateModeCursor(
    const MouseEvent& event)
{
    requestCursorForPointer(event, getModeCursor());
}

//= Mouse =================================================================

Viewport* DrawingAreaInteractionTechniques::getViewportAtPointer(
    const MouseEvent& event)
{
    return viewportSetTechniques->findViewportAt(
        drawingArea->toSurfaceEvent(event));
}

Viewport* DrawingAreaInteractionTechniques::getInteractionViewportAtPointer(
    const MouseEvent& event)
{
    // A translation gesture (drag of the gizmo) belongs to the viewport
    // where it started, even if the cursor goes over another one.
    Viewport* dragViewport = getGestureViewport();

    if ( dragViewport != nullptr ) {
        return dragViewport;
    }
    return getViewportAtPointer(event);
}

Viewport* DrawingAreaInteractionTechniques::getGestureViewport()
{
    Viewport* dragViewport =
        interactionTechniques->getTranslationDragViewport();

    if ( dragViewport != nullptr ) {
        return dragViewport;
    }
    return interactionTechniques->getRotationDragViewport();
}

bool DrawingAreaInteractionTechniques::isTranslationGestureConfined()
{
    return getGestureViewport() != nullptr;
}

MouseEvent DrawingAreaInteractionTechniques::toViewportEvent(
    const MouseEvent& event, Viewport* viewport)
{
    return viewportSetTechniques->toViewportEvent(
        drawingArea->toSurfaceEvent(event), viewport);
}

void DrawingAreaInteractionTechniques::wrapCursorIfRequested(
    Viewport* viewport)
{
    // Places the pointer as requested by the translation technique when
    // the cursor leaves the viewport of the gesture (infinite drag).
    TranslateGizmoInteractionTechnique::CursorWarp warp;

    if ( !interactionTechniques->consumeTranslationCursorWarp(&warp) ||
         viewport == nullptr ) {
        return;
    }
    listener->cursorWarpRequested(
        viewportSet->toSetX(viewport, warp.x()),
        viewportSet->toSetY(viewport, warp.y()));
}

bool DrawingAreaInteractionTechniques::processTranslationEvent(
    const MouseEvent& event, Viewport* viewport, const Vector3Dd& centroid,
    const MouseTechnique& technique)
{
    // Places the translation gizmo over the selection centroid, seen from
    // the viewport, and lets the technique process the event. If the gizmo
    // moves the selection, it is moved with it.
    if ( viewport == nullptr ) {
        return false;
    }
    translationGizmo->setCamera(viewport->getActiveCamera());
    translationGizmo->setTransformationMatrix(
        SceneSelectionEditor::createTranslationGizmoMatrix(centroid));
    if ( technique(toViewportEvent(event, viewport)) ) {
        selectionEditor->applyTranslationToSelectedObjects(centroid,
            translationGizmo->getPosition());
        listener->repaintRequested();
    }
    return true;
}

bool DrawingAreaInteractionTechniques::processRotationEvent(
    const MouseEvent& event, Viewport* viewport,
    const MouseTechnique& technique)
{
    // Places the rotation gizmo over the first selected body, seen from the
    // viewport, and lets the technique process the event.
    SimpleBody* body = selectionEditor->getFirstSelectedBody();

    if ( viewport == nullptr || body == nullptr ) {
        return false;
    }
    rotateGizmo->setCamera(viewport->getActiveCamera());
    rotateGizmo->setTransformationMatrix(
        SceneSelectionEditor::createRotationGizmoMatrix(body));
    if ( technique(toViewportEvent(event, viewport)) ) {
        listener->repaintRequested();
    }
    return true;
}

bool DrawingAreaInteractionTechniques::processScaleEvent(
    const MouseEvent& event, Viewport* viewport,
    const MouseTechnique& technique)
{
    // Places the scale gizmo over the first selected body (its frame: the
    // same one the rotation gizmo uses, and its current scale factors), seen
    // from the viewport, and lets the technique process the event.
    SimpleBody* body = selectionEditor->getFirstSelectedBody();

    if ( viewport == nullptr || body == nullptr ) {
        return false;
    }
    scaleGizmo->setCamera(viewport->getActiveCamera());
    scaleGizmo->setTransformationMatrix(
        SceneSelectionEditor::createRotationGizmoMatrix(body));
    scaleGizmo->setScale(body->getScale());
    if ( technique(toViewportEvent(event, viewport)) ) {
        listener->repaintRequested();
    }
    return true;
}

void DrawingAreaInteractionTechniques::cancelInputGizmoEditing()
{
    // Discards the numbers typed in the boxes of the gizmos, so they show
    // the real state of what the gizmos manipulate again.
    interactionTechniques->getTranslationInputGizmo()->cancelEditing();
    interactionTechniques->getRotationInputGizmo()->cancelEditing();
    interactionTechniques->getScaleInputGizmo()->cancelEditing();
}

void DrawingAreaInteractionTechniques::applyRotationGizmoToBody(
    SimpleBody* body)
{
    body->setRotation(rotateGizmo->getTransformationMatrix().withoutTranslation());
}

void DrawingAreaInteractionTechniques::applyScaleGizmoToBody(SimpleBody* body)
{
    body->setScale(scaleGizmo->getScale());
}

void DrawingAreaInteractionTechniques::processMouseEnteredEvent(
    const MouseEvent& event)
{
    // WARNING / TODO
    // There should be a cameraController.getFutureAction(e) that calculates
    // the proper icon for display ... here an Aquynza operation is
    // assumed and hard-coded
    updateModeCursor(event);
}

void DrawingAreaInteractionTechniques::processMousePressedEvent(
    const MouseEvent& event)
{
    Viewport* mouseView = getViewportAtPointer(event);
    InteractionMode mode = drawingArea->getInteractionMode();

    if ( mouseView == nullptr ) {
        return;
    }
    // Everything the gesture changes, up to its release, is one operation
    editRecorder->beginGesture(sceneOperationName(mode));
    viewportSetTechniques->processMousePressedEvent(
        drawingArea->toSurfaceEvent(event));
    activateViewport(mouseView);

    //-----------------------------------------------------------------
    // WARNING / TODO
    // There should be a cameraController.getFutureAction(e) that calculates
    // the proper icon for display ... here an Aquynza operation is
    // assumed and hard-coded
    int m = event.getModifiers();
    PointerCursor::Value cursor;

    if ( mode == InteractionMode::CAMERA &&
         (m & MouseEvent::BUTTON1_DOWN_MASK) != 0 ) {
        cursor = PointerCursor::CAMERA_ROTATE;
    }
    else if ( mode == InteractionMode::CAMERA &&
              (m & MouseEvent::BUTTON2_DOWN_MASK) != 0 ) {
        cursor = PointerCursor::CAMERA_TRANSLATE;
    }
    else if ( mode == InteractionMode::CAMERA &&
              (m & MouseEvent::BUTTON3_DOWN_MASK) != 0 ) {
        cursor = PointerCursor::CAMERA_ADVANCE;
    }
    else {
        cursor = getModeCursor();
    }
    requestCursorForPointer(event, cursor);

    //-----------------------------------------------------------------
    if ( mode == InteractionMode::CAMERA &&
         interactionTechniques->processCameraMousePressedEvent(event) ) {
    }
    else if ( mode != InteractionMode::CAMERA ) {
        bool composite = (m & MouseEvent::CTRL_DOWN_MASK) != 0;
        MouseEvent viewportEvent = toViewportEvent(event, mouseView);
        Vector3Dd centroid;

        scene->activeCamera = mouseView->getActiveCamera();

        // A press over a gizmo handle grabs the gizmo: the ray selection
        // is skipped, so the whole selected group is kept
        bool gizmoGrabbed =
            mode == InteractionMode::TRANSLATE &&
            interactionTechniques->getTranslationTechnique()->isActive() &&
            selectionEditor->computeSelectionCentroid(&centroid);

        if ( mode == InteractionMode::ROTATE ) {
            // The ring under the pointer is found again, as the press
            // could come without a previous movement
            gizmoGrabbed = processRotationEvent(event, mouseView,
                [this, mouseView](const MouseEvent& e) {
                    return interactionTechniques->
                        processRotationMousePressedEvent(e, mouseView);
                }) &&
                interactionTechniques->getRotationTechnique()->isActive();
        }
        else if ( mode == InteractionMode::SCALE ) {
            // The handle under the pointer is found again, as the press
            // could come without a previous movement
            gizmoGrabbed = processScaleEvent(event, mouseView,
                [this, mouseView](const MouseEvent& e) {
                    return interactionTechniques->
                        processScaleMousePressedEvent(e, mouseView);
                }) &&
                interactionTechniques->getScaleTechnique()->isActive();
        }

        if ( !gizmoGrabbed ) {
            // Numbers typed belong to the previous selection
            cancelInputGizmoEditing();
            Ray selectedRay = scenePicker->selectObjectWithMouse(
                viewportEvent.getX(), viewportEvent.getY(), composite);
            model->setVisualDebugRay(&selectedRay);
        }

        if ( selectionEditor->computeSelectionCentroid(&centroid) ) {
            translationGizmo->setCamera(mouseView->getActiveCamera());
            translationGizmo->setTransformationMatrix(
                SceneSelectionEditor::createTranslationGizmoMatrix(centroid));
            // Only the translation gizmo has mouse interaction (a gesture
            // started in other mode would never be ended)
            if ( mode == InteractionMode::TRANSLATE ) {
                interactionTechniques->processTranslationMousePressedEvent(
                    viewportEvent, mouseView);
            }
        }

        reportObjectSelection();

        // The selection may have changed what the pointer can do
        updateModeCursor(event);
    }
    listener->repaintRequested();
}

void DrawingAreaInteractionTechniques::processMouseReleasedEvent(
    const MouseEvent& event)
{
    Viewport* mouseView = getInteractionViewportAtPointer(event);
    bool confinedGesture = isTranslationGestureConfined();
    InteractionMode mode = drawingArea->getInteractionMode();

    if ( mouseView != nullptr && !confinedGesture ) {
        viewportSetTechniques->processMouseReleasedEvent(
            drawingArea->toSurfaceEvent(event));
    }
    activateViewport(mouseView);

    // WARNING / TODO
    // There should be a cameraController.getFutureAction(e) that calculates
    // the proper icon for display ... here an Aquynza operation is
    // assumed and hard-coded

    Vector3Dd centroid;
    bool withSelection = selectionEditor->computeSelectionCentroid(&centroid);

    updateModeCursor(event);

    if ( mode == InteractionMode::CAMERA &&
         interactionTechniques->processCameraMouseReleasedEvent(event) ) {
        listener->repaintRequested();
    }
    else if ( mode == InteractionMode::TRANSLATE && withSelection ) {
        processTranslationEvent(event, mouseView, centroid,
            [this](const MouseEvent& e) {
                return interactionTechniques->
                    processTranslationMouseReleasedEvent(e);
            });
    }
    else if ( mode == InteractionMode::ROTATE ) {
        processRotationEvent(event, mouseView,
            [this](const MouseEvent& e) {
                return interactionTechniques->
                    processRotationMouseReleasedEvent(e);
            });
    }
    else if ( mode == InteractionMode::SCALE ) {
        processScaleEvent(event, mouseView,
            [this](const MouseEvent& e) {
                return interactionTechniques->
                    processScaleMouseReleasedEvent(e);
            });
    }
    editRecorder->endGesture();
}

void DrawingAreaInteractionTechniques::processMouseClickedEvent(
    const MouseEvent& event)
{
    InteractionMode mode = drawingArea->getInteractionMode();

    editRecorder->record(sceneOperationName(mode),
        [this, &event]() { processMouseClickedEventWithoutRecording(event); });
}

void DrawingAreaInteractionTechniques::processMouseClickedEventWithoutRecording(
    const MouseEvent& event)
{
    Viewport* mouseView = getViewportAtPointer(event);
    InteractionMode mode = drawingArea->getInteractionMode();

    if ( mouseView != nullptr ) {
        viewportSetTechniques->processMouseClickedEvent(
            drawingArea->toSurfaceEvent(event));
    }
    activateViewport(mouseView);

    Vector3Dd centroid;
    bool withSelection = selectionEditor->computeSelectionCentroid(&centroid);

    if ( mode == InteractionMode::CAMERA &&
         interactionTechniques->processCameraMouseClickedEvent(event) ) {
        listener->repaintRequested();
    }
    else if ( mode == InteractionMode::TRANSLATE && withSelection ) {
        processTranslationEvent(event, mouseView, centroid,
            [this](const MouseEvent& e) {
                return interactionTechniques->
                    processTranslationMouseClickedEvent(e);
            });
    }
    else if ( mode == InteractionMode::ROTATE ) {
        processRotationEvent(event, mouseView,
            [this](const MouseEvent& e) {
                return interactionTechniques->
                    processRotationMouseClickedEvent(e);
            });
    }
    else if ( mode == InteractionMode::SCALE ) {
        processScaleEvent(event, mouseView,
            [this](const MouseEvent& e) {
                return interactionTechniques->
                    processScaleMouseClickedEvent(e);
            });
    }
}

void DrawingAreaInteractionTechniques::processMouseMovedEvent(
    const MouseEvent& event)
{
    Viewport* mouseView = getInteractionViewportAtPointer(event);
    bool confinedGesture = isTranslationGestureConfined();
    InteractionMode mode = drawingArea->getInteractionMode();

    // Moved events can come while dragging the gizmo (i.e. when the cursor
    // is wrapped): the viewport of the gesture keeps on being the one used
    if ( mouseView != nullptr && !confinedGesture ) {
        interactionTechniques->setCamera(mouseView->getActiveCamera());
    }

    //-----------------------------------------------------------------
    Vector3Dd centroid;
    bool withSelection = selectionEditor->computeSelectionCentroid(&centroid);

    if ( mode == InteractionMode::CAMERA &&
         interactionTechniques->processCameraMouseMovedEvent(event) ) {
        listener->repaintRequested();
    }
    else if ( mode == InteractionMode::TRANSLATE && withSelection ) {
        processTranslationEvent(event, mouseView, centroid,
            [this](const MouseEvent& e) {
                return interactionTechniques->
                    processTranslationMouseMovedEvent(e);
            });
    }
    else if ( mode == InteractionMode::ROTATE ) {
        processRotationEvent(event, mouseView,
            [this](const MouseEvent& e) {
                return interactionTechniques->
                    processRotationMouseMovedEvent(e);
            });
    }
    else if ( mode == InteractionMode::SCALE ) {
        processScaleEvent(event, mouseView,
            [this](const MouseEvent& e) {
                return interactionTechniques->
                    processScaleMouseMovedEvent(e);
            });
    }
}

void DrawingAreaInteractionTechniques::processMouseDraggedEvent(
    const MouseEvent& event)
{
    Viewport* mouseView = getInteractionViewportAtPointer(event);
    bool confinedGesture = isTranslationGestureConfined();
    InteractionMode mode = drawingArea->getInteractionMode();

    // While dragging the gizmo, the pointer over other viewport does not
    // select it
    if ( mouseView != nullptr && !confinedGesture ) {
        viewportSetTechniques->processMouseDraggedEvent(
            drawingArea->toSurfaceEvent(event));
    }
    activateViewport(mouseView);

    Vector3Dd centroid;
    bool withSelection = selectionEditor->computeSelectionCentroid(&centroid);

    if ( mode == InteractionMode::CAMERA &&
         interactionTechniques->processCameraMouseDraggedEvent(event) ) {
        listener->repaintRequested();
    }
    else if ( mode == InteractionMode::TRANSLATE && withSelection ) {
        if ( processTranslationEvent(event, mouseView, centroid,
                 [this](const MouseEvent& e) {
                     return interactionTechniques->
                         processTranslationMouseDraggedEvent(e);
                 }) ) {
            wrapCursorIfRequested(mouseView);
        }
    }
    else if ( mode == InteractionMode::ROTATE ) {
        SimpleBody* body = selectionEditor->getFirstSelectedBody();

        // What the gizmo turns is oriented as the gizmo is
        processRotationEvent(event, mouseView,
            [this, body](const MouseEvent& e) {
                bool changed = interactionTechniques->
                    processRotationMouseDraggedEvent(e);

                if ( changed && body != nullptr ) {
                    applyRotationGizmoToBody(body);
                }
                return changed;
            });
    }
    else if ( mode == InteractionMode::SCALE ) {
        SimpleBody* body = selectionEditor->getFirstSelectedBody();

        processScaleEvent(event, mouseView,
            [this, body](const MouseEvent& e) {
                bool changed = interactionTechniques->
                    processScaleMouseDraggedEvent(e);

                if ( changed && body != nullptr ) {
                    applyScaleGizmoToBody(body);
                }
                return changed;
            });
    }
}

void DrawingAreaInteractionTechniques::processMouseWheelEvent(
    const MouseEvent& event)
{
    InteractionMode mode = drawingArea->getInteractionMode();

    editRecorder->beginAction();
    try {
        if ( mode == InteractionMode::CAMERA &&
             interactionTechniques->processCameraMouseWheelEvent(event) ) {
            listener->repaintRequested();
        }
    }
    catch ( ... ) {
        editRecorder->endAction(sceneOperationName(mode), true);
        throw;
    }
    editRecorder->endAction(sceneOperationName(mode), true);
}

//= Keyboard ==============================================================

void DrawingAreaInteractionTechniques::processKeyPressedEvent(
    const KeyEvent& event)
{
    InteractionMode mode = drawingArea->getInteractionMode();
    UndoRedoCommand::Value command;

    if ( UndoRedoInteractionTechnique::recognize(event, &command) ) {
        processUndoRedoCommand(command);
        return;
    }
    if ( isControlLetterChord(event) ) {
        // Other Ctrl+letter chords are not commands of the drawing area:
        // the letters alone are, so they must not be taken as them
        return;
    }

    editRecorder->beginAction();
    try {
        processEditionKeyPressedEvent(event);
    }
    catch ( ... ) {
        editRecorder->endAction(keyOperationName(mode, event), true);
        throw;
    }
    editRecorder->endAction(keyOperationName(mode, event), true);
}

void DrawingAreaInteractionTechniques::processEditionKeyPressedEvent(
    const KeyEvent& event)
{
    InteractionMode mode = drawingArea->getInteractionMode();

    if ( (mode == InteractionMode::TRANSLATE ||
          mode == InteractionMode::ROTATE ||
          mode == InteractionMode::SCALE) &&
         processInputGizmoKeyPressedEvent(mode, event) ) {
        listener->repaintRequested();
        return;
    }

    if ( mode == InteractionMode::CAMERA &&
         interactionTechniques->processCameraKeyPressedEvent(event) ) {
    }
    else {
        processModeKeyPressedEvent(mode, event);
    }

    processGlobalKeyPressedEvent(event);

    // Viewport set commands (selection, layout, per-viewport display)
    viewportSetTechniques->processKeyPressedEvent(event);

    processCharacterKeyPressedEvent(event);

    listener->cursorRequested(getModeCursor());
    listener->repaintRequested();
}

bool DrawingAreaInteractionTechniques::processInputGizmoKeyPressedEvent(
    InteractionMode mode, const KeyEvent& event)
{
    // Lets the input gizmo of the gizmo of the interaction mode use a key,
    // returning true if it used the key, so it must not be processed as any
    // other command
    if ( mode == InteractionMode::ROTATE ) {
        return processRotationInputGizmoKeyPressedEvent(event);
    }
    if ( mode == InteractionMode::SCALE ) {
        return processScaleInputGizmoKeyPressedEvent(event);
    }
    return processTranslationInputGizmoKeyPressedEvent(event);
}

bool DrawingAreaInteractionTechniques::processRotationInputGizmoKeyPressedEvent(
    const KeyEvent& event)
{
    // If the user accepts what was typed, the first selected body takes the
    // orientation the angles say.
    SimpleBody* body = selectionEditor->getFirstSelectedBody();

    if ( body == nullptr ) {
        return false;
    }
    rotateGizmo->setTransformationMatrix(
        SceneSelectionEditor::createRotationGizmoMatrix(body));
    if ( !interactionTechniques->isRotationInputGizmoKey(event) ) {
        return false;
    }
    if ( interactionTechniques->processRotateKeyPressedEvent(event) ) {
        applyRotationGizmoToBody(body);
    }
    return true;
}

bool DrawingAreaInteractionTechniques::processScaleInputGizmoKeyPressedEvent(
    const KeyEvent& event)
{
    // If the user accepts what was typed, the first selected body takes the
    // scale factors the numbers say.
    SimpleBody* body = selectionEditor->getFirstSelectedBody();

    if ( body == nullptr ) {
        return false;
    }
    scaleGizmo->setTransformationMatrix(
        SceneSelectionEditor::createRotationGizmoMatrix(body));
    scaleGizmo->setScale(body->getScale());
    if ( !interactionTechniques->isScaleInputGizmoKey(event) ) {
        return false;
    }
    if ( interactionTechniques->processScaleKeyPressedEvent(event) ) {
        body->setScale(scaleGizmo->getScale());
    }
    return true;
}

bool DrawingAreaInteractionTechniques::processTranslationInputGizmoKeyPressedEvent(
    const KeyEvent& event)
{
    // If the user accepts what was typed, the selection is moved so the
    // gizmo is exactly where the numbers say.
    Vector3Dd centroid;

    if ( !selectionEditor->computeSelectionCentroid(&centroid) ) {
        return false;
    }
    translationGizmo->setTransformationMatrix(
        SceneSelectionEditor::createTranslationGizmoMatrix(centroid));
    if ( !interactionTechniques->isTranslationInputGizmoKey(event) ) {
        return false;
    }
    if ( interactionTechniques->processTranslationKeyPressedEvent(event) ) {
        selectionEditor->applyTranslationToSelectedObjects(centroid,
            translationGizmo->getPosition());
    }
    return true;
}

void DrawingAreaInteractionTechniques::processModeKeyPressedEvent(
    InteractionMode mode, const KeyEvent& event)
{
    // Keys with a meaning that depends on the interaction mode.
    SimpleBody* body;
    Vector3Dd centroid;

    switch ( mode ) {
      case InteractionMode::SELECT:
        if ( event.unicodeId == KeyEvent::KEY_NONE ) {
            if ( event.keycode == KeyEvent::KEY_LEFT ) {
                cancelInputGizmoEditing();
                selectionEditor->selectPrevious();
                reportObjectSelection();
            }
            else if ( event.keycode == KeyEvent::KEY_RIGHT ) {
                cancelInputGizmoEditing();
                selectionEditor->selectNext();
                reportObjectSelection();
            }
        }
        break;
      case InteractionMode::TRANSLATE:
        if ( selectionEditor->computeSelectionCentroid(&centroid) ) {
            translationGizmo->setTransformationMatrix(
                SceneSelectionEditor::createTranslationGizmoMatrix(centroid));
            if ( interactionTechniques->processTranslationKeyPressedEvent(event) ) {
                selectionEditor->applyTranslationToSelectedObjects(centroid,
                    translationGizmo->getPosition());
            }
        }
        break;
      case InteractionMode::ROTATE:
        body = selectionEditor->getFirstSelectedBody();
        if ( body != nullptr ) {
            rotateGizmo->setTransformationMatrix(
                SceneSelectionEditor::createRotationGizmoMatrix(body));
            if ( interactionTechniques->processRotateKeyPressedEvent(event) ) {
                applyRotationGizmoToBody(body);
            }
        }
        break;
      case InteractionMode::SCALE:
        body = selectionEditor->getFirstSelectedBody();
        if ( body != nullptr ) {
            scaleGizmo->setTransformationMatrix(
                SceneSelectionEditor::createRotationGizmoMatrix(body));
            scaleGizmo->setScale(body->getScale());
            if ( interactionTechniques->processScaleKeyPressedEvent(event) ) {
                body->setScale(scaleGizmo->getScale());
            }
        }
        break;
      default:
        break;
    }
}

void DrawingAreaInteractionTechniques::processGlobalKeyPressedEvent(
    const KeyEvent& event)
{
    // Keys with the same meaning in every interaction mode.
    int apparentSize;

    switch ( event.keycode ) {
      case KeyEvent::KEY_ESC:
        listener->closeRequested();
        break;
      case KeyEvent::KEY_EQUALS:
        apparentSize = translationGizmo->getBaseApparentSizeInPixels();
        apparentSize += 10;
        if ( apparentSize > 300 ) apparentSize = 300;
        translationGizmo->setBaseApparentSizeInPixels(apparentSize);
        break;
      case KeyEvent::KEY_MINUS:
        apparentSize = translationGizmo->getBaseApparentSizeInPixels();
        apparentSize -= 20;
        if ( apparentSize < 20 ) apparentSize = 20;
        translationGizmo->setBaseApparentSizeInPixels(apparentSize);
        break;
      case KeyEvent::KEY_DELETE:
        cancelInputGizmoEditing();
        selectionEditor->deleteSelected();
        break;
      case KeyEvent::KEY_F10:
        listener->raytracingRequested();
        break;
      default:
        break;
    }

    if ( interactionTechniques->processQualityKeyPressedEvent(event) ) {
        java::System::out.println(qualitySelection->toString().c_str());
    }
}

void DrawingAreaInteractionTechniques::processCharacterKeyPressedEvent(
    const KeyEvent& event)
{
    // Keys with a character (letters, digits and symbols).
    if ( event.unicodeId == KeyEvent::KEY_NONE ) {
        return;
    }

    // Visual debug ray control
    if ( rayDebugController->processKeyPressedEvent(event) ) {
        return;
    }

    SimpleBody* body;

    switch ( event.unicodeId ) {
      case 'T':
        body = selectionEditor->getFirstSelectedBody();
        if ( body != nullptr ) {
            mapToggles->toggleTexture(body);
        }
        break;
      case 'B':
        body = selectionEditor->getFirstSelectedBody();
        if ( body != nullptr ) {
            mapToggles->toggleNormalMap(body);
        }
        break;
      case 'h':
        listener->selectorDialogRequested();
        printBodyNames();
        break;
      case 'c':
        switchMode(InteractionMode::CAMERA,
            "Camera mode interaction - drag mouse with different buttons over the scene to change current camera.");
        break;
      case 'q':
        switchMode(InteractionMode::SELECT,
            "Selection mode interaction - click mouse to select objects, LEFT/RIGHT arrow keys to select sequencialy.");
        break;
      case 'w':
        // Alt+w (maximize viewport) is a viewport set command
        if ( (event.modifierMask & KeyEvent::MASK_ALT) == 0 ) {
            switchMode(InteractionMode::TRANSLATE,
                "Translation mode interaction - click mouse to select objects, X, Y, Z keys and gizmo to move it.");
        }
        break;
      case 'e':
        switchMode(InteractionMode::ROTATE,
            "Rotation mode interaction - click mouse to select objects, X, Y, Z keys and gizmo to rotate it.");
        break;
      case 'r':
        switchMode(InteractionMode::SCALE,
            "Scale mode interaction - click mouse to select objects, X, Y, Z/ARROWS keys and gizmo to scale it.");
        break;
      default:
        break;
    }
}

void DrawingAreaInteractionTechniques::switchMode(InteractionMode mode,
                                                  const java::String& message)
{
    listener->statusMessageRequested(message);
    cancelInputGizmoEditing();
    drawingArea->switchInteractionMode(mode);
}

void DrawingAreaInteractionTechniques::printBodyNames()
{
    java::ArrayList<SimpleBody*>& bodies = scene->scene->getSimpleBodies();
    java::String name;
    int i;

    for ( i = 0; i < bodies.size(); i++ ) {
        java::System::out.println((java::String("Consultando cosa ") +
            java::String::valueOf(i) + ":").c_str());
        name = bodies.get(i)->getName();
        if ( name.equals("") ) {
            name = "Not named object";
        }
        java::System::out.println((java::String("Object: ") + name).c_str());
    }
}

void DrawingAreaInteractionTechniques::processKeyReleasedEvent(
    const KeyEvent& event)
{
    InteractionMode mode = drawingArea->getInteractionMode();
    UndoRedoCommand::Value command;

    if ( UndoRedoInteractionTechnique::recognize(event, &command) ||
         isControlLetterChord(event) ) {
        return;
    }
    editRecorder->beginAction();
    try {
        if ( mode == InteractionMode::CAMERA &&
             interactionTechniques->processCameraKeyReleasedEvent(event) ) {
            listener->repaintRequested();
        }
    }
    catch ( ... ) {
        editRecorder->endAction(sceneOperationName(mode), true);
        throw;
    }
    editRecorder->endAction(sceneOperationName(mode), true);
}

//= Viewport commands =====================================================

bool DrawingAreaInteractionTechniques::processViewportCommand(
    const java::String& command, Viewport* viewport)
{
    bool processed;

    editRecorder->beginAction();
    try {
        processed = viewportSetTechniques->processCommand(command, viewport);
    }
    catch ( ... ) {
        editRecorder->endAction(
            sceneOperationName(drawingArea->getInteractionMode()), false);
        throw;
    }
    editRecorder->endAction(
        sceneOperationName(drawingArea->getInteractionMode()), false);
    if ( processed && viewport == viewportSet->getSelectedViewport() ) {
        activateViewport(viewport);
    }
    listener->repaintRequested();
    return processed;
}

//= Undo / redo ===========================================================

void DrawingAreaInteractionTechniques::processUndoRedoCommand(
    UndoRedoCommand::Value command)
{
    Viewport* selectedViewport = viewportSet->getSelectedViewport();

    editRecorder->endGesture();
    // Numbers typed in the gizmo boxes belong to the state before
    cancelInputGizmoEditing();
    UndoRedoInteractionTechnique::Result result =
        undoRedoTechnique->execute(command, selectedViewport);

    if ( result.isDone() ) {
        if ( UndoRedoCommand::isViewportCommand(command) ) {
            // The viewport may be showing other camera now
            activateViewport(selectedViewport);
        }
        else {
            listener->selectionChanged();
        }
    }
    listener->statusMessageRequested(result.message());
    listener->cursorRequested(getModeCursor());
    listener->repaintRequested();
}

bool DrawingAreaInteractionTechniques::isControlLetterChord(
    const KeyEvent& event)
{
    return (event.modifierMask & KeyEvent::MASK_CTRL) != 0 &&
        event.keycode >= KeyEvent::KEY_A && event.keycode <= KeyEvent::KEY_z;
}

java::String DrawingAreaInteractionTechniques::sceneOperationName(
    InteractionMode mode)
{
    switch ( mode ) {
      case InteractionMode::TRANSLATE:
        return "Translation";
      case InteractionMode::ROTATE:
        return "Rotation";
      case InteractionMode::SCALE:
        return "Scale";
      default:
        return "Scene edition";
    }
}

java::String DrawingAreaInteractionTechniques::keyOperationName(
    InteractionMode mode, const KeyEvent& event)
{
    if ( event.keycode == KeyEvent::KEY_DELETE ) {
        return "Deletion";
    }
    return sceneOperationName(mode);
}

//= Selection feedback ====================================================

void DrawingAreaInteractionTechniques::reportObjectSelection()
{
    listener->statusMessageRequested(selectionEditor->describeSelection());
    listener->selectionChanged();
}
