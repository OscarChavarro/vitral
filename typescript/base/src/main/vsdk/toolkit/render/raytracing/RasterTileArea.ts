import { IllegalArgumentException } from "../../../../java/lang/IllegalArgumentException.js";
import type { Image } from "../../media/Image.js";

/**
Represents an absolute sub-region of a target output image.

The origin and end coordinates are expressed in the coordinate system of the
target image. Rendering code must iterate from startX to x1 and startY to y1, and
write directly to the target image at those absolute coordinates. The width
and height values are extents, not a local coordinate system.
*/
export class RasterTileArea {
    private readonly image: Image;
    private readonly startX: number;
    private readonly startY: number;
    private readonly width: number;
    private readonly height: number;

    public constructor(image: Image, startX: number, startY: number, width: number, height: number) {
        if (image === null) {
            throw new IllegalArgumentException("image can not be null");
        }
        if (startX < 0 || startY < 0) {
            throw new IllegalArgumentException("tile origin must be >= 0");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("tile size must be > 0");
        }
        if (startX + width > image.getXSize() || startY + height > image.getYSize()) {
            throw new IllegalArgumentException("tile bounds must be inside target image");
        }

        this.image = image;
        this.startX = startX;
        this.startY = startY;
        this.width = width;
        this.height = height;
    }

    public getImage(): Image {
        return this.image;
    }

    public getStartX(): number {
        return this.startX;
    }

    public getStartY(): number {
        return this.startY;
    }

    public getWidth(): number {
        return this.width;
    }

    public getHeight(): number {
        return this.height;
    }

    public getEndX(): number {
        return this.startX + this.width;
    }

    public getEndY(): number {
        return this.startY + this.height;
    }
}
