#ifndef __ENTITY_LIST_VIEW__
#define __ENTITY_LIST_VIEW__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"

class Entity;
class SimpleBody;
class Light;
class Camera;
class SimpleBodyGroup;

/**
Access to a list of pointers to some `Entity` subclass, seen as a list of
`Entity*` (implemented by a class template over the element type).
*/
class EntityListAccess {
public:
    virtual ~EntityListAccess() {}
    virtual EntityListAccess* clone() const = 0;
    virtual const void* getList() const = 0;
    virtual long size() const = 0;
    virtual Entity* get(long index) const = 0;
    virtual BOOLEAN insert(long index, Entity* element) = 0;
    virtual Entity* remove(long index) = 0;
};

/**
C++ port helper (no Java counterpart): a non-owning view of a list of
pointers to some `Entity` subclass (i.e. `ArrayList<SimpleBody*>`), seen as
a list of `Entity*`. Java code uses raw `ArrayList`s (or
`ArrayList<? extends Entity>`) for this; C++ lists of different pointer types
are unrelated types, so this view erases the element type.

C++ port note: legacy compilers (gcc 2.7, Turbo C++ 3.0) have no member
templates, so there is a constructor for each kind of list of the scene.
*/
class EntityListView {
private:
    EntityListAccess* access;

public:
    EntityListView();
    EntityListView(const EntityListView& other);
    EntityListView& operator=(const EntityListView& other);
    ~EntityListView();

    /**
    @param externalList list to view; it must outlive this view
    */
    explicit EntityListView(ArrayList<SimpleBody*>* externalList);
    explicit EntityListView(ArrayList<Light*>* externalList);
    explicit EntityListView(ArrayList<Camera*>* externalList);
    explicit EntityListView(ArrayList<SimpleBodyGroup*>* externalList);

    BOOLEAN isValid() const
    {
        return access != NULL;
    }

    /**
    @return true if both views show the same list
    */
    BOOLEAN isSameList(const EntityListView& other) const
    {
        return (access == NULL ? NULL : access->getList()) ==
            (other.access == NULL ? NULL : other.access->getList());
    }

    long size() const
    {
        return access == NULL ? 0 : access->size();
    }

    Entity* get(long index) const
    {
        return access->get(index);
    }

    /**
    @return false if the element is not of the type of the list
    */
    BOOLEAN insert(long index, Entity* element)
    {
        return access->insert(index, element);
    }

    /**
    @return the removed element (not deleted)
    */
    Entity* remove(long index)
    {
        return access->remove(index);
    }
};

#endif
