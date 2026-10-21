#include "java/util/ArrayList.txx"
#include "model/history/SceneMembershipOperation.h"

SceneMembershipOperation::SceneMembershipOperation(
    const String& name, Scene* scene, SceneElementKind::Value kind,
    const ArrayList<Entry>& removed,
    const ArrayList<Entry>& inserted)
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

void SceneMembershipOperation::apply(const ArrayList<Entry>& toRemove,
                                     const ArrayList<Entry>& toInsert)
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

String SceneMembershipOperation::getName() const
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
