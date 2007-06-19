//===========================================================================
//= This example serves as a testbed for basic RGB image manipulation,      =
//= dependent of Java's Swing, but not dependent on JOGL/OpenGL.            =
//=-------------------------------------------------------------------------=
//= Proposed exercises based on this program:                               =
//=   - Change default RGBImage management to generate an equivalent code   =
//=     that manages other types of Image, such as RGBAImage or             =
//=     IndexedColorImage                                                   =
//=   - Study basic image persistence/loading, producing an equivalent code =
//=     that does not use any vsdk.toolkit.io.image.* persistence class.    =
//=   - Use the provided `performImageOperation1` method to study basic     =
//=     image processing operations like thresholding, color inversion      =
//=     (negative image generation), 90 degrees rotations and others.       =
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 8 2005 - Oscar Chavarro: Original base version                 =
//= - August 2 2006 - Oscar Chavarro: comments added                        =
//= - December 18 2006 - Oscar Chavarro: minor changes to ease Image format =
//=   changes.                                                              =
//===========================================================================

// Basic JDK classes
import java.io.File;

// AWT classes
import java.awt.BorderLayout;
import java.awt.Dimension;
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

// Swing classes
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

// VSDK classes
import vsdk.toolkit.common.VSDK;                      // Utilities
import vsdk.toolkit.media.Image;                      // Model elements
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.io.image.ImagePersistence;        // Persistence elements
import vsdk.toolkit.render.awt.AwtRGBImageRenderer;   // View elements

// Note that this code defines 2 internal private classes:
// MyButtonsPanel & MyCanvasPanel

public class ImageSwingExample 
    extends JFrame
    implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener 
{
    private MyButtonsPanel controlsPanelWidget;
    private MyCanvasPanel canvasWidget;
    private JMenuBar menubarWidget;

    public ImageSwingExample() {
        super("VSDK concept test - An RGBImage in swing test");

        canvasWidget = new MyCanvasPanel();
        controlsPanelWidget = new MyButtonsPanel(canvasWidget);
        menubarWidget = this.buildMenu();

        this.add(canvasWidget, BorderLayout.CENTER);
        this.add(controlsPanelWidget, BorderLayout.SOUTH);
        this.setJMenuBar(menubarWidget);
    }

    public Dimension getPreferredSize() {
        return new Dimension (640, 480);
    }
    
    public static void main (String[] args) {
        JFrame mainWindowWidget = new ImageSwingExample();
        mainWindowWidget.pack();
        mainWindowWidget.setVisible(true);
    }
    
    public void mouseEntered(MouseEvent e) {
        canvasWidget.requestFocusInWindow();
    }

    public void mouseExited(MouseEvent e) {
        ;
    }

    public void mousePressed(MouseEvent e) {
        canvasWidget.repaint();
    }

    public void mouseReleased(MouseEvent e) {
        canvasWidget.repaint();
    }

    public void mouseClicked(MouseEvent e) {
        canvasWidget.repaint();
    }

    public void mouseMoved(MouseEvent e) {
        canvasWidget.repaint();
    }

    public void mouseDragged(MouseEvent e) {
        canvasWidget.repaint();
    }

    /**
    WARNING: It is not working... check pending
    */
    public void mouseWheelMoved(MouseWheelEvent e) {
        System.out.println(".");
        canvasWidget.repaint();
    }

    public void keyPressed(KeyEvent e) {
        if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) {
            System.exit(0);
        }
    }

    public void keyReleased(KeyEvent e) {
        canvasWidget.repaint();
    }

    /**
    Do NOT call your controller from the `keyTyped` method, or the controller
    will be invoked twice for each key. Call it only from the `keyPressed` and
    `keyReleased` method
    */
    public void keyTyped(KeyEvent e) {
        ;
    }

    public JMenuBar buildMenu() {
        JMenuBar menubarWidget;
        JMenu popupWidget;
        JMenuItem option;

        menubarWidget = new JMenuBar();
        popupWidget = new JMenu("File");
        menubarWidget.add(popupWidget);

        option = popupWidget.add(new JMenuItem("Exit"));
        option.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }});

        popupWidget.getPopupMenu().setLightWeightPopupEnabled(false);

        return menubarWidget;
    }
}

class MyButtonsPanel extends JPanel implements ActionListener
{
    private MyCanvasPanel workingAreaCanvasWidget;

    public MyButtonsPanel(MyCanvasPanel p) {
        JButton b = null;

        b = new JButton("Exit");
        b.addActionListener(this);
        add(b);

        b = new JButton("Change Image");
        b.addActionListener(this);
        add(b);

        b = new JButton("Save Image as output.jpg");
        b.addActionListener(this);
        add(b);

        workingAreaCanvasWidget = p;
    }

    public void actionPerformed(ActionEvent ev) {
        String label = ev.getActionCommand();

        if ( label.equals("Exit") ) {
            System.exit(1);
        }
        else if ( label.equals("Change Image") ) {
            performImageOperation1(workingAreaCanvasWidget.img);
            workingAreaCanvasWidget.repaint();
        }
        else if ( label.equals("Save Image as output.jpg") ) {
            ImagePersistence.exportJPG(new File("output.jpg"), 
                                       workingAreaCanvasWidget.img);
        }
    }

    private void performImageOperation1(Image img)
    {
        int x, y;
        int xSize, ySize;
        RGBPixel p = new RGBPixel();

        xSize = img.getXSize();
        ySize = img.getYSize();
        for ( y = 0; y < ySize/2; y++ ) {
            for ( x = 0; x < xSize/2; x++ ) {
                p.r = VSDK.unsigned8BitInteger2signedByte(255);
                p.g = VSDK.unsigned8BitInteger2signedByte(0);
                p.b = VSDK.unsigned8BitInteger2signedByte(0);
                img.putPixelRgb(x, y, p);
            }
        }
    }
}

//===========================================================================

class MyCanvasPanel extends JPanel
{
    public RGBImage img;
    String imageFilename = "../../../etc/images/render.jpg";

    public MyCanvasPanel() {
        try {
            img = ImagePersistence.importRGB(new File(imageFilename));

            /*
            // If using IndexedColorImage, try this code, and think on it...
            GrayScalePalette p = new GrayScalePalette();
            //p.setColorAt(254, 1, 0, 0); // Try to enable this 2 lines...
            //p.setColorAt(255, 0, 1, 0); // explain performance change!
            img = new IndexedColorImage(p);
            img.init(640, 480);
            img.createTestPattern();
            */
        }
        catch ( Exception e ) {
            System.err.println("Error: could not read the image file \"" + imageFilename + "\".");
            System.err.println("Check you have access to that file from current working directory.");
            System.err.println(e);
            System.exit(0);
        }
    }

    public void paint(Graphics dc) {
        super.paint(dc);
        AwtRGBImageRenderer.draw(dc, img, 100, 50);
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
