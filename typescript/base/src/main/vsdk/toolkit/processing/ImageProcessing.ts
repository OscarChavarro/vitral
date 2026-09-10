import { ProcessingElement } from "./ProcessingElement.js";
import { Image } from "../media/Image.js";
import { IndexedColorImageUncompressed } from "../media/IndexedColorImageUncompressed.js";
import { RGBImageUncompressed } from "../media/RGBImageUncompressed.js";
import { RGBPixel } from "../media/RGBPixel.js";
import { VSDK } from "../common/VSDK.js";
export abstract class ImageProcessing extends ProcessingElement {
    private static gamma(v: number, g: number): number {
        return Math.trunc(Math.pow(v / 255, 1 / g) * 255);
    }
    public static gammaCorrection(img: IndexedColorImageUncompressed | RGBImageUncompressed, g: number): void {
        for (let x = 0; x < img.getXSize(); x++)
            for (let y = 0; y < img.getYSize(); y++) {
                if (img instanceof IndexedColorImageUncompressed) img.putPixel(x, y, this.gamma(img.getPixel(x, y), g));
                else {
                    const p = img.getPixelRgb(x, y);
                    p.r = VSDK.unsigned8BitInteger2signedByte(this.gamma(VSDK.signedByte2unsignedInteger(p.r), g));
                    p.g = VSDK.unsigned8BitInteger2signedByte(this.gamma(VSDK.signedByte2unsignedInteger(p.g), g));
                    p.b = VSDK.unsigned8BitInteger2signedByte(this.gamma(VSDK.signedByte2unsignedInteger(p.b), g));
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
        for (let x = 0; x < wo; x++)
            for (let y = 0; y < ho; y++) {
                const c = input.getColorRgbBiLinear(x / wo, y / ho),
                    p = new RGBPixel();
                p.importFromColorRgb(c);
                out.putPixelRgb(x, y, p);
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
        return this.processDistanceFieldWithArray(i, o, t);
    }
}
