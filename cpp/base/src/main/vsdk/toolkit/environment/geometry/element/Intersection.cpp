#include "vsdk/toolkit/environment/geometry/element/Intersection.h"

Intersection::Intersection(double inT, const Vector3Dd& inPoint, const Vector3Dd& inNormal)
    : t(inT), point(inPoint), normal(inNormal) {}
