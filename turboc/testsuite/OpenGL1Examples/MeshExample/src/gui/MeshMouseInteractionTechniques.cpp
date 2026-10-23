#include <stdio.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/gui/CameraController.h"
#include "gui/MeshMouseInteractionTechniques.h"

MeshMouseInteractionTechniques::MeshMouseInteractionTechniques(CameraController* cameraController)
    : cameraController(cameraController)
{
}

BOOLEAN MeshMouseInteractionTechniques::processMousePressedEvent(const MouseEvent& event)
{
    return cameraController != 0 && cameraController->processMousePressedEvent(event);
}

BOOLEAN MeshMouseInteractionTechniques::processMouseReleasedEvent(const MouseEvent& event)
{
    return cameraController != 0 && cameraController->processMouseReleasedEvent(event);
}

BOOLEAN MeshMouseInteractionTechniques::processMouseClickedEvent(const MouseEvent& event)
{
    return cameraController != 0 && cameraController->processMouseClickedEvent(event);
}

BOOLEAN MeshMouseInteractionTechniques::processMouseMovedEvent(const MouseEvent& event)
{
    return cameraController != 0 && cameraController->processMouseMovedEvent(event);
}

BOOLEAN MeshMouseInteractionTechniques::processMouseDraggedEvent(const MouseEvent& event)
{
    return cameraController != 0 && cameraController->processMouseDraggedEvent(event);
}

BOOLEAN MeshMouseInteractionTechniques::processMouseWheelEvent(const MouseEvent& event)
{
    printf(".\n");
    return cameraController != 0 && cameraController->processMouseWheelEvent(event);
}
