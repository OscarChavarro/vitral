//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - September 15 2005 - Oscar Chavarro: Original base version             =
//= - November 28 2005 - Oscar Chavarro: Quality check                      =
//===========================================================================

package vsdk.toolkit.media;

import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

public class RGBAImage 
{
    private RGBAPixel data[];
    private int xSize;
    private int ySize;

    private int pixelDepth;

    /**
    Constructora de imagen. OJO: No la inicializa, antes de usarla debe
    llamarse el m&eacute;todo init.
    */
    public RGBAImage()
    {
        xSize = 0;
        ySize = 0;
        data = null;

        pixelDepth=24;
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

            pixelDepth=24;
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
          data = new RGBAPixel[width * height];
          for ( int i = 0; i < width*height; i++ ) {
              data[i] = new RGBAPixel();
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
        data[index].a = 0;
    }
    
    public void putPixel(int x, int y, byte r, byte g, byte b, byte a)
    {
        int index = xSize*y + x;
        data[index].r = r;
        data[index].g = g;
        data[index].b = b;
        data[index].a = a;
    }

    public void putPixel(int x, int y, RGBAPixel p)
    {
        int index = xSize*y + x;
        data[index].r = p.r;
        data[index].g = p.g;
        data[index].b = p.b;
        data[index].a = p.a;
    }

    /**
    Este m&eacute;todo retorna las coordenadas de color (r, g, b, a) para el 
    pixel de la posicion (x, y) de la imagen.
    */
    public RGBAPixel getPixel(int x, int y)
    {
        RGBAPixel p = new RGBAPixel();
        int index = xSize*y + x;

        p.r = data[index].r;
        p.g = data[index].g;
        p.b = data[index].b;
        p.a = data[index].a;

        return p;
    }

    /**
    Este m&eacute;todo genera un patron de prueba visual tipo ajedrez, con
    un par de lineas de colores atravezandolo por el centro.  Puede utilizarse
    para probar otras operaciones de imagenes, o para inicializarlas.
    */
    public void createTestPattern()
    {
        int i;
        int j;
        byte r;
        byte g;
        byte b;

        for ( i = 0; i < ySize ; i++ ) {
            for ( j = 0; j < xSize ; j++ ) {
                if ( ((i % 2 != 0) && (j % 2 == 0)) || 
                     ((j % 2 != 0) && (i % 2 == 0)) ) {
                    r = (byte)255;
                    g = (byte)255;
                    b = (byte)255;
                  }
                  else {
                    r = 0;
                    g = 0;
                    b = 0;
                }
                if ( i == ySize/2 ) {
                    r = (byte)255;
                    g = 0;
                    b = 0;
                }
                if ( j == xSize/2) {
                    r = 0;
                    g = (byte)255;
                    b = 0;
                }
                putPixel(j, i, r, g, b);
            }
        }
    }

    /**
    Este m&eacute;todo escribe los contenidos de la imagen actual en un
    archivo de imagen en formato PPM RGB (i.e. P6). Retorna true si todo
    sale bien o false si algo falla (i.e. como un problema de permisos o
    que se acabe el espacio en el dispositivo de almacenamiento `fd`).
    */
    public boolean exportPPM(File fd)
    {
        try {
            BufferedOutputStream escritor;

            escritor = new BufferedOutputStream(new FileOutputStream(fd));

            String linea1 = "P6\n";
            String linea2 = xSize + " " + ySize + "\n";
            String linea3 = "255\n";
            byte arr[];

            arr = linea1.getBytes();
            escritor.write(arr, 0, arr.length);
            arr = linea2.getBytes();
            escritor.write(arr, 0, arr.length);
            arr = linea3.getBytes();
            escritor.write(arr, 0, arr.length);

            for ( int i = 0; i < xSize*ySize; i++ ) {
                escritor.write(data[i].r);
                escritor.write(data[i].g);
                escritor.write(data[i].b);
            }
            escritor.close();
        }
        catch (Exception e) {
            return false;
        }
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
    
    public void setPixelDepth(int pixelDepth)
    {
        this.pixelDepth=pixelDepth;
    }
    
    public int getPixelDepth()
    {
        return this.pixelDepth;
    }
    
    /** ?? */
    public void setRawImage(byte[] d, int pixelDepth, int xTam, int yTam)
    {
        this.init(xTam, yTam);
        this.pixelDepth=pixelDepth;
        int dPos=0;
        for(int i=0; i<this.data.length; i++)
        {
            this.data[i].r=d[dPos];
            dPos++;
            this.data[i].g=d[dPos];
            dPos++;
            this.data[i].b=d[dPos];
            dPos++;
            if(pixelDepth==32)
            {
                this.data[i].a=d[dPos];
                dPos++;
            }
        }
    }

    public byte[] getRawImage()
    {
        byte[] pixels=new byte[this.xSize*this.ySize*4];

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
                if ( pixelDepth == 32 ) {
                    pixels[acum]=this.data[i].a;
                    acum++;
                  }
                  else {
                    pixels[acum]=0;
                    acum++;
                }
            }
        }
        return pixels;
    }
    
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
