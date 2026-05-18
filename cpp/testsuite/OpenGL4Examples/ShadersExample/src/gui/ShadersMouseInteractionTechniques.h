#ifndef SHADERSEXAMPLE_GUI_SHADERSMOUSEINTERACTIONTECHNIQUES_H
#define SHADERSEXAMPLE_GUI_SHADERSMOUSEINTERACTIONTECHNIQUES_H

#include "vsdk/toolkit/gui/MouseEvent.h"
#include "vsdk/toolkit/gui/CameraControllerAquynza.h"

class ShadersModel;

class ShadersMouseInteractionTechniques {
public:
    bool processMousePressed(ShadersModel* model, const MouseEvent& e);
    bool processMouseReleased(ShadersModel* model, const MouseEvent& e);
    bool processMouseClicked(ShadersModel* model, const MouseEvent& e);
    bool processMouseMoved(ShadersModel* model, const MouseEvent& e);
    bool processMouseDragged(ShadersModel* model, const MouseEvent& e);
    bool processMouseWheelMoved(ShadersModel* model, const MouseEvent& e);
    bool processMousePressedForApp(CameraControllerAquynza* cameraController, const MouseEvent& e);
    bool processMouseReleasedForApp(CameraControllerAquynza* cameraController, const MouseEvent& e);
    bool processMouseMovedForApp(CameraControllerAquynza* cameraController, const MouseEvent& e);
    bool processMouseDraggedForApp(CameraControllerAquynza* cameraController, const MouseEvent& e);
    bool processMouseWheelMovedForApp(CameraControllerAquynza* cameraController, const MouseEvent& e);
};

#endif
