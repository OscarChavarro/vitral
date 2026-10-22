#include "model/history/EntityTransformState.h"

const Class EntityTransformState::CLASS = { "EntityTransformState", NULL };

const Class *
EntityTransformState::getClass() const
{
    return &CLASS;
}

bool
EntityTransformState::instanceOf(const Class &type) const
{
    return type.isAssignableFrom(getClass());
}
