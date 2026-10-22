#include "vsdk/toolkit/gui/gizmo/Gizmo.h"

const Class Gizmo::CLASS = { "Gizmo", &PresentationElement::CLASS };

const Class *
Gizmo::getClass() const
{
    return &CLASS;
}
