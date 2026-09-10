import { ProcessingElement } from "./ProcessingElement.js";
import { Image } from "../media/Image.js";
import { IndexedColorImageUncompressed } from "../media/IndexedColorImageUncompressed.js";
import { RGBImageUncompressed } from "../media/RGBImageUncompressed.js";
import { RGBPixel } from "../media/RGBPixel.js";
import { VSDK } from "../common/VSDK.js";
export abstract class ImageProcessing extends ProcessingElement {
    private static gammaCorrection8bits(v: number, g: number): number {
        return Math.trunc(Math.pow(v / 255, 1 / g) * 255);
    }
    public static gammaCorrection(img: IndexedColorImageUncompressed | RGBImageUncompressed, g: number): void {
        for (let x = 0; x < img.getXSize(); x++)
            for (let y = 0; y < img.getYSize(); y++) {
                if (img instanceof IndexedColorImageUncompressed)
                    img.putPixel(
                        x,
                        y,
                        VSDK.unsigned8BitInteger2signedByte(this.gammaCorrection8bits(img.getPixel(x, y), g)),
                    );
                else {
                    const p = img.getPixelRgb(x, y);
                    p.r = VSDK.unsigned8BitInteger2signedByte(
                        this.gammaCorrection8bits(VSDK.signedByte2unsignedInteger(p.r), g),
                    );
                    p.g = VSDK.unsigned8BitInteger2signedByte(
                        this.gammaCorrection8bits(VSDK.signedByte2unsignedInteger(p.g), g),
                    );
                    p.b = VSDK.unsigned8BitInteger2signedByte(
                        this.gammaCorrection8bits(VSDK.signedByte2unsignedInteger(p.b), g),
                    );
                    img.putPixelRgb(x, y, p);
                }
            }
    }
    public static copy(input: Image, out: Image): void {
        out.init(input.getXSize(), input.getYSize());
        for (let x = 0; x < input.getXSize(); x++)
            for (let y = 0; y < input.getYSize(); y++) out.putPixelRgb(x, y, input.getPixelRgb(x, y));
    }
    public static resize(input: Image, out: Image): void {
        const wi = input.getXSize(),
            hi = input.getYSize(),
            wo = out.getXSize(),
            ho = out.getYSize();
        if (wi === wo && hi === ho) {
            this.copy(input, out);
            return;
        }
        if (wo > wi && ho > hi) {
            const target = new RGBPixel();
            for (let x = 0; x < wo; x++)
                for (let y = 0; y < ho; y++) {
                    const source = input.getColorRgbBiLinear(x / wo, y / ho);
                    target.r = VSDK.unsigned8BitInteger2signedByte(Math.trunc(source.r() * 255));
                    target.g = VSDK.unsigned8BitInteger2signedByte(Math.trunc(source.g() * 255));
                    target.b = VSDK.unsigned8BitInteger2signedByte(Math.trunc(source.b() * 255));
                    out.putPixelRgb(x, y, target);
                }
            return;
        }
        out.init(wo, ho);
        const xf = wi / wo,
            yf = hi / ho,
            xfi = Math.trunc(xf),
            yfi = Math.trunc(yf),
            weight = xfi * yfi;
        const accumulated = new RGBPixel();
        for (let xx = 0; xx < wo; xx++)
            for (let yy = 0; yy < ho; yy++) {
                let red = 0,
                    green = 0,
                    blue = 0;
                const x0 = Math.trunc(xx * xf),
                    x1 = x0 + xfi;
                for (let x = x0; x < x1 && x < wi; x++) {
                    const y0 = Math.trunc(yy * yf),
                        y1 = y0 + yfi;
                    for (let y = y0; y < y1 && y < hi; y++) {
                        const target = input.getPixelRgb(x, y);
                        red += VSDK.signedByte2unsignedInteger(target.r) / weight;
                        green += VSDK.signedByte2unsignedInteger(target.g) / weight;
                        blue += VSDK.signedByte2unsignedInteger(target.b) / weight;
                    }
                }
                accumulated.r = VSDK.unsigned8BitInteger2signedByte(Math.trunc(Math.min(red, 255)));
                accumulated.g = VSDK.unsigned8BitInteger2signedByte(Math.trunc(Math.min(green, 255)));
                accumulated.b = VSDK.unsigned8BitInteger2signedByte(Math.trunc(Math.min(blue, 255)));
                out.putPixelRgb(xx, yy, accumulated);
            }
    }
    public static squareFill(input: Image, out: Image): void {
        const n = Math.max(input.getXSize(), input.getYSize());
        out.init(n, n);
        const dx = Math.trunc((n - input.getXSize()) / 2),
            dy = Math.trunc((n - input.getYSize()) / 2);
        for (let x = 0; x < input.getXSize(); x++)
            for (let y = 0; y < input.getYSize(); y++) out.putPixelRgb(x + dx, y + dy, input.getPixelRgb(x, y));
    }
    public static frame(input: Image, out: Image, b: number): void {
        out.init(input.getXSize() + 2 * b, input.getYSize() + 2 * b);
        for (let x = 0; x < input.getXSize(); x++)
            for (let y = 0; y < input.getYSize(); y++) out.putPixelRgb(x + b, y + b, input.getPixelRgb(x, y));
    }
    public static extractRoi(s: Image, r: Image, x0: number, y0: number, x1: number, y1: number): void {
        if (x0 > x1) [x0, x1] = [x1, x0];
        if (y0 > y1) [y0, y1] = [y1, y0];
        x0 = Math.max(0, x0);
        y0 = Math.max(0, y0);
        x1 = Math.min(s.getXSize() - 1, x1);
        y1 = Math.min(s.getYSize() - 1, y1);
        if (x0 >= s.getXSize() || y0 >= s.getYSize()) return;
        r.init(x1 - x0 + 1, y1 - y0 + 1);
        for (let x = 0; x < r.getXSize(); x++)
            for (let y = 0; y < r.getYSize(); y++) r.putPixelRgb(x, y, s.getPixelRgb(x0 + x, y0 + y));
    }
    public static processDistanceFieldWithArray(input: Image, out: IndexedColorImageUncompressed, t: number): boolean {
        if (input.getXSize() !== out.getXSize() || input.getYSize() !== out.getYSize()) return false;
        const inside: [number, number][] = [];
        for (let x = 0; x < input.getXSize(); x++)
            for (let y = 0; y < input.getYSize(); y++) {
                const p = input.getPixelRgb(x, y);
                if (
                    (VSDK.signedByte2unsignedInteger(p.r) +
                        VSDK.signedByte2unsignedInteger(p.g) +
                        VSDK.signedByte2unsignedInteger(p.b)) /
                        3 >=
                    t
                )
                    inside.push([x, y]);
            }
        const max = Math.hypot(input.getXSize(), input.getYSize());
        for (let x = 0; x < input.getXSize(); x++)
            for (let y = 0; y < input.getYSize(); y++) {
                let d = max;
                for (const [a, b] of inside) d = Math.min(d, Math.hypot(a - x, b - y));
                out.putPixel(x, y, Math.trunc((d / max) * 255));
            }
        return true;
    }
    public static processDistanceField(i: Image, o: IndexedColorImageUncompressed, t: number): boolean {
        if (i === null || o === null || i.getXSize() !== o.getXSize() || i.getYSize() !== o.getYSize()) return false;
        const width = i.getXSize(),
            height = i.getYSize();
        const maximumDistanceSquared = width * width + height * height;
        const maximumDistance = Math.sqrt(maximumDistanceSquared);
        for (let x = 0; x < width; x++)
            for (let y = 0; y < height; y++) {
                let minimumDistanceSquared = maximumDistanceSquared;
                for (let xx = 0; xx < width; xx++)
                    for (let yy = 0; yy < height; yy++) {
                        const pixel = i.getPixelRgb(xx, yy);
                        const value =
                            (VSDK.signedByte2unsignedInteger(pixel.r) +
                                VSDK.signedByte2unsignedInteger(pixel.g) +
                                VSDK.signedByte2unsignedInteger(pixel.b)) /
                            3;
                        if (value >= t) {
                            const distanceSquared = (xx - x) * (xx - x) + (yy - y) * (yy - y);
                            if (distanceSquared < minimumDistanceSquared) minimumDistanceSquared = distanceSquared;
                        }
                    }
                o.putPixel(
                    x,
                    y,
                    VSDK.unsigned8BitInteger2signedByte(
                        Math.trunc((Math.sqrt(minimumDistanceSquared) / maximumDistance) * 255),
                    ),
                );
            }
        return true;
    }
}
