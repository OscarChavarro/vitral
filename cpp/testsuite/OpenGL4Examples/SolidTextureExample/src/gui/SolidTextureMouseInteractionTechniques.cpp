#include <cstdio>

#include "vsdk/toolkit/gui/CameraController.h"
#include "vsdk/toolkit/gui/MouseEvent.h"
#include "gui/SolidTextureMouseInteractionTechniques.h"

SolidTextureMouseInteractionTechniques::SolidTextureMouseInteractionTechniques(
    CameraController* cameraController)
    : cameraController(cameraController)
{
}

bool SolidTextureMouseInteractionTechniques::processMousePressedEvent(MouseEvent& event)
{ return cameraController != 0 && cameraController->processMousePressedEvent(event); }
bool SolidTextureMouseInteractionTechniques::processMouseReleasedEvent(MouseEvent& event)
{ return cameraController != 0 && cameraController->processMouseReleasedEvent(event); }
bool SolidTextureMouseInteractionTechniques::processMouseClickedEvent(MouseEvent& event)
{ return cameraController != 0 && cameraController->processMouseClickedEvent(event); }
bool SolidTextureMouseInteractionTechniques::processMouseMovedEvent(MouseEvent& event)
{ return cameraController != 0 && cameraController->processMouseMovedEvent(event); }
bool SolidTextureMouseInteractionTechniques::processMouseDraggedEvent(MouseEvent& event)
{ return cameraController != 0 && cameraController->processMouseDraggedEvent(event); }
bool SolidTextureMouseInteractionTechniques::processMouseWheelEvent(MouseEvent& event)
{
    std::printf(".\n");
    return cameraController != 0 && cameraController->processMouseWheelEvent(event);
}
