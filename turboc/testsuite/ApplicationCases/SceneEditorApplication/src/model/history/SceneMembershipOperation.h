#ifndef __SCENE_MEMBERSHIP_OPERATION__
#define __SCENE_MEMBERSHIP_OPERATION__

#include "vsdk/toolkit/common/VSDK.h"
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
    static const Class CLASS;
    virtual const Class *getClass() const;

    /**
    An element of the list and its position and selection state.
    */
    class Entry {
    private:
        int indexValue;
        Entity* elementValue;
        BOOLEAN selectedValue;

    public:
        Entry() : indexValue(0), elementValue(NULL), selectedValue(FALSE) {}

        /**
        @param index position of the element in the list
        @param element the element
        @param selected true if the element was selected
        */
        Entry(int index, Entity* element, BOOLEAN selected)
            : indexValue(index), elementValue(element),
              selectedValue(selected) {}

        int index() const { return indexValue; }
        Entity* element() const { return elementValue; }
        BOOLEAN selected() const { return selectedValue; }
    };

private:
    String name;
    Scene* scene;
    SceneElementKind::Value kind;
    /// Elements not present after the operation, with their positions
    /// before it
    ArrayList<Entry> removed;
    /// Elements not present before the operation, with their positions
    /// after it
    ArrayList<Entry> inserted;

    void apply(const ArrayList<Entry>& toRemove,
               const ArrayList<Entry>& toInsert);

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
    SceneMembershipOperation(const String& name, Scene* scene,
                             SceneElementKind::Value kind,
                             const ArrayList<Entry>& removed,
                             const ArrayList<Entry>& inserted);
    virtual ~SceneMembershipOperation() {}

    virtual void undo();
    virtual void redo();
    virtual String getName() const;

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
