#include "vsdk/toolkit/common/VSDK.h"
#include "model/history/EntityTransformState.h"

const Class EntityTransformState::CLASS = { "EntityTransformState", NULL };

const Class *
EntityTransformState::getClass() const
{
    return &CLASS;
}

BOOLEAN
EntityTransformState::instanceOf(const Class &type) const
{
    return type.isAssignableFrom(getClass());
}
