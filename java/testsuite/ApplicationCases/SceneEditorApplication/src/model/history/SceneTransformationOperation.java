package model.history;

import java.util.ArrayList;
import java.util.List;

/**
Change of the placement (translation, rotation, scale) of some elements of a
scene: bodies, lights or cameras. It keeps the placements before and after
the change of each changed element.

Operations done by the keyboard over the same elements in a short time can
be merged (see `absorb`), so an auto-repeated key is a single step.
*/
public class SceneTransformationOperation implements UndoableOperation
{
    /// Maximum time between two mergeable operations to be merged
    public static final long MERGE_INTERVAL_MILLISECONDS = 1000;

    private final String name;
    private final ArrayList<EntityTransformState> before;
    private ArrayList<EntityTransformState> after;
    private final boolean mergeable;
    private long lastChangeTime;

    /**
    @param name name of the user action
    @param before placements before the change, one per changed element
    @param after placements after the change, in the same order
    @param mergeable true if following operations over the same elements
    can be merged into this one
    */
    public SceneTransformationOperation(String name,
                                        List<EntityTransformState> before,
                                        List<EntityTransformState> after,
                                        boolean mergeable)
    {
        this.name = name;
        this.before = new ArrayList<EntityTransformState>(before);
        this.after = new ArrayList<EntityTransformState>(after);
        this.mergeable = mergeable;
        this.lastChangeTime = System.currentTimeMillis();
    }

    @Override
    public void undo()
    {
        for ( EntityTransformState state : before ) {
            state.restore();
        }
    }

    @Override
    public void redo()
    {
        for ( EntityTransformState state : after ) {
            state.restore();
        }
    }

    @Override
    public String getName()
    {
        return name;
    }

    /**
    @return number of entities whose placement changed
    */
    public int getEntityCount()
    {
        return before.size();
    }

    @Override
    public boolean absorb(UndoableOperation next)
    {
        if ( !(next instanceof SceneTransformationOperation other) ||
             !mergeable || !other.mergeable || !name.equals(other.name) ||
             other.lastChangeTime - lastChangeTime > MERGE_INTERVAL_MILLISECONDS ||
             !sameElements(other) ) {
            return false;
        }
        after = other.after;
        lastChangeTime = other.lastChangeTime;
        return true;
    }

    private boolean sameElements(SceneTransformationOperation other)
    {
        int i;

        if ( other.before.size() != before.size() ) {
            return false;
        }
        for ( i = 0; i < before.size(); i++ ) {
            if ( other.before.get(i).getEntity() != before.get(i).getEntity() ) {
                return false;
            }
        }
        return true;
    }
}
