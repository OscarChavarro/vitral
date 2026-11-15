#include "UncertifiedTestBody.h"

GeometryContainment::Value UncertifiedTestBody::classifyPointCertified(const Vector3Dd&, double) const
{ return GeometryContainment::Unsupported; }

const Geometry* UncertifiedTestBody::getSolidEquivalenceGeometry() const { return getGeometry(); }

const Class UncertifiedTestBody::CLASS = { "UncertifiedTestBody", &SimpleBody::CLASS };

const Class *
UncertifiedTestBody::getClass() const
{
    return &CLASS;
}
