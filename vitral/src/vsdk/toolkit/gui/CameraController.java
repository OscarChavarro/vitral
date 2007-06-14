//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 8 2005 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.gui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyEvent;

import vsdk.toolkit.environment.Camera;

public interface CameraController {
  public boolean processMouseEventAwt(MouseEvent mouseEvent);

  public boolean processKeyPressedEventAwt(KeyEvent keyEvent);

  public boolean processKeyReleasedEventAwt(KeyEvent keyEvent);

  public boolean processMousePressedEventAwt(MouseEvent e);

  public boolean processMouseReleasedEventAwt(MouseEvent e);

  public boolean processMouseClickedEventAwt(MouseEvent e);

  public boolean processMouseMovedEventAwt(MouseEvent e);

  public boolean processMouseDraggedEventAwt(MouseEvent e);

  public boolean processMouseWheelEventAwt(MouseWheelEvent e);

  public Camera getCamera();

  public void setCamera(Camera camera);
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
