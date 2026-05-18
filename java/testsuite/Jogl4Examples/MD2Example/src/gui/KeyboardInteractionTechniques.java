package gui;

import model.DebuggerModel;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.light.Light;

public class KeyboardInteractionTechniques {
    public static final double DELTA_MOVEMENT = 10.0;

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
        if ( keyEvent.keycode == vsdk.toolkit.gui.KeyEvent.KEY_3 ) {
            cycleSelectedObject(-1);
            return true;
        }
        if ( keyEvent.keycode == vsdk.toolkit.gui.KeyEvent.KEY_4 ) {
            cycleSelectedObject(1);
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
        if ( isMovementKey(keyEvent) && model.selectedObject >= 0 ) {
            moveSelectedLight(keyEvent);
            return true;
        }

        return false;
    }

    public boolean processKeyReleasedEvent(vsdk.toolkit.gui.KeyEvent keyEvent)
    {
        return false;
    }

    public static boolean isMovementKey(vsdk.toolkit.gui.KeyEvent keyEvent)
    {
        if ( keyEvent == null ) {
            return false;
        }
        return keyEvent.keycode == vsdk.toolkit.gui.KeyEvent.KEY_x ||
               keyEvent.keycode == vsdk.toolkit.gui.KeyEvent.KEY_X ||
               keyEvent.keycode == vsdk.toolkit.gui.KeyEvent.KEY_y ||
               keyEvent.keycode == vsdk.toolkit.gui.KeyEvent.KEY_Y ||
               keyEvent.keycode == vsdk.toolkit.gui.KeyEvent.KEY_z ||
               keyEvent.keycode == vsdk.toolkit.gui.KeyEvent.KEY_Z;
    }

    private void cycleSelectedObject(int direction)
    {
        int lightCount = model.lights != null ? model.lights.size() : 0;
        int totalObjects = lightCount + 1; // camera + lights
        if ( totalObjects <= 0 ) {
            model.selectedObject = -1;
            return;
        }

        int currentIndex = model.selectedObject + 1;
        int next = (currentIndex + direction) % totalObjects;
        if ( next < 0 ) {
            next += totalObjects;
        }
        model.selectedObject = next - 1;
    }

    private void moveSelectedLight(vsdk.toolkit.gui.KeyEvent keyEvent)
    {
        if ( model.lights == null || model.selectedObject < 0 ||
             model.selectedObject >= model.lights.size() ) {
            return;
        }
        Light selectedLight = model.lights.get(model.selectedObject);
        if ( selectedLight == null ) {
            return;
        }

        Vector3Dd p = selectedLight.getPosition();
        switch ( keyEvent.keycode ) {
          case vsdk.toolkit.gui.KeyEvent.KEY_x:
            p = p.withX(p.x() - DELTA_MOVEMENT);
            break;
          case vsdk.toolkit.gui.KeyEvent.KEY_X:
            p = p.withX(p.x() + DELTA_MOVEMENT);
            break;
          case vsdk.toolkit.gui.KeyEvent.KEY_y:
            p = p.withY(p.y() - DELTA_MOVEMENT);
            break;
          case vsdk.toolkit.gui.KeyEvent.KEY_Y:
            p = p.withY(p.y() + DELTA_MOVEMENT);
            break;
          case vsdk.toolkit.gui.KeyEvent.KEY_z:
            p = p.withZ(p.z() - DELTA_MOVEMENT);
            break;
          case vsdk.toolkit.gui.KeyEvent.KEY_Z:
            p = p.withZ(p.z() + DELTA_MOVEMENT);
            break;
          default:
            return;
        }
        selectedLight.setPosition(p);
    }
}
