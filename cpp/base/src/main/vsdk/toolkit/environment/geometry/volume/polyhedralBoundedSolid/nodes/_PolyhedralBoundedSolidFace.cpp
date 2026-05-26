#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"

#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"

#include "java/util/ArrayList.txx"

_PolyhedralBoundedSolidFace::_PolyhedralBoundedSolidFace(PolyhedralBoundedSolid* parent, int inId)
    : id(inId), parentSolid(parent)
{
}

_PolyhedralBoundedSolidHalfEdge* _PolyhedralBoundedSolidFace::findHalfEdge(int vn1, int vn2)
{
    for (long int i = 0; i < boundariesList.size(); ++i) {
        _PolyhedralBoundedSolidHalfEdge* he = boundariesList[i]->halfEdgeVertices(vn1, vn2);
        if ( he != 0 ) return he;
    }
    return 0;
}

_PolyhedralBoundedSolidHalfEdge* _PolyhedralBoundedSolidFace::findHalfEdge(int vn1)
{
    for (long int i = 0; i < boundariesList.size(); ++i) {
        _PolyhedralBoundedSolidHalfEdge* he = boundariesList[i]->firstHalfEdgeAtVertex(vn1);
        if ( he != 0 ) return he;
    }
    return 0;
}

bool _PolyhedralBoundedSolidFace::calculatePlane()
{
    return getContainingPlane() == 0;
}

InfinitePlane* _PolyhedralBoundedSolidFace::getContainingPlane()
{
    if ( boundariesList.size() == 0 ) return 0;
    _PolyhedralBoundedSolidLoop* loop = boundariesList[0];
    if ( loop == 0 || loop->halfEdgesList.size() < 3 ) return 0;

    Vector3Dd p0 = loop->halfEdgesList[0]->startingVertex->position;
    Vector3Dd p1 = loop->halfEdgesList[1]->startingVertex->position;
    Vector3Dd p2 = loop->halfEdgesList[2]->startingVertex->position;
    Vector3Dd n = p1.subtract(p0).crossProduct(p2.subtract(p0));
    if ( n.length() <= 1e-9 ) return 0;
    return new InfinitePlane(n.normalized(), p0);
}

int _PolyhedralBoundedSolidFace::testPointInside(const Vector3Dd&, double)
{
    return Geometry::INSIDE;
}

void _PolyhedralBoundedSolidFace::revert()
{
    for (long int i = 0; i < boundariesList.size(); ++i) {
        if ( boundariesList[i] != 0 ) boundariesList[i]->revert();
    }
}
