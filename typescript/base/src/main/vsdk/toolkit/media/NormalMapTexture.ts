import { NormalMap } from "./NormalMap.js";
import { RGBImageUncompressed } from "./RGBImageUncompressed.js";

/** Float normal-map snapshot with an RGB8 preview. */
export class NormalMapTexture extends RGBImageUncompressed {
    public readonly samples: Float32Array;
    public constructor(source: NormalMap) {
        super();
        const w = source.getXSize(), h = source.getYSize();
        this.init(w, h);
        this.samples = new Float32Array(w * h * 3);
        let i = 0;
        for (let y = h - 1; y >= 0; y--) for (let x = 0; x < w; x++) {
            const n = source.getNormal(x, y)!.normalized();
            const r = (n.x() + 1) / 2, g = (n.y() + 1) / 2, b = (n.z() + 1) / 2;
            this.samples[i++] = r; this.samples[i++] = g; this.samples[i++] = b;
            this.putPixel(x, y, r * 255, g * 255, b * 255);
        }
    }
}
