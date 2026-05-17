#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_ELEMENTS_RAYHIT_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_ELEMENTS_RAYHIT_H__

#include "Ray.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class SimpleMaterial;
class Image;
class NormalMap;

/**
RayHit describes the result of a ray/geometry intersection.
*/
class RayHit {
public:
    static const int DETAIL_NONE = 0;
    static const int DETAIL_POINT = 1 << 0;
    static const int DETAIL_NORMAL = 1 << 1;
    static const int DETAIL_UV = 1 << 2;
    static const int DETAIL_TANGENT = 1 << 3;
    static const int DETAIL_ALL = DETAIL_POINT | DETAIL_NORMAL | DETAIL_UV | DETAIL_TANGENT;

    Vector3Dd p; // Intersection point coordinates
    Vector3Dd n; // Surface normal at intersection point
    Vector3Dd t; // Surface tangent at intersection point
    // Note that surface binormal at intersection point must be calculated
    // by the application as the cross product (n x t).

    double u; // Texture coordinate of intersection point
    double v;

    // This can be null.
    SimpleMaterial* material; // Internal geometry selected material

    // This can be null.
    Image* texture; // Internal geometry selected texture map

    // This can be null.
    NormalMap* normalMap; // Internal geometry selected normal map

private:
    Ray rayValue_;
    bool hasRay_;

    int requiredDetailMask_;
    bool storeRay_;
    double hitDistance_;
    bool hasHitDistance_;

public:
    RayHit();
    explicit RayHit(int requiredDetailMask);
    RayHit(int requiredDetailMask, bool storeRay);
    RayHit(const RayHit& other);
    ~RayHit();

    void clear();
    void reset(int newRequiredDetailMask);
    void resetForDistanceOnly();

    void clone(const RayHit& other);

    int requiredDetailMask() const;
    void setRequiredDetailMask(int requiredDetailMask);

    bool shouldStoreRay() const;
    void setStoreRay(bool storeRay);

    bool needsPoint() const;
    bool needsNormal() const;
    bool needsTextureCoordinates() const;
    bool needsTangent() const;
    bool needsAnySurfaceData() const;

    const Ray* ray() const;
    void setRay(const Ray& ray);

    bool hasHitDistance() const;
    double hitDistance() const;
    void setHitDistance(double hitDistance);

};

#endif
