#include "vsdk/toolkit/gui/widget/WidgetElement.h"

const Class WidgetElement::CLASS = { "WidgetElement", &PresentationElement::CLASS };

const Class *
WidgetElement::getClass() const
{
    return &CLASS;
}
