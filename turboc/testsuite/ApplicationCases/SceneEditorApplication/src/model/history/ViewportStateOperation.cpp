#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/System.h"
#include "model/history/ViewportState.h"
#include "model/history/ViewportStateOperation.h"

ViewportStateOperation::ViewportStateOperation(const String& name,
                                               ViewportState* before,
                                               ViewportState* after,
                                               BOOLEAN mergeable)
    : name(name), before(before), after(after), mergeable(mergeable),
      lastChangeTime(System::currentTimeMillis())
{
}

ViewportStateOperation::~ViewportStateOperation()
{
    delete before;
    delete after;
}

Viewport* ViewportStateOperation::getViewport() const
{
    return before->getViewport();
}

void ViewportStateOperation::undo()
{
    before->restore();
}

void ViewportStateOperation::redo()
{
    after->restore();
}

String ViewportStateOperation::getName() const
{
    return name;
}

BOOLEAN ViewportStateOperation::absorb(UndoableOperation* next)
{
    ViewportStateOperation* other =
        (next != NULL && next->instanceOf(ViewportStateOperation::CLASS) ? (ViewportStateOperation*)next : NULL);

    if ( other == NULL ||
         !mergeable || !other->mergeable || !name.equals(other->name) ||
         other->getViewport() != getViewport() ||
         other->lastChangeTime - lastChangeTime > MERGE_INTERVAL_MILLISECONDS ) {
        return FALSE;
    }
    // Takes the state after the other operation; the old one goes to the
    // other operation, deleted by the caller
    ViewportState* oldAfter = after;
    after = other->after;
    other->after = oldAfter;
    lastChangeTime = other->lastChangeTime;
    return TRUE;
}

// Integral constants bound to references (i.e. by `?:`) need a
// definition out of the class in C++98
const long long ViewportStateOperation::MERGE_INTERVAL_MILLISECONDS = 1000;

const Class ViewportStateOperation::CLASS = { "ViewportStateOperation", &UndoableOperation::CLASS };

const Class *
ViewportStateOperation::getClass() const
{
    return &CLASS;
}
