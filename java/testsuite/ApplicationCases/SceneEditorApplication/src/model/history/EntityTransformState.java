package model.history;

import vsdk.toolkit.common.Entity;

/**
Captured placement of one entity of a scene (a body, a light or a camera),
which can be given back to the entity later (Memento pattern). Scene elements
are vitral `Entity`s, which give each port of the toolkit the same base for
identity and serialization.
*/
public interface EntityTransformState
{
    /**
    @return the entity whose placement was captured
    */
    Entity getEntity();

    /**
    Gives back the captured placement to the entity.
    */
    void restore();

    /**
    @param other state captured from the same entity
    @return true if both states are the same placement
    */
    boolean isSameState(EntityTransformState other);
}
