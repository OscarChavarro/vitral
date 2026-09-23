#include "java/lang/System.h"
#include "java/util/ArrayList.txx"
#include "java/util/Collections.h"
#include "model/history/EntityTransformState.h"
#include "model/history/SceneTransformationOperation.h"

namespace {
void copyStates(const java::ArrayList<EntityTransformState*>& source,
                java::ArrayList<EntityTransformState*>& target)
{
    long i;
    for ( i = 0; i < source.size(); i++ ) {
        target.add(source.get(i)->clone());
    }
}

void deleteStates(java::ArrayList<EntityTransformState*>& states)
{
    long i;
    for ( i = 0; i < states.size(); i++ ) {
        delete states.get(i);
    }
    states.clear();
}
}

SceneTransformationOperation::SceneTransformationOperation(
    const java::String& name,
    const java::ArrayList<EntityTransformState*>& before,
    const java::ArrayList<EntityTransformState*>& after,
    bool mergeable)
    : name(name), mergeable(mergeable),
      lastChangeTime(java::System::currentTimeMillis())
{
    copyStates(before, this->before);
    copyStates(after, this->after);
}

SceneTransformationOperation::~SceneTransformationOperation()
{
    deleteStates(before);
    deleteStates(after);
}

void SceneTransformationOperation::undo()
{
    long i;
    for ( i = 0; i < before.size(); i++ ) {
        before.get(i)->restore();
    }
}

void SceneTransformationOperation::redo()
{
    long i;
    for ( i = 0; i < after.size(); i++ ) {
        after.get(i)->restore();
    }
}

java::String SceneTransformationOperation::getName() const
{
    return name;
}

int SceneTransformationOperation::getEntityCount() const
{
    return (int)before.size();
}

bool SceneTransformationOperation::absorb(UndoableOperation* next)
{
    SceneTransformationOperation* other =
        dynamic_cast<SceneTransformationOperation*>(next);

    if ( other == nullptr ||
         !mergeable || !other->mergeable || !name.equals(other->name) ||
         other->lastChangeTime - lastChangeTime > MERGE_INTERVAL_MILLISECONDS ||
         !sameElements(other) ) {
        return false;
    }
    // Takes the placements after the other operation; the old ones go to
    // the other operation, deleted by the caller
    java::Collections::swap(after, other->after);
    lastChangeTime = other->lastChangeTime;
    return true;
}

bool SceneTransformationOperation::sameElements(
    const SceneTransformationOperation* other) const
{
    long i;

    if ( other->before.size() != before.size() ) {
        return false;
    }
    for ( i = 0; i < before.size(); i++ ) {
        if ( other->before.get(i)->getEntity() !=
             before.get(i)->getEntity() ) {
            return false;
        }
    }
    return true;
}
