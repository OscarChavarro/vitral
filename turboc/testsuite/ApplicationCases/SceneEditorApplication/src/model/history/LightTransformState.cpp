#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "model/history/LightTransformState.h"

LightTransformState::LightTransformState(Light* light)
    : light(light), position(light->getPosition())
{
}

LightTransformState* LightTransformState::capture(Light* light)
{
    return new LightTransformState(light);
}

Entity* LightTransformState::getEntity() const
{
    return light;
}

void LightTransformState::restore() const
{
    light->setPosition(position);
}

BOOLEAN LightTransformState::isSameState(const EntityTransformState* other) const
{
    const LightTransformState* state =
        (other != NULL && other->instanceOf(LightTransformState::CLASS) ? (const LightTransformState*)other : NULL);
    if ( state == NULL ) {
        return FALSE;
    }
    return state->light == light && position.equals(state->position);
}

EntityTransformState* LightTransformState::clone() const
{
    return new LightTransformState(*this);
}

const Class LightTransformState::CLASS = { "LightTransformState", &EntityTransformState::CLASS };

const Class *
LightTransformState::getClass() const
{
    return &CLASS;
}
