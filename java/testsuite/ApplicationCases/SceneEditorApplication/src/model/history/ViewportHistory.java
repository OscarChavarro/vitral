package model.history;

import java.util.IdentityHashMap;
import java.util.Map;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.logging.Logger;
import vsdk.toolkit.gui.viewport.Viewport;
import vsdk.toolkit.gui.viewport.ViewportSet;

/**
Undo/redo histories of the views: one queue per viewport, holding the changes
of how that viewport shows the scene (see `ViewportStateOperation`).

User actions are recorded as in `SceneHistory`: `begin` captures every
viewport of a viewport set and `end` records, in the queue of each viewport
that changed, one operation. Brackets can be nested; only the outermost one
records.
*/
public class ViewportHistory
{
    private final Map<Viewport, UndoQueue> queues;
    private final IdentityHashMap<Viewport, ViewportState> before;
    private int depth;

    public ViewportHistory()
    {
        queues = new IdentityHashMap<Viewport, UndoQueue>();
        before = new IdentityHashMap<Viewport, ViewportState>();
        depth = 0;
    }

    /**
    @param viewport a viewport
    @return the queue of operations done over the view of the viewport
    (created empty the first time)
    */
    public UndoQueue getQueue(Viewport viewport)
    {
        return queues.computeIfAbsent(viewport, v -> new UndoQueue());
    }

    /**
    @return true if a user action is being recorded
    */
    public boolean isRecording()
    {
        return depth > 0;
    }

    /**
    Starts recording a user action over the viewports of a set.
    @param viewportSet the viewports that the action can change
    */
    public void begin(ViewportSet viewportSet)
    {
        if ( depth == 0 ) {
            before.clear();
            if ( viewportSet != null ) {
                for ( Viewport viewport : viewportSet.getViewports() ) {
                    before.put(viewport, ViewportState.capture(viewport));
                }
            }
        }
        depth++;
    }

    /**
    Ends recording a user action. If it is the outermost one, each viewport
    whose view changed since `begin` gets one operation in its queue, named
    after what changed (see `ViewportState.describeChangeTo`).
    @param mergeable true if the operation can be merged with the one of the
    next action over the same viewport
    @return number of viewports whose change was recorded
    */
    public int end(boolean mergeable)
    {
        int recorded = 0;

        if ( depth == 0 ) {
            Logger.reportMessage(this, VSDK.WARNING, "end",
                "Viewport history end() without begin(): the action is not recorded");
            return 0;
        }
        depth--;
        if ( depth > 0 ) {
            return 0;
        }
        for ( Map.Entry<Viewport, ViewportState> entry : before.entrySet() ) {
            ViewportState start = entry.getValue();
            ViewportState after = ViewportState.capture(entry.getKey());

            if ( !start.isSameState(after) ) {
                getQueue(entry.getKey()).record(new ViewportStateOperation(
                    start.describeChangeTo(after), start, after, mergeable));
                recorded++;
            }
        }
        before.clear();
        return recorded;
    }

    /**
    @param viewport a viewport
    @return the undone operation, or null if there was nothing to undo or an
    action is being recorded
    */
    public UndoableOperation undo(Viewport viewport)
    {
        if ( viewport == null || isRecording() ) {
            return null;
        }
        return getQueue(viewport).undo();
    }

    /**
    @param viewport a viewport
    @return the redone operation, or null if there was nothing to redo or an
    action is being recorded
    */
    public UndoableOperation redo(Viewport viewport)
    {
        if ( viewport == null || isRecording() ) {
            return null;
        }
        return getQueue(viewport).redo();
    }

    /**
    Forgets the histories of every viewport.
    */
    public void clear()
    {
        queues.clear();
        before.clear();
        depth = 0;
    }
}
