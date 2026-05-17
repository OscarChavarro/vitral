#include "_PolyhedralBoundedSolidLoop.h"

#include "_PolyhedralBoundedSolidFace.h"
#include "_PolyhedralBoundedSolidHalfEdge.h"
#include "_PolyhedralBoundedSolidVertex.h"

#include <algorithm>

_PolyhedralBoundedSolidLoop::_PolyhedralBoundedSolidLoop(_PolyhedralBoundedSolidFace* parent)
    : parentFace(parent), boundaryStartHalfEdge(0)
{
    if ( parentFace != 0 ) {
        parentFace->boundariesList.push_back(this);
    }
}

void _PolyhedralBoundedSolidLoop::unlistHalfEdge(_PolyhedralBoundedSolidHalfEdge* he)
{
    halfEdgesList.erase(std::remove(halfEdgesList.begin(), halfEdgesList.end(), he), halfEdgesList.end());
    boundaryStartHalfEdge = halfEdgesList.empty() ? 0 : halfEdgesList[0];
}

_PolyhedralBoundedSolidHalfEdge* _PolyhedralBoundedSolidLoop::halfEdgeVertices(int a, int b)
{
    if ( halfEdgesList.empty() ) return 0;
    for (size_t i = 0; i < halfEdgesList.size(); ++i) {
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
    for (size_t i = 0; i < halfEdgesList.size(); ++i) {
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
    std::reverse(halfEdgesList.begin(), halfEdgesList.end());
    boundaryStartHalfEdge = halfEdgesList.empty() ? 0 : halfEdgesList[0];
}

_PolyhedralBoundedSolidHalfEdge* _PolyhedralBoundedSolidLoop::previousOf(_PolyhedralBoundedSolidHalfEdge* he) const
{
    if ( he == 0 || halfEdgesList.empty() ) return 0;
    for (size_t i = 0; i < halfEdgesList.size(); ++i) {
        if ( halfEdgesList[i] == he ) {
            size_t j = (i == 0) ? halfEdgesList.size() - 1 : i - 1;
            return halfEdgesList[j];
        }
    }
    return 0;
}

_PolyhedralBoundedSolidHalfEdge* _PolyhedralBoundedSolidLoop::nextOf(_PolyhedralBoundedSolidHalfEdge* he) const
{
    if ( he == 0 || halfEdgesList.empty() ) return 0;
    for (size_t i = 0; i < halfEdgesList.size(); ++i) {
        if ( halfEdgesList[i] == he ) {
            return halfEdgesList[(i + 1) % halfEdgesList.size()];
        }
    }
    return 0;
}
