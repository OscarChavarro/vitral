//===========================================================================

// VITRAL recomendation: Use explicit class imports (not .*) in hello world 
// type programs so the user/programmer can be exposed to all the complexity 
// involved. This will help him to dominate the involved libraries.

import java.io.IOException;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.geom.AffineTransform;

import javax.swing.JFrame;
import javax.swing.JPanel;

import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import com.sun.media.jai.codec.FileSeekableStream;

/**
This program decodes an image file of any JAI supported format, such as GIF,
JPEG, TIFF, BMP, PNM, PNG, into a RenderedImage, scales the image by 2X with
bilinear interpolation, and then displays the result of the scale
operation.
*/
public class HelloWorldJAI extends JPanel {

    private RenderedOp image1, image2;

    public HelloWorldJAI(String[] args) {
        //- 1. Prepare for image input ------------------------------------
        // Validate input.
        if ( args.length != 1 ) {
            System.out.println("Usage: java JAISampleProgram " +
                               "input_image_filename");
            System.exit(-1);
        }

        /* Create an input stream from the specified file name
        to be used with the file decoding operator. */
        FileSeekableStream stream = null;
        try {
            stream = new FileSeekableStream(args[0]);
          } 
          catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }

        //- 2. Do current image processing operations ---------------------
        // Create an operator to decode the image file. 
        image1 = JAI.create("stream", stream);

        // Create a standard bilinear interpolation object to be
        // used with the "scale" operator.
        Interpolation interp = Interpolation.getInstance(
                                   Interpolation.INTERP_BILINEAR);

        // Stores the required input source and parameters in a ParameterBlock
        // to be sent to the operation registry, and eventually to the "scale" 
        // operator.
        ParameterBlock params = new ParameterBlock();
        params.addSource(image1);
        params.add(2.0F);         // x scale factor
        params.add(2.0F);         // y scale factor
        params.add(0.0F);         // x translate
        params.add(0.0F);         // y translate
        params.add(interp);       // interpolation method

        // Create an operator to scale image1.
        image2 = JAI.create("scale", params);
    }

    public static void main(String[] args) {
        HelloWorldJAI canvas = new HelloWorldJAI(args);

        // Create a frame to contain the panel.
        int width = canvas.image2.getWidth();
        int height = canvas.image2.getHeight();
        JFrame widgetMainWindow;

        widgetMainWindow = new JFrame("VITRAL concept test - JAI Hello World");
        widgetMainWindow.add(canvas);
        widgetMainWindow.setSize(width+100, height+100);
        widgetMainWindow.setVisible(true);
        widgetMainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void paint(Graphics gc) {
        gc.drawLine(0, 0, 100, 100);
        BufferedImage bi = image2.getAsBufferedImage();
        ((Graphics2D)gc).drawRenderedImage(bi, new AffineTransform());
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
