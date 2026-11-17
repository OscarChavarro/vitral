//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "java/util/TreeMap.txx"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidStructuralCopy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

PolyhedralBoundedSolidStructuralCopy::PolyhedralBoundedSolidStructuralCopy()
{
}

PolyhedralBoundedSolid* PolyhedralBoundedSolidStructuralCopy::copy(
    PolyhedralBoundedSolid* source)
{
    PolyhedralBoundedSolid* target;
    TreeMap<_PolyhedralBoundedSolidVertex*,
        _PolyhedralBoundedSolidVertex*> vertices;
    TreeMap<_PolyhedralBoundedSolidEdge*,
        _PolyhedralBoundedSolidEdge*> edges;
    TreeMap<_PolyhedralBoundedSolidHalfEdge*,
        _PolyhedralBoundedSolidHalfEdge*> halfEdges;
    long i;
    long j;
    long k;

    if ( source == NULL ) {
        return NULL;
    }

    target = new PolyhedralBoundedSolid();

    for ( i = 0; i < source->getVerticesList().size(); i++ ) {
        _PolyhedralBoundedSolidVertex* v = source->getVerticesList().get(i);
        _PolyhedralBoundedSolidVertex* c = new _PolyhedralBoundedSolidVertex(
            v->position, v->id);
        target->getVerticesList().add(c);
        c->debugColor = v->debugColor;
        vertices.put(v, c);
    }
    for ( i = 0; i < source->getEdgesList().size(); i++ ) {
        _PolyhedralBoundedSolidEdge* e = source->getEdgesList().get(i);
        _PolyhedralBoundedSolidEdge* c = new _PolyhedralBoundedSolidEdge();
        target->getEdgesList().add(c);
        c->id = e->id;
        c->debugColor = e->debugColor;
        edges.put(e, c);
    }
    for ( i = 0; i < source->getPolygonsList().size(); i++ ) {
        _PolyhedralBoundedSolidFace* f = source->getPolygonsList().get(i);
        _PolyhedralBoundedSolidFace* cf = new _PolyhedralBoundedSolidFace(
            target, f->id);
        target->getPolygonsList().add(cf);
        for ( j = 0; j < f->boundariesList.size(); j++ ) {
            _PolyhedralBoundedSolidLoop* l = f->boundariesList.get(j);
            _PolyhedralBoundedSolidLoop* cl = new _PolyhedralBoundedSolidLoop(
                cf);
            for ( k = 0; k < l->halfEdgesList.size(); k++ ) {
                _PolyhedralBoundedSolidHalfEdge* he = l->halfEdgesList.get(k);
                _PolyhedralBoundedSolidHalfEdge* ch =
                    new _PolyhedralBoundedSolidHalfEdge(
                        mapVertex(vertices, target, he->startingVertex), cl);
                ch->id = he->id;
                if ( he->parentEdge == NULL ) {
                    ch->parentEdge = NULL;
                }
                else {
                    ch->parentEdge = mapEdge(edges, target, he->parentEdge);
                }
                cl->halfEdgesList.add(ch);
                halfEdges.put(he, ch);
            }
            cl->boundaryStartHalfEdge = halfEdges.getOrDefault(
                l->boundaryStartHalfEdge, (_PolyhedralBoundedSolidHalfEdge*)NULL);
        }
    }
    for ( i = 0; i < edges.size(); i++ ) {
        edges.getValue(i)->rightHalf = halfEdges.getOrDefault(
            edges.getKey(i)->rightHalf, (_PolyhedralBoundedSolidHalfEdge*)NULL);
        edges.getValue(i)->leftHalf = halfEdges.getOrDefault(
            edges.getKey(i)->leftHalf, (_PolyhedralBoundedSolidHalfEdge*)NULL);
    }
    for ( i = 0; i < vertices.size(); i++ ) {
        vertices.getValue(i)->emanatingHalfEdge = halfEdges.getOrDefault(
            vertices.getKey(i)->emanatingHalfEdge,
            (_PolyhedralBoundedSolidHalfEdge*)NULL);
    }
    target->setMaxVertexId(source->getMaxVertexId());
    target->setMaxFaceId(source->getMaxFaceId());
    target->setValidationState(source->isValid());
    return target;
}

/** A vertex referenced by a loop but missing from the vertex list. */
_PolyhedralBoundedSolidVertex* PolyhedralBoundedSolidStructuralCopy::mapVertex(
    TreeMap<_PolyhedralBoundedSolidVertex*,
        _PolyhedralBoundedSolidVertex*>& vertices,
    PolyhedralBoundedSolid* target,
    _PolyhedralBoundedSolidVertex* v)
{
    _PolyhedralBoundedSolidVertex* c = vertices.getOrDefault(v,
        (_PolyhedralBoundedSolidVertex*)NULL);
    if ( c == NULL && v != NULL ) {
        c = new _PolyhedralBoundedSolidVertex(v->position, v->id);
        target->getVerticesList().add(c);
        vertices.put(v, c);
    }
    return c;
}

/** An edge referenced by a half-edge but missing from the edge list. */
_PolyhedralBoundedSolidEdge* PolyhedralBoundedSolidStructuralCopy::mapEdge(
    TreeMap<_PolyhedralBoundedSolidEdge*,
        _PolyhedralBoundedSolidEdge*>& edges,
    PolyhedralBoundedSolid* target,
    _PolyhedralBoundedSolidEdge* e)
{
    _PolyhedralBoundedSolidEdge* c = edges.getOrDefault(e,
        (_PolyhedralBoundedSolidEdge*)NULL);
    if ( c == NULL ) {
        c = new _PolyhedralBoundedSolidEdge();
        target->getEdgesList().add(c);
        c->id = e->id;
        edges.put(e, c);
    }
    return c;
}
