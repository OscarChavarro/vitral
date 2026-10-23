#ifndef __VIEWPORT_INTERACTION_TECHNIQUES__
#define __VIEWPORT_INTERACTION_TECHNIQUES__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/gui/KeyEvent.h"
#include "vsdk/toolkit/gui/MouseEvent.h"
#include "vsdk/toolkit/gui/gizmo/TranslateGizmoInteractionTechnique.h"

class Camera;
class CameraController;
class InputGizmo;
class RendererConfiguration;
class RendererConfigurationController;
class RotateGizmo;
class RotateGizmoInteractionTechnique;
class ScaleGizmo;
class ScaleGizmoInteractionTechnique;
class TranslateGizmo;
class Viewport;

/**
Set of interaction techniques (camera controller, renderer configuration
controller and transformation gizmos with their techniques) used over the
viewports of a `ViewportSet`. It owns all of them; the camera and the
renderer configuration are referenced, not owned.
*/
class ViewportInteractionTechniques {
private:
    CameraController* cameraController;
    RendererConfigurationController* qualityController;
    TranslateGizmo* translationGizmo;
    TranslateGizmoInteractionTechnique* translationTechnique;
    RotateGizmo* rotateGizmo;
    RotateGizmoInteractionTechnique* rotationTechnique;
    ScaleGizmo* scaleGizmo;
    ScaleGizmoInteractionTechnique* scaleTechnique;

    ViewportInteractionTechniques(const ViewportInteractionTechniques& other);
    ViewportInteractionTechniques& operator=(
        const ViewportInteractionTechniques& other);

public:
    ViewportInteractionTechniques(Camera* camera,
                                  RendererConfiguration* rendererConfiguration);
    virtual ~ViewportInteractionTechniques();

    CameraController* getCameraController() const;
    RendererConfigurationController* getQualityController() const;
    TranslateGizmo* getTranslationGizmo() const;
    TranslateGizmoInteractionTechnique* getTranslationTechnique() const;
    RotateGizmo* getRotateGizmo() const;
    RotateGizmoInteractionTechnique* getRotationTechnique() const;
    ScaleGizmo* getScaleGizmo() const;
    void setCamera(Camera* camera);
    void setRendererConfiguration(RendererConfiguration* rendererConfiguration);

    BOOLEAN processCameraKeyPressedEvent(const KeyEvent& event);
    BOOLEAN processCameraKeyReleasedEvent(const KeyEvent& event);
    BOOLEAN processCameraMousePressedEvent(const MouseEvent& event);
    BOOLEAN processCameraMouseReleasedEvent(const MouseEvent& event);
    BOOLEAN processCameraMouseClickedEvent(const MouseEvent& event);
    BOOLEAN processCameraMouseMovedEvent(const MouseEvent& event);
    BOOLEAN processCameraMouseDraggedEvent(const MouseEvent& event);
    BOOLEAN processCameraMouseWheelEvent(const MouseEvent& event);
    BOOLEAN processQualityKeyPressedEvent(const KeyEvent& event);

    /**
    @return the input gizmo that shows and edits the coordinates of the
    translation gizmo
    */
    InputGizmo* getTranslationInputGizmo();

    /**
    @return true if the input gizmo of the translation gizmo uses the key
    */
    BOOLEAN isTranslationInputGizmoKey(const KeyEvent& event);
    BOOLEAN processTranslationKeyPressedEvent(const KeyEvent& event);
    BOOLEAN processTranslationMousePressedEvent(const MouseEvent& event);

    /**
    Processes the press of a mouse button over a viewport, starting a
    translation gesture confined to it (see `getTranslationDragViewport`).
    @return false (a press never changes the gizmo)
    */
    BOOLEAN processTranslationMousePressedEvent(const MouseEvent& event,
                                             Viewport* viewport);

    /**
    @return the viewport where the translation gesture in course started, or
    null if there is none
    */
    Viewport* getTranslationDragViewport() const;

    /**
    @param enabled true if the caller is able to place the cursor when the
    translation technique requests it
    */
    void setTranslationCursorWrapEnabled(BOOLEAN enabled);

    /**
    @param outWarp the pending request to place the cursor while dragging
    @return true if there was a pending request (the Java version returns
    null when there is none)
    */
    BOOLEAN consumeTranslationCursorWarp(
        TranslateGizmoInteractionTechnique::CursorWarp* outWarp);
    BOOLEAN processTranslationMouseReleasedEvent(const MouseEvent& event);
    BOOLEAN processTranslationMouseClickedEvent(const MouseEvent& event);
    BOOLEAN processTranslationMouseMovedEvent(const MouseEvent& event);
    BOOLEAN processTranslationMouseDraggedEvent(const MouseEvent& event);

    /**
    @return the input gizmo that shows and edits the angles of the rotation
    gizmo
    */
    InputGizmo* getRotationInputGizmo();
    BOOLEAN isRotationInputGizmoKey(const KeyEvent& event);
    BOOLEAN processRotateKeyPressedEvent(const KeyEvent& event);
    BOOLEAN processRotationMousePressedEvent(const MouseEvent& event);

    /**
    Processes the press of a mouse button over a viewport, starting a
    rotation gesture confined to it if the pointer is over a ring.
    @return false (a press never changes the gizmo)
    */
    BOOLEAN processRotationMousePressedEvent(const MouseEvent& event,
                                          Viewport* viewport);
    Viewport* getRotationDragViewport() const;
    BOOLEAN processRotationMouseReleasedEvent(const MouseEvent& event);
    BOOLEAN processRotationMouseDraggedEvent(const MouseEvent& event);
    BOOLEAN processRotationMouseClickedEvent(const MouseEvent& event);
    BOOLEAN processRotationMouseMovedEvent(const MouseEvent& event);
    BOOLEAN processScaleKeyPressedEvent(const KeyEvent& event);

    /**
    @return the input gizmo that shows and edits the scale factors of the
    scale gizmo
    */
    InputGizmo* getScaleInputGizmo();
    BOOLEAN isScaleInputGizmoKey(const KeyEvent& event);

    /**
    @return the interaction technique that hovers, selects and drags the
    handles of the scale gizmo
    */
    ScaleGizmoInteractionTechnique* getScaleTechnique() const;
    BOOLEAN processScaleMousePressedEvent(const MouseEvent& event);

    /**
    Processes the press of a mouse button over a viewport, starting a scale
    gesture confined to it if the pointer is over a handle.
    @return false (a press never changes the gizmo)
    */
    BOOLEAN processScaleMousePressedEvent(const MouseEvent& event,
                                       Viewport* viewport);
    Viewport* getScaleDragViewport() const;
    BOOLEAN processScaleMouseReleasedEvent(const MouseEvent& event);
    BOOLEAN processScaleMouseDraggedEvent(const MouseEvent& event);
    BOOLEAN processScaleMouseClickedEvent(const MouseEvent& event);
    BOOLEAN processScaleMouseMovedEvent(const MouseEvent& event);
};

#endif
