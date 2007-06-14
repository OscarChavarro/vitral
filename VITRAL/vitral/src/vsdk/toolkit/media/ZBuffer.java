//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 14 2006 - Fabio Aroca & Eduardo Mendoza: Original base version  =
//= - March 14 2006 - Oscar Chavarro: quality check                         =
//===========================================================================

package vsdk.toolkit.media;

import java.util.ArrayList;
import vsdk.toolkit.media.RGBAImage;

public class ZBuffer {
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

    public RGBAImage exportRGBAImage() {
        RGBAImage image = new RGBAImage();
        image.init(xSize, ySize);
        int pos = 0;
        for (int y = 0; y <image.getYSize(); y++) {
            for (int x = 0; x < image.getXSize(); x++) {
                float f = depth[pos];
                int v = (int) (f * 256);
                byte b = (byte) v;
                image.putPixel(x, y, b, b, b);
                pos++;
            }
        }
        return image;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
