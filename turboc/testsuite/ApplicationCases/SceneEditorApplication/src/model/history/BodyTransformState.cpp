#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "model/history/BodyTransformState.h"

BodyTransformState::BodyTransformState(SimpleBody* body)
    : body(body), position(body->getPosition()), rotation(body->getRotation()),
      scale(body->getScale())
{
}

BodyTransformState* BodyTransformState::capture(SimpleBody* body)
{
    return new BodyTransformState(body);
}

Entity* BodyTransformState::getEntity() const
{
    return body;
}

void BodyTransformState::restore() const
{
    body->setScale(scale);
    // The inverse rotation is derived from this one
    body->setRotation(rotation);
    body->setPosition(position);
}

BOOLEAN BodyTransformState::isSameState(const EntityTransformState* other) const
{
    const BodyTransformState* state =
        (other != NULL && other->instanceOf(BodyTransformState::CLASS) ? (const BodyTransformState*)other : NULL);
    if ( state == NULL ) {
        return FALSE;
    }
    return state->body == body &&
        state->position.equals(position) &&
        state->rotation.equals(rotation) &&
        state->scale.equals(scale);
}

EntityTransformState* BodyTransformState::clone() const
{
    return new BodyTransformState(*this);
}

const Class BodyTransformState::CLASS = { "BodyTransformState", &EntityTransformState::CLASS };

const Class *
BodyTransformState::getClass() const
{
    return &CLASS;
}
