import { MediaEntity } from "./MediaEntity.js";
import { Vector3Dd } from "../common/linealAlgebra/Vector3Dd.js";
import { RGBImageUncompressed } from "./RGBImageUncompressed.js";
import { RGBPixel } from "./RGBPixel.js";
import { Rasterizer2D } from "../render/raster/Rasterizer2D.js";
export class Calligraphic2DBuffer extends MediaEntity {
    private lines: number[] = [];
    public init(): void {
        this.lines = [];
    }
    public add2DLine(a: Vector3Dd, b: Vector3Dd): void;
    public add2DLine(x0: number, y0: number, x1: number, y1: number): void;
    public add2DLine(a: number | Vector3Dd, b: number | Vector3Dd, c?: number, d?: number): void {
        if (a instanceof Vector3Dd) {
            this.add2DLine(a.x(), a.y(), (b as Vector3Dd).x(), (b as Vector3Dd).y());
            return;
        }
        this.lines.push(a, b as number, c!, d!, 0, 0, 0, 1);
    }
    public get2DLine(i: number): [Vector3Dd, Vector3Dd] {
        const p = i * 8;
        return [
            new Vector3Dd(this.lines[p]!, this.lines[p + 1]!, 0),
            new Vector3Dd(this.lines[p + 2]!, this.lines[p + 3]!, 0),
        ];
    }
    public getNumLines(): number {
        return this.lines.length / 8;
    }
    public exportRgbImage(inOutRasterViewport: RGBImageUncompressed): void {
        const xt: number = inOutRasterViewport.getXSize();
        const yt: number = inOutRasterViewport.getYSize();

        let e0: Vector3Dd;
        let e1: Vector3Dd;
        let x0: number, y0: number, x1: number, y1: number;
        const pixel: RGBPixel = new RGBPixel();

        pixel.r = -1;
        pixel.g = -1;
        pixel.b = -1;

        for (let j = 0; j < this.getNumLines(); j++) {
            const segment: [Vector3Dd, Vector3Dd] = this.get2DLine(j);
            e0 = segment[0];
            e1 = segment[1];
            x0 = Math.trunc((xt - 1) * ((e0.x() + 1) / 2));
            y0 = Math.trunc((yt - 1) * (1 - (e0.y() + 1) / 2));
            x1 = Math.trunc((xt - 1) * ((e1.x() + 1) / 2));
            y1 = Math.trunc((yt - 1) * (1 - (e1.y() + 1) / 2));
            Rasterizer2D.drawLine(inOutRasterViewport, x0, y0, x1, y1, pixel);
        }
    }
}
