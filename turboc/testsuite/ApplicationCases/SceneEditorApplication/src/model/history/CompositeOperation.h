#ifndef __COMPOSITE_OPERATION__
#define __COMPOSITE_OPERATION__

#include "java/util/ArrayList.h"
#include "model/history/UndoableOperation.h"

/**
Several operations done by a single user action (i.e. a command that
deletes some things and moves others), undone and redone as one step. It
owns its operations.
*/
class CompositeOperation : public UndoableOperation {
private:
    String name;
    ArrayList<UndoableOperation*> operations;

    CompositeOperation(const CompositeOperation& other);
    CompositeOperation& operator=(const CompositeOperation& other);

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    /**
    @param name name of the user action
    @param operations operations in the order they were done; this
    operation takes their ownership
    */
    CompositeOperation(const String& name,
                       const ArrayList<UndoableOperation*>& operations);
    virtual ~CompositeOperation();

    virtual void undo();
    virtual void redo();
    virtual String getName() const;
};

#endif
