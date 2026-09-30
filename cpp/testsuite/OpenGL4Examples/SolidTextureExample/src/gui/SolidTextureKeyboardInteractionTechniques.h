#ifndef __SOLID_TEXTURE_KEYBOARD_INTERACTION_TECHNIQUES__
#define __SOLID_TEXTURE_KEYBOARD_INTERACTION_TECHNIQUES__

class CameraController;
class KeyEvent;
class RayGizmoInteractionTechniques;
class RendererConfigurationController;
class SolidTextureModel;

class SolidTextureKeyboardInteractionTechniques {
private:
    SolidTextureModel* model;
    CameraController* cameraController;
    RendererConfigurationController* qualityController;
    RayGizmoInteractionTechniques* rayGizmoTechniques;
    bool* shouldClose;

public:
    SolidTextureKeyboardInteractionTechniques(
        SolidTextureModel* model, CameraController* cameraController,
        RendererConfigurationController* qualityController, bool* shouldClose);
    ~SolidTextureKeyboardInteractionTechniques();
    bool processKeyPressedEvent(const KeyEvent& event);
    bool processKeyReleasedEvent(const KeyEvent& event);
};

#endif
