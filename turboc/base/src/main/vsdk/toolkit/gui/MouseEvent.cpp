#include "vsdk/toolkit/gui/MouseEvent.h"

// Integral constants bound to references (i.e. by `?:`) need a
// definition out of the class in C++98
const int MouseEvent::BUTTON1;
const int MouseEvent::BUTTON2;
const int MouseEvent::BUTTON3;
const int MouseEvent::CTRL_DOWN_MASK;
const int MouseEvent::BUTTON1_DOWN_MASK;
const int MouseEvent::BUTTON2_DOWN_MASK;
const int MouseEvent::BUTTON3_DOWN_MASK;

const Class MouseEvent::CLASS = { "MouseEvent", &PresentationElement::CLASS };

const Class *
MouseEvent::getClass() const
{
    return &CLASS;
}
