#ifndef __RAY_HIT__
#define __RAY_HIT__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
class SimpleMaterial;
class Image;
class NormalMap;

/**
RayHit describes the result of a ray/geometry intersection.
*/
class RayHit {
public:
    enum {
        DETAIL_NONE = 0,
        DETAIL_POINT = 1 << 0,
        DETAIL_NORMAL = 1 << 1,
        DETAIL_UV = 1 << 2,
        DETAIL_TANGENT = 1 << 3,
        DETAIL_ALL = DETAIL_POINT | DETAIL_NORMAL | DETAIL_UV | DETAIL_TANGENT
    };

    Vector3Dd point; // Intersection point coordinates
    Vector3Dd normal; // Surface normal at intersection point
    Vector3Dd tangent; // Surface tangent at intersection point
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
    Ray ray;
    BOOLEAN hasRay;

    int requiredDetailMask;
    BOOLEAN storeRay;
    double hitDistance;
    BOOLEAN hitDistanceKnown;

public:
    RayHit();
    explicit RayHit(int requiredDetailMask);
    RayHit(int requiredDetailMask, BOOLEAN storeRay);
    RayHit(const RayHit& other);
    ~RayHit();

    void clear();
    void reset(int newRequiredDetailMask);
    void resetForDistanceOnly();

    void clone(const RayHit& other);

    int getRequiredDetailMask() const;
    void setRequiredDetailMask(int value);

    BOOLEAN shouldStoreRay() const;
    void setStoreRay(BOOLEAN value);

    BOOLEAN needsPoint() const;
    BOOLEAN needsNormal() const;
    BOOLEAN needsTextureCoordinates() const;
    BOOLEAN needsTangent() const;
    BOOLEAN needsAnySurfaceData() const;

    const Ray* getRay() const;
    void setRay(const Ray& value);

    BOOLEAN hasHitDistance() const;
    double getHitDistance() const;
    void setHitDistance(double value);

};

#endif
