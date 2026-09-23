#include "java/util/ArrayList.txx"
#include "model/history/SceneMembershipOperation.h"

SceneMembershipOperation::SceneMembershipOperation(
    const java::String& name, Scene* scene, SceneElementKind::Value kind,
    const java::ArrayList<Entry>& removed,
    const java::ArrayList<Entry>& inserted)
    : name(name), scene(scene), kind(kind), removed(removed),
      inserted(inserted)
{
}

void SceneMembershipOperation::undo()
{
    apply(inserted, removed);
}

void SceneMembershipOperation::redo()
{
    apply(removed, inserted);
}

void SceneMembershipOperation::apply(const java::ArrayList<Entry>& toRemove,
                                     const java::ArrayList<Entry>& toInsert)
{
    long i;

    for ( i = toRemove.size() - 1; i >= 0; i-- ) {
        SceneElementKind::remove(kind, scene, toRemove.get(i).index());
    }
    for ( i = 0; i < toInsert.size(); i++ ) {
        const Entry& entry = toInsert[i];
        SceneElementKind::insert(kind, scene, entry.index(), entry.element(),
                                 entry.selected());
    }
}

java::String SceneMembershipOperation::getName() const
{
    return name;
}

SceneElementKind::Value SceneMembershipOperation::getKind() const
{
    return kind;
}

int SceneMembershipOperation::getRemovedCount() const
{
    return (int)removed.size();
}

int SceneMembershipOperation::getInsertedCount() const
{
    return (int)inserted.size();
}
