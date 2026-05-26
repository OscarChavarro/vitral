#include "vsdk/toolkit/environment/geometry/Geometry.h"

void Geometry::doExtraInformation(const Ray&, double, RayHit*) {}
int Geometry::computeQuantitativeInvisibility(const Vector3Dd&, const Vector3Dd&) { return 0; }
int Geometry::doContainmentTest(const Vector3Dd&, double) { return OUTSIDE; }
