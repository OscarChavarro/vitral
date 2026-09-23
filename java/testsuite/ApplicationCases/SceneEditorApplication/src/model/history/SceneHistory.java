package model.history;

import java.util.function.Supplier;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.logging.Logger;

import model.Scene;

/**
Global undo/redo history of the scene: creation, deletion and placement
changes of bodies, lights, scene cameras and debug groups.

User actions are recorded by bracketing them with `begin` and `end`: the
scene is captured at `begin` and compared at `end`, and what changed becomes
one operation of the queue. Brackets can be nested (i.e. a key pressed during
a mouse drag): only the outermost one records, so the whole gesture is a
single step.
*/
public class SceneHistory
{
    private final Supplier<Scene> sceneSource;
    private final UndoQueue queue;
    private SceneSnapshot before;
    private int depth;

    /**
    @param sceneSource gives the scene currently edited
    */
    public SceneHistory(Supplier<Scene> sceneSource)
    {
        this.sceneSource = sceneSource;
        this.queue = new UndoQueue();
        this.before = null;
        this.depth = 0;
    }

    /**
    @return the queue of operations done over the scene
    */
    public UndoQueue getQueue()
    {
        return queue;
    }

    /**
    @return true if a user action is being recorded
    */
    public boolean isRecording()
    {
        return depth > 0;
    }

    /**
    Starts recording a user action over the scene.
    */
    public void begin()
    {
        if ( depth == 0 ) {
            Scene scene = sceneSource.get();

            before = scene == null ? null : SceneSnapshot.capture(scene);
        }
        depth++;
    }

    /**
    Ends recording a user action. If it is the outermost one, what changed in
    the scene since its `begin` is recorded as one operation.
    @param name name of the action, to be shown to the user
    @param mergeable true if its placement changes can be merged with the
    ones of the next action over the same elements (see
    `SceneTransformationOperation`)
    @return true if an operation was recorded
    */
    public boolean end(String name, boolean mergeable)
    {
        SceneSnapshot start;
        Scene scene;
        UndoableOperation operation;

        if ( depth == 0 ) {
            Logger.reportMessage(this, VSDK.WARNING, "end",
                "Scene history end() without begin(): the action \"" + name +
                "\" is not recorded");
            return false;
        }
        depth--;
        if ( depth > 0 ) {
            return false;
        }
        start = before;
        before = null;
        scene = sceneSource.get();
        if ( start == null || scene == null || start.getScene() != scene ) {
            // The whole scene was replaced: the action can not be undone
            return false;
        }
        operation = start.operationTo(SceneSnapshot.capture(scene), name, mergeable);
        if ( operation == null ) {
            return false;
        }
        queue.record(operation);
        return true;
    }

    /**
    Executes an action recording it as one operation.
    @param name name of the action
    @param action what changes the scene
    @return true if an operation was recorded
    */
    public boolean perform(String name, Runnable action)
    {
        begin();
        try {
            action.run();
        }
        catch ( RuntimeException e ) {
            // What the action did before failing is still recorded
            end(name, false);
            throw e;
        }
        return end(name, false);
    }

    /**
    @return the undone operation, or null if there was nothing to undo or an
    action is being recorded
    */
    public UndoableOperation undo()
    {
        if ( isRecording() ) {
            return null;
        }
        return queue.undo();
    }

    /**
    @return the redone operation, or null if there was nothing to redo or an
    action is being recorded
    */
    public UndoableOperation redo()
    {
        if ( isRecording() ) {
            return null;
        }
        return queue.redo();
    }

    /**
    Forgets the history (i.e. when the scene is replaced).
    */
    public void clear()
    {
        queue.clear();
        before = null;
        depth = 0;
    }
}
