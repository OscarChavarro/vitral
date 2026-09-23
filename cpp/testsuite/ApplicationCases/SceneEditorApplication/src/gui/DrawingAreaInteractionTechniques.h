#ifndef __DRAWING_AREA_INTERACTION_TECHNIQUES__
#define __DRAWING_AREA_INTERACTION_TECHNIQUES__

#include <functional>

#include "java/lang/String.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/gui/KeyEvent.h"
#include "vsdk/toolkit/gui/MouseEvent.h"
#include "model/InteractionMode.h"
#include "gui/PointerCursor.h"
#include "gui/history/UndoRedoCommand.h"

class ApplicationModel;
class DrawingArea;
class DrawingAreaInteractionListener;
class InteractionEditRecorder;
class RendererConfiguration;
class RotateGizmo;
class ScaleGizmo;
class Scene;
class ScenePicker;
class SceneSelectionEditor;
class SelectedBodyMapToggles;
class SimpleBody;
class TranslateGizmo;
class UndoRedoInteractionTechnique;
class Viewport;
class ViewportInteractionTechniques;
class ViewportSet;
class ViewportSetInteractionTechniques;
class VisualRayDebugController;

/**
Mouse and keyboard interaction techniques of the drawing area of the editor:
camera control, selection of things, gizmo manipulation, mode selection and
global commands. It processes only vitral events, so callers must convert
events from the GUI technology in use before calling it. Anything the user
must see or the GUI technology must do (pointer shape, messages, dialogs) is
requested through the `DrawingAreaInteractionListener`.

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
`RotateGizmoInteractionTechnique`); `DELETE` removes the selected things;
`F10` requests a raytraced image; `T` / `B` toggle a sample texture / bump
map on the selected body; `h` shows the object selector; `ESC` closes the
application. Commands of the visual debug ray and of the viewport set are
processed by their own techniques.

Undo and redo (see `UndoRedoInteractionTechnique`): `Ctrl+Z` / `Ctrl+Y` over
the scene (creation, deletion and transformation of things),
`Ctrl+Shift+Z` / `Ctrl+Shift+Y` over the view of the selected viewport
(camera placement, projection, display settings). Every mouse gesture (press
to release), key press, click and viewport menu command is recorded in the
edition history of the model by an `InteractionEditRecorder`.
*/
class DrawingAreaInteractionTechniques {
private:
    typedef std::function<bool(const MouseEvent&)> MouseTechnique;

    DrawingArea* drawingArea;
    ApplicationModel* model;
    Scene* scene;
    ViewportSet* viewportSet;
    SceneSelectionEditor* selectionEditor;
    ScenePicker* scenePicker;
    ViewportInteractionTechniques* interactionTechniques;
    ViewportSetInteractionTechniques* viewportSetTechniques;
    VisualRayDebugController* rayDebugController;
    SelectedBodyMapToggles* mapToggles;
    TranslateGizmo* translationGizmo;
    RotateGizmo* rotateGizmo;
    ScaleGizmo* scaleGizmo;
    DrawingAreaInteractionListener* listener;
    InteractionEditRecorder* editRecorder;
    UndoRedoInteractionTechnique* undoRedoTechnique;

    RendererConfiguration* qualitySelection;

    //= Pointer shape =====================================================
    PointerCursor::Value getModeCursor();
    void requestCursorForPointer(const MouseEvent& event,
                                 PointerCursor::Value cursor);

    //= Mouse =============================================================
    Viewport* getViewportAtPointer(const MouseEvent& event);
    Viewport* getInteractionViewportAtPointer(const MouseEvent& event);
    Viewport* getGestureViewport();
    bool isTranslationGestureConfined();
    MouseEvent toViewportEvent(const MouseEvent& event, Viewport* viewport);
    void wrapCursorIfRequested(Viewport* viewport);
    bool processTranslationEvent(const MouseEvent& event, Viewport* viewport,
                                 const Vector3Dd& centroid,
                                 const MouseTechnique& technique);
    bool processRotationEvent(const MouseEvent& event, Viewport* viewport,
                              const MouseTechnique& technique);
    bool processScaleEvent(const MouseEvent& event, Viewport* viewport,
                           const MouseTechnique& technique);
    void cancelInputGizmoEditing();
    void applyRotationGizmoToBody(SimpleBody* body);
    void applyScaleGizmoToBody(SimpleBody* body);
    void processMouseClickedEventWithoutRecording(const MouseEvent& event);

    //= Keyboard ==========================================================
    void processEditionKeyPressedEvent(const KeyEvent& event);
    bool processInputGizmoKeyPressedEvent(InteractionMode mode,
                                          const KeyEvent& event);
    bool processRotationInputGizmoKeyPressedEvent(const KeyEvent& event);
    bool processScaleInputGizmoKeyPressedEvent(const KeyEvent& event);
    bool processTranslationInputGizmoKeyPressedEvent(const KeyEvent& event);
    void processModeKeyPressedEvent(InteractionMode mode,
                                    const KeyEvent& event);
    void processGlobalKeyPressedEvent(const KeyEvent& event);
    void processCharacterKeyPressedEvent(const KeyEvent& event);
    void switchMode(InteractionMode mode, const java::String& message);
    void printBodyNames();
    static bool isControlLetterChord(const KeyEvent& event);
    static java::String sceneOperationName(InteractionMode mode);
    static java::String keyOperationName(InteractionMode mode,
                                         const KeyEvent& event);

    //= Selection feedback ================================================
    void reportObjectSelection();

    DrawingAreaInteractionTechniques(
        const DrawingAreaInteractionTechniques& other);
    DrawingAreaInteractionTechniques& operator=(
        const DrawingAreaInteractionTechniques& other);

public:
    /**
    @param model application model, providing the scene to interact with
    @param listener who presents the requests derived from interaction
    */
    DrawingAreaInteractionTechniques(ApplicationModel* model,
                                     DrawingAreaInteractionListener* listener);
    virtual ~DrawingAreaInteractionTechniques();

    ViewportSetInteractionTechniques* getViewportSetTechniques() const;
    TranslateGizmo* getTranslationGizmo() const;
    RotateGizmo* getRotateGizmo() const;
    ScaleGizmo* getScaleGizmo() const;

    /**
    @param enabled true if the caller is able to place the pointer when the
    translation gizmo technique requests it (see
    `DrawingAreaInteractionListener::cursorWarpRequested`)
    */
    void setCursorWrapEnabled(bool enabled);

    /**
    Makes the camera controllers work over the camera and configuration of
    the given viewport. Viewport selection is processed by the viewport set
    interaction techniques.
    @param viewport the viewport to work over, or null for none
    */
    void activateViewport(Viewport* viewport);

    /**
    Requests the pointer shape for the current interaction mode, at the
    position of the event.
    @param event pointer position
    */
    void updateModeCursor(const MouseEvent& event);

    /**
    The pointer entered the drawing area.
    */
    void processMouseEnteredEvent(const MouseEvent& event);
    void processMousePressedEvent(const MouseEvent& event);
    void processMouseReleasedEvent(const MouseEvent& event);
    void processMouseClickedEvent(const MouseEvent& event);

    /**
    The pointer moved without buttons pressed.
    */
    void processMouseMovedEvent(const MouseEvent& event);
    void processMouseDraggedEvent(const MouseEvent& event);

    /**
    WARNING: It is not working... check pending
    */
    void processMouseWheelEvent(const MouseEvent& event);

    /**
    Processes a key press: undo/redo chords go to the history, any other key
    is processed recording what it changes.
    @param event key press, with the key of Ctrl chords in its keycode
    */
    void processKeyPressedEvent(const KeyEvent& event);

    /**
    @param event key released event
    */
    void processKeyReleasedEvent(const KeyEvent& event);

    /**
    Processes one of the standard commands of the viewport set (projection
    location or render mode, i.e. chosen in the menu of a viewport) over a
    viewport, recording the change of its view.
    @param command the id of the command, starting with `IDV_`
    @param viewport viewport to change
    @return true if the command was a viewport set one and was processed
    */
    bool processViewportCommand(const java::String& command,
                                Viewport* viewport);

    /**
    Undoes or redoes an operation of the scene history or of the view
    history of the selected viewport. A gesture whose release was lost is
    finished (recorded) first, so it is what gets undone.
    @param command the command to execute
    */
    void processUndoRedoCommand(UndoRedoCommand::Value command);
};

#endif
