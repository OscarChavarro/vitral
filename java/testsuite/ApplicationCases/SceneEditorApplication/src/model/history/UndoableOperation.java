package model.history;

/**
An edition done by the user that can be undone and done again (Command
pattern). Operations are kept, in the order they were done, by an
`UndoQueue`. They are plain model objects: they do not depend on any GUI or
rendering technology.
*/
public interface UndoableOperation
{
    /**
    Reverts the effect of the operation.
    */
    void undo();

    /**
    Applies again the effect of the operation, after an `undo`.
    */
    void redo();

    /**
    @return short name of the operation, to be shown to the user
    */
    String getName();

    /**
    Absorbs an operation recorded just after this one, so both become a single
    step of the history (i.e. the key presses of an auto-repeated key moving
    the same camera). By default operations absorb nothing.
    @param next operation recorded just after this one
    @return true if `next` was absorbed into this operation, so it must not be
    recorded
    */
    default boolean absorb(UndoableOperation next)
    {
        return false;
    }
}
