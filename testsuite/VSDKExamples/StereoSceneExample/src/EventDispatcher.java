//import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseMotionListener;
import javax.media.opengl.awt.GLJPanel;
import com.jogamp.opengl.util.Animator;

import vsdk.toolkit.gui.CameraController;            // Controller elements
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.KeyEvent;
import vsdk.toolkit.gui.AwtSystem;

public class EventDispatcher implements 
    KeyListener, MouseListener, MouseMotionListener, MouseWheelListener
{
    private JoglDrawingArea drawingArea;
    private GLJPanel canvas;
    private StereoSceneExample parent;
    private CameraController cameraController;

    public EventDispatcher(StereoSceneExample parent)
    {
        this.drawingArea = null;
        this.canvas = null;
        this.parent = parent;
        cameraController = new CameraControllerAquynza(parent.scene.camera);
    }

    public void setDrawingArea(JoglDrawingArea drawingArea)
    {
        this.drawingArea = drawingArea;
    }

    public void setCanvas(GLJPanel canvas)
    {
        this.canvas = canvas;
    }

    public void keyPressed(java.awt.event.KeyEvent e)
    {
        KeyEvent vsdke;
        vsdke = AwtSystem.awt2vsdkEvent(e);

        switch ( vsdke.keycode ) {
          case KeyEvent.KEY_0:
            if ( parent.animator == null ) {
                parent.animator = new Animator(parent.canvas);
                parent.animator.start();
                parent.isRotating = true;
            }
            else if ( !parent.animator.isStarted() ) {
                parent.animator.start();
                parent.isRotating = true;
	    }
	    else if ( parent.animator.isAnimating() ) {
                parent.animator.pause();
                parent.isRotating = false;
	    }
	    else {
                parent.animator.resume();
                parent.isRotating = true;
	    }
            break;
          case KeyEvent.KEY_ESC:
            System.exit(0);
            break;
          case KeyEvent.KEY_1:
	    parent.setStereoStrategyId(1);
	    parent.destroyGUI();
	    parent.createGUI();
            break;
          case KeyEvent.KEY_2:
	    parent.setStereoStrategyId(2);
	    parent.destroyGUI();
	    parent.createGUI();
            break;
          case KeyEvent.KEY_3:
	    parent.setStereoStrategyId(3);
	    parent.destroyGUI();
	    parent.createGUI();
            break;
          case KeyEvent.KEY_4:
	    parent.setStereoStrategyId(4);
	    parent.destroyGUI();
	    parent.createGUI();
            break;
          case KeyEvent.KEY_5:
	    parent.setStereoStrategyId(5);
	    parent.destroyGUI();
	    parent.createGUI();
            break;
          case KeyEvent.KEY_6:
	    parent.setStereoStrategyId(6);
	    parent.destroyGUI();
	    parent.createGUI();
            break;
          case KeyEvent.KEY_7:
	    parent.setStereoStrategyId(7);
	    parent.destroyGUI();
	    parent.createGUI();
            break;
          case KeyEvent.KEY_8:
	    parent.setStereoStrategyId(8);
	    parent.destroyGUI();
	    parent.createGUI();
            break;
          case KeyEvent.KEY_9:
	    parent.setStereoStrategyId(9);
	    parent.destroyGUI();
	    parent.createGUI();
            break;
          case KeyEvent.KEY_f:
	    parent.destroyGUI();
            parent.swithFullScreenMode();
	    parent.createGUI();
            break;
          case KeyEvent.KEY_d:
	    parent.scene.eyeDistance -= 0.01;
            if ( parent.scene.eyeDistance < 0.0 ) {
                parent.scene.eyeDistance = 0.0;
	    }
	    System.out.printf("Eye distance: %.3f\n", parent.scene.eyeDistance);
            break;
          case KeyEvent.KEY_D:
	    parent.scene.eyeDistance += 0.01;
	    System.out.printf("Eye distance: %.3f\n", parent.scene.eyeDistance);
            break;

          case KeyEvent.KEY_t:
	    parent.scene.eyeTorsionAngle -= 0.1;
            if ( parent.scene.eyeTorsionAngle < 0.0 ) {
                parent.scene.eyeTorsionAngle = 0.0;
	    }
	    System.out.printf("Eye angle: %.2f\n", parent.scene.eyeTorsionAngle);
            break;
          case KeyEvent.KEY_T:
	    parent.scene.eyeTorsionAngle += 0.1;
	    System.out.printf("Eye angle: %.2f\n", parent.scene.eyeTorsionAngle);
            break;

          case KeyEvent.KEY_c:
            if ( parent.stereoStrategy != null ) {
                if ( parent.stereoStrategy.getSwapChannels() ) {
                    parent.stereoStrategy.setSwapChannels(false);
                }
                else {
                    parent.stereoStrategy.setSwapChannels(true);
                }
            }
            break;
        }

        if ( cameraController.processKeyPressedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
	    ;
        }

        canvas.repaint();
    }

    public void keyReleased(java.awt.event.KeyEvent e) 
    {
        if ( cameraController.processKeyReleasedEventAwt(e) ) {
            canvas.repaint();
        }
    }

    /**
    Do NOT call your controller from the `keyTyped` method, or the controller
    will be invoked twice for each key. Call it only from the `keyPressed` and
    `keyReleased` method
    */
    public void keyTyped(java.awt.event.KeyEvent e)
    {
        ;
    }

    public void mouseEntered(MouseEvent e)
    {
        canvas.requestFocusInWindow();
    }

    public void mouseExited(MouseEvent e) 
    {
        ;
    }

    public void mousePressed(MouseEvent e)
    {
        if ( cameraController.processMousePressedEventAwt(e) ) {
            canvas.repaint();
        }
    }

    public void mouseReleased(MouseEvent e)
    {
        if ( cameraController.processMouseReleasedEventAwt(e) ) {
            canvas.repaint();
        }
    }

    public void mouseClicked(MouseEvent e)
    {
        if ( cameraController.processMouseClickedEventAwt(e) ) {
            canvas.repaint();
        }
    }

    public void mouseMoved(MouseEvent e)
    {
        if ( cameraController.processMouseMovedEventAwt(e) ) {
            canvas.repaint();
        }
    }

    public void mouseDragged(MouseEvent e)
    {
        if ( cameraController.processMouseDraggedEventAwt(e) ) {
            canvas.repaint();
        }
    }

    public void mouseWheelMoved(MouseWheelEvent e)
    {
        if ( cameraController.processMouseWheelEventAwt(e) ) {
            canvas.repaint();
        }
    }

}
