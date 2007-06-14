//===========================================================================

// VITRAL recomendation: Use explicit class imports (not .*) in hello world 
// type programs so the user/programmer can be exposed to all the complexity 
// involved. This will help him to dominate the involved libraries.

// Basic JDK classes
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

// GUI classes
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JFrame;
import javax.swing.JPanel;

// JAI classes
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.BorderExtender;

// VitralSDK classes
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.render.awt.AwtRGBImageRenderer;

/**
*/
public class jaitest extends JPanel implements KeyListener {

    private RGBImage sourceImage;
    private RGBImage processedImage;
    private double rotationAngle = 0;
    private int startX = 0;
    private int startY = 0;
    private int delta = 10;
    private String namePattern = null;

    public jaitest(String[] args) {
        if ( args.length != 1 ) {
            System.err.println("Error: usage with 1 parameter");
            System.exit(1);
        }

        namePattern = args[0];

        //- Prepare for image input ---------------------------------------
        sourceImage = new RGBImage();
        processedImage = new RGBImage();
        String imageFilename = args[0];
        try {
            System.out.print("Loading... ");
            sourceImage = ImagePersistence.importRGB(new File(imageFilename));
            System.out.println("Ok!");
        }
        catch ( Exception e ) {
            System.err.println("Error: could not read the image file \"" + imageFilename + "\".");
            System.err.println("Check you have access to that file from current working directory.");
            System.err.println(e);
            System.exit(0);
        }


        processImage();
    }

    public void processImage()
    {
        //- Do current image processing operations ------------------------
        // Create an operator to decode the image file. 
        RenderedOp image1;
        RenderedOp image2;

        image1 = JAI.create("AWTImage", (java.awt.Image)
            AwtRGBImageRenderer.exportToAwtBufferedImage(sourceImage));

        // Create a standard bilinear interpolation object to be
        // used with the "scale" operator.
        Interpolation interp = Interpolation.getInstance(
                                   Interpolation.INTERP_BILINEAR);

        ParameterBlock params = new ParameterBlock();
        params.addSource(image1);
        params.add(0.0f);         // x pivot coordinate
        params.add(0.0f);         // y pivot coordinate
        params.add((float)Math.toRadians(rotationAngle));
        params.add(interp);       // interpolation method
        image2 = JAI.create("rotate", params);
        BufferedImage bi = image2.getAsBufferedImage();
        RGBImage uncut = new RGBImage();
        AwtRGBImageRenderer.importFromAwtBufferedImage(bi, uncut);
        if ( processedImage.getXSize() != uncut.getXSize() ||
             processedImage.getYSize() != uncut.getYSize() ) {
            processedImage.init(uncut.getXSize(), uncut.getYSize());
        }
        int i, j, ii, jj;
        RGBPixel p;
        for ( i = 0; i < uncut.getXSize(); i++ ) {
            for ( j = 0; j < uncut.getYSize(); j++ ) {
                p = uncut.getPixelRgb(i, j);
        ii = i-startX;
        jj = j-startY;
                if ( ii >= 0 && jj >= 0 &&
                     ii < processedImage.getXSize() &&
                     jj < processedImage.getYSize() ) {
                    processedImage.putPixel(ii, jj, p);
                }
            }
        }
    }

    public static void main(String[] args) {
        jaitest canvas = new jaitest(args);
        canvas.addKeyListener(canvas);

        // Create a frame to contain the panel.
        int width = canvas.processedImage.getXSize();
        int height = canvas.processedImage.getYSize();
        JFrame widgetMainWindow;

        widgetMainWindow = new JFrame("VITRAL concept test - JAI tests");
        widgetMainWindow.add(canvas);
        widgetMainWindow.setSize(width+50, height+50);
        widgetMainWindow.setVisible(true);
        canvas.requestFocusInWindow();
        widgetMainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void paint(Graphics gc) {
        super.paint(gc);
        AwtRGBImageRenderer.draw(gc, processedImage, 0, 0);
        gc.setColor(new Color(255, 0, 0));
        gc.drawLine(0, 10, 4096, 10);
        gc.drawLine(0, 30, 4096, 30);
        gc.drawLine(0, 50, 4096, 50);
        gc.drawLine(0, 70, 4096, 70);
        gc.drawOval(1545-100, 502-100, 200, 200);
        gc.drawOval(474-85, 181-85, 170, 170);
        gc.setColor(new Color(0, 0, 0));
    }

    public void keyPressed(KeyEvent e) {
        if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) {
            System.exit(0);
        }

        int unicode_id = e.getKeyChar();
        boolean redraw = false;
        if ( unicode_id != e.CHAR_UNDEFINED ) {
            switch ( unicode_id ) {
              case 'T': 
                rotationAngle += 5.0;
                redraw = true;
                break;
              case 't': 
                rotationAngle -= 5.0;
                redraw = true;
                break;
              case 'R': 
                rotationAngle += 0.5;
                redraw = true;
                break;
              case 'r': 
                rotationAngle -= 0.5;
                redraw = true;
                break;
              case 'X': 
                startX += delta;
                redraw = true;
                break;
              case 'x': 
                startX -= delta;
                redraw = true;
                break;
              case 'Y': 
                startY += delta;
                redraw = true;
                break;
              case 'y': 
                startY -= delta;
                redraw = true;
                break; 
              case 'u': 
                switch ( delta ) {
                    case 100: delta = 1; break;
                    case 1: delta = 10; break;
                    default: delta = 100; break;
                }
                System.out.println("New delta steps: " + delta);
                break;
              case ' ':
                processedImage.init(1, 1);
                processImage();
                ImagePersistence.exportPPM(new File(namePattern + ".ppm"), processedImage);
                try {
                    BufferedOutputStream writer;
            byte arr[];

                    writer = new BufferedOutputStream(new FileOutputStream(new File(namePattern + ".log")));
                    String data = namePattern + "\t" + startX + "\t" + startY + "\t" + rotationAngle + "\n";
                    arr = data.getBytes();
            writer.write(arr, 0, arr.length);
                    writer.flush();
                    writer.close();
        }
        catch ( Exception ex ) {
            System.out.println("Error writing data!");
        }
                
                break;
           }
        }
        if ( redraw ) {
            System.out.println("Rotation: " + rotationAngle);
            System.out.println("Start: (" + startX + ", " + startY + ")");
            processImage();
            repaint();
        }
    }

    public void keyReleased(KeyEvent e) {
        ;
    }

    public void keyTyped(KeyEvent e) {
        ;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
