#ifndef __INTERACTION_EDIT_RECORDER__
#define __INTERACTION_EDIT_RECORDER__

#include <functional>

#include "java/lang/String.h"

class EditHistory;
class ViewportSet;

/**
Brackets the processing of vitral events by the interaction techniques, so
what each user action changes is recorded in the edition history: the scene
changes in the global history of the scene, the view changes in the history
of each changed viewport.

Two kinds of actions are recorded:

- Gestures: from a mouse press to its release (a drag of a gizmo, a camera
  movement). They are one step of the history, whatever the events in
  between do (keys included).
- Single actions: a key press, a click, a command of a menu.

A gesture whose release never came (i.e. consumed by a popup menu) is
finished by the next gesture, or before an undo or redo.
*/
class InteractionEditRecorder {
private:
    EditHistory* history;
    ViewportSet* viewportSet;
    bool gestureOpen;
    java::String gestureSceneOperationName;

public:
    /**
    @param history where the actions are recorded
    @param viewportSet viewports whose view changes are recorded
    */
    InteractionEditRecorder(EditHistory* history, ViewportSet* viewportSet);

    /**
    @return the recorded history
    */
    EditHistory* getHistory() const;

    /**
    @return true if a mouse gesture is being recorded
    */
    bool isGestureOpen() const;

    /**
    Starts recording a mouse gesture (at a mouse press), finishing first the
    one in course, if any.
    @param sceneOperationName name of the operation over the scene (the ones
    over the views are named after what changed)
    */
    void beginGesture(const java::String& sceneOperationName);

    /**
    Ends recording the mouse gesture in course (at a mouse release). Nothing
    is done if there is none.
    */
    void endGesture();

    /**
    Starts recording a single action. Inside a gesture, the action becomes
    part of the gesture.
    */
    void beginAction();

    /**
    Ends recording a single action.
    @param sceneOperationName name of the operation over the scene
    @param mergeable true if the operations can be merged with the ones of
    the next action over the same things (keyboard actions)
    */
    void endAction(const java::String& sceneOperationName, bool mergeable);

    /**
    Executes an action recording it.
    @param sceneOperationName name of the operation over the scene
    @param action what the user does
    */
    void record(const java::String& sceneOperationName,
                const std::function<void()>& action);
};

#endif
