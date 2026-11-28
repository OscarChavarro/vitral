import { IOException } from "../../../java/io/IOException.js";
import { VSDK } from "../common/VSDK.js";
import { Logger } from "../common/logging/Logger.js";
import type { Camera } from "../environment/camera/Camera.js";
import type { Image } from "../media/Image.js";
import type { RGBAImageUncompressed } from "../media/RGBAImageUncompressed.js";
import { RGBAPixel } from "../media/RGBAPixel.js";
import { RGBPixel } from "../media/RGBPixel.js";
import { KeyEvent } from "./KeyEvent.js";
import { PresentationElement } from "./PresentationElement.js";

/**
Reads the image of a `HudIcon` from its file name: Java's
`ImagePersistence.importRGBA(new File(imageFilename))`. `@vitral/base` has no
file access, so each runtime that has one registers its reader with
`HudIcon.setImageImporter` (i.e. `ImagePersistenceRasterImport.importRGBA` in
`@vitral/fs`). It throws, as Java's import does, when the image cannot be read.
*/
export type HudIconImageImporter = (imageFilename: string) => RGBAImageUncompressed;

/**
Class used to manage HUD based interactions based on button like images with
interaction area.
*/
export class HudIcon extends PresentationElement {
    private static imageImporter: HudIconImageImporter | null = null;

    /// Image to show, can be null
    private image: RGBAImageUncompressed | null = null;

    /// Image coordinate to locate inside framebuffer. Can be negative following
    /// X11 geometry style
    private x = 0;
    private y = 0;

    /// Can be 0, in that case size is taken from image size
    private xSize = 0;
    private ySize = 0;

    /// URL to link when user does it click
    private keyEvent = 0;

    public constructor(
        x: number,
        y: number,
        xSize: number,
        ySize: number,
        imageFilename: string | null,
        keyEvent: number,
    ) {
        super();
        this.init(x, y, xSize, ySize, keyEvent, imageFilename);
    }

    /**
    @param importer the reader every later `HudIcon` uses for its image file,
    or null for none (the icons then take Java's "Image not found" branch)
    */
    public static setImageImporter(importer: HudIconImageImporter | null): void {
        HudIcon.imageImporter = importer;
    }

    /**
    Given a (x, y) coordinate in screen reference system (with 0, 0 coordinate
    on top left corner of screen), this method determines if that coordinate
    falls between current hud icon.
    @param inX
    @param inY
    @return true if current coordinate lies inside HudIcon with respect to
    camera's viewport configuration, false otherwise
    */
    public doIntersection(inX: number, inY: number, c: Camera): boolean {
        let currentXSize: number = this.xSize;
        let currentYSize: number = this.ySize;

        if (currentXSize <= 0) {
            currentXSize = this.image!.getXSize();
        }

        if (currentYSize <= 0) {
            currentYSize = this.image!.getYSize();
        }

        let xx: number = this.x;
        let yy: number = this.y;

        if (xx < 0) {
            xx = -xx;
            xx = Math.trunc(c.getViewportXSize()) - this.image!.getXSize() - xx;
        }
        if (yy < 0) {
            yy = -yy;
            yy = Math.trunc(c.getViewportYSize()) - this.image!.getYSize() - yy;
        }

        return inX >= xx && inX <= xx + currentXSize && inY >= yy && inY <= yy + currentYSize;
    }

    /**
    Given a (x, y) coordinate in screen reference system (with 0, 0 coordinate
    on top left corner of screen), this method determines if that coordinate
    falls between current hud icon. This version takes into account a gravity
    range around base rectangles.
    @param inX
    @param inY
    @param gravity
    @param c
    @return true if current coordinate lies inside HudIcon with respect to
    camera's viewport configuration, false otherwise
    */
    public doIntersectionWithGravity(inX: number, inY: number, gravity: number, c: Camera): boolean {
        if (this.doIntersection(inX, inY, c)) {
            return true;
        }
        let currentXSize: number = this.xSize;
        let currentYSize: number = this.ySize;

        if (currentXSize <= 0) {
            currentXSize = this.image!.getXSize();
        }

        if (currentYSize <= 0) {
            currentYSize = this.image!.getYSize();
        }

        let xx: number = this.x;
        let yy: number = this.y;

        if (xx < 0) {
            xx = -xx;
            xx = Math.trunc(c.getViewportXSize()) - this.image!.getXSize() - xx;
        }
        if (yy < 0) {
            yy = -yy;
            yy = Math.trunc(c.getViewportYSize()) - this.image!.getYSize() - yy;
        }

        return (
            inX >= xx - gravity &&
            inX <= xx + gravity + currentXSize &&
            inY >= yy - gravity &&
            inY <= yy + gravity + currentYSize
        );
    }

    /**
    Generates a segment of HTML1 code to encode current icon over a map for
    simple image interaction.
    @param baseUrl
    @return a string representing contents
    */
    public getMapTag(baseUrl: string): string {
        return (
            '    <AREA SHAPE="rect" COORDS="' +
            this.getX() +
            ", " +
            this.getY() +
            ", " +
            (this.getX() + this.xSize) +
            ", " +
            (this.getY() + this.ySize) +
            '" HREF="' +
            baseUrl +
            KeyEvent.getKeyName(this.getKeyEvent()) +
            '" ' +
            'style="cursor: default;"' +
            "/>"
        );
    }

    /**
    Modifies output image to add current image over it in the corresponding
    coordinates
    @param output Image to where pixels will be drawn
    */
    public overWritePixels(output: Image): void {
        const image: RGBAImageUncompressed | null = this.image;
        if (image === null) {
            return;
        }

        const myXSize: number = image.getXSize();
        const myYSize: number = image.getYSize();
        let xi: number, yi: number;
        const origin: RGBAPixel = new RGBAPixel();
        const target: RGBPixel = new RGBPixel();

        for (xi = 0; xi < myXSize; xi++) {
            for (yi = 0; yi < myYSize; yi++) {
                image.getPixelRgba(xi, yi, origin);
                target.r = origin.r;
                target.g = origin.g;
                target.b = origin.b;
                if (VSDK.signedByte2unsignedInteger(origin.a) > 250) {
                    output.putPixelRgb(xi + this.getX(), yi + this.getY(), target);
                }
            }
        }
    }

    private init(
        x: number,
        y: number,
        xSize: number,
        ySize: number,
        keyEvent: number,
        imageFilename: string | null,
    ): void {
        this.setX(x);
        this.setY(y);
        this.xSize = xSize;
        this.ySize = ySize;
        this.keyEvent = keyEvent;

        try {
            this.image = null;

            if (imageFilename !== null) {
                const importer: HudIconImageImporter | null = HudIcon.imageImporter;
                if (importer === null) {
                    throw new IOException("No image importer registered for " + imageFilename);
                }
                const image: RGBAImageUncompressed = importer(imageFilename);
                this.image = image;
                if (xSize === 0) {
                    this.xSize = image.getXSize();
                }
                if (ySize === 0) {
                    this.ySize = image.getYSize();
                }
            }
        } catch (e) {
            Logger.reportMessageWithException(
                this,
                VSDK.WARNING,
                "WebIcon",
                "Image not found",
                e instanceof Error ? e : null,
            );
        }
    }

    /**
    @return the image
    */
    public getImage(): RGBAImageUncompressed | null {
        return this.image;
    }

    /**
    @param image the image to set
    */
    public setImage(image: RGBAImageUncompressed | null): void {
        this.image = image;
    }

    public override toString(): string {
        let msg: string;

        if (this.image !== null) {
            msg =
                "ICON -> x:" +
                this.getX() +
                " y: " +
                this.getY() +
                " xSize: " +
                this.xSize +
                " ySize: " +
                this.ySize +
                " img: LOADED keyEvent: " +
                KeyEvent.getKeyName(this.getKeyEvent()) +
                "\n";
        } else {
            msg =
                "ICON -> x:" +
                this.getX() +
                " y: " +
                this.getY() +
                " xSize: " +
                this.xSize +
                " ySize: " +
                this.ySize +
                " img: null keyEvent: " +
                KeyEvent.getKeyName(this.getKeyEvent()) +
                "\n";
        }

        return msg;
    }

    /**
    @return the x
    */
    public getX(): number {
        return this.x;
    }

    /**
    @param x the x to set
    */
    public setX(x: number): void {
        this.x = x;
    }

    /**
    @return the y
    */
    public getY(): number {
        return this.y;
    }

    /**
    @param y the y to set
    */
    public setY(y: number): void {
        this.y = y;
    }

    /**
     * @return the keyEvent
     */
    public getKeyEvent(): number {
        return this.keyEvent;
    }
}
