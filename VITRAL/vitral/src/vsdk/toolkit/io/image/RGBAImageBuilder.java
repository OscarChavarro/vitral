//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - September 2 2005 - David Diaz: Original base version                  =
//= - November 24 2005 - Oscar Chavarro: check pending:                     =
//=   THIS CLASS HAS NOT BEEN REVISED... DESIGN COULD CHANGE                =
//===========================================================================

package vsdk.toolkit.io.image;

import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.awt.image.ColorModel;
import javax.swing.ImageIcon;

import vsdk.toolkit.media.RGBAImage;

/**
Does this implements a "Builder" design pattern???
 */
public class RGBAImageBuilder
{
    /**
    Will change:
      - Choose a better name for this method
      - Do not recieve a File, but a Stream of bytes
      - Move this class to readers/helpers/util package
    */
    public static RGBAImage buildImage(File imagen) throws ImageNotRecognizedException
    {
        String type = accept(imagen);
        RGBAImage retImage = new RGBAImage();

        if( type.equals("tga") ) {
            TargaImage t = new TargaImage(imagen);
            retImage.setRawImage(t.getTexture(), t.getPixelDepth(), t.getWidth(), t.getHeight());
            return retImage;
        }
        if( type.equals("jpg") || type.equals("png") || 
            type.equals("jpeg") || type.equals("gif") )  {
            Toolkit awtTools = Toolkit.getDefaultToolkit();
            Image image = awtTools.getImage(imagen.getAbsolutePath());
            BufferedImage bi = toBufferedImage(image);
            
            int w = bi.getWidth();
            int h = bi.getHeight();
            //System.out.println("w: "+w+"; h: "+h);
            retImage.init(w, h);
            int x, y;
            int pixel;
            byte r, g, b, a;

            //DataBuffer salvage = bi.getRaster().getDataBuffer();

            for ( y = 0; y < h; y++ ) {
                for ( x = 0; x < w; x++ ) {
                    // Warning: This method call is so slow...
            pixel = bi.getRGB(x, y);
                    a = (byte)((pixel & 0xFF000000) >> 24);
                    r = (byte)((pixel & 0x00FF0000) >> 16);
            g = (byte)((pixel & 0x0000FF00) >> 8);
            b = (byte)((pixel & 0x000000FF));
                    retImage.putPixel(x, y, r, g, b, a);
        }
        }
/*
            int[] pix = new int[w*h];

            pix = bi.getRGB(0, 0, w, h, pix, 0, w);
            
            int pixelDepth=24;
            if(hasAlpha(i))
            {
                pixelDepth=32;
            }
            byte[] data=new byte[w*h*pixelDepth];
            
            transferPixels(pix, data, w, h, pixelDepth); 
            retImage.setRawImage(data, pixelDepth, w, h);
*/

            return retImage;
        }
        throw new ImageNotRecognizedException("Image not recognized", imagen);
    }
   
    private static void transferPixels(int[] ori, byte[] dest, int w, int h, int pixelDepth)
    {
        int bPos=0;
        for(int i=0; i<w*h; i++)
        {
            dest[bPos]=(byte)(ori[i]>>16);
            bPos++;
            dest[bPos]=(byte)(ori[i]>>8);
            bPos++;
            dest[bPos]=(byte)(ori[i]>>0);
            bPos++;
            if(pixelDepth==32)
            {
                dest[bPos]=(byte)(ori[i]>>24);
                bPos++;
            }
        }
    }
    
    private static boolean hasAlpha(Image image) 
    {
        if (image instanceof BufferedImage) 
        {
            BufferedImage bimage = (BufferedImage)image;
            return bimage.getColorModel().hasAlpha();
        }
    
        PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
        try 
        {
            pg.grabPixels();
        } 
        catch (InterruptedException e) 
        {
        }
    
        ColorModel cm = pg.getColorModel();
        return cm.hasAlpha();
    }

    private static BufferedImage toBufferedImage(Image image) 
    {
        if ( image instanceof BufferedImage ) {
            return (BufferedImage)image;
        }
    System.out.println(image.getClass().getName());
    
        // This code ensures that all the pixels in the image are loaded
        image = new ImageIcon(image).getImage();
    
        // Determine if the image has transparent pixels; for this method's
        // implementation, see e661 Determining If an Image Has Transparent Pixels
        boolean hasAlpha = hasAlpha(image); 
    
        // Create a buffered image with a format that's compatible with the screen
        BufferedImage bimage = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try 
        {
            // Determine the type of transparency of the new buffered image
            int transparency = Transparency.OPAQUE;
            if (hasAlpha) 
            {
                transparency = Transparency.BITMASK;
            }
    
            // Create the buffered image
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            bimage = gc.createCompatibleImage(image.getWidth(null), image.getHeight(null), transparency);
        } 
        catch (HeadlessException e) 
        {
            // The system does not have a screen
        }
    
        if (bimage == null) 
        {
            // Create a buffered image using the default color model
            int type = BufferedImage.TYPE_INT_RGB;
            if (hasAlpha) 
            {
                type = BufferedImage.TYPE_INT_ARGB;
            }
            bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
        }
    
        // Copy image to buffered image
        Graphics g = bimage.createGraphics();
    
        // Paint the image onto the buffered image
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return bimage;
    }
    
    private static String accept(File f)
    {
        String fName=f.getName();
        StringTokenizer st=new StringTokenizer(fName, ".");
        int numTokens=st.countTokens();
        for(int i=0; i<numTokens-1;i++)
        {
            st.nextToken();
        }
        String ext=st.nextToken();
        return ext;
    }
    
    
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
