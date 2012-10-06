//===========================================================================

package application.gui;

// Java basic classes
import java.io.FileReader;

// Java GUI classes
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import javax.swing.border.Border; 
import javax.swing.BorderFactory; 
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;

// VSDK classes
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.render.awt.AwtRGBImageRenderer;

// Application classes
import vsdk.transition.io.presentation.GuiPersistence;
import vsdk.transition.gui.Gui;
import vsdk.transition.render.swing.SwingGuiRenderer;

public class SwingImageControlWindow
{
    private JFrame windowWidget;
    public JLabel statusMessage;
    private RGBImage controlledImage;
    private ImageDisplayPanel workArea;

    public SwingImageControlWindow(RGBImage image, 
                                 Gui gui, ActionListener executor) {
        controlledImage = image;

        windowWidget = new JFrame("Image control tool");
        windowWidget.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE );

        JMenuBar menubar;
        menubar = SwingGuiRenderer.buildMenubar(gui, null, executor);

        JPanel statusBar = createStatusBar();
        workArea = new ImageDisplayPanel(controlledImage);

        windowWidget.add(workArea, BorderLayout.CENTER);
        windowWidget.add(statusBar, BorderLayout.SOUTH);
        windowWidget.setJMenuBar(menubar);

        windowWidget.setPreferredSize(new Dimension(640, 480));
        windowWidget.pack();
        windowWidget.setVisible(true);
    }

    public void setImage(RGBImage i)
    {
        controlledImage = i;
        workArea.setImage(i);
    }

    public void redrawImage()
    {
        windowWidget.setVisible(true);
        workArea.repaint();
    }

    private JPanel createStatusBar()
    {
        JPanel panel;

        statusMessage = new JLabel("Image control window ready");
        Border border = BorderFactory.createLoweredBevelBorder();
        statusMessage.setBorder(border);

        panel = new JPanel();
        panel.setLayout(new GridLayout());

        border = BorderFactory.createEmptyBorder(3, 3, 3, 3);
        panel.setBorder(border);

        panel.add(statusMessage);

        return panel;
    }
}

class ImageDisplayPanel extends JPanel
{
    RGBImage imageToPaint;

    public ImageDisplayPanel(RGBImage img)
    {
        Dimension d = new Dimension(320+20, 240+30);

        setMinimumSize(d);        
        setSize(d);        
        imageToPaint = img;
    }

    public void setImage(RGBImage i)
    {
        imageToPaint = i;
    }

    public void paint(Graphics g)
    {
        super.paint(g);
        AwtRGBImageRenderer.draw(g, imageToPaint, 10, 10);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
