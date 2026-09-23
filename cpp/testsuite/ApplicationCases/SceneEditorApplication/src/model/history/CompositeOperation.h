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
    java::String name;
    java::ArrayList<UndoableOperation*> operations;

    CompositeOperation(const CompositeOperation& other);
    CompositeOperation& operator=(const CompositeOperation& other);

public:
    /**
    @param name name of the user action
    @param operations operations in the order they were done; this
    operation takes their ownership
    */
    CompositeOperation(const java::String& name,
                       const java::ArrayList<UndoableOperation*>& operations);
    virtual ~CompositeOperation();

    virtual void undo() override;
    virtual void redo() override;
    virtual java::String getName() const override;
};

#endif
