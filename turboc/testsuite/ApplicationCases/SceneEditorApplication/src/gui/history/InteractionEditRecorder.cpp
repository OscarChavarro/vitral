#include "vsdk/toolkit/common/VSDK.h"
#include "model/history/EditHistory.h"
#include "gui/history/InteractionEditRecorder.h"

InteractionEditRecorder::InteractionEditRecorder(EditHistory* history,
                                                 ViewportSet* viewportSet)
    : history(history), viewportSet(viewportSet), gestureOpen(FALSE)
{
}

EditHistory* InteractionEditRecorder::getHistory() const
{
    return history;
}

BOOLEAN InteractionEditRecorder::isGestureOpen() const
{
    return gestureOpen;
}

void InteractionEditRecorder::beginGesture(
    const String& sceneOperationName)
{
    endGesture();
    gestureSceneOperationName = sceneOperationName;
    beginAction();
    gestureOpen = TRUE;
}

void InteractionEditRecorder::endGesture()
{
    if ( !gestureOpen ) {
        return;
    }
    gestureOpen = FALSE;
    endAction(gestureSceneOperationName, FALSE);
}

void InteractionEditRecorder::beginAction()
{
    history->getSceneHistory()->begin();
    history->getViewportHistory()->begin(viewportSet);
}

void InteractionEditRecorder::endAction(
    const String& sceneOperationName, BOOLEAN mergeable)
{
    history->getSceneHistory()->end(sceneOperationName, mergeable);
    history->getViewportHistory()->end(mergeable);
}

void InteractionEditRecorder::record(const String& sceneOperationName,
                                     Runnable* action)
{
    beginAction();
    try {
        action->run();
    }
    catch ( ... ) {
        endAction(sceneOperationName, FALSE);
        throw;
    }
    endAction(sceneOperationName, FALSE);
}
