package vsdk.toolkit.render.dithering;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DitheringTest {
    private static int hash(byte[] values) {
        int hash = 0x811c9dc5;
        for (byte value : values) hash = (hash ^ (value & 255)) * 0x01000193;
        return hash;
    }
    @Test void matchesCppPaletteAndConversions() {
        int width = 47, height = 19;
        byte[] rgb = new byte[width * height * 3], indices = new byte[width * height];
        for (int i = 0; i < rgb.length; i++) rgb[i] = (byte)((i * 73 + i / 7 * 19 + 11) % 256);
        int[][] expected = {{0x82e89bfe, 0x20ce637d, 0xe6e355b9, 0x6476640c},
            {0x91e3e123, 0x48fded21, 0x99eab687, 0xd011121c},
            {0x4a6012d3, 0x919e026d, 0x738c2aec, 0x4bc57a28}};
        int[] counts = {2, 16, 256};
        for (int t = 0; t < counts.length; t++) {
            Dithering.Palette palette = new Dithering.Palette();
            assertTrue(Dithering.optimizePalette(rgb, width, height, counts[t], palette));
            int hash = 0x811c9dc5;
            for (int i = 0; i < counts[t] * 3; i++) hash = (hash ^ palette.rgb[i]) * 0x01000193;
            assertEquals(expected[t][0], hash);
            for (Dithering.Method method : Dithering.Method.values()) {
                assertTrue(Dithering.convert(method, rgb, width, height, palette, indices));
                assertEquals(expected[t][method.ordinal() + 1], hash(indices));
            }
        }
        Dithering.orderedRgb565(rgb, width, height);
        assertEquals(0x4292ca03, hash(rgb));
    }
    @Test void rejectsInvalidInputs() {
        Dithering.Palette palette = new Dithering.Palette();
        assertFalse(Dithering.convert(Dithering.Method.Nearest, new byte[3], 1, 1, palette, new byte[1]));
        assertFalse(Dithering.optimizePalette(new byte[2], 1, 1, 2, palette));
    }
}
