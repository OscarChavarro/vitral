#include "vsdk/toolkit/common/VSDK.h"
#include "model/Scene.h"
#include "model/history/SceneSelectionOperation.h"
#include "model/selection/SelectionSet.h"
#include "java/util/Collections.h"
#include "java/util/ArrayList.txx"

SceneSelectionOperation::SceneSelectionOperation(
    const String& name, Scene* scene,
    const ArrayList<BOOLEAN>& beforeBodies,
    const ArrayList<BOOLEAN>& beforeLights,
    const ArrayList<BOOLEAN>& beforeGizmos,
    const ArrayList<BOOLEAN>& afterBodies,
    const ArrayList<BOOLEAN>& afterLights,
    const ArrayList<BOOLEAN>& afterGizmos)
    : scene(scene), name(name), beforeBodies(beforeBodies),
      beforeLights(beforeLights), beforeGizmos(beforeGizmos),
      afterBodies(afterBodies), afterLights(afterLights),
      afterGizmos(afterGizmos)
{
}

void SceneSelectionOperation::undo()
{
    apply(beforeBodies, beforeLights, beforeGizmos);
}

void SceneSelectionOperation::redo()
{
    apply(afterBodies, afterLights, afterGizmos);
}

String SceneSelectionOperation::getName() const
{
    return name;
}

void SceneSelectionOperation::apply(const ArrayList<BOOLEAN>& bodies,
                                    const ArrayList<BOOLEAN>& lights,
                                    const ArrayList<BOOLEAN>& gizmos)
{
    apply(scene->selectedThings, bodies);
    apply(scene->selectedLights, lights);
    apply(scene->selectedDebugThingGroups, gizmos);
}

void SceneSelectionOperation::apply(SelectionSet* selection,
                                    const ArrayList<BOOLEAN>& state)
{
    selection->sync();
    selection->unselectAll();
    for ( int i = 0; i < ((int)(state.size())) &&
                     i < selection->size(); i++ ) {
        if ( state[i] ) {
            selection->select(i);
        }
    }
}

ArrayList<BOOLEAN> SceneSelectionOperation::capture(SelectionSet* selection)
{
    selection->sync();
    ArrayList<BOOLEAN> result(Collections<BOOLEAN>::nCopies(selection->size(), FALSE));
    for ( int i = 0; i < ((int)(result.size())); i++ ) {
        result[i] = selection->isSelected(i);
    }
    return result;
}

BOOLEAN SceneSelectionOperation::isSame(const ArrayList<BOOLEAN>& a,
                                     const ArrayList<BOOLEAN>& b)
{
    return a.equals(b);
}

const Class SceneSelectionOperation::CLASS = { "SceneSelectionOperation", &UndoableOperation::CLASS };

const Class *
SceneSelectionOperation::getClass() const
{
    return &CLASS;
}
