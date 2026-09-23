#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/gui/viewport/ViewportSet.h"
#include "model/history/UndoQueue.h"
#include "model/history/ViewportHistory.h"
#include "model/history/ViewportState.h"
#include "model/history/ViewportStateOperation.h"

ViewportHistory::ViewportHistory() : depth(0)
{
}

ViewportHistory::~ViewportHistory()
{
    clear();
}

UndoQueue* ViewportHistory::getQueue(Viewport* viewport)
{
    long i;
    for ( i = 0; i < queueKeys.size(); i++ ) {
        if ( queueKeys.get(i) == viewport ) {
            return queues.get(i);
        }
    }
    UndoQueue* queue = new UndoQueue();
    queueKeys.add(viewport);
    queues.add(queue);
    return queue;
}

bool ViewportHistory::isRecording() const
{
    return depth > 0;
}

void ViewportHistory::begin(ViewportSet* viewportSet)
{
    if ( depth == 0 ) {
        clearBefore();
        if ( viewportSet != nullptr ) {
            const java::ArrayList<Viewport*>& viewports =
                viewportSet->getViewports();
            long i;
            for ( i = 0; i < viewports.size(); i++ ) {
                beforeKeys.add(viewports.get(i));
                before.add(ViewportState::capture(viewports.get(i)));
            }
        }
    }
    depth++;
}

int ViewportHistory::end(bool mergeable)
{
    int recorded = 0;
    long i;

    if ( depth == 0 ) {
        Logger::reportMessage("ViewportHistory", Logger::WARNING, "end",
            "Viewport history end() without begin(): the action is not recorded");
        return 0;
    }
    depth--;
    if ( depth > 0 ) {
        return 0;
    }
    for ( i = 0; i < beforeKeys.size(); i++ ) {
        ViewportState* start = before.get(i);
        ViewportState* after = ViewportState::capture(beforeKeys.get(i));

        if ( !start->isSameState(after) ) {
            getQueue(beforeKeys.get(i))->record(new ViewportStateOperation(
                start->describeChangeTo(after), start, after, mergeable));
            // The operation owns both states now
            before.set(i, nullptr);
            recorded++;
        }
        else {
            delete after;
        }
    }
    clearBefore();
    return recorded;
}

UndoableOperation* ViewportHistory::undo(Viewport* viewport)
{
    if ( viewport == nullptr || isRecording() ) {
        return nullptr;
    }
    return getQueue(viewport)->undo();
}

UndoableOperation* ViewportHistory::redo(Viewport* viewport)
{
    if ( viewport == nullptr || isRecording() ) {
        return nullptr;
    }
    return getQueue(viewport)->redo();
}

void ViewportHistory::clearBefore()
{
    long i;
    for ( i = 0; i < before.size(); i++ ) {
        delete before.get(i);
    }
    before.clear();
    beforeKeys.clear();
}

void ViewportHistory::clear()
{
    long i;
    for ( i = 0; i < queues.size(); i++ ) {
        delete queues.get(i);
    }
    queues.clear();
    queueKeys.clear();
    clearBefore();
    depth = 0;
}
