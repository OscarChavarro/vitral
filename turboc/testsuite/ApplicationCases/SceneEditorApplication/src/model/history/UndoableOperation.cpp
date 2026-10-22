#include "model/history/UndoableOperation.h"

const Class UndoableOperation::CLASS = { "UndoableOperation", NULL };

const Class *
UndoableOperation::getClass() const
{
    return &CLASS;
}

bool
UndoableOperation::instanceOf(const Class &type) const
{
    return type.isAssignableFrom(getClass());
}
