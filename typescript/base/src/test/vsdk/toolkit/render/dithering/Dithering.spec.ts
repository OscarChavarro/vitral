import { describe, expect, it } from "vitest";
import { Dithering, DitheringMethod, DitheringPalette } from "vsdk/toolkit/render/dithering/Dithering.js";

function hash(values: ArrayLike<number>): number {
    let value = 0x811c9dc5;
    for (let i = 0; i < values.length; i++) value = Math.imul(value ^ values[i]!, 0x01000193) >>> 0;
    return value;
}

describe("Dithering", () => {
    it("matches the C++ palette and conversion results", () => {
        const width = 47, height = 19, rgb = new Uint8Array(width * height * 3), indices = new Uint8Array(width * height);
        for (let i = 0; i < rgb.length; i++) rgb[i] = (i * 73 + Math.floor(i / 7) * 19 + 11) % 256;
        const expected = [
            [0x82e89bfe, 0x20ce637d, 0xe6e355b9, 0x6476640c],
            [0x91e3e123, 0x48fded21, 0x99eab687, 0xd011121c],
            [0x4a6012d3, 0x919e026d, 0x738c2aec, 0x4bc57a28],
        ];
        for (const [t, count] of [2, 16, 256].entries()) {
            const palette = new DitheringPalette();
            expect(Dithering.optimizePalette(rgb, width, height, count, palette)).toBe(true);
            expect(hash(palette.rgb.subarray(0, count * 3))).toBe(expected[t]![0]);
            for (const method of [DitheringMethod.Nearest, DitheringMethod.Ordered, DitheringMethod.FloydSteinberg]) {
                expect(Dithering.convert(method, rgb, width, height, palette, indices)).toBe(true);
                expect(hash(indices)).toBe(expected[t]![method + 1]);
            }
        }
        Dithering.orderedRgb565(rgb, width, height);
        expect(hash(rgb)).toBe(0x4292ca03);
    });
    it("rejects invalid palettes and undersized images", () => {
        const palette = new DitheringPalette();
        expect(Dithering.convert(DitheringMethod.Nearest, new Uint8Array(3), 1, 1, palette, new Uint8Array(1))).toBe(false);
        expect(Dithering.optimizePalette(new Uint8Array(2), 1, 1, 2, palette)).toBe(false);
    });
});
