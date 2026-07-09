#include <cstdio>

#include "vsdk/toolkit/gui/CameraController.h"
#include "gui/MeshMouseInteractionTechniques.h"

MeshMouseInteractionTechniques::MeshMouseInteractionTechniques(CameraController* cameraController)
    : cameraController(cameraController)
{
}

bool MeshMouseInteractionTechniques::processMousePressedEvent(const MouseEvent& event)
{
    return cameraController != 0 && cameraController->processMousePressedEvent(event);
}

bool MeshMouseInteractionTechniques::processMouseReleasedEvent(const MouseEvent& event)
{
    return cameraController != 0 && cameraController->processMouseReleasedEvent(event);
}

bool MeshMouseInteractionTechniques::processMouseClickedEvent(const MouseEvent& event)
{
    return cameraController != 0 && cameraController->processMouseClickedEvent(event);
}

bool MeshMouseInteractionTechniques::processMouseMovedEvent(const MouseEvent& event)
{
    return cameraController != 0 && cameraController->processMouseMovedEvent(event);
}

bool MeshMouseInteractionTechniques::processMouseDraggedEvent(const MouseEvent& event)
{
    return cameraController != 0 && cameraController->processMouseDraggedEvent(event);
}

bool MeshMouseInteractionTechniques::processMouseWheelEvent(const MouseEvent& event)
{
    std::printf(".\n");
    return cameraController != 0 && cameraController->processMouseWheelEvent(event);
}
