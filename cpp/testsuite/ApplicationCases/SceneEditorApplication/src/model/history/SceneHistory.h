#ifndef __SCENE_HISTORY__
#define __SCENE_HISTORY__

#include <functional>

#include "java/lang/String.h"
#include "model/history/UndoQueue.h"

class Scene;
class SceneSnapshot;
class UndoableOperation;

/**
Global undo/redo history of the scene: creation, deletion and placement
changes of bodies, lights, scene cameras and debug groups.

User actions are recorded by bracketing them with `begin` and `end`: the
scene is captured at `begin` and compared at `end`, and what changed becomes
one operation of the queue. Brackets can be nested (i.e. a key pressed during
a mouse drag): only the outermost one records, so the whole gesture is a
single step.
*/
class SceneHistory {
private:
    std::function<Scene*()> sceneSource;
    UndoQueue queue;
    SceneSnapshot* before;
    int depth;

    SceneHistory(const SceneHistory& other);
    SceneHistory& operator=(const SceneHistory& other);

public:
    /**
    @param sceneSource gives the scene currently edited
    */
    explicit SceneHistory(const std::function<Scene*()>& sceneSource);
    virtual ~SceneHistory();

    /**
    @return the queue of operations done over the scene
    */
    UndoQueue* getQueue();

    /**
    @return true if a user action is being recorded
    */
    bool isRecording() const;

    /**
    Starts recording a user action over the scene.
    */
    void begin();

    /**
    Ends recording a user action. If it is the outermost one, what changed in
    the scene since its `begin` is recorded as one operation.
    @param name name of the action, to be shown to the user
    @param mergeable true if its placement changes can be merged with the
    ones of the next action over the same elements (see
    `SceneTransformationOperation`)
    @return true if an operation was recorded
    */
    bool end(const java::String& name, bool mergeable);

    /**
    Executes an action recording it as one operation.
    @param name name of the action
    @param action what changes the scene
    @return true if an operation was recorded
    */
    bool perform(const java::String& name,
                 const std::function<void()>& action);

    /**
    @return the undone operation, or null if there was nothing to undo or an
    action is being recorded
    */
    UndoableOperation* undo();

    /**
    @return the redone operation, or null if there was nothing to redo or an
    action is being recorded
    */
    UndoableOperation* redo();

    /**
    Forgets the history (i.e. when the scene is replaced).
    */
    void clear();
};

#endif
