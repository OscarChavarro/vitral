//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid;

import java.util.IdentityHashMap;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;

/**
Structural copy of a half-edge solid, in the spirit of the copy routine
from [MANT1988] Sec. 15.8 ("scopy"): every vertex, edge, face, loop and
half-edge is duplicated and all the cross references are remapped to the
duplicates, keeping identifiers, list order and loop start half-edges.

<p>Unlike a serialization round trip it allocates no byte buffers and uses no
recursion, so its cost is linear in the size of the boundary and it cannot
overflow the stack on large models.</p>
 */
public final class PolyhedralBoundedSolidStructuralCopy
{
    private PolyhedralBoundedSolidStructuralCopy()
    {
    }

    /**
    Returns an independent copy of {@code source}; {@code null} for
    {@code null}. The copy shares no node and no position vector with the
    source. Half-edges without a parent edge (loops of a single vertex, as
    left by mvfs or lkemr) are copied as they are.
    */
    public static PolyhedralBoundedSolid copy(PolyhedralBoundedSolid source)
    {
        PolyhedralBoundedSolid target;
        IdentityHashMap<_PolyhedralBoundedSolidVertex,
            _PolyhedralBoundedSolidVertex> vertices;
        IdentityHashMap<_PolyhedralBoundedSolidEdge,
            _PolyhedralBoundedSolidEdge> edges;
        IdentityHashMap<_PolyhedralBoundedSolidHalfEdge,
            _PolyhedralBoundedSolidHalfEdge> halfEdges;
        int i;
        int j;
        int k;

        if ( source == null ) {
            return null;
        }

        target = new PolyhedralBoundedSolid();
        vertices = new IdentityHashMap<>();
        edges = new IdentityHashMap<>();
        halfEdges = new IdentityHashMap<>();

        for ( i = 0; i < source.getVerticesList().size(); i++ ) {
            _PolyhedralBoundedSolidVertex v = source.getVerticesList().get(i);
            _PolyhedralBoundedSolidVertex c = new _PolyhedralBoundedSolidVertex(
                target, v.position, v.id);
            c.debugColor = v.debugColor;
            vertices.put(v, c);
        }
        for ( i = 0; i < source.getEdgesList().size(); i++ ) {
            _PolyhedralBoundedSolidEdge e = source.getEdgesList().get(i);
            _PolyhedralBoundedSolidEdge c = new _PolyhedralBoundedSolidEdge(
                target);
            c.id = e.id;
            c.debugColor = e.debugColor;
            edges.put(e, c);
        }
        for ( i = 0; i < source.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace f = source.getPolygonsList().get(i);
            _PolyhedralBoundedSolidFace cf = new _PolyhedralBoundedSolidFace(
                target, f.id);
            for ( j = 0; j < f.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop l = f.boundariesList.get(j);
                _PolyhedralBoundedSolidLoop cl = new _PolyhedralBoundedSolidLoop(
                    cf);
                for ( k = 0; k < l.halfEdgesList.size(); k++ ) {
                    _PolyhedralBoundedSolidHalfEdge he = l.halfEdgesList.get(k);
                    _PolyhedralBoundedSolidHalfEdge ch =
                        new _PolyhedralBoundedSolidHalfEdge(
                            mapVertex(vertices, target, he.startingVertex),
                            cl, target);
                    ch.id = he.id;
                    ch.parentEdge = he.parentEdge == null ? null :
                        mapEdge(edges, target, he.parentEdge);
                    cl.halfEdgesList.add(ch);
                    halfEdges.put(he, ch);
                }
                cl.boundaryStartHalfEdge = halfEdges.get(
                    l.boundaryStartHalfEdge);
            }
        }
        for ( java.util.Map.Entry<_PolyhedralBoundedSolidEdge,
                  _PolyhedralBoundedSolidEdge> entry : edges.entrySet() ) {
            entry.getValue().rightHalf = halfEdges.get(
                entry.getKey().rightHalf);
            entry.getValue().leftHalf = halfEdges.get(entry.getKey().leftHalf);
        }
        for ( java.util.Map.Entry<_PolyhedralBoundedSolidVertex,
                  _PolyhedralBoundedSolidVertex> entry : vertices.entrySet() ) {
            entry.getValue().emanatingHalfEdge = halfEdges.get(
                entry.getKey().emanatingHalfEdge);
        }
        target.setMaxVertexId(source.getMaxVertexId());
        target.setMaxFaceId(source.getMaxFaceId());
        target.setValidationState(source.isValid());
        return target;
    }

    /** A vertex referenced by a loop but missing from the vertex list. */
    private static _PolyhedralBoundedSolidVertex mapVertex(
        IdentityHashMap<_PolyhedralBoundedSolidVertex,
            _PolyhedralBoundedSolidVertex> vertices,
        PolyhedralBoundedSolid target,
        _PolyhedralBoundedSolidVertex v)
    {
        _PolyhedralBoundedSolidVertex c = vertices.get(v);
        if ( c == null && v != null ) {
            c = new _PolyhedralBoundedSolidVertex(target, v.position, v.id);
            vertices.put(v, c);
        }
        return c;
    }

    /** An edge referenced by a half-edge but missing from the edge list. */
    private static _PolyhedralBoundedSolidEdge mapEdge(
        IdentityHashMap<_PolyhedralBoundedSolidEdge,
            _PolyhedralBoundedSolidEdge> edges,
        PolyhedralBoundedSolid target,
        _PolyhedralBoundedSolidEdge e)
    {
        _PolyhedralBoundedSolidEdge c = edges.get(e);
        if ( c == null ) {
            c = new _PolyhedralBoundedSolidEdge(target);
            c.id = e.id;
            edges.put(e, c);
        }
        return c;
    }
}
