#ifndef __SCENE_SNAPSHOT__
#define __SCENE_SNAPSHOT__

#include "java/util/ArrayList.h"
#include "java/util/HashMap.h"
#include "model/history/SceneElementKind.h"
#include "model/history/SceneMembershipOperation.h"

class Entity;
class EntityTransformState;
class Scene;
class UndoableOperation;

/**
What a user action can change of a scene, captured before and after the
action to find out the operations it did: which elements each list of the
scene holds (bodies, lights, cameras and debug groups, with their selection
state) and the placement of each body, light and camera. Only references and
immutable values are kept, so a capture is cheap.

Elements (vitral `Entity`s) are compared by identity: the scene lists may
hold entities that are `equals` without being the same one.
*/
class SceneSnapshot {
private:
    Scene* scene;
    java::ArrayList<Entity*> elements[SceneElementKind::COUNT];
    java::ArrayList<bool> selections[SceneElementKind::COUNT];
    java::HashMap<Entity*, EntityTransformState*> transforms;
    /// Owned states, the values of `transforms`
    java::ArrayList<EntityTransformState*> ownedStates;

    explicit SceneSnapshot(Scene* scene);
    void putTransform(Entity* entity, EntityTransformState* state);
    EntityTransformState* getTransform(Entity* entity) const;
    UndoableOperation* transformationTo(const SceneSnapshot* later,
                                        const java::String& name,
                                        bool mergeable) const;
    UndoableOperation* membershipTo(const SceneSnapshot* later,
                                    SceneElementKind::Value kind,
                                    const java::String& name) const;
    java::ArrayList<SceneMembershipOperation::Entry> entriesMissingIn(
        const SceneSnapshot* other, SceneElementKind::Value kind) const;

    SceneSnapshot(const SceneSnapshot& other);
    SceneSnapshot& operator=(const SceneSnapshot& other);

public:
    virtual ~SceneSnapshot();

    /**
    @param scene scene to capture
    @return the current state of the scene, owned by the caller
    */
    static SceneSnapshot* capture(Scene* scene);

    /**
    @return the captured scene
    */
    Scene* getScene() const;

    /**
    Finds out the operations that go from this state of the scene to a later
    one.
    @param later state of the same scene captured after the user action
    @param name name of the user action
    @param mergeable true if the placement changes of the action can be
    merged with the ones of a following action over the same elements
    @return the operation done by the action (owned by the caller), or null
    if nothing changed
    */
    UndoableOperation* operationTo(const SceneSnapshot* later,
                                   const java::String& name,
                                   bool mergeable) const;
};

#endif
