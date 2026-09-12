import { IllegalArgumentException } from "../../../../java/lang/IllegalArgumentException.js";
import type { Image } from "../../media/Image.js";

/**
Represents an absolute sub-region of a target output image.

The origin and end coordinates are expressed in the coordinate system of the
target image. Rendering code must iterate from x0 to x1 and y0 to y1, and
write directly to the target image at those absolute coordinates. The width
and height values are extents, not a local coordinate system.
*/
export class RasterTileArea {
    private readonly image: Image;
    private readonly x0: number;
    private readonly y0: number;
    private readonly dx: number;
    private readonly dy: number;

    public constructor(image: Image, x0: number, y0: number, dx: number, dy: number) {
        if (image === null) {
            throw new IllegalArgumentException("image can not be null");
        }
        if (x0 < 0 || y0 < 0) {
            throw new IllegalArgumentException("tile origin must be >= 0");
        }
        if (dx <= 0 || dy <= 0) {
            throw new IllegalArgumentException("tile size must be > 0");
        }
        if (x0 + dx > image.getXSize() || y0 + dy > image.getYSize()) {
            throw new IllegalArgumentException("tile bounds must be inside target image");
        }

        this.image = image;
        this.x0 = x0;
        this.y0 = y0;
        this.dx = dx;
        this.dy = dy;
    }

    public getImage(): Image {
        return this.image;
    }

    public getX0(): number {
        return this.x0;
    }

    public getY0(): number {
        return this.y0;
    }

    public getDx(): number {
        return this.dx;
    }

    public getDy(): number {
        return this.dy;
    }

    public getX1(): number {
        return this.x0 + this.dx;
    }

    public getY1(): number {
        return this.y0 + this.dy;
    }
}
