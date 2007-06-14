/*
 * RGBImageFactory.java
 *
 * Created on 2 de septiembre de 2005, 10:35 AM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package vitral.toolkits.media;

import java.io.*;
import java.util.*;
import java.awt.Toolkit;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
/**
 *
 * @author usuario
 */
public class RGBAImageBuilder 
{
    public static RGBAImage buildImage(File imagen)throws ImageNotRecognizedException
    {
        String type=accept(imagen);
        RGBAImage retImage=new RGBAImage();
        if(type.equals("tga"))
        {
            TargaImage t=new TargaImage(imagen);
            retImage.loadImage(t.getTexture(), t.getPixelDepth(), t.getWidth(), t.getHeight());
            return retImage;
        }
        if(type.equals("jpg") || type.equals("png") || type.equals("jpeg") || type.equals("gif"))
        {
            Toolkit tools=Toolkit.getDefaultToolkit();
            Image i=tools.getImage(imagen.getAbsolutePath());
            
            BufferedImage bi=toBufferedImage(i);
            
            int w=bi.getWidth();
            int h=bi.getHeight();
            System.out.println("w: "+w+"; h: "+h);
            int[] pix=new int[w*h];
            
            pix=bi.getRGB(0,0,w,h, pix,0,w);
            
            int pixelDepth=24;
            if(hasAlpha(i))
            {
                pixelDepth=32;
            }
            byte[] data=new byte[w*h*pixelDepth];
            
            transferPixels(pix, data, w, h, pixelDepth); 
            retImage.loadImage(data, pixelDepth, w, h);
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
        if (image instanceof BufferedImage) 
        {
            return (BufferedImage)image;
        }
    
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
