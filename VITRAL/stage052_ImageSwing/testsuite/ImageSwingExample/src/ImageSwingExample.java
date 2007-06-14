// VITRAL recomendation: Use explicit class imports (not .*) in hello world type programs
// so the user/programmer can be exposed to all the complexity involved. This will help him
// to dominate the involved libraries.

import java.io.File;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseMotionListener;

import vitral.toolkits.media.RGBAImage;
import vitral.toolkits.media.RGBAImageBuilder;
import vitral.toolkits.visual.awt.AwtRGBAImageRenderer;

public class ImageSwingExample extends JFrame implements 
    MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private ButtonsPanel controls;
    private JMenuBar menubar;
    private MyPanel canvas;

    public ImageSwingExample() {
        super("VITRAL concept test - Image in swing test");

        controls = new ButtonsPanel();
        menubar = this.buildMenu();
        canvas = new MyPanel();

        this.add(canvas, BorderLayout.CENTER);
        this.add(controls, BorderLayout.SOUTH);
        this.setJMenuBar(menubar);
    }

    public Dimension getPreferredSize() {
        return new Dimension (640, 480);
    }
    
    public static void main (String[] args) {
        JFrame f = new ImageSwingExample();
        f.pack();
        f.setVisible(true);
    }
    
  public void mouseEntered(MouseEvent e) {
      canvas.requestFocusInWindow();
  }

  public void mouseExited(MouseEvent e) {
    //System.out.println("Mouse exited");
  }

  public void mousePressed(MouseEvent e) {
      canvas.repaint();
  }

  public void mouseReleased(MouseEvent e) {
      canvas.repaint();
  }

  public void mouseClicked(MouseEvent e) {
      canvas.repaint();
  }

  public void mouseMoved(MouseEvent e) {
      canvas.repaint();
  }

  public void mouseDragged(MouseEvent e) {
      canvas.repaint();
  }

  /**
  WARNING: It is not working... check pending
  */
  public void mouseWheelMoved(MouseWheelEvent e) {
      System.out.println(".");
      canvas.repaint();
  }

  public void keyPressed(KeyEvent e) {
      if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) {
          System.exit(0);
      }
  }

  public void keyReleased(KeyEvent e) {
      canvas.repaint();
  }

  /**
  Do NOT call your controller from the `keyTyped` method, or the controller
  will be invoked twice for each key. Call it only from the `keyPressed` and
  `keyReleased` method
  */
  public void keyTyped(KeyEvent e) {
      ;
  }

    public JMenuBar buildMenu()
    {
        JMenuBar menubar;
        JMenu popup;
        JMenuItem option;

        menubar = new JMenuBar();
        popup = new JMenu("File");
        menubar.add(popup);

        option = popup.add(new JMenuItem("Exit"));
        option.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }});

        popup.getPopupMenu().setLightWeightPopupEnabled(false);

        return menubar;
    }

}

class ButtonsPanel extends JPanel implements ActionListener
{
    public ButtonsPanel()
    {
        JButton b = null;

        b = new JButton("Exit");
        b.addActionListener(this);
        add(b);
    }

    public void actionPerformed(ActionEvent ev) {
        String label = ev.getActionCommand();

        if ( label == "Exit" ) {
            System.exit(1);
        }
    }
}

//===========================================================================

class MyPanel extends JPanel
{
    private RGBAImage img;

    public MyPanel()
    {
        try {
            img = RGBAImageBuilder.buildImage(new File("./etc/entorno1.jpg"));
        }
        catch (Exception e) {
            System.err.println(e);
            System.exit(0);
        }
    }

    public void paint(Graphics dc)
    {
        AwtRGBAImageRenderer.draw(dc, img, 100, 50);
    }
}

//===========================================================================
