//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 14 2006 - Fabio Aroca & Eduardo Mendoza: Original base version  =
//= - March 14 2006 - Oscar Chavarro: quality check                         =
//===========================================================================

package vsdk.toolkit.media;

import java.util.ArrayList;

import vsdk.toolkit.common.Entity;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.media.RGBColorPalette;

public class ZBuffer extends Entity {
    private float[] depth;
    private int xSize;
    private int ySize;

    public ZBuffer(int width, int height) {
        xSize = width;
        ySize = height;
        depth=new float[xSize*ySize];
    }

    public ZBuffer(float[] dep, int width, int height) {
        xSize = width;
        ySize = height;
        depth = new float[width*height];

        int pos = 0;
        for (int y = ySize - 1; y >= 0; y--) {
            for (int x = 0; x < xSize; x++) {
                depth[xSize*y+x] = dep[pos];
                pos++;
            }
        }
    }

    public int getXSize()
    {
        return xSize;
    }

    public int getYSize()
    {
        return ySize;
    }

    public float[] getZBuffer() {
        return depth;
    }

    public float getZ(int x, int y) {
        int pos = (xSize * y) + x;
        return depth[pos];
    }

    public void setZBuffer(float[] dep) {
        depth = new float[dep.length];
        for (int i = 0; i < dep.length; i++) {
            depth[i] = dep[i];
        }
    }

    public void setZ(int x, int y, float v) {
        int pos = (xSize * y) + x;
        depth[pos] = v;
    }

    public RGBImage exportRGBImage(RGBColorPalette p) {
        RGBImage image = new RGBImage();
        image.init(xSize, ySize);
        int pos = 0;

        ColorRgb c;

        for (int y = 0; y <image.getYSize(); y++) {
            for (int x = 0; x < image.getXSize(); x++) {
                float f = depth[pos];
                if ( f < 0.0 ) f = 0.0f;
                if ( f > 1.0 ) f = 1.0f;
                c = p.evalLinear(f);
                image.putPixel(x, y, 
                    (byte)(c.r*256), (byte)(c.g*256), (byte)(c.b*256));
                pos++;
            }
        }
        return image;
    }

    public RGBAImage exportRGBAImage(RGBColorPalette p) {
        RGBAImage image = new RGBAImage();
        image.init(xSize, ySize);
        int pos = 0;

        ColorRgb c;

        for (int y = 0; y <image.getYSize(); y++) {
            for (int x = 0; x < image.getXSize(); x++) {
                float f = depth[pos];
                if ( f < 0.0 ) f = 0.0f;
                if ( f > 1.0 ) f = 1.0f;
                c = p.evalLinear(f);
                image.putPixel(x, y, 
                    (byte)(c.r*256), (byte)(c.g*256), (byte)(c.b*256));
                pos++;
            }
        }
        return image;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
