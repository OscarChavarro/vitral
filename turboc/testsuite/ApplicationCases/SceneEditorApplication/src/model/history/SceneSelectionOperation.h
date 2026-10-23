#ifndef __SCENE_SELECTION_OPERATION__
#define __SCENE_SELECTION_OPERATION__

#include "vsdk/toolkit/common/VSDK.h"
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
    ArrayList<BOOLEAN> beforeBodies;
    ArrayList<BOOLEAN> beforeLights;
    ArrayList<BOOLEAN> beforeGizmos;
    ArrayList<BOOLEAN> afterBodies;
    ArrayList<BOOLEAN> afterLights;
    ArrayList<BOOLEAN> afterGizmos;

    void apply(const ArrayList<BOOLEAN>& bodies,
               const ArrayList<BOOLEAN>& lights,
               const ArrayList<BOOLEAN>& gizmos);
    static void apply(SelectionSet* selection, const ArrayList<BOOLEAN>& state);

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    SceneSelectionOperation(const String& name, Scene* scene,
                            const ArrayList<BOOLEAN>& beforeBodies,
                            const ArrayList<BOOLEAN>& beforeLights,
                            const ArrayList<BOOLEAN>& beforeGizmos,
                            const ArrayList<BOOLEAN>& afterBodies,
                            const ArrayList<BOOLEAN>& afterLights,
                            const ArrayList<BOOLEAN>& afterGizmos);

    virtual void undo();
    virtual void redo();
    virtual String getName() const;

    /**
    @return the selection state of each element of the selection set
    */
    static ArrayList<BOOLEAN> capture(SelectionSet* selection);

    static BOOLEAN isSame(const ArrayList<BOOLEAN>& a, const ArrayList<BOOLEAN>& b);
};

#endif
