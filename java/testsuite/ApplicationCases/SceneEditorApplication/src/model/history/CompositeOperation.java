package model.history;

import java.util.ArrayList;
import java.util.List;

/**
Several operations done by a single user action (i.e. a command that
deletes some things and moves others), undone and redone as one step.
*/
public class CompositeOperation implements UndoableOperation
{
    private final String name;
    private final ArrayList<UndoableOperation> operations;

    /**
    @param name name of the user action
    @param operations operations in the order they were done
    */
    public CompositeOperation(String name, List<UndoableOperation> operations)
    {
        this.name = name;
        this.operations = new ArrayList<UndoableOperation>(operations);
    }

    @Override
    public void undo()
    {
        int i;

        for ( i = operations.size() - 1; i >= 0; i-- ) {
            operations.get(i).undo();
        }
    }

    @Override
    public void redo()
    {
        for ( UndoableOperation operation : operations ) {
            operation.redo();
        }
    }

    @Override
    public String getName()
    {
        return name;
    }
}
