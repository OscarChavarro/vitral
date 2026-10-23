#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "model/history/SceneHistory.h"
#include "model/history/SceneSnapshot.h"
#include "model/history/UndoableOperation.h"

SceneHistory::SceneHistory(Supplier<Scene*>* sceneSource)
    : sceneSource(sceneSource), before(NULL), depth(0)
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

BOOLEAN SceneHistory::isRecording() const
{
    return depth > 0;
}

void SceneHistory::begin()
{
    if ( depth == 0 ) {
        Scene* scene = sceneSource != NULL ? sceneSource->get() : NULL;

        delete before;
        before = scene == NULL ? NULL : SceneSnapshot::capture(scene);
    }
    depth++;
}

BOOLEAN SceneHistory::end(const String& name, BOOLEAN mergeable)
{
    SceneSnapshot* start;
    Scene* scene;
    UndoableOperation* operation;

    if ( depth == 0 ) {
        Logger::reportMessage("SceneHistory", Logger::WARNING, "end",
            String("Scene history end() without begin(): the action \"") +
            name + "\" is not recorded");
        return FALSE;
    }
    depth--;
    if ( depth > 0 ) {
        return FALSE;
    }
    start = before;
    before = NULL;
    scene = sceneSource != NULL ? sceneSource->get() : NULL;
    if ( start == NULL || scene == NULL || start->getScene() != scene ) {
        // The whole scene was replaced: the action can not be undone
        delete start;
        return FALSE;
    }
    SceneSnapshot* after = SceneSnapshot::capture(scene);
    operation = start->operationTo(after, name, mergeable);
    delete after;
    delete start;
    if ( operation == NULL ) {
        return FALSE;
    }
    queue.record(operation);
    return TRUE;
}

BOOLEAN SceneHistory::perform(const String& name, Runnable* action)
{
    begin();
    try {
        action->run();
    }
    catch ( ... ) {
        // What the action did before failing is still recorded
        end(name, FALSE);
        throw;
    }
    return end(name, FALSE);
}

UndoableOperation* SceneHistory::undo()
{
    if ( isRecording() ) {
        return NULL;
    }
    return queue.undo();
}

UndoableOperation* SceneHistory::redo()
{
    if ( isRecording() ) {
        return NULL;
    }
    return queue.redo();
}

void SceneHistory::clear()
{
    queue.clear();
    delete before;
    before = NULL;
    depth = 0;
}
