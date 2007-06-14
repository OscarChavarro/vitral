//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - December 20 2006 - Oscar Chavarro: Original base version              =
//===========================================================================

package vsdk.toolkit.media;

import java.util.ArrayList;

import vsdk.toolkit.common.Entity;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.media.IndexedColorImage;
import vsdk.toolkit.media.RGBImage;

public class NormalMap extends Entity
{
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20061220L;

    private int xSize;
    private int ySize;
    private ArrayList<Vector3D> data;

    public NormalMap()
    {
        xSize = 0;
        ySize = 0;
        data = null;
    }

    public boolean init(int width, int height)
    {
        try {
            data = new ArrayList<Vector3D>();
            for ( int i = 0; i < width*height; i++ ) {
                data.add(new Vector3D());
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

    public void putNormal(int i, int j, Vector3D n)
    {
        if ( i < 0 || j < 0 || i >= xSize || j >= ySize ) return;
        int index = j * xSize + i;
        Vector3D elem = data.get(i);
        elem.x = n.x;
        elem.y = n.y;
        elem.z = n.z;
    }

    public Vector3D getNormal(int i, int j)
    {
        if ( i < 0 || j < 0 || i >= xSize || j >= ySize ) return null;
        int index = j * xSize + i;
        return data.get(i);
    }

    public RGBImage exportToRgbImage()
    {
        RGBImage output = new RGBImage();

        if ( !output.init(xSize, ySize) ) {
            return output;
        }

        int x, y;
        Vector3D n;
        byte r, g, b;

        for ( y = 0; y < ySize; y++ ) {
            for ( x = 0; x < xSize; x++ ) {
                n = getNormal(x, y);
                n.normalize();
                r = VSDK.unsigned8BitInteger2signedByte((int)(n.x * 255.0));
                g = VSDK.unsigned8BitInteger2signedByte((int)(n.y * 255.0));
                b = VSDK.unsigned8BitInteger2signedByte((int)(n.z * 255.0));
                output.putPixel(x, y, r, g, b);
            }
        }
        return output;
    }

    public void importBumpMap(IndexedColorImage inBumpmap, Vector3D inOutScale)
    {
        //-------------------------------------------------------------------
        int xSize = inBumpmap.getXSize();
        int ySize = inBumpmap.getYSize();
        int i, j;

        //- 1. Si el vector de escala dado es erroneo, crear uno base -------
        if( inOutScale.x < VSDK.EPSILON || inOutScale.y < VSDK.EPSILON ||
            inOutScale.z < VSDK.EPSILON ) {
            double val = ((double)(xSize)) / ((double)(ySize));
            if( val < 1.0 ) {
                inOutScale.x = 1.0;
                inOutScale.y = 1.0 / val;
            }
            else {
                inOutScale.x = val;
                inOutScale.y = 1.0;
            }
            inOutScale.z = 1.0;
        }
        init(xSize, ySize);

        //- 2. Calculo de las derivadas parciales al interior de la imagen --
        Vector3D df_du = new Vector3D();
        Vector3D df_dv = new Vector3D();
        Vector3D normal;
        int a, b, c, d;

        for( i = 1; i < xSize - 1; i++ ) {
            for( j = 1; j < ySize - 1; j++ ) {
                a = inBumpmap.getPixel(i+1, j);
                b = inBumpmap.getPixel(i-1, j);
                c = inBumpmap.getPixel(i, j+1);
                d = inBumpmap.getPixel(i, j-1);

                df_du.x = 2;
                df_du.y = 0;
                df_du.z = ( ((double)a) - ((double)b) ) / 255.0;

                df_dv.x = 0;
                df_dv.y = 2;
                df_dv.z =( (double)c - (double)d )/255.0;

                normal = df_du.crossProduct(df_dv);

                // Modular el vector `normal` respecto al vector `*in_Escala`
                normal.x *= inOutScale.x;
                normal.y *= inOutScale.y;
                normal.z *= inOutScale.z;
                normal.normalize();

                putNormal(i, ySize - 1 - j, normal);
            }
        }

        //- 3. Copia de las derivadas para los bordes de la imagen ----------
        for( i = 0; i < xSize; i++ ) {
            putNormal(i, 0, getNormal(i, 1));
            putNormal(i, ySize-1, getNormal(i, ySize-2));
        }
        for( j = 0; j < ySize; j++ ) {
            putNormal(0, j, getNormal(1, j));
            putNormal(xSize-1, j, getNormal(xSize-2, j));
        }

    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
