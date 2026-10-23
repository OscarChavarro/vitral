#include "vsdk/toolkit/common/VSDK.h"
#include "model/selection/EntityListView.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleBodyGroup.h"

/**
Access to an `ArrayList<T*>`, for an `Entity` subclass `T`.
*/
template <class T>
class TypedEntityListAccess : public EntityListAccess {
private:
    ArrayList<T*>* list;

public:
    explicit TypedEntityListAccess(ArrayList<T*>* list) : list(list) {}

    virtual EntityListAccess* clone() const
    {
        return new TypedEntityListAccess<T>(list);
    }

    virtual const void* getList() const
    {
        return list;
    }

    virtual long size() const
    {
        return list->size();
    }

    virtual Entity* get(long index) const
    {
        return list->get(index);
    }

    virtual BOOLEAN insert(long index, Entity* element)
    {
        T* typed = element != NULL && element->instanceOf(T::CLASS) ?
            (T*)element : NULL;
        if ( element != NULL && typed == NULL ) {
            Logger::reportMessage("EntityListView", Logger::ERROR, "insert",
                "Element of a wrong type for this list, not inserted.");
            return FALSE;
        }
        list->add(index, typed);
        return TRUE;
    }

    virtual Entity* remove(long index)
    {
        T* element = list->get(index);
        list->remove(index);
        return element;
    }
};

EntityListView::EntityListView() : access(NULL)
{
}

EntityListView::EntityListView(const EntityListView& other)
    : access(other.access != NULL ? other.access->clone() : NULL)
{
}

EntityListView& EntityListView::operator=(const EntityListView& other)
{
    if ( this != &other ) {
        delete access;
        access = other.access != NULL ? other.access->clone() : NULL;
    }
    return *this;
}

EntityListView::~EntityListView()
{
    delete access;
}

EntityListView::EntityListView(ArrayList<SimpleBody*>* externalList)
    : access(new TypedEntityListAccess<SimpleBody>(externalList))
{
}

EntityListView::EntityListView(ArrayList<Light*>* externalList)
    : access(new TypedEntityListAccess<Light>(externalList))
{
}

EntityListView::EntityListView(ArrayList<Camera*>* externalList)
    : access(new TypedEntityListAccess<Camera>(externalList))
{
}

EntityListView::EntityListView(ArrayList<SimpleBodyGroup*>* externalList)
    : access(new TypedEntityListAccess<SimpleBodyGroup>(externalList))
{
}
