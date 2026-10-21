#ifndef __ENTITY_TRANSFORM_STATE__
#define __ENTITY_TRANSFORM_STATE__

class Entity;

/**
Captured placement of one entity of a scene (a body, a light or a camera),
which can be given back to the entity later (Memento pattern). Scene
elements are vitral `Entity`s, which give each port of the toolkit the same
base for identity and serialization.
*/
class EntityTransformState {
public:
    virtual ~EntityTransformState() {}

    /**
    @return the entity whose placement was captured
    */
    virtual Entity* getEntity() const = 0;

    /**
    Gives back the captured placement to the entity.
    */
    virtual void restore() const = 0;

    /**
    @param other state captured from the same entity
    @return true if both states are the same placement
    */
    virtual bool isSameState(const EntityTransformState* other) const = 0;

    /**
    C++ port helper: states are immutable values, shared by reference in
    Java; here each owner keeps its own copy.
    @return a copy of this state, owned by the caller
    */
    virtual EntityTransformState* clone() const = 0;
};

#endif
