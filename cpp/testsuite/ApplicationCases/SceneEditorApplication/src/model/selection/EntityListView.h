#ifndef __ENTITY_LIST_VIEW__
#define __ENTITY_LIST_VIEW__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/Entity.h"
#include "vsdk/toolkit/common/logging/Logger.h"

/**
C++ port helper (no Java counterpart): a non-owning view of a list of
pointers to some `Entity` subclass (i.e. `java::ArrayList<SimpleBody*>`),
seen as a list of `Entity*`. Java code uses raw `ArrayList`s (or
`ArrayList<? extends Entity>`) for this; C++ lists of different pointer types
are unrelated types, so this view erases the element type.
*/
class EntityListView {
private:
    void* list;
    long (*sizeFunction)(void* list);
    Entity* (*getFunction)(void* list, long index);
    bool (*insertFunction)(void* list, long index, Entity* element);
    Entity* (*removeFunction)(void* list, long index);

    template <class T>
    static long typedSize(void* list)
    {
        return static_cast<java::ArrayList<T*>*>(list)->size();
    }

    template <class T>
    static Entity* typedGet(void* list, long index)
    {
        return static_cast<java::ArrayList<T*>*>(list)->get(index);
    }

    template <class T>
    static bool typedInsert(void* list, long index, Entity* element)
    {
        T* typed = dynamic_cast<T*>(element);
        if ( element != nullptr && typed == nullptr ) {
            Logger::reportMessage("EntityListView", Logger::ERROR, "insert",
                "Element of a wrong type for this list, not inserted.");
            return false;
        }
        static_cast<java::ArrayList<T*>*>(list)->add(index, typed);
        return true;
    }

    template <class T>
    static Entity* typedRemove(void* list, long index)
    {
        java::ArrayList<T*>* typedList = static_cast<java::ArrayList<T*>*>(list);
        T* element = typedList->get(index);
        typedList->remove(index);
        return element;
    }

public:
    EntityListView()
        : list(nullptr), sizeFunction(nullptr), getFunction(nullptr),
          insertFunction(nullptr), removeFunction(nullptr)
    {
    }

    /**
    @param externalList list to view; it must outlive this view
    */
    template <class T>
    explicit EntityListView(java::ArrayList<T*>* externalList)
        : list(externalList), sizeFunction(&typedSize<T>),
          getFunction(&typedGet<T>), insertFunction(&typedInsert<T>),
          removeFunction(&typedRemove<T>)
    {
    }

    bool isValid() const
    {
        return list != nullptr;
    }

    /**
    @return true if both views show the same list
    */
    bool isSameList(const EntityListView& other) const
    {
        return list == other.list;
    }

    long size() const
    {
        return list == nullptr ? 0 : sizeFunction(list);
    }

    Entity* get(long index) const
    {
        return getFunction(list, index);
    }

    /**
    @return false if the element is not of the type of the list
    */
    bool insert(long index, Entity* element)
    {
        return insertFunction(list, index, element);
    }

    /**
    @return the removed element (not deleted)
    */
    Entity* remove(long index)
    {
        return removeFunction(list, index);
    }
};

#endif
