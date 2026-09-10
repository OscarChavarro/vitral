import { MediaEntity } from "./MediaEntity.js";
import { IndexedColorImageUncompressed } from "./IndexedColorImageUncompressed.js";
import { RGBImageUncompressed } from "./RGBImageUncompressed.js";
import { RGBAImageUncompressed } from "./RGBAImageUncompressed.js";
import { RGBColorPalette } from "./RGBColorPalette.js";
import { byte } from "./_pixel.js";
export class ZBuffer extends MediaEntity {
    private depth: Float32Array;
    private xSize: number;
    private ySize: number;
    public constructor(width: number | Float32Array, height: number, maybeHeight?: number) {
        super();
        if (width instanceof Float32Array) {
            this.xSize = height;
            this.ySize = maybeHeight!;
            this.depth = new Float32Array(this.xSize * this.ySize);
            let p = 0;
            for (let y = this.ySize - 1; y >= 0; y--)
                for (let x = 0; x < this.xSize; x++) this.depth[this.xSize * y + x] = width[p++]!;
        } else {
            this.xSize = width;
            this.ySize = height;
            this.depth = new Float32Array(width * height);
        }
    }
    public getXSize(): number {
        return this.xSize;
    }
    public getYSize(): number {
        return this.ySize;
    }
    public getZBuffer(): Float32Array {
        return this.depth;
    }
    public getZ(x: number, y: number): number {
        return this.depth[this.xSize * y + x]!;
    }
    public setZBuffer(d: Float32Array): void {
        this.depth = new Float32Array(d);
    }
    public setZ(x: number, y: number, v: number): void {
        this.depth[this.xSize * y + x] = v;
    }
    public exportIndexedColorImage(): IndexedColorImageUncompressed {
        const o = new IndexedColorImageUncompressed();
        o.init(this.xSize, this.ySize);
        for (let y = 0, p = 0; y < this.ySize; y++)
            for (let x = 0; x < this.xSize; x++, p++)
                o.putPixel(x, y, Math.trunc(Math.max(0, Math.min(1, this.depth[p]!)) * 255));
        return o;
    }
    public exportRGBImage(p: RGBColorPalette): RGBImageUncompressed {
        const o = new RGBImageUncompressed();
        o.init(this.xSize, this.ySize);
        for (let y = 0, k = 0; y < this.ySize; y++)
            for (let x = 0; x < this.xSize; x++, k++) {
                const c = p.evalLinear(Math.max(0, Math.min(1, this.depth[k]!)));
                o.putPixel(x, y, byte(c.r() * 256), byte(c.g() * 256), byte(c.b() * 256));
            }
        return o;
    }
    public exportRGBAImage(p: RGBColorPalette): RGBAImageUncompressed {
        const o = new RGBAImageUncompressed();
        o.init(this.xSize, this.ySize);
        for (let y = 0, k = 0; y < this.ySize; y++)
            for (let x = 0; x < this.xSize; x++, k++) {
                const c = p.evalLinear(Math.max(0, Math.min(1, this.depth[k]!)));
                o.putPixel(x, y, byte(c.r() * 256), byte(c.g() * 256), byte(c.b() * 256));
            }
        return o;
    }
}
