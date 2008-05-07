//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - September 2 2005 - David Diaz: Original base version                  =
//= - November 24 2005 - Oscar Chavarro: check pending:                     =
//= - May 22 2006 - David Diaz/Oscar Chavarro: documentation added          =
//= - August 6 2006                                                         =
//=   - Oscar Chavarro: managed RGB and RGBA cases independently            =
//=   - Oscar Chavarro: Awt BufferedImage convertion moved to render.awt    =
//= - May 1 2007 - Oscar Chavarro: updated to ImageIO API                   =
//===========================================================================

package vsdk.toolkit.io.image;

// Basic JDK classes
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

// Extended JDK classes
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.media.IndexedColorImage;
import vsdk.toolkit.render.awt.AwtIndexedColorImageRenderer;
import vsdk.toolkit.render.awt.AwtRGBImageRenderer;
import vsdk.toolkit.render.awt.AwtRGBAImageRenderer;
import vsdk.toolkit.io.PersistenceElement;

/**
This class is a front end front which images of various formats can be
exported and/or imported to/from files.

@todo Does this implements a "Builder" design pattern??? A Factory design 
      pattern... possibly some of that combined with a Facade design 
      pattern?
 */
public class ImagePersistence extends PersistenceElement
{
    /**
    Given the filename of an input data file which contains an image, this
    method tries to recognize the file format and load the contents of it
    to the image.

    @todo Do not assume the file format only from the filename extension,
    but trying to detect file headers.

    @param imagen - The file respesenting the image
    @return An RGBAImage entity that contains the image loaded in memory.

    Will change:
      - Choose a better name for this method
      - Do not recieve a File, but a Stream of bytes
    */
    public static RGBAImage importRGBA(File imagen) throws ImageNotRecognizedException
    {
        String type = extractExtensionFromFile(imagen);
        RGBAImage retImage = new RGBAImage();

        if( type.equals("tga") ) {
            TargaImage t = new TargaImage(imagen);
            t.exportRGBA(retImage);
            return retImage;
        }
        else if( type.equals("jpg") || type.equals("jpeg") ||
                 type.equals("gif") || type.equals("png") )  {
            BufferedImage bi = null;

            // OLD SLOW METHOD, DO NOT USE!
            //java.awt.Toolkit awtTools = java.awt.Toolkit.getDefaultToolkit();
            //java.awt.Image image;
            //image = awtTools.getImage(imagen.getAbsolutePath());
            //bi = toBufferedImage(image);

            try {
                bi = ImageIO.read(imagen);
              }
              catch ( Exception e ) {
                  VSDK.reportMessage(null, VSDK.ERROR, "importRGBA",
                                     "Cannot import image file \"" + imagen.getAbsolutePath() + "\"");
                return null;
            }
            AwtRGBAImageRenderer.importFromAwtBufferedImage(bi, retImage);

            return retImage;
        }
        throw new ImageNotRecognizedException("Image not recognized", imagen);
    }

    /**
    Given the filename of an input data file which contains an image, this
    method tries to recognize the file format and load the contents of it
    to the image.

    @todo Do not assume the file format only from the filename extension,
    but trying to detect file headers.

    @param imagen - The file respesenting the image
    @return An RGBImage entity that contains the image loaded in memory.

    Will change:
      - Choose a better name for this method
      - Do not recieve a File, but a Stream of bytes
    */
    public static RGBImage importRGB(File inImageFd) throws ImageNotRecognizedException
    {
        String type = extractExtensionFromFile(inImageFd);
        RGBImage retImage = new RGBImage();

        if( type.equals("tga") ) {
            TargaImage t = new TargaImage(inImageFd);
            t.exportRGB(retImage);
            return retImage;
        }
        else if( type.equals("jpg") || type.equals("jpeg") ||
                 type.equals("gif") || type.equals("png") )  {
            BufferedImage bi = null;

            // OLD SLOW METHOD, DO NOT USE!
            //java.awt.Toolkit awtTools = java.awt.Toolkit.getDefaultToolkit();
            //java.awt.Image image;
            //image = awtTools.getImage(inImageFd.getAbsolutePath());
            //bi = toBufferedImage(image);

            try {
                bi = ImageIO.read(inImageFd);
              }
              catch ( Exception e ) {
                  VSDK.reportMessage(null, VSDK.ERROR, "importRGB",
                                     "Cannot import image file \"" + inImageFd.getAbsolutePath() + "\"");
                 throw new ImageNotRecognizedException("Error reading internal file:\n" + e, inImageFd);
            }
            AwtRGBImageRenderer.importFromAwtBufferedImage(bi, retImage);

            return retImage;
        }
        throw new ImageNotRecognizedException("Image not recognized", inImageFd);
    }

    /**
    Given the filename of an input data file which contains an image, this
    method tries to recognize the file format and load the contents of it
    to the image.

    @todo Do not assume the file format only from the filename extension,
    but trying to detect file headers.

    @param imagen - The file respesenting the image
    @return An IndexedColorImage entity that contains the image loaded in memory.

    Will change:
      - Choose a better name for this method
      - Do not recieve a File, but a Stream of bytes
    */
    public static IndexedColorImage importIndexedColor(File imagen) throws ImageNotRecognizedException
    {
        String type = extractExtensionFromFile(imagen);
        IndexedColorImage retImage;
        Image img;

        if( type.equals("bw") ) {
            img = ImagePersistenceSGI.readImageSGI(imagen.getAbsolutePath());
            if ( img instanceof IndexedColorImage ) {
                retImage = (IndexedColorImage)img;
            }
            else {
                throw new ImageNotRecognizedException("Convertion needed", 
                imagen);
            }
            return retImage;
        }
        else if( type.equals("jpg") || type.equals("jpeg") ||
                 type.equals("gif") || type.equals("png") )  {
            BufferedImage bi = null;

            try {
                bi = ImageIO.read(imagen);
              }
              catch ( Exception e ) {
                  VSDK.reportMessage(null, VSDK.ERROR, "importRGB",
                                     "Cannot import image file \"" + imagen.getAbsolutePath() + "\"");
                 throw new ImageNotRecognizedException("Error reading internal file:\n" + e, imagen);
            }
            retImage = new IndexedColorImage();
            AwtIndexedColorImageRenderer.importFromAwtBufferedImage(bi, retImage);

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
    
    /**
    This method writes the contents of the specified image to a file in 
    binary JPEG image format. Returns true if everything
    works fine, false if something fails, like a permission access denied
    or if storage device runs out of space.
    */    
    public static boolean exportJPG(File fd, Image img)
    {
        try {
            FileOutputStream fos = new FileOutputStream(fd);

            exportJPG(fos, img);

            fos.close();
        }
        catch ( Exception e ) {
            return false;
        }
        return true;
    }

    public static void exportJPG(OutputStream os, Image img)
        throws Exception
    {
        BufferedImage bimg;
        int x, y, xSize, ySize;
        RGBPixel p;

        xSize = img.getXSize();
        ySize = img.getYSize();
        bimg =  new BufferedImage(xSize, ySize, BufferedImage.TYPE_INT_RGB);
        for ( y = 0; y < ySize; y++ ) {
            for ( x = 0; x < xSize; x++ ) {
                p = img.getPixelRgb(x, y);
                bimg.setRGB(x, y, 
                  (VSDK.signedByte2unsignedInteger(p.r)) * 256 * 256 +
                  (VSDK.signedByte2unsignedInteger(p.g)) * 256 +
                  (VSDK.signedByte2unsignedInteger(p.b))
                );
            }
        }

        ImageIO.write(bimg, "jpg", os);

        // OLD DEPRECATED API, DO NOT USE!
        //com.sun.image.codec.jpeg.JPEGImageEncoder jpeg;
        //jpeg = com.sun.image.codec.jpeg.JPEGCodec.createJPEGEncoder(fos);
        //jpeg.encode(bimg);
    }

    /**
    This method writes the contents of the specified image to a file in 
    binary JPEG image format. Returns true if everything
    works fine, false if something fails, like a permission access denied
    or if storage device runs out of space.
    */    
    public static boolean exportPNG(File fd, Image img)
    {
        try {
            FileOutputStream fos = new FileOutputStream(fd);

            exportPNG(fos, img);

            fos.close();
        }
        catch ( Exception e ) {
            return false;
        }
        return true;
    }

    private static void exportPNG_24bitRgb(OutputStream os, Image img)
        throws Exception
    {
        BufferedImage bimg;
        int x, y, xSize, ySize;
        RGBPixel p;

        xSize = img.getXSize();
        ySize = img.getYSize();
        bimg =  new BufferedImage(xSize, ySize, BufferedImage.TYPE_INT_RGB);
        for ( y = 0; y < ySize; y++ ) {
            for ( x = 0; x < xSize; x++ ) {
                p = img.getPixelRgb(x, y);
                bimg.setRGB(x, y, 
                  (VSDK.signedByte2unsignedInteger(p.r)) * 256 * 256 +
                  (VSDK.signedByte2unsignedInteger(p.g)) * 256 +
                  (VSDK.signedByte2unsignedInteger(p.b))
                );
            }
        }

        ImageIO.write(bimg, "png", os);
    }

    public static void exportPNG(OutputStream os, Image img)
        throws Exception
    {
/*
        if ( img instanceof IndexedColorImage ) {
//NOT WORKING
            exportPNG_8bitGrayscale(os, (IndexedColorImage)img);
        }
        else {
*/
        exportPNG_24bitRgb(os, img);
    }

    /**
    This method writes the contents of the specified image to a file in 
    binary GIF image format. Returns true if everything
    works fine, false if something fails, like a permission access denied
    or if storage device runs out of space.
    */    
    public static boolean exportGIF(File fd, Image img)
    {
        try {
            BufferedImage bimg;
            int x, y, xSize, ySize;
            RGBPixel p;

            xSize = img.getXSize();
            ySize = img.getYSize();
            bimg =  new BufferedImage(xSize, ySize, BufferedImage.TYPE_INT_RGB);
            for ( y = 0; y < ySize; y++ ) {
                for ( x = 0; x < xSize; x++ ) {
                    p = img.getPixelRgb(x, y);
                    bimg.setRGB(x, y, 
                      (VSDK.signedByte2unsignedInteger(p.r)) * 256 * 256 +
                      (VSDK.signedByte2unsignedInteger(p.g)) * 256 +
                      (VSDK.signedByte2unsignedInteger(p.b))
                    );
                }
            }

            FileOutputStream fos = new FileOutputStream(fd);

            ImageIO.write(bimg, "gif", fos);

            // OLD DEPRECATED API, DO NOT USE!
            //com.sun.image.codec.jpeg.JPEGImageEncoder jpeg;
            //jpeg = com.sun.image.codec.jpeg.JPEGCodec.createJPEGEncoder(fos);
            //jpeg.encode(bimg);

            fos.close();
        }
        catch ( Exception e ) {
            return false;
        }
        return true;
    }

    /**
    This method writes the contents of the specified image to a file in 
    binary RGB PPM format (i.e. P6 PPM sub-format). Returns true if everything
    works fine, false if something fails, like a permission access denied
    or if storage device runs out of space.
    */
    public static boolean exportPPM(File fd, Image img)
    {
        try {
            BufferedOutputStream writer;
            FileOutputStream fos = new FileOutputStream(fd);
            writer = new BufferedOutputStream(fos);

            String line1 = "P6\n";
            String line2 = "# Image generated by VitralSDK (http://vitral.sf.net)\n";
            String line3 = img.getXSize() + " " + img.getYSize() + "\n";
            String line4 = "255\n";
            byte arr[];

            arr = line1.getBytes();
            writer.write(arr, 0, arr.length);
            arr = line2.getBytes();
            writer.write(arr, 0, arr.length);
            arr = line3.getBytes();
            writer.write(arr, 0, arr.length);
            arr = line4.getBytes();
            writer.write(arr, 0, arr.length);

            RGBPixel p;
            int x = 0, y = 0;
            for ( y = 0; y < img.getYSize(); y++ ) {
                for ( x = 0; x < img.getXSize(); x++ ) {
                    p = img.getPixelRgb(x, y);
                    writer.write(p.r);
                    writer.write(p.g);
                    writer.write(p.b);
                }
            }

            writer.flush();
            writer.close();
            fos.close();
        }
        catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
    This method writes the contents of the specified image to a file in 
    binary GrayScale PPM format (i.e. P5 PPM sub-format). Returns true if
    everything works fine, false if something fails, like a permission access
    denied or if storage device runs out of space.
    */
    public static boolean exportPNM(File fd, Image img)
    {
        try {
            BufferedOutputStream writer;
            FileOutputStream fos = new FileOutputStream(fd);
            writer = new BufferedOutputStream(fos);

            String line1 = "P5\n";
            String line2 = "# Image generated by VitralSDK (http://vitral.sf.net)\n";
            String line3 = img.getXSize() + " " + img.getYSize() + "\n";
            String line4 = "255\n";
            byte arr[];

            arr = line1.getBytes();
            writer.write(arr, 0, arr.length);
            arr = line2.getBytes();
            writer.write(arr, 0, arr.length);
            arr = line3.getBytes();
            writer.write(arr, 0, arr.length);
            arr = line4.getBytes();
            writer.write(arr, 0, arr.length);

            int x = 0, y = 0;
            for ( y = 0; y < img.getYSize(); y++ ) {
                for ( x = 0; x < img.getXSize(); x++ ) {
                    writer.write(img.getPixel8bitGrayScale(x, y));
                }
            }

            writer.flush();
            writer.close();
            fos.close();
        }
        catch (Exception e) {
            return false;
        }
        return true;
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
