#ifndef __SHADERSKEYBOARDINTERACTIONTECHNIQUES__
#define __SHADERSKEYBOARDINTERACTIONTECHNIQUES__

#include "../model/ShaderOperationMode.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/gui/CameraControllerAquynza.h"
#include "vsdk/toolkit/gui/KeyEvent.h"
#include "vsdk/toolkit/gui/RendererConfigurationController.h"
class ShadersModel;

class ShadersKeyboardInteractionTechniques {
public:
    class Actions {
    public:
        virtual ~Actions() {}
        virtual void requestExit() = 0;
        virtual void animationStateChanged() = 0;
    };

    bool processPressed(const KeyEvent& event, ShadersModel* model, Actions* actions);
    bool processReleased(const KeyEvent& event, ShadersModel* model);
    bool processPressedForApp(
        const KeyEvent& event,
        CameraControllerAquynza* cameraController,
        RendererConfigurationController* qualityController,
        Light* light,
        RendererConfiguration* quality,
        int* meridians,
        int* parallels,
        bool* showHud,
        bool* animationEnabled,
        bool* lightAnimationEnabled,
        ShaderOperationMode* renderingMode,
        Actions* actions);
    bool processReleasedForApp(
        const KeyEvent& event,
        CameraControllerAquynza* cameraController);
};

#endif
