#ifndef __VSDK_TOOLKIT_MEDIA_NORMALMAP_H__
#define __VSDK_TOOLKIT_MEDIA_NORMALMAP_H__

#include "MediaEntity.h"
#include <vector>

class Vector3Dd;
class IndexedColorImageUncompressed;
class RGBImageUncompressed;

/**
This class represents a normal map, containing normal vectors for each
pixel position.
*/
class NormalMap : public MediaEntity {

private:
    int xSize;
    int ySize;
    std::vector<Vector3Dd*> data;

    Vector3Dd* bumpMapScale;

public:
    NormalMap();
    NormalMap(const NormalMap& other);
    virtual ~NormalMap();

    bool init(int width, int height);

    int getXSize() const;

    int getYSize() const;

    Vector3Dd* getBumpMapScale() const;

    void setBumpMapScale(const Vector3Dd& scale);

    void putNormal(int i, int j, const Vector3Dd& n);

    Vector3Dd* getNormal(int u, int v) const;

    /**
    Provide a bilinear interpolation scheme for normal vectors.
    @param u u coordinate in [0, 1]
    @param v v coordinate in [0, 1]
    @return interpolated normal
    */
    Vector3Dd* getNormalBiLinear(double u, double v) const;

    RGBImageUncompressed* exportToRgbImage() const;
    Vector3Dd importBumpMap(IndexedColorImageUncompressed* inBumpmap, const Vector3Dd& inScale);
    NormalMap* clone() const;
};

#endif // __VSDK_TOOLKIT_MEDIA_NORMALMAP_H__
