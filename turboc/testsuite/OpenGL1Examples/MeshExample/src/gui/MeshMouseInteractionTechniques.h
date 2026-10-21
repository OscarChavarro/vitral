#ifndef __MESH_MOUSE_INTERACTION_TECHNIQUES__
#define __MESH_MOUSE_INTERACTION_TECHNIQUES__

class CameraController;
class MouseEvent;

class MeshMouseInteractionTechniques {
private:
    CameraController* cameraController;

public:
    explicit MeshMouseInteractionTechniques(CameraController* cameraController);

    bool processMousePressedEvent(const MouseEvent& event);
    bool processMouseReleasedEvent(const MouseEvent& event);
    bool processMouseClickedEvent(const MouseEvent& event);
    bool processMouseMovedEvent(const MouseEvent& event);
    bool processMouseDraggedEvent(const MouseEvent& event);
    bool processMouseWheelEvent(const MouseEvent& event);
};

#endif
