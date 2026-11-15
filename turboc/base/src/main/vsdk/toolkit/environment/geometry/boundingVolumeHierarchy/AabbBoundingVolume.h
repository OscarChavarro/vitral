#ifndef __AABB_BOUNDING_VOLUME__
#define __AABB_BOUNDING_VOLUME__

#include "vsdk/toolkit/common/VSDK.h"

#include "vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/AxisAlignedBoundingBox.h"
#include "vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/BoundingVolumeHierarchy.h"

/** Bounding volume made of one axis aligned box. */
class AabbBoundingVolume : public BoundingVolumeHierarchy {
  private:
    AxisAlignedBoundingBox bounds;

  public:
    AabbBoundingVolume();
    explicit AabbBoundingVolume(const AxisAlignedBoundingBox &bounds);
    AabbBoundingVolume(const Vector3Dd &min, const Vector3Dd &max);

    const AxisAlignedBoundingBox &getBounds() const;

    virtual BOOLEAN mayIntersect(const Ray &ray) const;
    virtual BOOLEAN mayIntersectBefore(const Ray &ray, double maxT) const;
    virtual BOOLEAN containsPoint(const Vector3Dd &point, double tolerance) const;
    virtual BOOLEAN isUnbounded() const;
    virtual AxisAlignedBoundingBox axisAlignedExtent() const;
    virtual BoundingVolumeHierarchy *copy() const;
};

inline const AxisAlignedBoundingBox &AabbBoundingVolume::getBounds() const
{
    return bounds;
}

#endif
