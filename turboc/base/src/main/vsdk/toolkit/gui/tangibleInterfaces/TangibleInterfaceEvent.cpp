#include <stdio.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceEvent.h"

TangibleInterfaceEvent::TangibleInterfaceEvent()
    : id(""), position(0, 0, 0), rotation()
{
}

TangibleInterfaceEvent::TangibleInterfaceEvent(
    const String& id, const Vector3Dd& position, const Quaterniond& rotation)
    : id(id), position(position), rotation(rotation)
{
}

const String& TangibleInterfaceEvent::getId() const
{
    return id;
}

const Vector3Dd& TangibleInterfaceEvent::getPosition() const
{
    return position;
}

const Quaterniond& TangibleInterfaceEvent::getRotation() const
{
    return rotation;
}

String TangibleInterfaceEvent::toString() const
{
    char buffer[512];
    snprintf(buffer, sizeof(buffer),
        "TangibleInterfaceEvent{id=%s, position=(%.6f, %.6f, %.6f), rotation=(%.6f, %.6f, %.6f, %.6f)}",
        id.c_str(), position.x(), position.y(), position.z(),
        rotation.magnitude(), rotation.direction().x(), rotation.direction().y(), rotation.direction().z());
    return String(buffer);
}

const Class TangibleInterfaceEvent::CLASS = { "TangibleInterfaceEvent", &PresentationElement::CLASS };

const Class *
TangibleInterfaceEvent::getClass() const
{
    return &CLASS;
}
