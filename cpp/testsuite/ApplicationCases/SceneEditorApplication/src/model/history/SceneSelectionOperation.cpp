#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "java/util/Collections.h"
#include "model/Scene.h"
#include "model/history/SceneSelectionOperation.h"
#include "model/selection/SelectionSet.h"

SceneSelectionOperation::SceneSelectionOperation(
    const java::String& name, Scene* scene,
    const java::ArrayList<bool>& beforeBodies,
    const java::ArrayList<bool>& beforeLights,
    const java::ArrayList<bool>& beforeGizmos,
    const java::ArrayList<bool>& afterBodies,
    const java::ArrayList<bool>& afterLights,
    const java::ArrayList<bool>& afterGizmos)
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

void SceneSelectionOperation::apply(const java::ArrayList<bool>& bodies,
                                    const java::ArrayList<bool>& lights,
                                    const java::ArrayList<bool>& gizmos)
{
    apply(scene->selectedThings, bodies);
    apply(scene->selectedLights, lights);
    apply(scene->selectedDebugThingGroups, gizmos);
}

void SceneSelectionOperation::apply(SelectionSet* selection,
                                    const java::ArrayList<bool>& state)
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

java::ArrayList<bool> SceneSelectionOperation::capture(SelectionSet* selection)
{
    selection->sync();
    java::ArrayList<bool> result = java::Collections::nCopies<bool>(selection->size(), false);
    for ( int i = 0; i < static_cast<int>(result.size()); i++ ) {
        result[i] = selection->isSelected(i);
    }
    return result;
}

bool SceneSelectionOperation::isSame(const java::ArrayList<bool>& a,
                                     const java::ArrayList<bool>& b)
{
    return a.equals(b);
}
