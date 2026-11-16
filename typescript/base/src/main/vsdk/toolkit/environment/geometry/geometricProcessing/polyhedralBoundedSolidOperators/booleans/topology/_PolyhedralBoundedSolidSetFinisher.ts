//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import { Boolean as JavaBoolean } from "../../../../../../../../java/lang/Boolean.js";
import { ArrayList } from "../../../../../../../../java/util/ArrayList.js";
import { VSDK } from "../../../../../../common/VSDK.js";
import { Logger } from "../../../../../../common/logging/Logger.js";
import type { Vector3Dd } from "../../../../../../common/linealAlgebra/Vector3Dd.js";
import { Geometry } from "../../../../Geometry.js";
import type { PolyhedralBoundedSolid } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidEulerOperators } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import { PolyhedralBoundedSolidFaceGeometry } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidFaceGeometry.js";
import { PolyhedralBoundedSolidGeometricValidator } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.js";
import { PolyhedralBoundedSolidNumericPolicy } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import type { ToleranceContext } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import { PolyhedralBoundedSolidTopologyEditing } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologyEditing.js";
import type { _PolyhedralBoundedSolidFace } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { _PolyhedralBoundedSolidLoop } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";
import { _PolyhedralBoundedSolidOperator } from "../../_PolyhedralBoundedSolidOperator.js";
import { SolidOperationFailedException } from "../../SolidOperationFailedException.js";
import { _PolyhedralBoundedSolidSetNonIntersectingClassifier } from "../classification/_PolyhedralBoundedSolidSetNonIntersectingClassifier.js";
import { _PolyhedralBoundedSolidSetNullEdgesConnector } from "./_PolyhedralBoundedSolidSetNullEdgesConnector.js";

/**
Finish stage (big phase 4) for set operations, corresponding to the answer
integration step of program [MANT1988].15.15.
*/
export class _PolyhedralBoundedSolidSetFinisher extends _PolyhedralBoundedSolidOperator {
    private static readonly TRACE_PIPELINE_SUMMARY_PROPERTY = "vsdk.setop.tracePipelineSummary";
    private static readonly DEBUG_01_STRUCTURE = 0x01;
    private static readonly DEBUG_06_FINISH = 0x20;

    /**
    §9.2 instrumentation: faces triangulated in the most recent finish().
    Java keeps it in a `ThreadLocal`; this runtime is single-threaded.
    */
    private static lastTriangulatedFaceCount = 0;

    public static getLastTriangulatedFaceCount(): number {
        return _PolyhedralBoundedSolidSetFinisher.lastTriangulatedFaceCount;
    }

    private static isPipelineSummaryTraceEnabled(): boolean {
        return JavaBoolean.getBoolean(_PolyhedralBoundedSolidSetFinisher.TRACE_PIPELINE_SUMMARY_PROPERTY);
    }

    private static tracePipelineSummary(message: string): void {
        if (!_PolyhedralBoundedSolidSetFinisher.isPipelineSummaryTraceEnabled()) {
            return;
        }
        console.log("[SetOpPipelineTrace] " + message);
    }

    private static hasUsableIntegrationRing(face: _PolyhedralBoundedSolidFace | null): boolean {
        let ring: _PolyhedralBoundedSolidLoop;
        let start: _PolyhedralBoundedSolidHalfEdge | null;
        let current: _PolyhedralBoundedSolidHalfEdge | null;
        let reference: Vector3Dd;
        let context: ToleranceContext;
        let guard: number;

        if (
            face === null ||
            !_PolyhedralBoundedSolidSetFinisher.hasCompleteHalfEdgeConnectivity(face) ||
            face.boundariesList.size() < 2 ||
            face.boundariesList.get(1) === null ||
            face.boundariesList.get(1)!.halfEdgesList === null
        ) {
            return false;
        }
        ring = face.boundariesList.get(1)!;
        if (ring.halfEdgesList.size() < 2) {
            return false;
        }
        start = ring.boundaryStartHalfEdge;
        if (start === null || start.startingVertex === null || start.startingVertex.position === null) {
            return false;
        }
        reference = start.startingVertex.position;
        context = PolyhedralBoundedSolidNumericPolicy.forFace(face);
        current = start.next();
        guard = 0;
        while (current !== null && current !== start && guard <= ring.halfEdgesList.size()) {
            if (
                current.startingVertex !== null &&
                current.startingVertex.position !== null &&
                !PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                    reference,
                    current.startingVertex.position,
                    context,
                )
            ) {
                return true;
            }
            current = current.next();
            guard++;
        }
        return false;
    }

    private static hasCompleteHalfEdgeConnectivity(face: _PolyhedralBoundedSolidFace | null): boolean {
        let i: number;

        if (face === null || face.boundariesList === null) {
            return false;
        }
        for (i = 0; i < face.boundariesList.size(); i++) {
            const loop = face.boundariesList.get(i);
            let start: _PolyhedralBoundedSolidHalfEdge;
            let current: _PolyhedralBoundedSolidHalfEdge | null;
            let guard: number;

            if (loop === null || loop.halfEdgesList === null || loop.boundaryStartHalfEdge === null) {
                return false;
            }
            start = loop.boundaryStartHalfEdge;
            current = start;
            guard = 0;
            do {
                if (
                    current === null ||
                    current.parentEdge === null ||
                    current.parentLoop === null ||
                    current.startingVertex === null ||
                    current.mirrorHalfEdge() === null ||
                    current.next() === null ||
                    current.previous() === null
                ) {
                    return false;
                }
                current = current.next();
                guard++;
            } while (current !== start && guard <= loop.halfEdgesList.size() + 1);
            if (current !== start) {
                return false;
            }
        }
        return true;
    }

    private static integrationRingSummary(
        face: _PolyhedralBoundedSolidFace | null,
        pairs: _PolyhedralBoundedSolidSetNullEdgesConnector.ConnectResult,
    ): string {
        if (face === null) {
            return "null";
        }
        if (
            face.boundariesList.size() < 2 ||
            face.boundariesList.get(1) === null ||
            face.boundariesList.get(1)!.halfEdgesList === null
        ) {
            return "face=" + face.id + " boundaries=" + face.boundariesList.size();
        }
        return (
            "face=" +
            face.id +
            " ringSize=" +
            face.boundariesList.get(1)!.halfEdgesList.size() +
            " usable=" +
            _PolyhedralBoundedSolidSetFinisher.hasUsableIntegrationRing(face) +
            " connected=" +
            _PolyhedralBoundedSolidSetFinisher.hasCompleteHalfEdgeConnectivity(face) +
            " pair=" +
            pairs.pairIndexA(face) +
            "/" +
            pairs.pairIndexB(face)
        );
    }

    private static sanitizePairedFaces(
        sonfa: ArrayList<_PolyhedralBoundedSolidFace> | null,
        sonfb: ArrayList<_PolyhedralBoundedSolidFace> | null,
        pairs: _PolyhedralBoundedSolidSetNullEdgesConnector.ConnectResult,
    ): number {
        let i: number;
        let j: number;

        if (sonfa === null || sonfb === null) {
            return 0;
        }

        const matchedA: _PolyhedralBoundedSolidFace[] = [];
        const matchedB: _PolyhedralBoundedSolidFace[] = [];
        const usedB = new Array<boolean>(sonfb.size()).fill(false);

        for (i = 0; i < sonfa.size(); i++) {
            const faceA = sonfa.get(i);
            let pairIndexA: number;
            const validA = _PolyhedralBoundedSolidSetFinisher.hasUsableIntegrationRing(faceA);

            if (!validA) {
                _PolyhedralBoundedSolidSetFinisher.tracePipelineSummary(
                    "finish sanitize skip A " + _PolyhedralBoundedSolidSetFinisher.integrationRingSummary(faceA, pairs),
                );
                continue;
            }

            pairIndexA = pairs.pairIndexA(faceA);
            for (j = 0; j < sonfb.size(); j++) {
                const faceB = sonfb.get(j);
                let pairIndexB: number;
                let validB: boolean;

                if (usedB[j]!) {
                    continue;
                }
                validB = _PolyhedralBoundedSolidSetFinisher.hasUsableIntegrationRing(faceB);
                if (!validB) {
                    _PolyhedralBoundedSolidSetFinisher.tracePipelineSummary(
                        "finish sanitize skip B " +
                            _PolyhedralBoundedSolidSetFinisher.integrationRingSummary(faceB, pairs),
                    );
                    continue;
                }
                pairIndexB = pairs.pairIndexB(faceB);
                if (pairIndexA !== -1 && pairIndexA === pairIndexB) {
                    _PolyhedralBoundedSolidSetFinisher.tracePipelineSummary(
                        "finish sanitize match A " +
                            _PolyhedralBoundedSolidSetFinisher.integrationRingSummary(faceA, pairs) +
                            " B " +
                            _PolyhedralBoundedSolidSetFinisher.integrationRingSummary(faceB, pairs),
                    );
                    matchedA.push(faceA);
                    matchedB.push(faceB);
                    usedB[j] = true;
                    break;
                }
            }
        }

        if (matchedA.length === 0 && !sonfa.isEmpty() && sonfa.size() === sonfb.size()) {
            // [MANT1988] Program 15.15 pairs sonfa[i] with sonfb[i]. Connect
            // tags every pair with its index; when none of them matches, the
            // null faces cannot be paired and finish has nothing to glue.
            // This used to fall back to list order; it is now an error.
            throw new SolidOperationFailedException(
                "finish: no null face pair matched by pair index (sonfa=" +
                    sonfa.size() +
                    " sonfb=" +
                    sonfb.size() +
                    ")",
            );
        }

        sonfa.clear();
        sonfa.addAll(matchedA);
        sonfb.clear();
        sonfb.addAll(matchedB);
        _PolyhedralBoundedSolidSetFinisher.tracePipelineSummary("finish sanitize matched=" + matchedA.length);
        return matchedA.length;
    }

    /**
    Restores the planar-face invariant of [MANT1988].10.2.1 after the answer
    integration step.  The Connect phase merges adjacent operand faces via
    `lkef` whenever a null-edge crosses their shared boundary; for tessellated
    curved surfaces (spheres, cylinders) those neighbours have distinct face
    normals, so the merged face is non-planar.  Subsequent `lkfmrh` + `loopGlue`
    in Finish carry that non-planarity into single-loop result faces.

    This routine fans each offending face into triangles using the same
    `lmef(scan.next, scan.previous, newId)` split used by
    `PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar`. Each
    split peels off one triangle (always planar) from the remaining polygon
    until the polygon itself is a triangle, restoring the planarity invariant
    expected by `validateIntermediate`.
    */
    private static findNonDegenerateEar(
        start: _PolyhedralBoundedSolidHalfEdge | null,
        loopSize: number,
        context: ToleranceContext | null,
    ): _PolyhedralBoundedSolidHalfEdge | null {
        let candidate: _PolyhedralBoundedSolidHalfEdge | null;
        let nextHe: _PolyhedralBoundedSolidHalfEdge | null;
        let prevHe: _PolyhedralBoundedSolidHalfEdge | null;
        let bestCandidate: _PolyhedralBoundedSolidHalfEdge | null;
        let p0: Vector3Dd;
        let p1: Vector3Dd;
        let p2: Vector3Dd;
        let a: Vector3Dd;
        let b: Vector3Dd;
        let bestSinTheta: number;
        let safety: number;

        if (start === null || context === null || loopSize <= 0) {
            return null;
        }
        bestCandidate = null;
        bestSinTheta = 0.0;
        candidate = start;
        safety = 0;
        do {
            nextHe = candidate!.next();
            prevHe = candidate!.previous();
            if (
                nextHe !== null &&
                prevHe !== null &&
                nextHe !== prevHe &&
                candidate!.parentLoop !== null &&
                nextHe.parentLoop === candidate!.parentLoop &&
                prevHe.parentLoop === candidate!.parentLoop &&
                candidate!.startingVertex !== null &&
                nextHe.startingVertex !== null &&
                prevHe.startingVertex !== null
            ) {
                p0 = prevHe.startingVertex.position;
                p1 = candidate!.startingVertex.position;
                p2 = nextHe.startingVertex.position;
                if (p0 !== null && p1 !== null && p2 !== null) {
                    a = p1.subtract(p0);
                    b = p2.subtract(p0);
                    // Use the same normalized-vector collinearity test as
                    // validateFacePointsAreCoplanar to guarantee the ear
                    // triangle will pass planarity after the split.
                    if (a.length() > context.epsilon() && b.length() > context.epsilon()) {
                        const an = a.normalized();
                        const bn = b.normalized();
                        const aDotB = Math.abs(an.dotProduct(bn));
                        const sinTheta = an.crossProduct(bn).length();
                        if (aDotB < 1.0 - context.unitVectorTolerance()) {
                            return candidate;
                        }
                        if (sinTheta > bestSinTheta) {
                            bestSinTheta = sinTheta;
                            bestCandidate = candidate;
                        }
                    }
                }
            }
            candidate = candidate!.next();
            safety++;
        } while (candidate !== null && candidate !== start && safety <= loopSize + 1);
        // No ear with sufficient angle found — fall back to the widest ear
        // above epsilon for cases where the polygon is nearly degenerate.
        if (bestCandidate !== null && bestSinTheta > context.unitVectorTolerance() / 10.0) {
            return bestCandidate;
        }
        return null;
    }

    /**
    Returns true when the loop has at least one vertex whose position
    coincides with another vertex earlier in the loop (i.e., the boundary
    is self-touching / figure-8). Uses scaled bigEpsilon from tol.

    A self-touching inner ring cannot be extracted as a valid face via lmfkrh
    because the resulting face would be a degenerate inverted membrane.
    */
    private static hasSelfTouchingVertex(loop: _PolyhedralBoundedSolidLoop | null, tol: ToleranceContext): boolean {
        if (loop === null) {
            return false;
        }
        const size = loop.halfEdgesList.size();
        let i: number;
        let j: number;
        for (i = 1; i < size; i++) {
            for (j = 0; j < i; j++) {
                if (
                    PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                        loop.halfEdgesList.get(i)!.startingVertex.position,
                        loop.halfEdgesList.get(j)!.startingVertex.position,
                        tol,
                    )
                ) {
                    return true;
                }
            }
        }
        return false;
    }

    private static extractInnerLoopsOfNonPlanarFace(
        solid: PolyhedralBoundedSolid,
        face: _PolyhedralBoundedSolidFace | null,
    ): void {
        let safety: number;
        let maxLoops: number;
        let tol: ToleranceContext;

        if (face === null || face.boundariesList === null) {
            return;
        }
        maxLoops = face.boundariesList.size();
        safety = 0;
        tol = PolyhedralBoundedSolidNumericPolicy.forSolid(solid);
        while (face.boundariesList.size() > 1 && safety <= maxLoops) {
            safety++;
            const innerLoop = face.boundariesList.get(1);
            if (innerLoop === null) {
                break;
            }
            // §3 guard: a self-touching inner loop would produce an inverted
            // membrane face when extracted via lmfkrh. Retain it as-is so the
            // containing face keeps its inner ring but no invalid face is created.
            if (_PolyhedralBoundedSolidSetFinisher.hasSelfTouchingVertex(innerLoop, tol)) {
                Logger.reportMessage(
                    solid,
                    VSDK.WARNING,
                    "extractInnerLoopsOfNonPlanarFace",
                    "finish: skipped self-touching inner loop in face " +
                        face.id +
                        "; retaining loop to avoid membrane artifact",
                );
                break;
            }
            if (PolyhedralBoundedSolidEulerOperators.lmfkrh(solid, innerLoop, solid.getMaxFaceId() + 1) === null) {
                break;
            }
        }
    }

    public static triangulateNonPlanarFaces(solid: PolyhedralBoundedSolid): void {
        let i: number;
        let safetyCount: number;
        let maxIterations: number;
        let initialCount: number;

        _PolyhedralBoundedSolidSetFinisher.lastTriangulatedFaceCount = 0;
        i = 0;
        safetyCount = 0;
        initialCount = solid.getPolygonsList().size();
        maxIterations = 50 * (initialCount + 1);
        while (i < solid.getPolygonsList().size() && safetyCount < maxIterations) {
            let face: _PolyhedralBoundedSolidFace;
            let scan: _PolyhedralBoundedSolidHalfEdge | null;
            let ear: _PolyhedralBoundedSolidHalfEdge | null;
            let next: _PolyhedralBoundedSolidHalfEdge | null;
            let prev: _PolyhedralBoundedSolidHalfEdge | null;
            let context: ToleranceContext;
            let loopSize: number;
            let newFaceId: number;

            safetyCount++;
            face = solid.getPolygonsList().get(i)!;
            // §3: For multi-loop faces, check whether any inner loop is self-touching
            // BEFORE the planarity check. A self-touching inner ring can be coplanar
            // with the outer boundary, causing validateFaceIsPlanar to return true and
            // skip the face — leaving the guard in extractInnerLoopsOfNonPlanarFace
            // unreachable. We only bypass the planarity gate when at least one inner
            // loop is self-touching; valid inner rings preserve the baseline behaviour.
            if (face.boundariesList.size() > 1) {
                let hasSelfTouching = false;
                const tol = PolyhedralBoundedSolidNumericPolicy.forSolid(solid);
                let li: number;
                for (li = 1; li < face.boundariesList.size(); li++) {
                    if (_PolyhedralBoundedSolidSetFinisher.hasSelfTouchingVertex(face.boundariesList.get(li), tol)) {
                        hasSelfTouching = true;
                        break;
                    }
                }
                if (hasSelfTouching) {
                    _PolyhedralBoundedSolidSetFinisher.extractInnerLoopsOfNonPlanarFace(solid, face);
                    if (face.boundariesList.size() !== 1) {
                        i++;
                        continue;
                    }
                    // All inner loops were extracted or guarded; face now has
                    // 1 loop — fall through to planarity check.
                }
            }
            if (PolyhedralBoundedSolidGeometricValidator.validateFaceIsPlanar(face)) {
                i++;
                continue;
            }
            if (face.boundariesList.size() > 1) {
                _PolyhedralBoundedSolidSetFinisher.extractInnerLoopsOfNonPlanarFace(solid, face);
                if (face.boundariesList.size() !== 1) {
                    i++;
                    continue;
                }
            }
            loopSize = face.boundariesList.get(0)!.halfEdgesList.size();
            if (loopSize <= 3) {
                // Degenerate or collinear small face.
                const loop0 = face.boundariesList.get(0)!;
                // §9.5-degenerate: size-1 self-referential face with orphaned
                // mirror cannot be killed via lkef. Prune it directly — the
                // edge is dangling (mirror has no parentLoop) so removing the
                // face and edge is safe and leaves the solid consistent.
                if (
                    loopSize === 1 &&
                    loop0.halfEdgesList.get(0) !== null &&
                    loop0.halfEdgesList.get(0)!.next() === loop0.halfEdgesList.get(0)
                ) {
                    const h0 = loop0.halfEdgesList.get(0)!;
                    if (h0.mirrorHalfEdge() === null || h0.mirrorHalfEdge()!.parentLoop === null) {
                        solid.getPolygonsList().remove(i);
                        if (h0.parentEdge !== null) {
                            let edgeIdx: number;
                            for (edgeIdx = 0; edgeIdx < solid.getEdgesList().size(); edgeIdx++) {
                                if (solid.getEdgesList().get(edgeIdx) === h0.parentEdge) {
                                    solid.getEdgesList().remove(edgeIdx);
                                    break;
                                }
                            }
                        }
                        continue; // don't increment i: next face slides to i
                    }
                }
                // Try to absorb into adjacent face via lkef. When lkef
                // succeeds, the absorbed vertices may make the adjacent face
                // non-planar; restart the scan from index 0 so it is re-checked.
                let killed = false;
                let k: number;
                for (k = 0; k < loop0.halfEdgesList.size(); k++) {
                    const h = loop0.halfEdgesList.get(k);
                    if (
                        h !== null &&
                        h.mirrorHalfEdge() !== null &&
                        h.mirrorHalfEdge()!.parentLoop !== null &&
                        h.mirrorHalfEdge()!.parentLoop.parentFace !== face
                    ) {
                        PolyhedralBoundedSolidEulerOperators.lkef(solid, h.mirrorHalfEdge()!, h);
                        killed = true;
                        break;
                    }
                }
                if (killed) {
                    i = 0; // restart to re-check faces that absorbed the triangle
                } else {
                    i++;
                }
                continue;
            }
            scan = face.boundariesList.get(0)!.boundaryStartHalfEdge;
            if (scan === null) {
                i++;
                continue;
            }
            context = PolyhedralBoundedSolidNumericPolicy.forFace(face);
            ear = _PolyhedralBoundedSolidSetFinisher.findNonDegenerateEar(scan, loopSize, context);
            if (ear === null) {
                i++;
                continue;
            }
            next = ear.next();
            prev = ear.previous();
            if (
                next === null ||
                prev === null ||
                next === prev ||
                next.parentLoop !== ear.parentLoop ||
                prev.parentLoop !== ear.parentLoop
            ) {
                i++;
                continue;
            }
            newFaceId =
                _PolyhedralBoundedSolidOperator.idNamespace !== null
                    ? _PolyhedralBoundedSolidOperator.idNamespace.nextFaceId(solid)
                    : solid.getMaxFaceId() + 1;
            if (PolyhedralBoundedSolidEulerOperators.lmef(solid, next, prev, newFaceId) === null) {
                i++;
            } else {
                _PolyhedralBoundedSolidSetFinisher.lastTriangulatedFaceCount++;
            }
        }
    }

    /**
    Answer integrator for the set-operations pipeline.
    Following program [MANT1988].15.15.
    */
    private static exchange(faces: ArrayList<_PolyhedralBoundedSolidFace>, i: number, j: number): void {
        const exchanged = faces.get(i);

        faces.set(i, faces.get(j));
        faces.set(j, exchanged);
    }

    /**
    Adds to `counts` the answer for one face: element 0 when a point of its
    interior is strictly inside `other`, element 1 when strictly outside.
    A face without such a point, or with it on the boundary of `other`,
    adds nothing.
    */
    private static countSide(
        face: _PolyhedralBoundedSolidFace,
        other: PolyhedralBoundedSolid,
        otherFaces: PolyhedralBoundedSolidFaceGeometry[],
        counts: number[],
    ): void {
        const witness = _PolyhedralBoundedSolidSetNonIntersectingClassifier.faceInteriorWitness(face);

        if (witness === null) {
            return;
        }
        const status = _PolyhedralBoundedSolidSetNonIntersectingClassifier.classifyPointAgainstSolid(
            other,
            otherFaces,
            witness,
        );
        if (status === Geometry.INSIDE) {
            counts[0]!++;
        } else if (status === Geometry.OUTSIDE) {
            counts[1]!++;
        }
    }

    /**
    `countSide` over the faces across the half-edges of `loop`, each
    face once.
    */
    private static sidesAcross(
        loop: _PolyhedralBoundedSolidLoop,
        other: PolyhedralBoundedSolid,
        otherFaces: PolyhedralBoundedSolidFaceGeometry[],
    ): number[] {
        const counts = [0, 0];
        // Java's IdentityHashMap: JavaScript Map keys compare by identity.
        const counted = new Map<_PolyhedralBoundedSolidFace, boolean>();
        const start = loop.boundaryStartHalfEdge;
        let he = start;
        let guard = loop.halfEdgesList.size();

        if (start === null) {
            return counts;
        }
        do {
            const mate = he!.mirrorHalfEdge();
            if (
                mate !== null &&
                mate.parentLoop !== null &&
                mate.parentLoop.parentFace !== loop.parentFace &&
                !counted.has(mate.parentLoop.parentFace)
            ) {
                counted.set(mate.parentLoop.parentFace, true);
                _PolyhedralBoundedSolidSetFinisher.countSide(mate.parentLoop.parentFace, other, otherFaces, counts);
            }
            he = he!.next();
            guard--;
        } while (he !== null && he !== start && guard >= 0);
        return counts;
    }

    /**
    True when the outer loop of `face` runs through the points of its ring
    and no others: the face is the null face between the two copies of a
    cut, without area of its own.
    */
    private static isBand(face: _PolyhedralBoundedSolidFace): boolean {
        const outer = face.boundariesList.get(0)!;
        const ring = face.boundariesList.get(1)!;
        const context = PolyhedralBoundedSolidNumericPolicy.forFace(face);
        let i: number;
        let j: number;

        for (i = 0; i < outer.halfEdgesList.size(); i++) {
            let found = false;
            for (j = 0; j < ring.halfEdgesList.size() && !found; j++) {
                found = PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                    outer.halfEdgesList.get(i)!.startingVertex.position,
                    ring.halfEdgesList.get(j)!.startingVertex.position,
                    context,
                );
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    /**
    `face` is a null face of connect: its ring is one copy of a cut. By
    convention the faces across the ring are the part of the solid inside
    `other`, and what lies on the side of the outer loop (the faces across
    it for a band, the face itself otherwise) is the part outside. True
    when both sides say the opposite and neither has a face that says
    otherwise.
    */
    private static sidesAreSwapped(
        face: _PolyhedralBoundedSolidFace,
        other: PolyhedralBoundedSolid,
        otherFaces: PolyhedralBoundedSolidFaceGeometry[],
        cut: number,
        name: string,
    ): boolean {
        const inside = _PolyhedralBoundedSolidSetFinisher.sidesAcross(face.boundariesList.get(1)!, other, otherFaces);
        let outside: number[];

        if (_PolyhedralBoundedSolidSetFinisher.isBand(face)) {
            outside = _PolyhedralBoundedSolidSetFinisher.sidesAcross(face.boundariesList.get(0)!, other, otherFaces);
        } else {
            outside = [0, 0];
            _PolyhedralBoundedSolidSetFinisher.countSide(face, other, otherFaces, outside);
        }
        const swapped = inside[0] === 0 && outside[1] === 0 && inside[1]! > 0 && outside[0]! > 0;
        _PolyhedralBoundedSolidSetFinisher.tracePipelineSummary(
            "finish sides of cut " +
                cut +
                " on " +
                name +
                ": the part taken for inside has " +
                inside[0] +
                " faces inside and " +
                inside[1] +
                " outside; the part taken for outside " +
                outside[0] +
                " and " +
                outside[1] +
                (swapped ? "; exchanged" : ""),
        );
        return swapped;
    }

    /** Adds to `reached` every face connected to `seed` through edges. */
    private static flood(
        seed: _PolyhedralBoundedSolidFace | null,
        reached: Map<_PolyhedralBoundedSolidFace, boolean>,
        component: ArrayList<_PolyhedralBoundedSolidFace> | null,
    ): void {
        const pending = new ArrayList<_PolyhedralBoundedSolidFace>();
        let i: number;

        if (seed === null || reached.has(seed)) {
            return;
        }
        reached.set(seed, true);
        pending.add(seed);
        while (!pending.isEmpty()) {
            const face = pending.remove(pending.size() - 1);
            if (component !== null) {
                component.add(face);
            }
            for (i = 0; i < face.boundariesList.size(); i++) {
                const loop = face.boundariesList.get(i)!;
                const start = loop.boundaryStartHalfEdge;
                let he = start;
                let guard = loop.halfEdgesList.size();
                if (start === null) {
                    continue;
                }
                do {
                    const mate = he!.mirrorHalfEdge();
                    if (
                        mate !== null &&
                        mate.parentLoop !== null &&
                        mate.parentLoop.parentFace !== null &&
                        !reached.has(mate.parentLoop.parentFace)
                    ) {
                        reached.set(mate.parentLoop.parentFace, true);
                        pending.add(mate.parentLoop.parentFace);
                    }
                    he = he!.next();
                    guard--;
                } while (he !== null && he !== start && guard >= 0);
            }
        }
    }

    /**
    One face of every shell of `solid` that no cut reaches and that lies on
    the wanted side of `other`: inside when `keepInside`, outside
    otherwise. The side is the one of the first vertex of the shell that is
    not on the boundary of `other`; a shell with every vertex on that
    boundary is left out.
    @param cutFaces the null faces of `solid` and the faces made from their
           rings: every cut part of the solid is connected to one of them
    */
    private static untouchedShellsToKeep(
        solid: PolyhedralBoundedSolid,
        cutFaces: ArrayList<_PolyhedralBoundedSolidFace>,
        other: PolyhedralBoundedSolid,
        keepInside: boolean,
    ): ArrayList<_PolyhedralBoundedSolidFace> {
        const seeds = new ArrayList<_PolyhedralBoundedSolidFace>();
        const reached = new Map<_PolyhedralBoundedSolidFace, boolean>();
        let otherFaces: PolyhedralBoundedSolidFaceGeometry[] | null = null;
        let i: number;
        let j: number;
        let k: number;

        for (i = 0; i < cutFaces.size(); i++) {
            _PolyhedralBoundedSolidSetFinisher.flood(cutFaces.get(i), reached, null);
        }
        for (i = 0; i < solid.getPolygonsList().size(); i++) {
            const face = solid.getPolygonsList().get(i)!;
            if (reached.has(face)) {
                continue;
            }
            const shell = new ArrayList<_PolyhedralBoundedSolidFace>();
            _PolyhedralBoundedSolidSetFinisher.flood(face, reached, shell);
            if (otherFaces === null) {
                _PolyhedralBoundedSolidOperator.setNumericContext(
                    PolyhedralBoundedSolidNumericPolicy.forSolids(solid, other),
                );
                otherFaces = PolyhedralBoundedSolidFaceGeometry.of(other);
            }
            let side = Geometry.LIMIT;
            for (j = 0; j < shell.size() && side === Geometry.LIMIT; j++) {
                const member = shell.get(j);
                for (k = 0; k < member.boundariesList.size() && side === Geometry.LIMIT; k++) {
                    const he = member.boundariesList.get(k)!.boundaryStartHalfEdge;
                    if (he === null || he.startingVertex === null) {
                        continue;
                    }
                    side = _PolyhedralBoundedSolidSetNonIntersectingClassifier.classifyPointAgainstSolid(
                        other,
                        otherFaces,
                        he.startingVertex.position,
                    );
                }
            }
            if ((keepInside && side === Geometry.INSIDE) || (!keepInside && side === Geometry.OUTSIDE)) {
                seeds.add(face);
                _PolyhedralBoundedSolidSetFinisher.tracePipelineSummary(
                    "finish keeps a whole shell of " +
                        shell.size() +
                        " faces, " +
                        (keepInside ? "inside" : "outside") +
                        " the other solid",
                );
            }
        }
        return seeds;
    }

    public static finish(
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
        outRes: PolyhedralBoundedSolid,
        op: number,
        debugFlags: number,
        sonfa: ArrayList<_PolyhedralBoundedSolidFace>,
        sonfb: ArrayList<_PolyhedralBoundedSolidFace>,
        pairs: _PolyhedralBoundedSolidSetNullEdgesConnector.ConnectResult | null,
    ): void {
        let i: number;
        let inda: number;
        let indb: number;
        let f: _PolyhedralBoundedSolidFace | null;

        if ((debugFlags & _PolyhedralBoundedSolidSetFinisher.DEBUG_01_STRUCTURE) !== 0x00) {
            console.log(
                "- 4. ------------------------------------------------------------------------------------------------------------------------------------------------------",
            );
            console.log("setOpFinish");
        }

        if ((debugFlags & _PolyhedralBoundedSolidSetFinisher.DEBUG_06_FINISH) !== 0x00) {
            console.log("TESTING FINISH: " + sonfa.size());
        }
        _PolyhedralBoundedSolidSetFinisher.tracePipelineSummary(
            "finish start op=" + op + " sonfa=" + sonfa.size() + " sonfb=" + sonfb.size(),
        );

        const oldsize = _PolyhedralBoundedSolidSetFinisher.sanitizePairedFaces(sonfa, sonfb, pairs!);
        inda = op === _PolyhedralBoundedSolidOperator.INTERSECTION ? sonfa.size() : 0;
        indb = op === _PolyhedralBoundedSolidOperator.UNION ? 0 : sonfb.size();

        // [MANT1988] Program 15.15 takes sonf[i] for the part of the solid
        // outside the other one and the face lmfkrh makes from its ring
        // for the part inside. That rests on the orientation every null
        // edge got on its way here. The parts themselves say which is
        // which: a point of the faces on each side of the cut is inside
        // the other solid or outside it. When they say the opposite of the
        // convention for a whole cut, the two faces are exchanged
        // (plan-stage11 Phase F).
        const swappedA: boolean[] = new Array<boolean>(oldsize).fill(false);
        const swappedB: boolean[] = new Array<boolean>(oldsize).fill(false);
        if (oldsize > 0) {
            // Nothing edits the two solids while the sides are read, so
            // their face planes and boxes are computed once.
            _PolyhedralBoundedSolidOperator.setNumericContext(
                PolyhedralBoundedSolidNumericPolicy.forSolids(inSolidA, inSolidB),
            );
            const facesOfA = PolyhedralBoundedSolidFaceGeometry.of(inSolidA);
            const facesOfB = PolyhedralBoundedSolidFaceGeometry.of(inSolidB);
            for (i = 0; i < oldsize; i++) {
                swappedA[i] = _PolyhedralBoundedSolidSetFinisher.sidesAreSwapped(
                    sonfa.get(i),
                    inSolidB,
                    facesOfB,
                    i,
                    "A",
                );
                swappedB[i] = _PolyhedralBoundedSolidSetFinisher.sidesAreSwapped(
                    sonfb.get(i),
                    inSolidA,
                    facesOfA,
                    i,
                    "B",
                );
            }
        }

        for (i = 0; i < oldsize; i++) {
            f = PolyhedralBoundedSolidEulerOperators.lmfkrh(
                inSolidA,
                sonfa.get(i).boundariesList.get(1)!,
                inSolidA.getMaxFaceId() + 1,
            );
            sonfa.add(f!);

            f = PolyhedralBoundedSolidEulerOperators.lmfkrh(
                inSolidB,
                sonfb.get(i).boundariesList.get(1)!,
                inSolidB.getMaxFaceId() + 1,
            );
            sonfb.add(f!);
        }

        for (i = 0; i < oldsize; i++) {
            if (swappedA[i]) {
                _PolyhedralBoundedSolidSetFinisher.exchange(sonfa, i, i + oldsize);
            }
            if (swappedB[i]) {
                _PolyhedralBoundedSolidSetFinisher.exchange(sonfb, i, i + oldsize);
            }
        }

        // Shells that no intersection curve reaches are not connected to
        // any null face, so the moves below never see them. Each one lies
        // wholly inside or wholly outside the other solid and belongs to
        // the result on the same terms as the cut parts: A outside B for
        // the union and the difference, A inside B for the intersection;
        // B outside A for the union, B inside A otherwise. They used to be
        // left behind without an error (plan-stage11 Phase F): the union
        // of a two-piece solid with a solid that crosses only one of the
        // pieces lost the other piece.
        const wholeShellsOfA = _PolyhedralBoundedSolidSetFinisher.untouchedShellsToKeep(
            inSolidA,
            sonfa,
            inSolidB,
            op === _PolyhedralBoundedSolidOperator.INTERSECTION,
        );
        const wholeShellsOfB = _PolyhedralBoundedSolidSetFinisher.untouchedShellsToKeep(
            inSolidB,
            sonfb,
            inSolidA,
            op !== _PolyhedralBoundedSolidOperator.UNION,
        );

        if (op === _PolyhedralBoundedSolidOperator.SUBTRACT) {
            inSolidB.revert();
        }

        for (i = 0; i < oldsize; i++) {
            _PolyhedralBoundedSolidOperator.movefac(sonfa.get(i + inda), outRes);
            _PolyhedralBoundedSolidOperator.movefac(sonfb.get(i + indb), outRes);
        }
        for (i = 0; i < wholeShellsOfA.size(); i++) {
            _PolyhedralBoundedSolidOperator.movefac(wholeShellsOfA.get(i), outRes);
        }
        for (i = 0; i < wholeShellsOfB.size(); i++) {
            _PolyhedralBoundedSolidOperator.movefac(wholeShellsOfB.get(i), outRes);
        }

        _PolyhedralBoundedSolidOperator.cleanup(outRes);

        for (i = 0; i < oldsize; i++) {
            PolyhedralBoundedSolidEulerOperators.lkfmrh(outRes, sonfa.get(i + inda), sonfb.get(i + indb));
            PolyhedralBoundedSolidTopologyEditing.loopGlue(outRes, sonfa.get(i + inda));
        }
        _PolyhedralBoundedSolidOperator.cleanup(outRes);
        _PolyhedralBoundedSolidSetFinisher.triangulateNonPlanarFaces(outRes);
        PolyhedralBoundedSolidTopologyEditing.compactIds(outRes);
        _PolyhedralBoundedSolidSetFinisher.tracePipelineSummary(
            "finish end outRes faces=" +
                outRes.getPolygonsList().size() +
                " edges=" +
                outRes.getEdgesList().size() +
                " vertices=" +
                outRes.getVerticesList().size(),
        );
    }
}
