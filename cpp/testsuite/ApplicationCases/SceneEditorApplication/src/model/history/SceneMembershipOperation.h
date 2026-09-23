#ifndef __SCENE_MEMBERSHIP_OPERATION__
#define __SCENE_MEMBERSHIP_OPERATION__

#include "java/util/ArrayList.h"
#include "model/history/SceneElementKind.h"
#include "model/history/UndoableOperation.h"

class Entity;
class Scene;

/**
Creation and deletion of elements of one kind (bodies, lights, cameras or
debug groups) of a scene. The elements themselves are kept, so undoing a
deletion puts back the very same objects, at their former positions in the
list and with their former selection state.

Undo goes from the list after the operation to the list before it: the
inserted elements are removed (from the last position to the first one) and
then the removed elements are inserted again (from the first position to the
last one). Redo does the opposite.

C++ port note: elements are referenced, not owned (see `Scene`).
*/
class SceneMembershipOperation : public UndoableOperation {
public:
    /**
    An element of the list and its position and selection state.
    */
    class Entry {
    private:
        int indexValue;
        Entity* elementValue;
        bool selectedValue;

    public:
        Entry() : indexValue(0), elementValue(nullptr), selectedValue(false) {}

        /**
        @param index position of the element in the list
        @param element the element
        @param selected true if the element was selected
        */
        Entry(int index, Entity* element, bool selected)
            : indexValue(index), elementValue(element),
              selectedValue(selected) {}

        int index() const { return indexValue; }
        Entity* element() const { return elementValue; }
        bool selected() const { return selectedValue; }
    };

private:
    java::String name;
    Scene* scene;
    SceneElementKind::Value kind;
    /// Elements not present after the operation, with their positions
    /// before it
    java::ArrayList<Entry> removed;
    /// Elements not present before the operation, with their positions
    /// after it
    java::ArrayList<Entry> inserted;

    void apply(const java::ArrayList<Entry>& toRemove,
               const java::ArrayList<Entry>& toInsert);

public:
    /**
    @param name name of the user action
    @param scene scene holding the elements
    @param kind kind of the elements
    @param removed elements deleted by the operation, sorted by increasing
    position in the list before the operation
    @param inserted elements created by the operation, sorted by increasing
    position in the list after the operation
    */
    SceneMembershipOperation(const java::String& name, Scene* scene,
                             SceneElementKind::Value kind,
                             const java::ArrayList<Entry>& removed,
                             const java::ArrayList<Entry>& inserted);
    virtual ~SceneMembershipOperation() {}

    virtual void undo() override;
    virtual void redo() override;
    virtual java::String getName() const override;

    /**
    @return kind of the elements created or deleted
    */
    SceneElementKind::Value getKind() const;

    /**
    @return number of elements deleted by the operation
    */
    int getRemovedCount() const;

    /**
    @return number of elements created by the operation
    */
    int getInsertedCount() const;
};

#endif
