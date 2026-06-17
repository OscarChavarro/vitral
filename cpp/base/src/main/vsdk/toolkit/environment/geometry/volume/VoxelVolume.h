#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_VOLUME_VOXELVOLUME_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_VOLUME_VOXELVOLUME_H__

#include "vsdk/toolkit/environment/geometry/volume/Solid.h"
class IndexedColorImageUncompressed;
class Ray;
class RayHit;
class Matrix4x4d;

class VoxelVolume : public Solid {
private:
    IndexedColorImageUncompressed** data;
    int xSize;
    int ySize;
    int zSize;

public:
    VoxelVolume();
    virtual ~VoxelVolume();

    int getXSize() const;
    int getYSize() const;
    int getZSize() const;

    bool init(int xSize, int ySize, int zSize);

    void putVoxel(int x, int y, int z, char val);
    int getVoxel(int x, int y, int z) const;

    Vector3Dd getVoxelPosition(int x, int y, int z) const;
    int getNearestIFromX(double x) const;
    int getNearestJFromY(double y) const;
    int getNearestKFromZ(double z) const;

    int getVoxelAtPosition(double x, double y, double z) const;
    int getVoxelAtPosition(const Vector3Dd& p) const;
    void putVoxelAtPosition(double x, double y, double z, char val);
    void putVoxelAtPosition(const Vector3Dd& p, char val);

    virtual double* getMinMax();
    Ray* doIntersection(const Ray& inOutRay);
    virtual bool doIntersection(const Ray& inRay, RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData);

    static Matrix4x4d getTransformFromVoxelFrameToMinMax(const double minmax[6]);
    virtual Vector3Dd doCenterOfMass();
};

#endif
