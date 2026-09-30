#ifndef __SOLID_TEXTURE_MOUSE_INTERACTION_TECHNIQUES__
#define __SOLID_TEXTURE_MOUSE_INTERACTION_TECHNIQUES__

class CameraController;
class MouseEvent;

class SolidTextureMouseInteractionTechniques {
private:
    CameraController* cameraController;

public:
    explicit SolidTextureMouseInteractionTechniques(CameraController* cameraController);
    bool processMousePressedEvent(MouseEvent& event);
    bool processMouseReleasedEvent(MouseEvent& event);
    bool processMouseClickedEvent(MouseEvent& event);
    bool processMouseMovedEvent(MouseEvent& event);
    bool processMouseDraggedEvent(MouseEvent& event);
    bool processMouseWheelEvent(MouseEvent& event);
};

#endif
