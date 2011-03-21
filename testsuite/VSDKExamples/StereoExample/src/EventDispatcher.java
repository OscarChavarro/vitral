//import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseMotionListener;
import javax.media.opengl.awt.GLJPanel;

import vsdk.toolkit.gui.KeyEvent;
import vsdk.toolkit.gui.AwtSystem;

public class EventDispatcher implements 
    KeyListener, MouseListener, MouseMotionListener, MouseWheelListener
{
    private JoglDrawingArea drawingArea;
    private GLJPanel canvas;
    private StereoApplicationExample parent;

    public EventDispatcher(StereoApplicationExample parent)
    {
        this.drawingArea = null;
        this.canvas = null;
        this.parent = parent;
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

        canvas.repaint();
    }

    public void keyReleased(java.awt.event.KeyEvent e) 
    {
        ;
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
        ;
    }

    public void mouseReleased(MouseEvent e)
    {
        ;
    }

    public void mouseClicked(MouseEvent e)
    {
        ;
    }

    public void mouseMoved(MouseEvent e)
    {
        ;
    }

    public void mouseDragged(MouseEvent e)
    {
        ;
    }

    public void mouseWheelMoved(MouseWheelEvent e)
    {
        ;
    }

}
