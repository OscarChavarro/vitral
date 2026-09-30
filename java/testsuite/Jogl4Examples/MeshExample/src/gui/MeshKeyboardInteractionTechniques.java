package gui;

import model.MeshModel;
import vsdk.toolkit.gui.KeyEvent;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.RendererConfigurationController;
import vsdk.toolkit.gui.gizmo.RayGizmoInteractionTechniques;

public class MeshKeyboardInteractionTechniques {
    private final MeshModel model;
    private final CameraController cameraController;
    private final RendererConfigurationController qualityController;
    private final RayGizmoInteractionTechniques rayGizmoTechniques;

    public MeshKeyboardInteractionTechniques(
        MeshModel model,
        CameraController cameraController,
        RendererConfigurationController qualityController)
    {
        this.model = model;
        this.cameraController = cameraController;
        this.qualityController = qualityController;
        this.rayGizmoTechniques =
            new RayGizmoInteractionTechniques(model.getRayGizmo(),
                KeyEvent.KEY_r, KeyEvent.KEY_R);
    }

    public boolean processKeyPressedEvent(KeyEvent event)
    {
        if ( event == null ) {
            return false;
        }

        if ( event.keycode == KeyEvent.KEY_ESC ) {
            System.exit(0);
        }
        if ( event.keycode == KeyEvent.KEY_I ) {
            System.out.println(model.getQualitySelection());
            return true;
        }
        if ( rayGizmoTechniques.processKeyPressedEvent(event) ) {
            return true;
        }
        if ( cameraController.processKeyPressedEvent(event) ) {
            return true;
        }
        if ( qualityController.processKeyPressedEvent(event) ) {
            System.out.println(model.getQualitySelection());
            return true;
        }

        return false;
    }

    public boolean processKeyReleasedEvent(KeyEvent event)
    {
        if ( event == null ) {
            return false;
        }

        if (cameraController.processKeyReleasedEvent(event)) {
            return true;
        }
        if (qualityController.processKeyReleasedEvent(event)) {
            return true;
        }
        return false;
    }
}
