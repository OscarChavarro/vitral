package model.history;

import java.util.ArrayList;
import java.util.List;

import vsdk.toolkit.common.Entity;

import model.Scene;

/**
Creation and deletion of elements of one kind (bodies, lights, cameras or
debug groups) of a scene. The elements themselves are kept, so undoing a
deletion puts back the very same objects, at their former positions in the
list and with their former selection state.

Undo goes from the list after the operation to the list before it: the
inserted elements are removed (from the last position to the first one) and
then the removed elements are inserted again (from the first position to the
last one). Redo does the opposite.
*/
public class SceneMembershipOperation implements UndoableOperation
{
    /**
    An element of the list and its position and selection state.
    @param index position of the element in the list
    @param element the element
    @param selected true if the element was selected
    */
    public record Entry(int index, Entity element, boolean selected)
    {
    }

    private final String name;
    private final Scene scene;
    private final SceneElementKind kind;
    /// Elements not present after the operation, with their positions before it
    private final ArrayList<Entry> removed;
    /// Elements not present before the operation, with their positions after it
    private final ArrayList<Entry> inserted;

    /**
    @param name name of the user action
    @param scene scene holding the elements
    @param kind kind of the elements
    @param removed elements deleted by the operation, sorted by increasing
    position in the list before the operation
    @param inserted elements created by the operation, sorted by increasing
    position in the list after the operation
    */
    public SceneMembershipOperation(String name, Scene scene, SceneElementKind kind,
                                    List<Entry> removed, List<Entry> inserted)
    {
        this.name = name;
        this.scene = scene;
        this.kind = kind;
        this.removed = new ArrayList<Entry>(removed);
        this.inserted = new ArrayList<Entry>(inserted);
    }

    @Override
    public void undo()
    {
        apply(inserted, removed);
    }

    @Override
    public void redo()
    {
        apply(removed, inserted);
    }

    private void apply(ArrayList<Entry> toRemove, ArrayList<Entry> toInsert)
    {
        int i;

        for ( i = toRemove.size() - 1; i >= 0; i-- ) {
            kind.remove(scene, toRemove.get(i).index());
        }
        for ( Entry entry : toInsert ) {
            kind.insert(scene, entry.index(), entry.element(), entry.selected());
        }
    }

    @Override
    public String getName()
    {
        return name;
    }

    /**
    @return kind of the elements created or deleted
    */
    public SceneElementKind getKind()
    {
        return kind;
    }

    /**
    @return number of elements deleted by the operation
    */
    public int getRemovedCount()
    {
        return removed.size();
    }

    /**
    @return number of elements created by the operation
    */
    public int getInsertedCount()
    {
        return inserted.size();
    }
}
