#include <stddef.h>
#include "vsdk/toolkit/environment/geometry/curve/Curve.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
Ray* Curve::doIntersectionFirstHit(const Ray&)
{
    return NULL;
}

bool Curve::doIntersectionFirstHit(const Ray& inRay, RayHit* outHit)
{
    Ray* hit = doIntersectionFirstHit(inRay);
    if ( hit == NULL ) {
        return false;
    }

    if ( outHit != NULL ) {
        outHit->setRay(*hit);
        doExtraInformation(*hit, hit->getT(), outHit);
    }
    return true;
}

void Curve::doExtraInformation(const Ray&, double, RayHit*)
{
}

const Class Curve::CLASS = { "Curve", &Geometry::CLASS };

const Class *
Curve::getClass() const
{
    return &CLASS;
}
