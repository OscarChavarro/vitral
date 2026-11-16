//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "java/lang/Math.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidVertexNeighborhood.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

_PolyhedralBoundedSolidVertexNeighborhood::_PolyhedralBoundedSolidVertexNeighborhood()
{
}

void _PolyhedralBoundedSolidVertexNeighborhood::classify(
    _PolyhedralBoundedSolidVertex* vtx,
    const InfinitePlane& plane,
    ArrayList<_PolyhedralBoundedSolidSectorClassification>& neighbors)
{
    _PolyhedralBoundedSolidHalfEdge* he = vtx->emanatingHalfEdge;

    do {
        if ( isReflexSector(he) ) {
            Vector3Dd bisect = inwardReflexBisector(he);
            _PolyhedralBoundedSolidSectorClassification c;
            c.sector = he;
            c.classification = compareToZero(plane.pointDistance(bisect));
            c.isWide = TRUE;
            c.position = bisect;
            c.situation =
                _PolyhedralBoundedSolidSectorClassification::CROSSING_EDGE;
            neighbors.add(c);
        }
        Vector3Dd farPoint = he->next()->startingVertex->position;
        _PolyhedralBoundedSolidSectorClassification c;
        c.sector = he;
        c.classification = compareToZero(plane.pointDistance(farPoint));
        c.isWide = FALSE;
        c.position = farPoint;
        if ( c.classification ==
             _PolyhedralBoundedSolidSectorClassification::ON ) {
            c.situation =
                _PolyhedralBoundedSolidSectorClassification::INPLANE_EDGE;
        }
        else {
            c.situation =
                _PolyhedralBoundedSolidSectorClassification::UNDEFINED;
        }
        neighbors.add(c);
        he = he->mirrorHalfEdge()->next();
    } while ( he != vtx->emanatingHalfEdge );
}

BOOLEAN _PolyhedralBoundedSolidVertexNeighborhood::isReflexSector(
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
        return FALSE;
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
    double reach = 0.5 * Math::min(toNext.length(), toPrevious.length());
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
    if ( length > 0.0 ) {
        return v.multiply(1.0 / length);
    }
    return v;
}

int _PolyhedralBoundedSolidVertexNeighborhood::compareToZero(double value)
{
    return PolyhedralBoundedSolidNumericPolicy::compareToZero(value,
        numericContext);
}
