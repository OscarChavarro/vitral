#include "vsdk/toolkit/environment/geometry/Geometry.h"

// Definitions for constants that may be bound to references.
// (i.e. by the comparisons of the tests)
const int Geometry::INSIDE;
const int Geometry::LIMIT;
const int Geometry::OUTSIDE;

void Geometry::doExtraInformation(const Ray&, double, RayHit*) {}
int Geometry::computeQuantitativeInvisibility(const Vector3Dd&, const Vector3Dd&) { return 0; }
int Geometry::doContainmentTest(const Vector3Dd&, double) { return OUTSIDE; }

const Class Geometry::CLASS = { "Geometry", &Entity::CLASS };

const Class *
Geometry::getClass() const
{
    return &CLASS;
}
