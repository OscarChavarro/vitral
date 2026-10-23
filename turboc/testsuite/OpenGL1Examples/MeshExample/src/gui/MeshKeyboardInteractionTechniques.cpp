#include <stdio.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/gui/CameraController.h"
#include "vsdk/toolkit/gui/KeyEvent.h"
#include "vsdk/toolkit/gui/RendererConfigurationController.h"
#include "vsdk/toolkit/gui/gizmo/RayGizmoInteractionTechniques.h"
#include "gui/MeshKeyboardInteractionTechniques.h"
#include "model/MeshModel.h"

MeshKeyboardInteractionTechniques::MeshKeyboardInteractionTechniques(
    MeshModel* model,
    CameraController* cameraController,
    RendererConfigurationController* qualityController,
    BOOLEAN* shouldClose)
    : model(model),
      cameraController(cameraController),
      qualityController(qualityController),
      rayGizmoTechniques(new RayGizmoInteractionTechniques(
          model != 0 ? model->getRayGizmo() : 0,
          KeyEvent::KEY_r, KeyEvent::KEY_R)),
      shouldClose(shouldClose)
{
}

MeshKeyboardInteractionTechniques::~MeshKeyboardInteractionTechniques()
{
    delete rayGizmoTechniques;
}

BOOLEAN MeshKeyboardInteractionTechniques::processKeyPressedEvent(const KeyEvent& event)
{
    if ( event.keycode == KeyEvent::KEY_ESC ) {
        if ( shouldClose != 0 ) {
            *shouldClose = TRUE;
        }
        return TRUE;
    }
    if ( event.keycode == KeyEvent::KEY_I ) {
        printf("%s\n", model->getQualitySelection()->toString().c_str());
        return TRUE;
    }
    if ( rayGizmoTechniques != 0 &&
         rayGizmoTechniques->processKeyPressedEvent(event) ) {
        return TRUE;
    }
    if ( cameraController != 0 && cameraController->processKeyPressedEvent(event) ) {
        return TRUE;
    }
    if ( qualityController != 0 && qualityController->processKeyPressedEvent(event) ) {
        printf("%s\n", model->getQualitySelection()->toString().c_str());
        return TRUE;
    }

    return FALSE;
}

BOOLEAN MeshKeyboardInteractionTechniques::processKeyReleasedEvent(const KeyEvent& event)
{
    if ( cameraController != 0 && cameraController->processKeyReleasedEvent(event) ) {
        return TRUE;
    }
    if ( qualityController != 0 && qualityController->processKeyReleasedEvent(event) ) {
        return TRUE;
    }
    return FALSE;
}
