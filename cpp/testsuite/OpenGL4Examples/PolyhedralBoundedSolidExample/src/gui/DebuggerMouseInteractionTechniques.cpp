#include "gui/DebuggerMouseInteractionTechniques.h"

#include "java/lang/System.h"
#include "vsdk/toolkit/gui/CameraController.h"
#include "models/DebuggerModel.h"

/**
C++ port note: Java gives the keyboard focus to the canvas inside its
frame (`requestFocusInWindow`); the GLFW window has no inner components,
so it already has it.
*/
void DebuggerMouseInteractionTechniques::processMouseEntered(DebuggerModel& /*model*/)
{
}

void DebuggerMouseInteractionTechniques::processMouseExited(DebuggerModel& /*model*/)
{
    ;
}

bool DebuggerMouseInteractionTechniques::processMousePressed(
    DebuggerModel& model, const MouseEvent& e)
{
    return model.getCameraController()->processMousePressedEvent(e);
}

bool DebuggerMouseInteractionTechniques::processMouseReleased(
    DebuggerModel& model, const MouseEvent& e)
{
    return model.getCameraController()->processMouseReleasedEvent(e);
}

bool DebuggerMouseInteractionTechniques::processMouseClicked(
    DebuggerModel& model, const MouseEvent& e)
{
    return model.getCameraController()->processMouseClickedEvent(e);
}

bool DebuggerMouseInteractionTechniques::processMouseMoved(
    DebuggerModel& model, const MouseEvent& e)
{
    return model.getCameraController()->processMouseMovedEvent(e);
}

bool DebuggerMouseInteractionTechniques::processMouseDragged(
    DebuggerModel& model, const MouseEvent& e)
{
    return model.getCameraController()->processMouseDraggedEvent(e);
}

bool DebuggerMouseInteractionTechniques::processMouseWheelMoved(
    DebuggerModel& model, const MouseEvent& e)
{
    java::System::out.println(".");
    return model.getCameraController()->processMouseWheelEvent(e);
}
