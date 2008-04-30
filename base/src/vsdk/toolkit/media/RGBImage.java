//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 8 2005 - Oscar Chavarro: Original base version                 =
//= - November 28 2005 - Oscar Chavarro: Quality check                      =
//= - March 19 2006 - Oscar Chavarro: VSDK integration                      =
//===========================================================================

package vsdk.toolkit.media;

import vsdk.toolkit.common.VSDK;

public class RGBImage extends Image
{
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20060502L;

    private byte[] data;
    private int xSize;
    private int ySize;

    /**
    Check the general signature contract in superclass method
    Image.init.
    */
    public RGBImage()
    {
        xSize = 0;
        ySize = 0;
        data = null;
    }

    /**
    This is the class destructor.
    */
    public void finalize()
    {
        if ( data != null ) {
            xSize = 0;
            ySize = 0;
            data = null;
        }
    }

    public int getSizeInBytes()
    {
        return xSize*ySize*3 + 2*VSDK.sizeofInt + VSDK.sizeofReference;
    }

    /**
    Image initialize with black background fill.

    Given the desired width and height, this method asigns the needed memory
    to hold such image uncompressed.

    Returns true if memory allocation succeed, false if not.
    */
    public boolean init(int width, int height)
    {
        try {
            data = new byte[width * height * 3];
            for ( int i = 0; i < width*height*3; i++ ) {
                data[i] = 0;
            }
        }
        catch ( Exception e ) {
            data = null;
            return false;
        }
        xSize = width;        
        ySize = height;        
        return true;
    }

    /**
    Image initialize.

    Given the desired width and height, this method asigns the needed memory
    to hold such image uncompressed.

    Returns true if memory allocation succeed, false if not.
    */
    public boolean initNoFill(int width, int height)
    {
        try {
            data = new byte[width * height * 3];
        }
        catch ( Exception e ) {
            data = null;
            return false;
        }
        xSize = width;        
        ySize = height;        
        return true;
    }

    /**
    Este m&eacute;todo cambia la posicion (x, y) de la matriz de imagen y
    escribe en ella un pixel con coordenadas (r, g, b).
    */
    public void putPixel(int x, int y, byte r, byte g, byte b)
    {
        int index = (xSize*(ySize-1-y) + x)*3;
        data[index] = r;
        data[index+1] = g;
        data[index+2] = b;
    }

    public void putPixel(int x, int y, RGBPixel p)
    {
        int index = (xSize*(ySize-1-y) + x)*3;
        data[index] = p.r;
        data[index+1] = p.g;
        data[index+2] = p.b;
    }

    /**
    Check the general signature contract in superclass method
    Image.putPixelRgb.
    */
    public void putPixelRgb(int x, int y, RGBPixel p)
    {
        int index = (xSize*(ySize-1-y) + x)*3;
        data[index] = p.r;
        data[index+1] = p.g;
        data[index+2] = p.b;
    }

    /**
    Este m&eacute;todo retorna las coordenadas de color (r, g, b) para el pixel
    de la posicion (x, y) de la imagen.
    */
    public RGBPixel getPixel(int x, int y)
    {
        RGBPixel p = new RGBPixel();
        int index = (xSize*(ySize-1-y) + x)*3;

        p.r = data[index];
        p.g = data[index+1];
        p.b = data[index+2];

        return p;
    }

    /**
    Check the general signature contract in superclass method
    Image.getPixelRgb.
    */
    public RGBPixel getPixelRgb(int x, int y)
    {
        RGBPixel p = new RGBPixel();
        int index = (xSize*(ySize-1-y) + x)*3;

        p.r = data[index];
        p.g = data[index+1];
        p.b = data[index+2];

        return p;
    }

    /**
    Check the general signature contract in superclass method
    Image.getXSize.
    */
    public int getXSize()
    {
        return xSize;
    }

    /**
    Check the general signature contract in superclass method
    Image.getYSize.
    */
    public int getYSize()
    {
        return ySize;
    }

    public byte[] getRawImage()
    {
        return data;
    }

    public void setRawImage(int xSize, int ySize, byte[] data)
    {
        this.xSize = xSize;
        this.ySize = ySize;
        this.data = data;
    }

    /** Returns a copy of current image in its own memory */
    public RGBImage clone()
    {
        RGBImage copy;
        int xSize = getXSize();
        int ySize = getYSize();
        int x, y;

        copy = new RGBImage();
        copy.init(xSize, ySize);
        for ( x = 0; x < xSize; x++ ) {
            for ( y = 0; y < ySize; y++ ) {
                copy.putPixel(x, y, getPixel(x, y));
            }
        }
        return copy;
    }

    /** Returns a copy of current image in its own memory */
    public RGBAImage cloneToRgba()
    {
        RGBAImage copy;
        int xSize = getXSize();
        int ySize = getYSize();
        int x, y;
        RGBPixel source;
        RGBAPixel target = new RGBAPixel();

        copy = new RGBAImage();
        copy.init(xSize, ySize);
        for ( x = 0; x < xSize; x++ ) {
            for ( y = 0; y < ySize; y++ ) {
                source = getPixel(x, y);
                target.r = source.r;
                target.g = source.g;
                target.b = source.b;
                copy.putPixel(x, y, target);
            }
        }
        return copy;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
