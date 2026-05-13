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
        boolean handledLetterShortcut = false;

        if ( event.keycode == KeyEvent.KEY_ESC ) {
            actions.requestExit();
            return false;
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
            model.setShowReferenceFrame(!model.isShowReferenceFrame());
            handled = true;
            break;
          case KeyEvent.KEY_c, KeyEvent.KEY_C:
            model.setShowClipPolygon(!model.isShowClipPolygon());
            handled = true;
            handledLetterShortcut = true;
            break;
          case KeyEvent.KEY_s, KeyEvent.KEY_S:
            model.setShowSubjectPolygon(!model.isShowSubjectPolygon());
            handled = true;
            handledLetterShortcut = true;
            break;
          case KeyEvent.KEY_i, KeyEvent.KEY_I:
            model.setShowInnerPolygon(!model.isShowInnerPolygon());
            handled = true;
            handledLetterShortcut = true;
            break;
          case KeyEvent.KEY_o, KeyEvent.KEY_O:
            model.setShowOuterPolygon(!model.isShowOuterPolygon());
            handled = true;
            handledLetterShortcut = true;
            break;
          case KeyEvent.KEY_p, KeyEvent.KEY_P:
            model.setShowIntersections(!model.isShowIntersections());
            handled = true;
            handledLetterShortcut = true;
            break;
          case KeyEvent.KEY_t, KeyEvent.KEY_T:
            model.setShowFilledPolygons(!model.isShowFilledPolygons());
            handled = true;
            handledLetterShortcut = true;
            break;
          case KeyEvent.KEY_h, KeyEvent.KEY_H:
            actions.requestSnapshot();
            handled = true;
            handledLetterShortcut = true;
            break;
          case KeyEvent.KEY_f, KeyEvent.KEY_F:
            actions.toggleFullscreen();
            handled = true;
            handledLetterShortcut = true;
            break;
          default:
            break;
        }

        if ( !handledLetterShortcut
             && model.getCameraController().processKeyPressedEvent(event) ) {
            repaint = true;
        }
        if ( model.getQualityController().processKeyPressedEvent(event) ) {
            repaint = true;
        }

        return repaint || handled;
    }
}
