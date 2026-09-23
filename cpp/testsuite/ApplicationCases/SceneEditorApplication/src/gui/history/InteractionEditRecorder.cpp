#include "model/history/EditHistory.h"
#include "gui/history/InteractionEditRecorder.h"

InteractionEditRecorder::InteractionEditRecorder(EditHistory* history,
                                                 ViewportSet* viewportSet)
    : history(history), viewportSet(viewportSet), gestureOpen(false)
{
}

EditHistory* InteractionEditRecorder::getHistory() const
{
    return history;
}

bool InteractionEditRecorder::isGestureOpen() const
{
    return gestureOpen;
}

void InteractionEditRecorder::beginGesture(
    const java::String& sceneOperationName)
{
    endGesture();
    gestureSceneOperationName = sceneOperationName;
    beginAction();
    gestureOpen = true;
}

void InteractionEditRecorder::endGesture()
{
    if ( !gestureOpen ) {
        return;
    }
    gestureOpen = false;
    endAction(gestureSceneOperationName, false);
}

void InteractionEditRecorder::beginAction()
{
    history->getSceneHistory()->begin();
    history->getViewportHistory()->begin(viewportSet);
}

void InteractionEditRecorder::endAction(
    const java::String& sceneOperationName, bool mergeable)
{
    history->getSceneHistory()->end(sceneOperationName, mergeable);
    history->getViewportHistory()->end(mergeable);
}

void InteractionEditRecorder::record(const java::String& sceneOperationName,
                                     const std::function<void()>& action)
{
    beginAction();
    try {
        action();
    }
    catch ( ... ) {
        endAction(sceneOperationName, false);
        throw;
    }
    endAction(sceneOperationName, false);
}
