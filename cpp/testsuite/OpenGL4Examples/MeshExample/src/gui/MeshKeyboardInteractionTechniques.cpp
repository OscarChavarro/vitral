#include <cstdio>

#include "vsdk/toolkit/gui/CameraController.h"
#include "vsdk/toolkit/gui/KeyEvent.h"
#include "vsdk/toolkit/gui/RendererConfigurationController.h"
#include "gui/MeshKeyboardInteractionTechniques.h"
#include "model/MeshModel.h"

MeshKeyboardInteractionTechniques::MeshKeyboardInteractionTechniques(
    MeshModel* model,
    CameraController* cameraController,
    RendererConfigurationController* qualityController,
    bool* shouldClose)
    : model(model),
      cameraController(cameraController),
      qualityController(qualityController),
      shouldClose(shouldClose)
{
}

bool MeshKeyboardInteractionTechniques::processKeyPressedEvent(const KeyEvent& event)
{
    if ( event.keycode == KeyEvent::KEY_ESC ) {
        if ( shouldClose != 0 ) {
            *shouldClose = true;
        }
        return true;
    }
    if ( event.keycode == KeyEvent::KEY_I ) {
        std::printf("%s\n", model->getQualitySelection()->toString().c_str());
        return true;
    }
    if ( cameraController != 0 && cameraController->processKeyPressedEvent(event) ) {
        return true;
    }
    if ( qualityController != 0 && qualityController->processKeyPressedEvent(event) ) {
        std::printf("%s\n", model->getQualitySelection()->toString().c_str());
        return true;
    }

    return false;
}

bool MeshKeyboardInteractionTechniques::processKeyReleasedEvent(const KeyEvent& event)
{
    if ( cameraController != 0 && cameraController->processKeyReleasedEvent(event) ) {
        return true;
    }
    if ( qualityController != 0 && qualityController->processKeyReleasedEvent(event) ) {
        return true;
    }
    return false;
}
