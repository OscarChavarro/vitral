#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
_PolyhedralBoundedSolidLoop::_PolyhedralBoundedSolidLoop(_PolyhedralBoundedSolidFace* parent)
    : parentFace(parent), boundaryStartHalfEdge(0)
{
    if ( parentFace != 0 ) {
        parentFace->boundariesList.add(this);
    }
}

_PolyhedralBoundedSolidLoop::~_PolyhedralBoundedSolidLoop()
{
    for (long int i = 0; i < halfEdgesList.size(); ++i) {
        if (halfEdgesList[i] != 0) {
            delete halfEdgesList[i];
        }
    }
}

void _PolyhedralBoundedSolidLoop::unlistHalfEdge(_PolyhedralBoundedSolidHalfEdge* he)
{
    halfEdgesList.remove(he);
    boundaryStartHalfEdge = halfEdgesList.size() == 0 ? 0 : halfEdgesList[0];
}

_PolyhedralBoundedSolidHalfEdge* _PolyhedralBoundedSolidLoop::halfEdgeVertices(int a, int b)
{
    if ( halfEdgesList.size() == 0 ) return 0;
    for (long int i = 0; i < halfEdgesList.size(); ++i) {
        _PolyhedralBoundedSolidHalfEdge* oldhe = halfEdgesList[i];
        _PolyhedralBoundedSolidHalfEdge* he = oldhe->next();
        if ( he != 0 && oldhe->startingVertex != 0 && he->startingVertex != 0 &&
             oldhe->startingVertex->id == a && he->startingVertex->id == b ) {
            return oldhe;
        }
    }
    return 0;
}

_PolyhedralBoundedSolidHalfEdge* _PolyhedralBoundedSolidLoop::firstHalfEdgeAtVertex(int a)
{
    for (long int i = 0; i < halfEdgesList.size(); ++i) {
        _PolyhedralBoundedSolidHalfEdge* oldhe = halfEdgesList[i];
        if ( oldhe->startingVertex != 0 && oldhe->startingVertex->id == a ) {
            return oldhe;
        }
    }
    return 0;
}

void _PolyhedralBoundedSolidLoop::delhe(_PolyhedralBoundedSolidHalfEdge* he)
{
    unlistHalfEdge(he);
}

void _PolyhedralBoundedSolidLoop::revert()
{
    long int n = halfEdgesList.size();
    for (long int i = 0; i < n / 2; i++) {
        _PolyhedralBoundedSolidHalfEdge* tmp = halfEdgesList[i];
        halfEdgesList[i] = halfEdgesList[n - 1 - i];
        halfEdgesList[n - 1 - i] = tmp;
    }
    boundaryStartHalfEdge = n == 0 ? 0 : halfEdgesList[0];
}

_PolyhedralBoundedSolidHalfEdge* _PolyhedralBoundedSolidLoop::previousOf(_PolyhedralBoundedSolidHalfEdge* he) const
{
    long int n = halfEdgesList.size();
    if ( he == 0 || n == 0 ) return 0;
    for (long int i = 0; i < n; ++i) {
        if ( halfEdgesList.get(i) == he ) {
            long int j = (i == 0) ? n - 1 : i - 1;
            return halfEdgesList.get(j);
        }
    }
    return 0;
}

_PolyhedralBoundedSolidHalfEdge* _PolyhedralBoundedSolidLoop::nextOf(_PolyhedralBoundedSolidHalfEdge* he) const
{
    long int n = halfEdgesList.size();
    if ( he == 0 || n == 0 ) return 0;
    for (long int i = 0; i < n; ++i) {
        if ( halfEdgesList.get(i) == he ) {
            return halfEdgesList.get((i + 1) % n);
        }
    }
    return 0;
}
