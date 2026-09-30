package gui;

import model.SolidTextureModel;
import vsdk.toolkit.gui.KeyEvent;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.RendererConfigurationController;
import vsdk.toolkit.gui.gizmo.RayGizmoInteractionTechniques;

public class SolidTextureKeyboardInteractionTechniques {
    private final SolidTextureModel model;
    private final CameraController cameraController;
    private final RendererConfigurationController qualityController;
    private final RayGizmoInteractionTechniques rayGizmoTechniques;

    public SolidTextureKeyboardInteractionTechniques(
        SolidTextureModel model,
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
        if ( event.keycode == KeyEvent.KEY_1 ) {
            model.rotateOperationMode();
            return true;
        }
        if ( event.keycode == KeyEvent.KEY_2 ) {
            model.decreaseSolidTextureSize();
            return true;
        }
        if ( event.keycode == KeyEvent.KEY_3 ) {
            model.increaseSolidTextureSize();
            return true;
        }
        if ( event.keycode == KeyEvent.KEY_4 ) {
            model.selectPreviousSolidTexture();
            return true;
        }
        if ( event.keycode == KeyEvent.KEY_5 ) {
            model.selectNextSolidTexture();
            return true;
        }
        if ( rayGizmoTechniques.processKeyPressedEvent(event) ) {
            return true;
        }
        if ( event.keycode == KeyEvent.KEY_a || event.keycode == KeyEvent.KEY_A ) {
            model.toggleAnimationEnabled();
            return true;
        }
        if ( event.keycode == KeyEvent.KEY_h || event.keycode == KeyEvent.KEY_H ) {
            model.toggleHudVisible();
            return true;
        }
        if ( event.keycode == KeyEvent.KEY_I ) {
            System.out.println(model.getQualitySelection());
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
        return qualityController.processKeyReleasedEvent(event);
    }
}
