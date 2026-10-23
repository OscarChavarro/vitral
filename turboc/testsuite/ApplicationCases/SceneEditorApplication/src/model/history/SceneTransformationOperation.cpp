#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/System.h"
#include "java/util/ArrayList.txx"
#include "java/util/Collections.h"
#include "model/history/EntityTransformState.h"
#include "model/history/SceneTransformationOperation.h"

static void copyStates(const ArrayList<EntityTransformState*>& source,
                ArrayList<EntityTransformState*>& target)
{
    long i;
    for ( i = 0; i < source.size(); i++ ) {
        target.add(source.get(i)->clone());
    }
}

static void deleteStates(ArrayList<EntityTransformState*>& states)
{
    long i;
    for ( i = 0; i < states.size(); i++ ) {
        delete states.get(i);
    }
    states.clear();
}

SceneTransformationOperation::SceneTransformationOperation(
    const String& name,
    const ArrayList<EntityTransformState*>& before,
    const ArrayList<EntityTransformState*>& after,
    BOOLEAN mergeable)
    : name(name), mergeable(mergeable),
      lastChangeTime(System::currentTimeMillis())
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

String SceneTransformationOperation::getName() const
{
    return name;
}

int SceneTransformationOperation::getEntityCount() const
{
    return (int)before.size();
}

BOOLEAN SceneTransformationOperation::absorb(UndoableOperation* next)
{
    SceneTransformationOperation* other =
        (next != NULL && next->instanceOf(SceneTransformationOperation::CLASS) ? (SceneTransformationOperation*)next : NULL);

    if ( other == NULL ||
         !mergeable || !other->mergeable || !name.equals(other->name) ||
         other->lastChangeTime - lastChangeTime > MERGE_INTERVAL_MILLISECONDS ||
         !sameElements(other) ) {
        return FALSE;
    }
    // Takes the placements after the other operation; the old ones go to
    // the other operation, deleted by the caller
    Collections<EntityTransformState*>::swap(after, other->after);
    lastChangeTime = other->lastChangeTime;
    return TRUE;
}

BOOLEAN SceneTransformationOperation::sameElements(
    const SceneTransformationOperation* other) const
{
    long i;

    if ( other->before.size() != before.size() ) {
        return FALSE;
    }
    for ( i = 0; i < before.size(); i++ ) {
        if ( other->before.get(i)->getEntity() !=
             before.get(i)->getEntity() ) {
            return FALSE;
        }
    }
    return TRUE;
}

// Integral constants bound to references (i.e. by `?:`) need a
// definition out of the class in C++98
const long long SceneTransformationOperation::MERGE_INTERVAL_MILLISECONDS = 1000;

const Class SceneTransformationOperation::CLASS = { "SceneTransformationOperation", &UndoableOperation::CLASS };

const Class *
SceneTransformationOperation::getClass() const
{
    return &CLASS;
}
