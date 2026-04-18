import vsdk.toolkit.gui.KeyEvent;

public class PolygonClippingKeyboardInteractionTechniques
{
    public interface Actions
    {
        void requestExit();
        void rebuildScene();
        void toggleFullscreen();
        void requestSnapshot();
    }

    public boolean processPressed(PolygonClippingDebuggerModel model,
        KeyEvent event, Actions actions)
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
            repaint = true;
        }

        switch ( event.keycode ) {
          case KeyEvent.KEY_1:
            model.stepTest(-1);
            actions.rebuildScene();
            handled = true;
            break;
          case KeyEvent.KEY_2:
            model.stepTest(1);
            actions.rebuildScene();
            handled = true;
            break;
          case KeyEvent.KEY_SPACE:
            model.showReferenceFrame = !model.showReferenceFrame;
            handled = true;
            break;
          case KeyEvent.KEY_c:
          case KeyEvent.KEY_C:
            model.showClipPolygon = !model.showClipPolygon;
            handled = true;
            break;
          case KeyEvent.KEY_s:
          case KeyEvent.KEY_S:
            model.showSubjectPolygon = !model.showSubjectPolygon;
            handled = true;
            break;
          case KeyEvent.KEY_i:
          case KeyEvent.KEY_I:
            model.showInnerPolygon = !model.showInnerPolygon;
            handled = true;
            break;
          case KeyEvent.KEY_o:
          case KeyEvent.KEY_O:
            model.showOuterPolygon = !model.showOuterPolygon;
            handled = true;
            break;
          case KeyEvent.KEY_p:
          case KeyEvent.KEY_P:
            model.showIntersections = !model.showIntersections;
            handled = true;
            break;
          case KeyEvent.KEY_t:
          case KeyEvent.KEY_T:
            model.showFilledPolygons = !model.showFilledPolygons;
            handled = true;
            break;
          case KeyEvent.KEY_h:
          case KeyEvent.KEY_H:
            actions.requestSnapshot();
            handled = true;
            break;
          case KeyEvent.KEY_f:
          case KeyEvent.KEY_F:
            actions.toggleFullscreen();
            handled = true;
            break;
        }

        return repaint || handled;
    }
}
