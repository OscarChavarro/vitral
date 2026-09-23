#include "vsdk/toolkit/common/logging/Logger.h"
#include "model/history/SceneHistory.h"
#include "model/history/SceneSnapshot.h"
#include "model/history/UndoableOperation.h"

SceneHistory::SceneHistory(const std::function<Scene*()>& sceneSource)
    : sceneSource(sceneSource), before(nullptr), depth(0)
{
}

SceneHistory::~SceneHistory()
{
    delete before;
}

UndoQueue* SceneHistory::getQueue()
{
    return &queue;
}

bool SceneHistory::isRecording() const
{
    return depth > 0;
}

void SceneHistory::begin()
{
    if ( depth == 0 ) {
        Scene* scene = sceneSource ? sceneSource() : nullptr;

        delete before;
        before = scene == nullptr ? nullptr : SceneSnapshot::capture(scene);
    }
    depth++;
}

bool SceneHistory::end(const java::String& name, bool mergeable)
{
    SceneSnapshot* start;
    Scene* scene;
    UndoableOperation* operation;

    if ( depth == 0 ) {
        Logger::reportMessage("SceneHistory", Logger::WARNING, "end",
            java::String("Scene history end() without begin(): the action \"") +
            name + "\" is not recorded");
        return false;
    }
    depth--;
    if ( depth > 0 ) {
        return false;
    }
    start = before;
    before = nullptr;
    scene = sceneSource ? sceneSource() : nullptr;
    if ( start == nullptr || scene == nullptr || start->getScene() != scene ) {
        // The whole scene was replaced: the action can not be undone
        delete start;
        return false;
    }
    SceneSnapshot* after = SceneSnapshot::capture(scene);
    operation = start->operationTo(after, name, mergeable);
    delete after;
    delete start;
    if ( operation == nullptr ) {
        return false;
    }
    queue.record(operation);
    return true;
}

bool SceneHistory::perform(const java::String& name,
                           const std::function<void()>& action)
{
    begin();
    try {
        action();
    }
    catch ( ... ) {
        // What the action did before failing is still recorded
        end(name, false);
        throw;
    }
    return end(name, false);
}

UndoableOperation* SceneHistory::undo()
{
    if ( isRecording() ) {
        return nullptr;
    }
    return queue.undo();
}

UndoableOperation* SceneHistory::redo()
{
    if ( isRecording() ) {
        return nullptr;
    }
    return queue.redo();
}

void SceneHistory::clear()
{
    queue.clear();
    delete before;
    before = nullptr;
    depth = 0;
}
