package vsdk.toolkit.render.dithering;

import java.util.Arrays;

/** Packed RGB uses 8-bit channels; palette channels use the VGA range 0..63.
 * Palette optimization uses 5-bit color bins and conversion writes one index per pixel.
 */
public final class Dithering {
    public static final int MAX_COLORS = 256;
    public enum Method { Nearest, Ordered, FloydSteinberg }
    public static final class Palette {
        public int count;
        public final int[] rgb = new int[MAX_COLORS * 3];
    }
    private static final class Finder {
        final int count;
        final int[] pal;
        final int[] cache = new int[32768];
        Finder(Palette palette) {
            count = palette.count;
            pal = new int[count * 3];
            for (int i = 0; i < pal.length; i++) {
                int v = palette.rgb[i] & 63;
                pal[i] = (v << 2) | (v >> 4);
            }
            Arrays.fill(cache, -1);
        }
        int nearest(int r, int g, int b) {
            int key = ((r >> 3) << 10) | ((g >> 3) << 5) | (b >> 3);
            if (cache[key] >= 0) return cache[key];
            int best = 0, distance = 1 << 30;
            for (int i = 0; i < count; i++) {
                int dr = r - pal[i * 3], dg = g - pal[i * 3 + 1], db = b - pal[i * 3 + 2];
                int d = dr * dr + dg * dg + db * db;
                if (d < distance) { distance = d; best = i; }
            }
            return cache[key] = best;
        }
    }
    private static final class Entry { int key, count; }
    private static final class Box {
        int first, end, longest;
        long pixels, score;
        final int[] low = new int[3], high = new int[3];
    }
    private Dithering() {}
    private static int bayer(int x, int y) {
        int mixed = x ^ y, v = 0;
        for (int bit = 0; bit < 3; bit++) v = (v << 2) | (((y >> bit) & 1) << 1) | ((mixed >> bit) & 1);
        return v;
    }
    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
    public static void orderedRgb565(byte[] rgb, int width, int height) {
        if (rgb == null) return;
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int p = (y * width + x) * 3, threshold = bayer(x % 8, y % 8);
            for (int c = 0; c < 3; c++) {
                int levels = c == 1 ? 63 : 31, scaled = (rgb[p + c] & 255) * levels, value = scaled / 255;
                if ((scaled % 255) * 64 > threshold * 255 && value < levels) value++;
                rgb[p + c] = (byte)((value * 255 + levels - 1) / levels);
            }
        }
    }
    private static boolean valid(byte[] rgb, int width, int height) {
        return rgb != null && width >= 0 && height >= 0 &&
               (width == 0 || height <= rgb.length / 3 / width);
    }
    public static boolean convert(Method method, byte[] rgb, int width, int height, Palette palette, byte[] indices) {
        if (!valid(rgb, width, height) || indices == null || (long)width * height > indices.length ||
            palette == null || palette.count <= 0 || palette.count > MAX_COLORS || method == null) return false;
        Finder finder = new Finder(palette);
        int levels = 2;
        while (levels * levels * levels < palette.count) levels++;
        int spread = 255 / (levels - 1);
        if (method != Method.FloydSteinberg) {
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                int i = y * width + x, p = i * 3;
                int offset = method == Method.Ordered ? (bayer(x % 8, y % 8) * 2 + 1 - 64) * spread / 128 : 0;
                indices[i] = (byte)finder.nearest(clamp((rgb[p] & 255) + offset), clamp((rgb[p + 1] & 255) + offset),
                                                   clamp((rgb[p + 2] & 255) + offset));
            }
            return true;
        }
        int[] current = new int[(width + 2) * 3], next = new int[(width + 2) * 3];
        int[] wanted = new int[3];
        for (int y = 0; y < height; y++) {
            int step = (y & 1) == 0 ? 1 : -1;
            Arrays.fill(next, 0);
            for (int i = 0; i < width; i++) {
                int x = step == 1 ? i : width - 1 - i, p = (y * width + x) * 3, here = (x + 1) * 3;
                for (int c = 0; c < 3; c++) wanted[c] = clamp((rgb[p + c] & 255) + current[here + c] / 16);
                int index = finder.nearest(wanted[0], wanted[1], wanted[2]);
                indices[y * width + x] = (byte)index;
                for (int c = 0; c < 3; c++) {
                    int error = wanted[c] - finder.pal[index * 3 + c];
                    current[here + step * 3 + c] += error * 7;
                    next[here - step * 3 + c] += error * 3;
                    next[here + c] += error * 5;
                    next[here + step * 3 + c] += error;
                }
            }
            int[] swap = current; current = next; next = swap;
        }
        return true;
    }
    private static int channel(Entry e, int c) { return (e.key >> (10 - c * 5)) & 31; }
    private static void measure(Entry[] entries, Box box) {
        box.pixels = 0;
        Arrays.fill(box.low, 31); Arrays.fill(box.high, 0);
        for (int i = box.first; i < box.end; i++) {
            Entry e = entries[i]; box.pixels += e.count;
            for (int c = 0; c < 3; c++) { int v = channel(e, c); box.low[c] = Math.min(box.low[c], v); box.high[c] = Math.max(box.high[c], v); }
        }
        box.longest = 0;
        for (int c = 1; c < 3; c++) if (box.high[c] - box.low[c] > box.high[box.longest] - box.low[box.longest]) box.longest = c;
        box.score = Math.min(0xffffffffL, box.pixels * (box.high[box.longest] - box.low[box.longest]));
    }
    public static boolean optimizePalette(byte[] rgb, int width, int height, int colors, Palette out) {
        if (!valid(rgb, width, height) || colors <= 0 || colors > MAX_COLORS || out == null) return false;
        int[] histogram = new int[32768];
        for (int i = 0; i < width * height; i++) histogram[((rgb[i * 3] & 255) >> 3 << 10) |
            ((rgb[i * 3 + 1] & 255) >> 3 << 5) | ((rgb[i * 3 + 2] & 255) >> 3)]++;
        Entry[] entries = new Entry[32768]; int distinct = 0;
        for (int key = 0; key < histogram.length; key++) if (histogram[key] != 0) {
            Entry e = new Entry(); e.key = key; e.count = histogram[key]; entries[distinct++] = e;
        }
        Box[] boxes = new Box[MAX_COLORS]; int boxCount = 0;
        if (distinct > 0) { Box box = new Box(); box.end = distinct; measure(entries, box); boxes[boxCount++] = box; }
        while (boxCount > 0 && boxCount < colors) {
            int best = 0;
            for (int b = 1; b < boxCount; b++) if (boxes[b].score > boxes[best].score) best = b;
            Box box = boxes[best]; if (box.score == 0) break;
            int c = box.longest; long[] perValue = new long[32];
            for (int i = box.first; i < box.end; i++) perValue[channel(entries[i], c)] += entries[i].count;
            int split = box.low[c]; long seen = 0;
            while (split < box.high[c] - 1 && (seen += perValue[split]) * 2 < box.pixels) split++;
            int i = box.first, j = box.end;
            while (i < j) { if (channel(entries[i], c) <= split) i++; else { j--; Entry e = entries[i]; entries[i] = entries[j]; entries[j] = e; } }
            Box upper = new Box(); upper.first = i; upper.end = box.end; box.end = i;
            measure(entries, box); measure(entries, upper); boxes[boxCount++] = upper;
        }
        out.count = colors;
        for (int b = 0; b < colors; b++) {
            long[] sum = new long[3];
            if (b < boxCount) {
                Box box = boxes[b];
                for (int i = box.first; i < box.end; i++) for (int c = 0; c < 3; c++) {
                    int v = channel(entries[i], c); sum[c] += (long)((v << 3) | (v >> 2)) * entries[i].count;
                }
                for (int c = 0; c < 3; c++) out.rgb[b * 3 + c] = (int)((sum[c] / box.pixels) >> 2);
            } else for (int c = 0; c < 3; c++) out.rgb[b * 3 + c] = boxCount > 0 ? out.rgb[c] : 0;
        }
        return true;
    }
}
