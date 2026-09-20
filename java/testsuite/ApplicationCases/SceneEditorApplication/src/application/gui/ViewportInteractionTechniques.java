package application.gui;

import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.KeyEvent;
import vsdk.toolkit.gui.MouseEvent;
import vsdk.toolkit.gui.RendererConfigurationController;
import vsdk.toolkit.gui.gizmo.RotateGizmo;
import vsdk.toolkit.gui.gizmo.ScaleGizmo;
import vsdk.toolkit.gui.gizmo.TranslateGizmo;

public class ViewportInteractionTechniques
{
    private final CameraController cameraController;
    private final RendererConfigurationController qualityController;
    private final TranslateGizmo translationGizmo;
    private final RotateGizmo rotateGizmo;
    private final ScaleGizmo scaleGizmo;

    public ViewportInteractionTechniques(Camera camera,
                                         RendererConfiguration rendererConfiguration)
    {
        cameraController = new CameraControllerAquynza(camera);
        qualityController = new RendererConfigurationController(rendererConfiguration);
        translationGizmo = new TranslateGizmo(camera);
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

    public boolean processTranslationKeyPressedEvent(KeyEvent event)
    {
        return translationGizmo.processKeyPressedEvent(event);
    }

    public boolean processTranslationMousePressedEvent(MouseEvent event)
    {
        return translationGizmo.processMousePressedEvent(event);
    }

    public boolean processTranslationMouseReleasedEvent(MouseEvent event)
    {
        return translationGizmo.processMouseReleasedEvent(event);
    }

    public boolean processTranslationMouseClickedEvent(MouseEvent event)
    {
        return translationGizmo.processMouseClickedEvent(event);
    }

    public boolean processTranslationMouseMovedEvent(MouseEvent event)
    {
        return translationGizmo.processMouseMovedEvent(event);
    }

    public boolean processTranslationMouseDraggedEvent(MouseEvent event)
    {
        return translationGizmo.processMouseDraggedEvent(event);
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
