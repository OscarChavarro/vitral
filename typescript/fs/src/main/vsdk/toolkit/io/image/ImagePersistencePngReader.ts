import { inflateSync } from "node:zlib";

/**
PNG decoder for `ImagePersistence.importRGB` and `importRGBA`.

This class has no Java counterpart: Java decodes PNG through `javax.imageio`
and the C++ port through `libpng`. As `ImagePersistencePng` writes PNG with
Vitral's own code, this reads it with Vitral's own code too, with DEFLATE
from `node:zlib`. It answers 8 bit RGBA samples, as the C++ port asks libpng
for: 16 bit samples are stripped to their high byte (`png_set_strip_16`),
palettes and gray levels below 8 bits are expanded, `tRNS` becomes alpha and
files without alpha are opaque. Every color type, bit depth and Adam7
interlacing are read.
*/

export interface DecodedPng {
    width: number;
    height: number;
    // RGBA quadruplets, top row first
    pixels: Uint8Array;
    // Whether the file has an alpha channel or a tRNS chunk
    hasAlpha: boolean;
}

const SIGNATURE: readonly number[] = [137, 80, 78, 71, 13, 10, 26, 10];

// Adam7 passes: start x, start y, step x, step y
const ADAM7: readonly (readonly number[])[] = [
    [0, 0, 8, 8],
    [4, 0, 8, 8],
    [0, 4, 4, 8],
    [2, 0, 4, 4],
    [0, 2, 2, 4],
    [1, 0, 2, 2],
    [0, 1, 1, 2],
];

export class ImagePersistencePngReader {
    /**
    @return the image, or null when the file is not a readable PNG
    */
    public static decode(data: Uint8Array): DecodedPng | null {
        try {
            return ImagePersistencePngReader.read(data);
        } catch {
            return null;
        }
    }

    private static read32(data: Uint8Array, p: number): number {
        return ((data[p]! << 24) | (data[p + 1]! << 16) | (data[p + 2]! << 8) | data[p + 3]!) >>> 0;
    }

    private static read(data: Uint8Array): DecodedPng | null {
        for (let i = 0; i < 8; i++) {
            if (data[i] !== SIGNATURE[i]) return null;
        }
        let width = 0;
        let height = 0;
        let bitDepth = 0;
        let colorType = 0;
        let interlace = 0;
        let palette: Uint8Array | null = null;
        let transparency: Uint8Array | null = null;
        const idat: Uint8Array[] = [];
        let p = 8;
        while (p + 8 <= data.length) {
            const length = ImagePersistencePngReader.read32(data, p);
            const type = String.fromCharCode(data[p + 4]!, data[p + 5]!, data[p + 6]!, data[p + 7]!);
            const chunk = data.subarray(p + 8, p + 8 + length);
            p += 12 + length;
            if (type === "IHDR") {
                width = ImagePersistencePngReader.read32(chunk, 0);
                height = ImagePersistencePngReader.read32(chunk, 4);
                bitDepth = chunk[8]!;
                colorType = chunk[9]!;
                interlace = chunk[12]!;
            } else if (type === "PLTE") {
                palette = chunk;
            } else if (type === "tRNS") {
                transparency = chunk;
            } else if (type === "IDAT") {
                idat.push(chunk);
            } else if (type === "IEND") {
                break;
            }
        }
        if (width <= 0 || height <= 0 || idat.length === 0) return null;
        const channelsByType: { [type: number]: number } = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 };
        const channels = channelsByType[colorType];
        if (channels === undefined) return null;
        if (colorType === 3 && palette === null) return null;

        const compressed = new Uint8Array(idat.reduce((total, chunk) => total + chunk.length, 0));
        let offset = 0;
        for (const chunk of idat) {
            compressed.set(chunk, offset);
            offset += chunk.length;
        }
        const inflated = inflateSync(compressed);
        const raw = new Uint8Array(inflated.buffer, inflated.byteOffset, inflated.byteLength);

        const pixels = new Uint8Array(width * height * 4);
        const bitsPerPixel = channels * bitDepth;
        const bytesPerPixel = Math.max(1, bitsPerPixel >> 3);
        let position = 0;
        const passes = interlace === 1 ? ADAM7 : [[0, 0, 1, 1]];
        for (const pass of passes) {
            const [startX, startY, stepX, stepY] = pass as [number, number, number, number];
            const passWidth = Math.ceil((width - startX) / stepX);
            const passHeight = Math.ceil((height - startY) / stepY);
            if (passWidth <= 0 || passHeight <= 0) continue;
            const rowBytes = Math.ceil((passWidth * bitsPerPixel) / 8);
            let previous = new Uint8Array(rowBytes);
            for (let row = 0; row < passHeight; row++) {
                const filter = raw[position]!;
                const line = raw.slice(position + 1, position + 1 + rowBytes);
                position += 1 + rowBytes;
                ImagePersistencePngReader.unfilter(filter, line, previous, bytesPerPixel);
                const y = startY + row * stepY;
                for (let column = 0; column < passWidth; column++) {
                    const x = startX + column * stepX;
                    ImagePersistencePngReader.storePixel(
                        line,
                        column,
                        bitDepth,
                        colorType,
                        channels,
                        palette,
                        transparency,
                        pixels,
                        (y * width + x) * 4,
                    );
                }
                previous = line;
            }
        }
        const hasAlpha = colorType === 4 || colorType === 6 || transparency !== null;
        return { width, height, pixels, hasAlpha };
    }

    private static unfilter(filter: number, line: Uint8Array, previous: Uint8Array, bpp: number): void {
        for (let i = 0; i < line.length; i++) {
            const left = i >= bpp ? line[i - bpp]! : 0;
            const up = previous[i]!;
            const upLeft = i >= bpp ? previous[i - bpp]! : 0;
            let predictor = 0;
            switch (filter) {
                case 0:
                    predictor = 0;
                    break;
                case 1:
                    predictor = left;
                    break;
                case 2:
                    predictor = up;
                    break;
                case 3:
                    predictor = (left + up) >> 1;
                    break;
                case 4: {
                    const estimate = left + up - upLeft;
                    const distanceLeft = Math.abs(estimate - left);
                    const distanceUp = Math.abs(estimate - up);
                    const distanceUpLeft = Math.abs(estimate - upLeft);
                    if (distanceLeft <= distanceUp && distanceLeft <= distanceUpLeft) predictor = left;
                    else if (distanceUp <= distanceUpLeft) predictor = up;
                    else predictor = upLeft;
                    break;
                }
                default:
                    throw new Error("unknown PNG filter type " + filter);
            }
            line[i] = (line[i]! + predictor) & 0xff;
        }
    }

    // Sample `index` of a row, at its full bit depth
    private static sample(line: Uint8Array, index: number, bitDepth: number): number {
        if (bitDepth === 8) return line[index]!;
        if (bitDepth === 16) return (line[index * 2]! << 8) | line[index * 2 + 1]!;
        const bitPosition = index * bitDepth;
        const byte = line[bitPosition >> 3]!;
        const shift = 8 - bitDepth - (bitPosition & 7);
        return (byte >> shift) & ((1 << bitDepth) - 1);
    }

    // A sample to 8 bits: the high byte of 16 bit ones, and gray levels below
    // 8 bits scaled to 0..255 (png_set_expand_gray_1_2_4_to_8)
    private static to8(value: number, bitDepth: number): number {
        if (bitDepth === 16) return value >> 8;
        if (bitDepth === 8) return value;
        return (value * 255) / ((1 << bitDepth) - 1);
    }

    private static storePixel(
        line: Uint8Array,
        column: number,
        bitDepth: number,
        colorType: number,
        channels: number,
        palette: Uint8Array | null,
        transparency: Uint8Array | null,
        out: Uint8Array,
        o: number,
    ): void {
        const first = column * channels;
        const s = (k: number): number => ImagePersistencePngReader.sample(line, first + k, bitDepth);
        const t16 = (k: number): number => (transparency![k * 2]! << 8) | transparency![k * 2 + 1]!;
        switch (colorType) {
            case 0: {
                const gray = s(0);
                const value = ImagePersistencePngReader.to8(gray, bitDepth);
                out[o] = value;
                out[o + 1] = value;
                out[o + 2] = value;
                out[o + 3] = transparency !== null && transparency.length >= 2 && gray === t16(0) ? 0 : 255;
                break;
            }
            case 2: {
                const r = s(0);
                const g = s(1);
                const b = s(2);
                out[o] = ImagePersistencePngReader.to8(r, bitDepth);
                out[o + 1] = ImagePersistencePngReader.to8(g, bitDepth);
                out[o + 2] = ImagePersistencePngReader.to8(b, bitDepth);
                const transparent =
                    transparency !== null && transparency.length >= 6 && r === t16(0) && g === t16(1) && b === t16(2);
                out[o + 3] = transparent ? 0 : 255;
                break;
            }
            case 3: {
                const index = s(0);
                out[o] = palette![index * 3] ?? 0;
                out[o + 1] = palette![index * 3 + 1] ?? 0;
                out[o + 2] = palette![index * 3 + 2] ?? 0;
                out[o + 3] = transparency !== null && index < transparency.length ? transparency[index]! : 255;
                break;
            }
            case 4: {
                const value = ImagePersistencePngReader.to8(s(0), bitDepth);
                out[o] = value;
                out[o + 1] = value;
                out[o + 2] = value;
                out[o + 3] = ImagePersistencePngReader.to8(s(1), bitDepth);
                break;
            }
            default: {
                out[o] = ImagePersistencePngReader.to8(s(0), bitDepth);
                out[o + 1] = ImagePersistencePngReader.to8(s(1), bitDepth);
                out[o + 2] = ImagePersistencePngReader.to8(s(2), bitDepth);
                out[o + 3] = ImagePersistencePngReader.to8(s(3), bitDepth);
                break;
            }
        }
    }
}
