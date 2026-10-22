#include "vsdk/toolkit/gui/CommandListener.h"

const Class CommandListener::CLASS = { "CommandListener", &PresentationElement::CLASS };

const Class *
CommandListener::getClass() const
{
    return &CLASS;
}
