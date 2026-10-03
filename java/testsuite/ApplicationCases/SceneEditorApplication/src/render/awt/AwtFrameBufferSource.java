package render.awt;

// AWT classes
import java.awt.image.BufferedImage;

// VSDK classes
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.ZBuffer;
import vsdk.toolkit.render.awt.AwtRGBImageUncompressedRenderer;

// Application classes
import render.FrameBufferSource;

/**
Frame buffer of the AWT presentation: an area of the off-screen image where
the viewport set is drawn. AWT draws only in 2D, so there is no depth buffer:
reading it gives a buffer at the far distance everywhere.
*/
public class AwtFrameBufferSource implements FrameBufferSource
{
    private final BufferedImage frame;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    /**
    @param frame the off-screen image of the viewport set
    @param x left pixel of the area to read
    @param y top pixel of the area to read
    @param width width of the area to read
    @param height height of the area to read
    */
    public AwtFrameBufferSource(BufferedImage frame, int x, int y, int width,
                                int height)
    {
        this.frame = frame;
        this.x = Math.max(0, x);
        this.y = Math.max(0, y);
        this.width = Math.max(1, Math.min(width, frame.getWidth() - this.x));
        this.height = Math.max(1, Math.min(height, frame.getHeight() - this.y));
    }

    @Override
    public RGBImageUncompressed readColor()
    {
        RGBImageUncompressed image = new RGBImageUncompressed();

        AwtRGBImageUncompressedRenderer.importFromAwtBufferedImage(
            frame.getSubimage(x, y, width, height), image);
        return image;
    }

    @Override
    public ZBuffer readDepth()
    {
        ZBuffer depth = new ZBuffer(width, height);
        int i;
        int j;

        for ( j = 0; j < height; j++ ) {
            for ( i = 0; i < width; i++ ) {
                depth.setZ(i, j, 1.0f);
            }
        }
        return depth;
    }
}
