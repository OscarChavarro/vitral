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
import type { InfinitePlane } from "../../../../surface/InfinitePlane.js";
import type { PolyhedralBoundedSolid } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidEulerOperators } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import { PolyhedralBoundedSolidNumericPolicy } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import type { ToleranceContext } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
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
    private readonly connector: _PolyhedralBoundedSolidSetNullEdgesConnector;

    public constructor(
        sonfa: ArrayList<_PolyhedralBoundedSolidFace>,
        sonfb: ArrayList<_PolyhedralBoundedSolidFace>,
        connector: _PolyhedralBoundedSolidSetNullEdgesConnector,
    ) {
        this.sonfaList = sonfa;
        this.sonfbList = sonfb;
        this.connector = connector;
    }

    /** Pair index of a null face of A, or -1 (used by finish). */
    public pairIndexA(face: _PolyhedralBoundedSolidFace | null): number {
        return this.connector.getSonfaPairIndex(face);
    }

    /** Pair index of a null face of B, or -1 (used by finish). */
    public pairIndexB(face: _PolyhedralBoundedSolidFace | null): number {
        return this.connector.getSonfbPairIndex(face);
    }

    public sonfa(): ArrayList<_PolyhedralBoundedSolidFace> {
        return this.sonfaList;
    }

    public sonfb(): ArrayList<_PolyhedralBoundedSolidFace> {
        return this.sonfbList;
    }
}

/**
Counters of the most recent `connect` on the calling thread, for
tests and diagnostics. The connector itself keeps all its working state
in the instance (Fable5.2Analysis.md pending item 9).
*/
class Diagnostics {
    public looseA = 0;
    public looseB = 0;
    public sonfaCount = 0;
    public sonfbCount = 0;
    public pairCount = 0;
    public curveReport: _PolyhedralBoundedSolidSetIntersectionCurveBuilder.Report | null = null;
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
    private operation = 0;
    private nextNullEdgeIndex = 0;
    private sonea: ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> | null = null;
    private soneb: ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> | null = null;
    private endsa: ArrayList<_PolyhedralBoundedSolidHalfEdge> | null = null;
    private endsb: ArrayList<_PolyhedralBoundedSolidHalfEdge> | null = null;
    private sonfa: ArrayList<_PolyhedralBoundedSolidFace> | null = null;
    private sonfb: ArrayList<_PolyhedralBoundedSolidFace> | null = null;
    private sonfaPairIndexByFaceId: HashMap<number, number> | null = null;
    private sonfbPairIndexByFaceId: HashMap<number, number> | null = null;
    /**
    Java keeps the diagnostics in a `ThreadLocal`; this runtime is
    single-threaded, so a static field holds the same per-caller value.
    */
    private static LAST: Diagnostics = new Diagnostics();
    private currentConnectPairIndex = 0;
    private nextSyntheticPairIndex = 0;

    /**
    Structural report of the intersection curves reconstructed from the
    sonea/soneb pairs of this connect. Captured unconditionally (the
    reconstruction is cheap) so tests and diagnostics can audit curve
    closure without enabling pipeline traces; see `getLastCurveReport`.
    */
    private curveReport: _PolyhedralBoundedSolidSetIntersectionCurveBuilder.Report | null = null;

    /** Curve report of the most recent connect on the calling thread. */
    public static getLastCurveReport(): _PolyhedralBoundedSolidSetIntersectionCurveBuilder.Report | null {
        return _PolyhedralBoundedSolidSetNullEdgesConnector.LAST.curveReport;
    }

    public static clearLastCurveReport(): void {
        _PolyhedralBoundedSolidSetNullEdgesConnector.LAST.curveReport = null;
    }

    /**
    True when `sortNullEdges()` already oriented every strut along
    the intersection curves (mythosPlan §5.3). The connect loop must then
    respect that orientation instead of re-normalizing edge halves by
    vertex-id comparison (the legacy rule, which encodes classifier emission
    order and breaks when that order is not the curve order).
    */
    private curveOrientationApplied = false;

    /**
    Junction adjacency in processing-position space when the curve order is
    active (from
    `_PolyhedralBoundedSolidSetIntersectionCurveBuilder`); null
    otherwise. `scanjoin` joins a null edge only with a loose end left by
    one of its neighbours along the curve.
    */
    private curveNeighborPositions: number[][] | null = null;

    /**
    Processing pair index that pushed each loose entry, parallel to
    `endsa`/`endsb`.
    */
    private endsPairIndex: ArrayList<number> | null = null;

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

    private setCurrentConnectContext(pairIndex: number): void {
        this.currentConnectPairIndex = pairIndex;
    }

    private allocateSyntheticPairIndex(): number {
        const pairIndex = this.nextSyntheticPairIndex;
        this.nextSyntheticPairIndex++;
        return pairIndex;
    }

    public getSonfaPairIndex(face: _PolyhedralBoundedSolidFace | null): number {
        if (face === null || this.sonfaPairIndexByFaceId === null) {
            return -1;
        }
        const pairIndex = this.sonfaPairIndexByFaceId.get(face.id);
        return pairIndex !== undefined ? pairIndex : -1;
    }

    public getSonfbPairIndex(face: _PolyhedralBoundedSolidFace | null): number {
        if (face === null || this.sonfbPairIndexByFaceId === null) {
            return -1;
        }
        const pairIndex = this.sonfbPairIndexByFaceId.get(face.id);
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

    private removeLoosePair(index: number): void {
        this.endsa!.remove(index);
        this.endsb!.remove(index);
    }

    public static getLastLooseACount(): number {
        return _PolyhedralBoundedSolidSetNullEdgesConnector.LAST.looseA;
    }

    public static getLastLooseBCount(): number {
        return _PolyhedralBoundedSolidSetNullEdgesConnector.LAST.looseB;
    }

    public static getLastSonfaCount(): number {
        return _PolyhedralBoundedSolidSetNullEdgesConnector.LAST.sonfaCount;
    }

    public static getLastSonfbCount(): number {
        return _PolyhedralBoundedSolidSetNullEdgesConnector.LAST.sonfbCount;
    }

    public static getLastPairCount(): number {
        return _PolyhedralBoundedSolidSetNullEdgesConnector.LAST.pairCount;
    }

    private updateLastSnapshot(): void {
        const last = _PolyhedralBoundedSolidSetNullEdgesConnector.LAST;
        last.looseA = this.endsa !== null ? this.endsa.size() : 0;
        last.looseB = this.endsb !== null ? this.endsb.size() : 0;
        last.sonfaCount = this.sonfa !== null ? this.sonfa.size() : 0;
        last.sonfbCount = this.sonfb !== null ? this.sonfb.size() : 0;
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

    private rebindClassicCurrentNullEdgeIfNeeded(
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
            this.operation !== _PolyhedralBoundedSolidOperator.SUBTRACT ||
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

    private rebindClassicCurrentNullEdgesIfNeeded(
        currentARight: _PolyhedralBoundedSolidHalfEdge | null,
        currentALeft: _PolyhedralBoundedSolidHalfEdge | null,
        currentBLeft: _PolyhedralBoundedSolidHalfEdge | null,
        currentBRight: _PolyhedralBoundedSolidHalfEdge | null,
    ): void {
        this.rebindClassicCurrentNullEdgeIfNeeded(
            currentARight,
            currentALeft,
            currentBLeft,
            currentBRight,
            this.endsa,
            this.endsb,
            "A",
        );
        this.rebindClassicCurrentNullEdgeIfNeeded(
            currentBLeft,
            currentBRight,
            currentARight,
            currentALeft,
            this.endsb,
            this.endsa,
            "B",
        );
    }

    public connect(
        op: number,
        flags: number,
        inSonea: ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>,
        inSoneb: ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>,
    ): ConnectResult {
        this.operation = op;
        _PolyhedralBoundedSolidSetNullEdgesConnector.debugFlags = flags;
        this.sonea = inSonea;
        this.soneb = inSoneb;
        const last = new Diagnostics();
        _PolyhedralBoundedSolidSetNullEdgesConnector.LAST = last;
        last.pairCount = Math.min(this.sonea.size(), this.soneb.size());
        this.nextSyntheticPairIndex = last.pairCount;
        // [MANT1988] §15.7 Program 15.14: single Connect implementation.
        // The "flexibleEndpointChains" alternative path was removed in §6.1
        // of plan-csg-boolean-fix-stage2 because it duplicated setOpConnect
        // with extra heuristics that the book does not require.
        this.setOpConnect();
        return new ConnectResult(this.sonfa!, this.sonfb!, this);
    }

    private sortNullEdges(): void {
        this.curveOrientationApplied = false;
        this.curveNeighborPositions = null;

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
        this.curveReport = _PolyhedralBoundedSolidSetIntersectionCurveBuilder.build(
            this.sonea!,
            this.soneb!,
            PolyhedralBoundedSolidNumericPolicy.defaultContext().unitVectorTolerance(),
        );
        _PolyhedralBoundedSolidSetNullEdgesConnector.LAST.curveReport = this.curveReport;
        _PolyhedralBoundedSolidSetNullEdgesConnector.tracePipelineSummary("connect " + this.curveReport.summarize());
        if (this.dropIsolatedNodes()) {
            // The report kept for diagnostics stays the one above: what
            // the classifier emitted. This one describes what is left to
            // connect.
            this.curveReport = _PolyhedralBoundedSolidSetIntersectionCurveBuilder.build(
                this.sonea!,
                this.soneb!,
                PolyhedralBoundedSolidNumericPolicy.defaultContext().unitVectorTolerance(),
            );
            _PolyhedralBoundedSolidSetNullEdgesConnector.tracePipelineSummary(
                "connect " + this.curveReport.summarize(),
            );
        }

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
        if (this.curveReport.isCleanlyClosed()) {
            const neighbors: (number[][] | null)[] = [null];
            const curveOrder = _PolyhedralBoundedSolidSetIntersectionCurveBuilder.orderAndOrientAlongCurves(
                this.curveReport.cycles,
                n,
                this.sonea!,
                this.soneb!,
                neighbors,
            );
            if (curveOrder !== null) {
                this.applyOrderPermutation(curveOrder);
                this.curveOrientationApplied = true;
                this.curveNeighborPositions = neighbors[0]!;
                _PolyhedralBoundedSolidSetNullEdgesConnector.tracePipelineSummary(
                    "connect curve-order applied: cycles=" + this.curveReport.cycles.size(),
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
        const endsa = this.endsa!;
        const endsb = this.endsb!;
        const debugFlags = _PolyhedralBoundedSolidSetNullEdgesConnector.debugFlags;

        for (i = 0; i < endsa.size(); i++) {
            // A face that is not convex can be crossed several times by
            // the same face of the other solid: the crossings lie on one
            // line and all of them pass the face and role test of
            // `neighbor`. Only the loose end left by a neighbour along the
            // intersection curve is the other end of this chord; joining
            // any other one draws a chord through the notch of the face
            // (plan-stage11 Phase F). Program 15.13 of [MANT1988] gets the
            // same effect from the order of its sorted null edges.
            if (!this.isCurveNeighborOfCurrentPair(i)) {
                continue;
            }
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
                if (i < this.endsPairIndex!.size()) {
                    this.endsPairIndex!.remove(i);
                }
                return ret;
            }
        }

        endsa.add(hea);
        endsb.add(heb);
        this.endsPairIndex!.add(this.currentConnectPairIndex);
        return null;
    }

    /**
    True when the loose entry `i` was left by one of the two pairs next to
    the current pair on its intersection curve, or when the curves are not
    known (no curve order applied): then every entry is a candidate, as in
    the book.
    */
    private isCurveNeighborOfCurrentPair(i: number): boolean {
        if (
            !this.curveOrientationApplied ||
            this.curveNeighborPositions === null ||
            this.currentConnectPairIndex < 0 ||
            this.currentConnectPairIndex >= this.curveNeighborPositions.length ||
            this.curveNeighborPositions[this.currentConnectPairIndex] === null ||
            i >= this.endsPairIndex!.size()
        ) {
            return true;
        }
        const pusherPosition = this.endsPairIndex!.get(i);

        return (
            pusherPosition === this.curveNeighborPositions[this.currentConnectPairIndex]![0] ||
            pusherPosition === this.curveNeighborPositions[this.currentConnectPairIndex]![1]
        );
    }

    private isLooseA(he: _PolyhedralBoundedSolidHalfEdge | null): boolean {
        let i: number;
        const endsa = this.endsa!;

        for (i = 0; i < endsa.size(); i++) {
            if (he === endsa.get(i)) return true;
        }

        return false;
    }

    private isLooseB(he: _PolyhedralBoundedSolidHalfEdge | null): boolean {
        let i: number;
        const endsb = this.endsb!;

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
                if (this.sonfaPairIndexByFaceId !== null) {
                    this.sonfaPairIndexByFaceId.put(addedFace.id, this.currentConnectPairIndex);
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
                if (this.sonfbPairIndexByFaceId !== null) {
                    this.sonfbPairIndexByFaceId.put(addedFace.id, this.currentConnectPairIndex);
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
        const withDebug =
            (debugFlags & _PolyhedralBoundedSolidSetNullEdgesConnector.DEBUG_99_SHOWOPERATIONS) !== 0x0 &&
            (debugFlags & _PolyhedralBoundedSolidSetNullEdgesConnector.DEBUG_05_CONNECT) !== 0x00;

        this.endsa = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();
        this.endsb = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();
        this.endsPairIndex = new ArrayList<number>();

        this.sonfa = new ArrayList<_PolyhedralBoundedSolidFace>();
        this.sonfb = new ArrayList<_PolyhedralBoundedSolidFace>();
        this.sonfaPairIndexByFaceId = new HashMap<number, number>();
        this.sonfbPairIndexByFaceId = new HashMap<number, number>();
        this.setCurrentConnectContext(-1);
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
                const endsa = this.endsa!;
                const endsb = this.endsb!;
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
            if (!this.curveOrientationApplied) {
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

            this.rebindClassicCurrentNullEdgesIfNeeded(
                nextedgea.rightHalf,
                nextedgea.leftHalf,
                nextedgeb.leftHalf,
                nextedgeb.rightHalf,
            );

            this.setCurrentConnectContext(i);
            r = this.scanjoin(nextedgea.rightHalf!, nextedgeb.leftHalf!);
            if (r !== null) {
                h1a = r[0]!;
                h2b = r[1]!;
                _PolyhedralBoundedSolidOperator.join(h1a, nextedgea.rightHalf!, withDebug);
                if (!this.isLooseA(h1a.mirrorHalfEdge())) {
                    this.cutA(h1a);
                }
                _PolyhedralBoundedSolidOperator.join(h2b, nextedgeb.leftHalf!, withDebug);
                if (!this.isLooseB(h2b.mirrorHalfEdge())) {
                    this.cutB(h2b);
                }
            }

            this.setCurrentConnectContext(i);
            r = this.scanjoin(nextedgea.leftHalf!, nextedgeb.rightHalf!);
            if (r !== null) {
                h2a = r[0]!;
                h1b = r[1]!;
                _PolyhedralBoundedSolidOperator.join(h2a, nextedgea.leftHalf!, withDebug);
                if (!this.isLooseA(h2a.mirrorHalfEdge())) {
                    this.cutA(h2a);
                }
                _PolyhedralBoundedSolidOperator.join(h1b, nextedgeb.rightHalf!, withDebug);
                if (!this.isLooseB(h1b.mirrorHalfEdge())) {
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
                        this.endsa!.size() +
                        " looseB=" +
                        this.endsb!.size(),
                );
            }
        }

        const endsa = this.endsa!;
        const endsb = this.endsb!;

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
        // Per [MANT1988] Program 15.14 the main loop must leave
        // looseA == looseB == 0. The only survivors left are isolated
        // struts: both halves of one null edge still loose on A and on B.
        // The vertex/vertex classifier emits them at a coplanar contact
        // along a boundary ray (sectoroverlap is closed, see its comment)
        // where the curve does not pass, e.g. MANT1988_15_1 ∩ and −. Such
        // a strut bounds no null face; killing it restores the vertex it
        // split. It never joins anything, so it cannot fuse shells (the
        // retired pairLatentLooseEnds closure did, on HOLLOW_BRICK).
        this.dropIsolatedStruts();
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

    /**
    A node without neighbours on the intersection curve is a point where
    the two boundaries touch without crossing: nothing is to be cut there.
    When such nodes are the only thing that keeps the curves from being
    closed cycles, their null edges are removed before connect (each with
    lkev, which merges its two coincident vertices back into one), so that
    the rest is connected in curve order. Groups of nodes on a pair of
    coplanar faces do not prevent it: on coplanar faces those groups are
    made of the isolated nodes themselves (vertices of one solid lying in
    a face of the other) and are gone once they are removed. They used to stay, and a single
    one of them sent the whole operation to the order of emission, where a
    face crossed twice along one line is joined across its hole
    (plan-stage11, coplanar non-convex faces).
    @return true when some null edge was removed
    */
    private dropIsolatedNodes(): boolean {
        const dropped = new ArrayList<number>();
        const curveReport = this.curveReport!;
        const sonea = this.sonea!;
        const soneb = this.soneb!;
        let covered = 0;
        let i: number;
        let j: number;

        if (
            !curveReport.pinchNodes.isEmpty() ||
            curveReport.oddFacePairGroupCount > 0 ||
            sonea.size() !== soneb.size()
        ) {
            return false;
        }
        for (i = 0; i < curveReport.openChains.size(); i++) {
            const chain = curveReport.openChains.get(i);
            for (j = 0; j < chain.length; j++) {
                if (!this.liesInCoplanarFaces(chain[j]!)) {
                    return false;
                }
                dropped.add(chain[j]!);
            }
            covered += chain.length;
        }
        dropped.addAll(curveReport.isolatedNodes);
        if (dropped.isEmpty()) {
            return false;
        }
        for (i = 0; i < curveReport.cycles.size(); i++) {
            covered += curveReport.cycles.get(i).length;
        }
        if (covered + curveReport.isolatedNodes.size() !== curveReport.nodeCount) {
            return false;
        }
        Collections.sort(dropped);
        for (i = dropped.size() - 1; i >= 0; i--) {
            const index = dropped.get(i);
            const a = sonea.get(index).e;
            const b = soneb.get(index).e;
            _PolyhedralBoundedSolidSetNullEdgesConnector.tracePipelineSummary(
                "connect drop contact node " +
                    index +
                    " A=" +
                    _PolyhedralBoundedSolidSetNullEdgesConnector.summarizeHalfEdge(a.rightHalf) +
                    " B=" +
                    _PolyhedralBoundedSolidSetNullEdgesConnector.summarizeHalfEdge(b.rightHalf),
            );
            PolyhedralBoundedSolidEulerOperators.lkev(
                a.rightHalf!.parentLoop.parentFace.parentSolid,
                a.rightHalf!,
                a.leftHalf!,
            );
            PolyhedralBoundedSolidEulerOperators.lkev(
                b.rightHalf!.parentLoop.parentFace.parentSolid,
                b.rightHalf!,
                b.leftHalf!,
            );
            sonea.remove(index);
            soneb.remove(index);
        }
        return true;
    }

    /**
    True when node `index` is a vertex of one solid lying inside a face of
    the other, with a face of its own on the plane of that face: on that
    solid the null edge is a strut inside the face, and some face around
    the vertex of the first solid is coplanar with it. A chain of such
    nodes is an edge lying in a coplanar face; it crosses nothing.
    */
    private liesInCoplanarFaces(index: number): boolean {
        const a = this.sonea!.get(index).e;
        const b = this.soneb!.get(index).e;

        if (
            a === null ||
            b === null ||
            a.rightHalf === null ||
            a.leftHalf === null ||
            b.rightHalf === null ||
            b.leftHalf === null
        ) {
            return false;
        }
        return (
            _PolyhedralBoundedSolidSetNullEdgesConnector.isVertexInCoplanarFace(a, b) ||
            _PolyhedralBoundedSolidSetNullEdgesConnector.isVertexInCoplanarFace(b, a)
        );
    }

    /**
    True when `strut` lies inside one face and some face around the ends
    of `vertexEdge` is on the plane of that face.
    */
    private static isVertexInCoplanarFace(
        vertexEdge: _PolyhedralBoundedSolidEdge,
        strut: _PolyhedralBoundedSolidEdge,
    ): boolean {
        const face = strut.rightHalf!.parentLoop.parentFace;
        const plane = face.getContainingPlane();

        if (face !== strut.leftHalf!.parentLoop.parentFace || plane === null) {
            return false;
        }
        const context = PolyhedralBoundedSolidNumericPolicy.forSolids(
            face.parentSolid,
            vertexEdge.rightHalf!.parentLoop.parentFace.parentSolid,
        );
        return (
            _PolyhedralBoundedSolidSetNullEdgesConnector.hasFaceOnPlane(vertexEdge.rightHalf!, plane, context) ||
            _PolyhedralBoundedSolidSetNullEdgesConnector.hasFaceOnPlane(vertexEdge.leftHalf!, plane, context)
        );
    }

    /** Walks the faces around the starting vertex of `start`. */
    private static hasFaceOnPlane(
        start: _PolyhedralBoundedSolidHalfEdge,
        plane: InfinitePlane,
        context: ToleranceContext,
    ): boolean {
        let he: _PolyhedralBoundedSolidHalfEdge | null = start;
        const normal = plane.getNormal();
        let guard = 0;

        do {
            const own = he!.parentLoop.parentFace.getContainingPlane();
            if (own !== null) {
                const ownNormal = own.getNormal();
                if (
                    normal.crossProduct(ownNormal).length() <=
                        context.unitVectorTolerance() * normal.length() * ownNormal.length() &&
                    Math.abs(plane.pointDistance(he!.startingVertex.position)) <= context.bigEpsilon()
                ) {
                    return true;
                }
            }
            const mate = he!.mirrorHalfEdge();
            if (mate === null) {
                return false;
            }
            he = mate.next();
            guard++;
        } while (he !== null && he !== start && guard < 1000);
        return false;
    }

    /**
    Removes the null edges whose two halves are both loose, on A and on B
    at the same loose positions (see the call site). Each is killed with
    lkev, which merges its two coincident vertices back into one.
    */
    private dropIsolatedStruts(): void {
        const endsa = this.endsa!;
        const endsb = this.endsb!;
        const endsPairIndex = this.endsPairIndex!;
        let i: number;
        let j: number;

        for (i = 0; i < endsa.size(); i++) {
            const a = endsa.get(i);
            const b = endsb.get(i);
            for (j = i + 1; j < endsa.size(); j++) {
                if (endsa.get(j) !== a.mirrorHalfEdge() || endsb.get(j) !== b.mirrorHalfEdge()) {
                    continue;
                }
                _PolyhedralBoundedSolidSetNullEdgesConnector.tracePipelineSummary(
                    "connect drop isolated strut A=" +
                        _PolyhedralBoundedSolidSetNullEdgesConnector.summarizeHalfEdge(a) +
                        " B=" +
                        _PolyhedralBoundedSolidSetNullEdgesConnector.summarizeHalfEdge(b),
                );
                PolyhedralBoundedSolidEulerOperators.lkev(a.parentLoop.parentFace.parentSolid, a, a.mirrorHalfEdge()!);
                PolyhedralBoundedSolidEulerOperators.lkev(b.parentLoop.parentFace.parentSolid, b, b.mirrorHalfEdge()!);
                this.removeLoosePair(j);
                this.removeLoosePair(i);
                if (j < endsPairIndex.size()) {
                    endsPairIndex.remove(j);
                }
                if (i < endsPairIndex.size()) {
                    endsPairIndex.remove(i);
                }
                i--;
                break;
            }
        }
    }
}

type _ConnectResult = ConnectResult;

export namespace _PolyhedralBoundedSolidSetNullEdgesConnector {
    export type ConnectResult = _ConnectResult;
}
