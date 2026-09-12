import { RGBAPixelHDR } from "./RGBAPixelHDR.js";

/**
Port of `vsdk.toolkit.media.IndexedColorImageHDRUncompressed`.

A palette-indexed image whose color table holds high-dynamic-range pixels.
Java's `byte[]` of indexes is a `Uint8Array` here, which removes the `& 0xff`
of the getter and the `(byte)` cast of the setter; the `& 0xff` of the setter
is kept, because it is the value truncation Java performs, not a sign fix.
*/
export class IndexedColorImageHDRUncompressed {
    private data: Uint8Array | null = null;
    private xSize = 0;
    private ySize = 0;
    private colorMapSize = 0;
    private colorTable: RGBAPixelHDR[] | null = null;

    public getXSize(): number {
        return this.xSize;
    }

    public getYSize(): number {
        return this.ySize;
    }

    public setXSize(w: number): void {
        this.xSize = w;
    }

    public setYSize(h: number): void {
        this.ySize = h;
    }

    public getColorMapSize(): number {
        return this.colorMapSize;
    }

    public setColorMapSize(n: number): void {
        this.colorMapSize = n;
    }

    public getColorTable(): RGBAPixelHDR[] | null {
        return this.colorTable;
    }

    public setColorTable(ct: RGBAPixelHDR[] | null): void {
        this.colorTable = ct;
    }

    public allocate(w: number, h: number): void {
        this.xSize = w;
        this.ySize = h;
        this.data = new Uint8Array(w * h);
    }

    public getPixel(x: number, y: number): number {
        return this.data![y * this.xSize + x]!;
    }

    public setPixel(x: number, y: number, value: number): void {
        this.data![y * this.xSize + x] = value & 0xff;
    }
}
