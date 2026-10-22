#ifndef __SCENE_SELECTION_OPERATION__
#define __SCENE_SELECTION_OPERATION__

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
    String name;
    ArrayList<bool> beforeBodies;
    ArrayList<bool> beforeLights;
    ArrayList<bool> beforeGizmos;
    ArrayList<bool> afterBodies;
    ArrayList<bool> afterLights;
    ArrayList<bool> afterGizmos;

    void apply(const ArrayList<bool>& bodies,
               const ArrayList<bool>& lights,
               const ArrayList<bool>& gizmos);
    static void apply(SelectionSet* selection, const ArrayList<bool>& state);

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    SceneSelectionOperation(const String& name, Scene* scene,
                            const ArrayList<bool>& beforeBodies,
                            const ArrayList<bool>& beforeLights,
                            const ArrayList<bool>& beforeGizmos,
                            const ArrayList<bool>& afterBodies,
                            const ArrayList<bool>& afterLights,
                            const ArrayList<bool>& afterGizmos);

    virtual void undo();
    virtual void redo();
    virtual String getName() const;

    /**
    @return the selection state of each element of the selection set
    */
    static ArrayList<bool> capture(SelectionSet* selection);

    static bool isSame(const ArrayList<bool>& a, const ArrayList<bool>& b);
};

#endif
