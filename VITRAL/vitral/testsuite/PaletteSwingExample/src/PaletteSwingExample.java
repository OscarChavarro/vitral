// VITRAL recomendation: Use explicit class imports (not .*) in hello world type programs
// so the user/programmer can be exposed to all the complexity involved. This will help him
// to dominate the involved libraries.

import java.io.File;
import java.io.FileReader;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JFileChooser;
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

import vsdk.toolkit.media.RGBColorPalette;
import vsdk.toolkit.io.image.RGBColorPaletteBuilder;
import vsdk.toolkit.render.awt.AwtRGBColorPaletteRenderer;

abstract class SuffixAwareFilter
    extends javax.swing.filechooser.FileFilter {

  public String getSuffix(File f) {
    String s = f.getPath(), suffix = null;
    int i = s.lastIndexOf('.');

    if (i > 0 && i < s.length() - 1) {
      suffix = s.substring(i + 1).toLowerCase();
    }

    return suffix;
  }

  public boolean accept(File f) {
    return f.isDirectory();
  }

}

class MyFilter
    extends SuffixAwareFilter {
  private String suffix;
  private String description;

  public MyFilter(String suffix, String description) {
    this.suffix = suffix;
    this.description = description;
  }

  public boolean accept(File f) {
    boolean accept = super.accept(f);
    if (!accept) {
      String _suffix = getSuffix(f);
      if (suffix != null) {
        accept = suffix.equals(_suffix);
      }
    }
    return accept;
  }

  public String getDescription() {
    return description + " (*." + suffix + ")";
  }
}


public class PaletteSwingExample extends JFrame implements 
    MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private ButtonsPanel controls;
    private JMenuBar menubar;
    private MyPanel canvas;

    public PaletteSwingExample() {
        super("VITRAL concept test - Color Palette in swing test");

        controls = new ButtonsPanel();
        canvas = new MyPanel();
        menubar = this.buildMenu();

        this.add(canvas, BorderLayout.CENTER);
        this.add(controls, BorderLayout.SOUTH);
        this.setJMenuBar(menubar);
    }

    public Dimension getPreferredSize() {
        return new Dimension (640, 480);
    }
    
    public static void main (String[] args) {
        JFrame f = new PaletteSwingExample();
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

        option = popup.add(new JMenuItem("Reset to grayscale256"));
        option.addActionListener(canvas);

        option = popup.add(new JMenuItem("Open"));
        option.addActionListener(canvas);

        option = popup.add(new JMenuItem("Exit"));
        option.addActionListener(canvas);

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

class MyPanel extends JPanel implements ActionListener
{
    private RGBColorPalette palette;

    public MyPanel()
    {
        initPalette(1, null);
    }

    public void initPalette(int type, String filename)
    {
        palette = new RGBColorPalette();
        switch ( type ) {
          case 1:
        palette.init(256);
            break;
          case 2:
            try {
                palette = RGBColorPaletteBuilder.importGimpPalette(new FileReader(filename));
            }
            catch (Exception e) {
                System.err.println(e);
                System.exit(0);
            }
            break;
        }
    }

    public void paint(Graphics dc)
    {
        super.paint(dc);
        AwtRGBColorPaletteRenderer.drawFlatVertical(dc, palette, 100, 50, 10, 256);
        AwtRGBColorPaletteRenderer.drawShadedVertical(dc, palette, 120, 50, 10, 256);
    }

    public void actionPerformed(ActionEvent e) {
        String label = e.getActionCommand();

        if ( label == "Open" ) {
            JFileChooser jfc = null;
            jfc = new JFileChooser( (new File("")).getAbsolutePath() + "/../../etc/palettes");
            jfc.removeChoosableFileFilter(jfc.getFileFilter());
            jfc.addChoosableFileFilter(new MyFilter("gpl", "gpl Gimp Palettes"));
            int opc = jfc.showOpenDialog(new JPanel());
            if (opc == JFileChooser.APPROVE_OPTION) {
                try {
                    File file = jfc.getSelectedFile();
                    initPalette(2, file.getAbsolutePath());
                    repaint();
                }
                catch (Exception ex) {
                    System.out.println("Failed to read file");
                    return;
                }
            }

        }
        else if ( label == "Reset to grayscale256" ) {
            initPalette(1, null);
    }
        else if ( label == "Exit" ) {
            System.exit(0);
    }
    }
}

//===========================================================================
