import { ColorRgb } from "../../common/color/ColorRgb.js";

const f = Math.fround;

/**
Color texture of 1, 3 or 4 channels of bytes, sampled with wrapping
bilinear weights. Single precision arithmetic (`float` in C++) is rounded
with `Math.fround`.
*/
export class Texture {
    private readonly width: number;
    private readonly height: number;
    private readonly channels: number;
    /** First bytes correspond to bottom-left pixel (as in OpenGL) */
    private readonly data: Uint8Array | null;

    public constructor();
    public constructor(inWidth: number, inHeight: number, inChannels: number, inData: ArrayLike<number> | null);
    public constructor(inWidth = 0, inHeight = 0, inChannels = 0, inData: ArrayLike<number> | null = null) {
        this.width = inWidth;
        this.height = inHeight;
        this.channels = inChannels;
        this.data = null;
        const byteCount = this.width * this.height * this.channels;
        if (byteCount <= 0 || inData === null) {
            return;
        }
        const copy = new Uint8Array(byteCount);
        for (let i = 0; i < byteCount; i++) {
            copy[i] = inData[i]!;
        }
        this.data = copy;
    }

    public getWidth(): number {
        return this.width;
    }

    public getHeight(): number {
        return this.height;
    }

    public getChannels(): number {
        return this.channels;
    }

    public getData(): Uint8Array | null {
        return this.data;
    }

    public evaluateColor(u: number, v: number): ColorRgb {
        u = f(u);
        v = f(v);
        const u1 = f(u - Math.floor(u));
        const u0 = 1.0 - u1;
        const v1 = f(v - Math.floor(v));
        const v0 = 1.0 - v1;
        let i = Math.trunc(u1 * this.width);
        let i1 = i + 1;
        let j = Math.trunc(v1 * this.height);
        let j1 = j + 1;
        if (i < 0) {
            i = 0;
        }
        if (i >= this.width) {
            i = this.width - 1;
        }
        if (j < 0) {
            j = 0;
        }
        if (j >= this.height) {
            j = this.height - 1;
        }
        if (i1 >= this.width) {
            i1 -= this.width;
        }
        if (j1 >= this.height) {
            j1 -= this.height;
        }

        const data = this.data;
        if (data === null) {
            return new ColorRgb(0.0, 0.0, 0.0);
        }

        const pixelIndex00 = (j * this.width + i) * this.channels;
        const pixelIndex01 = (j1 * this.width + i) * this.channels;
        const pixelIndex10 = (j * this.width + i1) * this.channels;
        const pixelIndex11 = (j1 * this.width + i1) * this.channels;

        const channelValue = (pixelIndex: number, channel: number): number => f(data[pixelIndex + channel]! / 255.0);

        let r00 = 0.0;
        let g00 = 0.0;
        let b00 = 0.0;
        let r10 = 0.0;
        let g10 = 0.0;
        let b10 = 0.0;
        let r01 = 0.0;
        let g01 = 0.0;
        let b01 = 0.0;
        let r11 = 0.0;
        let g11 = 0.0;
        let b11 = 0.0;

        switch (this.channels) {
            case 1:
                r00 = g00 = b00 = channelValue(pixelIndex00, 0);
                r10 = g10 = b10 = channelValue(pixelIndex10, 0);
                r01 = g01 = b01 = channelValue(pixelIndex01, 0);
                r11 = g11 = b11 = channelValue(pixelIndex11, 0);
                break;
            case 3:
            case 4:
                r00 = channelValue(pixelIndex00, 0);
                g00 = channelValue(pixelIndex00, 1);
                b00 = channelValue(pixelIndex00, 2);
                r10 = channelValue(pixelIndex10, 0);
                g10 = channelValue(pixelIndex10, 1);
                b10 = channelValue(pixelIndex10, 2);
                r01 = channelValue(pixelIndex01, 0);
                g01 = channelValue(pixelIndex01, 1);
                b01 = channelValue(pixelIndex01, 2);
                r11 = channelValue(pixelIndex11, 0);
                g11 = channelValue(pixelIndex11, 1);
                b11 = channelValue(pixelIndex11, 2);
                break;
            default:
                break;
        }

        const r = 0.25 * (u0 * v0 * r00 + u1 * v0 * r10 + u0 * v1 * r01 + u1 * v1 * r11);
        const g = 0.25 * (u0 * v0 * g00 + u1 * v0 * g10 + u0 * v1 * g01 + u1 * v1 * g11);
        const b = 0.25 * (u0 * v0 * b00 + u1 * v0 * b10 + u0 * v1 * b01 + u1 * v1 * b11);
        return new ColorRgb(r, g, b);
    }
}
