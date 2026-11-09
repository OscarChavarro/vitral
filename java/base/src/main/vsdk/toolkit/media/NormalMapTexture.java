package vsdk.toolkit.media;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;

/** Snapshot with a float RGB payload for rendering and an RGB8 preview.
 * A flat normal encodes as exactly (0.5, 0.5, 1), without RGB8 bias.
 */
public final class NormalMapTexture extends RGBImageUncompressed {
    private final FloatBuffer samples;

    public NormalMapTexture(NormalMap source) {
        int width = source.getXSize(), height = source.getYSize();
        init(width, height);
        samples = ByteBuffer.allocateDirect(width * height * 3 * Float.BYTES)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        // Match RGBImageUncompressed's bottom-up OpenGL storage.
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                Vector3Dd n = source.getNormal(x, y).normalized();
                float r = (float)((n.x() + 1) * 0.5);
                float g = (float)((n.y() + 1) * 0.5);
                float b = (float)((n.z() + 1) * 0.5);
                samples.put(r).put(g).put(b);
                putPixel(x, y, (byte)(r * 255), (byte)(g * 255), (byte)(b * 255));
            }
        }
        samples.flip();
    }

    public FloatBuffer getFloatSamples() {
        return samples.asReadOnlyBuffer();
    }
}
