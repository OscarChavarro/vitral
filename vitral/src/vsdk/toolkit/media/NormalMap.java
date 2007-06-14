//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - December 20 2006 - Oscar Chavarro: Original base version              =
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [BLIN1978b] Blinn, James F. "Simulation of wrinkled surfaces", SIGGRAPH =
//=          proceedings, 1978.                                             =
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

    public int getXSize()
    {
        return xSize;
    }

    public int getYSize()
    {
        return ySize;
    }

    public void putNormal(int i, int j, Vector3D n)
    {
        if ( i < 0 || j < 0 || i >= xSize || j >= ySize ) return;
        int index = j * xSize + i;
        Vector3D elem = data.get(index);
    elem.clone(n);
    }

    public Vector3D getNormal(int u, int v)
    {
        if ( u < 0 || v < 0 || u >= xSize || v >= ySize ) return null;
        int index = v * xSize + u;
        return data.get(index);
    }

    /**
    Provide a bilinear interpolation scheme as proposed in [BLIN1978b].
    */
    public Vector3D getNormal(double x, double y)
    {
        //-----------------------------------------------------------------
        double u = x - Math.floor(x);
        double v = y - Math.floor(y);
        double U = u * ((double)(getXSize()-2));
        double V = v * ((double)(getYSize()-2));
        int i = (int)Math.floor(U);
        int j = (int)Math.floor(V);
        double du = U - (double)i;
        double dv = V - (double)j;

        //-----------------------------------------------------------------
        Vector3D F00, F10, F01, F11, FU0, FU1, FVAL;

        F00 = getNormal(i, j);
        F01 = getNormal(i, j+1);
        F10 = getNormal(i+1, j);
        F11 = getNormal(i+1, j+1);

        FU0 = F00.add(F10.substract(F00).multiply(du));
        FU1 = F01.add(F11.substract(F01).multiply(du));
        FVAL = FU0.add(FU1.substract(FU0).multiply(dv));
        return FVAL;
    }

    /**
    @warning This method converts double values to signed bytes. However,
    it is interesting to note that java language aparently makes a weird
    conversion from double to int when casting directly (sometimes
    produces negative integers from positive floats). Note that this
    method checkes this explicity.  Is there a better/clearer way to
    assure the correctness of this conversion?
    */
    public RGBImage exportToRgbImage()
    {
        RGBImage output = new RGBImage();

        if ( !output.init(xSize, ySize) ) {
            return null;
        }

        int x, y;
        Vector3D n;
        byte r, g, b;
    int rr, gg, bb;

        for ( y = 0; y < ySize; y++ ) {
            for ( x = 0; x < xSize; x++ ) {
                n = getNormal(x, y);
                n.normalize();

        rr = (int)(n.x * 255.0);
        gg = (int)(n.y * 255.0);
        bb = (int)(n.z * 255.0);
        if ( rr < 0 ) rr += 256;
        if ( gg < 0 ) gg += 256;
        if ( bb < 0 ) bb += 256;
                r = VSDK.unsigned8BitInteger2signedByte(rr);
                g = VSDK.unsigned8BitInteger2signedByte(gg);
                b = VSDK.unsigned8BitInteger2signedByte(bb);

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

        //- 1. Si el vector de escala dado es erroneo, crear uno base -------
        if( inOutScale.x < VSDK.EPSILON || inOutScale.y < VSDK.EPSILON ||
            inOutScale.z < VSDK.EPSILON ) {
            double val = ((double)xSize) / ((double)ySize);
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
        int u, v;

        for( u = 1; u < xSize - 1; u++ ) {
            for( v = 1; v < ySize - 1; v++ ) {
                a = inBumpmap.getPixel(u+1, v);
                b = inBumpmap.getPixel(u-1, v);
                c = inBumpmap.getPixel(u, v+1);
                d = inBumpmap.getPixel(u, v-1);

                df_du.x = 2;
                df_du.y = 0;
                df_du.z = ( (double)(a - b) ) / 255.0;

                df_dv.x = 0;
                df_dv.y = 2;
                df_dv.z = ( (double)(d - c) ) / 255.0;

                normal = df_du.crossProduct(df_dv);

                // Modular el vector `normal` respecto al vector `inOutScale`
                normal.x *= inOutScale.x;
                normal.y *= inOutScale.y;
                normal.z *= inOutScale.z;
                normal.normalize();

                putNormal(u, v, normal);
            }
        }

        //- 3. Copia de las derivadas para los bordes de la imagen ----------
        for( u = 0; u < xSize; u++ ) {
            putNormal(u, 0, getNormal(u, 1));
            putNormal(u, ySize-1, getNormal(u, ySize-2));
        }
        for( v = 0; v < ySize; v++ ) {
            putNormal(0, v, getNormal(1, v));
            putNormal(xSize-1, v, getNormal(xSize-2, v));
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
