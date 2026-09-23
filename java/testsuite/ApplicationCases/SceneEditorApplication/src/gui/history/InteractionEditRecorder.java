package gui.history;

import vsdk.toolkit.gui.viewport.ViewportSet;

import model.history.EditHistory;

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
public class InteractionEditRecorder
{
    private final EditHistory history;
    private final ViewportSet viewportSet;
    private boolean gestureOpen;
    private String gestureSceneOperationName;

    /**
    @param history where the actions are recorded
    @param viewportSet viewports whose view changes are recorded
    */
    public InteractionEditRecorder(EditHistory history, ViewportSet viewportSet)
    {
        this.history = history;
        this.viewportSet = viewportSet;
        this.gestureOpen = false;
    }

    /**
    @return the recorded history
    */
    public EditHistory getHistory()
    {
        return history;
    }

    /**
    @return true if a mouse gesture is being recorded
    */
    public boolean isGestureOpen()
    {
        return gestureOpen;
    }

    /**
    Starts recording a mouse gesture (at a mouse press), finishing first the
    one in course, if any.
    @param sceneOperationName name of the operation over the scene (the ones
    over the views are named after what changed)
    */
    public void beginGesture(String sceneOperationName)
    {
        endGesture();
        gestureSceneOperationName = sceneOperationName;
        beginAction();
        gestureOpen = true;
    }

    /**
    Ends recording the mouse gesture in course (at a mouse release). Nothing
    is done if there is none.
    */
    public void endGesture()
    {
        if ( !gestureOpen ) {
            return;
        }
        gestureOpen = false;
        endAction(gestureSceneOperationName, false);
    }

    /**
    Starts recording a single action. Inside a gesture, the action becomes
    part of the gesture.
    */
    public void beginAction()
    {
        history.getSceneHistory().begin();
        history.getViewportHistory().begin(viewportSet);
    }

    /**
    Ends recording a single action.
    @param sceneOperationName name of the operation over the scene
    @param mergeable true if the operations can be merged with the ones of
    the next action over the same things (keyboard actions)
    */
    public void endAction(String sceneOperationName, boolean mergeable)
    {
        history.getSceneHistory().end(sceneOperationName, mergeable);
        history.getViewportHistory().end(mergeable);
    }

    /**
    Executes an action recording it.
    @param sceneOperationName name of the operation over the scene
    @param action what the user does
    */
    public void record(String sceneOperationName, Runnable action)
    {
        beginAction();
        try {
            action.run();
        }
        finally {
            endAction(sceneOperationName, false);
        }
    }
}
