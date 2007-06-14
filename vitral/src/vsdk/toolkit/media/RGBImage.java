//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 8 2005 - Oscar Chavarro: Original base version                 =
//= - November 28 2005 - Oscar Chavarro: Quality check                      =
//= - March 19 2006 - Oscar Chavarro: VSDK integration                      =
//===========================================================================

package vsdk.toolkit.media;

public class RGBImage extends Image
{
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20060502L;

    private RGBPixel data[];
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
    Esta es la destructora de la clase.  Si es usted tan gentil, por favor 
    llame este metodo al final del alcance de su objeto.
    */
    public void dispose()
    {
        if ( data != null ) {
            for ( int i = 0; i < xSize*ySize; i++ ) {
                if ( data[i] != null ) {
                    data[i] = null;
                }
            }
            xSize = 0;
            ySize = 0;
            data = null;
            System.gc();
        }
    }

    /**
    Inicializaci&oacute;n del contenido de una imagen.

    Recibe el width y el height que debe tener esta RGBImage (n&oacute;tese
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
          data = new RGBPixel[width * height];
          for ( int i = 0; i < width*height; i++ ) {
              data[i] = new RGBPixel();
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
        int index = xSize*y + x;
        data[index].r = r;
        data[index].g = g;
        data[index].b = b;
    }

    public void putPixel(int x, int y, RGBPixel p)
    {
        int index = xSize*y + x;
        data[index].r = p.r;
        data[index].g = p.g;
        data[index].b = p.b;
    }

    /**
    Check the general signature contract in superclass method
    Image.putPixelRgb.
    */
    public void putPixelRgb(int x, int y, RGBPixel p)
    {
        int index = xSize*y + x;
        data[index].r = p.r;
        data[index].g = p.g;
        data[index].b = p.b;
    }

    /**
    Este m&eacute;todo retorna las coordenadas de color (r, g, b) para el pixel
    de la posicion (x, y) de la imagen.
    */
    public RGBPixel getPixel(int x, int y)
    {
        RGBPixel p = new RGBPixel();
        int index = xSize*y + x;

        p.r = data[index].r;
        p.g = data[index].g;
        p.b = data[index].b;

        return p;
    }

    /**
    Check the general signature contract in superclass method
    Image.getPixelRgb.
    */
    public RGBPixel getPixelRgb(int x, int y)
    {
        RGBPixel p = new RGBPixel();
        int index = xSize*y + x;

        p.r = data[index].r;
        p.g = data[index].g;
        p.b = data[index].b;

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
        byte[] pixels=new byte[this.xSize*this.ySize*3];

        int i, j;
        int acum = 0;
        for ( i = ySize-1; i >= 0 ; i-- ) {
            for ( j = 0; j < xSize ; j++ ) {
                pixels[acum]=this.data[i*xSize+j].r;
                acum++;
                pixels[acum]=this.data[i*xSize+j].g;
                acum++;
                pixels[acum]=this.data[i*xSize+j].b;
                acum++;
            }
        }
        return pixels;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
