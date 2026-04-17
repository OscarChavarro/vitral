import vsdk.toolkit.gui.KeyEvent;
import vsdk.toolkit.environment.geometry.PolyhedralBoundedSolidValidationEngine;

public class DebuggerKeyboardInteractionTechniques
{
    public interface Actions
    {
        void requestExit();
        void rebuildSolid();
        void toggleFullscreen();
    }

    public boolean processPressed(DebuggerModel model, KeyEvent event, Actions actions)
    {
        boolean repaint = false;
        boolean handled = false;

        if ( event.keycode == KeyEvent.KEY_ESC ) {
            actions.requestExit();
            return false;
        }

        if ( model.cameraController.processKeyPressedEvent(event) ) {
            repaint = true;
        }
        if ( model.qualityController.processKeyPressedEvent(event) ) {
            System.out.println(model.quality);
            repaint = true;
        }

        switch ( event.keycode ) {
          case KeyEvent.KEY_0:
            model.debugEdges = !model.debugEdges;
            handled = true;
            break;
          case KeyEvent.KEY_SPACE:
            model.showCoordinateSystem = !model.showCoordinateSystem;
            handled = true;
            break;
          case KeyEvent.KEY_1:
            model.faceIndex--;
            handled = true;
            break;
          case KeyEvent.KEY_2:
            model.faceIndex++;
            handled = true;
            break;
          case KeyEvent.KEY_8:
            model.edgeIndex--;
            handled = true;
            break;
          case KeyEvent.KEY_9:
            model.edgeIndex++;
            handled = true;
            break;
          case KeyEvent.KEY_I:
            System.out.println(model.solid);
            if ( PolyhedralBoundedSolidValidationEngine.validateIntermediate(model.solid) ) {
                System.out.println("SOLID MODEL IS VALID!");
            }
            else {
                System.out.println("SOLID MODEL IS INVALID!");
            }
            handled = true;
            break;

          case KeyEvent.KEY_3:
            model.solidModelName = model.solidModelName.previousClamped();
            actions.rebuildSolid();
            handled = true;
            break;

          case KeyEvent.KEY_4:
            model.solidModelName = model.solidModelName.nextCircular();
            actions.rebuildSolid();
            handled = true;
            break;

          case KeyEvent.KEY_5:
            model.csgOperation++;
            if ( model.csgOperation > 3 ) {
                model.csgOperation = 0;
            }
            actions.rebuildSolid();
            handled = true;
            break;

          case KeyEvent.KEY_6:
            model.csgSample++;
            if ( model.csgSample > 7 ) {
                model.csgSample = 0;
            }
            actions.rebuildSolid();
            handled = true;
            break;

          case KeyEvent.KEY_v:
            model.debugVertices = !model.debugVertices;
            handled = true;
            break;

          case KeyEvent.KEY_d:
            model.debugCsg = !model.debugCsg;
            actions.rebuildSolid();
            handled = true;
            break;

          case KeyEvent.KEY_f:
          case KeyEvent.KEY_F:
            actions.toggleFullscreen();
            handled = true;
            break;
        }

        if ( model.faceIndex < -2 ) {
            model.faceIndex = -2;
        }
        if ( model.edgeIndex < -3 ) {
            model.edgeIndex = -3;
        }

        return repaint || handled;
    }
}
