#ifndef __MESH_KEYBOARD_INTERACTION_TECHNIQUES__
#define __MESH_KEYBOARD_INTERACTION_TECHNIQUES__

#include "vsdk/toolkit/common/VSDK.h"

class CameraController;
class KeyEvent;
class MeshModel;
class RayGizmoInteractionTechniques;
class RendererConfigurationController;

class MeshKeyboardInteractionTechniques {
private:
    MeshModel* model;
    CameraController* cameraController;
    RendererConfigurationController* qualityController;
    RayGizmoInteractionTechniques* rayGizmoTechniques;
    BOOLEAN* shouldClose;

public:
    MeshKeyboardInteractionTechniques(
        MeshModel* model,
        CameraController* cameraController,
        RendererConfigurationController* qualityController,
        BOOLEAN* shouldClose);
    ~MeshKeyboardInteractionTechniques();

    BOOLEAN processKeyPressedEvent(const KeyEvent& event);
    BOOLEAN processKeyReleasedEvent(const KeyEvent& event);
};

#endif
