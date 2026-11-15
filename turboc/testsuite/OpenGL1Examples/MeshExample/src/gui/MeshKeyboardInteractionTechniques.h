#ifndef __MSH_KYBRD_INTRCTN_TCHNQS__
#define __MSH_KYBRD_INTRCTN_TCHNQS__

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
