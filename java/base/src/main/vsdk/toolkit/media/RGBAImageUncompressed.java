package vsdk.toolkit.media;
import java.io.Serial;

import java.nio.ByteBuffer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import vsdk.toolkit.io.PersistenceElement;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.logging.Logger;

/**
Current class is an specific low level implementation of an uncompressed
32 bits per pixel RGBA image over a byte array (ordered in a sequential array
of RGBA bytes, row by row from upper left pixel, and left to right on each
row).

Note that this class implements two version of vector access operations:
one simple basic one, and one optimized for using Java's "Direct Buffers".

If Java would have C/C++ - like preprocessor directives, the two
implementations could be selected using conditional compilation. As
conditional compilation is not supported on Java, manual comments are
provided. It was choosen not to use hierarchy to keep a simple class
easy to understand at a design level, and to use it for teaching purposes.
Another reason for using this "comment-based conditional compilation" is
to keep this class conceptually consistent with non-java VSDK realizations
(particulary the corresponding C++ AQUYNZA class).
*/
public class RGBAImageUncompressed extends Image
{
    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    @Serial private static final long serialVersionUID = 20060502L;

    transient private ByteBuffer data;

    private int xSize;
    private int ySize;

    /**
    Check the general signature contract in superclass method
    Image.init.
    */
    public RGBAImageUncompressed()
    {
        xSize = 0;
        ySize = 0;

        data = null;
    }

    /**
    Experimental method. Used for rendering-only applications that has
    transfered image contents to a JOGL context (GPU's Video memory) */
    public void dettach() {
        if ( data != null ) {
            data = null;
        }
    }

    @Override
    public int getSizeInBytes()
    {
        // Warning: it is not taking into account the internal occupancy of the
        // ByteBuffer
        return xSize*ySize*4 + 2*INT_SIZE_IN_BYTES + POINTER_SIZE_IN_BYTES;
    }

    /**
    Image initialize with black background fill.

    Given the desired width and height, this method asigns the needed memory
    to hold such image uncompressed.

    Returns true if memory allocation succeed, false if not.
    @return 
    */
    @Override
    public boolean init(int width, int height)
    {
        try {
          data = ByteBuffer.allocateDirect(width * height * 4);
          data.rewind();
          for ( int i = 0; i < width*height*4; i++ ) {
              data.put((byte)0);
          }
        }
        catch (Exception e) {
          data = null;
          return false;
        }
        xSize = width;        
        ySize = height;        
        return true;
    }

    /**
    Image initialize with black background fill.

    Given the desired width and height, this method asigns the needed memory
    to hold such image uncompressed.

    Returns true if memory allocation succeed, false if not.
    @return 
    */
    @Override
    public boolean initNoFill(int width, int height)
    {
        if ( data != null && width == xSize && height == ySize ) {
            data.rewind();
            return true;
        }

        try {
          data = ByteBuffer.allocateDirect(width * height * 4);
          data.rewind();
        }
        catch (Exception e) {
          data = null;
          return false;
        }
        xSize = width;        
        ySize = height;        
        return true;
    }

    /**
    This method changes the pixel information for pixel (x, y) on the
    represented image matrix, to contain the values <r, g, b, -1>
    (fully opaque pixel).
    @param x
    @param y
    @param r
    @param g
    @param b
    */
    public void putPixel(int x, int y, byte r, byte g, byte b)
    {
        int index = ((xSize*(ySize-1-y)) + x)*4;

        data.put(index, r);
        data.put(index+1, g);
        data.put(index+2, b);
        data.put(index+3, (byte)-1);
    }
    
    public void putPixel(int x, int y, byte r, byte g, byte b, byte a)
    {
        int index = ((xSize*(ySize-1-y)) + x)*4;

        data.put(index, r);
        data.put(index+1, g);
        data.put(index+2, b);
        data.put(index+3, a);
    }

    public void putPixel(int x, int y, RGBAPixel p)
    {
        int index = ((xSize*(ySize-1-y)) + x)*4;
        data.put(index, p.r);
        data.put(index+1, p.g);
        data.put(index+2, p.b);
        data.put(index+3, p.a);
    }

    /**
    Check the general signature contract in superclass method
    Image.putPixelRgb.
    */
    @Override
    public void putPixelRgb(int x, int y, RGBPixel p)
    {
        int index = ((xSize*(ySize-1-y)) + x)*4;

        data.put(index, p.r);
        data.put(index+1, p.g);
        data.put(index+2, p.b);
        data.put(index+3, Byte.MAX_VALUE);
    }

    /**
    This method returns the color component <r, g, b, a> contained on the pixel
    <x, y> of current image.
    @param x
    @param y
    @return 
    */
    public RGBAPixel getPixel(int x, int y)
    {
        RGBAPixel p = new RGBAPixel();
        int index = ((xSize*(ySize-1-y)) + x)*4;

        p.r = data.get(index);
        p.g = data.get(index+1);
        p.b = data.get(index+2);
        p.a = data.get(index+3);

        return p;
    }

    /**
    Check the general signature contract in superclass method
    Image.getPixelRgb.
    @return 
    */
    @Override
    public RGBPixel getPixelRgb(int x, int y)
    {
        RGBPixel p = new RGBPixel();
        int index = ((xSize*(ySize-1-y)) + x)*4;

        p.r = data.get(index);
        p.g = data.get(index+1);
        p.b = data.get(index+2);

        return p;
    }

    public void getPixelRgba(int x, int y, RGBAPixel p)
    {
        int index = ((xSize*(ySize-1-y)) + x)*4;

        p.r = data.get(index);
        p.g = data.get(index+1);
        p.b = data.get(index+2);
        p.a = data.get(index+3);
    }

    /**
    Check the general signature contract in superclass method
    Image.getPixelRgb.
    */
    @Override
    public void getPixelRgb(int x, int y, RGBPixel p)
    {
        int index = ((xSize*(ySize-1-y)) + x)*4;

        p.r = data.get(index);
        p.g = data.get(index+1);
        p.b = data.get(index+2);
    }

    /**
    Check the general signature contract in superclass method
    Image.getXSize.
    @return 
    */
    @Override
    public int getXSize()
    {
        return xSize;
    }

    /**
    Check the general signature contract in superclass method
    Image.getYSize.
    @return 
    */
    @Override
    public int getYSize()
    {
        return ySize;
    }
    
    public byte[] getRawImage()
    {

        if ( !data.hasArray() ) {
            Logger.reportMessage(this, VSDK.FATAL_ERROR, "getRawImage", "cannot return raw bytes for a direct buffer optimized image, use getRawImageDirectBuffer instead.");
        }
        return data.array();
    }

    public ByteBuffer getRawImageDirectBuffer()
    {
        data.rewind();
        return data;
    }

    public void setRawImage(int xSize, int ySize, byte[] data)
    {
        this.xSize = xSize;
        this.ySize = ySize;

        Logger.reportMessage(this, VSDK.FATAL_ERROR, "setRawImage", "NOT IMPLEMENTED! CHECK VSDK CODE!");
    }
    
    /** 
    Returns a copy of current image in its own memory 
     * @return 
     * @throws java.lang.CloneNotSupportedException
    */
    @Override
    public RGBAImageUncompressed clone() throws CloneNotSupportedException
    {
        super.clone();
        RGBAImageUncompressed copy;
        int xxSize = getXSize();
        int yySize = getYSize();
        int x, y;

        copy = new RGBAImageUncompressed();
        copy.init(xxSize, yySize);
        for ( x = 0; x < xxSize; x++ ) {
            for ( y = 0; y < yySize; y++ ) {
                copy.putPixel(x, y, getPixel(x, y));
            }
        }
        return copy;
    }

    /** 
    Returns a copy of current image in its own memory 
     * @return 
    */
    public RGBImageUncompressed exportToRgbImage()
    {
        RGBImageUncompressed copy;
        int xxSize = getXSize();
        int yySize = getYSize();
        int x, y;
        RGBAPixel source;
        RGBPixel target = new RGBPixel();

        copy = new RGBImageUncompressed();
        copy.init(xxSize, yySize);
        for ( x = 0; x < xxSize; x++ ) {
            for ( y = 0; y < yySize; y++ ) {
                source = getPixel(x, y);
                target.r = source.r;
                target.g = source.g;
                target.b = source.b;
                copy.putPixel(x, y, target);
            }
        }
        return copy;
    }

    private void writeObject(ObjectOutputStream out) throws IOException
    {
        try {
            int x, y;

            PersistenceElement.writeSignedShortBE(out, xSize);
            PersistenceElement.writeSignedShortBE(out, ySize);
            byte arr[] = new byte[4];

            data.rewind();
            for ( y = 0; y < ySize; y++ ) {
                for ( x = 0; x < xSize; x++ ) {
                    arr[0] = data.get();
                    arr[1] = data.get();
                    arr[2] = data.get();
                    arr[3] = data.get();
                    PersistenceElement.writeBytes(out, arr);
                }
            }
        }
        catch ( Exception e ) {
            throw new IOException("Error in custom RGBAImageUncompressed writeObject");
        }
    }

    private void readObject(ObjectInputStream in) throws Exception
    {
        int x, y;

        xSize = PersistenceElement.readSignedShortBE(in);
        ySize = PersistenceElement.readSignedShortBE(in);

        initNoFill(xSize, ySize);
        data.rewind();

        byte arr[] = new byte[4];
        for ( y = 0; y < ySize; y++ ) {
            for ( x = 0; x < xSize; x++ ) {
                PersistenceElement.readBytes(in, arr);
                data.put(arr[0]);
                data.put(arr[1]);
                data.put(arr[2]);
                data.put(arr[3]);
            }
        }
    }
}
