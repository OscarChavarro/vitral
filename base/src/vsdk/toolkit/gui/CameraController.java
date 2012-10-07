//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 8 2005 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.gui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
//import java.awt.event.KeyEvent;

import vsdk.toolkit.environment.Camera;

public abstract class CameraController extends Controller {
    @Deprecated
    public abstract boolean processMouseEventAwt(MouseEvent mouseEvent);
    
    public abstract boolean processKeyPressedEvent(KeyEvent keyEvent);
    
    @Deprecated
    public abstract boolean processKeyPressedEventAwt(java.awt.event.KeyEvent keyEvent);
    
    @Deprecated
    public abstract boolean processKeyReleasedEventAwt(java.awt.event.KeyEvent keyEvent);
    
    @Deprecated
    public abstract boolean processMousePressedEventAwt(MouseEvent e);
    
    @Deprecated
    public abstract boolean processMouseReleasedEventAwt(MouseEvent e);
    
    @Deprecated
    public abstract boolean processMouseClickedEventAwt(MouseEvent e);
    
    @Deprecated
    public abstract boolean processMouseMovedEventAwt(MouseEvent e);
    
    @Deprecated
    public abstract boolean processMouseDraggedEventAwt(MouseEvent e);
    
    @Deprecated
    public abstract boolean processMouseWheelEventAwt(MouseWheelEvent e);
    
    public abstract Camera getCamera();
    public abstract void setCamera(Camera camera);
    public abstract void setDeltaMovement(double factor);
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
