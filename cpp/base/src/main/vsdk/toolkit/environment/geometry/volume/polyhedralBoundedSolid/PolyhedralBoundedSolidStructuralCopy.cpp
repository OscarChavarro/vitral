//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#include "java/util/ArrayList.txx"
#include "java/util/HashMap.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidStructuralCopy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

namespace {

template <class K, class V>
V* lookup(java::HashMap<K*, V*>& map, K* key)
{
    V** found = map.get(key);
    return found == 0 ? 0 : *found;
}

}

PolyhedralBoundedSolid* PolyhedralBoundedSolidStructuralCopy::copy(
    PolyhedralBoundedSolid* source)
{
    PolyhedralBoundedSolid* target;
    java::HashMap<_PolyhedralBoundedSolidVertex*,
        _PolyhedralBoundedSolidVertex*> vertices;
    java::ArrayList<_PolyhedralBoundedSolidVertex*> vertexKeys;
    java::HashMap<_PolyhedralBoundedSolidEdge*,
        _PolyhedralBoundedSolidEdge*> edges;
    java::ArrayList<_PolyhedralBoundedSolidEdge*> edgeKeys;
    java::HashMap<_PolyhedralBoundedSolidHalfEdge*,
        _PolyhedralBoundedSolidHalfEdge*> halfEdges;
    int i;
    int j;
    int k;

    if ( source == 0 ) {
        return 0;
    }

    target = new PolyhedralBoundedSolid();

    for ( i = 0; i < source->getVerticesList().size(); i++ ) {
        _PolyhedralBoundedSolidVertex* v = source->getVerticesList().get(i);
        _PolyhedralBoundedSolidVertex* c = new _PolyhedralBoundedSolidVertex(
            v->position, v->id);
        target->getVerticesList().add(c);
        c->debugColor = v->debugColor;
        if ( !vertices.containsKey(v) ) {
            vertexKeys.add(v);
        }
        vertices.put(v, c);
    }
    for ( i = 0; i < source->getEdgesList().size(); i++ ) {
        _PolyhedralBoundedSolidEdge* e = source->getEdgesList().get(i);
        _PolyhedralBoundedSolidEdge* c = new _PolyhedralBoundedSolidEdge();
        target->getEdgesList().add(c);
        c->id = e->id;
        c->debugColor = e->debugColor;
        if ( !edges.containsKey(e) ) {
            edgeKeys.add(e);
        }
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
                        mapVertex(vertices, vertexKeys, target,
                            he->startingVertex),
                        cl);
                ch->id = he->id;
                ch->parentEdge = he->parentEdge == 0 ? 0 :
                    mapEdge(edges, edgeKeys, target, he->parentEdge);
                cl->halfEdgesList.add(ch);
                halfEdges.put(he, ch);
            }
            cl->boundaryStartHalfEdge = lookup(halfEdges,
                l->boundaryStartHalfEdge);
        }
    }
    for ( i = 0; i < edgeKeys.size(); i++ ) {
        _PolyhedralBoundedSolidEdge* key = edgeKeys.get(i);
        _PolyhedralBoundedSolidEdge* value = lookup(edges, key);
        value->rightHalf = lookup(halfEdges, key->rightHalf);
        value->leftHalf = lookup(halfEdges, key->leftHalf);
    }
    for ( i = 0; i < vertexKeys.size(); i++ ) {
        _PolyhedralBoundedSolidVertex* key = vertexKeys.get(i);
        lookup(vertices, key)->emanatingHalfEdge = lookup(halfEdges,
            key->emanatingHalfEdge);
    }
    target->setMaxVertexId(source->getMaxVertexId());
    target->setMaxFaceId(source->getMaxFaceId());
    target->setValidationState(source->isValid());
    return target;
}

_PolyhedralBoundedSolidVertex* PolyhedralBoundedSolidStructuralCopy::mapVertex(
    java::HashMap<_PolyhedralBoundedSolidVertex*,
        _PolyhedralBoundedSolidVertex*>& vertices,
    java::ArrayList<_PolyhedralBoundedSolidVertex*>& vertexKeys,
    PolyhedralBoundedSolid* target,
    _PolyhedralBoundedSolidVertex* v)
{
    _PolyhedralBoundedSolidVertex* c = lookup(vertices, v);
    if ( c == 0 && v != 0 ) {
        c = new _PolyhedralBoundedSolidVertex(v->position, v->id);
        target->getVerticesList().add(c);
        if ( !vertices.containsKey(v) ) {
            vertexKeys.add(v);
        }
        vertices.put(v, c);
    }
    return c;
}

_PolyhedralBoundedSolidEdge* PolyhedralBoundedSolidStructuralCopy::mapEdge(
    java::HashMap<_PolyhedralBoundedSolidEdge*,
        _PolyhedralBoundedSolidEdge*>& edges,
    java::ArrayList<_PolyhedralBoundedSolidEdge*>& edgeKeys,
    PolyhedralBoundedSolid* target,
    _PolyhedralBoundedSolidEdge* e)
{
    _PolyhedralBoundedSolidEdge* c = lookup(edges, e);
    if ( c == 0 ) {
        c = new _PolyhedralBoundedSolidEdge();
        target->getEdgesList().add(c);
        c->id = e->id;
        if ( !edges.containsKey(e) ) {
            edgeKeys.add(e);
        }
        edges.put(e, c);
    }
    return c;
}
