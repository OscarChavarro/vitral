//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 21 2006 - Oscar Chavarro: Original base version                =
//===========================================================================

package vsdk.toolkit.media;

import vsdk.toolkit.common.VSDK;

public class IndexedColorImage extends Image
{
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20060821L;

    private byte data[];
    private int xSize;
    private int ySize;

    /**
    Check the general signature contract in superclass method
    Image.init.
    */
    public IndexedColorImage()
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
            System.gc();
        }
    }

    public boolean init(int width, int height)
    {
        try {
          data = new byte[width * height];
          for ( int i = 0; i < width*height; i++ ) {
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

    public int getXSize()
    {
        return xSize;
    }

    public int getYSize()
    {
        return ySize;
    }

    public void putPixel(int x, int y, byte val)
    {
        int index = xSize*y + x;
        data[index] = val;
    }

    public void putPixel(int x, int y, int val)
    {
        int index = xSize*y + x;
        data[index] = VSDK.unsigned8BitInteger2signedByte(val);
    }

    public int getPixel(int x, int y)
    {
        int index = xSize*y + x;
        return VSDK.signedByte2unsignedInteger(data[index]);
    }

    public RGBPixel getPixelRgb(int x, int y)
    {
        VSDK.reportMessage(this, VSDK.FATAL_ERROR, "getPixelRgb",
        "Method not implemented");
        return null;
    }

    public void putPixelRgb(int x, int y, RGBPixel p)
    {
        VSDK.reportMessage(this, VSDK.FATAL_ERROR, "putPixelRgb",
        "Method not implemented");
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
