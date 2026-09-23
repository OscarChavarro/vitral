#include "java/util/ArrayList.txx"
#include "model/history/UndoQueue.h"
#include "model/history/UndoableOperation.h"

UndoQueue::UndoQueue() : capacity(DEFAULT_CAPACITY), cursor(0)
{
}

UndoQueue::UndoQueue(int capacity)
    : capacity(capacity > 1 ? capacity : 1), cursor(0)
{
}

UndoQueue::~UndoQueue()
{
    clear();
}

void UndoQueue::record(UndoableOperation* operation)
{
    if ( operation == nullptr ) {
        return;
    }
    while ( operations.size() > cursor ) {
        delete operations.get(operations.size() - 1);
        operations.remove((long)(operations.size() - 1));
    }
    if ( cursor > 0 && operations.get(cursor - 1)->absorb(operation) ) {
        delete operation;
        return;
    }
    operations.add(operation);
    cursor++;
    while ( operations.size() > capacity ) {
        delete operations.get(0);
        operations.remove((long)0);
        cursor--;
    }
}

bool UndoQueue::canUndo() const
{
    return cursor > 0;
}

bool UndoQueue::canRedo() const
{
    return cursor < operations.size();
}

UndoableOperation* UndoQueue::undo()
{
    UndoableOperation* operation;

    if ( !canUndo() ) {
        return nullptr;
    }
    cursor--;
    operation = operations.get(cursor);
    operation->undo();
    return operation;
}

UndoableOperation* UndoQueue::redo()
{
    UndoableOperation* operation;

    if ( !canRedo() ) {
        return nullptr;
    }
    operation = operations.get(cursor);
    operation->redo();
    cursor++;
    return operation;
}

java::String UndoQueue::getUndoName() const
{
    return canUndo() ? operations.get(cursor - 1)->getName() : java::String("");
}

java::String UndoQueue::getRedoName() const
{
    return canRedo() ? operations.get(cursor)->getName() : java::String("");
}

int UndoQueue::getUndoCount() const
{
    return cursor;
}

int UndoQueue::getRedoCount() const
{
    return (int)operations.size() - cursor;
}

void UndoQueue::clear()
{
    long i;
    for ( i = 0; i < operations.size(); i++ ) {
        delete operations.get(i);
    }
    operations.clear();
    cursor = 0;
}
