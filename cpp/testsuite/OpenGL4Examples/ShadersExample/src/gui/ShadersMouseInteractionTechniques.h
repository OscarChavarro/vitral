#ifndef SHADERSEXAMPLE_GUI_SHADERSMOUSEINTERACTIONTECHNIQUES_H
#define SHADERSEXAMPLE_GUI_SHADERSMOUSEINTERACTIONTECHNIQUES_H

#include "vsdk/toolkit/gui/MouseEvent.h"
#include "vsdk/toolkit/gui/CameraControllerAquynza.h"

class ShadersModel;

class ShadersMouseInteractionTechniques {
public:
    bool processMousePressed(ShadersModel* model, const vsdk::toolkit::gui::MouseEvent& e);
    bool processMouseReleased(ShadersModel* model, const vsdk::toolkit::gui::MouseEvent& e);
    bool processMouseClicked(ShadersModel* model, const vsdk::toolkit::gui::MouseEvent& e);
    bool processMouseMoved(ShadersModel* model, const vsdk::toolkit::gui::MouseEvent& e);
    bool processMouseDragged(ShadersModel* model, const vsdk::toolkit::gui::MouseEvent& e);
    bool processMouseWheelMoved(ShadersModel* model, const vsdk::toolkit::gui::MouseEvent& e);
    bool processMousePressedForApp(vsdk::toolkit::gui::CameraControllerAquynza* cameraController, const vsdk::toolkit::gui::MouseEvent& e);
    bool processMouseReleasedForApp(vsdk::toolkit::gui::CameraControllerAquynza* cameraController, const vsdk::toolkit::gui::MouseEvent& e);
    bool processMouseMovedForApp(vsdk::toolkit::gui::CameraControllerAquynza* cameraController, const vsdk::toolkit::gui::MouseEvent& e);
    bool processMouseDraggedForApp(vsdk::toolkit::gui::CameraControllerAquynza* cameraController, const vsdk::toolkit::gui::MouseEvent& e);
    bool processMouseWheelMovedForApp(vsdk::toolkit::gui::CameraControllerAquynza* cameraController, const vsdk::toolkit::gui::MouseEvent& e);
};

#endif
