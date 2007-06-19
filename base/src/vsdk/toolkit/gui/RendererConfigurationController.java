package vsdk.toolkit.gui;

import java.awt.event.KeyEvent;
import vsdk.toolkit.common.RendererConfiguration;

public class RendererConfigurationController extends Controller {

  private RendererConfiguration qualitySelection;

  public RendererConfigurationController() {

  }

  public RendererConfigurationController(RendererConfiguration qualitySelection) {
    this.qualitySelection = qualitySelection;
  }

  public boolean processKeyPressedEventAwt(KeyEvent keyEvent) {
    boolean updated = false;
    char unicode_id;
    int keycode;
    int st;

    unicode_id = keyEvent.getKeyChar();
    keycode = keyEvent.getKeyCode();

    if (unicode_id == keyEvent.CHAR_UNDEFINED) {
      switch (keycode) {
        case KeyEvent.VK_F1:
          qualitySelection.changePoints();
          updated = true;
          break;
        case KeyEvent.VK_F2:
          qualitySelection.changeWires();
          updated = true;
          break;
        case KeyEvent.VK_F3:
          qualitySelection.changeSurfaces();
          updated = true;
          break;
        case KeyEvent.VK_F4:
          qualitySelection.changeBoundingVolume();
          updated = true;
          break;
        case KeyEvent.VK_F5:
          qualitySelection.changeNormals();
          updated = true;
          break;
        case KeyEvent.VK_F6:
          qualitySelection.changeTrianglesNormals();
          updated = true;
          break;
        case KeyEvent.VK_F7:
          st = qualitySelection.getShadingType();
          if ( st == qualitySelection.SHADING_TYPE_NOLIGHT ) {
              st = qualitySelection.SHADING_TYPE_FLAT;
            }
            else if ( st == qualitySelection.SHADING_TYPE_FLAT ) {
              st = qualitySelection.SHADING_TYPE_GOURAUD;
            }
            else if ( st == qualitySelection.SHADING_TYPE_GOURAUD ) {
              st = qualitySelection.SHADING_TYPE_PHONG;
            }
            else {
              st = qualitySelection.SHADING_TYPE_NOLIGHT;
            }
          ;
          qualitySelection.setShadingType(st);
          updated = true;
          break;
        case KeyEvent.VK_F8:
          qualitySelection.changeTexture();
          updated = true;
          break;
        case KeyEvent.VK_F9:
          qualitySelection.changeBumpMap();
          updated = true;
          break;
      }
    }
    return updated;
  }

  public boolean processKeyReleasedEventAwt(KeyEvent keyEvent) {
    return false;
  }

}
