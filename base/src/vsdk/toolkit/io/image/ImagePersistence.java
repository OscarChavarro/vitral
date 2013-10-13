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
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.StringTokenizer;

// Extended JDK classes
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

// VitralSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.media.IndexedColorImage;
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
    public static boolean nativeWarningGiven = false;
    private static ImagePersistenceJogl joglHelper;
    private static ImagePersistenceAwt awtHelper;

    static {
        joglHelper = null;
        awtHelper = null;

        // Comment this line out if JOGL is not available
        joglHelper = new ImagePersistenceJogl();

        // Comment this line out if Awt is not avaible
        awtHelper = new ImagePersistenceAwt();
    }

    private static RGBAImage importDDSRGBA(File inImageFd)
    {
        RGBAImage data = null;

        if ( joglHelper != null ) {
            data = joglHelper.importDDSRGBA(inImageFd);
        }
        else {
            data = createNotAvailableImageRGBA();
        }
        return data;
    }

    private static RGBAImage createNotAvailableImageRGBA()
    {
        RGBAImage data;
        data = new RGBAImage();
        data.init(256, 256);
        data.createTestPattern();
        VSDK.reportMessage(null, VSDK.WARNING, 
            "ImagePersistence",
            "Helper class not available, returning test RGBA image"
        );
        return data;
    }

    private static RGBImage createNotAvailableImageRGB()
    {
        RGBImage data;
        data = new RGBImage();
        data.init(256, 256);
        data.createTestPattern();
        VSDK.reportMessage(null, VSDK.WARNING, 
            "ImagePersistence",
            "Helper class not available, returning test RGB image"
        );
        return data;
    }

    private static IndexedColorImage createNotAvailableImageIndexedColor()
    {
        IndexedColorImage data;
        data = new IndexedColorImage();
        data.init(256, 256);
        data.createTestPattern();
        VSDK.reportMessage(null, VSDK.WARNING, 
            "ImagePersistence",
            "Helper class not available, returning test IndexedColor image"
        );
        return data;
    }

    private static RGBImage importDDSRGB(File inImageFd)
    {
        RGBImage data = null;

        if ( joglHelper != null ) {
            data = joglHelper.importDDSRGB(inImageFd);
        }
        else {
            data = createNotAvailableImageRGB();
        }
        return data;
    }

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
    public static RGBAImage importRGBA(File inImageFd) throws ImageNotRecognizedException, Exception
    {
        String type = extractExtensionFromFile(inImageFd);
        RGBAImage retImage = new RGBAImage();

        //- Try optimized reading, if native library is available ---------
        if ( NativeImageReaderWrapper.available && type.equals("png") ) {
            _NativeImageReaderWrapperHeaderInfo header;
            header = new _NativeImageReaderWrapperHeaderInfo();
            NativeImageReaderWrapper.readPngHeader(header,
                inImageFd.getAbsolutePath());
            retImage.initNoFill((int)header.xSize, (int)header.ySize);
            NativeImageReaderWrapper.readPngDataRGBA(header, retImage.getRawImageDirectBuffer());
            return retImage;
        }

        if ( !nativeWarningGiven && !NativeImageReaderWrapper.available &&
             type.equals("png") ) {
            nativeWarningGiven = true;
            VSDK.reportMessage(null, VSDK.WARNING, "ImagePersistence.importRGBA", "NativeImageReader library not found, falling to AWT-based PNG reading, which can be slow.");
        }

        //-----------------------------------------------------------------
        if( type.equals("tga") ) {
            ImagePersistenceAwtTarga t = new ImagePersistenceAwtTarga(inImageFd);
            t.exportRGBA(retImage);
            return retImage;
        }
        else if( type.equals("jpg") || type.equals("jpeg") ||
                 type.equals("gif") || type.equals("png") )  {
            if ( awtHelper != null ) {
                return awtHelper.importRGBA(inImageFd);
	    }
	    else {
		return createNotAvailableImageRGBA();
	    }
        }
        else if( type.equals("dds") ) {
            //delete retImage;
            return importDDSRGBA(inImageFd);
        }
        throw new ImageNotRecognizedException("Image not recognized", inImageFd);
    }

    /**
    Given a text string, this method determines if is a Unix style single
    line comment, that is, if line begins with a '#' character.
    */
    private static boolean
    isTextComment(String line)
    {
        char arr[] = line.toCharArray();
        int i;

        for ( i = 0; i < arr.length && (arr[i] != ' ' && arr[i] != '\t'); i++ );

        if ( i < arr.length && arr[i] == '#' ) {
            return true;
        }
        return false;
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
    public static RGBImage importRGB(File inImageFd) throws ImageNotRecognizedException, Exception
    {
        String type = extractExtensionFromFile(inImageFd);
        RGBImage retImage = new RGBImage();

        //- Try optimized reading, if native library is available ---------
        if ( NativeImageReaderWrapper.available && type.equals("png") ) {
            _NativeImageReaderWrapperHeaderInfo header;
            header = new _NativeImageReaderWrapperHeaderInfo();
            NativeImageReaderWrapper.readPngHeader(header,
                inImageFd.getAbsolutePath());
            retImage.initNoFill((int)header.xSize, (int)header.ySize);
            NativeImageReaderWrapper.readPngDataRGB(header, retImage.getRawImageDirectBuffer());
            return retImage;
        }

        if ( !nativeWarningGiven && !NativeImageReaderWrapper.available &&
             type.equals("png") ) {
            nativeWarningGiven = true;
            VSDK.reportMessage(null, VSDK.WARNING, "ImagePersistence.importRGB", "NativeImageReader library not found, falling to AWT-based PNG reading, which can be slow.");
        }

        //-----------------------------------------------------------------
        if( type.equals("tga") ) {
            ImagePersistenceAwtTarga t = new ImagePersistenceAwtTarga(inImageFd);
            t.exportRGB(retImage);
            return retImage;
        }
        else if( type.equals("jpg") || type.equals("jpeg") ||
                 type.equals("gif") || type.equals("png") )  {
            if ( awtHelper != null ) {
                return awtHelper.importRGB(inImageFd);
      	      }
	      else {
		return createNotAvailableImageRGB();
	    }

        }
        else if( type.equals("dds") ) {
            //delete retImage;
            return importDDSRGB(inImageFd);
        }
        else if( type.equals("ppm") )  {
            try {
                BufferedInputStream bis;
                FileInputStream fis;

                fis = new FileInputStream(inImageFd);
                bis = new BufferedInputStream(fis);

                boolean exit = false;
                String line;
                int stage = 1;
                int xSize = 0, ySize = 0;

                do {
                    line = readAsciiLine(bis);
                    if ( line.equals("255") ) {
                        exit = true;
                    }
                    if ( isTextComment(line) ) {
                        continue;
                    }
                    switch ( stage ) {
                      case 1: // PPM signature - data type
                        if ( !line.startsWith("P6") ) {
                            throw new ImageNotRecognizedException("Error reading internal PPM file subformat:\n" + line, inImageFd);
                        }
                        stage++;
                        break;
                      case 2:
                        if ( line.startsWith("#") ) {
                            // Skip comment line
                            ;
                          }
                          else {
                            StringTokenizer parser;
                            parser = new StringTokenizer(line);
                            xSize = Integer.parseInt(parser.nextToken());
                            ySize = Integer.parseInt(parser.nextToken());
                            stage++;
                        }
                        break;
                    }

                } while ( !exit );

                retImage = new RGBImage();
                retImage.initNoFill(xSize, ySize);
                //byte barr[] = retImage.getRawImage();
                //readBytes(bis, barr);

                ByteBuffer bb = retImage.getRawImageDirectBuffer();
                byte barr[] = new byte[xSize*3];
                for ( int i = 0; i < ySize; i++ ) {
                    readBytes(bis, barr);
                    bb.put(barr);
                }

                //-------------------------------------------------------------
                // Invert image
                int x, y;
                RGBPixel pa = new RGBPixel();
                RGBPixel pb = new RGBPixel();

                for ( y = 0; y < ySize/2; y++ ) {
                    for ( x = 0; x < xSize; x++ ) {
                        retImage.getPixelRgb(x, y, pa);
                        retImage.getPixelRgb(x, ySize-y-1, pb);
                        retImage.putPixelRgb(x, y, pb);
                        retImage.putPixelRgb(x, ySize-y-1, pa);
                    }
                }
                //-------------------------------------------------------------

                bis.close();
                fis.close();
                return retImage;
              }
              catch ( Exception e ) {
                  VSDK.reportMessage(null, VSDK.ERROR, "importRGB (B)",
                                     "Cannot import image file \"" + inImageFd.getAbsolutePath() + "\"" + e);
                 throw new ImageNotRecognizedException("Error reading internal file:\n" + e, inImageFd);
            }
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
    public static IndexedColorImage importIndexedColor(File inImageFd) throws ImageNotRecognizedException
    {
        String type = extractExtensionFromFile(inImageFd);
        IndexedColorImage retImage;
        Image img;

        if( type.equals("bw") ) {
            img = ImagePersistenceSGI.readImageSGI(inImageFd.getAbsolutePath());
            if ( img instanceof IndexedColorImage ) {
                retImage = (IndexedColorImage)img;
            }
            else {
                throw new ImageNotRecognizedException("Convertion needed", 
                inImageFd);
            }
            return retImage;
        }
        else if( type.equals("jpg") || type.equals("jpeg") ||
                 type.equals("gif") || type.equals("png") )  {
            if ( awtHelper != null ) {
                return awtHelper.importIndexedColor(inImageFd);
	    }
	    else {
                return createNotAvailableImageIndexedColor();
	    }
        }
        throw new ImageNotRecognizedException("Image not recognized", inImageFd);
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
            dest[bPos]=(byte)(ori[i]);
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
            int x;
            int y;
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
    binary RGB uncompressed BMP format. Returns true if everything
    works fine, false if something fails, like a permission access denied
    or if storage device runs out of space.
    */
    public static boolean exportBMP(File fd, Image img)
    {
        try {
            BufferedOutputStream writer;
            FileOutputStream fos = new FileOutputStream(fd);
            writer = new BufferedOutputStream(fos);

            int y, x;

            //- Write BMP header ----------------------------------------------
            byte magic[] = new byte[2];
            magic[0] = 'B';
            magic[1] = 'M';

            writeBytes(writer, magic);
            writeLongLE(writer, img.getXSize()*img.getYSize()*3 + 54);
            writeLongLE(writer, 0);
            writeLongLE(writer, 54);

            //- Write Windows V3 DIB header -----------------------------------
            writeLongLE(writer, 40);
            writeLongLE(writer, img.getXSize());
            writeLongLE(writer, img.getYSize());
            writeIntLE(writer, 1);
            writeIntLE(writer, 24);
            writeLongLE(writer, 0);
            writeLongLE(writer, 16);
            writeLongLE(writer, 2835);
            writeLongLE(writer, 2835);
            writeLongLE(writer, 0);
            writeLongLE(writer, 0);

            //- Manejo de imagenes nativas de 24 bits por pixel ---------------
            RGBPixel pixel;

            for ( y = img.getYSize() - 1; y >= 0; y-- ) {
                for ( x = 0; x < img.getXSize() ; x++ ) {
                    pixel = img.getPixelRgb(x, y);
                    writer.write(pixel.b);
                    writer.write(pixel.g);
                    writer.write(pixel.r);
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

            int x;
            int y;
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
    
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
