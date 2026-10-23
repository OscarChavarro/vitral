#include "vsdk/toolkit/common/VSDK.h"
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
    if ( operation == NULL ) {
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

BOOLEAN UndoQueue::canUndo() const
{
    return cursor > 0;
}

BOOLEAN UndoQueue::canRedo() const
{
    return cursor < operations.size();
}

UndoableOperation* UndoQueue::undo()
{
    UndoableOperation* operation;

    if ( !canUndo() ) {
        return NULL;
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
        return NULL;
    }
    operation = operations.get(cursor);
    operation->redo();
    cursor++;
    return operation;
}

String UndoQueue::getUndoName() const
{
    return canUndo() ? operations.get(cursor - 1)->getName() : String("");
}

String UndoQueue::getRedoName() const
{
    return canRedo() ? operations.get(cursor)->getName() : String("");
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

// Integral constants bound to references (i.e. by `?:`) need a
// definition out of the class in C++98
