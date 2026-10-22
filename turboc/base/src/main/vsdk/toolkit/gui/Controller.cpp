#include "vsdk/toolkit/gui/Controller.h"

const Class Controller::CLASS = { "Controller", &PresentationElement::CLASS };

const Class *
Controller::getClass() const
{
    return &CLASS;
}
