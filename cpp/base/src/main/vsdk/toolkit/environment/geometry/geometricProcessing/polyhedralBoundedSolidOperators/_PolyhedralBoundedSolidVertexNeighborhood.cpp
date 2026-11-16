//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#include "java/lang/Math.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidVertexNeighborhood.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"

bool _PolyhedralBoundedSolidVertexNeighborhood::isReflexSector(
    _PolyhedralBoundedSolidHalfEdge* he)
{
    Vector3Dd v = he->startingVertex->position;
    Vector3Dd a = he->next()->startingVertex->position.subtract(v);
    Vector3Dd b = he->previous()->startingVertex->position.subtract(v);
    InfinitePlane* plane = he->parentLoop->parentFace->getContainingPlane();
    Vector3Dd n = plane->getNormal();
    delete plane;
    Vector3Dd axb = a.crossProduct(b);

    if ( axb.length() <= numericContext.unitVectorTolerance() *
             a.length() * b.length() ) {
        return false;
    }
    return axb.dotProduct(n) < 0.0;
}

Vector3Dd _PolyhedralBoundedSolidVertexNeighborhood::inwardReflexBisector(
    _PolyhedralBoundedSolidHalfEdge* he)
{
    Vector3Dd v = he->startingVertex->position;
    Vector3Dd toNext = he->next()->startingVertex->position.subtract(v);
    Vector3Dd toPrevious =
        he->previous()->startingVertex->position.subtract(v);
    double reach = 0.5 * java::Math::min(toNext.length(), toPrevious.length());
    Vector3Dd a = unit(toNext);
    Vector3Dd b = unit(toPrevious);
    Vector3Dd sum = a.add(b);

    if ( sum.length() <= numericContext.unitVectorTolerance() ) {
        // Straight angle: any inward normal works.
        InfinitePlane* plane =
            he->parentLoop->parentFace->getContainingPlane();
        sum = plane->getNormal().crossProduct(a).multiply(-1.0);
        delete plane;
    }
    return v.subtract(unit(sum).multiply(reach));
}

Vector3Dd _PolyhedralBoundedSolidVertexNeighborhood::unit(const Vector3Dd& v)
{
    double length = v.length();
    return length > 0.0 ? v.multiply(1.0 / length) : v;
}

int _PolyhedralBoundedSolidVertexNeighborhood::compareToZero(double value)
{
    return PolyhedralBoundedSolidNumericPolicy::compareToZero(value,
        numericContext);
}
