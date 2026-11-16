//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import { PolyhedralBoundedSolid } from "./PolyhedralBoundedSolid.js";
import { _PolyhedralBoundedSolidEdge } from "./nodes/_PolyhedralBoundedSolidEdge.js";
import { _PolyhedralBoundedSolidFace } from "./nodes/_PolyhedralBoundedSolidFace.js";
import { _PolyhedralBoundedSolidHalfEdge } from "./nodes/_PolyhedralBoundedSolidHalfEdge.js";
import { _PolyhedralBoundedSolidLoop } from "./nodes/_PolyhedralBoundedSolidLoop.js";
import { _PolyhedralBoundedSolidVertex } from "./nodes/_PolyhedralBoundedSolidVertex.js";

/**
Structural copy of a half-edge solid, in the spirit of the copy routine
from [MANT1988] Sec. 15.8 ("scopy"): every vertex, edge, face, loop and
half-edge is duplicated and all the cross references are remapped to the
duplicates, keeping identifiers, list order and loop start half-edges.

<p>Unlike a serialization round trip it allocates no byte buffers and uses no
recursion, so its cost is linear in the size of the boundary and it cannot
overflow the stack on large models.</p>
 */
export class PolyhedralBoundedSolidStructuralCopy {
    private constructor() {}

    /**
    Returns an independent copy of {@code source}; {@code null} for
    {@code null}. The copy shares no node and no position vector with the
    source. Half-edges without a parent edge (loops of a single vertex, as
    left by mvfs or lkemr) are copied as they are.
    */
    public static copy(source: PolyhedralBoundedSolid): PolyhedralBoundedSolid;
    public static copy(source: PolyhedralBoundedSolid | null): PolyhedralBoundedSolid | null;
    public static copy(source: PolyhedralBoundedSolid | null): PolyhedralBoundedSolid | null {
        let target: PolyhedralBoundedSolid;
        let vertices: Map<_PolyhedralBoundedSolidVertex, _PolyhedralBoundedSolidVertex>;
        let edges: Map<_PolyhedralBoundedSolidEdge, _PolyhedralBoundedSolidEdge>;
        let halfEdges: Map<_PolyhedralBoundedSolidHalfEdge, _PolyhedralBoundedSolidHalfEdge>;
        let i: number;
        let j: number;
        let k: number;

        if (source === null) {
            return null;
        }

        target = new PolyhedralBoundedSolid();
        vertices = new Map();
        edges = new Map();
        halfEdges = new Map();

        for (i = 0; i < source.getVerticesList().size(); i++) {
            const v = source.getVerticesList().get(i)!;
            const c = new _PolyhedralBoundedSolidVertex(target, v.position, v.id);
            c.debugColor = v.debugColor;
            vertices.set(v, c);
        }
        for (i = 0; i < source.getEdgesList().size(); i++) {
            const e = source.getEdgesList().get(i)!;
            const c = new _PolyhedralBoundedSolidEdge(target);
            c.id = e.id;
            c.debugColor = e.debugColor;
            edges.set(e, c);
        }
        for (i = 0; i < source.getPolygonsList().size(); i++) {
            const f = source.getPolygonsList().get(i)!;
            const cf = new _PolyhedralBoundedSolidFace(target, f.id);
            for (j = 0; j < f.boundariesList.size(); j++) {
                const l = f.boundariesList.get(j)!;
                const cl = new _PolyhedralBoundedSolidLoop(cf);
                for (k = 0; k < l.halfEdgesList.size(); k++) {
                    const he = l.halfEdgesList.get(k)!;
                    const ch = new _PolyhedralBoundedSolidHalfEdge(
                        PolyhedralBoundedSolidStructuralCopy.mapVertex(vertices, target, he.startingVertex)!,
                        cl,
                        target,
                    );
                    ch.id = he.id;
                    ch.parentEdge =
                        he.parentEdge === null
                            ? null
                            : PolyhedralBoundedSolidStructuralCopy.mapEdge(edges, target, he.parentEdge);
                    cl.halfEdgesList.add(ch);
                    halfEdges.set(he, ch);
                }
                cl.boundaryStartHalfEdge =
                    l.boundaryStartHalfEdge === null ? null : (halfEdges.get(l.boundaryStartHalfEdge) ?? null);
            }
        }
        for (const [key, value] of edges) {
            value.rightHalf = key.rightHalf === null ? null : (halfEdges.get(key.rightHalf) ?? null);
            value.leftHalf = key.leftHalf === null ? null : (halfEdges.get(key.leftHalf) ?? null);
        }
        for (const [key, value] of vertices) {
            value.emanatingHalfEdge =
                key.emanatingHalfEdge === null ? null : (halfEdges.get(key.emanatingHalfEdge) ?? null);
        }
        target.setMaxVertexId(source.getMaxVertexId());
        target.setMaxFaceId(source.getMaxFaceId());
        target.setValidationState(source.isValid());
        return target;
    }

    /** A vertex referenced by a loop but missing from the vertex list. */
    private static mapVertex(
        vertices: Map<_PolyhedralBoundedSolidVertex, _PolyhedralBoundedSolidVertex>,
        target: PolyhedralBoundedSolid,
        v: _PolyhedralBoundedSolidVertex | null,
    ): _PolyhedralBoundedSolidVertex | null {
        let c = v === null ? null : (vertices.get(v) ?? null);
        if (c === null && v !== null) {
            c = new _PolyhedralBoundedSolidVertex(target, v.position, v.id);
            vertices.set(v, c);
        }
        return c;
    }

    /** An edge referenced by a half-edge but missing from the edge list. */
    private static mapEdge(
        edges: Map<_PolyhedralBoundedSolidEdge, _PolyhedralBoundedSolidEdge>,
        target: PolyhedralBoundedSolid,
        e: _PolyhedralBoundedSolidEdge,
    ): _PolyhedralBoundedSolidEdge {
        let c = edges.get(e) ?? null;
        if (c === null) {
            c = new _PolyhedralBoundedSolidEdge(target);
            c.id = e.id;
            edges.set(e, c);
        }
        return c;
    }
}
