package model.history;

import java.util.ArrayList;

/**
History of the operations done over something (the scene, or the view of a
viewport), to undo and redo them. It is a list with a cursor: the operations
before the cursor are done (the last one is the next to undo), the ones after
it were undone (the first one is the next to redo). Recording a new operation
discards the undone ones, and the oldest operations are forgotten when the
capacity is exceeded.
*/
public class UndoQueue
{
    /// Default maximum number of operations kept
    public static final int DEFAULT_CAPACITY = 256;

    private final ArrayList<UndoableOperation> operations;
    private final int capacity;
    /// Number of operations currently done
    private int cursor;

    public UndoQueue()
    {
        this(DEFAULT_CAPACITY);
    }

    /**
    @param capacity maximum number of operations kept (at least 1)
    */
    public UndoQueue(int capacity)
    {
        this.operations = new ArrayList<UndoableOperation>();
        this.capacity = Math.max(1, capacity);
        this.cursor = 0;
    }

    /**
    Adds an operation already done by the user. The undone operations are
    discarded: a new edition starts a new branch of the history.
    @param operation operation to record; null is ignored
    */
    public void record(UndoableOperation operation)
    {
        if ( operation == null ) {
            return;
        }
        while ( operations.size() > cursor ) {
            operations.remove(operations.size() - 1);
        }
        if ( cursor > 0 && operations.get(cursor - 1).absorb(operation) ) {
            return;
        }
        operations.add(operation);
        cursor++;
        while ( operations.size() > capacity ) {
            operations.remove(0);
            cursor--;
        }
    }

    public boolean canUndo()
    {
        return cursor > 0;
    }

    public boolean canRedo()
    {
        return cursor < operations.size();
    }

    /**
    Reverts the last done operation.
    @return the undone operation, or null if there was nothing to undo
    */
    public UndoableOperation undo()
    {
        UndoableOperation operation;

        if ( !canUndo() ) {
            return null;
        }
        cursor--;
        operation = operations.get(cursor);
        operation.undo();
        return operation;
    }

    /**
    Applies again the last undone operation.
    @return the redone operation, or null if there was nothing to redo
    */
    public UndoableOperation redo()
    {
        UndoableOperation operation;

        if ( !canRedo() ) {
            return null;
        }
        operation = operations.get(cursor);
        operation.redo();
        cursor++;
        return operation;
    }

    /**
    @return the name of the next operation to undo, or null if there is none
    */
    public String getUndoName()
    {
        return canUndo() ? operations.get(cursor - 1).getName() : null;
    }

    /**
    @return the name of the next operation to redo, or null if there is none
    */
    public String getRedoName()
    {
        return canRedo() ? operations.get(cursor).getName() : null;
    }

    /**
    @return number of operations that can be undone
    */
    public int getUndoCount()
    {
        return cursor;
    }

    /**
    @return number of operations that can be redone
    */
    public int getRedoCount()
    {
        return operations.size() - cursor;
    }

    /**
    Forgets every operation.
    */
    public void clear()
    {
        operations.clear();
        cursor = 0;
    }
}
