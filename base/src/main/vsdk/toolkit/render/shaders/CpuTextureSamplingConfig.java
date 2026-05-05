package vsdk.toolkit.render.shaders;

import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.NormalMap;

public final class CpuTextureSamplingConfig {
    private static volatile double textureOffsetUTexels = -0.5;
    private static volatile double textureOffsetVTexels = -0.5;
    private static volatile double normalOffsetUTexels = 0.0;
    private static volatile double normalOffsetVTexels = 0.0;

    private CpuTextureSamplingConfig()
    {
    }

    public static void setTextureOffsetTexels(double uTexels, double vTexels)
    {
        textureOffsetUTexels = uTexels;
        textureOffsetVTexels = vTexels;
    }

    public static void setNormalOffsetTexels(double uTexels, double vTexels)
    {
        normalOffsetUTexels = uTexels;
        normalOffsetVTexels = vTexels;
    }

    static ColorRgb sample(Image texture, double u, double v)
    {
        if ( texture == null ) {
            return null;
        }
        double du = 0.0;
        double dv = 0.0;
        int width = texture.getXSize();
        int height = texture.getYSize();
        if ( width > 0 ) {
            du = textureOffsetUTexels / (double)width;
        }
        if ( height > 0 ) {
            dv = textureOffsetVTexels / (double)height;
        }
        return texture.getColorRgbBiLinear(u + du, v + dv);
    }

    static Vector3D sampleNormal(NormalMap normalMap, double u, double v)
    {
        if ( normalMap == null ) {
            return null;
        }
        double du = 0.0;
        double dv = 0.0;
        int width = normalMap.getXSize();
        int height = normalMap.getYSize();
        if ( width > 0 ) {
            du = normalOffsetUTexels / (double)width;
        }
        if ( height > 0 ) {
            dv = normalOffsetVTexels / (double)height;
        }
        return normalMap.getNormal(u + du, v + dv);
    }
}
