#ifndef __POLYHEDRAL_BOUNDED_SOLID_PREDICATES__
#define __POLYHEDRAL_BOUNDED_SOLID_PREDICATES__

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.h"
class PolyhedralBoundedSolid;

class PolyhedralBoundedSolidPredicates {
public:
    static bool isPointInside(
        PolyhedralBoundedSolid* solid,
        const Vector3Dd& point);

    static int quantitativeInvisibility(
        PolyhedralBoundedSolid* solid,
        const Vector3Dd& eye,
        const Vector3Dd& point);

private:
    static const int OUTSIDE = 0;
    static const int INTERIOR = 1;
    static const int ON_SURFACE = 2;

    static int classifyOnSegment(
        PolyhedralBoundedSolid* solid,
        const Vector3Dd& point,
        const PolyhedralBoundedSolidNumericPolicy::ToleranceContext& context);
};

#endif
