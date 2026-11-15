#ifndef __MSH_MS_INTRCTN_TCHNQS__
#define __MSH_MS_INTRCTN_TCHNQS__

#include "vsdk/toolkit/common/VSDK.h"

class CameraController;
class MouseEvent;

class MeshMouseInteractionTechniques {
private:
    CameraController* cameraController;

public:
    explicit MeshMouseInteractionTechniques(CameraController* cameraController);

    BOOLEAN processMousePressedEvent(const MouseEvent& event);
    BOOLEAN processMouseReleasedEvent(const MouseEvent& event);
    BOOLEAN processMouseClickedEvent(const MouseEvent& event);
    BOOLEAN processMouseMovedEvent(const MouseEvent& event);
    BOOLEAN processMouseDraggedEvent(const MouseEvent& event);
    BOOLEAN processMouseWheelEvent(const MouseEvent& event);
};

#endif
