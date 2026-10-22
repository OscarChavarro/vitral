#include "model/Scene.h"
#include "model/history/SceneSelectionOperation.h"
#include "model/selection/SelectionSet.h"
#include "java/util/Collections.h"
#include "java/util/ArrayList.txx"

SceneSelectionOperation::SceneSelectionOperation(
    const String& name, Scene* scene,
    const ArrayList<bool>& beforeBodies,
    const ArrayList<bool>& beforeLights,
    const ArrayList<bool>& beforeGizmos,
    const ArrayList<bool>& afterBodies,
    const ArrayList<bool>& afterLights,
    const ArrayList<bool>& afterGizmos)
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

void SceneSelectionOperation::apply(const ArrayList<bool>& bodies,
                                    const ArrayList<bool>& lights,
                                    const ArrayList<bool>& gizmos)
{
    apply(scene->selectedThings, bodies);
    apply(scene->selectedLights, lights);
    apply(scene->selectedDebugThingGroups, gizmos);
}

void SceneSelectionOperation::apply(SelectionSet* selection,
                                    const ArrayList<bool>& state)
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

ArrayList<bool> SceneSelectionOperation::capture(SelectionSet* selection)
{
    selection->sync();
    ArrayList<bool> result(Collections<bool>::nCopies(selection->size(), false));
    for ( int i = 0; i < ((int)(result.size())); i++ ) {
        result[i] = selection->isSelected(i);
    }
    return result;
}

bool SceneSelectionOperation::isSame(const ArrayList<bool>& a,
                                     const ArrayList<bool>& b)
{
    return a.equals(b);
}

const Class SceneSelectionOperation::CLASS = { "SceneSelectionOperation", &UndoableOperation::CLASS };

const Class *
SceneSelectionOperation::getClass() const
{
    return &CLASS;
}
