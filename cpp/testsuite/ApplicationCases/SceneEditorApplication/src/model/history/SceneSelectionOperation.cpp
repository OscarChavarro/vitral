#include "model/Scene.h"
#include "model/history/SceneSelectionOperation.h"
#include "model/selection/SelectionSet.h"

SceneSelectionOperation::SceneSelectionOperation(
    const java::String& name, Scene* scene,
    const std::vector<bool>& beforeBodies,
    const std::vector<bool>& beforeLights,
    const std::vector<bool>& beforeGizmos,
    const std::vector<bool>& afterBodies,
    const std::vector<bool>& afterLights,
    const std::vector<bool>& afterGizmos)
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

java::String SceneSelectionOperation::getName() const
{
    return name;
}

void SceneSelectionOperation::apply(const std::vector<bool>& bodies,
                                    const std::vector<bool>& lights,
                                    const std::vector<bool>& gizmos)
{
    apply(scene->selectedThings, bodies);
    apply(scene->selectedLights, lights);
    apply(scene->selectedDebugThingGroups, gizmos);
}

void SceneSelectionOperation::apply(SelectionSet* selection,
                                    const std::vector<bool>& state)
{
    selection->sync();
    selection->unselectAll();
    for ( int i = 0; i < static_cast<int>(state.size()) &&
                     i < selection->size(); i++ ) {
        if ( state[i] ) {
            selection->select(i);
        }
    }
}

std::vector<bool> SceneSelectionOperation::capture(SelectionSet* selection)
{
    selection->sync();
    std::vector<bool> result(selection->size());
    for ( int i = 0; i < static_cast<int>(result.size()); i++ ) {
        result[i] = selection->isSelected(i);
    }
    return result;
}

bool SceneSelectionOperation::isSame(const std::vector<bool>& a,
                                     const std::vector<bool>& b)
{
    return a == b;
}
