#ifndef __POLYHEDRAL_EXAMPLE_DEBUGGER_MOUSE_INTERACTION_TECHNIQUES__
#define __POLYHEDRAL_EXAMPLE_DEBUGGER_MOUSE_INTERACTION_TECHNIQUES__

#include "vsdk/toolkit/gui/MouseEvent.h"

class DebuggerModel;

/**
C++ port of Java's `gui.DebuggerMouseInteractionTechniques`. The events
arrive already converted to Vitral events (Java converts the AWT ones with
`AwtSystem.awt2vsdkEvent`).
*/
class DebuggerMouseInteractionTechniques {
public:
    void processMouseEntered(DebuggerModel& model);
    void processMouseExited(DebuggerModel& model);
    bool processMousePressed(DebuggerModel& model, const MouseEvent& e);
    bool processMouseReleased(DebuggerModel& model, const MouseEvent& e);
    bool processMouseClicked(DebuggerModel& model, const MouseEvent& e);
    bool processMouseMoved(DebuggerModel& model, const MouseEvent& e);
    bool processMouseDragged(DebuggerModel& model, const MouseEvent& e);
    bool processMouseWheelMoved(DebuggerModel& model, const MouseEvent& e);
};

#endif
