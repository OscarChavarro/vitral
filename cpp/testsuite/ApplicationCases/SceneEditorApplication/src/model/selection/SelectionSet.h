#ifndef __SELECTION_SET__
#define __SELECTION_SET__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "model/selection/EntityListView.h"

/**
Selection marks over an external list of entities (bodies, lights, debug
groups...), kept aligned with the list.

C++ port note: the Java version associates a raw `ArrayList`; here the list
is seen through an `EntityListView`, so any list of pointers to `Entity`
subclasses can be used.
*/
class SelectionSet {
private:
    // This is an association to an external list
    EntityListView elements;
    java::ArrayList<bool> selection;

public:
    /**
    @param externalList list whose elements are selected; it must outlive
    this selection set
    */
    template <class T>
    explicit SelectionSet(java::ArrayList<T*>* externalList)
        : elements(externalList)
    {
        sync();
    }

    /**
    Checks the size of the element list. If it is different to current
    selection list, selection list is updated.
    */
    void sync();
    java::String toString() const;
    bool isSelected(int i) const;
    int firstSelected() const;
    void selectAll();
    void unselectAll();
    void select(int i);
    void unselect(int i);
    void change(int i);
    void selectPrevious();
    void selectNext();

    /**
    Removes from the external list the selected elements, keeping the
    selection of the remaining ones.
    @return the removed elements (not deleted), in decreasing order of their
    former index
    */
    java::ArrayList<Entity*> removeSelected();

    /**
    Inserts an element in the external list, keeping the selection marks of
    the other elements aligned with them (`sync` only works at the end of the
    list).
    @param index position of the new element in the external list
    @param element element to insert
    @param selected true if the inserted element must be selected
    */
    void insertElement(int index, Entity* element, bool selected);

    /**
    Removes an element from the external list, keeping the selection marks of
    the other elements aligned with them.
    @param index position of the element in the external list
    @return the removed element (not deleted)
    */
    Entity* removeElement(int index);
    int numberOfSelections();
    int size() const;

    /**
    @return the view of the external list
    */
    const EntityListView& getElements() const;
};

#endif
