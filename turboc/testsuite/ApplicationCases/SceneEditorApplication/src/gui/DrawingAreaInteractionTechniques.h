#ifndef __DRAWING_AREA_INTERACTION_TECHNIQUES__
#define __DRAWING_AREA_INTERACTION_TECHNIQUES__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Runnable.h"
#include "java/util/function/Consumer.h"
#include "java/util/function/Function.h"
#include "java/util/function/Supplier.h"

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
class RayGizmoInteractionTechniques;
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
    /**
    The technique of a gizmo that processes a mouse event, once the gizmo
    is placed for the viewport (a lambda in the C++11 port).
    */
    class MouseTechnique {
    public:
        enum Kind {
            ROTATION_PRESSED, SCALE_PRESSED,
            TRANSLATION_RELEASED, ROTATION_RELEASED, SCALE_RELEASED,
            TRANSLATION_CLICKED, ROTATION_CLICKED, SCALE_CLICKED,
            TRANSLATION_MOVED, ROTATION_MOVED, SCALE_MOVED,
            TRANSLATION_DRAGGED, ROTATION_DRAGGED, SCALE_DRAGGED
        };

        /**
        @param viewport viewport given to the pressed techniques
        @param body body that follows the rotation and scale gizmos while
        they are dragged, or null
        */
        MouseTechnique(DrawingAreaInteractionTechniques* owner, Kind kind,
                       Viewport* viewport = NULL, SimpleBody* body = NULL)
            : owner(owner), kind(kind), viewport(viewport), body(body) {}

        /**
        @return true if the gizmo changed
        */
        BOOLEAN apply(const MouseEvent& e) const;

    private:
        DrawingAreaInteractionTechniques* owner;
        Kind kind;
        Viewport* viewport;
        SimpleBody* body;
    };
    friend class MouseTechnique;

    /// A click, recorded in the edit history (a lambda in the C++11 port)
    class ClickRecording : public Runnable {
    public:
        ClickRecording(DrawingAreaInteractionTechniques* owner,
                       const MouseEvent& event)
            : owner(owner), event(event) {}
        virtual void run();
    private:
        DrawingAreaInteractionTechniques* owner;
        const MouseEvent& event;
    };
    friend class ClickRecording;

    DrawingArea* drawingArea;
    ApplicationModel* model;
    Scene* scene;
    ViewportSet* viewportSet;
    SceneSelectionEditor* selectionEditor;
    ScenePicker* scenePicker;
    ViewportInteractionTechniques* interactionTechniques;
    ViewportSetInteractionTechniques* viewportSetTechniques;
    RayGizmoInteractionTechniques* rayGizmoTechniques;
    SelectedBodyMapToggles* mapToggles;
    TranslateGizmo* translationGizmo;
    RotateGizmo* rotateGizmo;
    ScaleGizmo* scaleGizmo;
    DrawingAreaInteractionListener* listener;
    InteractionEditRecorder* editRecorder;
    UndoRedoInteractionTechnique* undoRedoTechnique;

    RendererConfiguration* qualitySelection;
    /// A viewport title belongs to the HUD, never to the camera controller.
    BOOLEAN viewportTitleGesture;
    /// Suppresses the synthetic click following a title press/release.
    BOOLEAN viewportTitleClickConsumed;

    //= Pointer shape =====================================================
    PointerCursor::Value getModeCursor();
    void requestCursorForPointer(const MouseEvent& event,
                                 PointerCursor::Value cursor);

    //= Mouse =============================================================
    Viewport* getViewportAtPointer(const MouseEvent& event);
    Viewport* getInteractionViewportAtPointer(const MouseEvent& event);
    Viewport* getGestureViewport();
    BOOLEAN isTranslationGestureConfined();
    MouseEvent toViewportEvent(const MouseEvent& event, Viewport* viewport);
    void wrapCursorIfRequested(Viewport* viewport);
    BOOLEAN processTranslationEvent(const MouseEvent& event, Viewport* viewport,
                                 const Vector3Dd& centroid,
                                 const MouseTechnique& technique);
    BOOLEAN processRotationEvent(const MouseEvent& event, Viewport* viewport,
                              const MouseTechnique& technique);
    BOOLEAN processScaleEvent(const MouseEvent& event, Viewport* viewport,
                           const MouseTechnique& technique);
    void cancelInputGizmoEditing();
    void applyRotationGizmoToBody(SimpleBody* body);
    void applyScaleGizmoToBody(SimpleBody* body);
    void processMouseClickedEventWithoutRecording(const MouseEvent& event);

    //= Keyboard ==========================================================
    void processEditionKeyPressedEvent(const KeyEvent& event);
    BOOLEAN processInputGizmoKeyPressedEvent(InteractionMode mode,
                                          const KeyEvent& event);
    BOOLEAN processRotationInputGizmoKeyPressedEvent(const KeyEvent& event);
    BOOLEAN processScaleInputGizmoKeyPressedEvent(const KeyEvent& event);
    BOOLEAN processTranslationInputGizmoKeyPressedEvent(const KeyEvent& event);
    void processModeKeyPressedEvent(InteractionMode mode,
                                    const KeyEvent& event);
    void processGlobalKeyPressedEvent(const KeyEvent& event);
    void processCharacterKeyPressedEvent(const KeyEvent& event);
    void switchMode(InteractionMode mode, const String& message);
    void printBodyNames();
    static BOOLEAN isControlLetterChord(const KeyEvent& event);
    static String sceneOperationName(InteractionMode mode);
    static String keyOperationName(InteractionMode mode,
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
    void setCursorWrapEnabled(BOOLEAN enabled);

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
    BOOLEAN processViewportCommand(const String& command,
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
