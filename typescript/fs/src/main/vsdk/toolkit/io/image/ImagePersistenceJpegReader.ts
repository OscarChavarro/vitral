/**
Baseline JPEG (JFIF) decoder for `ImagePersistence.importRGB`.

This class has no Java counterpart: Java decodes JPEG through `javax.imageio`
and the C++ port through `libjpeg`. To render the same images as the C++ port,
it follows the default decompression path of libjpeg (IJG 6b, as kept by
libjpeg-turbo) step by step:
  - sequential Huffman decoding (SOF0 and SOF1), with restart markers;
  - the accurate integer inverse DCT (`jpeg_idct_islow` of `jidctint.c`) and
    its range limit table (`prepare_range_limit_table` of `jdmaster.c`);
  - "fancy" (triangle filter) upsampling of `jdsample.c` for the h2v1 and h2v2
    chroma subsamplings, and box replication for the other ratios;
  - the YCbCr to RGB tables of `jdcolor.c`.
Progressive, lossless and arithmetic coded files are not supported.
*/

// Zig-zag order of the coefficients to their natural (row major) position
const NATURAL_ORDER: readonly number[] = [
    0, 1, 8, 16, 9, 2, 3, 10, 17, 24, 32, 25, 18, 11, 4, 5, 12, 19, 26, 33, 40, 48, 41, 34, 27, 20, 13, 6, 7, 14, 21,
    28, 35, 42, 49, 56, 57, 50, 43, 36, 29, 22, 15, 23, 30, 37, 44, 51, 58, 59, 52, 45, 38, 31, 39, 46, 53, 60, 61, 54,
    47, 55, 62, 63,
];

// jidctint.c constants (CONST_BITS = 13)
const CONST_BITS = 13;
const PASS1_BITS = 2;
const FIX_0_298631336 = 2446;
const FIX_0_390180644 = 3196;
const FIX_0_541196100 = 4433;
const FIX_0_765366865 = 6270;
const FIX_0_899976223 = 7373;
const FIX_1_175875602 = 9633;
const FIX_1_501321110 = 12299;
const FIX_1_847759065 = 15137;
const FIX_1_961570560 = 16069;
const FIX_2_053119869 = 16819;
const FIX_2_562915447 = 20995;
const FIX_3_072711026 = 25172;

// Arithmetic right shift with rounding, as the DESCALE macro of libjpeg. It
// works on doubles: the intermediate products of the IDCT can exceed 32 bits.
function descale(x: number, n: number): number {
    return Math.floor((x + 2 ** (n - 1)) / 2 ** n);
}

class HuffmanTable {
    public readonly maxCode = new Int32Array(18);
    public readonly valOffset = new Int32Array(17);
    public readonly values: Uint8Array;

    // jdhuff.c jpeg_make_d_derived_tbl
    public constructor(counts: Uint8Array, values: Uint8Array) {
        this.values = values;
        let code = 0;
        let p = 0;
        for (let l = 1; l <= 16; l++) {
            const count = counts[l - 1]!;
            if (count > 0) {
                this.valOffset[l] = p - code;
                p += count;
                code += count;
                this.maxCode[l] = code - 1;
            } else {
                this.maxCode[l] = -1;
            }
            code <<= 1;
        }
        this.maxCode[17] = 0x7fffffff;
    }
}

class JpegComponent {
    public dcTable: HuffmanTable | null = null;
    public acTable: HuffmanTable | null = null;
    public prediction = 0;
    public blocksPerLine = 0;
    public blocksPerColumn = 0;
    public downsampledWidth = 0;
    public downsampledHeight = 0;
    public samples: Uint8Array = new Uint8Array(0);

    public constructor(
        public readonly id: number,
        public readonly horizontalSampling: number,
        public readonly verticalSampling: number,
        public readonly quantTable: number,
    ) {}
}

class BitReader {
    private bitBuffer = 0;
    private bitsLeft = 0;
    // A marker ends the entropy coded data: libjpeg feeds zeros after it
    private markerFound = false;

    public constructor(
        private readonly data: Uint8Array,
        public position: number,
    ) {}

    private readBit(): number {
        if (this.bitsLeft === 0) {
            let byte = 0;
            if (!this.markerFound && this.position < this.data.length) {
                byte = this.data[this.position]!;
                if (byte === 0xff) {
                    const next = this.position + 1 < this.data.length ? this.data[this.position + 1]! : 0xd9;
                    if (next === 0x00) {
                        this.position += 2;
                    } else {
                        this.markerFound = true;
                        byte = 0;
                    }
                } else {
                    this.position++;
                }
            }
            this.bitBuffer = byte;
            this.bitsLeft = 8;
        }
        this.bitsLeft--;
        return (this.bitBuffer >> this.bitsLeft) & 1;
    }

    public receive(length: number): number {
        let value = 0;
        for (let i = 0; i < length; i++) {
            value = (value << 1) | this.readBit();
        }
        return value;
    }

    // HUFF_EXTEND
    public receiveExtend(length: number): number {
        if (length === 0) return 0;
        const value = this.receive(length);
        return value < 1 << (length - 1) ? value - (1 << length) + 1 : value;
    }

    public decode(table: HuffmanTable): number {
        let code = 0;
        for (let l = 1; l <= 16; l++) {
            code = (code << 1) | this.readBit();
            if (code <= table.maxCode[l]!) {
                return table.values[(table.valOffset[l]! + code) & 0xff]!;
            }
        }
        // Corrupt data: libjpeg warns and answers 0
        return 0;
    }

    // Skips the restart marker expected after a restart interval
    public restart(): void {
        this.bitsLeft = 0;
        this.markerFound = false;
        while (this.position + 1 < this.data.length) {
            if (
                this.data[this.position] === 0xff &&
                this.data[this.position + 1]! >= 0xd0 &&
                this.data[this.position + 1]! <= 0xd7
            ) {
                this.position += 2;
                return;
            }
            if (
                this.data[this.position] === 0xff &&
                this.data[this.position + 1] !== 0x00 &&
                this.data[this.position + 1] !== 0xff
            ) {
                // Another marker: no restart marker here
                return;
            }
            this.position++;
        }
    }
}

export interface DecodedJpeg {
    width: number;
    height: number;
    // RGB triplets, top row first
    pixels: Uint8Array;
}

export class ImagePersistenceJpegReader {
    // jdmaster.c prepare_range_limit_table, the post-IDCT part: indexed by the
    // IDCT output masked with RANGE_MASK (1023)
    private static readonly IDCT_RANGE_LIMIT: Uint8Array = ImagePersistenceJpegReader.buildIdctRangeLimit();

    private static buildIdctRangeLimit(): Uint8Array {
        const table = new Uint8Array(1024);
        for (let i = 0; i < 1024; i++) {
            if (i < 128) table[i] = 128 + i;
            else if (i < 384) table[i] = 255;
            else if (i < 896) table[i] = 0;
            else table[i] = i - 896;
        }
        return table;
    }

    /**
    @return the image, or null when the file is not a supported JPEG
    */
    public static decode(data: Uint8Array): DecodedJpeg | null {
        try {
            return new ImagePersistenceJpegReader(data).read();
        } catch {
            return null;
        }
    }

    private readonly quantTables: (Int32Array | null)[] = [null, null, null, null];
    private readonly dcTables: (HuffmanTable | null)[] = [null, null, null, null];
    private readonly acTables: (HuffmanTable | null)[] = [null, null, null, null];
    private components: JpegComponent[] = [];
    private width = 0;
    private height = 0;
    private maxH = 1;
    private maxV = 1;
    private restartInterval = 0;
    private frameRead = false;

    private constructor(private readonly data: Uint8Array) {}

    private read16(position: number): number {
        return (this.data[position]! << 8) | this.data[position + 1]!;
    }

    private read(): DecodedJpeg | null {
        const data = this.data;
        if (data.length < 4 || data[0] !== 0xff || data[1] !== 0xd8) return null;
        let position = 2;
        while (position + 3 < data.length) {
            if (data[position] !== 0xff) {
                position++;
                continue;
            }
            const marker = data[position + 1]!;
            if (marker === 0xff) {
                position++;
                continue;
            }
            if (marker === 0xd9) break;
            if (marker >= 0xd0 && marker <= 0xd7) {
                position += 2;
                continue;
            }
            const length = this.read16(position + 2);
            const segment = position + 4;
            switch (marker) {
                case 0xdb:
                    this.readQuantTables(segment, segment + length - 2);
                    break;
                case 0xc4:
                    this.readHuffmanTables(segment, segment + length - 2);
                    break;
                case 0xdd:
                    this.restartInterval = this.read16(segment);
                    break;
                case 0xc0:
                case 0xc1:
                    this.readFrame(segment);
                    break;
                case 0xc2:
                case 0xc3:
                case 0xc5:
                case 0xc6:
                case 0xc7:
                case 0xc9:
                case 0xca:
                case 0xcb:
                case 0xcd:
                case 0xce:
                case 0xcf:
                    // Progressive, lossless, hierarchical or arithmetic coded
                    return null;
                case 0xda:
                    position = this.readScan(segment);
                    continue;
                default:
                    break;
            }
            position += 2 + length;
        }
        if (!this.frameRead) return null;
        return this.toRgb();
    }

    private readQuantTables(start: number, end: number): void {
        let p = start;
        while (p < end) {
            const precision = this.data[p]! >> 4;
            const id = this.data[p]! & 15;
            p++;
            const table = new Int32Array(64);
            for (let i = 0; i < 64; i++) {
                let value: number;
                if (precision === 0) {
                    value = this.data[p]!;
                    p++;
                } else {
                    value = this.read16(p);
                    p += 2;
                }
                table[NATURAL_ORDER[i]!] = value;
            }
            this.quantTables[id & 3] = table;
        }
    }

    private readHuffmanTables(start: number, end: number): void {
        let p = start;
        while (p < end) {
            const tableClass = this.data[p]! >> 4;
            const id = this.data[p]! & 15;
            p++;
            const counts = this.data.slice(p, p + 16);
            p += 16;
            let total = 0;
            for (let i = 0; i < 16; i++) total += counts[i]!;
            const values = this.data.slice(p, p + total);
            p += total;
            const table = new HuffmanTable(counts, values);
            if (tableClass === 0) this.dcTables[id & 3] = table;
            else this.acTables[id & 3] = table;
        }
    }

    private readFrame(p: number): void {
        if (this.data[p] !== 8) throw new Error("only 8 bit JPEG samples are supported");
        this.height = this.read16(p + 1);
        this.width = this.read16(p + 3);
        const count = this.data[p + 5]!;
        this.components = [];
        for (let i = 0; i < count; i++) {
            const q = p + 6 + i * 3;
            this.components.push(
                new JpegComponent(this.data[q]!, this.data[q + 1]! >> 4, this.data[q + 1]! & 15, this.data[q + 2]!),
            );
        }
        if (this.width <= 0 || this.height <= 0 || count === 0) throw new Error("invalid JPEG frame");
        this.maxH = 1;
        this.maxV = 1;
        for (const c of this.components) {
            this.maxH = Math.max(this.maxH, c.horizontalSampling);
            this.maxV = Math.max(this.maxV, c.verticalSampling);
        }
        const mcusX = Math.ceil(this.width / (8 * this.maxH));
        const mcusY = Math.ceil(this.height / (8 * this.maxV));
        for (const c of this.components) {
            c.blocksPerLine = mcusX * c.horizontalSampling;
            c.blocksPerColumn = mcusY * c.verticalSampling;
            c.downsampledWidth = Math.ceil((this.width * c.horizontalSampling) / this.maxH);
            c.downsampledHeight = Math.ceil((this.height * c.verticalSampling) / this.maxV);
            c.samples = new Uint8Array(c.blocksPerLine * 8 * c.blocksPerColumn * 8);
        }
        this.frameRead = true;
    }

    // @return the position after the entropy coded data of the scan
    private readScan(p: number): number {
        const count = this.data[p]!;
        const scanComponents: JpegComponent[] = [];
        for (let i = 0; i < count; i++) {
            const id = this.data[p + 1 + i * 2]!;
            const tables = this.data[p + 2 + i * 2]!;
            const c = this.components.find((candidate) => candidate.id === id);
            if (c === undefined) throw new Error("unknown JPEG scan component");
            c.dcTable = this.dcTables[tables >> 4]!;
            c.acTable = this.acTables[tables & 15]!;
            if (c.dcTable === null || c.acTable === null) throw new Error("missing JPEG Huffman table");
            c.prediction = 0;
            scanComponents.push(c);
        }
        const length = this.read16(p - 2);
        const reader = new BitReader(this.data, p + length - 2);
        const coefficients = new Int32Array(64);

        let mcusX: number;
        let mcusY: number;
        const single = scanComponents.length === 1;
        if (single) {
            // Non interleaved: one block per MCU, over the component size only
            const c = scanComponents[0]!;
            mcusX = Math.ceil(c.downsampledWidth / 8);
            mcusY = Math.ceil(c.downsampledHeight / 8);
        } else {
            mcusX = Math.ceil(this.width / (8 * this.maxH));
            mcusY = Math.ceil(this.height / (8 * this.maxV));
        }
        let mcusToRestart = this.restartInterval;
        for (let mcuY = 0; mcuY < mcusY; mcuY++) {
            for (let mcuX = 0; mcuX < mcusX; mcuX++) {
                if (this.restartInterval > 0) {
                    if (mcusToRestart === 0) {
                        reader.restart();
                        for (const c of scanComponents) c.prediction = 0;
                        mcusToRestart = this.restartInterval;
                    }
                    mcusToRestart--;
                }
                for (const c of scanComponents) {
                    const blocksH = single ? 1 : c.horizontalSampling;
                    const blocksV = single ? 1 : c.verticalSampling;
                    for (let by = 0; by < blocksV; by++) {
                        for (let bx = 0; bx < blocksH; bx++) {
                            this.decodeBlock(reader, c, coefficients);
                            const blockRow = mcuY * blocksV + by;
                            const blockColumn = mcuX * blocksH + bx;
                            this.inverseDct(c, coefficients, blockRow, blockColumn);
                        }
                    }
                }
            }
        }
        // The data ends at the next marker
        let position = reader.position;
        while (position + 1 < this.data.length) {
            if (this.data[position] === 0xff && this.data[position + 1] !== 0x00) {
                const marker = this.data[position + 1]!;
                if (marker < 0xd0 || marker > 0xd7) break;
            }
            position++;
        }
        return position;
    }

    // jdhuff.c decode_mcu (sequential): coefficients in natural order
    private decodeBlock(reader: BitReader, c: JpegComponent, coefficients: Int32Array): void {
        coefficients.fill(0);
        const t = reader.decode(c.dcTable!);
        c.prediction += reader.receiveExtend(t);
        coefficients[0] = c.prediction;
        for (let k = 1; k < 64; k++) {
            const rs = reader.decode(c.acTable!);
            const r = rs >> 4;
            const s = rs & 15;
            if (s !== 0) {
                k += r;
                if (k > 63) break;
                coefficients[NATURAL_ORDER[k]!] = reader.receiveExtend(s);
            } else {
                if (r !== 15) break;
                k += 15;
            }
        }
    }

    // jidctint.c jpeg_idct_islow
    private inverseDct(c: JpegComponent, input: Int32Array, blockRow: number, blockColumn: number): void {
        const quant = this.quantTables[c.quantTable & 3];
        if (quant === null || quant === undefined) throw new Error("missing JPEG quantization table");
        const workspace = new Array<number>(64);
        const rangeLimit = ImagePersistenceJpegReader.IDCT_RANGE_LIMIT;

        // Pass 1: columns of the input, to the workspace
        for (let column = 0; column < 8; column++) {
            const q = (i: number): number => input[column + 8 * i]! * quant[column + 8 * i]!;
            if (
                input[column + 8]! === 0 &&
                input[column + 16]! === 0 &&
                input[column + 24]! === 0 &&
                input[column + 32]! === 0 &&
                input[column + 40]! === 0 &&
                input[column + 48]! === 0 &&
                input[column + 56]! === 0
            ) {
                const dcValue = q(0) * (1 << PASS1_BITS);
                for (let i = 0; i < 8; i++) workspace[column + 8 * i] = dcValue;
                continue;
            }
            let z2 = q(2);
            let z3 = q(6);
            let z1 = (z2 + z3) * FIX_0_541196100;
            let tmp2 = z1 + z3 * -FIX_1_847759065;
            let tmp3 = z1 + z2 * FIX_0_765366865;
            z2 = q(0);
            z3 = q(4);
            let tmp0 = (z2 + z3) * (1 << CONST_BITS);
            let tmp1 = (z2 - z3) * (1 << CONST_BITS);
            const tmp10 = tmp0 + tmp3;
            const tmp13 = tmp0 - tmp3;
            const tmp11 = tmp1 + tmp2;
            const tmp12 = tmp1 - tmp2;

            tmp0 = q(7);
            tmp1 = q(5);
            tmp2 = q(3);
            tmp3 = q(1);
            z1 = tmp0 + tmp3;
            z2 = tmp1 + tmp2;
            z3 = tmp0 + tmp2;
            let z4 = tmp1 + tmp3;
            const z5 = (z3 + z4) * FIX_1_175875602;
            tmp0 = tmp0 * FIX_0_298631336;
            tmp1 = tmp1 * FIX_2_053119869;
            tmp2 = tmp2 * FIX_3_072711026;
            tmp3 = tmp3 * FIX_1_501321110;
            z1 = z1 * -FIX_0_899976223;
            z2 = z2 * -FIX_2_562915447;
            z3 = z3 * -FIX_1_961570560;
            z4 = z4 * -FIX_0_390180644;
            z3 += z5;
            z4 += z5;
            tmp0 += z1 + z3;
            tmp1 += z2 + z4;
            tmp2 += z2 + z3;
            tmp3 += z1 + z4;

            const shift = CONST_BITS - PASS1_BITS;
            workspace[column] = descale(tmp10 + tmp3, shift);
            workspace[column + 56] = descale(tmp10 - tmp3, shift);
            workspace[column + 8] = descale(tmp11 + tmp2, shift);
            workspace[column + 48] = descale(tmp11 - tmp2, shift);
            workspace[column + 16] = descale(tmp12 + tmp1, shift);
            workspace[column + 40] = descale(tmp12 - tmp1, shift);
            workspace[column + 24] = descale(tmp13 + tmp0, shift);
            workspace[column + 32] = descale(tmp13 - tmp0, shift);
        }

        // Pass 2: rows of the workspace, to the samples (the shortcut of
        // libjpeg for rows without AC terms gives the same values)
        const stride = c.blocksPerLine * 8;
        const shift = CONST_BITS + PASS1_BITS + 3;
        for (let row = 0; row < 8; row++) {
            const w = (i: number): number => workspace[row * 8 + i]!;
            let z2 = w(2);
            let z3 = w(6);
            let z1 = (z2 + z3) * FIX_0_541196100;
            let tmp2 = z1 + z3 * -FIX_1_847759065;
            let tmp3 = z1 + z2 * FIX_0_765366865;
            let tmp0 = (w(0) + w(4)) * (1 << CONST_BITS);
            let tmp1 = (w(0) - w(4)) * (1 << CONST_BITS);
            const tmp10 = tmp0 + tmp3;
            const tmp13 = tmp0 - tmp3;
            const tmp11 = tmp1 + tmp2;
            const tmp12 = tmp1 - tmp2;

            tmp0 = w(7);
            tmp1 = w(5);
            tmp2 = w(3);
            tmp3 = w(1);
            z1 = tmp0 + tmp3;
            z2 = tmp1 + tmp2;
            z3 = tmp0 + tmp2;
            let z4 = tmp1 + tmp3;
            const z5 = (z3 + z4) * FIX_1_175875602;
            tmp0 = tmp0 * FIX_0_298631336;
            tmp1 = tmp1 * FIX_2_053119869;
            tmp2 = tmp2 * FIX_3_072711026;
            tmp3 = tmp3 * FIX_1_501321110;
            z1 = z1 * -FIX_0_899976223;
            z2 = z2 * -FIX_2_562915447;
            z3 = z3 * -FIX_1_961570560;
            z4 = z4 * -FIX_0_390180644;
            z3 += z5;
            z4 += z5;
            tmp0 += z1 + z3;
            tmp1 += z2 + z4;
            tmp2 += z2 + z3;
            tmp3 += z1 + z4;

            const out = (blockRow * 8 + row) * stride + blockColumn * 8;
            const samples = c.samples;
            samples[out] = rangeLimit[descale(tmp10 + tmp3, shift) & 1023]!;
            samples[out + 7] = rangeLimit[descale(tmp10 - tmp3, shift) & 1023]!;
            samples[out + 1] = rangeLimit[descale(tmp11 + tmp2, shift) & 1023]!;
            samples[out + 6] = rangeLimit[descale(tmp11 - tmp2, shift) & 1023]!;
            samples[out + 2] = rangeLimit[descale(tmp12 + tmp1, shift) & 1023]!;
            samples[out + 5] = rangeLimit[descale(tmp12 - tmp1, shift) & 1023]!;
            samples[out + 3] = rangeLimit[descale(tmp13 + tmp0, shift) & 1023]!;
            samples[out + 4] = rangeLimit[descale(tmp13 - tmp0, shift) & 1023]!;
        }
    }

    // A component at full resolution (width x height), as jdsample.c
    private upsample(c: JpegComponent): Uint8Array {
        const width = this.width;
        const height = this.height;
        const stride = c.blocksPerLine * 8;
        const inWidth = c.downsampledWidth;
        const inHeight = c.downsampledHeight;
        const input = c.samples;
        const output = new Uint8Array(width * height);
        const at = (x: number, y: number): number =>
            input[Math.min(y, inHeight - 1) * stride + Math.max(0, Math.min(x, inWidth - 1))]!;
        const hRatio = this.maxH / c.horizontalSampling;
        const vRatio = this.maxV / c.verticalSampling;

        if (hRatio === 1 && vRatio === 1) {
            for (let y = 0; y < height; y++) {
                for (let x = 0; x < width; x++) output[y * width + x] = input[y * stride + x]!;
            }
            return output;
        }
        if (hRatio === 2 && (vRatio === 1 || vRatio === 2)) {
            // h2v1_fancy_upsample and h2v2_fancy_upsample
            const row = new Uint8Array(inWidth * 2);
            for (let y = 0; y < height; y++) {
                if (vRatio === 1) {
                    const sum = (x: number): number => at(x, y);
                    ImagePersistenceJpegReader.fancyRow(row, inWidth, sum, 4, 1, 2);
                } else {
                    const inRow = y >> 1;
                    const otherRow = (y & 1) === 0 ? Math.max(inRow - 1, 0) : Math.min(inRow + 1, inHeight - 1);
                    const sum = (x: number): number => at(x, inRow) * 3 + at(x, otherRow);
                    ImagePersistenceJpegReader.fancyRow(row, inWidth, sum, 16, 8, 7);
                }
                for (let x = 0; x < width; x++) output[y * width + x] = row[x]!;
            }
            return output;
        }
        // int_upsample: box replication
        for (let y = 0; y < height; y++) {
            for (let x = 0; x < width; x++) {
                output[y * width + x] = at(Math.floor(x / hRatio), Math.floor(y / vRatio));
            }
        }
        return output;
    }

    /*
    Horizontal triangle filter of jdsample.c over the column sums `sum`:
    h2v1 (divisor 4, biases 1 and 2, sums of one sample) and h2v2 (divisor 16,
    biases 8 and 7, sums of 3 * nearer row + further row).
    */
    private static fancyRow(
        row: Uint8Array,
        inWidth: number,
        sum: (x: number) => number,
        divisor: number,
        leftBias: number,
        rightBias: number,
    ): void {
        const shift = divisor === 4 ? 2 : 4;
        let last = sum(0);
        let current = last;
        for (let x = 0; x < inWidth; x++) {
            const next = x + 1 < inWidth ? sum(x + 1) : current;
            if (x === 0) {
                row[0] = divisor === 4 ? current : (current * 4 + 8) >> 4;
            } else {
                row[2 * x] = (current * 3 + last + leftBias) >> shift;
            }
            if (x === inWidth - 1) {
                row[2 * x + 1] = divisor === 4 ? current : (current * 4 + 7) >> 4;
            } else {
                row[2 * x + 1] = (current * 3 + next + rightBias) >> shift;
            }
            last = current;
            current = next;
        }
    }

    // jdcolor.c ycc_rgb_convert (and grayscale)
    private toRgb(): DecodedJpeg {
        const width = this.width;
        const height = this.height;
        const pixels = new Uint8Array(width * height * 3);
        const planes = this.components.map((c) => this.upsample(c));
        if (planes.length === 1) {
            const gray = planes[0]!;
            for (let i = 0; i < width * height; i++) {
                pixels[i * 3] = gray[i]!;
                pixels[i * 3 + 1] = gray[i]!;
                pixels[i * 3 + 2] = gray[i]!;
            }
            return { width, height, pixels };
        }
        if (planes.length !== 3) throw new Error("only grayscale and YCbCr JPEG files are supported");
        const SCALEBITS = 16;
        const ONE_HALF = 1 << (SCALEBITS - 1);
        const fix = (x: number): number => Math.floor(x * (1 << SCALEBITS) + 0.5);
        const crR = new Int32Array(256);
        const cbB = new Int32Array(256);
        const crG = new Array<number>(256);
        const cbG = new Array<number>(256);
        for (let i = 0; i < 256; i++) {
            const x = i - 128;
            crR[i] = (fix(1.402) * x + ONE_HALF) >> SCALEBITS;
            cbB[i] = (fix(1.772) * x + ONE_HALF) >> SCALEBITS;
            crG[i] = -fix(0.71414) * x;
            cbG[i] = -fix(0.34414) * x + ONE_HALF;
        }
        const clamp = (v: number): number => (v < 0 ? 0 : v > 255 ? 255 : v);
        const [yPlane, cbPlane, crPlane] = planes as [Uint8Array, Uint8Array, Uint8Array];
        for (let i = 0; i < width * height; i++) {
            const y = yPlane[i]!;
            const cb = cbPlane[i]!;
            const cr = crPlane[i]!;
            pixels[i * 3] = clamp(y + crR[cr]!);
            pixels[i * 3 + 1] = clamp(y + ((cbG[cb]! + crG[cr]!) >> SCALEBITS));
            pixels[i * 3 + 2] = clamp(y + cbB[cb]!);
        }
        return { width, height, pixels };
    }
}
