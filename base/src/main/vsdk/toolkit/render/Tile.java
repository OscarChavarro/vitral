package vsdk.toolkit.render;

import vsdk.toolkit.media.Image;

/**
Represents an image sub-region bound to a target image sink.
*/
public class Tile
{
    private final Image image;
    private final int x0;
    private final int y0;
    private final int dx;
    private final int dy;

    public Tile(Image image, int x0, int y0, int dx, int dy)
    {
        if ( image == null ) {
            throw new IllegalArgumentException("image can not be null");
        }
        if ( x0 < 0 || y0 < 0 ) {
            throw new IllegalArgumentException("tile origin must be >= 0");
        }
        if ( dx <= 0 || dy <= 0 ) {
            throw new IllegalArgumentException("tile size must be > 0");
        }
        if ( x0 + dx > image.getXSize() || y0 + dy > image.getYSize() ) {
            throw new IllegalArgumentException(
                "tile bounds must be inside target image");
        }

        this.image = image;
        this.x0 = x0;
        this.y0 = y0;
        this.dx = dx;
        this.dy = dy;
    }

    public Image getImage()
    {
        return image;
    }

    public int getX0()
    {
        return x0;
    }

    public int getY0()
    {
        return y0;
    }

    public int getDx()
    {
        return dx;
    }

    public int getDy()
    {
        return dy;
    }
}
