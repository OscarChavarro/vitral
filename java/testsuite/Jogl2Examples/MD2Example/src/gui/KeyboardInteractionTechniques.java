package gui;

import model.DebuggerModel;

public class KeyboardInteractionTechniques {
    private final DebuggerModel model;

    public KeyboardInteractionTechniques(DebuggerModel model)
    {
        this.model = model;
    }

    public boolean processKeyPressedEvent(vsdk.toolkit.gui.KeyEvent keyEvent)
    {
        if ( keyEvent == null || model == null || model.md2Mesh == null ) {
            return false;
        }

        short[] animStartEnd = new short[2];
        if ( keyEvent.keycode == vsdk.toolkit.gui.KeyEvent.KEY_1 ) {
            model.md2Mesh.returnStartEndAnim(model.md2Mesh.getCurrentAnimationInd(), animStartEnd);
            if ( model.md2Mesh.getCurrentAnimationInd() == animStartEnd[0] ) {
                model.md2Mesh.setCurrentAnimationInd(model.md2Mesh.getMaxAnimationInd());
            }
            else {
                model.md2Mesh.setCurrentAnimationInd((short)(model.md2Mesh.getCurrentAnimationInd() - 1));
            }
            return true;
        }
        if ( keyEvent.keycode == vsdk.toolkit.gui.KeyEvent.KEY_2 ) {
            model.md2Mesh.setCurrentAnimationInd((short)(model.md2Mesh.getCurrentAnimationInd() + 1));
            return true;
        }
        if ( keyEvent.keycode == vsdk.toolkit.gui.KeyEvent.KEY_ESC ) {
            System.exit(0);
            return true;
        }
        if ( keyEvent.keycode == vsdk.toolkit.gui.KeyEvent.KEY_I ) {
            System.out.println(model.qualitySelection);
            return true;
        }

        return false;
    }

    public boolean processKeyReleasedEvent(vsdk.toolkit.gui.KeyEvent keyEvent)
    {
        return false;
    }
}
