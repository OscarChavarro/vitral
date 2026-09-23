package model.history;

import vsdk.toolkit.gui.viewport.Viewport;

/**
Change of how a viewport shows the scene: camera movement, projection
location (perspective / parallel projections), render mode and display
settings. It keeps the states of the viewport before and after the change.

Operations done by the keyboard over the same viewport in a short time can be
merged (see `absorb`), so an auto-repeated camera key is a single step.
*/
public class ViewportStateOperation implements UndoableOperation
{
    /// Maximum time between two mergeable operations to be merged
    public static final long MERGE_INTERVAL_MILLISECONDS = 1000;

    private final String name;
    private final ViewportState before;
    private ViewportState after;
    private final boolean mergeable;
    private long lastChangeTime;

    /**
    @param name name of the user action
    @param before state of the viewport before the change
    @param after state of the same viewport after the change
    @param mergeable true if following operations over the same viewport can
    be merged into this one
    */
    public ViewportStateOperation(String name, ViewportState before,
                                  ViewportState after, boolean mergeable)
    {
        this.name = name;
        this.before = before;
        this.after = after;
        this.mergeable = mergeable;
        this.lastChangeTime = System.currentTimeMillis();
    }

    /**
    @return the changed viewport
    */
    public Viewport getViewport()
    {
        return before.getViewport();
    }

    @Override
    public void undo()
    {
        before.restore();
    }

    @Override
    public void redo()
    {
        after.restore();
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public boolean absorb(UndoableOperation next)
    {
        if ( !(next instanceof ViewportStateOperation other) ||
             !mergeable || !other.mergeable || !name.equals(other.name) ||
             other.getViewport() != getViewport() ||
             other.lastChangeTime - lastChangeTime > MERGE_INTERVAL_MILLISECONDS ) {
            return false;
        }
        after = other.after;
        lastChangeTime = other.lastChangeTime;
        return true;
    }
}
