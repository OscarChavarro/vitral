#ifndef SHADERSEXAMPLE_GUI_SHADERSKEYBOARDINTERACTIONTECHNIQUES_H
#define SHADERSEXAMPLE_GUI_SHADERSKEYBOARDINTERACTIONTECHNIQUES_H

#include "vsdk/toolkit/gui/KeyEvent.h"
#include "vsdk/toolkit/gui/CameraControllerAquynza.h"
#include "vsdk/toolkit/gui/RendererConfigurationController.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "../model/ShaderOperationMode.h"

class ShadersModel;

class ShadersKeyboardInteractionTechniques {
public:
    class Actions {
    public:
        virtual ~Actions() {}
        virtual void requestExit() = 0;
        virtual void animationStateChanged() = 0;
    };

    bool processPressed(const vsdk::toolkit::gui::KeyEvent& event, ShadersModel* model, Actions* actions);
    bool processReleased(const vsdk::toolkit::gui::KeyEvent& event, ShadersModel* model);
    bool processPressedForApp(
        const vsdk::toolkit::gui::KeyEvent& event,
        vsdk::toolkit::gui::CameraControllerAquynza* cameraController,
        vsdk::toolkit::gui::RendererConfigurationController* qualityController,
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
        const vsdk::toolkit::gui::KeyEvent& event,
        vsdk::toolkit::gui::CameraControllerAquynza* cameraController);
};

#endif
