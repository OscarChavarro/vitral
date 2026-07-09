#ifndef __MESH_KEYBOARD_INTERACTION_TECHNIQUES__
#define __MESH_KEYBOARD_INTERACTION_TECHNIQUES__

class CameraController;
class KeyEvent;
class MeshModel;
class RendererConfigurationController;

class MeshKeyboardInteractionTechniques {
private:
    MeshModel* model;
    CameraController* cameraController;
    RendererConfigurationController* qualityController;
    bool* shouldClose;

public:
    MeshKeyboardInteractionTechniques(
        MeshModel* model,
        CameraController* cameraController,
        RendererConfigurationController* qualityController,
        bool* shouldClose);

    bool processKeyPressedEvent(const KeyEvent& event);
    bool processKeyReleasedEvent(const KeyEvent& event);
};

#endif
