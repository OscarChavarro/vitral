package gui;

import model.SolidTextureModel;
import vsdk.toolkit.gui.KeyEvent;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.RendererConfigurationController;

public class SolidTextureKeyboardInteractionTechniques {
    private final SolidTextureModel model;
    private final CameraController cameraController;
    private final RendererConfigurationController qualityController;

    public SolidTextureKeyboardInteractionTechniques(
        SolidTextureModel model,
        CameraController cameraController,
        RendererConfigurationController qualityController)
    {
        this.model = model;
        this.cameraController = cameraController;
        this.qualityController = qualityController;
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
        if (qualityController.processKeyReleasedEvent(event)) {
            return true;
        }
        return false;
    }
}
