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

bool LightTransformState::isSameState(const EntityTransformState* other) const
{
    const LightTransformState* state =
        dynamic_cast<const LightTransformState*>(other);
    if ( state == nullptr ) {
        return false;
    }
    return state->light == light && position.equals(state->position);
}

EntityTransformState* LightTransformState::clone() const
{
    return new LightTransformState(*this);
}
