//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - September 15 2005 - Oscar Chavarro: Original base version             =
//= - November 28 2005 - Oscar Chavarro: Quality check                      =
//===========================================================================

package vsdk.toolkit.media;

public class RGBAImage extends Image
{
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20060502L;

    private byte data[];
    private int xSize;
    private int ySize;

    /**
    Check the general signature contract in superclass method
    Image.init.
    */
    public RGBAImage()
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

    /**
    Inicializaci&oacute;n del contenido de una imagen.

    Recibe el ancho y el alto que debe tener esta RGBImage (n&oacute;tese
    que una imagen puede asi cambiar de tama&ntilde;o en cualquier momento)
    y asigna la memoria necesaria para hacerlo.

    OJO: NO inicializa la imagen, solo asigna la memoria necesaria.

    Este m&eacute;todo retorna true si todo sale bien, o false si no
    se pudo asignar la cantidad de memoria necesaria para almacenar la
    imagen del tama&ntilde;o seleccionado.
    */
    public boolean init(int width, int height)
    {
        try {
          data = new byte[width * height * 4];
          for ( int i = 0; i < width*height*4; i++ ) {
              data[i] = 0;
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
    Este m&eacute;todo cambia la posicion (x, y) de la matriz de imagen y
    escribe en ella un pixel con coordenadas (r, g, b).
    */
    public void putPixel(int x, int y, byte r, byte g, byte b)
    {
        int index = ((xSize*(ySize-1-y)) + x)*4;
        data[index] = r;
        data[index+1] = g;
        data[index+2] = b;
        data[index+3] = 0;
    }
    
    public void putPixel(int x, int y, byte r, byte g, byte b, byte a)
    {
        int index = ((xSize*(ySize-1-y)) + x)*4;
        data[index] = r;
        data[index+1] = g;
        data[index+2] = b;
        data[index+3] = a;
    }

    public void putPixel(int x, int y, RGBAPixel p)
    {
        int index = ((xSize*(ySize-1-y)) + x)*4;
        data[index] = p.r;
        data[index+1] = p.g;
        data[index+2] = p.b;
        data[index+3] = p.a;
    }

    /**
    Check the general signature contract in superclass method
    Image.putPixelRgb.
    */
    public void putPixelRgb(int x, int y, RGBPixel p)
    {
        int index = ((xSize*(ySize-1-y)) + x)*4;
        data[index] = p.r;
        data[index+1] = p.g;
        data[index+2] = p.b;
        data[index+3] = Byte.MAX_VALUE;
    }

    /**
    Este m&eacute;todo retorna las coordenadas de color (r, g, b, a) para el 
    pixel de la posicion (x, y) de la imagen.
    */
    public RGBAPixel getPixel(int x, int y)
    {
        RGBAPixel p = new RGBAPixel();
        int index = ((xSize*(ySize-1-y)) + x)*4;

        p.r = data[index];
        p.g = data[index+1];
        p.b = data[index+2];
        p.a = data[index+3];

        return p;
    }

    /**
    Check the general signature contract in superclass method
    Image.getPixelRgb.
    */
    public RGBPixel getPixelRgb(int x, int y)
    {
        RGBPixel p = new RGBPixel();
        int index = ((xSize*(ySize-1-y)) + x)*4;

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
    public RGBAImage clone()
    {
        RGBAImage copy;
    int xSize = getXSize();
    int ySize = getYSize();
    int x, y;

        copy = new RGBAImage();
    copy.init(xSize, ySize);
    for ( x = 0; x < xSize; x++ ) {
          for ( y = 0; y < ySize; y++ ) {
        copy.putPixel(x, y, getPixel(x, y));
        }
    }
    return copy;
    }

    /** Returns a copy of current image in its own memory */
    public RGBImage exportToRgbImage()
    {
        RGBImage copy;
    int xSize = getXSize();
    int ySize = getYSize();
    int x, y;
    RGBAPixel source;
    RGBPixel target = new RGBPixel();

        copy = new RGBImage();
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
