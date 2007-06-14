//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 16 2006 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.gui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyEvent;

import vsdk.toolkit.common.Matrix4x4;

public class TranslateGizmo {

  private Matrix4x4 T;

  public void setTransformationMatrix(Matrix4x4 T)
  {
      this.T = T;
  }

  public Matrix4x4 getTransformationMatrix()
  {
      return T;
  }

  public boolean processMouseEventAwt(MouseEvent mouseEvent)
  {
      return false;
  }

  public boolean processKeyPressedEventAwt(KeyEvent keyEvent)
  {
      char unicode_id;
      int keycode;
      double deltaMov = 0.1;
      boolean updateNeeded = false;

      unicode_id = keyEvent.getKeyChar();
      keycode = keyEvent.getKeyCode();

      if ( unicode_id != keyEvent.CHAR_UNDEFINED ) {
            switch ( unicode_id ) {
              // Position
              case 'x':
                T.M[0][3] -= deltaMov;
                updateNeeded = true;
                break;
              case 'X':
                T.M[0][3] += deltaMov;
                updateNeeded = true;
                break;
              case 'y':
                T.M[1][3] -= deltaMov;
                updateNeeded = true;
                break;
              case 'Y':
                T.M[1][3] += deltaMov;
                updateNeeded = true;
                break;
              case 'z':
                T.M[2][3] -= deltaMov;
                updateNeeded = true;
                break;
              case 'Z':
                T.M[2][3] += deltaMov;
                updateNeeded = true;
                break; 
        }
      }

      return updateNeeded;
  }

  public boolean processKeyReleasedEventAwt(KeyEvent mouseEvent)
  {
      return false;
  }

  public boolean processMousePressedEventAwt(MouseEvent e)
  {
      return false;
  }

  public boolean processMouseReleasedEventAwt(MouseEvent e)
  {
      return false;
  }

  public boolean processMouseClickedEventAwt(MouseEvent e)
  {
      return false;
  }

  public boolean processMouseMovedEventAwt(MouseEvent e)
  {
      return false;
  }

  public boolean processMouseDraggedEventAwt(MouseEvent e)
  {
      return false;
  }

  public boolean processMouseWheelEventAwt(MouseWheelEvent e)
  {
      return false;
  }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
