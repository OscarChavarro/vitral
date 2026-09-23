#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "model/Scene.h"
#include "model/history/BodyTransformState.h"
#include "model/history/CameraState.h"
#include "model/history/CompositeOperation.h"
#include "model/history/LightTransformState.h"
#include "model/history/SceneSnapshot.h"
#include "model/history/SceneTransformationOperation.h"
#include "model/selection/SelectionSet.h"

SceneSnapshot::SceneSnapshot(Scene* scene) : scene(scene)
{
}

SceneSnapshot::~SceneSnapshot()
{
    long i;
    for ( i = 0; i < ownedStates.size(); i++ ) {
        delete ownedStates.get(i);
    }
}

void SceneSnapshot::putTransform(Entity* entity, EntityTransformState* state)
{
    transforms.put(entity, state);
    ownedStates.add(state);
}

EntityTransformState* SceneSnapshot::getTransform(Entity* entity) const
{
    EntityTransformState* const* state = transforms.get(entity);
    return state == nullptr ? nullptr : *state;
}

SceneSnapshot* SceneSnapshot::capture(Scene* scene)
{
    SceneSnapshot* snapshot = new SceneSnapshot(scene);
    int kind;
    long i;

    for ( kind = 0; kind < SceneElementKind::COUNT; kind++ ) {
        SceneElementKind::Value value = (SceneElementKind::Value)kind;
        EntityListView list = SceneElementKind::getList(value, scene);
        SelectionSet* selection = SceneElementKind::getSelection(value, scene);

        if ( selection != nullptr ) {
            selection->sync();
        }
        for ( i = 0; i < list.size(); i++ ) {
            snapshot->elements[kind].add(list.get(i));
            snapshot->selections[kind].add(
                selection != nullptr && selection->isSelected((int)i));
        }
    }

    java::ArrayList<SimpleBody*>& bodies = scene->scene->getSimpleBodies();
    for ( i = 0; i < bodies.size(); i++ ) {
        snapshot->putTransform(bodies.get(i),
                               BodyTransformState::capture(bodies.get(i)));
    }
    java::ArrayList<Light*>& lights = scene->scene->getLights();
    for ( i = 0; i < lights.size(); i++ ) {
        snapshot->putTransform(lights.get(i),
                               LightTransformState::capture(lights.get(i)));
    }
    java::ArrayList<Camera*>& cameras = scene->scene->getCameras();
    for ( i = 0; i < cameras.size(); i++ ) {
        snapshot->putTransform(cameras.get(i),
                               CameraState::capture(cameras.get(i)));
    }
    return snapshot;
}

Scene* SceneSnapshot::getScene() const
{
    return scene;
}

UndoableOperation* SceneSnapshot::operationTo(const SceneSnapshot* later,
                                              const java::String& name,
                                              bool mergeable) const
{
    java::ArrayList<UndoableOperation*> operations;
    UndoableOperation* transformation;
    int kind;

    if ( later == nullptr || later->scene != scene ) {
        return nullptr;
    }
    transformation = transformationTo(later, name, mergeable);
    if ( transformation != nullptr ) {
        operations.add(transformation);
    }
    for ( kind = 0; kind < SceneElementKind::COUNT; kind++ ) {
        UndoableOperation* membership =
            membershipTo(later, (SceneElementKind::Value)kind, name);

        if ( membership != nullptr ) {
            operations.add(membership);
        }
    }
    if ( operations.size() == 0 ) {
        return nullptr;
    }
    if ( operations.size() == 1 ) {
        return operations.get(0);
    }
    return new CompositeOperation(name, operations);
}

UndoableOperation* SceneSnapshot::transformationTo(const SceneSnapshot* later,
                                                   const java::String& name,
                                                   bool mergeable) const
{
    java::ArrayList<EntityTransformState*> before;
    java::ArrayList<EntityTransformState*> after;
    int kind;
    long i;

    for ( kind = 0; kind < SceneElementKind::COUNT; kind++ ) {
        for ( i = 0; i < elements[kind].size(); i++ ) {
            Entity* element = elements[kind].get(i);
            EntityTransformState* oldState = getTransform(element);
            EntityTransformState* newState = later->getTransform(element);

            if ( oldState != nullptr && newState != nullptr &&
                 !oldState->isSameState(newState) ) {
                before.add(oldState);
                after.add(newState);
            }
        }
    }
    if ( before.size() == 0 ) {
        return nullptr;
    }
    // The operation keeps its own copies of the states
    return new SceneTransformationOperation(name, before, after, mergeable);
}

UndoableOperation* SceneSnapshot::membershipTo(const SceneSnapshot* later,
                                               SceneElementKind::Value kind,
                                               const java::String& name) const
{
    java::ArrayList<SceneMembershipOperation::Entry> removed =
        entriesMissingIn(later, kind);
    java::ArrayList<SceneMembershipOperation::Entry> inserted =
        later->entriesMissingIn(this, kind);

    if ( removed.size() == 0 && inserted.size() == 0 ) {
        return nullptr;
    }
    return new SceneMembershipOperation(name, scene, kind, removed, inserted);
}

java::ArrayList<SceneMembershipOperation::Entry>
SceneSnapshot::entriesMissingIn(const SceneSnapshot* other,
                                SceneElementKind::Value kind) const
{
    const java::ArrayList<Entity*>& list = elements[kind];
    const java::ArrayList<bool>& selected = selections[kind];
    java::HashMap<Entity*, bool> otherElements;
    java::ArrayList<SceneMembershipOperation::Entry> missing;
    long i;

    for ( i = 0; i < other->elements[kind].size(); i++ ) {
        otherElements.put(other->elements[kind].get(i), true);
    }
    for ( i = 0; i < list.size(); i++ ) {
        if ( !otherElements.containsKey(list.get(i)) ) {
            missing.add(SceneMembershipOperation::Entry(
                (int)i, list.get(i), selected.get(i)));
        }
    }
    return missing;
}
