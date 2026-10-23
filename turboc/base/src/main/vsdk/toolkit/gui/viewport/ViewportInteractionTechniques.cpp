#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/gui/CameraControllerAquynza.h"
#include "vsdk/toolkit/gui/RendererConfigurationController.h"
#include "vsdk/toolkit/gui/gizmo/InputGizmo.h"
#include "vsdk/toolkit/gui/gizmo/RotateGizmo.h"
#include "vsdk/toolkit/gui/gizmo/RotateGizmoInteractionTechnique.h"
#include "vsdk/toolkit/gui/gizmo/ScaleGizmo.h"
#include "vsdk/toolkit/gui/gizmo/ScaleGizmoInteractionTechnique.h"
#include "vsdk/toolkit/gui/gizmo/TranslateGizmo.h"
#include "vsdk/toolkit/gui/viewport/ViewportInteractionTechniques.h"

ViewportInteractionTechniques::ViewportInteractionTechniques(
    Camera* camera, RendererConfiguration* rendererConfiguration)
{
    cameraController = new CameraControllerAquynza(camera);
    qualityController =
        new RendererConfigurationController(rendererConfiguration);
    translationGizmo = new TranslateGizmo(camera);
    translationTechnique =
        new TranslateGizmoInteractionTechnique(translationGizmo);
    rotateGizmo = new RotateGizmo(camera);
    rotationTechnique = new RotateGizmoInteractionTechnique(rotateGizmo);
    scaleGizmo = new ScaleGizmo(camera);
    scaleTechnique = new ScaleGizmoInteractionTechnique(scaleGizmo);
}

ViewportInteractionTechniques::~ViewportInteractionTechniques()
{
    delete scaleTechnique;
    delete scaleGizmo;
    delete rotationTechnique;
    delete rotateGizmo;
    delete translationTechnique;
    delete translationGizmo;
    delete qualityController;
    delete cameraController;
}

CameraController* ViewportInteractionTechniques::getCameraController() const
{
    return cameraController;
}

RendererConfigurationController*
ViewportInteractionTechniques::getQualityController() const
{
    return qualityController;
}

TranslateGizmo* ViewportInteractionTechniques::getTranslationGizmo() const
{
    return translationGizmo;
}

TranslateGizmoInteractionTechnique*
ViewportInteractionTechniques::getTranslationTechnique() const
{
    return translationTechnique;
}

RotateGizmo* ViewportInteractionTechniques::getRotateGizmo() const
{
    return rotateGizmo;
}

RotateGizmoInteractionTechnique*
ViewportInteractionTechniques::getRotationTechnique() const
{
    return rotationTechnique;
}

ScaleGizmo* ViewportInteractionTechniques::getScaleGizmo() const
{
    return scaleGizmo;
}

void ViewportInteractionTechniques::setCamera(Camera* camera)
{
    cameraController->setCamera(camera);
}

void ViewportInteractionTechniques::setRendererConfiguration(
    RendererConfiguration* rendererConfiguration)
{
    qualityController->setRendererConfiguration(rendererConfiguration);
}

BOOLEAN ViewportInteractionTechniques::processCameraKeyPressedEvent(
    const KeyEvent& event)
{
    return cameraController->processKeyPressedEvent(event);
}

BOOLEAN ViewportInteractionTechniques::processCameraKeyReleasedEvent(
    const KeyEvent& event)
{
    return cameraController->processKeyReleasedEvent(event);
}

BOOLEAN ViewportInteractionTechniques::processCameraMousePressedEvent(
    const MouseEvent& event)
{
    return cameraController->processMousePressedEvent(event);
}

BOOLEAN ViewportInteractionTechniques::processCameraMouseReleasedEvent(
    const MouseEvent& event)
{
    return cameraController->processMouseReleasedEvent(event);
}

BOOLEAN ViewportInteractionTechniques::processCameraMouseClickedEvent(
    const MouseEvent& event)
{
    return cameraController->processMouseClickedEvent(event);
}

BOOLEAN ViewportInteractionTechniques::processCameraMouseMovedEvent(
    const MouseEvent& event)
{
    return cameraController->processMouseMovedEvent(event);
}

BOOLEAN ViewportInteractionTechniques::processCameraMouseDraggedEvent(
    const MouseEvent& event)
{
    return cameraController->processMouseDraggedEvent(event);
}

BOOLEAN ViewportInteractionTechniques::processCameraMouseWheelEvent(
    const MouseEvent& event)
{
    return cameraController->processMouseWheelEvent(event);
}

BOOLEAN ViewportInteractionTechniques::processQualityKeyPressedEvent(
    const KeyEvent& event)
{
    return qualityController->processKeyPressedEvent(event);
}

InputGizmo* ViewportInteractionTechniques::getTranslationInputGizmo()
{
    return translationGizmo->getInputGizmo();
}

BOOLEAN ViewportInteractionTechniques::isTranslationInputGizmoKey(
    const KeyEvent& event)
{
    return translationTechnique->isInputGizmoKey(event);
}

BOOLEAN ViewportInteractionTechniques::processTranslationKeyPressedEvent(
    const KeyEvent& event)
{
    return translationTechnique->processKeyPressedEvent(event);
}

BOOLEAN ViewportInteractionTechniques::processTranslationMousePressedEvent(
    const MouseEvent& event)
{
    return translationTechnique->processMousePressedEvent(event);
}

BOOLEAN ViewportInteractionTechniques::processTranslationMousePressedEvent(
    const MouseEvent& event, Viewport* viewport)
{
    return translationTechnique->processMousePressedEvent(event, viewport);
}

Viewport* ViewportInteractionTechniques::getTranslationDragViewport() const
{
    return translationTechnique->getDragViewport();
}

void ViewportInteractionTechniques::setTranslationCursorWrapEnabled(
    BOOLEAN enabled)
{
    translationTechnique->setCursorWrapEnabled(enabled);
}

BOOLEAN ViewportInteractionTechniques::consumeTranslationCursorWarp(
    TranslateGizmoInteractionTechnique::CursorWarp* outWarp)
{
    return translationTechnique->consumeCursorWarp(outWarp);
}

BOOLEAN ViewportInteractionTechniques::processTranslationMouseReleasedEvent(
    const MouseEvent& event)
{
    return translationTechnique->processMouseReleasedEvent(event);
}

BOOLEAN ViewportInteractionTechniques::processTranslationMouseClickedEvent(
    const MouseEvent& event)
{
    return translationTechnique->processMouseClickedEvent(event);
}

BOOLEAN ViewportInteractionTechniques::processTranslationMouseMovedEvent(
    const MouseEvent& event)
{
    return translationTechnique->processMouseMovedEvent(event);
}

BOOLEAN ViewportInteractionTechniques::processTranslationMouseDraggedEvent(
    const MouseEvent& event)
{
    return translationTechnique->processMouseDraggedEvent(event);
}

InputGizmo* ViewportInteractionTechniques::getRotationInputGizmo()
{
    return rotateGizmo->getInputGizmo();
}

BOOLEAN ViewportInteractionTechniques::isRotationInputGizmoKey(
    const KeyEvent& event)
{
    return rotationTechnique->isInputGizmoKey(event);
}

BOOLEAN ViewportInteractionTechniques::processRotateKeyPressedEvent(
    const KeyEvent& event)
{
    return rotationTechnique->processKeyPressedEvent(event);
}

BOOLEAN ViewportInteractionTechniques::processRotationMousePressedEvent(
    const MouseEvent& event)
{
    return rotationTechnique->processMousePressedEvent(event);
}

BOOLEAN ViewportInteractionTechniques::processRotationMousePressedEvent(
    const MouseEvent& event, Viewport* viewport)
{
    return rotationTechnique->processMousePressedEvent(event, viewport);
}

Viewport* ViewportInteractionTechniques::getRotationDragViewport() const
{
    return rotationTechnique->getDragViewport();
}

BOOLEAN ViewportInteractionTechniques::processRotationMouseReleasedEvent(
    const MouseEvent& event)
{
    return rotationTechnique->processMouseReleasedEvent(event);
}

BOOLEAN ViewportInteractionTechniques::processRotationMouseDraggedEvent(
    const MouseEvent& event)
{
    return rotationTechnique->processMouseDraggedEvent(event);
}

BOOLEAN ViewportInteractionTechniques::processRotationMouseClickedEvent(
    const MouseEvent& event)
{
    return rotationTechnique->processMouseClickedEvent(event);
}

BOOLEAN ViewportInteractionTechniques::processRotationMouseMovedEvent(
    const MouseEvent& event)
{
    return rotationTechnique->processMouseMovedEvent(event);
}

BOOLEAN ViewportInteractionTechniques::processScaleKeyPressedEvent(
    const KeyEvent& event)
{
    return scaleTechnique->processKeyPressedEvent(event);
}

InputGizmo* ViewportInteractionTechniques::getScaleInputGizmo()
{
    return scaleGizmo->getInputGizmo();
}

BOOLEAN ViewportInteractionTechniques::isScaleInputGizmoKey(
    const KeyEvent& event)
{
    return scaleTechnique->isInputGizmoKey(event);
}

ScaleGizmoInteractionTechnique*
ViewportInteractionTechniques::getScaleTechnique() const
{
    return scaleTechnique;
}

BOOLEAN ViewportInteractionTechniques::processScaleMousePressedEvent(
    const MouseEvent& event)
{
    return scaleTechnique->processMousePressedEvent(event);
}

BOOLEAN ViewportInteractionTechniques::processScaleMousePressedEvent(
    const MouseEvent& event, Viewport* viewport)
{
    return scaleTechnique->processMousePressedEvent(event, viewport);
}

Viewport* ViewportInteractionTechniques::getScaleDragViewport() const
{
    return scaleTechnique->getDragViewport();
}

BOOLEAN ViewportInteractionTechniques::processScaleMouseReleasedEvent(
    const MouseEvent& event)
{
    return scaleTechnique->processMouseReleasedEvent(event);
}

BOOLEAN ViewportInteractionTechniques::processScaleMouseDraggedEvent(
    const MouseEvent& event)
{
    return scaleTechnique->processMouseDraggedEvent(event);
}

BOOLEAN ViewportInteractionTechniques::processScaleMouseClickedEvent(
    const MouseEvent& event)
{
    return scaleTechnique->processMouseClickedEvent(event);
}

BOOLEAN ViewportInteractionTechniques::processScaleMouseMovedEvent(
    const MouseEvent& event)
{
    return scaleTechnique->processMouseMovedEvent(event);
}
