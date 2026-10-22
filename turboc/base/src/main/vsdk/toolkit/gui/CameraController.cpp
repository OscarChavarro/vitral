#include "vsdk/toolkit/gui/CameraController.h"

const Class CameraController::CLASS = { "CameraController", &Controller::CLASS };

const Class *
CameraController::getClass() const
{
    return &CLASS;
}
