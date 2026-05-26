#include "vsdk/toolkit/environment/geometry/element/Intersection.h"

Intersection::Intersection() : t(0.0), point(0, 0, 0), normal(0, 0, 0) {}

Intersection::Intersection(double inT, const Vector3Dd& inPoint, const Vector3Dd& inNormal)
    : t(inT), point(inPoint), normal(inNormal) {}
