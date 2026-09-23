package vsdk.toolkit.render.raytracing;

import vsdk.toolkit.media.Image;

/**
Represents an absolute sub-region of a target output image.

The origin and end coordinates are expressed in the coordinate system of the
target image. Rendering code must iterate from startX to x1 and startY to y1, and
write directly to the target image at those absolute coordinates. The width
and height values are extents, not a local coordinate system.
*/
public class RasterTileArea
{
    private final Image image;
    private final int startX;
    private final int startY;
    private final int width;
    private final int height;

    public RasterTileArea(Image image, int startX, int startY, int width, int height)
    {
        if ( image == null ) {
            throw new IllegalArgumentException("image can not be null");
        }
        if ( startX < 0 || startY < 0 ) {
            throw new IllegalArgumentException("tile origin must be >= 0");
        }
        if ( width <= 0 || height <= 0 ) {
            throw new IllegalArgumentException("tile size must be > 0");
        }
        if ( startX + width > image.getXSize() || startY + height > image.getYSize() ) {
            throw new IllegalArgumentException(
                "tile bounds must be inside target image");
        }

        this.image = image;
        this.startX = startX;
        this.startY = startY;
        this.width = width;
        this.height = height;
    }

    public Image getImage()
    {
        return image;
    }

    public int getStartX()
    {
        return startX;
    }

    public int getStartY()
    {
        return startY;
    }

    public int getWidth()
    {
        return width;
    }

    public int getHeight()
    {
        return height;
    }

    public int getEndX()
    {
        return startX + width;
    }

    public int getEndY()
    {
        return startY + height;
    }
}
