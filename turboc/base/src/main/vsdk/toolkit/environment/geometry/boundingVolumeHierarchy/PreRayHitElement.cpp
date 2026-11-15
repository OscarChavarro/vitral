#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/PreRayHitElement.h"

PreRayHitElement::~PreRayHitElement()
{
}

BOOLEAN PreRayHitElement::mayIntersectBefore(const Ray &ray, double maxT) const
{
    (void)maxT;
    return mayIntersect(ray);
}
