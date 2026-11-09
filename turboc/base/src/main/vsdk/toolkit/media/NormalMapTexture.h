#ifndef VSDK_NORMAL_MAP_TEXTURE_H
#define VSDK_NORMAL_MAP_TEXTURE_H
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/NormalMap.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class NormalMapTexture : public RGBImageUncompressed {
    float* samples;
    NormalMapTexture(const NormalMapTexture&);
    NormalMapTexture& operator=(const NormalMapTexture&);
public:
    explicit NormalMapTexture(const NormalMap& source) : samples(0) {
        int w = source.getXSize(), h = source.getYSize();
        init(w, h);
        samples = new float[w * h * 3];
        int i = 0;
        for (int y = h - 1; y >= 0; --y) for (int x = 0; x < w; ++x) {
            Vector3Dd* sample = source.getNormal(x, y);
            Vector3Dd n = sample ? sample->normalized() : Vector3Dd(0, 0, 1);
            delete sample;
            float r = (float)((n.x() + 1) * 0.5);
            float g = (float)((n.y() + 1) * 0.5);
            float b = (float)((n.z() + 1) * 0.5);
            samples[i++] = r; samples[i++] = g; samples[i++] = b;
            putPixel(x, y, (char)(r * 255), (char)(g * 255), (char)(b * 255));
        }
    }
    virtual ~NormalMapTexture() { delete[] samples; }
    const float* getFloatSamples() const { return samples; }
};
#endif
