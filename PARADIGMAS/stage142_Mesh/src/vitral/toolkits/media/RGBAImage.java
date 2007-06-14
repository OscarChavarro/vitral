//===========================================================================
package vitral.toolkits.media;

import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

import vitral.toolkits.media.RGBAPixel;

public class RGBAImage 
{

    private RGBAPixel data[];
    private int x_tam;
    private int y_tam;
    private int pixelDepth;

    /**
    Constructora de imagen. OJO: No la inicializa, antes de usarla debe
    llamarse el m&eacute;todo init.
    */
    public RGBAImage()
    {
        x_tam = 0;
        y_tam = 0;
        pixelDepth=24;
        data = null;
    }

    /**
    Esta es la destructora de la clase.  Si es usted tan gentil, por favor 
    llame este metodo al final del alcance de su objeto.
    */
    public void dispose()
    {
        if ( data != null ) {
            for ( int i = 0; i < x_tam*y_tam; i++ ) {
                if ( data[i] != null ) {
                    data[i] = null;
                }
            }
            x_tam = 0;
            y_tam = 0;
            pixelDepth=24;
            data = null;
            System.gc();
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
    public boolean init(int ancho, int alto)
    {
        try {
          data = new RGBAPixel[ancho * alto];
          for ( int i = 0; i < ancho*alto; i++ ) {
              data[i] = new RGBAPixel();
          }
        }
        catch (Exception e) {
          data = null;
          return false;
        }
        x_tam = ancho;        
        y_tam = alto;        
        return true;
    }

    /**
    Este m&eacute;todo cambia la posicion (x, y) de la matriz de imagen y
    escribe en ella un pixel con coordenadas (r, g, b).
    */
    public void putpixel(int x, int y, byte r, byte g, byte b)
    {
        int index = x_tam*y + x;
        data[index].r = r;
        data[index].g = g;
        data[index].b = b;
    }
    
    public void putPixel(int x, int y, byte r, byte g, byte b, byte a)
    {
        int index = x_tam*y + x;
        data[index].r = r;
        data[index].g = g;
        data[index].b = b;
        data[index].a = a;
    }

    /**
    Este m&eacute;todo retorna las coordenadas de color (r, g, b) para el pixel
    de la posicion (x, y) de la imagen.
    */
    public RGBAPixel getpixel(int x, int y)
    {
        RGBAPixel p = new RGBAPixel();
        int index = x_tam*y + x;
        p.r = data[index].r;
        p.g = data[index].g;
        p.b = data[index].b;
        p.a = data[index].a;

        return new RGBAPixel();
    }

    /**
    Este m&eacute;todo genera un patron de prueba visual tipo ajedrez, con
    un par de lineas de colores atravezandolo por el centro.  Puede utilizarse
    para probar otras operaciones de imagenes, o para inicializarlas.
    */
    public void crear_patron_de_prueba()
    {
        int i;
        int j;
        byte r;
        byte g;
        byte b;

        for ( i = 0; i < y_tam ; i++ ) {
            for ( j = 0; j < x_tam ; j++ ) {
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
                if ( i == y_tam/2 ) {
                    r = (byte)255;
                    g = 0;
                    b = 0;
                }
                if ( j == x_tam/2) {
                    r = 0;
                    g = (byte)255;
                    b = 0;
                }
                putpixel(j, i, r, g, b);
            }
        }
    }

    /**
    Este m&eacute;todo escribe los contenidos de la imagen actual en un
    archivo de imagen en formato PPM RGB (i.e. P6). Retorna true si todo
    sale bien o false si algo falla (i.e. como un problema de permisos o
    que se acabe el espacio en el dispositivo de almacenamiento `fd`).
    */
    public boolean exportar_ppm(File fd)
    {
        try {
            FileOutputStream escritor = new FileOutputStream(fd);

            String linea1 = "P6\n";
            String linea2 = x_tam + " " + y_tam + "\n";
            String linea3 = "255\n";
            byte arr[];

            arr = linea1.getBytes();
            escritor.write(arr, 0, arr.length);
            arr = linea2.getBytes();
            escritor.write(arr, 0, arr.length);
            arr = linea3.getBytes();
            escritor.write(arr, 0, arr.length);

            for ( int i = 0; i < x_tam*y_tam; i++ ) {
                escritor.write(data[i].r);
                escritor.write(data[i].g);
                escritor.write(data[i].b);
            }
        }
        catch (Exception e) {
            return false;
        }
        return true;
    }

    public int xtam()
    {
        return x_tam;
    }

    public int ytam()
    {
        return y_tam;
    }
    
    public void setPixelDepth(int pixelDepth)
    {
        this.pixelDepth=pixelDepth;
    }
    
    public int getPixelDepth()
    {
        return this.pixelDepth;
    }
    
    public void loadImage(byte[] d, int pixelDepth, int xTam, int yTam)
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
        byte[] pixels=new byte[this.x_tam*this.y_tam*this.pixelDepth];
        
        int dPos=0;
        for(int i=0; i<this.data.length; i++)
        {
            pixels[dPos]=this.data[i].r;
            dPos++;
            pixels[dPos]=this.data[i].g;
            dPos++;
            pixels[dPos]=this.data[i].b;
            dPos++;
            if(pixelDepth==32)
            {
                pixels[dPos]=this.data[i].a;
                dPos++;
            }
        }
        return pixels;
    }
    
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
