package vitral.framework.gui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyEvent;

public interface CameraController {
  public boolean processMouseEventAwt(MouseEvent mouseEvent);

  public boolean processKeyPressedEventAwt(KeyEvent mouseEvent);

  public boolean processKeyReleasedEventAwt(KeyEvent mouseEvent);

  public boolean processMousePressedEventAwt(MouseEvent e);

  public boolean processMouseReleasedEventAwt(MouseEvent e);

  public boolean processMouseClickedEventAwt(MouseEvent e);

  public boolean processMouseMovedEventAwt(MouseEvent e);

  public boolean processMouseDraggedEventAwt(MouseEvent e);

  public boolean processMouseWheelEventAwt(MouseWheelEvent e);
}
