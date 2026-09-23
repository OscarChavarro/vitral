#ifndef __SCENE_TRANSFORMATION_OPERATION__
#define __SCENE_TRANSFORMATION_OPERATION__

#include "java/util/ArrayList.h"
#include "model/history/UndoableOperation.h"

class EntityTransformState;

/**
Change of the placement (translation, rotation, scale) of some elements of a
scene: bodies, lights or cameras. It keeps the placements before and after
the change of each changed element (and owns them).

Operations done by the keyboard over the same elements in a short time can
be merged (see `absorb`), so an auto-repeated key is a single step.
*/
class SceneTransformationOperation : public UndoableOperation {
public:
    /// Maximum time between two mergeable operations to be merged
    static const long long MERGE_INTERVAL_MILLISECONDS = 1000;

private:
    java::String name;
    java::ArrayList<EntityTransformState*> before;
    java::ArrayList<EntityTransformState*> after;
    bool mergeable;
    long long lastChangeTime;

    bool sameElements(const SceneTransformationOperation* other) const;

    SceneTransformationOperation(const SceneTransformationOperation& other);
    SceneTransformationOperation& operator=(
        const SceneTransformationOperation& other);

public:
    /**
    @param name name of the user action
    @param before placements before the change, one per changed element
    (copied)
    @param after placements after the change, in the same order (copied)
    @param mergeable true if following operations over the same elements
    can be merged into this one
    */
    SceneTransformationOperation(
        const java::String& name,
        const java::ArrayList<EntityTransformState*>& before,
        const java::ArrayList<EntityTransformState*>& after,
        bool mergeable);
    virtual ~SceneTransformationOperation();

    virtual void undo() override;
    virtual void redo() override;
    virtual java::String getName() const override;

    /**
    @return number of entities whose placement changed
    */
    int getEntityCount() const;

    virtual bool absorb(UndoableOperation* next) override;
};

#endif
