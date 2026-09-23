#include "java/util/ArrayList.txx"
#include "model/history/CompositeOperation.h"

CompositeOperation::CompositeOperation(
    const java::String& name,
    const java::ArrayList<UndoableOperation*>& operations)
    : name(name), operations(operations)
{
}

CompositeOperation::~CompositeOperation()
{
    long i;
    for ( i = 0; i < operations.size(); i++ ) {
        delete operations.get(i);
    }
}

void CompositeOperation::undo()
{
    long i;

    for ( i = operations.size() - 1; i >= 0; i-- ) {
        operations.get(i)->undo();
    }
}

void CompositeOperation::redo()
{
    long i;

    for ( i = 0; i < operations.size(); i++ ) {
        operations.get(i)->redo();
    }
}

java::String CompositeOperation::getName() const
{
    return name;
}
