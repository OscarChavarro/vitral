#ifndef __CPUTEXTURESAMPLINGCONFIG__
#define __CPUTEXTURESAMPLINGCONFIG__

class ColorRgb;
class Vector3Dd;
class Image;
class NormalMap;

class CpuTextureSamplingConfig {
private:
    static double textureOffsetUTexels;
    static double textureOffsetVTexels;
    static const double NORMAL_OFFSET_U_TEXELS;
    static const double NORMAL_OFFSET_V_TEXELS;

public:
    static void setTextureOffsetTexels(double uTexels, double vTexels);
    static ColorRgb sample(Image* texture, double u, double v);
    static Vector3Dd sampleNormal(NormalMap* normalMap, double u, double v);
};

#endif
