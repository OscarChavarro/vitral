#include "ShadersMouseInteractionTechniques.h"
#include "../model/ShadersModel.h"

bool ShadersMouseInteractionTechniques::processMousePressed(ShadersModel* model, const MouseEvent& e) { return model && model->cameraController->processMousePressedEvent(e); }
bool ShadersMouseInteractionTechniques::processMouseReleased(ShadersModel* model, const MouseEvent& e) { return model && model->cameraController->processMouseReleasedEvent(e); }
bool ShadersMouseInteractionTechniques::processMouseClicked(ShadersModel* model, const MouseEvent& e) { return model && model->cameraController->processMouseClickedEvent(e); }
bool ShadersMouseInteractionTechniques::processMouseMoved(ShadersModel* model, const MouseEvent& e) { return model && model->cameraController->processMouseMovedEvent(e); }
bool ShadersMouseInteractionTechniques::processMouseDragged(ShadersModel* model, const MouseEvent& e) { return model && model->cameraController->processMouseDraggedEvent(e); }
bool ShadersMouseInteractionTechniques::processMouseWheelMoved(ShadersModel* model, const MouseEvent& e) { return model && model->cameraController->processMouseWheelEvent(e); }
bool ShadersMouseInteractionTechniques::processMousePressedForApp(CameraControllerAquynza* cameraController, const MouseEvent& e) { return cameraController && cameraController->processMousePressedEvent(e); }
bool ShadersMouseInteractionTechniques::processMouseReleasedForApp(CameraControllerAquynza* cameraController, const MouseEvent& e) { return cameraController && cameraController->processMouseReleasedEvent(e); }
bool ShadersMouseInteractionTechniques::processMouseMovedForApp(CameraControllerAquynza* cameraController, const MouseEvent& e) { return cameraController && cameraController->processMouseMovedEvent(e); }
bool ShadersMouseInteractionTechniques::processMouseDraggedForApp(CameraControllerAquynza* cameraController, const MouseEvent& e) { return cameraController && cameraController->processMouseDraggedEvent(e); }
bool ShadersMouseInteractionTechniques::processMouseWheelMovedForApp(CameraControllerAquynza* cameraController, const MouseEvent& e) { return cameraController && cameraController->processMouseWheelEvent(e); }
