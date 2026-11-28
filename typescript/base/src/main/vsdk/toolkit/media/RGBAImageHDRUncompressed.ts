import { RGBAPixelHDR } from "./RGBAPixelHDR.js";
import { checkArrayIndex } from "./_pixel.js";
export class RGBAImageHDRUncompressed {
    private xSize = 0;
    private ySize = 0;
    private data: Uint16Array | null = null;
    public getXSize(): number {
        return this.xSize;
    }
    public getYSize(): number {
        return this.ySize;
    }
    public allocate(w: number, h: number): void {
        this.xSize = w;
        this.ySize = h;
        this.data = new Uint16Array(w * h * 4);
    }
    public getPixel(x: number, y: number, p: RGBAPixelHDR): void {
        const i = (y * this.xSize + x) * 4,
            d = this.data!;
        checkArrayIndex(i, d.length);
        p.r = d[i]!;
        checkArrayIndex(i + 1, d.length);
        p.g = d[i + 1]!;
        checkArrayIndex(i + 2, d.length);
        p.b = d[i + 2]!;
        checkArrayIndex(i + 3, d.length);
        p.a = d[i + 3]!;
    }
    public setPixel(x: number, y: number, p: RGBAPixelHDR): void {
        const i = (y * this.xSize + x) * 4,
            d = this.data!;
        checkArrayIndex(i, d.length);
        d[i] = p.r;
        checkArrayIndex(i + 1, d.length);
        d[i + 1] = p.g;
        checkArrayIndex(i + 2, d.length);
        d[i + 2] = p.b;
        checkArrayIndex(i + 3, d.length);
        d[i + 3] = p.a;
    }
}
