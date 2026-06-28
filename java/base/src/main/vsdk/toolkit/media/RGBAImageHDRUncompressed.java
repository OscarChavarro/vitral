package vsdk.toolkit.media;

/**
Uncompressed HDR RGBA image storage with row-major 16-bit channels.
This mirrors the C++ counterpart as a small data container with direct pixel
access.
*/
public class RGBAImageHDRUncompressed
{
    private int xSize;
    private int ySize;
    private char[] data;

    public RGBAImageHDRUncompressed()
    {
        xSize = 0;
        ySize = 0;
        data = null;
    }

    public int getXSize()
    {
        return xSize;
    }

    public int getYSize()
    {
        return ySize;
    }

    public void allocate(int w, int h)
    {
        xSize = w;
        ySize = h;
        data = new char[w * h * 4];
    }

    public void getPixel(int x, int y, RGBAPixelHDR pixel)
    {
        int base = ((y * xSize) + x) * 4;

        pixel.r = data[base];
        pixel.g = data[base + 1];
        pixel.b = data[base + 2];
        pixel.a = data[base + 3];
    }

    public void setPixel(int x, int y, final RGBAPixelHDR pixel)
    {
        int base = ((y * xSize) + x) * 4;

        data[base] = pixel.r;
        data[base + 1] = pixel.g;
        data[base + 2] = pixel.b;
        data[base + 3] = pixel.a;
    }
}
