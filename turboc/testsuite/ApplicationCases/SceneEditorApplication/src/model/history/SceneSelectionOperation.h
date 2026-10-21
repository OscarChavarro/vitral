#ifndef __SCENE_SELECTION_OPERATION__
#define __SCENE_SELECTION_OPERATION__

#include <vector>

#include "model/history/UndoableOperation.h"

class Scene;
class SelectionSet;

/**
Undoable selection change made by the scene tree (the scene selector
dialog): the selection states of the bodies, the lights and the debug
groups before and after the change.
*/
class SceneSelectionOperation : public UndoableOperation {
private:
    Scene* scene;
    String name;
    std::vector<bool> beforeBodies;
    std::vector<bool> beforeLights;
    std::vector<bool> beforeGizmos;
    std::vector<bool> afterBodies;
    std::vector<bool> afterLights;
    std::vector<bool> afterGizmos;

    void apply(const std::vector<bool>& bodies,
               const std::vector<bool>& lights,
               const std::vector<bool>& gizmos);
    static void apply(SelectionSet* selection, const std::vector<bool>& state);

public:
    SceneSelectionOperation(const String& name, Scene* scene,
                            const std::vector<bool>& beforeBodies,
                            const std::vector<bool>& beforeLights,
                            const std::vector<bool>& beforeGizmos,
                            const std::vector<bool>& afterBodies,
                            const std::vector<bool>& afterLights,
                            const std::vector<bool>& afterGizmos);

    virtual void undo();
    virtual void redo();
    virtual String getName() const;

    /**
    @return the selection state of each element of the selection set
    */
    static std::vector<bool> capture(SelectionSet* selection);

    static bool isSame(const std::vector<bool>& a, const std::vector<bool>& b);
};

#endif
