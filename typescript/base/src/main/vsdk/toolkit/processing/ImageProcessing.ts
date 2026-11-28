import { ProcessingElement } from "./ProcessingElement.js";
import type { ColorRgb } from "../common/color/ColorRgb.js";
import type { Image } from "../media/Image.js";
import { IndexedColorImageUncompressed } from "../media/IndexedColorImageUncompressed.js";
import type { RGBImageUncompressed } from "../media/RGBImageUncompressed.js";
import { RGBPixel } from "../media/RGBPixel.js";
import { javaIntCast } from "../media/_pixel.js";
import { VSDK } from "../common/VSDK.js";

/**
\todo  Current implementation is not well designed. This class' design should
be checked to inforce:
  - Interoperability with existing image processing toolkits/frameworks like
    JAI, ITK, Khoros, OpenCV, Matlab+ImageToolbox, ImageMagick+JMagick, GIMP,
    etc.
  - Programmability of image processing operations using GPUs
  - Filter graph approach

Java's `(int)` casts are `javaIntCast` (NaN is 0), and Java's integer divisions
are truncated. Where Java calls `IndexedColorImageUncompressed.putPixel(int,
int, byte)`, this edition calls its `putPixelByte`.
*/
export abstract class ImageProcessing extends ProcessingElement {
    private static gammaCorrection8bits(input: number, gamma: number): number {
        let a: number, b: number;
        let out: number;

        a = input / 255.0;
        b = Math.pow(a, 1.0 / gamma);
        out = javaIntCast(b * 255.0);
        return out;
    }

    /** Both Java overloads: `IndexedColorImageUncompressed` and `RGBImageUncompressed`. */
    public static gammaCorrection(img: IndexedColorImageUncompressed | RGBImageUncompressed, gamma: number): void {
        if (img instanceof IndexedColorImageUncompressed) {
            ImageProcessing.gammaCorrectionIndexed(img, gamma);
        } else {
            ImageProcessing.gammaCorrectionRgb(img, gamma);
        }
    }

    private static gammaCorrectionIndexed(img: IndexedColorImageUncompressed, gamma: number): void {
        let x: number, y: number;
        let val: number;

        for (x = 0; x < img.getXSize(); x++) {
            for (y = 0; y < img.getYSize(); y++) {
                val = img.getPixel(x, y);
                val = ImageProcessing.gammaCorrection8bits(val, gamma);
                img.putPixelByte(x, y, VSDK.unsigned8BitInteger2signedByte(val));
            }
        }
    }

    private static gammaCorrectionRgb(img: RGBImageUncompressed, gamma: number): void {
        let x: number, y: number;
        const p: RGBPixel = new RGBPixel();
        let r: number, g: number, b: number;

        for (x = 0; x < img.getXSize(); x++) {
            for (y = 0; y < img.getYSize(); y++) {
                img.getPixelRgb(x, y, p);
                r = ImageProcessing.gammaCorrection8bits(VSDK.signedByte2unsignedInteger(p.r), gamma);
                g = ImageProcessing.gammaCorrection8bits(VSDK.signedByte2unsignedInteger(p.g), gamma);
                b = ImageProcessing.gammaCorrection8bits(VSDK.signedByte2unsignedInteger(p.b), gamma);
                p.r = VSDK.unsigned8BitInteger2signedByte(r);
                p.g = VSDK.unsigned8BitInteger2signedByte(g);
                p.b = VSDK.unsigned8BitInteger2signedByte(b);
                img.putPixelRgb(x, y, p);
            }
        }
    }

    /**
    Given the `input` and `output` previously created images, fills in
    `output`'s space the `this` image using bilinear interpolation.
    \todo  worked well only in the growing case. Must add the shrinking
    case for area averaging.
    */
    public static resize(input: Image, output: Image): void {
        const xSizeIn: number = input.getXSize();
        const ySizeIn: number = input.getYSize();
        const xSizeOut: number = output.getXSize();
        const ySizeOut: number = output.getYSize();
        let u: number, v: number;
        let x: number, y: number;
        let source: ColorRgb;
        let target: RGBPixel = new RGBPixel();
        let acum: RGBPixel;

        if (xSizeOut === xSizeIn && ySizeOut === ySizeIn) {
            ImageProcessing.copy(input, output);
        } else if (xSizeOut > xSizeIn && ySizeOut > ySizeIn) {
            for (x = 0; x < xSizeOut; x++) {
                for (y = 0; y < ySizeOut; y++) {
                    u = x / xSizeOut;
                    v = y / ySizeOut;
                    source = input.getColorRgbBiLinear(u, v);
                    target.r = VSDK.unsigned8BitInteger2signedByte(javaIntCast(source.r() * 255));
                    target.g = VSDK.unsigned8BitInteger2signedByte(javaIntCast(source.g() * 255));
                    target.b = VSDK.unsigned8BitInteger2signedByte(javaIntCast(source.b() * 255));
                    output.putPixelRgb(x, y, target);
                }
            }
        } else {
            output.init(xSizeOut, ySizeOut);
            const xf: number = xSizeIn / xSizeOut;
            const yf: number = ySizeIn / ySizeOut;
            const xfi: number = javaIntCast(xf);
            const yfi: number = javaIntCast(yf);
            const w: number = Math.imul(xfi, yfi);
            let acumr: number;
            let acumg: number;
            let acumb: number;
            let xx: number, yy: number, x0: number, y0: number, x1: number, y1: number;

            acum = new RGBPixel();
            for (xx = 0; xx < xSizeOut; xx++) {
                for (yy = 0; yy < ySizeOut; yy++) {
                    acumr = acumg = acumb = 0.0;
                    x0 = javaIntCast(xx * xf);
                    x1 = (x0 + xfi) | 0;
                    for (x = x0; x < x1 && x < xSizeIn; x++) {
                        y0 = javaIntCast(yy * yf);
                        y1 = (y0 + yfi) | 0;
                        for (y = y0; y < y1 && y < ySizeIn; y++) {
                            target = input.getPixelRgb(x, y);
                            acumr += VSDK.signedByte2unsignedInteger(target.r) / w;
                            acumg += VSDK.signedByte2unsignedInteger(target.g) / w;
                            acumb += VSDK.signedByte2unsignedInteger(target.b) / w;
                        }
                    }
                    if (acumr >= 255.0) acumr = 255.0;
                    if (acumg >= 255.0) acumg = 255.0;
                    if (acumb >= 255.0) acumb = 255.0;
                    acum.r = VSDK.unsigned8BitInteger2signedByte(javaIntCast(acumr));
                    acum.g = VSDK.unsigned8BitInteger2signedByte(javaIntCast(acumg));
                    acum.b = VSDK.unsigned8BitInteger2signedByte(javaIntCast(acumb));
                    output.putPixelRgb(xx, yy, acum);
                }
            }
        }
    }

    /**
    Copies the contents from the `input` image to the `output` image. Note that
    this method can serve also as a format conversion between different Image
    formats (i.e. convert an RGBImageUncompressed to an IndexedColorImageUncompressed).
    */
    public static copy(input: Image, output: Image): void {
        const xSize: number = input.getXSize();
        const ySize: number = input.getYSize();
        let x: number, y: number;
        let target: RGBPixel;

        output.init(xSize, ySize);

        for (x = 0; x < xSize; x++) {
            for (y = 0; y < ySize; y++) {
                target = input.getPixelRgb(x, y);
                output.putPixelRgb(x, y, target);
            }
        }
    }

    /**
    Takes the greater dimension of the input image, and uses it to create an
    output square image of such dimension. Then copies the input image
    centered in the output image.
    */
    public static squareFill(input: Image, output: Image): void {
        const xSize: number = input.getXSize();
        const ySize: number = input.getYSize();
        let x: number, y: number;
        let target: RGBPixel;
        let maxSize: number = xSize;

        if (ySize > maxSize) {
            maxSize = ySize;
        }

        output.init(maxSize, maxSize);

        let dx: number, dy: number;
        dx = Math.trunc((maxSize - xSize) / 2);
        dy = Math.trunc((maxSize - ySize) / 2);

        for (x = 0; x < xSize; x++) {
            for (y = 0; y < ySize; y++) {
                target = input.getPixelRgb(x, y);
                output.putPixelRgb(x + dx, y + dy, target);
            }
        }
    }

    /**
    Grows the input image copying the input in the center of a border of
    `border` size.
    */
    public static frame(input: Image, output: Image, border: number): void {
        const xSize: number = input.getXSize();
        const ySize: number = input.getYSize();
        let x: number, y: number;
        let target: RGBPixel;

        output.init(xSize + 2 * border, ySize + 2 * border);

        for (x = 0; x < xSize; x++) {
            for (y = 0; y < ySize; y++) {
                target = input.getPixelRgb(x, y);
                output.putPixelRgb(x + border, y + border, target);
            }
        }
    }

    /**
    This method extracts a region of interest from source image rectangle
    including points from <x0Roi, y0Roi> to <x1Roi, y1Roi>.
    */
    public static extractRoi(
        source: Image,
        roi: Image,
        x0Roi: number,
        y0Roi: number,
        x1Roi: number,
        y1Roi: number,
    ): void {
        let tmp: number;
        let dx: number, dy: number;

        if (x0Roi > x1Roi) {
            tmp = x0Roi;
            x0Roi = x1Roi;
            x1Roi = tmp;
        }
        if (y0Roi > y1Roi) {
            tmp = y0Roi;
            y0Roi = y1Roi;
            y1Roi = tmp;
        }

        if (x0Roi < 0) x0Roi = 0;
        if (y0Roi < 0) y0Roi = 0;
        if (x1Roi >= source.getXSize()) x1Roi = source.getXSize() - 1;
        if (y1Roi >= source.getYSize()) y1Roi = source.getYSize() - 1;

        if (x0Roi >= source.getXSize() || y0Roi >= source.getYSize()) {
            return;
        }

        dx = x1Roi - x0Roi + 1;
        dy = y1Roi - y0Roi + 1;

        let target: RGBPixel;
        let x: number, y: number;

        roi.init(dx, dy);
        for (x = 0; x < dx; x++) {
            for (y = 0; y < dy; y++) {
                target = source.getPixelRgb(x0Roi + x, y0Roi + y);
                roi.putPixelRgb(x, y, target);
            }
        }
    }

    /**
    A distance field is a scalar map where each pixel value correspond to the
    nearest distance to an "inside" pixel. Every pixel in the input image with
    a value greater or equal to `threshold` will be noted as "inside",
    otherwise will be "outside".

    This implements the naive, real, full, simple (direct) and non-optimized
    version of the algorithm (time O(N^4), space O(2*N^2)). It is provided
    for reference; use processDistanceFieldWithArray instead.
    */
    public static processDistanceField(
        inInput: Image | null,
        outOutput: IndexedColorImageUncompressed | null,
        threshold: number,
    ): boolean {
        if (inInput === null || outOutput === null) {
            return false;
        }

        const dx: number = inInput.getXSize();
        const dy: number = inInput.getYSize();

        if (dx !== outOutput.getXSize() || dy !== outOutput.getYSize()) {
            return false;
        }

        let x: number, y: number, xx: number, yy: number;
        let p: RGBPixel;
        let dist2: number;
        const maxdist2: number = dx * dx + dy * dy;
        let mindist2: number;
        const maxdist: number = Math.sqrt(maxdist2);
        let val: number;

        for (x = 0; x < dx; x++) {
            for (y = 0; y < dy; y++) {
                mindist2 = maxdist2;
                for (xx = 0; xx < dx; xx++) {
                    for (yy = 0; yy < dy; yy++) {
                        p = inInput.getPixelRgb(xx, yy);
                        val = Math.trunc(
                            (VSDK.signedByte2unsignedInteger(p.r) +
                                VSDK.signedByte2unsignedInteger(p.g) +
                                VSDK.signedByte2unsignedInteger(p.b)) /
                                3,
                        );
                        if (val >= threshold) {
                            dist2 = (xx - x) * (xx - x) + (yy - y) * (yy - y);
                            if (dist2 < mindist2) {
                                mindist2 = dist2;
                            }
                        }
                    }
                }
                val = javaIntCast((Math.sqrt(mindist2) / maxdist) * 255.0);
                outOutput.putPixelByte(x, y, VSDK.unsigned8BitInteger2signedByte(val));
            }
        }
        return true;
    }

    /**
    A distance field is a scalar map where each pixel value correspond to the
    nearest distance to an "inside" pixel. Every pixel in the input image with
    a value greater or equal to `threshold` will be noted as "inside",
    otherwise will be "outside".

    This implements an optimized version of the algorithm in method
    `processDistanceField`, using a dynamic programming technique which
    requires an extra preprocessing step and an array of the inside pixels.
    */
    public static processDistanceFieldWithArray(
        inInput: Image | null,
        outOutput: IndexedColorImageUncompressed | null,
        threshold: number,
    ): boolean {
        if (inInput === null || outOutput === null) {
            return false;
        }

        const dx: number = inInput.getXSize();
        const dy: number = inInput.getYSize();

        if (dx !== outOutput.getXSize() || dy !== outOutput.getYSize()) {
            return false;
        }

        let x: number, y: number;
        let arrSize = 0;
        let p: RGBPixel;
        let val: number;

        for (x = 0; x < dx; x++) {
            for (y = 0; y < dy; y++) {
                p = inInput.getPixelRgb(x, y);
                val = Math.trunc(
                    (VSDK.signedByte2unsignedInteger(p.r) +
                        VSDK.signedByte2unsignedInteger(p.g) +
                        VSDK.signedByte2unsignedInteger(p.b)) /
                        3,
                );
                if (val >= threshold) {
                    arrSize++;
                }
            }
        }

        let i = 0;
        const xcoords: Int32Array = new Int32Array(arrSize);
        const ycoords: Int32Array = new Int32Array(arrSize);

        for (x = 0; x < dx; x++) {
            for (y = 0; y < dy; y++) {
                p = inInput.getPixelRgb(x, y);
                val = Math.trunc(
                    (VSDK.signedByte2unsignedInteger(p.r) +
                        VSDK.signedByte2unsignedInteger(p.g) +
                        VSDK.signedByte2unsignedInteger(p.b)) /
                        3,
                );
                if (val >= threshold) {
                    xcoords[i] = x;
                    ycoords[i] = y;
                    i++;
                }
            }
        }

        let xx: number, yy: number;
        let dist2: number;
        const maxdist2: number = dx * dx + dy * dy;
        let mindist2: number;
        const maxdist: number = Math.sqrt(maxdist2);

        for (x = 0; x < dx; x++) {
            for (y = 0; y < dy; y++) {
                mindist2 = maxdist2;
                for (i = 0; i < arrSize; i++) {
                    xx = xcoords[i]!;
                    yy = ycoords[i]!;
                    dist2 = (xx - x) * (xx - x) + (yy - y) * (yy - y);
                    if (dist2 < mindist2) {
                        mindist2 = dist2;
                    }
                }
                val = javaIntCast((Math.sqrt(mindist2) / maxdist) * 255.0);
                outOutput.putPixelByte(x, y, VSDK.unsigned8BitInteger2signedByte(val));
            }
        }
        return true;
    }
}
