import java.nio.FloatBuffer;
import vsdk.toolkit.media.NormalMap;
import vsdk.toolkit.media.NormalMapTexture;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;

/** Run with -PrunMainClass=NormalMapTextureRegression. */
public final class NormalMapTextureRegression {
    public static void main(String[] args) {
        NormalMap map = new NormalMap();
        map.init(2, 2);
        map.putNormal(0, 0, new Vector3Dd(0, 0, 1));
        map.putNormal(1, 0, new Vector3Dd(0.2, -0.1, 1).normalized());
        map.putNormal(0, 1, new Vector3Dd(-0.2, 0.1, 1).normalized());
        map.putNormal(1, 1, new Vector3Dd(0, 0, 1));
        FloatBuffer pixels = new NormalMapTexture(map).getFloatSamples();
        for (int y = 1; y >= 0; y--) {
            for (int x = 0; x < 2; x++) {
                Vector3Dd expected = map.getNormal(x, y);
                double[] components = {expected.x(), expected.y(), expected.z()};
                for (double component : components) {
                    double decoded = pixels.get() * 2.0 - 1.0;
                    if (Math.abs(decoded - component) > 1e-7 ||
                        (component == 0.0 && decoded != 0.0)) {
                        throw new AssertionError("Biased or incorrectly oriented normal texture");
                    }
                }
            }
        }
        System.out.println("Normal texture regression passed: flat zero, signed slopes, row orientation.");
    }
}
