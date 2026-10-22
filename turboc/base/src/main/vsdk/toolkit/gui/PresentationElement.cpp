#include "vsdk/toolkit/gui/PresentationElement.h"

const Class PresentationElement::CLASS = { "PresentationElement", NULL };

const Class *
PresentationElement::getClass() const
{
    return &CLASS;
}

bool
PresentationElement::instanceOf(const Class &type) const
{
    return type.isAssignableFrom(getClass());
}
