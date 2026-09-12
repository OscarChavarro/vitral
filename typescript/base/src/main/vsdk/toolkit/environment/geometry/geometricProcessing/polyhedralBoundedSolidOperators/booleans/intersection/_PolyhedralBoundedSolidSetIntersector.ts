//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import { ArrayList } from "../../../../../../../../java/util/ArrayList.js";
import type { Vector3Dd } from "../../../../../../common/linealAlgebra/Vector3Dd.js";
import { Geometry } from "../../../../Geometry.js";
import type { PolyhedralBoundedSolid } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidEulerOperators } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import { PolyhedralBoundedSolidNumericPolicy } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import type { _PolyhedralBoundedSolidEdge } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.js";
import type {
    _PolyhedralBoundedSolidFace,
    PointInsideResult,
} from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { _PolyhedralBoundedSolidVertex } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";
import { _PolyhedralBoundedSolidOperator } from "../../_PolyhedralBoundedSolidOperator.js";
import { _PolyhedralBoundedSolidSetOperatorVertexFace } from "../classification/_PolyhedralBoundedSolidSetOperatorVertexFace.js";
import { _PolyhedralBoundedSolidSetOperatorVertexVertex } from "../classification/_PolyhedralBoundedSolidSetOperatorVertexVertex.js";

class BoundaryHit {
    public readonly halfEdge: _PolyhedralBoundedSolidHalfEdge | null;
    public readonly vertex: _PolyhedralBoundedSolidVertex | null;
    public readonly point: Vector3Dd;

    /** Java private constructor, used only by the enclosing intersector. */
    public constructor(
        halfEdge: _PolyhedralBoundedSolidHalfEdge | null,
        vertex: _PolyhedralBoundedSolidVertex | null,
        point: Vector3Dd,
    ) {
        this.halfEdge = halfEdge;
        this.vertex = vertex;
        this.point = point;
    }
}

class GenerationResult {
    private readonly sonvvValue: ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex>;
    private readonly sonvaValue: ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace>;
    private readonly sonvbValue: ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace>;

    /** Java private constructor, used only by the enclosing intersector. */
    public constructor(
        sonvv: ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex>,
        sonva: ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace>,
        sonvb: ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace>,
    ) {
        this.sonvvValue = sonvv;
        this.sonvaValue = sonva;
        this.sonvbValue = sonvb;
    }

    public sonvv(): ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex> {
        return this.sonvvValue;
    }

    public sonva(): ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> {
        return this.sonvaValue;
    }

    public sonvb(): ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> {
        return this.sonvbValue;
    }
}

/**
Helper for set operation big phase 0: generation of vertex/face and
vertex/vertex intersections, following the initial detection phase from
program [MANT1988].15.2.
*/
export class _PolyhedralBoundedSolidSetIntersector extends _PolyhedralBoundedSolidOperator {
    /** Diagnostic trace: one entry per vertex created during intersection. Cleared at the start
    of each {@link #setOpGenerate} call. Accessible from tests in the same package. */
    public static readonly intersectionTrace = new ArrayList<string>();

    private static compareToZero(value: number): number {
        return PolyhedralBoundedSolidNumericPolicy.compareToZero(value, _PolyhedralBoundedSolidOperator.numericContext);
    }

    private static isZero(value: number): boolean {
        return PolyhedralBoundedSolidNumericPolicy.isZero(value, _PolyhedralBoundedSolidOperator.numericContext);
    }

    private static isZeroBig(value: number): boolean {
        return PolyhedralBoundedSolidNumericPolicy.isZeroBig(value, _PolyhedralBoundedSolidOperator.numericContext);
    }

    private static pointInFaceDetailed(face: _PolyhedralBoundedSolidFace, point: Vector3Dd): PointInsideResult {
        return face.testPointInsideDetailed(point, _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon());
    }

    /**
    Java overloads `findNearbyBoundaryHit(_PolyhedralBoundedSolidFace, Vector3Dd)`
    and `findNearbyBoundaryHit(_PolyhedralBoundedSolidHalfEdge, Vector3Dd, double)`.
    */
    private static findNearbyBoundaryHit(
        face: _PolyhedralBoundedSolidFace | null,
        point: Vector3Dd | null,
    ): BoundaryHit | null;
    private static findNearbyBoundaryHit(
        halfEdge: _PolyhedralBoundedSolidHalfEdge | null,
        point: Vector3Dd,
        tolerance: number,
    ): BoundaryHit | null;
    private static findNearbyBoundaryHit(
        faceOrHalfEdge: _PolyhedralBoundedSolidFace | _PolyhedralBoundedSolidHalfEdge | null,
        point: Vector3Dd | null,
        tolerance?: number,
    ): BoundaryHit | null {
        if (tolerance !== undefined) {
            const halfEdge = faceOrHalfEdge as _PolyhedralBoundedSolidHalfEdge | null;
            const pointOnHalfEdge = point!;
            if (
                halfEdge === null ||
                halfEdge.startingVertex === null ||
                halfEdge.next() === null ||
                halfEdge.next()!.startingVertex === null
            ) {
                return null;
            }

            const a = halfEdge.startingVertex.position;
            const b = halfEdge.next()!.startingVertex.position;
            if (a === null || b === null) {
                return null;
            }
            if (pointOnHalfEdge.subtract(a).length() <= tolerance) {
                return new BoundaryHit(null, halfEdge.startingVertex, a);
            }
            if (pointOnHalfEdge.subtract(b).length() <= tolerance) {
                return new BoundaryHit(null, halfEdge.next()!.startingVertex, b);
            }

            const ab = b.subtract(a);
            const lengthSquared = ab.dotProduct(ab);
            if (lengthSquared <= tolerance * tolerance) {
                return null;
            }
            const t = pointOnHalfEdge.subtract(a).dotProduct(ab) / lengthSquared;
            if (t <= 0.0 || t >= 1.0) {
                return null;
            }
            const closest = a.add(ab.multiply(t));
            if (pointOnHalfEdge.subtract(closest).length() > tolerance) {
                return null;
            }
            return new BoundaryHit(halfEdge, null, closest);
        }
        const face = faceOrHalfEdge as _PolyhedralBoundedSolidFace | null;
        let i: number;

        if (face === null || point === null || face.boundariesList === null) {
            return null;
        }

        const faceTolerance = Math.max(_PolyhedralBoundedSolidOperator.numericContext.bigEpsilon() * 10.0, 1.0e-7);
        for (i = 0; i < face.boundariesList.size(); i++) {
            let current: _PolyhedralBoundedSolidHalfEdge | null;
            let guard: number;

            if (face.boundariesList.get(i) === null || face.boundariesList.get(i)!.boundaryStartHalfEdge === null) {
                continue;
            }
            const start = face.boundariesList.get(i)!.boundaryStartHalfEdge!;
            current = start;
            guard = 0;
            do {
                const hit = _PolyhedralBoundedSolidSetIntersector.findNearbyBoundaryHit(current, point, faceTolerance);

                if (hit !== null) {
                    return hit;
                }
                current = current!.next();
                guard++;
            } while (current !== start && guard <= face.boundariesList.get(i)!.halfEdgesList.size() + 1);
        }
        return null;
    }

    private static nextVertexId(current: PolyhedralBoundedSolid, other: PolyhedralBoundedSolid): number {
        const idNamespace = _PolyhedralBoundedSolidOperator.idNamespace;
        if (idNamespace !== null) {
            return idNamespace.nextVertexId(current, other);
        }
        let currentMax: number;

        currentMax = current.getMaxVertexId();
        const otherMax = other.getMaxVertexId();
        if (otherMax > currentMax) {
            currentMax = otherMax;
        }
        return currentMax + 1;
    }

    /**
    Inserts a vertex/face coincidence into the set corresponding to the
    `sonva`/`sonvb` variables of program [MANT1988].15.1.
    */
    private static addsovf(
        he: _PolyhedralBoundedSolidHalfEdge,
        f: _PolyhedralBoundedSolidFace,
        BvsA: number,
        sonva: ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace>,
        sonvb: ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace>,
    ): void {
        let elem: _PolyhedralBoundedSolidSetOperatorVertexFace;
        let sonv: ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace>;
        let i: number;

        if (BvsA === 0) {
            sonv = sonva;
        } else {
            sonv = sonvb;
        }

        for (i = 0; i < sonv.size(); i++) {
            elem = sonv.get(i);
            if (elem.v === he.startingVertex && elem.f === f) {
                return;
            }
        }

        elem = new _PolyhedralBoundedSolidSetOperatorVertexFace();
        elem.v = he.startingVertex;
        elem.f = f;
        sonv.add(elem);
    }

    /**
    Inserts a vertex/vertex coincidence into the set corresponding to the
    `sonvv` variable of program [MANT1988].15.1.
    */
    private static addsovv(
        a: _PolyhedralBoundedSolidVertex,
        b: _PolyhedralBoundedSolidVertex,
        BvsA: number,
        sonvv: ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex>,
    ): void {
        let elem: _PolyhedralBoundedSolidSetOperatorVertexVertex;
        let i: number;

        for (i = 0; i < sonvv.size(); i++) {
            elem = sonvv.get(i);
            if ((BvsA === 0 && elem.va === a && elem.vb === b) || (BvsA !== 0 && elem.va === b && elem.vb === a)) {
                return;
            }
        }

        elem = new _PolyhedralBoundedSolidSetOperatorVertexVertex();
        if (BvsA === 0) {
            elem.va = a;
            elem.vb = b;
        } else {
            elem.va = b;
            elem.vb = a;
        }
        sonvv.add(elem);
    }

    /**
    Handles the degenerate branch of the edge/face test from section
    [MANT1988].15.3 when one endpoint already lies on the reference face, using
    the point-on-edge and point-on-vertex bookkeeping suggested by problem
    [MANT1988].13.3.
    */
    private static doVertexOnFace(
        v: _PolyhedralBoundedSolidVertex,
        f: _PolyhedralBoundedSolidFace,
        BvsA: number,
        edgeSolid: PolyhedralBoundedSolid,
        faceSolid: PolyhedralBoundedSolid,
        sonvv: ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex>,
        sonva: ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace>,
        sonvb: ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace>,
    ): void {
        let cont: number;
        let intersectedHalfedge: _PolyhedralBoundedSolidHalfEdge | null;
        let intersectedVertex: _PolyhedralBoundedSolidVertex | null;

        const d = f.getContainingPlane()!.pointDistance(v.position);
        if (_PolyhedralBoundedSolidSetIntersector.compareToZero(d) === 0) {
            const containment = _PolyhedralBoundedSolidSetIntersector.pointInFaceDetailed(f, v.position);
            cont = containment.status();
            intersectedHalfedge = containment.intersectedHalfedge();
            intersectedVertex = containment.intersectedVertex();
            if (cont === Geometry.INSIDE) {
                const nearbyBoundaryHit = _PolyhedralBoundedSolidSetIntersector.findNearbyBoundaryHit(f, v.position);
                if (nearbyBoundaryHit !== null) {
                    cont = Geometry.LIMIT;
                    intersectedHalfedge = nearbyBoundaryHit.halfEdge;
                    intersectedVertex = nearbyBoundaryHit.vertex;
                }
            }
            if (cont === Geometry.INSIDE) {
                _PolyhedralBoundedSolidSetIntersector.addsovf(v.emanatingHalfEdge!, f, BvsA, sonva, sonvb);
            } else if (cont === Geometry.LIMIT && intersectedHalfedge !== null) {
                const newVIdVof = _PolyhedralBoundedSolidSetIntersector.nextVertexId(edgeSolid, faceSolid);
                const eLabelVof = BvsA === 0 ? "A" : "B";
                const fLabelVof = BvsA === 0 ? "B" : "A";
                const bv1Vof = intersectedHalfedge.startingVertex.id;
                const bv2Vof = intersectedHalfedge.next()!.startingVertex.id;
                _PolyhedralBoundedSolidSetIntersector.intersectionTrace.add(
                    "Vertex " +
                        fLabelVof +
                        ":" +
                        newVIdVof +
                        " created splitting " +
                        fLabelVof +
                        ":<" +
                        bv1Vof +
                        ", " +
                        bv2Vof +
                        "> boundary edge (at coincidence with " +
                        eLabelVof +
                        " vertex " +
                        v.id +
                        ").",
                );
                PolyhedralBoundedSolidEulerOperators.lmev(
                    faceSolid,
                    intersectedHalfedge,
                    intersectedHalfedge.mirrorHalfEdge()!.next(),
                    newVIdVof,
                    v.position,
                );
                _PolyhedralBoundedSolidSetIntersector.addsovv(v, intersectedHalfedge.startingVertex, BvsA, sonvv);
            } else if (cont === Geometry.LIMIT && intersectedVertex !== null) {
                _PolyhedralBoundedSolidSetIntersector.addsovv(v, intersectedVertex, BvsA, sonvv);
            }
        }
    }

    /**
    Performs one edge/face intersection test for the big-phase-0 generator.
    This is the edge/face crossing analysis required by section [MANT1988].15.3
    as part of the initial detector of program [MANT1988].15.2.
    */
    private static doSetOpGenerate(
        e: _PolyhedralBoundedSolidEdge,
        f: _PolyhedralBoundedSolidFace,
        BvsA: number,
        edgeSolid: PolyhedralBoundedSolid,
        faceSolid: PolyhedralBoundedSolid,
        sonvv: ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex>,
        sonva: ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace>,
        sonvb: ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace>,
    ): _PolyhedralBoundedSolidEdge | null {
        let d1: number;
        let d2: number;
        let t: number;
        let p: Vector3Dd;
        let cont: number;
        let containment: PointInsideResult;
        let intersectedHalfedge: _PolyhedralBoundedSolidHalfEdge;
        let intersectedVertex: _PolyhedralBoundedSolidVertex;
        let nearbyBoundaryHit: BoundaryHit | null;

        const v1 = e.rightHalf!.startingVertex;
        const v2 = e.leftHalf!.startingVertex;
        d1 = f.getContainingPlane()!.pointDistance(v1.position);
        d2 = f.getContainingPlane()!.pointDistance(v2.position);

        // Snap vertices in the (epsilon, bigEpsilon] gap onto the face plane.
        // Without this, such a vertex triggers the crossing branch and produces
        // a null-edge whose original endpoint is off-plane; after Connect, that
        // endpoint ends up in the result face and fails the coplanarity check.
        //
        // The snap is only valid when the vertex actually lies over the bounded
        // face: this test runs for every (edge, face) pair, so the infinite
        // plane of a face can pass within bigEpsilon of vertices that are
        // arbitrarily far from the face itself. Snapping those would drag
        // unrelated vertices off their own faces and make remote, previously
        // planar faces non-planar (stage-6 finding: moon motifs at z=9.0 moved
        // the bowl vertex antipodal to the motif). A vertex whose projection
        // falls outside the bounded face cannot contribute an intersection
        // inside that face, so skipping the snap there is always safe.
        if (_PolyhedralBoundedSolidSetIntersector.isZeroBig(d1) && !_PolyhedralBoundedSolidSetIntersector.isZero(d1)) {
            const snapped1 = f.getContainingPlane()!.projectPoint(v1.position);
            if (_PolyhedralBoundedSolidSetIntersector.pointInFaceDetailed(f, snapped1).status() !== Geometry.OUTSIDE) {
                v1.position = snapped1;
                d1 = 0.0;
            }
        }
        if (_PolyhedralBoundedSolidSetIntersector.isZeroBig(d2) && !_PolyhedralBoundedSolidSetIntersector.isZero(d2)) {
            const snapped2 = f.getContainingPlane()!.projectPoint(v2.position);
            if (_PolyhedralBoundedSolidSetIntersector.pointInFaceDetailed(f, snapped2).status() !== Geometry.OUTSIDE) {
                v2.position = snapped2;
                d2 = 0.0;
            }
        }

        const s1 = _PolyhedralBoundedSolidSetIntersector.compareToZero(d1);
        const s2 = _PolyhedralBoundedSolidSetIntersector.compareToZero(d2);

        if ((s1 === -1 && s2 === 1) || (s1 === 1 && s2 === -1)) {
            t = d1 / (d1 - d2);
            p = v1.position.add(v2.position.subtract(v1.position).multiply(t));

            const d3 = f.getContainingPlane()!.pointDistance(p);
            if (_PolyhedralBoundedSolidSetIntersector.compareToZero(d3) === 0) {
                const facePlane = f.getContainingPlane()!;
                p = facePlane.projectPoint(p);

                // Snap p to the intersection line of f's plane and the edge's
                // face plane. Without this, intrinsically non-planar faces (e.g.
                // sphere quads) produce a vertex that lies on f's plane but not
                // on the edge face's plane, causing coplanarity failures after
                // the boolean operation.
                const edgeFace = e.rightHalf!.parentLoop.parentFace;
                if (edgeFace !== null) {
                    const edgeFacePlane = edgeFace.getContainingPlane()!;
                    const dEdge = edgeFacePlane.pointDistance(p);
                    if (
                        !_PolyhedralBoundedSolidSetIntersector.isZero(dEdge) &&
                        _PolyhedralBoundedSolidSetIntersector.isZeroBig(dEdge)
                    ) {
                        const n1 = edgeFacePlane.getNormal();
                        const n2 = facePlane.getNormal();
                        const n1DotN2 = n1.dotProduct(n2);
                        const denom = 1.0 - n1DotN2 * n1DotN2;
                        if (denom > _PolyhedralBoundedSolidOperator.numericContext.epsilon()) {
                            const n1Perp = n1.subtract(n2.multiply(n1DotN2));
                            p = p.subtract(n1Perp.multiply(dEdge / denom));
                        }
                    }
                }

                containment = _PolyhedralBoundedSolidSetIntersector.pointInFaceDetailed(f, p);
                cont = containment.status();
                nearbyBoundaryHit = null;
                if (cont === Geometry.INSIDE) {
                    nearbyBoundaryHit = _PolyhedralBoundedSolidSetIntersector.findNearbyBoundaryHit(f, p);
                    if (nearbyBoundaryHit !== null) {
                        cont = Geometry.LIMIT;
                        p = nearbyBoundaryHit.point;
                    }
                }

                if (cont !== Geometry.OUTSIDE) {
                    const eLabel = BvsA === 0 ? "A" : "B";
                    const fLabel = BvsA === 0 ? "B" : "A";
                    const newVId = _PolyhedralBoundedSolidSetIntersector.nextVertexId(edgeSolid, faceSolid);
                    _PolyhedralBoundedSolidSetIntersector.intersectionTrace.add(
                        "Vertex " +
                            eLabel +
                            ":" +
                            newVId +
                            " created after intersection between " +
                            fLabel +
                            ":" +
                            f.id +
                            " face and " +
                            eLabel +
                            ":<" +
                            v1.id +
                            ", " +
                            v2.id +
                            "> edge.",
                    );
                    PolyhedralBoundedSolidEulerOperators.lmev(edgeSolid, e.rightHalf, e.leftHalf!.next(), newVId, p);

                    if (cont === Geometry.INSIDE) {
                        _PolyhedralBoundedSolidSetIntersector.addsovf(e.rightHalf!, f, BvsA, sonva, sonvb);
                    } else if (
                        cont === Geometry.LIMIT &&
                        ((nearbyBoundaryHit !== null && nearbyBoundaryHit.halfEdge !== null) ||
                            containment.intersectedHalfedge() !== null)
                    ) {
                        intersectedHalfedge =
                            nearbyBoundaryHit !== null && nearbyBoundaryHit.halfEdge !== null
                                ? nearbyBoundaryHit.halfEdge
                                : containment.intersectedHalfedge()!;
                        const newVIdBoundary = _PolyhedralBoundedSolidSetIntersector.nextVertexId(edgeSolid, faceSolid);
                        const bv1 = intersectedHalfedge.startingVertex.id;
                        const bv2 = intersectedHalfedge.next()!.startingVertex.id;
                        _PolyhedralBoundedSolidSetIntersector.intersectionTrace.add(
                            "Vertex " +
                                fLabel +
                                ":" +
                                newVIdBoundary +
                                " created splitting " +
                                fLabel +
                                ":<" +
                                bv1 +
                                ", " +
                                bv2 +
                                "> boundary edge (intersection on " +
                                fLabel +
                                ":" +
                                f.id +
                                " face boundary).",
                        );
                        PolyhedralBoundedSolidEulerOperators.lmev(
                            faceSolid,
                            intersectedHalfedge,
                            intersectedHalfedge.mirrorHalfEdge()!.next(),
                            newVIdBoundary,
                            p,
                        );
                        _PolyhedralBoundedSolidSetIntersector.addsovv(
                            e.rightHalf!.startingVertex,
                            intersectedHalfedge.startingVertex,
                            BvsA,
                            sonvv,
                        );
                    } else if (
                        cont === Geometry.LIMIT &&
                        ((nearbyBoundaryHit !== null && nearbyBoundaryHit.vertex !== null) ||
                            containment.intersectedVertex() !== null)
                    ) {
                        intersectedVertex =
                            nearbyBoundaryHit !== null && nearbyBoundaryHit.vertex !== null
                                ? nearbyBoundaryHit.vertex
                                : containment.intersectedVertex()!;
                        _PolyhedralBoundedSolidSetIntersector.addsovv(
                            e.rightHalf!.startingVertex,
                            intersectedVertex,
                            BvsA,
                            sonvv,
                        );
                    }
                    return e.rightHalf!.previous()!.parentEdge;
                }
            }
        } else {
            if (s1 === 0) {
                _PolyhedralBoundedSolidSetIntersector.doVertexOnFace(
                    v1,
                    f,
                    BvsA,
                    edgeSolid,
                    faceSolid,
                    sonvv,
                    sonva,
                    sonvb,
                );
            }
            if (s2 === 0) {
                _PolyhedralBoundedSolidSetIntersector.doVertexOnFace(
                    v2,
                    f,
                    BvsA,
                    edgeSolid,
                    faceSolid,
                    sonvv,
                    sonva,
                    sonvb,
                );
            }
        }

        return null;
    }

    private static processEdge(
        e: _PolyhedralBoundedSolidEdge,
        edgeSolid: PolyhedralBoundedSolid,
        faceSolid: PolyhedralBoundedSolid,
        BvsA: number,
        sonvv: ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex>,
        sonva: ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace>,
        sonvb: ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace>,
    ): void {
        let f: _PolyhedralBoundedSolidFace;
        let generatedEdge: _PolyhedralBoundedSolidEdge | null;
        let i: number;

        for (i = 0; i < faceSolid.getPolygonsList().size(); i++) {
            f = faceSolid.getPolygonsList().get(i)!;
            generatedEdge = _PolyhedralBoundedSolidSetIntersector.doSetOpGenerate(
                e,
                f,
                BvsA,
                edgeSolid,
                faceSolid,
                sonvv,
                sonva,
                sonvb,
            );
            if (generatedEdge !== null) {
                _PolyhedralBoundedSolidSetIntersector.processEdge(
                    generatedEdge,
                    edgeSolid,
                    faceSolid,
                    BvsA,
                    sonvv,
                    sonva,
                    sonvb,
                );
            }
        }
    }

    /**
    Initial vertex intersection detector for the set operations algorithm
    (big phase 0).
    Following program [MANT1988].15.2.
    */
    public static setOpGenerate(inSolidA: PolyhedralBoundedSolid, inSolidB: PolyhedralBoundedSolid): GenerationResult {
        let e: _PolyhedralBoundedSolidEdge;
        let i: number;

        _PolyhedralBoundedSolidSetIntersector.intersectionTrace.clear();
        const sonvv = new ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex>();
        const sonva = new ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace>();
        const sonvb = new ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace>();

        for (i = 0; i < inSolidA.getEdgesList().size(); i++) {
            e = inSolidA.getEdgesList().get(i)!;
            _PolyhedralBoundedSolidSetIntersector.processEdge(e, inSolidA, inSolidB, 0, sonvv, sonva, sonvb);
        }
        for (i = 0; i < inSolidB.getEdgesList().size(); i++) {
            e = inSolidB.getEdgesList().get(i)!;
            _PolyhedralBoundedSolidSetIntersector.processEdge(e, inSolidB, inSolidA, 1, sonvv, sonva, sonvb);
        }

        return new GenerationResult(sonvv, sonva, sonvb);
    }
}

type _BoundaryHit = BoundaryHit;
type _GenerationResult = GenerationResult;

export namespace _PolyhedralBoundedSolidSetIntersector {
    /** Java private nested class; exported only as a type for inventory traceability. */
    export type BoundaryHit = _BoundaryHit;
    export type GenerationResult = _GenerationResult;
}
