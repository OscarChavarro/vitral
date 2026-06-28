package vsdk.toolkit.media;

public final class IndexedColorImageHDRUncompressed
{
    private byte[] data;
    private int xSize;
    private int ySize;
    private int colorMapSize;
    private RGBAPixelHDR[] colorTable;

    public IndexedColorImageHDRUncompressed()
    {
        xSize = 0;
        ySize = 0;
        data = null;
        colorMapSize = 0;
        colorTable = null;
    }

    public int getXSize() { return xSize; }
    public int getYSize() { return ySize; }
    public void setXSize(int w) { xSize = w; }
    public void setYSize(int h) { ySize = h; }
    public int getColorMapSize() { return colorMapSize; }
    public void setColorMapSize(int n) { colorMapSize = n; }
    public RGBAPixelHDR[] getColorTable() { return colorTable; }
    public void setColorTable(RGBAPixelHDR[] ct) { colorTable = ct; }

    public void allocate(int w, int h)
    {
        xSize = w;
        ySize = h;
        data = new byte[w * h];
    }

    public int getPixel(int x, int y)
    {
        return data[y * xSize + x] & 0xff;
    }

    public void setPixel(int x, int y, int value)
    {
        data[y * xSize + x] = (byte)(value & 0xff);
    }
}
