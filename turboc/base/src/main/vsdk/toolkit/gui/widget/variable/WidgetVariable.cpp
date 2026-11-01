#include "vsdk/toolkit/gui/widget/variable/WidgetVariable.h"

const Class WidgetVariable::CLASS = { "WidgetVariable", &WidgetElement::CLASS };

const Class *
WidgetVariable::getClass() const
{
    return &CLASS;
}


String
WidgetVariable::toString() const
{
    String msg = "";
    msg = msg + "VARIABLE:\n"
        + "     TYPE: " + getType() + "\n"
        + "     NAME: " + getName() + "\n"
        + "     INITIAL_VALUE: " + getInitialvalue() + "\n"
        + "     VALID_RANGE: " + getValidRange() + "\n";
    return msg;
}
