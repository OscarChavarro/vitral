#include "vsdk/toolkit/gui/MouseEvent.h"

const Class MouseEvent::CLASS = { "MouseEvent", &PresentationElement::CLASS };

const Class *
MouseEvent::getClass() const
{
    return &CLASS;
}
