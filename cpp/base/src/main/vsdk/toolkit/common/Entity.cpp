#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/Entity.h"
#include "vsdk/toolkit/common/EntityListener.h"

Entity::Entity() : controlSpecifications(nullptr), entityListeners(nullptr)
{
}

Entity::Entity(const Entity &other) :
    controlSpecifications(nullptr), entityListeners(nullptr)
{
    if ( other.controlSpecifications != nullptr ) {
        controlSpecifications = new java::ArrayList<java::String>(
            *other.controlSpecifications);
    }
}

Entity &
Entity::operator=(const Entity &other)
{
    if ( this == &other ) {
        return *this;
    }
    delete controlSpecifications;
    controlSpecifications = nullptr;
    if ( other.controlSpecifications != nullptr ) {
        controlSpecifications = new java::ArrayList<java::String>(
            *other.controlSpecifications);
    }
    // Subscribers stay with this entity: they did not subscribe to `other`
    return *this;
}

Entity::~Entity()
{
    // Virtual calls inside a destructor resolve to Entity::dispose
    dispose();
    delete controlSpecifications;
}

java::ArrayList<java::String> &
Entity::getControlSpecifications()
{
    if ( controlSpecifications == nullptr ) {
        controlSpecifications = new java::ArrayList<java::String>();
    }
    return *controlSpecifications;
}

void
Entity::setControlSpecifications(
    const java::ArrayList<java::String> &controlSpecifications)
{
    getControlSpecifications() = controlSpecifications;
}

void
Entity::addEntityListener(EntityListener *listener)
{
    if ( listener == nullptr ) {
        return;
    }
    if ( entityListeners == nullptr ) {
        entityListeners = new java::ArrayList<EntityListener *>();
    }
    for ( long int i = 0; i < entityListeners->size(); i++ ) {
        if ( entityListeners->get(i) == listener ) {
            return;
        }
    }
    entityListeners->add(listener);
}

void
Entity::removeEntityListener(EntityListener *listener)
{
    if ( entityListeners == nullptr ) {
        return;
    }
    for ( long int i = 0; i < entityListeners->size(); i++ ) {
        if ( entityListeners->get(i) == listener ) {
            entityListeners->remove(i);
            return;
        }
    }
}

void
Entity::update()
{
    fireEntityEvent(EntityEvent::UPDATED);
}

void
Entity::dispose()
{
    fireEntityEvent(EntityEvent::DELETED);
    delete entityListeners;
    entityListeners = nullptr;
}

void
Entity::fireEntityEvent(EntityEvent::Type type)
{
    if ( entityListeners == nullptr || entityListeners->size() == 0 ) {
        return;
    }
    EntityEvent event(this, type);
    // Copy: listeners may unsubscribe while being notified
    java::ArrayList<EntityListener *> listeners(*entityListeners);
    for ( long int i = 0; i < listeners.size(); i++ ) {
        listeners.get(i)->notifyEntityEvent(event);
    }
}
