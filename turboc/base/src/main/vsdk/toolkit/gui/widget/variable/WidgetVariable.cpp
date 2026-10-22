#include "vsdk/toolkit/gui/widget/variable/WidgetVariable.h"

const Class WidgetVariable::CLASS = { "WidgetVariable", &WidgetElement::CLASS };

const Class *
WidgetVariable::getClass() const
{
    return &CLASS;
}
