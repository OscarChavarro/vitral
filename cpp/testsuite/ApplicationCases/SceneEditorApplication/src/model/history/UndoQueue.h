#ifndef __UNDO_QUEUE__
#define __UNDO_QUEUE__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"

class UndoableOperation;

/**
History of the operations done over something (the scene, or the view of a
viewport), to undo and redo them. It is a list with a cursor: the operations
before the cursor are done (the last one is the next to undo), the ones after
it were undone (the first one is the next to redo). Recording a new operation
discards the undone ones, and the oldest operations are forgotten when the
capacity is exceeded. The queue owns its operations.
*/
class UndoQueue {
public:
    /// Default maximum number of operations kept
    static const int DEFAULT_CAPACITY = 256;

private:
    java::ArrayList<UndoableOperation*> operations;
    int capacity;
    /// Number of operations currently done
    int cursor;

    UndoQueue(const UndoQueue& other);
    UndoQueue& operator=(const UndoQueue& other);

public:
    UndoQueue();

    /**
    @param capacity maximum number of operations kept (at least 1)
    */
    explicit UndoQueue(int capacity);
    virtual ~UndoQueue();

    /**
    Adds an operation already done by the user, taking its ownership. The
    undone operations are discarded: a new edition starts a new branch of
    the history.
    @param operation operation to record; null is ignored
    */
    void record(UndoableOperation* operation);
    bool canUndo() const;
    bool canRedo() const;

    /**
    Reverts the last done operation.
    @return the undone operation (still owned by the queue), or null if
    there was nothing to undo
    */
    UndoableOperation* undo();

    /**
    Applies again the last undone operation.
    @return the redone operation (still owned by the queue), or null if
    there was nothing to redo
    */
    UndoableOperation* redo();

    /**
    @return the name of the next operation to undo, or an empty string if
    there is none
    */
    java::String getUndoName() const;

    /**
    @return the name of the next operation to redo, or an empty string if
    there is none
    */
    java::String getRedoName() const;
    int getUndoCount() const;
    int getRedoCount() const;

    /**
    Forgets every operation.
    */
    void clear();
};

#endif
