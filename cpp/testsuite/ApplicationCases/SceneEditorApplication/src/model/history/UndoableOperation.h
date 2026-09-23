#ifndef __UNDOABLE_OPERATION__
#define __UNDOABLE_OPERATION__

#include "java/lang/String.h"

/**
An edition done by the user that can be undone and done again (Command
pattern). Operations are kept, in the order they were done, by an
`UndoQueue`. They are plain model objects: they do not depend on any GUI or
rendering technology.
*/
class UndoableOperation {
public:
    virtual ~UndoableOperation() {}

    /**
    Reverts the effect of the operation.
    */
    virtual void undo() = 0;

    /**
    Applies again the effect of the operation, after an `undo`.
    */
    virtual void redo() = 0;

    /**
    @return short name of the operation, to be shown to the user
    */
    virtual java::String getName() const = 0;

    /**
    Absorbs an operation recorded just after this one, so both become a
    single step of the history (i.e. the key presses of an auto-repeated key
    moving the same camera). By default operations absorb nothing.
    @param next operation recorded just after this one; when it is absorbed,
    this operation may take data from it, and the caller deletes it
    @return true if `next` was absorbed into this operation, so it must not
    be recorded
    */
    virtual bool absorb(UndoableOperation* /*next*/)
    {
        return false;
    }
};

#endif
