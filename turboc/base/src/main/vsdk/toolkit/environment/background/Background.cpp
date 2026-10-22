#include "vsdk/toolkit/environment/background/Background.h"

const Class Background::CLASS = { "Background", &Entity::CLASS };

const Class *
Background::getClass() const
{
    return &CLASS;
}
