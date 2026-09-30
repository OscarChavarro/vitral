#include <cstdio>

#include "vsdk/toolkit/gui/CameraController.h"
#include "vsdk/toolkit/gui/KeyEvent.h"
#include "vsdk/toolkit/gui/RendererConfigurationController.h"
#include "vsdk/toolkit/gui/gizmo/RayGizmoInteractionTechniques.h"
#include "gui/SolidTextureKeyboardInteractionTechniques.h"
#include "model/SolidTextureModel.h"

SolidTextureKeyboardInteractionTechniques::SolidTextureKeyboardInteractionTechniques(
    SolidTextureModel* model, CameraController* cameraController,
    RendererConfigurationController* qualityController, bool* shouldClose)
    : model(model), cameraController(cameraController),
      qualityController(qualityController),
      rayGizmoTechniques(new RayGizmoInteractionTechniques(
          model != 0 ? model->getRayGizmo() : 0,
          KeyEvent::KEY_r, KeyEvent::KEY_R)),
      shouldClose(shouldClose)
{
}

SolidTextureKeyboardInteractionTechniques::~SolidTextureKeyboardInteractionTechniques()
{
    delete rayGizmoTechniques;
}

bool SolidTextureKeyboardInteractionTechniques::processKeyPressedEvent(const KeyEvent& event)
{
    if ( event.keycode == KeyEvent::KEY_ESC ) {
        if ( shouldClose != 0 ) *shouldClose = true;
        return true;
    }
    if ( event.keycode == KeyEvent::KEY_1 ) { model->rotateOperationMode(); return true; }
    if ( event.keycode == KeyEvent::KEY_2 ) { model->decreaseSolidTextureSize(); return true; }
    if ( event.keycode == KeyEvent::KEY_3 ) { model->increaseSolidTextureSize(); return true; }
    if ( event.keycode == KeyEvent::KEY_4 ) { model->selectPreviousSolidTexture(); return true; }
    if ( event.keycode == KeyEvent::KEY_5 ) { model->selectNextSolidTexture(); return true; }
    if ( rayGizmoTechniques->processKeyPressedEvent(event) ) return true;
    if ( event.keycode == KeyEvent::KEY_a || event.keycode == KeyEvent::KEY_A ) {
        model->toggleAnimationEnabled(); return true;
    }
    if ( event.keycode == KeyEvent::KEY_h || event.keycode == KeyEvent::KEY_H ) {
        model->toggleHudVisible(); return true;
    }
    if ( event.keycode == KeyEvent::KEY_I ) {
        std::printf("%s\n", model->getQualitySelection()->toString().c_str());
        return true;
    }
    if ( cameraController != 0 && cameraController->processKeyPressedEvent(event) ) return true;
    if ( qualityController != 0 && qualityController->processKeyPressedEvent(event) ) {
        std::printf("%s\n", model->getQualitySelection()->toString().c_str());
        return true;
    }
    return false;
}

bool SolidTextureKeyboardInteractionTechniques::processKeyReleasedEvent(const KeyEvent& event)
{
    if ( cameraController != 0 && cameraController->processKeyReleasedEvent(event) ) return true;
    return qualityController != 0 && qualityController->processKeyReleasedEvent(event);
}
