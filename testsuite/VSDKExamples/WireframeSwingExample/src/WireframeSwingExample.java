//===========================================================================

// Awt / swing classes
import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;

/**
Note that this program is designed to work as a java application, or as a
java applet.  If current class does not extends from Applet, and `init` method
is deleted, this will continue working as a simple java application.

This is a simple programme recommended for use as a template in the development
of VitralSDK programs by incremental modification.
*/
@SuppressWarnings("removal")
public class WireframeSwingExample extends Applet
{
    public boolean appletMode;
    public SwingCanvas canvas;

    /**
    When running this class inside a browser (in applet mode) there is no
    warranty of calling this method, or calling before init. It is recommended
    that real initialization be done in another `createModel` method, and
    that such method be called explicity from entry point function.
    */
    public WireframeSwingExample() {
        // Empty! call `createModel` explicity from entry point function!
        ;
    }

    private void createGUI()
    {
        canvas = new SwingCanvas(appletMode);
    }

    public static void main (String[] args) {
        // Common VitralSDK initialization
        WireframeSwingExample instance = new WireframeSwingExample();
        instance.appletMode = false;

        // Create application based GUI
        JFrame frame;
        Dimension size;

        instance.createGUI();
        frame = new JFrame("VITRAL concept test - Camera control example");
        frame.add(instance.canvas, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        size = new Dimension(640, 480);
        //frame.setMinimumSize(size);
        frame.setSize(size);
        frame.setVisible(true);
        instance.canvas.requestFocusInWindow();
    }

    public void init()
    {
        appletMode = true;
        setLayout(new BorderLayout());
        createGUI();
        add("Center", canvas);
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
