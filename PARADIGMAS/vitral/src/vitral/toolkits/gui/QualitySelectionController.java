package vitral.toolkits.gui;

import java.awt.event.KeyEvent;
import vitral.toolkits.common.QualitySelection;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class QualitySelectionController {

  private QualitySelection qualitySelection;

  public QualitySelectionController() {

  }

  public QualitySelectionController(QualitySelection qualitySelection) {
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
          if ( st == qualitySelection.SHADING_TYPE_FLAT ) {
              st = qualitySelection.SHADING_TYPE_GOURAUD;
            }
          else if ( st == qualitySelection.SHADING_TYPE_GOURAUD ) {
              st = qualitySelection.SHADING_TYPE_PHONG;
            }
            else {
              st = qualitySelection.SHADING_TYPE_FLAT;
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
