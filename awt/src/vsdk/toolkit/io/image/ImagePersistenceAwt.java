//===========================================================================

package vsdk.toolkit.io.image;

// Basic JDK classes
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

// Extended JDK classes
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.media.IndexedColorImage;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.render.awt.AwtIndexedColorImageRenderer;
import vsdk.toolkit.render.awt.AwtRGBImageRenderer;
import vsdk.toolkit.render.awt.AwtRGBAImageRenderer;

public class ImagePersistenceAwt
{
    public IndexedColorImage importIndexedColor(File inImageFd) throws ImageNotRecognizedException
    {
        IndexedColorImage retImage;
        BufferedImage bi = null;

        try {
            BufferedInputStream bis;
            FileInputStream fis;

            fis = new FileInputStream(inImageFd);
            bis = new BufferedInputStream(fis);
            bi = ImageIO.read(bis);
            bis.close();
            fis.close();
          }
          catch ( Exception e ) {
              VSDK.reportMessage(null, VSDK.ERROR, "importRGB (C)",
                                 "Cannot import image file \"" + inImageFd.getAbsolutePath() + "\"");
             throw new ImageNotRecognizedException("Error reading internal file:\n" + e, inImageFd);
        }
        retImage = new IndexedColorImage();
        AwtIndexedColorImageRenderer.importFromAwtBufferedImage(bi, retImage);

        return retImage;
    }

    public RGBImage importRGB(File inImageFd) throws ImageNotRecognizedException, Exception
    {
        RGBImage retImage = new RGBImage();
        BufferedImage bi = null;

        // OLD SLOW METHOD, DO NOT USE!
        //java.awt.Toolkit awtTools = java.awt.Toolkit.getDefaultToolkit();
        //java.awt.Image image;
        //image = awtTools.getImage(inImageFd.getAbsolutePath());
        //bi = toBufferedImage(image);

        try {
            BufferedInputStream bis;
            FileInputStream fis;

            fis = new FileInputStream(inImageFd);
            bis = new BufferedInputStream(fis);
            bi = ImageIO.read(bis);
            bis.close();
            fis.close();
          }
          catch ( Exception e ) {
              VSDK.reportMessage(null, VSDK.ERROR, "importRGB (A)",
                                 "Cannot import image file \"" + inImageFd.getAbsolutePath() + "\"");
             throw new ImageNotRecognizedException("Error reading internal file:\n" + e, inImageFd);
        }
        AwtRGBImageRenderer.importFromAwtBufferedImage(bi, retImage);

        return retImage;
    }

    public RGBAImage importRGBA(File inImageFd) throws ImageNotRecognizedException, Exception
    {
        RGBAImage retImage = new RGBAImage();
        BufferedImage bi = null;

        // OLD SLOW METHOD, DO NOT USE!
        //java.awt.Toolkit awtTools = java.awt.Toolkit.getDefaultToolkit();
        //java.awt.Image image;
        //image = awtTools.getImage(inImageFd.getAbsolutePath());
        //bi = toBufferedImage(image);

        try {
            BufferedInputStream bis;
            FileInputStream fis;

            fis = new FileInputStream(inImageFd);
            bis = new BufferedInputStream(fis);
            bi = ImageIO.read(bis);
            bis.close();
            fis.close();
          }
          catch ( Exception e ) {
              VSDK.reportMessage(null, VSDK.ERROR, "importRGBA",
                                 "Cannot import image file \"" + inImageFd.getAbsolutePath() + "\"");
            return null;
        }
        AwtRGBAImageRenderer.importFromAwtBufferedImage(bi, retImage);

        return retImage;
    }

    //=================================================================
    // DESACTIVATED METHODS!!! DO NOT USE!!!
    //=================================================================
/*
    private static boolean hasAlpha(java.awt.Image image) 
    {
        if (image instanceof BufferedImage) 
        {
            BufferedImage bimage = (BufferedImage)image;
            return bimage.getColorModel().hasAlpha();
        }
    
        java.awt.image.PixelGrabber pg;
        pg = new java.awt.image.PixelGrabber(image, 0, 0, 1, 1, false);
        try 
        {
            pg.grabPixels();
        } 
        catch (InterruptedException e) 
        {
        }
    
        java.awt.image.ColorModel cm = pg.getColorModel();
        return cm.hasAlpha();
    }

    private static BufferedImage toBufferedImage(java.awt.Image image) 
    {
        if ( image instanceof BufferedImage ) {
            return (BufferedImage)image;
        }
        //System.out.println(image.getClass().getName());
    
        // This code ensures that all the pixels in the image are loaded
        image = new javax.swing.ImageIcon(image).getImage();
    
        // Determine if the image has transparent pixels; for this method's
        // implementation, see e661 Determining If an Image Has Transparent Pixels
        boolean hasAlpha = hasAlpha(image); 
    
        // Create a buffered image with a format that's compatible with the screen
        BufferedImage bimage = null;
        java.awt.GraphicsEnvironment ge;
        ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
        try 
        {
            // Determine the type of transparency of the new buffered image
            int transparency = java.awt.Transparency.OPAQUE;
            if ( hasAlpha ) {
                transparency = java.awt.Transparency.BITMASK;
            }
    
            // Create the buffered image
            java.awt.GraphicsDevice gs = ge.getDefaultScreenDevice();
            java.awt.GraphicsConfiguration gc = gs.getDefaultConfiguration();
            bimage = gc.createCompatibleImage(image.getWidth(null), image.getHeight(null), transparency);
        } 
        catch ( java.awt.HeadlessException e ) {
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
        java.awt.Graphics g = bimage.createGraphics();
    
        // Paint the image onto the buffered image
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return bimage;
    }
*/

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
