//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import { Boolean as JavaBoolean } from "../../../../../../../../java/lang/Boolean.js";
import { Integer } from "../../../../../../../../java/lang/Integer.js";
import { StringBuilder } from "../../../../../../../../java/lang/StringBuilder.js";
import { platformGetProperty } from "../../../../../../../../java/lang/_PlatformProperties.js";
import { ArrayList } from "../../../../../../../../java/util/ArrayList.js";
import { Collections } from "../../../../../../../../java/util/Collections.js";
import { HashMap } from "../../../../../../../../java/util/HashMap.js";
import { HashSet } from "../../../../../../../../java/util/HashSet.js";
import { LinkedHashMap } from "../../../../../../../../java/util/LinkedHashMap.js";
import { PolyhedralBoundedSolidStatistics } from "../../../../../../common/statistics/PolyhedralBoundedSolidStatistics.js";
import type { Vector3Dd } from "../../../../../../common/linealAlgebra/Vector3Dd.js";
import { Geometry } from "../../../../Geometry.js";
import type { PolyhedralBoundedSolid } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidEulerOperators } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import { PolyhedralBoundedSolidNumericPolicy } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import type { _PolyhedralBoundedSolidEdge } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.js";
import type { _PolyhedralBoundedSolidFace } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { _PolyhedralBoundedSolidLoop } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";
import { _PolyhedralBoundedSolidOperator } from "../../_PolyhedralBoundedSolidOperator.js";
import { _PolyhedralBoundedSolidSetGeometricPredicateProcessor } from "../intersection/_PolyhedralBoundedSolidSetGeometricPredicateProcessor.js";
import { _PolyhedralBoundedSolidSetIntersectionCurveBuilder } from "../intersection/_PolyhedralBoundedSolidSetIntersectionCurveBuilder.js";
import type { _PolyhedralBoundedSolidSetOperatorNullEdge } from "./_PolyhedralBoundedSolidSetOperatorNullEdge.js";

class ConnectResult {
    private readonly sonfaList: ArrayList<_PolyhedralBoundedSolidFace>;
    private readonly sonfbList: ArrayList<_PolyhedralBoundedSolidFace>;

    public constructor(sonfa: ArrayList<_PolyhedralBoundedSolidFace>, sonfb: ArrayList<_PolyhedralBoundedSolidFace>) {
        this.sonfaList = sonfa;
        this.sonfbList = sonfb;
    }

    public sonfa(): ArrayList<_PolyhedralBoundedSolidFace> {
        return this.sonfaList;
    }

    public sonfb(): ArrayList<_PolyhedralBoundedSolidFace> {
        return this.sonfbList;
    }
}

/**
Mutable carrier for the (nea, neb) pair returned by
`sgetnextnulledge(NullEdgePair)`, mirroring the out-param
style used by [MANT1988] Program 15.14 in C.
*/
class NullEdgePair {
    public nea: _PolyhedralBoundedSolidSetOperatorNullEdge | null = null;
    public neb: _PolyhedralBoundedSolidSetOperatorNullEdge | null = null;
    public pairIndex = 0;
}

/**
Connect stage (big phase 3) for set operations: null-edges pairing and joins,
following section [MANT1988].15.7 and programs [MANT1988].15.13 and
[MANT1988].15.14.
*/
export class _PolyhedralBoundedSolidSetNullEdgesConnector extends _PolyhedralBoundedSolidOperator {
    private static readonly TRACE_PIPELINE_SUMMARY_PROPERTY = "vsdk.setop.tracePipelineSummary";
    private static readonly KEEP_INSERTION_ORDER_PROPERTY = "vsdk.setop.connect.keepInsertionOrder";
    private static readonly DEBUG_01_STRUCTURE = 0x01;
    private static readonly DEBUG_05_CONNECT = 0x10;
    private static readonly DEBUG_99_SHOWOPERATIONS = 0x40;
    private static readonly ENDPOINT_SOLID_A = 0;
    private static readonly ENDPOINT_SOLID_B = 1;

    private static debugFlags = 0;
    private static operation = 0;
    private nextNullEdgeIndex = 0;
    private sonea: ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> | null = null;
    private soneb: ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> | null = null;
    private static endsa: ArrayList<_PolyhedralBoundedSolidHalfEdge> | null = null;
    private static endsb: ArrayList<_PolyhedralBoundedSolidHalfEdge> | null = null;
    private sonfa: ArrayList<_PolyhedralBoundedSolidFace> | null = null;
    private sonfb: ArrayList<_PolyhedralBoundedSolidFace> | null = null;
    private static sonfaPairIndexByFaceId: HashMap<number, number> | null = null;
    private static sonfbPairIndexByFaceId: HashMap<number, number> | null = null;
    private static lastLooseACount = 0;
    private static lastLooseBCount = 0;
    private static lastSonfaCount = 0;
    private static lastSonfbCount = 0;
    private static lastPairCount = 0;
    private static currentConnectPairIndex = 0;
    private static nextSyntheticPairIndex = 0;

    /**
    Structural report of the intersection curves reconstructed from the
    sonea/soneb pairs of the most recent `connect` call. Captured
    unconditionally (the reconstruction is cheap) so tests and diagnostics
    can audit curve closure without enabling pipeline traces.
    */
    public static lastCurveReport: _PolyhedralBoundedSolidSetIntersectionCurveBuilder.Report | null = null;

    /**
    Test-only injection point: when non-null and matching the pair count,
    `sortNullEdges()` reorders sonea/soneb by this permutation
    (entry `position -> originalIndex`) instead of the production
    ordering. Lets contract tests probe the connect stage's sensitivity to
    processing order without any production flag. Always null in production;
    cleared by the caller after use.
    */
    public static testOnlyForcedConnectOrder: number[] | null = null;

    /**
    True when `sortNullEdges()` already oriented every strut along
    the intersection curves (mythosPlan §5.3). The connect loop must then
    respect that orientation instead of re-normalizing edge halves by
    vertex-id comparison (the legacy rule, which encodes classifier emission
    order and breaks when that order is not the curve order).
    */
    private static curveOrientationApplied = false;

    /**
    Junction adjacency in processing-position space when the curve order is
    active (from
    `_PolyhedralBoundedSolidSetIntersectionCurveBuilder`); null
    otherwise. Used to restrict the near-miss ring rescue to true curve
    neighbors.
    */
    private static curveNeighborPositions: number[][] | null = null;

    /**
    Processing pair index that pushed each loose entry, parallel to
    `endsa`/`endsb`.
    */
    private static endsPairIndex: ArrayList<number> | null = null;

    private static isPipelineSummaryTraceEnabled(): boolean {
        return JavaBoolean.getBoolean(_PolyhedralBoundedSolidSetNullEdgesConnector.TRACE_PIPELINE_SUMMARY_PROPERTY);
    }

    private static isKeepInsertionOrderEnabled(): boolean {
        const propertyValue = platformGetProperty(
            _PolyhedralBoundedSolidSetNullEdgesConnector.KEEP_INSERTION_ORDER_PROPERTY,
        );
        if (propertyValue === null) {
            return true;
        }
        return JavaBoolean.parseBoolean(propertyValue);
    }

    private static tracePipelineSummary(message: string): void {
        if (!_PolyhedralBoundedSolidSetNullEdgesConnector.isPipelineSummaryTraceEnabled()) {
            return;
        }
        console.log("[SetOpPipelineTrace] " + message);
    }

    private static summarizeHalfEdge(he: _PolyhedralBoundedSolidHalfEdge | null): string {
        if (he === null) {
            return "null";
        }

        let from = "?";
        let to = "?";
        let face = "?";

        if (he.startingVertex !== null) {
            from = Integer.toString(he.startingVertex.id);
        }
        if (he.parentLoop !== null) {
            const next = he.next();
            if (next !== null && next.startingVertex !== null) {
                to = Integer.toString(next.startingVertex.id);
            }
            if (he.parentLoop.parentFace !== null) {
                face = Integer.toString(he.parentLoop.parentFace.id);
            }
        }
        let fromPoint = "?";
        let toPoint = "?";

        if (he.startingVertex !== null) {
            fromPoint = he.startingVertex.position.toString();
        }
        if (he.parentLoop !== null) {
            const next = he.next();
            if (next !== null && next.startingVertex !== null) {
                toPoint = next.startingVertex.position.toString();
            }
        }
        return "he(v=" + from + "->" + to + ",f=" + face + ",p=" + fromPoint + "->" + toPoint + ")";
    }

    private static summarizeNullEdge(edge: _PolyhedralBoundedSolidSetOperatorNullEdge | null): string {
        if (edge === null || edge.e === null) {
            return "null";
        }
        return (
            _PolyhedralBoundedSolidSetNullEdgesConnector.summarizeHalfEdge(edge.e.rightHalf) +
            " | " +
            _PolyhedralBoundedSolidSetNullEdgesConnector.summarizeHalfEdge(edge.e.leftHalf)
        );
    }

    private static setCurrentConnectContext(pairIndex: number): void {
        _PolyhedralBoundedSolidSetNullEdgesConnector.currentConnectPairIndex = pairIndex;
    }

    private static allocateSyntheticPairIndex(): number {
        const pairIndex = _PolyhedralBoundedSolidSetNullEdgesConnector.nextSyntheticPairIndex;
        _PolyhedralBoundedSolidSetNullEdgesConnector.nextSyntheticPairIndex++;
        return pairIndex;
    }

    public static getSonfaPairIndex(face: _PolyhedralBoundedSolidFace | null): number {
        if (face === null || _PolyhedralBoundedSolidSetNullEdgesConnector.sonfaPairIndexByFaceId === null) {
            return -1;
        }
        const pairIndex = _PolyhedralBoundedSolidSetNullEdgesConnector.sonfaPairIndexByFaceId.get(face.id);
        return pairIndex !== undefined ? pairIndex : -1;
    }

    public static getSonfbPairIndex(face: _PolyhedralBoundedSolidFace | null): number {
        if (face === null || _PolyhedralBoundedSolidSetNullEdgesConnector.sonfbPairIndexByFaceId === null) {
            return -1;
        }
        const pairIndex = _PolyhedralBoundedSolidSetNullEdgesConnector.sonfbPairIndexByFaceId.get(face.id);
        return pairIndex !== undefined ? pairIndex : -1;
    }

    private static summarizeLooseEnds(
        endsA: ArrayList<_PolyhedralBoundedSolidHalfEdge>,
        endsB: ArrayList<_PolyhedralBoundedSolidHalfEdge>,
    ): string {
        const out = new StringBuilder();
        let i: number;

        out.append("pairs=").append(String(endsA.size())).append(" [");
        for (i = 0; i < endsA.size() && i < endsB.size(); i++) {
            if (i > 0) {
                out.append(" | ");
            }
            out.append(String(i))
                .append(":A=")
                .append(_PolyhedralBoundedSolidSetNullEdgesConnector.summarizeHalfEdge(endsA.get(i)))
                .append(",B=")
                .append(_PolyhedralBoundedSolidSetNullEdgesConnector.summarizeHalfEdge(endsB.get(i)));
        }
        out.append("]");
        return out.toString();
    }

    private static isSamePoint(
        first: _PolyhedralBoundedSolidHalfEdge | null,
        second: _PolyhedralBoundedSolidHalfEdge | null,
    ): boolean {
        if (first === null || second === null || first.startingVertex === null || second.startingVertex === null) {
            return false;
        }
        return PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
            first.startingVertex.position,
            second.startingVertex.position,
            _PolyhedralBoundedSolidOperator.numericContext,
        );
    }

    private static canCutCoincidentHalfEdge(he: _PolyhedralBoundedSolidHalfEdge | null): boolean {
        if (he === null) {
            return false;
        }
        const edge = he.parentEdge;
        const loop = he.parentLoop;
        if (edge === null || loop === null) {
            return false;
        }
        if (edge.rightHalf === null || edge.leftHalf === null) {
            return false;
        }
        if (edge.rightHalf.parentLoop !== edge.leftHalf.parentLoop) {
            return true;
        }
        return loop.halfEdgesList.size() > 2;
    }

    private static hasReusableCoincidentCutFace(he: _PolyhedralBoundedSolidHalfEdge | null): boolean {
        if (he === null || he.parentLoop === null) {
            return false;
        }
        const face = he.parentLoop.parentFace;
        return face !== null && face.boundariesList.size() > 1;
    }

    private static canCutCoincidentFinishFace(he: _PolyhedralBoundedSolidHalfEdge | null): boolean {
        if (he === null) {
            return false;
        }
        const edge = he.parentEdge;
        const loop = he.parentLoop;
        if (
            edge === null ||
            loop === null ||
            edge.rightHalf === null ||
            edge.leftHalf === null ||
            edge.rightHalf.parentLoop === null ||
            edge.leftHalf.parentLoop === null
        ) {
            return false;
        }
        // Classic case: both halves in the same loop and loop large enough for
        // an interior cut.
        if (edge.rightHalf.parentLoop === edge.leftHalf.parentLoop) {
            return loop.halfEdgesList.size() > 2;
        }
        // Cross-loop intersection edge whose halves still share the same face
        // (typical for null-edges produced by the intersect+classify pipeline
        // on tessellated curved surfaces). Allowing this case prevents
        // `closeLegacyCoincidentLooseEnds` from rejecting legitimate pairs and
        // leaving the integration ring incomplete.
        return edge.rightHalf.parentLoop.parentFace === edge.leftHalf.parentLoop.parentFace;
    }

    private static registerCoincidentCutFace(
        he: _PolyhedralBoundedSolidHalfEdge | null,
        target: ArrayList<_PolyhedralBoundedSolidFace>,
        _label: string,
    ): _PolyhedralBoundedSolidFace | null {
        if (he === null || he.parentLoop === null) {
            return null;
        }
        const face = he.parentLoop.parentFace;
        if (face === null || face.boundariesList.size() <= 1) {
            return null;
        }
        target.add(face);
        return face;
    }

    private static canFinalizeCoincidentLooseA(he: _PolyhedralBoundedSolidHalfEdge | null): boolean {
        return (
            _PolyhedralBoundedSolidSetNullEdgesConnector.canCutCoincidentFinishFace(he) ||
            _PolyhedralBoundedSolidSetNullEdgesConnector.hasReusableCoincidentCutFace(he)
        );
    }

    private static canFinalizeCoincidentLooseB(he: _PolyhedralBoundedSolidHalfEdge | null): boolean {
        return (
            _PolyhedralBoundedSolidSetNullEdgesConnector.canCutCoincidentFinishFace(he) ||
            _PolyhedralBoundedSolidSetNullEdgesConnector.hasReusableCoincidentCutFace(he)
        );
    }

    private static isPointLikeHalfEdge(he: _PolyhedralBoundedSolidHalfEdge | null): boolean {
        return (
            _PolyhedralBoundedSolidSetNullEdgesConnector.isLiveHalfEdge(he) &&
            he!.next() !== null &&
            he!.next()!.startingVertex !== null &&
            _PolyhedralBoundedSolidSetNullEdgesConnector.isSamePoint(he, he!.next())
        );
    }

    private static isDegenerateCoincidentLooseClosure(
        firstA: _PolyhedralBoundedSolidHalfEdge | null,
        secondA: _PolyhedralBoundedSolidHalfEdge | null,
        firstB: _PolyhedralBoundedSolidHalfEdge | null,
        secondB: _PolyhedralBoundedSolidHalfEdge | null,
    ): boolean {
        return (
            _PolyhedralBoundedSolidSetNullEdgesConnector.isPointLikeHalfEdge(firstA) &&
            _PolyhedralBoundedSolidSetNullEdgesConnector.isPointLikeHalfEdge(secondA) &&
            _PolyhedralBoundedSolidSetNullEdgesConnector.isPointLikeHalfEdge(firstB) &&
            _PolyhedralBoundedSolidSetNullEdgesConnector.isPointLikeHalfEdge(secondB)
        );
    }

    private finalizeCoincidentLooseA(he: _PolyhedralBoundedSolidHalfEdge): void {
        if (_PolyhedralBoundedSolidSetNullEdgesConnector.canCutCoincidentFinishFace(he)) {
            this.cutA(he);
        } else {
            _PolyhedralBoundedSolidSetNullEdgesConnector.registerCoincidentCutFace(he, this.sonfa!, "reuse-sonfa");
        }
    }

    private finalizeCoincidentLooseB(he: _PolyhedralBoundedSolidHalfEdge): void {
        if (_PolyhedralBoundedSolidSetNullEdgesConnector.canCutCoincidentFinishFace(he)) {
            this.cutB(he);
        } else {
            _PolyhedralBoundedSolidSetNullEdgesConnector.registerCoincidentCutFace(he, this.sonfb!, "reuse-sonfb");
        }
    }

    private static removeLoosePair(index: number): void {
        _PolyhedralBoundedSolidSetNullEdgesConnector.endsa!.remove(index);
        _PolyhedralBoundedSolidSetNullEdgesConnector.endsb!.remove(index);
    }

    public static getLastLooseACount(): number {
        return _PolyhedralBoundedSolidSetNullEdgesConnector.lastLooseACount;
    }

    public static getLastLooseBCount(): number {
        return _PolyhedralBoundedSolidSetNullEdgesConnector.lastLooseBCount;
    }

    public static getLastSonfaCount(): number {
        return _PolyhedralBoundedSolidSetNullEdgesConnector.lastSonfaCount;
    }

    public static getLastSonfbCount(): number {
        return _PolyhedralBoundedSolidSetNullEdgesConnector.lastSonfbCount;
    }

    public static getLastPairCount(): number {
        return _PolyhedralBoundedSolidSetNullEdgesConnector.lastPairCount;
    }

    private updateLastSnapshot(): void {
        _PolyhedralBoundedSolidSetNullEdgesConnector.lastLooseACount =
            _PolyhedralBoundedSolidSetNullEdgesConnector.endsa !== null
                ? _PolyhedralBoundedSolidSetNullEdgesConnector.endsa.size()
                : 0;
        _PolyhedralBoundedSolidSetNullEdgesConnector.lastLooseBCount =
            _PolyhedralBoundedSolidSetNullEdgesConnector.endsb !== null
                ? _PolyhedralBoundedSolidSetNullEdgesConnector.endsb.size()
                : 0;
        _PolyhedralBoundedSolidSetNullEdgesConnector.lastSonfaCount = this.sonfa !== null ? this.sonfa.size() : 0;
        _PolyhedralBoundedSolidSetNullEdgesConnector.lastSonfbCount = this.sonfb !== null ? this.sonfb.size() : 0;
    }

    private static isLiveHalfEdge(he: _PolyhedralBoundedSolidHalfEdge | null): boolean {
        return he !== null && he.parentEdge !== null && he.parentLoop !== null && he.parentLoop.parentFace !== null;
    }

    private static sharesParentFace(
        first: _PolyhedralBoundedSolidHalfEdge | null,
        second: _PolyhedralBoundedSolidHalfEdge | null,
    ): boolean {
        return (
            _PolyhedralBoundedSolidSetNullEdgesConnector.isLiveHalfEdge(first) &&
            _PolyhedralBoundedSolidSetNullEdgesConnector.isLiveHalfEdge(second) &&
            first!.parentLoop.parentFace === second!.parentLoop.parentFace
        );
    }

    private static isOppositeHalfEdgeSide(
        first: _PolyhedralBoundedSolidHalfEdge | null,
        second: _PolyhedralBoundedSolidHalfEdge | null,
    ): boolean {
        if (first === null || second === null || first.parentEdge === null || second.parentEdge === null) {
            return false;
        }

        return (
            (first === first.parentEdge.rightHalf && second === second.parentEdge.leftHalf) ||
            (first === first.parentEdge.leftHalf && second === second.parentEdge.rightHalf)
        );
    }

    private static findUniqueClassicRebindTargetFace(
        currentTargetFirst: _PolyhedralBoundedSolidHalfEdge | null,
        currentTargetSecond: _PolyhedralBoundedSolidHalfEdge | null,
        currentReferenceFirst: _PolyhedralBoundedSolidHalfEdge | null,
        currentReferenceSecond: _PolyhedralBoundedSolidHalfEdge | null,
        targetEnds: ArrayList<_PolyhedralBoundedSolidHalfEdge> | null,
        referenceEnds: ArrayList<_PolyhedralBoundedSolidHalfEdge> | null,
    ): _PolyhedralBoundedSolidFace | null {
        let candidateFace: _PolyhedralBoundedSolidFace | null = null;
        let i: number;
        let j: number;

        if (
            !_PolyhedralBoundedSolidSetNullEdgesConnector.isLiveHalfEdge(currentTargetFirst) ||
            !_PolyhedralBoundedSolidSetNullEdgesConnector.isLiveHalfEdge(currentTargetSecond) ||
            !_PolyhedralBoundedSolidSetNullEdgesConnector.isLiveHalfEdge(currentReferenceFirst) ||
            !_PolyhedralBoundedSolidSetNullEdgesConnector.isLiveHalfEdge(currentReferenceSecond) ||
            targetEnds === null ||
            referenceEnds === null
        ) {
            return null;
        }

        const pairCount = Math.min(targetEnds.size(), referenceEnds.size());
        for (i = 0; i < pairCount; i++) {
            const looseTargetFirst = targetEnds.get(i);
            const looseReferenceFirst = referenceEnds.get(i);
            if (
                !_PolyhedralBoundedSolidSetNullEdgesConnector.isLiveHalfEdge(looseTargetFirst) ||
                !_PolyhedralBoundedSolidSetNullEdgesConnector.isLiveHalfEdge(looseReferenceFirst) ||
                !_PolyhedralBoundedSolidSetNullEdgesConnector.isOppositeHalfEdgeSide(
                    currentTargetFirst,
                    looseTargetFirst,
                ) ||
                !_PolyhedralBoundedSolidOperator.neighbor(currentReferenceFirst!, looseReferenceFirst)
            ) {
                continue;
            }

            for (j = 0; j < pairCount; j++) {
                if (i === j) {
                    continue;
                }

                const looseTargetSecond = targetEnds.get(j);
                const looseReferenceSecond = referenceEnds.get(j);
                if (
                    !_PolyhedralBoundedSolidSetNullEdgesConnector.isLiveHalfEdge(looseTargetSecond) ||
                    !_PolyhedralBoundedSolidSetNullEdgesConnector.isLiveHalfEdge(looseReferenceSecond) ||
                    !_PolyhedralBoundedSolidSetNullEdgesConnector.isOppositeHalfEdgeSide(
                        currentTargetSecond,
                        looseTargetSecond,
                    ) ||
                    !_PolyhedralBoundedSolidOperator.neighbor(currentReferenceSecond!, looseReferenceSecond)
                ) {
                    continue;
                }

                const looseFace = looseTargetFirst.parentLoop.parentFace;
                if (
                    looseFace !== looseTargetSecond.parentLoop.parentFace ||
                    looseFace === currentTargetFirst!.parentLoop.parentFace
                ) {
                    continue;
                }

                if (candidateFace === null) {
                    candidateFace = looseFace;
                } else if (candidateFace !== looseFace) {
                    return null;
                }
            }
        }
        return candidateFace;
    }

    private static isStrictRebindTargetFace(
        targetFace: _PolyhedralBoundedSolidFace | null,
        currentTarget: _PolyhedralBoundedSolidHalfEdge | null,
    ): boolean {
        let point: Vector3Dd;

        if (
            targetFace === null ||
            !_PolyhedralBoundedSolidSetNullEdgesConnector.isLiveHalfEdge(currentTarget) ||
            currentTarget!.startingVertex === null ||
            targetFace.getContainingPlane() === null
        ) {
            return false;
        }

        const numericContext = PolyhedralBoundedSolidNumericPolicy.forFace(targetFace);
        point = currentTarget!.startingVertex.position;
        if (Math.abs(targetFace.getContainingPlane()!.pointDistance(point)) > numericContext.bigEpsilon()) {
            return false;
        }

        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.pointInFace(targetFace, point) === Geometry.INSIDE;
    }

    private static rebindClassicCurrentNullEdgeIfNeeded(
        currentTargetFirst: _PolyhedralBoundedSolidHalfEdge | null,
        currentTargetSecond: _PolyhedralBoundedSolidHalfEdge | null,
        currentReferenceFirst: _PolyhedralBoundedSolidHalfEdge | null,
        currentReferenceSecond: _PolyhedralBoundedSolidHalfEdge | null,
        targetEnds: ArrayList<_PolyhedralBoundedSolidHalfEdge> | null,
        referenceEnds: ArrayList<_PolyhedralBoundedSolidHalfEdge> | null,
        label: string,
    ): void {
        let sourceLoop: _PolyhedralBoundedSolidLoop;
        let solid: PolyhedralBoundedSolid;

        if (
            _PolyhedralBoundedSolidSetNullEdgesConnector.operation !== _PolyhedralBoundedSolidOperator.SUBTRACT ||
            !_PolyhedralBoundedSolidSetNullEdgesConnector.isLiveHalfEdge(currentTargetFirst) ||
            !_PolyhedralBoundedSolidSetNullEdgesConnector.isLiveHalfEdge(currentTargetSecond) ||
            currentTargetFirst!.parentLoop !== currentTargetSecond!.parentLoop
        ) {
            return;
        }

        sourceLoop = currentTargetFirst!.parentLoop;
        const sourceFace = sourceLoop.parentFace;
        if (sourceLoop.halfEdgesList.size() !== 2) {
            return;
        }

        const targetFace = _PolyhedralBoundedSolidSetNullEdgesConnector.findUniqueClassicRebindTargetFace(
            currentTargetFirst,
            currentTargetSecond,
            currentReferenceFirst,
            currentReferenceSecond,
            targetEnds,
            referenceEnds,
        );
        if (
            targetFace === null ||
            targetFace === sourceFace ||
            !_PolyhedralBoundedSolidSetNullEdgesConnector.isStrictRebindTargetFace(targetFace, currentTargetFirst)
        ) {
            return;
        }

        solid = currentTargetFirst!.parentLoop.parentFace.parentSolid;
        if (!PolyhedralBoundedSolidEulerOperators.lringmv(solid, sourceLoop, targetFace, false)) {
            _PolyhedralBoundedSolidSetNullEdgesConnector.tracePipelineSummary(
                "connect rebind" +
                    label +
                    " failed sourceFace=" +
                    sourceFace.id +
                    " targetFace=" +
                    targetFace.id +
                    " edge=" +
                    _PolyhedralBoundedSolidSetNullEdgesConnector.summarizeHalfEdge(currentTargetFirst),
            );
            return;
        }
        _PolyhedralBoundedSolidSetNullEdgesConnector.tracePipelineSummary(
            "connect rebind" +
                label +
                " sourceFace=" +
                sourceFace.id +
                " targetFace=" +
                targetFace.id +
                " edge=" +
                _PolyhedralBoundedSolidSetNullEdgesConnector.summarizeHalfEdge(currentTargetFirst),
        );
    }

    private static rebindClassicCurrentNullEdgesIfNeeded(
        currentARight: _PolyhedralBoundedSolidHalfEdge | null,
        currentALeft: _PolyhedralBoundedSolidHalfEdge | null,
        currentBLeft: _PolyhedralBoundedSolidHalfEdge | null,
        currentBRight: _PolyhedralBoundedSolidHalfEdge | null,
    ): void {
        _PolyhedralBoundedSolidSetNullEdgesConnector.rebindClassicCurrentNullEdgeIfNeeded(
            currentARight,
            currentALeft,
            currentBLeft,
            currentBRight,
            _PolyhedralBoundedSolidSetNullEdgesConnector.endsa,
            _PolyhedralBoundedSolidSetNullEdgesConnector.endsb,
            "A",
        );
        _PolyhedralBoundedSolidSetNullEdgesConnector.rebindClassicCurrentNullEdgeIfNeeded(
            currentBLeft,
            currentBRight,
            currentARight,
            currentALeft,
            _PolyhedralBoundedSolidSetNullEdgesConnector.endsb,
            _PolyhedralBoundedSolidSetNullEdgesConnector.endsa,
            "B",
        );
    }

    public connect(
        op: number,
        flags: number,
        inSonea: ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>,
        inSoneb: ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>,
    ): ConnectResult {
        _PolyhedralBoundedSolidSetNullEdgesConnector.operation = op;
        _PolyhedralBoundedSolidSetNullEdgesConnector.debugFlags = flags;
        this.sonea = inSonea;
        this.soneb = inSoneb;
        _PolyhedralBoundedSolidSetNullEdgesConnector.lastPairCount = Math.min(this.sonea.size(), this.soneb.size());
        _PolyhedralBoundedSolidSetNullEdgesConnector.lastLooseACount = 0;
        _PolyhedralBoundedSolidSetNullEdgesConnector.lastLooseBCount = 0;
        _PolyhedralBoundedSolidSetNullEdgesConnector.lastSonfaCount = 0;
        _PolyhedralBoundedSolidSetNullEdgesConnector.lastSonfbCount = 0;
        _PolyhedralBoundedSolidSetNullEdgesConnector.nextSyntheticPairIndex =
            _PolyhedralBoundedSolidSetNullEdgesConnector.lastPairCount;
        // [MANT1988] §15.7 Program 15.14: single Connect implementation.
        // The "flexibleEndpointChains" alternative path was removed in §6.1
        // of plan-csg-boolean-fix-stage2 because it duplicated setOpConnect
        // with extra heuristics that the book does not require.
        this.setOpConnect();
        return new ConnectResult(this.sonfa!, this.sonfb!);
    }

    private sortNullEdges(): void {
        _PolyhedralBoundedSolidSetNullEdgesConnector.curveOrientationApplied = false;
        _PolyhedralBoundedSolidSetNullEdgesConnector.curveNeighborPositions = null;
        if (
            _PolyhedralBoundedSolidSetNullEdgesConnector.testOnlyForcedConnectOrder !== null &&
            _PolyhedralBoundedSolidSetNullEdgesConnector.testOnlyForcedConnectOrder.length === this.sonea!.size() &&
            _PolyhedralBoundedSolidSetNullEdgesConnector.testOnlyForcedConnectOrder.length === this.soneb!.size()
        ) {
            this.applyOrderPermutation(_PolyhedralBoundedSolidSetNullEdgesConnector.testOnlyForcedConnectOrder);
            _PolyhedralBoundedSolidSetNullEdgesConnector.lastCurveReport =
                _PolyhedralBoundedSolidSetIntersectionCurveBuilder.build(
                    this.sonea!,
                    this.soneb!,
                    PolyhedralBoundedSolidNumericPolicy.defaultContext().unitVectorTolerance(),
                );
            _PolyhedralBoundedSolidSetNullEdgesConnector.tracePipelineSummary("connect TEST-ONLY forced order applied");
            return;
        }

        // Always group null-edges by topological ring before any further
        // processing. Without this, the connect loop pairs null-edges from
        // different intersection curves (e.g., the outer and inner boundary
        // circles of a spherical shell), producing non-coplanar faces.
        // Ring grouping is safe for the single-ring case (it is a no-op).
        this.groupNullEdgesByRing();

        if (_PolyhedralBoundedSolidSetNullEdgesConnector.isKeepInsertionOrderEnabled()) {
            _PolyhedralBoundedSolidSetNullEdgesConnector.tracePipelineSummary(
                "connect sort skipped; using insertion order",
            );
            return;
        }

        // Geometric sort within each ring, enabled only when keepInsertionOrder
        // is explicitly disabled via the system property.
        Collections.sort(this.sonea!);
        Collections.sort(this.soneb!);
    }

    private static dbgDumpNullEdges(label: string, sone: ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>): void {
        let k: number;
        for (k = 0; k < sone.size(); k++) {
            const ne = sone.get(k);
            const rh = ne.e.rightHalf!;
            const lh = ne.e.leftHalf!;
            console.log(
                "[DBG-ne] " +
                    label +
                    "[" +
                    k +
                    "] " +
                    "R{v=" +
                    rh.startingVertex.id +
                    " f=" +
                    rh.parentLoop.parentFace.id +
                    " p=(" +
                    rh.startingVertex.position.x().toFixed(4) +
                    "," +
                    rh.startingVertex.position.y().toFixed(4) +
                    "," +
                    rh.startingVertex.position.z().toFixed(4) +
                    ")}" +
                    " L{v=" +
                    lh.startingVertex.id +
                    " f=" +
                    lh.parentLoop.parentFace.id +
                    " p=(" +
                    lh.startingVertex.position.x().toFixed(4) +
                    "," +
                    lh.startingVertex.position.y().toFixed(4) +
                    "," +
                    lh.startingVertex.position.z().toFixed(4) +
                    ")}",
            );
        }
    }

    /**
    Reorders sonea/soneb in lockstep by the given permutation
    (`position -> originalIndex`).
    @param order permutation covering every index exactly once
    */
    private applyOrderPermutation(order: number[]): void {
        let i: number;

        const orderedA = new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>();
        const orderedB = new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>();
        for (i = 0; i < order.length; i++) {
            orderedA.add(this.sonea!.get(order[i]!));
            orderedB.add(this.soneb!.get(order[i]!));
        }
        this.sonea!.clear();
        this.sonea!.addAll(orderedA);
        this.soneb!.clear();
        this.soneb!.addAll(orderedB);
    }

    private static curveComponentFind(parent: number[], x: number): number {
        while (parent[x] !== x) {
            parent[x] = parent[parent[x]!]!;
            x = parent[x]!;
        }
        return x;
    }

    private static nullEdgeFaceIds(ne: _PolyhedralBoundedSolidSetOperatorNullEdge): number[] {
        return [ne.e.rightHalf!.parentLoop.parentFace.id, ne.e.leftHalf!.parentLoop.parentFace.id];
    }

    private static nullEdgesShareFace(a: number[], b: number[]): boolean {
        return a[0] === b[0] || a[0] === b[1] || a[1] === b[0] || a[1] === b[1];
    }

    /**
    Reorders `sonea`/`soneb` so that null-edge pairs belonging to the
    same intersection curve are contiguous and ordered for `scanjoin`, and
    distinct curves are separated.

    <p>Each intersection curve is recovered as a connected component over the
    paired null-edge indices: pairs `k`, `k'` are adjacent when their
    A-null-edges share a face <b>or</b> their B-null-edges share a face — pure
    topological adjacency along the curve as it crosses from face to face. The
    null-edges are zero-length struts whose two endpoints sit at the same point,
    so consecutive struts share a <em>face</em> (not a vertex id and not a
    position); face adjacency is therefore the correct connectivity.</p>

    <p>Within each component the classifier's emission order is preserved — that
    order is the curve-traversal order `scanjoin` (Program 15.13) requires.
    Components are emitted by ascending lowest member index (stable). Pairs stay
    index-aligned (`sonea[k]` with `soneb[k]`) because the same index
    permutation is applied to both lists.</p>

    <p>This replaces the former vertex-id ring partition plus spatial signature
    sort: the struts never share a vertex id, so that partition produced only
    singleton rings and the signature sort then scrambled the curve order.</p>
    */
    private groupNullEdgesByRing(): void {
        if (_PolyhedralBoundedSolidSetNullEdgesConnector.isPipelineSummaryTraceEnabled()) {
            _PolyhedralBoundedSolidSetNullEdgesConnector.dbgDumpNullEdges("A", this.sonea!);
            _PolyhedralBoundedSolidSetNullEdgesConnector.dbgDumpNullEdges("B", this.soneb!);
        }

        // Reconstruct the intersection curves for diagnosis (§5 of
        // doc/mythosPlan.md). The report is captured before any reordering
        // decision so it always describes the classifier's raw emission.
        _PolyhedralBoundedSolidSetNullEdgesConnector.lastCurveReport =
            _PolyhedralBoundedSolidSetIntersectionCurveBuilder.build(
                this.sonea!,
                this.soneb!,
                PolyhedralBoundedSolidNumericPolicy.defaultContext().unitVectorTolerance(),
            );
        _PolyhedralBoundedSolidSetNullEdgesConnector.tracePipelineSummary(
            "connect " + _PolyhedralBoundedSolidSetNullEdgesConnector.lastCurveReport.summarize(),
        );

        const n = Math.min(this.sonea!.size(), this.soneb!.size());
        if (n !== this.sonea!.size() || n !== this.soneb!.size() || n < 2) {
            return;
        }

        // mythosPlan §5.3 (Phase 2): when every null-edge pair lies on a
        // cleanly closed intersection curve, reorder pairs along each curve
        // AND orient every strut consistently with the traversal. Probe
        // evidence (mythosPlan §9, 2026-06-10): order alone is insufficient
        // — the legacy in-loop vertex-id normalization encodes classifier
        // emission order, so curve order with mismatched strut orientation
        // leaves loose ends at the seams. With both applied, scanjoin closes
        // every consecutive pair by construction (derivation in
        // _PolyhedralBoundedSolidSetIntersectionCurveBuilder.applyCurveOrientation).
        // On any anomaly (open chain, isolated node, pinch, odd/degenerate
        // face-pair group) fall through to the legacy ordering below.
        if (_PolyhedralBoundedSolidSetNullEdgesConnector.lastCurveReport.isCleanlyClosed()) {
            const curveOrder = _PolyhedralBoundedSolidSetIntersectionCurveBuilder.orderAndOrientAlongCurves(
                _PolyhedralBoundedSolidSetNullEdgesConnector.lastCurveReport.cycles,
                n,
                this.sonea!,
                this.soneb!,
            );
            if (curveOrder !== null) {
                this.applyOrderPermutation(curveOrder);
                _PolyhedralBoundedSolidSetNullEdgesConnector.curveOrientationApplied = true;
                _PolyhedralBoundedSolidSetNullEdgesConnector.curveNeighborPositions =
                    _PolyhedralBoundedSolidSetIntersectionCurveBuilder.lastTraversalNeighborPositions;
                _PolyhedralBoundedSolidSetNullEdgesConnector.tracePipelineSummary(
                    "connect curve-order applied: cycles=" +
                        _PolyhedralBoundedSolidSetNullEdgesConnector.lastCurveReport.cycles.size(),
                );
                return;
            }
        }

        // When all null-edge rings are singletons (every pair is a zero-length
        // strut), there is no ring structure to separate, and a spatial
        // signature sort only scrambles the classifier's already-valid emission
        // order. Preserve insertion order in that case (fixes all 20 star motifs).
        // For cases where at least one proper ring exists (e.g. the shell-cylinder
        // intersection), fall through to the connected-component sort.
        const faceIdsA = new Array<number[]>(n);
        const faceIdsB = new Array<number[]>(n);
        let k: number;
        for (k = 0; k < n; k++) {
            faceIdsA[k] = _PolyhedralBoundedSolidSetNullEdgesConnector.nullEdgeFaceIds(this.sonea!.get(k));
            faceIdsB[k] = _PolyhedralBoundedSolidSetNullEdgesConnector.nullEdgeFaceIds(this.soneb!.get(k));
        }

        const parent = new Array<number>(n);
        for (k = 0; k < n; k++) {
            parent[k] = k;
        }
        let i: number;
        let j: number;
        for (i = 0; i < n; i++) {
            for (j = i + 1; j < n; j++) {
                if (
                    _PolyhedralBoundedSolidSetNullEdgesConnector.nullEdgesShareFace(faceIdsA[i]!, faceIdsA[j]!) ||
                    _PolyhedralBoundedSolidSetNullEdgesConnector.nullEdgesShareFace(faceIdsB[i]!, faceIdsB[j]!)
                ) {
                    parent[_PolyhedralBoundedSolidSetNullEdgesConnector.curveComponentFind(parent, i)] =
                        _PolyhedralBoundedSolidSetNullEdgesConnector.curveComponentFind(parent, j);
                }
            }
        }

        // Count distinct curve components.
        const roots = new HashSet<number>();
        for (k = 0; k < n; k++) {
            roots.add(_PolyhedralBoundedSolidSetNullEdgesConnector.curveComponentFind(parent, k));
        }
        const componentCount = roots.size();

        // Decide whether to reorder.
        // Two null-edges that share a vertex ID are topologically adjacent on
        // the same intersection curve (the vertex is the meeting point of two
        // consecutive null-edges). When ALL null-edges are isolated (no shared
        // vertex IDs — they are zero-length struts each at a distinct vertex),
        // the classifier's emission order is already the best available curve
        // order and reordering would only scramble it (this is the case for all
        // star-prism motifs AND for most moon motifs). Only reorder when at
        // least one shared vertex exists, which signals a multi-curve
        // intersection where distinct curves must be kept contiguous (e.g.
        // the shell-cylinder construction).
        //
        // NOTE: this check deliberately uses vertex IDs (not positions) because
        // after weldCoincidentVertices the coincident pairs are already merged;
        // two null-edges that are truly adjacent on the curve share the same
        // vertex object, not just the same position.
        const vidsA = new HashSet<number>();
        let hasSharedA = false;
        let ki: number;
        for (ki = 0; ki < n && !hasSharedA; ki++) {
            const v1 = this.sonea!.get(ki).e.rightHalf!.startingVertex.id;
            const v2 = this.sonea!.get(ki).e.leftHalf!.startingVertex.id;
            if (!vidsA.add(v1) || (v1 !== v2 && !vidsA.add(v2))) {
                hasSharedA = true;
            }
        }
        let hasSharedB = false;
        if (!hasSharedA) {
            const vidsB = new HashSet<number>();
            for (ki = 0; ki < n && !hasSharedB; ki++) {
                const v1 = this.soneb!.get(ki).e.rightHalf!.startingVertex.id;
                const v2 = this.soneb!.get(ki).e.leftHalf!.startingVertex.id;
                if (!vidsB.add(v1) || (v1 !== v2 && !vidsB.add(v2))) {
                    hasSharedB = true;
                }
            }
        }
        if (!hasSharedA && !hasSharedB) {
            _PolyhedralBoundedSolidSetNullEdgesConnector.tracePipelineSummary(
                "connect ring-group: all singletons; preserving insertion order",
            );
            return;
        }

        // Multiple pairs share face-adjacency → reconstruct curve order.
        const components = new LinkedHashMap<number, ArrayList<number>>();
        for (k = 0; k < n; k++) {
            const root = _PolyhedralBoundedSolidSetNullEdgesConnector.curveComponentFind(parent, k);
            let bucket = components.get(root);
            if (bucket === undefined) {
                bucket = new ArrayList<number>();
                components.put(root, bucket);
            }
            bucket.add(k);
        }

        const orderedA = new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>();
        const orderedB = new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>();
        for (const component of components.values()) {
            for (let ci = 0; ci < component.size(); ci++) {
                const idx = component.get(ci);
                orderedA.add(this.sonea!.get(idx));
                orderedB.add(this.soneb!.get(idx));
            }
        }
        this.sonea!.clear();
        this.sonea!.addAll(orderedA);
        this.soneb!.clear();
        this.soneb!.addAll(orderedB);

        _PolyhedralBoundedSolidSetNullEdgesConnector.tracePipelineSummary(
            "connect curve-components: count=" + componentCount,
        );
    }

    /**
    Implements sgetnextnulledge per [MANT1988] §15.7, Program 15.14.

    <p>Advances the internal cursor over `sonea`/`soneb` and
    fills `out` with the next (nea, neb) pair, mirroring the
    out-param style of the C original. Returns `true` while pairs
    remain; `false` signals the `while` loop body to exit.</p>

    <p>The cursor is reset to `0` in `setOpConnect()` before
    the loop starts. Order is the parametric ordering produced by
    `sortNullEdges()` (§4.3 of plan-csg-boolean-fix-stage2).</p>
    */
    private sgetnextnulledge(out: NullEdgePair): boolean {
        if (this.nextNullEdgeIndex >= this.sonea!.size() || this.nextNullEdgeIndex >= this.soneb!.size()) {
            return false;
        }
        out.nea = this.sonea!.get(this.nextNullEdgeIndex);
        out.neb = this.soneb!.get(this.nextNullEdgeIndex);
        out.pairIndex = this.nextNullEdgeIndex;
        this.nextNullEdgeIndex++;
        return true;
    }

    /**
    Implements scanjoin per [MANT1988] §15.7, Program 15.13.

    <p>Returns `{ha, hb}` (matched halves from `endsa[i]`/`endsb[i]`)
    only when there is a single index `i` such that <b>both</b>
    `hea` is a neighbor of `endsa.get(i)` AND `heb` is a
    neighbor of `endsb.get(i)` — i.e., both null-edges can close to
    the <em>same</em> previously-loose pair. The matched pair is removed
    from `endsa`/`endsb` and returned.</p>

    <p>If no such index exists, `hea` and `heb` are appended to
    the loose lists (becoming candidates for future pairings) and
    `null` is returned, signalling that the caller must not perform
    `join`/`cut` for this pair.</p>
    */
    private scanjoin(
        hea: _PolyhedralBoundedSolidHalfEdge,
        heb: _PolyhedralBoundedSolidHalfEdge,
    ): _PolyhedralBoundedSolidHalfEdge[] | null {
        let i: number;
        let condition1: boolean;
        let condition2: boolean;
        const endsa = _PolyhedralBoundedSolidSetNullEdgesConnector.endsa!;
        const endsb = _PolyhedralBoundedSolidSetNullEdgesConnector.endsb!;
        const debugFlags = _PolyhedralBoundedSolidSetNullEdgesConnector.debugFlags;

        for (i = 0; i < endsa.size(); i++) {
            condition1 = _PolyhedralBoundedSolidOperator.neighbor(hea, endsa.get(i));
            condition2 = _PolyhedralBoundedSolidOperator.neighbor(heb, endsb.get(i));

            if ((debugFlags & _PolyhedralBoundedSolidSetNullEdgesConnector.DEBUG_05_CONNECT) !== 0x00) {
                console.log(
                    "    . Testing for neighborhood A[" +
                        hea.startingVertex.id +
                        "/" +
                        hea.next()!.startingVertex.id +
                        "] vs. A[" +
                        endsa.get(i).startingVertex.id +
                        "/" +
                        endsa.get(i).next()!.startingVertex.id +
                        "]: " +
                        (condition1 ? "true" : "false") +
                        " ParentFaces: " +
                        hea.parentLoop.parentFace.id +
                        " / " +
                        endsa.get(i).parentLoop.parentFace.id,
                );
                console.log(
                    "    . Testing for neighborhood B[" +
                        heb.startingVertex.id +
                        "/" +
                        heb.next()!.startingVertex.id +
                        "] vs. B[" +
                        endsb.get(i).startingVertex.id +
                        "/" +
                        endsb.get(i).next()!.startingVertex.id +
                        "]: " +
                        (condition2 ? "true" : "false") +
                        " ParentFaces: " +
                        heb.parentLoop.parentFace.id +
                        " / " +
                        endsb.get(i).parentLoop.parentFace.id,
                );
            }

            if (condition1 && condition2) {
                const ret = new Array<_PolyhedralBoundedSolidHalfEdge>(2);
                ret[0] = endsa.get(i);
                ret[1] = endsb.get(i);
                endsa.remove(i);
                endsb.remove(i);
                if (i < _PolyhedralBoundedSolidSetNullEdgesConnector.endsPairIndex!.size()) {
                    _PolyhedralBoundedSolidSetNullEdgesConnector.endsPairIndex!.remove(i);
                }
                return ret;
            }
        }

        // mythosPlan Phase 3 (curve-ordered path only): rescue a unique
        // near-miss before declaring this pair loose. When a face is
        // crossed by several chords of the intersection curve, an earlier
        // division can re-parent a pending strut ring away from the face
        // where its junction partner waits; the junction then fails the
        // neighbor face-equality forever (the EMPTY/BLACK_FACES cusp
        // moons). If exactly one loose entry matches on one solid and
        // differs ONLY by parent face on the other — with a two-half-edge
        // strut ring on the mismatched side — re-parenting that ring to
        // the partner's face restores the junction the curve order
        // guarantees. Topological information only; no geometry.
        if (_PolyhedralBoundedSolidSetNullEdgesConnector.curveOrientationApplied) {
            const rescued = this.rescueRingFaceNearMiss(hea, heb);
            if (rescued !== null) {
                return rescued;
            }
        }

        endsa.add(hea);
        endsb.add(heb);
        _PolyhedralBoundedSolidSetNullEdgesConnector.endsPairIndex!.add(
            _PolyhedralBoundedSolidSetNullEdgesConnector.currentConnectPairIndex,
        );
        return null;
    }

    private static rolesOpposite(
        h1: _PolyhedralBoundedSolidHalfEdge | null,
        h2: _PolyhedralBoundedSolidHalfEdge | null,
    ): boolean {
        if (h1 === null || h2 === null || h1.parentEdge === null || h2.parentEdge === null) {
            return false;
        }
        return (
            (h1 === h1.parentEdge.rightHalf && h2 === h2.parentEdge.leftHalf) ||
            (h1 === h1.parentEdge.leftHalf && h2 === h2.parentEdge.rightHalf)
        );
    }

    /**
    True when the half-edge dangles in a pending two-half-edge strut ring
    (an inner loop holding only the null edge, as created by the vertex/face
    classifier's makeRing) — the only configuration this rescue may
    re-parent.
    @param he half-edge to inspect
    @return true for a pending strut ring's half-edge
    */
    private static isPendingStrutRing(he: _PolyhedralBoundedSolidHalfEdge | null): boolean {
        if (
            he === null ||
            he.parentLoop === null ||
            he.parentLoop.halfEdgesList === null ||
            he.parentLoop.parentFace === null ||
            he.parentLoop.parentFace.boundariesList.size() === 0
        ) {
            return false;
        }
        if (he.parentLoop.halfEdgesList.size() !== 2) {
            return false;
        }
        return he.parentLoop !== he.parentLoop.parentFace.boundariesList.get(0);
    }

    /**
    Attempts the near-miss rescue described at the scanjoin call site:
    finds the unique loose index that matches on one solid and fails only
    the face equality on the other, with a pending strut ring on the
    mismatched side; re-parents that ring and completes the match.
    @param hea query half on solid A
    @param heb query half on solid B
    @return the matched loose pair after the rescue, or null
    */
    private rescueRingFaceNearMiss(
        hea: _PolyhedralBoundedSolidHalfEdge,
        heb: _PolyhedralBoundedSolidHalfEdge,
    ): _PolyhedralBoundedSolidHalfEdge[] | null {
        let candidate = -1;
        let mismatchOnA = false;
        let i: number;
        const endsa = _PolyhedralBoundedSolidSetNullEdgesConnector.endsa!;
        const endsb = _PolyhedralBoundedSolidSetNullEdgesConnector.endsb!;
        const curveNeighborPositions = _PolyhedralBoundedSolidSetNullEdgesConnector.curveNeighborPositions;
        const currentConnectPairIndex = _PolyhedralBoundedSolidSetNullEdgesConnector.currentConnectPairIndex;

        for (i = 0; i < endsa.size(); i++) {
            // Only true curve neighbors of the current pair may be
            // rescued: the loose entry must have been pushed by one of the
            // two cycle-adjacent pairs. Without this guard the rescue can
            // stitch a cycle seed to a leftover of another curve
            // (regressed MANT1988_15_2_HOLED, mythosPlan §9).
            if (
                curveNeighborPositions === null ||
                currentConnectPairIndex < 0 ||
                currentConnectPairIndex >= curveNeighborPositions.length ||
                curveNeighborPositions[currentConnectPairIndex] === null ||
                curveNeighborPositions[currentConnectPairIndex] === undefined ||
                i >= _PolyhedralBoundedSolidSetNullEdgesConnector.endsPairIndex!.size()
            ) {
                continue;
            }
            const pusherPosition = _PolyhedralBoundedSolidSetNullEdgesConnector.endsPairIndex!.get(i);
            if (
                pusherPosition !== curveNeighborPositions[currentConnectPairIndex]![0] &&
                pusherPosition !== curveNeighborPositions[currentConnectPairIndex]![1]
            ) {
                continue;
            }
            const aOk = _PolyhedralBoundedSolidOperator.neighbor(hea, endsa.get(i));
            const bOk = _PolyhedralBoundedSolidOperator.neighbor(heb, endsb.get(i));
            const nearMissA =
                !aOk &&
                bOk &&
                _PolyhedralBoundedSolidSetNullEdgesConnector.rolesOpposite(hea, endsa.get(i)) &&
                (_PolyhedralBoundedSolidSetNullEdgesConnector.isPendingStrutRing(hea) ||
                    _PolyhedralBoundedSolidSetNullEdgesConnector.isPendingStrutRing(endsa.get(i)));
            const nearMissB =
                aOk &&
                !bOk &&
                _PolyhedralBoundedSolidSetNullEdgesConnector.rolesOpposite(heb, endsb.get(i)) &&
                (_PolyhedralBoundedSolidSetNullEdgesConnector.isPendingStrutRing(heb) ||
                    _PolyhedralBoundedSolidSetNullEdgesConnector.isPendingStrutRing(endsb.get(i)));
            if (nearMissA || nearMissB) {
                if (candidate >= 0) {
                    _PolyhedralBoundedSolidSetNullEdgesConnector.tracePipelineSummary(
                        "connect ring-rescue ambiguous; skipped",
                    );
                    return null;
                }
                candidate = i;
                mismatchOnA = nearMissA;
            }
        }
        if (candidate < 0) {
            return null;
        }

        const query = mismatchOnA ? hea : heb;
        const stored = mismatchOnA ? endsa.get(candidate) : endsb.get(candidate);
        let ringSide: _PolyhedralBoundedSolidHalfEdge;
        let anchorSide: _PolyhedralBoundedSolidHalfEdge;
        if (_PolyhedralBoundedSolidSetNullEdgesConnector.isPendingStrutRing(query)) {
            ringSide = query;
            anchorSide = stored;
        } else {
            ringSide = stored;
            anchorSide = query;
        }
        const targetFace = anchorSide.parentLoop.parentFace;
        if (targetFace === null || targetFace.parentSolid === null) {
            return null;
        }
        if (
            !PolyhedralBoundedSolidEulerOperators.lringmv(
                targetFace.parentSolid,
                ringSide.parentLoop,
                targetFace,
                false,
            )
        ) {
            return null;
        }
        if (
            !_PolyhedralBoundedSolidOperator.neighbor(hea, endsa.get(candidate)) ||
            !_PolyhedralBoundedSolidOperator.neighbor(heb, endsb.get(candidate))
        ) {
            _PolyhedralBoundedSolidSetNullEdgesConnector.tracePipelineSummary(
                "connect ring-rescue re-parent did not complete the match",
            );
            return null;
        }
        _PolyhedralBoundedSolidSetNullEdgesConnector.tracePipelineSummary(
            "connect ring-rescue applied: ring v=" +
                (ringSide.startingVertex === null ? "?" : Integer.toString(ringSide.startingVertex.id)) +
                " -> face " +
                targetFace.id,
        );
        const ret = new Array<_PolyhedralBoundedSolidHalfEdge>(2);
        ret[0] = endsa.get(candidate);
        ret[1] = endsb.get(candidate);
        endsa.remove(candidate);
        endsb.remove(candidate);
        if (candidate < _PolyhedralBoundedSolidSetNullEdgesConnector.endsPairIndex!.size()) {
            _PolyhedralBoundedSolidSetNullEdgesConnector.endsPairIndex!.remove(candidate);
        }
        return ret;
    }

    private static isLooseA(he: _PolyhedralBoundedSolidHalfEdge | null): boolean {
        let i: number;
        const endsa = _PolyhedralBoundedSolidSetNullEdgesConnector.endsa!;

        for (i = 0; i < endsa.size(); i++) {
            if (he === endsa.get(i)) return true;
        }

        return false;
    }

    private static isLooseB(he: _PolyhedralBoundedSolidHalfEdge | null): boolean {
        let i: number;
        const endsb = _PolyhedralBoundedSolidSetNullEdgesConnector.endsb!;

        for (i = 0; i < endsb.size(); i++) {
            if (he === endsb.get(i)) return true;
        }

        return false;
    }

    private cutA(he: _PolyhedralBoundedSolidHalfEdge): _PolyhedralBoundedSolidFace | null {
        let s: PolyhedralBoundedSolid;
        let addedFace: _PolyhedralBoundedSolidFace | null = null;
        const debugFlags = _PolyhedralBoundedSolidSetNullEdgesConnector.debugFlags;
        const withDebug =
            (debugFlags & _PolyhedralBoundedSolidSetNullEdgesConnector.DEBUG_99_SHOWOPERATIONS) !== 0x0 &&
            (debugFlags & _PolyhedralBoundedSolidSetNullEdgesConnector.DEBUG_05_CONNECT) !== 0x00;

        if (withDebug) {
            console.log("       -> CUTA:");
            console.log("          . He: " + he);
        }

        s = he.parentLoop.parentFace.parentSolid;
        if (he.parentEdge!.rightHalf!.parentLoop === he.parentEdge!.leftHalf!.parentLoop) {
            addedFace = he.parentLoop.parentFace;
            PolyhedralBoundedSolidEulerOperators.lkemr(s, he.parentEdge!.rightHalf!, he.parentEdge!.leftHalf!);
            if (addedFace.boundariesList.size() >= 2) {
                this.sonfa!.add(addedFace);
                if (_PolyhedralBoundedSolidSetNullEdgesConnector.sonfaPairIndexByFaceId !== null) {
                    _PolyhedralBoundedSolidSetNullEdgesConnector.sonfaPairIndexByFaceId.put(
                        addedFace.id,
                        _PolyhedralBoundedSolidSetNullEdgesConnector.currentConnectPairIndex,
                    );
                }
            } else {
                addedFace = null;
            }
        } else {
            PolyhedralBoundedSolidEulerOperators.lkef(s, he.parentEdge!.rightHalf!, he.parentEdge!.leftHalf!);
        }
        return addedFace;
    }

    private cutB(he: _PolyhedralBoundedSolidHalfEdge): _PolyhedralBoundedSolidFace | null {
        let s: PolyhedralBoundedSolid;
        let addedFace: _PolyhedralBoundedSolidFace | null = null;
        const debugFlags = _PolyhedralBoundedSolidSetNullEdgesConnector.debugFlags;
        const withDebug =
            (debugFlags & _PolyhedralBoundedSolidSetNullEdgesConnector.DEBUG_99_SHOWOPERATIONS) !== 0x0 &&
            (debugFlags & _PolyhedralBoundedSolidSetNullEdgesConnector.DEBUG_05_CONNECT) !== 0x00;

        if (withDebug) {
            console.log("       -> CUTB:");
            console.log("          . He: " + he);
        }

        s = he.parentLoop.parentFace.parentSolid;
        if (he.parentEdge!.rightHalf!.parentLoop === he.parentEdge!.leftHalf!.parentLoop) {
            addedFace = he.parentLoop.parentFace;
            PolyhedralBoundedSolidEulerOperators.lkemr(s, he.parentEdge!.rightHalf!, he.parentEdge!.leftHalf!);
            if (addedFace.boundariesList.size() >= 2) {
                this.sonfb!.add(addedFace);
                if (_PolyhedralBoundedSolidSetNullEdgesConnector.sonfbPairIndexByFaceId !== null) {
                    _PolyhedralBoundedSolidSetNullEdgesConnector.sonfbPairIndexByFaceId.put(
                        addedFace.id,
                        _PolyhedralBoundedSolidSetNullEdgesConnector.currentConnectPairIndex,
                    );
                }
            } else {
                addedFace = null;
            }
        } else {
            PolyhedralBoundedSolidEulerOperators.lkef(s, he.parentEdge!.rightHalf!, he.parentEdge!.leftHalf!);
        }
        return addedFace;
    }

    private static removeLastCutFaceIfSame(
        faces: ArrayList<_PolyhedralBoundedSolidFace> | null,
        face: _PolyhedralBoundedSolidFace | null,
    ): void {
        let last: number;

        if (faces === null || face === null || faces.isEmpty()) {
            return;
        }
        last = faces.size() - 1;
        if (faces.get(last) === face) {
            faces.remove(last);
        }
    }

    /**
    Neighbor null edges connector for the set operations algorithm
    (big phase 3).
    Following section [MANT1988].15.7. and program [MANT1988].15.14.
    */
    private setOpConnect(): void {
        const debugFlags = _PolyhedralBoundedSolidSetNullEdgesConnector.debugFlags;

        if ((debugFlags & _PolyhedralBoundedSolidSetNullEdgesConnector.DEBUG_01_STRUCTURE) !== 0x00) {
            console.log(
                "- 3. ------------------------------------------------------------------------------------------------------------------------------------------------------",
            );
        }

        this.sortNullEdges();

        let i: number;

        if ((debugFlags & _PolyhedralBoundedSolidSetNullEdgesConnector.DEBUG_05_CONNECT) !== 0x00) {
            console.log("SORTED SET OF " + this.sonea!.size() + " NULL EDGES PAIRS TO BE CONNECTED");
        }
        _PolyhedralBoundedSolidSetNullEdgesConnector.tracePipelineSummary(
            "connect start pairsA=" + this.sonea!.size() + " pairsB=" + this.soneb!.size(),
        );
        if (_PolyhedralBoundedSolidSetNullEdgesConnector.isPipelineSummaryTraceEnabled()) {
            let sameLoopA = 0;
            let diffLoopA = 0;
            for (let k = 0; k < this.sonea!.size(); k++) {
                const ne = this.sonea!.get(k);
                if (
                    ne.e.rightHalf !== null &&
                    ne.e.leftHalf !== null &&
                    ne.e.rightHalf.parentLoop === ne.e.leftHalf.parentLoop
                ) {
                    sameLoopA++;
                } else {
                    diffLoopA++;
                }
            }
            let sameLoopB = 0;
            let diffLoopB = 0;
            for (let k = 0; k < this.soneb!.size(); k++) {
                const ne = this.soneb!.get(k);
                if (
                    ne.e.rightHalf !== null &&
                    ne.e.leftHalf !== null &&
                    ne.e.rightHalf.parentLoop === ne.e.leftHalf.parentLoop
                ) {
                    sameLoopB++;
                } else {
                    diffLoopB++;
                }
            }
            _PolyhedralBoundedSolidSetNullEdgesConnector.tracePipelineSummary(
                "connect null-edge-loops A:sameLoop=" +
                    sameLoopA +
                    " diffLoop=" +
                    diffLoopA +
                    " B:sameLoop=" +
                    sameLoopB +
                    " diffLoop=" +
                    diffLoopB,
            );
        }

        let nextedgea: _PolyhedralBoundedSolidEdge;
        let nextedgeb: _PolyhedralBoundedSolidEdge;
        let h1a: _PolyhedralBoundedSolidHalfEdge | null = null;
        let h2a: _PolyhedralBoundedSolidHalfEdge | null = null;
        let h1b: _PolyhedralBoundedSolidHalfEdge | null = null;
        let h2b: _PolyhedralBoundedSolidHalfEdge | null = null;
        let r: _PolyhedralBoundedSolidHalfEdge[] | null;
        const allowRingMoveOnAJoin =
            _PolyhedralBoundedSolidSetNullEdgesConnector.operation === _PolyhedralBoundedSolidOperator.INTERSECTION;
        const withDebug =
            (debugFlags & _PolyhedralBoundedSolidSetNullEdgesConnector.DEBUG_99_SHOWOPERATIONS) !== 0x0 &&
            (debugFlags & _PolyhedralBoundedSolidSetNullEdgesConnector.DEBUG_05_CONNECT) !== 0x00;

        _PolyhedralBoundedSolidSetNullEdgesConnector.endsa = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();
        _PolyhedralBoundedSolidSetNullEdgesConnector.endsb = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();
        _PolyhedralBoundedSolidSetNullEdgesConnector.endsPairIndex = new ArrayList<number>();

        this.sonfa = new ArrayList<_PolyhedralBoundedSolidFace>();
        this.sonfb = new ArrayList<_PolyhedralBoundedSolidFace>();
        _PolyhedralBoundedSolidSetNullEdgesConnector.sonfaPairIndexByFaceId = new HashMap<number, number>();
        _PolyhedralBoundedSolidSetNullEdgesConnector.sonfbPairIndexByFaceId = new HashMap<number, number>();
        _PolyhedralBoundedSolidSetNullEdgesConnector.setCurrentConnectContext(-1);
        let j: number;

        if (this.sonea!.size() !== this.soneb!.size()) {
            PolyhedralBoundedSolidStatistics.recordOperationFailureCase();
            console.log("**** Not paired null edges!");
        }

        // [MANT1988] Program 15.14:
        //   while (sgetnextnulledge(&nea, &neb)) { ... }
        // The cursor is set to 0 here so each call to setOpConnect()
        // restarts iteration from the first pair of the sorted set.
        this.nextNullEdgeIndex = 0;
        const pair = new NullEdgePair();
        while (this.sgetnextnulledge(pair)) {
            const nea = pair.nea!;
            const neb = pair.neb!;
            i = pair.pairIndex;
            let ha: _PolyhedralBoundedSolidHalfEdge;
            let ham: _PolyhedralBoundedSolidHalfEdge;
            let hb: _PolyhedralBoundedSolidHalfEdge;
            let hbm: _PolyhedralBoundedSolidHalfEdge;
            let tmp: _PolyhedralBoundedSolidHalfEdge | null;

            ha = nea.e.rightHalf!;
            ham = nea.e.leftHalf!;
            hb = neb.e.rightHalf!;
            hbm = neb.e.leftHalf!;
            _PolyhedralBoundedSolidSetNullEdgesConnector.tracePipelineSummary(
                "connect pair[" +
                    i +
                    "] A{" +
                    _PolyhedralBoundedSolidSetNullEdgesConnector.summarizeNullEdge(nea) +
                    "} B{" +
                    _PolyhedralBoundedSolidSetNullEdgesConnector.summarizeNullEdge(neb) +
                    "}",
            );

            if ((debugFlags & _PolyhedralBoundedSolidSetNullEdgesConnector.DEBUG_05_CONNECT) !== 0x00) {
                const endsa = _PolyhedralBoundedSolidSetNullEdgesConnector.endsa!;
                const endsb = _PolyhedralBoundedSolidSetNullEdgesConnector.endsb!;
                console.log(
                    "  - " +
                        (endsa.size() + endsb.size()) +
                        " = " +
                        endsa.size() +
                        "+" +
                        endsb.size() +
                        " loose ends before processing pair [" +
                        i +
                        "]:",
                );

                for (j = 0; j < endsa.size(); j++) {
                    const hat = endsa.get(j);
                    const hbt = endsb.get(j);
                    console.log(
                        "    . [" +
                            j +
                            "]: He(A): " +
                            hat.startingVertex.id +
                            "/" +
                            hat.next()!.startingVertex.id +
                            " | He(B): " +
                            hbt.startingVertex.id +
                            "/" +
                            hbt.next()!.startingVertex.id,
                    );
                }

                if (ha.startingVertex.id > ham.startingVertex.id) {
                    console.log("********* FORCING ORDER!");
                }

                console.log(
                    "  - Processing pair [" +
                        i +
                        "]: " +
                        "He(A1): " +
                        ha.startingVertex.id +
                        "/" +
                        ha.next()!.startingVertex.id +
                        " He(A2): " +
                        ham.startingVertex.id +
                        "/" +
                        ham.next()!.startingVertex.id +
                        " He(B1): " +
                        hb.startingVertex.id +
                        "/" +
                        hb.next()!.startingVertex.id +
                        " He(B2): " +
                        hbm.startingVertex.id +
                        "/" +
                        hbm.next()!.startingVertex.id,
                );
            }

            nextedgea = nea.e;
            nextedgeb = neb.e;
            h1a = null;
            h2a = null;
            h1b = null;
            h2b = null;
            // Legacy strut orientation: vertex-id order encodes classifier
            // emission order. When sortNullEdges already oriented the struts
            // along the intersection curves (mythosPlan §5.3), that
            // orientation must be respected — re-normalizing by id here
            // would undo it and reintroduce seam mismatches in scanjoin.
            if (!_PolyhedralBoundedSolidSetNullEdgesConnector.curveOrientationApplied) {
                if (ha.startingVertex.id > ham.startingVertex.id) {
                    tmp = nextedgea.rightHalf;
                    nextedgea.rightHalf = nextedgea.leftHalf;
                    nextedgea.leftHalf = tmp;
                }
                if (hb.startingVertex.id > hbm.startingVertex.id) {
                    tmp = nextedgeb.rightHalf;
                    nextedgeb.rightHalf = nextedgeb.leftHalf;
                    nextedgeb.leftHalf = tmp;
                }
            }

            _PolyhedralBoundedSolidSetNullEdgesConnector.rebindClassicCurrentNullEdgesIfNeeded(
                nextedgea.rightHalf,
                nextedgea.leftHalf,
                nextedgeb.leftHalf,
                nextedgeb.rightHalf,
            );

            _PolyhedralBoundedSolidSetNullEdgesConnector.setCurrentConnectContext(i);
            r = this.scanjoin(nextedgea.rightHalf!, nextedgeb.leftHalf!);
            if (r !== null) {
                h1a = r[0]!;
                h2b = r[1]!;
                _PolyhedralBoundedSolidOperator.join(h1a, nextedgea.rightHalf!, withDebug, allowRingMoveOnAJoin);
                if (!_PolyhedralBoundedSolidSetNullEdgesConnector.isLooseA(h1a.mirrorHalfEdge())) {
                    this.cutA(h1a);
                }
                _PolyhedralBoundedSolidOperator.join(h2b, nextedgeb.leftHalf!, withDebug);
                if (!_PolyhedralBoundedSolidSetNullEdgesConnector.isLooseB(h2b.mirrorHalfEdge())) {
                    this.cutB(h2b);
                }
            }

            _PolyhedralBoundedSolidSetNullEdgesConnector.setCurrentConnectContext(i);
            r = this.scanjoin(nextedgea.leftHalf!, nextedgeb.rightHalf!);
            if (r !== null) {
                h2a = r[0]!;
                h1b = r[1]!;
                _PolyhedralBoundedSolidOperator.join(h2a, nextedgea.leftHalf!, withDebug, allowRingMoveOnAJoin);
                if (!_PolyhedralBoundedSolidSetNullEdgesConnector.isLooseA(h2a.mirrorHalfEdge())) {
                    this.cutA(h2a);
                }
                _PolyhedralBoundedSolidOperator.join(h1b, nextedgeb.rightHalf!, withDebug);
                if (!_PolyhedralBoundedSolidSetNullEdgesConnector.isLooseB(h1b.mirrorHalfEdge())) {
                    this.cutB(h1b);
                }
            }

            if (h1a !== null && h1b !== null && h2a !== null && h2b !== null) {
                this.cutA(nextedgea.rightHalf!);
                this.cutB(nextedgeb.rightHalf!);
                _PolyhedralBoundedSolidSetNullEdgesConnector.tracePipelineSummary(
                    "connect pair[" +
                        i +
                        "] produced cuts h1a=" +
                        _PolyhedralBoundedSolidSetNullEdgesConnector.summarizeHalfEdge(h1a) +
                        " h2a=" +
                        _PolyhedralBoundedSolidSetNullEdgesConnector.summarizeHalfEdge(h2a) +
                        " h1b=" +
                        _PolyhedralBoundedSolidSetNullEdgesConnector.summarizeHalfEdge(h1b) +
                        " h2b=" +
                        _PolyhedralBoundedSolidSetNullEdgesConnector.summarizeHalfEdge(h2b),
                );
            } else {
                PolyhedralBoundedSolidStatistics.recordJoinIncompleteCase();
                PolyhedralBoundedSolidStatistics.recordOperationFailureCase();
                _PolyhedralBoundedSolidSetNullEdgesConnector.tracePipelineSummary(
                    "connect pair[" +
                        i +
                        "] incomplete h1a=" +
                        _PolyhedralBoundedSolidSetNullEdgesConnector.summarizeHalfEdge(h1a) +
                        " h2a=" +
                        _PolyhedralBoundedSolidSetNullEdgesConnector.summarizeHalfEdge(h2a) +
                        " h1b=" +
                        _PolyhedralBoundedSolidSetNullEdgesConnector.summarizeHalfEdge(h1b) +
                        " h2b=" +
                        _PolyhedralBoundedSolidSetNullEdgesConnector.summarizeHalfEdge(h2b) +
                        " looseA=" +
                        _PolyhedralBoundedSolidSetNullEdgesConnector.endsa!.size() +
                        " looseB=" +
                        _PolyhedralBoundedSolidSetNullEdgesConnector.endsb!.size(),
                );
            }
        }

        const endsa = _PolyhedralBoundedSolidSetNullEdgesConnector.endsa!;
        const endsb = _PolyhedralBoundedSolidSetNullEdgesConnector.endsb!;

        if ((debugFlags & _PolyhedralBoundedSolidSetNullEdgesConnector.DEBUG_05_CONNECT) !== 0x00) {
            console.log("  . Pending null edges to connect:");
            for (i = 0; i < endsa.size(); i++) {
                console.log("    . A[" + (i + 1) + "]: " + endsa.get(i));
            }
            for (i = 0; i < endsb.size(); i++) {
                console.log("    . B[" + (i + 1) + "]: " + endsb.get(i));
            }
        }
        _PolyhedralBoundedSolidSetNullEdgesConnector.tracePipelineSummary(
            "connect end sonfa=" +
                this.sonfa!.size() +
                " sonfb=" +
                this.sonfb!.size() +
                " looseA=" +
                endsa.size() +
                " looseB=" +
                endsb.size(),
        );
        // §6.1-B: post-loop safety nets removed.
        // Per [MANT1988] Program 15.14, the main loop must leave
        // looseA == looseB == 0 by itself. Any survivor for
        // MANT1988_15_1 INTERSECTION/SUBTRACT (looseA=4) is the
        // visible symptom of the §5.2 deferred sectoroverlap fix
        // upstream — see plan §6.1-C analysis. A post-pass closure
        // (pairLatentLooseEnds) was attempted but turned out to fuse
        // legitimately-separate shells in cases like HOLLOW_BRICK, so
        // it was retired. The right fix is upstream, not here.
        _PolyhedralBoundedSolidSetNullEdgesConnector.tracePipelineSummary(
            "connect post-pass sonfa=" +
                this.sonfa!.size() +
                " sonfb=" +
                this.sonfb!.size() +
                " looseA=" +
                endsa.size() +
                " looseB=" +
                endsb.size(),
        );
        this.updateLastSnapshot();

        for (i = 0; i < endsa.size() && i < endsb.size(); i++) {
            _PolyhedralBoundedSolidSetNullEdgesConnector.tracePipelineSummary(
                "connect loose[" +
                    i +
                    "] A=" +
                    _PolyhedralBoundedSolidSetNullEdgesConnector.summarizeHalfEdge(endsa.get(i)) +
                    " B=" +
                    _PolyhedralBoundedSolidSetNullEdgesConnector.summarizeHalfEdge(endsb.get(i)),
            );
        }
    }
}

type _ConnectResult = ConnectResult;

export namespace _PolyhedralBoundedSolidSetNullEdgesConnector {
    export type ConnectResult = _ConnectResult;
}
