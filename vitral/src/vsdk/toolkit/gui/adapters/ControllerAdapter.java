/*
 * GravZeroAdapter.java
 *
 * Created on 1 de septiembre de 2005, 06:30 PM
 */

package vsdk.toolkit.gui.adapters;
/*
 * WaterFrame.java
 *
 * Created on 25 de agosto de 2005, 01:08 PM
 *
 */
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.gui.CameraController;

import vsdk.toolkit.common.Vector3D;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.JComponent;
import java.awt.Component;


public class ControllerAdapter implements MouseListener, MouseMotionListener, KeyListener, MouseWheelListener 
{
    private CameraController cameraController;

    public ControllerAdapter(CameraController cont) 
    {
        cameraController = cont;
    }
    
    public void register(Component c)    
    {
        c.addMouseListener(this);
        c.addMouseMotionListener(this);
        c.addKeyListener(this);
        c.addMouseWheelListener(this);
    }

    public void register(JComponent c)    
    {
        c.addMouseListener(this);
        c.addMouseMotionListener(this);
        c.addKeyListener(this);
        c.addMouseWheelListener(this);
    }

  public void mouseEntered(MouseEvent e) 
  {
  }

  public void mouseExited(MouseEvent e) 
  {
  }

  public void mousePressed(MouseEvent e) 
  {
      cameraController.processMousePressedEventAwt(e);
  }

  public void mouseReleased(MouseEvent e) 
  {
      cameraController.processMouseReleasedEventAwt(e);
  }

  public void mouseClicked(MouseEvent e) 
  {
      cameraController.processMouseClickedEventAwt(e);
  }

  public void mouseMoved(MouseEvent e) 
  {
      cameraController.processMouseMovedEventAwt(e);
  }

  public void mouseDragged(MouseEvent e) 
  {
      cameraController.processMouseDraggedEventAwt(e);
  }

  public void mouseWheelMoved(MouseWheelEvent e) 
  {
      cameraController.processMouseWheelEventAwt(e);
  }

  public void keyPressed(KeyEvent e) 
  {
      if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) 
      {
          System.exit(0);
      }

      cameraController.processKeyPressedEventAwt(e);
  }

  public void keyReleased(KeyEvent e) 
  {
      cameraController.processKeyReleasedEventAwt(e);
  }

  /**
  Do NOT call your controller from the `keyTyped` method, or the controller
  will be invoked twice for each key. Call it only from the `keyPressed` and
  `keyReleased` method
  */
  public void keyTyped(KeyEvent e) {
      ;
  }
}
