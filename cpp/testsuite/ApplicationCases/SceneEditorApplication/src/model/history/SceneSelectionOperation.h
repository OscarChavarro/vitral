#ifndef __SCENE_SELECTION_OPERATION__
#define __SCENE_SELECTION_OPERATION__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"

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
    java::String name;
    java::ArrayList<bool> beforeBodies;
    java::ArrayList<bool> beforeLights;
    java::ArrayList<bool> beforeGizmos;
    java::ArrayList<bool> afterBodies;
    java::ArrayList<bool> afterLights;
    java::ArrayList<bool> afterGizmos;

    void apply(const java::ArrayList<bool>& bodies,
               const java::ArrayList<bool>& lights,
               const java::ArrayList<bool>& gizmos);
    static void apply(SelectionSet* selection, const java::ArrayList<bool>& state);

public:
    SceneSelectionOperation(const java::String& name, Scene* scene,
                            const java::ArrayList<bool>& beforeBodies,
                            const java::ArrayList<bool>& beforeLights,
                            const java::ArrayList<bool>& beforeGizmos,
                            const java::ArrayList<bool>& afterBodies,
                            const java::ArrayList<bool>& afterLights,
                            const java::ArrayList<bool>& afterGizmos);

    virtual void undo() override;
    virtual void redo() override;
    virtual java::String getName() const override;

    /**
    @return the selection state of each element of the selection set
    */
    static java::ArrayList<bool> capture(SelectionSet* selection);

    static bool isSame(const java::ArrayList<bool>& a, const java::ArrayList<bool>& b);
};

#endif
