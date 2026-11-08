/** Packed RGB uses 8-bit channels; palette channels use the VGA range 0..63. */
export enum DitheringMethod { Nearest, Ordered, FloydSteinberg }

export class DitheringPalette {
    public count = 0;
    public readonly rgb = new Int32Array(256 * 3);
}

interface CutBox { first: number; end: number; pixels: number; low: number[]; high: number[]; longest: number; score: number }

class Finder {
    private readonly palette: Int32Array;
    private readonly cache = new Int32Array(32768).fill(-1);
    public constructor(private readonly source: DitheringPalette) {
        this.palette = new Int32Array(source.count * 3);
        for (let i = 0; i < this.palette.length; i++) {
            const v = source.rgb[i]! & 63;
            this.palette[i] = (v << 2) | (v >> 4);
        }
    }
    public component(index: number, channel: number): number { return this.palette[index * 3 + channel]!; }
    public nearest(r: number, g: number, b: number): number {
        const key = ((r >> 3) << 10) | ((g >> 3) << 5) | (b >> 3);
        if (this.cache[key]! >= 0) return this.cache[key]!;
        let best = 0, distance = 1 << 30;
        for (let i = 0; i < this.source.count; i++) {
            const dr = r - this.palette[i * 3]!, dg = g - this.palette[i * 3 + 1]!, db = b - this.palette[i * 3 + 2]!;
            const d = dr * dr + dg * dg + db * db;
            if (d < distance) { distance = d; best = i; }
        }
        this.cache[key] = best;
        return best;
    }
}

export class Dithering {
    public static readonly MAX_COLORS = 256;
    private static bayer(x: number, y: number): number {
        const mixed = x ^ y;
        let v = 0;
        for (let bit = 0; bit < 3; bit++) v = (v << 2) | (((y >> bit) & 1) << 1) | ((mixed >> bit) & 1);
        return v;
    }
    private static clamp(v: number): number { return Math.max(0, Math.min(255, v)); }
    public static orderedRgb565(rgb: Uint8Array, width: number, height: number): void {
        for (let y = 0; y < height; y++) for (let x = 0; x < width; x++) {
            const pixel = (y * width + x) * 3, threshold = this.bayer(x % 8, y % 8);
            for (let c = 0; c < 3; c++) {
                const levels = c === 1 ? 63 : 31, scaled = rgb[pixel + c]! * levels;
                let value = Math.floor(scaled / 255);
                if ((scaled % 255) * 64 > threshold * 255 && value < levels) value++;
                rgb[pixel + c] = Math.floor((value * 255 + levels - 1) / levels);
            }
        }
    }
    public static convert(method: DitheringMethod, rgb: Uint8Array, width: number, height: number,
                          palette: DitheringPalette, indices: Uint8Array): boolean {
        if (!Number.isInteger(palette.count) || palette.count <= 0 || palette.count > 256 || !Number.isInteger(width) || !Number.isInteger(height) ||
            width < 0 || height < 0 || rgb.length < width * height * 3 || indices.length < width * height) return false;
        if (method !== DitheringMethod.Nearest && method !== DitheringMethod.Ordered && method !== DitheringMethod.FloydSteinberg) return false;
        const finder = new Finder(palette);
        let levels = 2;
        while (levels * levels * levels < palette.count) levels++;
        const spread = Math.floor(255 / (levels - 1));
        if (method !== DitheringMethod.FloydSteinberg) {
            for (let y = 0; y < height; y++) for (let x = 0; x < width; x++) {
                const i = y * width + x, p = i * 3;
                const offset = method === DitheringMethod.Ordered ? Math.trunc((this.bayer(x % 8, y % 8) * 2 + 1 - 64) * spread / 128) : 0;
                indices[i] = finder.nearest(this.clamp(rgb[p]! + offset), this.clamp(rgb[p + 1]! + offset), this.clamp(rgb[p + 2]! + offset));
            }
            return true;
        }
        let current = new Int32Array((width + 2) * 3), next = new Int32Array((width + 2) * 3);
        const wanted = [0, 0, 0];
        for (let y = 0; y < height; y++) {
            const step = (y & 1) === 0 ? 1 : -1;
            next.fill(0);
            for (let i = 0; i < width; i++) {
                const x = step === 1 ? i : width - 1 - i, p = (y * width + x) * 3, here = (x + 1) * 3;
                for (let c = 0; c < 3; c++) wanted[c] = this.clamp(rgb[p + c]! + Math.trunc(current[here + c]! / 16));
                const index = finder.nearest(wanted[0]!, wanted[1]!, wanted[2]!);
                indices[y * width + x] = index;
                for (let c = 0; c < 3; c++) {
                    const error = wanted[c]! - finder.component(index, c);
                    current[here + step * 3 + c]! += error * 7;
                    next[here - step * 3 + c]! += error * 3;
                    next[here + c]! += error * 5;
                    next[here + step * 3 + c]! += error;
                }
            }
            const swap = current; current = next; next = swap;
        }
        return true;
    }
    public static optimizePalette(rgb: Uint8Array, width: number, height: number, colors: number, out: DitheringPalette): boolean {
        if (colors <= 0 || colors > 256 || !Number.isInteger(colors) || !Number.isInteger(width) || !Number.isInteger(height) ||
            width < 0 || height < 0 || rgb.length < width * height * 3) return false;
        const histogram = new Uint32Array(32768);
        for (let i = 0; i < width * height; i++) histogram[((rgb[i * 3]! >> 3) << 10) | ((rgb[i * 3 + 1]! >> 3) << 5) | (rgb[i * 3 + 2]! >> 3)]!++;
        const entries: {key: number; count: number}[] = [];
        for (let key = 0; key < histogram.length; key++) if (histogram[key]) entries.push({key, count: histogram[key]!});
        const channel = (key: number, c: number): number => (key >> (10 - c * 5)) & 31;
        const measure = (box: CutBox): void => {
            box.pixels = 0; box.low = [31, 31, 31]; box.high = [0, 0, 0];
            for (let i = box.first; i < box.end; i++) {
                const e = entries[i]!; box.pixels += e.count;
                for (let c = 0; c < 3; c++) { const v = channel(e.key, c); box.low[c] = Math.min(box.low[c]!, v); box.high[c] = Math.max(box.high[c]!, v); }
            }
            box.longest = 0;
            for (let c = 1; c < 3; c++) if (box.high[c]! - box.low[c]! > box.high[box.longest]! - box.low[box.longest]!) box.longest = c;
            box.score = Math.min(0xffffffff, box.pixels * (box.high[box.longest]! - box.low[box.longest]!));
        };
        const boxes: CutBox[] = [];
        const newBox = (first: number, end: number): CutBox => ({first, end, pixels: 0, low: [], high: [], longest: 0, score: 0});
        if (entries.length) { const box = newBox(0, entries.length); measure(box); boxes.push(box); }
        while (boxes.length && boxes.length < colors) {
            let best = 0;
            for (let b = 1; b < boxes.length; b++) if (boxes[b]!.score > boxes[best]!.score) best = b;
            const box = boxes[best]!; if (!box.score) break;
            const c = box.longest, perValue = new Float64Array(32);
            for (let i = box.first; i < box.end; i++) perValue[channel(entries[i]!.key, c)]! += entries[i]!.count;
            let split = box.low[c]!, seen = 0;
            while (split < box.high[c]! - 1 && (seen += perValue[split]!) * 2 < box.pixels) split++;
            let i = box.first, j = box.end;
            while (i < j) { if (channel(entries[i]!.key, c) <= split) i++; else { j--; const e = entries[i]!; entries[i] = entries[j]!; entries[j] = e; } }
            const upper = newBox(i, box.end); box.end = i; measure(box); measure(upper); boxes.push(upper);
        }
        out.count = colors;
        for (let b = 0; b < colors; b++) {
            const sum = [0, 0, 0];
            if (b < boxes.length) {
                const box = boxes[b]!;
                for (let i = box.first; i < box.end; i++) for (let c = 0; c < 3; c++) {
                    const v = channel(entries[i]!.key, c); sum[c]! += ((v << 3) | (v >> 2)) * entries[i]!.count;
                }
                for (let c = 0; c < 3; c++) out.rgb[b * 3 + c] = Math.floor(sum[c]! / box.pixels) >> 2;
            } else for (let c = 0; c < 3; c++) out.rgb[b * 3 + c] = boxes.length ? out.rgb[c]! : 0;
        }
        return true;
    }
}
