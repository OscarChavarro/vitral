#include "java/lang/System.h"
#include "model/history/ViewportState.h"
#include "model/history/ViewportStateOperation.h"

ViewportStateOperation::ViewportStateOperation(const java::String& name,
                                               ViewportState* before,
                                               ViewportState* after,
                                               bool mergeable)
    : name(name), before(before), after(after), mergeable(mergeable),
      lastChangeTime(java::System::currentTimeMillis())
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

java::String ViewportStateOperation::getName() const
{
    return name;
}

bool ViewportStateOperation::absorb(UndoableOperation* next)
{
    ViewportStateOperation* other =
        dynamic_cast<ViewportStateOperation*>(next);

    if ( other == nullptr ||
         !mergeable || !other->mergeable || !name.equals(other->name) ||
         other->getViewport() != getViewport() ||
         other->lastChangeTime - lastChangeTime > MERGE_INTERVAL_MILLISECONDS ) {
        return false;
    }
    // Takes the state after the other operation; the old one goes to the
    // other operation, deleted by the caller
    ViewportState* oldAfter = after;
    after = other->after;
    other->after = oldAfter;
    lastChangeTime = other->lastChangeTime;
    return true;
}
