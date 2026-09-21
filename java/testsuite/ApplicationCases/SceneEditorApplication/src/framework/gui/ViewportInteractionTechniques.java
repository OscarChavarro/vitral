package framework.gui;

import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.KeyEvent;
import vsdk.toolkit.gui.MouseEvent;
import vsdk.toolkit.gui.RendererConfigurationController;
import vsdk.toolkit.gui.gizmo.InputGizmo;
import vsdk.toolkit.gui.gizmo.RotateGizmo;
import vsdk.toolkit.gui.gizmo.ScaleGizmo;
import vsdk.toolkit.gui.gizmo.TranslateGizmo;
import vsdk.toolkit.gui.gizmo.TranslateGizmoInteractionTechnique;
import vsdk.toolkit.gui.viewport.Viewport;

public class ViewportInteractionTechniques
{
    private final CameraController cameraController;
    private final RendererConfigurationController qualityController;
    private final TranslateGizmo translationGizmo;
    private final TranslateGizmoInteractionTechnique translationTechnique;
    private final RotateGizmo rotateGizmo;
    private final ScaleGizmo scaleGizmo;

    public ViewportInteractionTechniques(Camera camera,
                                         RendererConfiguration rendererConfiguration)
    {
        cameraController = new CameraControllerAquynza(camera);
        qualityController = new RendererConfigurationController(rendererConfiguration);
        translationGizmo = new TranslateGizmo(camera);
        translationTechnique = new TranslateGizmoInteractionTechnique(translationGizmo);
        rotateGizmo = new RotateGizmo();
        scaleGizmo = new ScaleGizmo();
    }

    public CameraController getCameraController()
    {
        return cameraController;
    }

    public RendererConfigurationController getQualityController()
    {
        return qualityController;
    }

    public TranslateGizmo getTranslationGizmo()
    {
        return translationGizmo;
    }

    public TranslateGizmoInteractionTechnique getTranslationTechnique()
    {
        return translationTechnique;
    }

    public RotateGizmo getRotateGizmo()
    {
        return rotateGizmo;
    }

    public ScaleGizmo getScaleGizmo()
    {
        return scaleGizmo;
    }

    public void setCamera(Camera camera)
    {
        cameraController.setCamera(camera);
    }

    public void setRendererConfiguration(RendererConfiguration rendererConfiguration)
    {
        qualityController.setRendererConfiguration(rendererConfiguration);
    }

    public boolean processCameraKeyPressedEvent(KeyEvent event)
    {
        return cameraController.processKeyPressedEvent(event);
    }

    public boolean processCameraKeyReleasedEvent(KeyEvent event)
    {
        return cameraController.processKeyReleasedEvent(event);
    }

    public boolean processCameraMousePressedEvent(MouseEvent event)
    {
        return cameraController.processMousePressedEvent(event);
    }

    public boolean processCameraMouseReleasedEvent(MouseEvent event)
    {
        return cameraController.processMouseReleasedEvent(event);
    }

    public boolean processCameraMouseClickedEvent(MouseEvent event)
    {
        return cameraController.processMouseClickedEvent(event);
    }

    public boolean processCameraMouseMovedEvent(MouseEvent event)
    {
        return cameraController.processMouseMovedEvent(event);
    }

    public boolean processCameraMouseDraggedEvent(MouseEvent event)
    {
        return cameraController.processMouseDraggedEvent(event);
    }

    public boolean processCameraMouseWheelEvent(MouseEvent event)
    {
        return cameraController.processMouseWheelEvent(event);
    }

    public boolean processQualityKeyPressedEvent(KeyEvent event)
    {
        return qualityController.processKeyPressedEvent(event);
    }

    /**
    @return the input gizmo that shows and edits the coordinates of the
    translation gizmo
    */
    public InputGizmo getTranslationInputGizmo()
    {
        return translationGizmo.getInputGizmo();
    }

    /**
    @param event key press
    @return true if the input gizmo of the translation gizmo uses the key
    */
    public boolean isTranslationInputGizmoKey(KeyEvent event)
    {
        return translationTechnique.isInputGizmoKey(event);
    }

    public boolean processTranslationKeyPressedEvent(KeyEvent event)
    {
        return translationTechnique.processKeyPressedEvent(event);
    }

    public boolean processTranslationMousePressedEvent(MouseEvent event)
    {
        return translationTechnique.processMousePressedEvent(event);
    }

    /**
    Processes the press of a mouse button over a viewport, starting a
    translation gesture confined to it (see `getTranslationDragViewport`).
    @param event event with coordinates relative to the viewport
    @param viewport viewport where the button was pressed
    @return false (a press never changes the gizmo)
    */
    public boolean processTranslationMousePressedEvent(MouseEvent event, Viewport viewport)
    {
        return translationTechnique.processMousePressedEvent(event, viewport);
    }

    /**
    @return the viewport where the translation gesture in course started, or
    null if there is none
    */
    public Viewport getTranslationDragViewport()
    {
        return translationTechnique.getDragViewport();
    }

    /**
    @param enabled true if the caller is able to place the cursor when the
    translation technique requests it
    */
    public void setTranslationCursorWrapEnabled(boolean enabled)
    {
        translationTechnique.setCursorWrapEnabled(enabled);
    }

    /**
    @return the pending request to place the cursor while dragging, or null
    */
    public TranslateGizmoInteractionTechnique.CursorWarp consumeTranslationCursorWarp()
    {
        return translationTechnique.consumeCursorWarp();
    }

    public boolean processTranslationMouseReleasedEvent(MouseEvent event)
    {
        return translationTechnique.processMouseReleasedEvent(event);
    }

    public boolean processTranslationMouseClickedEvent(MouseEvent event)
    {
        return translationTechnique.processMouseClickedEvent(event);
    }

    public boolean processTranslationMouseMovedEvent(MouseEvent event)
    {
        return translationTechnique.processMouseMovedEvent(event);
    }

    public boolean processTranslationMouseDraggedEvent(MouseEvent event)
    {
        return translationTechnique.processMouseDraggedEvent(event);
    }

    public boolean processRotateKeyPressedEvent(KeyEvent event)
    {
        return rotateGizmo.processKeyPressedEvent(event);
    }

    public boolean processScaleKeyPressedEvent(KeyEvent event)
    {
        return scaleGizmo.processKeyPressedEvent(event);
    }
}
