//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import { VSDK } from "../../../../../../common/VSDK.js";
import { Logger } from "../../../../../../common/logging/Logger.js";
import type { Vector3Dd } from "../../../../../../common/linealAlgebra/Vector3Dd.js";
import type { PolyhedralBoundedSolid } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidEulerOperators } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import { PolyhedralBoundedSolidNumericPolicy } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import type { _PolyhedralBoundedSolidFace } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { _PolyhedralBoundedSolidVertex } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";
import { _PolyhedralBoundedSolidOperator } from "../../_PolyhedralBoundedSolidOperator.js";
import type { _SetOperationContext } from "../_SetOperationContext.js";
import { _SetOperationTrace } from "../_SetOperationTrace.js";
import { _PolyhedralBoundedSolidSetGeometricPredicateProcessor } from "../intersection/_PolyhedralBoundedSolidSetGeometricPredicateProcessor.js";
import { _PolyhedralBoundedSolidSetOperatorNullEdge } from "../topology/_PolyhedralBoundedSolidSetOperatorNullEdge.js";
import { _PolyhedralBoundedSolidSetNonIntersectingClassifier } from "./_PolyhedralBoundedSolidSetNonIntersectingClassifier.js";
import { _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector } from "./_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.js";
import { _PolyhedralBoundedSolidSetVertexFaceClassifier } from "./_PolyhedralBoundedSolidSetVertexFaceClassifier.js";
import { _PolyhedralBoundedSolidSetVertexVertexClassifier } from "./_PolyhedralBoundedSolidSetVertexVertexClassifier.js";

const isCoplanarTangentialTraceEnabled = (): boolean => _SetOperationTrace.isCoplanarTangentialTraceEnabled();
const traceCoplanarTangential = (message: string): void => _SetOperationTrace.traceCoplanarTangential(message);
const tracePipelineSummary = (message: string): void => _SetOperationTrace.tracePipelineSummary(message);

type VertexVertexClassificationData = _PolyhedralBoundedSolidSetVertexVertexClassifier.VertexVertexClassificationData;
const OnSector = _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector;

/**
Classification stages, connection stages, and no-intersection containment
logic for the Boolean set-operations pipeline of chapter [MANT1988].15.
*/
export class _PolyhedralBoundedSolidSetClassifier extends _PolyhedralBoundedSolidOperator {
    private static readonly DEBUG_01_STRUCTURE = 0x01;
    private static readonly DEBUG_04_VERTEX_VERTEX_CLASSIFIER = 0x08;
    private static readonly DEBUG_99_SHOW_OPERATIONS = 0x40;

    private static readonly NO_INT_RELATION_DISJOINT = 0;
    private static readonly NO_INT_RELATION_TOUCHING = 1;
    private static readonly NO_INT_RELATION_A_IN_B = 2;
    private static readonly NO_INT_RELATION_B_IN_A = 3;

    private static debugFlags = 0;

    public static runSetOpClassify(
        op: number,
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
        flags: number,
        ctx: _SetOperationContext,
    ): void {
        _PolyhedralBoundedSolidSetClassifier.debugFlags = flags;
        _PolyhedralBoundedSolidSetClassifier.setOpClassify(ctx, op, inSolidA, inSolidB);
        tracePipelineSummary(
            "classify done op=" +
                op +
                " sonva=" +
                ctx.sonva!.size() +
                " sonvb=" +
                ctx.sonvb!.size() +
                " sonvv=" +
                ctx.sonvv!.size() +
                " sonea=" +
                ctx.sonea!.size() +
                " soneb=" +
                ctx.soneb!.size(),
        );
    }

    public static runSetOpNoIntersectionCase(
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
        outRes: PolyhedralBoundedSolid,
        op: number,
    ): PolyhedralBoundedSolid {
        return _PolyhedralBoundedSolidSetNonIntersectingClassifier.runSetOpNoIntersectionCase(
            inSolidA,
            inSolidB,
            outRes,
            op,
        );
    }

    public static runTouchingOnlyPreflightCase(
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
    ): boolean {
        return _PolyhedralBoundedSolidSetNonIntersectingClassifier.runTouchingOnlyPreflightCase(inSolidA, inSolidB);
    }

    private static compareToZero(value: number): number {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.compareToZero(value);
    }

    private static summarizeHalfEdge(he: _PolyhedralBoundedSolidHalfEdge | null): string {
        if (he === null) {
            return "null";
        }

        return "he(v=" + he.startingVertex.id + ",f=" + he.parentLoop.parentFace.id + ")";
    }

    private static summarizeNullEdge(edge: _PolyhedralBoundedSolidSetOperatorNullEdge | null): string {
        if (edge === null || edge.e === null) {
            return "null";
        }
        return (
            _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(edge.e.rightHalf) +
            " | " +
            _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(edge.e.leftHalf)
        );
    }

    private static formatVertexVertexTraceContext(
        op: number,
        cursor: number,
        caseName: string,
        action: string,
        type: number,
    ): string {
        return (
            "op=" +
            op +
            " cursor=" +
            cursor +
            " case=" +
            caseName +
            " action=" +
            action +
            " target=" +
            (type === 0 ? "A" : "B")
        );
    }

    private static labelSectorState(state: number): string {
        switch (state) {
            case OnSector.ON:
                return "ON";
            case OnSector.OUT:
                return "OUT";
            case OnSector.IN:
                return "IN";
            default:
                return "?" + state;
        }
    }

    private static summarizeSector(data: VertexVertexClassificationData | null, index: number): string {
        if (data === null || index < 0 || index >= data.sectors.size()) {
            return "sector[idx=" + index + "]=<out>";
        }
        const sector = data.sectors.get(index);
        return (
            "sector[idx=" +
            index +
            ",intersect=" +
            sector.intersect +
            ",sectA=" +
            sector.secta +
            ",sectB=" +
            sector.sectb +
            ",s1a=" +
            _PolyhedralBoundedSolidSetClassifier.labelSectorState(sector.s1a) +
            ",s2a=" +
            _PolyhedralBoundedSolidSetClassifier.labelSectorState(sector.s2a) +
            ",s1b=" +
            _PolyhedralBoundedSolidSetClassifier.labelSectorState(sector.s1b) +
            ",s2b=" +
            _PolyhedralBoundedSolidSetClassifier.labelSectorState(sector.s2b) +
            ",hea=" +
            _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(data.nba.get(sector.secta).he) +
            ",heb=" +
            _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(data.nbb.get(sector.sectb).he) +
            "]"
        );
    }

    private static traceVertexVertexDecisionStep(
        data: VertexVertexClassificationData | null,
        op: number,
        phase: string,
        index: number,
        ha1: _PolyhedralBoundedSolidHalfEdge | null,
        ha2: _PolyhedralBoundedSolidHalfEdge | null,
        hb1: _PolyhedralBoundedSolidHalfEdge | null,
        hb2: _PolyhedralBoundedSolidHalfEdge | null,
    ): void {
        tracePipelineSummary(
            "vv-step op=" +
                op +
                " phase=" +
                phase +
                " " +
                _PolyhedralBoundedSolidSetClassifier.summarizeSector(data, index) +
                " => ha1=" +
                _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(ha1) +
                " ha2=" +
                _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(ha2) +
                " hb1=" +
                _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(hb1) +
                " hb2=" +
                _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(hb2),
        );
    }

    private static traceVertexVertexDecisionReason(
        op: number,
        cursor: number,
        caseName: string,
        reason: string,
        ha1: _PolyhedralBoundedSolidHalfEdge | null,
        ha2: _PolyhedralBoundedSolidHalfEdge | null,
        hb1: _PolyhedralBoundedSolidHalfEdge | null,
        hb2: _PolyhedralBoundedSolidHalfEdge | null,
    ): void {
        tracePipelineSummary(
            "vv-decision op=" +
                op +
                " cursor=" +
                cursor +
                " case=" +
                caseName +
                " reason=" +
                reason +
                " ha1=" +
                _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(ha1) +
                " ha2=" +
                _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(ha2) +
                " hb1=" +
                _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(hb1) +
                " hb2=" +
                _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(hb2),
        );
    }

    private static selectAlternativeBHalfEdge(
        data: VertexVertexClassificationData | null,
        sectb: number,
        avoid: _PolyhedralBoundedSolidHalfEdge | null,
    ): _PolyhedralBoundedSolidHalfEdge | null {
        let candidate: _PolyhedralBoundedSolidHalfEdge | null;

        if (data === null || data.nbb === null || data.nbb.isEmpty()) {
            return null;
        }
        if (sectb < 0 || sectb >= data.nbb.size()) {
            return null;
        }

        const nextsectb = sectb === data.nbb.size() - 1 ? 0 : sectb + 1;
        const prevsectb = sectb === 0 ? data.nbb.size() - 1 : sectb - 1;

        candidate = data.nbb.get(nextsectb).he;
        if (candidate !== null && candidate !== avoid) {
            return candidate;
        }

        candidate = data.nbb.get(prevsectb).he;
        if (candidate !== null && candidate !== avoid) {
            return candidate;
        }

        return null;
    }

    private static resolveBEndpointCandidate(
        data: VertexVertexClassificationData,
        sectorIndex: number,
        selectHb1: boolean,
        hb1: _PolyhedralBoundedSolidHalfEdge | null,
        hb2: _PolyhedralBoundedSolidHalfEdge | null,
    ): _PolyhedralBoundedSolidHalfEdge | null {
        const sector = data.sectors.get(sectorIndex);
        const candidate = data.nbb.get(sector.sectb).he;
        const avoid = selectHb1 ? hb2 : hb1;

        if (candidate !== null && candidate === avoid && sector.s2b === OnSector.ON) {
            const alternative = _PolyhedralBoundedSolidSetClassifier.selectAlternativeBHalfEdge(
                data,
                sector.sectb,
                avoid,
            );
            if (alternative !== null) {
                tracePipelineSummary(
                    "vv-adjust B endpoint sectorIdx=" +
                        sectorIndex +
                        " sectB=" +
                        sector.sectb +
                        " select=" +
                        (selectHb1 ? "hb1" : "hb2") +
                        " repeated=" +
                        _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(candidate) +
                        " alternative=" +
                        _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(alternative) +
                        " because s2b=ON",
                );
                return alternative;
            }
        }

        return candidate;
    }

    private static traceIntersectingSectorsSnapshot(data: VertexVertexClassificationData, cursor: number): void {
        let idx: number;

        if (!isCoplanarTangentialTraceEnabled()) {
            return;
        }

        for (idx = 0; idx < data.sectors.size(); idx++) {
            if (!data.sectors.get(idx).intersect) {
                continue;
            }
            traceCoplanarTangential(
                "  sector idx=" +
                    idx +
                    " cursor=" +
                    cursor +
                    " sectA=" +
                    data.sectors.get(idx).secta +
                    " sectB=" +
                    data.sectors.get(idx).sectb +
                    " s1a=" +
                    data.sectors.get(idx).s1a +
                    " s2a=" +
                    data.sectors.get(idx).s2a +
                    " s1b=" +
                    data.sectors.get(idx).s1b +
                    " s2b=" +
                    data.sectors.get(idx).s2b,
            );
        }
    }

    private static pointInFace(face: _PolyhedralBoundedSolidFace, point: Vector3Dd): number {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.pointInFace(face, point);
    }

    private static resolveCoplanarVertexVertexClass(op: number, sameOrientation: boolean, sideA: boolean): number {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.resolveCoplanarVertexVertexClass(
            op,
            sameOrientation,
            sideA,
        );
    }

    private static nextVertexId(current: PolyhedralBoundedSolid, other: PolyhedralBoundedSolid): number {
        const a = current.getMaxVertexId();
        const b = other.getMaxVertexId();
        let m = a;
        if (b > a) {
            m = b;
        }

        return m + 1;
    }

    /**
    Constructs a vector along the bisector of the sector defined by `he`.
    that points inward the he's containing face. This adapts the sector
    bisector idea from problem [MANT1988].14.1 to the set-operations
    classifiers of chapter [MANT1988].15.
    */
    public static inside(he: _PolyhedralBoundedSolidHalfEdge): Vector3Dd {
        let middle: Vector3Dd;
        let a: Vector3Dd;
        // Java names this local `b`; it is computed but unused there too.
        let _b: Vector3Dd;

        a = he.next()!.startingVertex.position.subtract(he.startingVertex.position);
        _b = he.previous()!.startingVertex.position.subtract(he.startingVertex.position);
        a = a.normalized();
        _b = _b.normalized();

        const n = he.parentLoop.parentFace.getContainingPlane()!.getNormal();

        middle = n.crossProduct(a);
        middle = middle.normalized();

        return middle;
    }

    private static sctrwitthin(dir: Vector3Dd, ref1: Vector3Dd, ref2: Vector3Dd, ref12: Vector3Dd): boolean {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.sctrwitthin(dir, ref1, ref2, ref12);
    }

    /**
    Normalizes one endpoint for `separateEdgeSequence` when a previous null
    strut edge was already inserted on the same vertex neighborhood.
    */
    private static recoverEdgeSequenceEndpointFromStrut(
        endpoint: _PolyhedralBoundedSolidHalfEdge | null,
        isFromEndpoint: boolean,
    ): _PolyhedralBoundedSolidHalfEdge | null {
        if (endpoint === null) {
            return null;
        }

        const prev = endpoint.previous();
        if (prev === null || prev.parentEdge === null) {
            return endpoint;
        }

        if (
            !_PolyhedralBoundedSolidSetClassifier.nulledge(prev) ||
            !_PolyhedralBoundedSolidSetClassifier.strutnulledge(prev)
        ) {
            return endpoint;
        }

        if (isFromEndpoint) {
            if (prev === prev.parentEdge.leftHalf) {
                return prev.previous();
            }
        } else {
            if (prev === prev.parentEdge.rightHalf) {
                return prev.previous();
            }
        }
        return endpoint;
    }

    /**
    Following program [MANT1988].15.12.
    Taking in to account the updated version modifications from
    [.wMANT2008].
    */
    private static separateEdgeSequence(
        ctx: _SetOperationContext,
        from: _PolyhedralBoundedSolidHalfEdge | null,
        to: _PolyhedralBoundedSolidHalfEdge | null,
        type: number,
        traceContext: string,
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
    ): void {
        const debugFlags = _PolyhedralBoundedSolidSetClassifier.debugFlags;
        //-----------------------------------------------------------------
        if ((debugFlags & _PolyhedralBoundedSolidSetClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !== 0x00) {
            console.log("      SEPARATEEDGESEQUENCE " + type);
            console.log("        From: " + from);
            console.log("        To: " + to);
        }

        if (from === null || to === null) {
            Logger.reportMessage(null, VSDK.FATAL_ERROR, "separateEdgeSequence", "Unexpected case: null halfedges!");
        }

        const s = from!.parentLoop.parentFace.parentSolid;

        if (s !== to!.parentLoop.parentFace.parentSolid) {
            Logger.reportMessage(
                null,
                VSDK.FATAL_ERROR,
                "separateEdgeSequence",
                "Unexpected case: halfedges on different solids!",
            );
        }

        //-----------------------------------------------------------------
        // Recover from null edges already inserted.
        // This block fully resolves the old A-E unsupported branches by
        // canonicalizing endpoint selection until both halfedges share origin.
        let recoveryGuard = 0;
        let changed: boolean;
        do {
            changed = false;

            const recoveredFrom = _PolyhedralBoundedSolidSetClassifier.recoverEdgeSequenceEndpointFromStrut(from, true);
            if (recoveredFrom !== from) {
                from = recoveredFrom;
                changed = true;
                if ((debugFlags & _PolyhedralBoundedSolidSetClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !== 0x00) {
                    console.log("        Recovered edge sequence case A");
                }
            }

            const recoveredTo = _PolyhedralBoundedSolidSetClassifier.recoverEdgeSequenceEndpointFromStrut(to, false);
            if (recoveredTo !== to) {
                to = recoveredTo;
                changed = true;
                if ((debugFlags & _PolyhedralBoundedSolidSetClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !== 0x00) {
                    console.log("        Recovered edge sequence case B");
                }
            }

            if (from!.startingVertex !== to!.startingVertex) {
                const fromPrev = from!.previous();
                const toPrev = to!.previous();

                if (
                    fromPrev !== null &&
                    toPrev !== null &&
                    fromPrev.parentEdge !== null &&
                    toPrev.parentEdge !== null &&
                    fromPrev === toPrev.mirrorHalfEdge()
                ) {
                    from = fromPrev;
                    changed = true;
                    if (
                        (debugFlags & _PolyhedralBoundedSolidSetClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !==
                        0x00
                    ) {
                        console.log("        Recovered edge sequence case C");
                    }
                } else if (fromPrev !== null && fromPrev.startingVertex === to!.startingVertex) {
                    from = fromPrev;
                    changed = true;
                    if (
                        (debugFlags & _PolyhedralBoundedSolidSetClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !==
                        0x00
                    ) {
                        console.log("        Recovered edge sequence case D");
                    }
                } else if (toPrev !== null && toPrev.startingVertex === from!.startingVertex) {
                    to = toPrev;
                    changed = true;
                    if (
                        (debugFlags & _PolyhedralBoundedSolidSetClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !==
                        0x00
                    ) {
                        console.log("        Recovered edge sequence case E");
                    }
                }
            }

            recoveryGuard++;
            if (recoveryGuard > 16) {
                break;
            }
        } while (changed);

        if (from!.startingVertex !== to!.startingVertex) {
            Logger.reportMessage(
                null,
                VSDK.FATAL_ERROR,
                "separateEdgeSequence",
                "Unable to recover endpoint pairing after A-E normalization.",
            );
            return;
        }

        //-----------------------------------------------------------------
        if (
            (debugFlags & _PolyhedralBoundedSolidSetClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !== 0x00 &&
            (debugFlags & _PolyhedralBoundedSolidSetClassifier.DEBUG_99_SHOW_OPERATIONS) !== 0x00
        ) {
            console.log("       -> LMEV (Separate edge sequence):");
            console.log("          . H1: " + to);
            console.log("          . H2: " + from);
            //from.startingVertex.debugColor = new ColorRgb(1, 0, 1);
        }

        const id = _PolyhedralBoundedSolidSetClassifier.nextVertexId(inSolidA, inSolidB);

        PolyhedralBoundedSolidEulerOperators.lmev(s, to, from, id, to!.startingVertex.position);

        if (
            (debugFlags & _PolyhedralBoundedSolidSetClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !== 0x00 &&
            (debugFlags & _PolyhedralBoundedSolidSetClassifier.DEBUG_99_SHOW_OPERATIONS) !== 0x00
        ) {
            console.log("          . New vertex: " + id);
        }

        if (type === 0) {
            const edge = new _PolyhedralBoundedSolidSetOperatorNullEdge(from!.previous()!.parentEdge!);
            ctx.sonea!.add(edge);
            tracePipelineSummary(
                "emit " +
                    traceContext +
                    " separateEdgeSequence sourceFrom=" +
                    _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(from) +
                    " sourceTo=" +
                    _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(to) +
                    " inserted=" +
                    _PolyhedralBoundedSolidSetClassifier.summarizeNullEdge(edge) +
                    " soneaSize=" +
                    ctx.sonea!.size() +
                    " sonebSize=" +
                    ctx.soneb!.size(),
            );
        } else {
            const edge = new _PolyhedralBoundedSolidSetOperatorNullEdge(from!.previous()!.parentEdge!);
            ctx.soneb!.add(edge);
            tracePipelineSummary(
                "emit " +
                    traceContext +
                    " separateEdgeSequence sourceFrom=" +
                    _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(from) +
                    " sourceTo=" +
                    _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(to) +
                    " inserted=" +
                    _PolyhedralBoundedSolidSetClassifier.summarizeNullEdge(edge) +
                    " soneaSize=" +
                    ctx.sonea!.size() +
                    " sonebSize=" +
                    ctx.soneb!.size(),
            );
        }
    }

    /**
    Inserts a null-edge strut for the coplanar V/V case (Program [MANT1988].15.12)
    and, when `orient` is `false`, swaps rightHalf/leftHalf so the
    null-edge points toward the open (OUT) side of the boundary per table 15.3.
    The orientation flip is required when the half-edge approaches the shared
    edge from the side that would otherwise produce an inward-facing null-edge.
    @param he half-edge whose starting vertex receives the new strut vertex
    @param type 0 = emitting to sonea (solidA side), 1 = soneb (solidB side)
    @param orient true when the null-edge natural direction is already correct
    @param traceContext caller label for pipeline-summary diagnostics
    */
    private static flipNullEdgeOrientationForOpenSide(
        ctx: _SetOperationContext,
        he: _PolyhedralBoundedSolidHalfEdge,
        type: number,
        orient: boolean,
        traceContext: string,
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
    ): void {
        const debugFlags = _PolyhedralBoundedSolidSetClassifier.debugFlags;
        if ((debugFlags & _PolyhedralBoundedSolidSetClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !== 0x00) {
            console.log("      SEPARATEINTERIOR " + type);
            console.log("        From/To: " + he);
        }

        let tmp: _PolyhedralBoundedSolidHalfEdge | null;

        const id = _PolyhedralBoundedSolidSetClassifier.nextVertexId(inSolidA, inSolidB);
        PolyhedralBoundedSolidEulerOperators.lmev(
            he.parentLoop.parentFace.parentSolid,
            he,
            he,
            id,
            he.startingVertex.position,
        );

        if (
            (debugFlags & _PolyhedralBoundedSolidSetClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !== 0x00 &&
            (debugFlags & _PolyhedralBoundedSolidSetClassifier.DEBUG_99_SHOW_OPERATIONS) !== 0x00
        ) {
            console.log("          . New vertex: " + id);
            //he.startingVertex.debugColor = new ColorRgb(0, 1, 1);
        }

        // A piece of Black Art: reverse orientation of the null edge
        if (!orient) {
            tmp = he.previous()!.parentEdge!.rightHalf;
            he.previous()!.parentEdge!.rightHalf = he.previous()!.parentEdge!.leftHalf;
            he.previous()!.parentEdge!.leftHalf = tmp;
        }

        if (type === 0) {
            const edge = new _PolyhedralBoundedSolidSetOperatorNullEdge(he.previous()!.parentEdge!);
            ctx.sonea!.add(edge);
            tracePipelineSummary(
                "emit " +
                    traceContext +
                    " flipNullEdgeOrientationForOpenSide orient=" +
                    orient +
                    " source=" +
                    _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(he) +
                    " inserted=" +
                    _PolyhedralBoundedSolidSetClassifier.summarizeNullEdge(edge) +
                    " soneaSize=" +
                    ctx.sonea!.size() +
                    " sonebSize=" +
                    ctx.soneb!.size(),
            );
        } else {
            const edge = new _PolyhedralBoundedSolidSetOperatorNullEdge(he.previous()!.parentEdge!);
            ctx.soneb!.add(edge);
            tracePipelineSummary(
                "emit " +
                    traceContext +
                    " flipNullEdgeOrientationForOpenSide orient=" +
                    orient +
                    " source=" +
                    _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(he) +
                    " inserted=" +
                    _PolyhedralBoundedSolidSetClassifier.summarizeNullEdge(edge) +
                    " soneaSize=" +
                    ctx.sonea!.size() +
                    " sonebSize=" +
                    ctx.soneb!.size(),
            );
        }
    }

    /**
    Borrowed from [.wMANT2008].
    */
    private static nulledge(he: _PolyhedralBoundedSolidHalfEdge): boolean {
        return PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
            he.startingVertex.position,
            he.next()!.startingVertex.position,
            _PolyhedralBoundedSolidOperator.numericContext,
        );
    }

    /**
    Borrowed from [.wMANT2008].
    */
    private static strutnulledge(he: _PolyhedralBoundedSolidHalfEdge): boolean {
        if (he === he.mirrorHalfEdge()!.next() || he === he.mirrorHalfEdge()!.previous()) {
            return true;
        }
        return false;
    }

    /**
    Borrowed from [.wMANT2008].
    */
    private static convexedg(he: _PolyhedralBoundedSolidHalfEdge): boolean {
        let h2: _PolyhedralBoundedSolidHalfEdge;

        h2 = he.next()!;
        if (_PolyhedralBoundedSolidSetClassifier.nulledge(he)) {
            h2 = h2.next()!;
        }
        const dir = h2.startingVertex.position.subtract(he.startingVertex.position);
        const cr = he.parentLoop.parentFace
            .getContainingPlane()!
            .getNormal()
            .crossProduct(he.mirrorHalfEdge()!.parentLoop.parentFace.getContainingPlane()!.getNormal());
        if (cr.length() < _PolyhedralBoundedSolidOperator.numericContext.unitVectorTolerance()) {
            return true;
        }
        return dir.dotProduct(cr) < 0.0;
    }

    /**
    Borrowed from [.wMANT2008].
    */
    private static sectorwide(he: _PolyhedralBoundedSolidHalfEdge, _ind: number): boolean {
        return _PolyhedralBoundedSolidOperator.checkWideness(he);
    }

    /**
    Borrowed from [.wMANT2008].
    */
    private static getOrientation(
        ref: _PolyhedralBoundedSolidHalfEdge,
        he1: _PolyhedralBoundedSolidHalfEdge,
        he2: _PolyhedralBoundedSolidHalfEdge,
    ): boolean {
        let retcode = false;

        const mhe1 = he1.mirrorHalfEdge()!.next()!;
        const mhe2 = he2.mirrorHalfEdge()!.next()!;
        if (mhe1 !== he2 && mhe2 === he1) {
            retcode = _PolyhedralBoundedSolidSetClassifier.convexedg(he2);
        } else {
            retcode = _PolyhedralBoundedSolidSetClassifier.convexedg(he1);
        }
        if (
            _PolyhedralBoundedSolidSetClassifier.sectorwide(mhe1, 0) &&
            _PolyhedralBoundedSolidSetClassifier.sectorwide(ref, 0)
        ) {
            retcode = !retcode;
        }

        return !retcode;
    }

    private static sectorContainsAState(
        sector: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector,
        wantedState: number,
    ): boolean {
        return sector.s1a === wantedState || sector.s2a === wantedState;
    }

    private static sectorContainsBState(
        sector: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector,
        wantedState: number,
    ): boolean {
        return sector.s1b === wantedState || sector.s2b === wantedState;
    }

    private static selectMissingEndpoint(
        data: VertexVertexClassificationData,
        fromSolidA: boolean,
        wantedState: number,
        avoid: _PolyhedralBoundedSolidHalfEdge | null,
    ): _PolyhedralBoundedSolidHalfEdge | null {
        let fallback: _PolyhedralBoundedSolidHalfEdge | null;
        let i: number;

        fallback = null;
        for (i = 0; i < data.sectors.size(); i++) {
            let candidate: _PolyhedralBoundedSolidHalfEdge | null;
            let containsState: boolean;

            const sector = data.sectors.get(i);
            if (!sector.intersect) {
                continue;
            }

            if (fromSolidA) {
                containsState = _PolyhedralBoundedSolidSetClassifier.sectorContainsAState(sector, wantedState);
                candidate = data.nba.get(sector.secta).he;
            } else {
                containsState = _PolyhedralBoundedSolidSetClassifier.sectorContainsBState(sector, wantedState);
                candidate = data.nbb.get(sector.sectb).he;
            }

            if (!containsState) {
                continue;
            }

            if (fallback === null) {
                fallback = candidate;
            }

            if (avoid !== null && candidate !== avoid) {
                return candidate;
            }

            if (avoid === null) {
                return candidate;
            }
        }

        return fallback;
    }

    private static recoverMissingCoplanarEndpoints(
        data: VertexVertexClassificationData,
        ha1: _PolyhedralBoundedSolidHalfEdge | null,
        ha2: _PolyhedralBoundedSolidHalfEdge | null,
        hb1: _PolyhedralBoundedSolidHalfEdge | null,
        hb2: _PolyhedralBoundedSolidHalfEdge | null,
    ): (_PolyhedralBoundedSolidHalfEdge | null)[] {
        traceCoplanarTangential(
            "recover endpoints before ha1=" +
                _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(ha1) +
                " ha2=" +
                _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(ha2) +
                " hb1=" +
                _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(hb1) +
                " hb2=" +
                _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(hb2),
        );
        if (ha1 === null) {
            ha1 = _PolyhedralBoundedSolidSetClassifier.selectMissingEndpoint(data, true, OnSector.OUT, ha2);
        }
        if (ha2 === null) {
            ha2 = _PolyhedralBoundedSolidSetClassifier.selectMissingEndpoint(data, true, OnSector.IN, ha1);
        }
        if (hb1 === null) {
            hb1 = _PolyhedralBoundedSolidSetClassifier.selectMissingEndpoint(data, false, OnSector.IN, hb2);
        }
        if (hb2 === null) {
            hb2 = _PolyhedralBoundedSolidSetClassifier.selectMissingEndpoint(data, false, OnSector.OUT, hb1);
        }

        traceCoplanarTangential(
            "recover endpoints after ha1=" +
                _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(ha1) +
                " ha2=" +
                _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(ha2) +
                " hb1=" +
                _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(hb1) +
                " hb2=" +
                _PolyhedralBoundedSolidSetClassifier.summarizeHalfEdge(hb2),
        );

        return [ha1, ha2, hb1, hb2];
    }

    /**
    Following section [MANT1988].15.6.2. and program [MANT1988].15.11.
    */
    private static vertexVertexInsertNullEdges(
        ctx: _SetOperationContext,
        data: VertexVertexClassificationData,
        op: number,
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
    ): void {
        let ha1: _PolyhedralBoundedSolidHalfEdge | null = null;
        let ha2: _PolyhedralBoundedSolidHalfEdge | null = null;
        let hb1: _PolyhedralBoundedSolidHalfEdge | null = null;
        let hb2: _PolyhedralBoundedSolidHalfEdge | null = null;
        let i: number;
        const debugFlags = _PolyhedralBoundedSolidSetClassifier.debugFlags;

        if ((debugFlags & _PolyhedralBoundedSolidSetClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !== 0x00) {
            console.log("   - Null edges insertion:");
        }

        let count = 0;

        for (i = 0; i < data.sectors.size(); i++) {
            if (data.sectors.get(i).intersect) count++;
        }

        if (count === 0 && data.sectors.size() > 0) {
            ha1 = data.nba.get(data.sectors.get(0).secta).he;
            hb1 = data.nbb.get(data.sectors.get(0).sectb).he;
            //System.out.println("**** EMPTY CASE");
        }

        traceCoplanarTangential(
            "vv insert null edges intersectCount=" + count + " totalSectors=" + data.sectors.size(),
        );

        i = 0;
        while (true) {
            let traceContext: string;
            //-------------------------------------------------------------
            if (i >= data.sectors.size()) {
                return;
            }
            while (!data.sectors.get(i).intersect) {
                i++;
                if (i === data.sectors.size()) {
                    return;
                }
            }
            if (data.sectors.get(i).s1a === OnSector.OUT) {
                ha1 = data.nba.get(data.sectors.get(i).secta).he;
            } else {
                ha2 = data.nba.get(data.sectors.get(i).secta).he;
            }
            if (data.sectors.get(i).s1b === OnSector.IN) {
                hb1 = data.nbb.get(data.sectors.get(i).sectb).he;
                i++;
            } else {
                hb2 = data.nbb.get(data.sectors.get(i).sectb).he;
                i++;
            }

            //-------------------------------------------------------------
            if (i >= data.sectors.size()) {
                return;
            }
            while (!data.sectors.get(i).intersect) {
                i++;
                if (i === data.sectors.size()) {
                    return;
                }
            }
            if (data.sectors.get(i).s1a === OnSector.OUT) {
                ha1 = data.nba.get(data.sectors.get(i).secta).he;
            } else {
                ha2 = data.nba.get(data.sectors.get(i).secta).he;
            }
            if (data.sectors.get(i).s1b === OnSector.IN) {
                hb1 = data.nbb.get(data.sectors.get(i).sectb).he;
                i++;
            } else {
                hb2 = data.nbb.get(data.sectors.get(i).sectb).he;
                i++;
            }

            //-------------------------------------------------------------
            if ((debugFlags & _PolyhedralBoundedSolidSetClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !== 0x00) {
                console.log("    . Deciding case:");
                console.log("      -> Ha1: " + ha1);
                console.log("      -> Ha2: " + ha2);
                console.log("      -> Hb1: " + hb1);
                console.log("      -> Hb2: " + hb2);
            }

            //-------------------------------------------------------------
            if (ha1 === null || ha2 === null || hb1 === null || hb2 === null) {
                let j: number;

                for (j = 0; j < data.sectors.size(); j++) {
                    data.sectors.get(j).intersect = false;
                }
                if ((debugFlags & _PolyhedralBoundedSolidSetClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !== 0x00) {
                    console.log("    . Incomplete coplanar pairing, skipping split");
                }
                return;
            }

            //-------------------------------------------------------------
            if (ha1 === ha2) {
                _PolyhedralBoundedSolidSetClassifier.traceVertexVertexDecisionReason(
                    op,
                    i,
                    "STRUT_A",
                    "ha1==ha2 after sector accumulation",
                    ha1,
                    ha2,
                    hb1,
                    hb2,
                );
                traceContext = _PolyhedralBoundedSolidSetClassifier.formatVertexVertexTraceContext(
                    op,
                    i,
                    "STRUT_A",
                    "separate",
                    0,
                );
                if ((debugFlags & _PolyhedralBoundedSolidSetClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !== 0x00) {
                    console.log("    . STRUT A CASE");
                }
                _PolyhedralBoundedSolidSetClassifier.flipNullEdgeOrientationForOpenSide(
                    ctx,
                    ha1,
                    0,
                    _PolyhedralBoundedSolidSetClassifier.getOrientation(ha1, hb1, hb2),
                    traceContext,
                    inSolidA,
                    inSolidB,
                );
                _PolyhedralBoundedSolidSetClassifier.separateEdgeSequence(
                    ctx,
                    hb1,
                    hb2,
                    1,
                    _PolyhedralBoundedSolidSetClassifier.formatVertexVertexTraceContext(
                        op,
                        i,
                        "STRUT_A",
                        "separate",
                        1,
                    ),
                    inSolidA,
                    inSolidB,
                );
            } else if (hb1 === hb2) {
                _PolyhedralBoundedSolidSetClassifier.traceVertexVertexDecisionReason(
                    op,
                    i,
                    "STRUT_B",
                    "hb1==hb2 after sector accumulation",
                    ha1,
                    ha2,
                    hb1,
                    hb2,
                );
                traceContext = _PolyhedralBoundedSolidSetClassifier.formatVertexVertexTraceContext(
                    op,
                    i,
                    "STRUT_B",
                    "separate",
                    1,
                );
                if ((debugFlags & _PolyhedralBoundedSolidSetClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !== 0x00) {
                    console.log("    . STRUT B CASE");
                }
                _PolyhedralBoundedSolidSetClassifier.flipNullEdgeOrientationForOpenSide(
                    ctx,
                    hb1,
                    1,
                    _PolyhedralBoundedSolidSetClassifier.getOrientation(hb1, ha2, ha1),
                    traceContext,
                    inSolidA,
                    inSolidB,
                );
                _PolyhedralBoundedSolidSetClassifier.separateEdgeSequence(
                    ctx,
                    ha2,
                    ha1,
                    0,
                    _PolyhedralBoundedSolidSetClassifier.formatVertexVertexTraceContext(
                        op,
                        i,
                        "STRUT_B",
                        "separate",
                        0,
                    ),
                    inSolidA,
                    inSolidB,
                );
            } else {
                _PolyhedralBoundedSolidSetClassifier.traceVertexVertexDecisionReason(
                    op,
                    i,
                    "PARALLEL",
                    "ha1!=ha2 and hb1!=hb2",
                    ha1,
                    ha2,
                    hb1,
                    hb2,
                );
                if ((debugFlags & _PolyhedralBoundedSolidSetClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !== 0x00) {
                    console.log("    . PARALLEL CASE");
                }
                _PolyhedralBoundedSolidSetClassifier.separateEdgeSequence(
                    ctx,
                    ha2,
                    ha1,
                    0,
                    _PolyhedralBoundedSolidSetClassifier.formatVertexVertexTraceContext(
                        op,
                        i,
                        "PARALLEL",
                        "separate",
                        0,
                    ),
                    inSolidA,
                    inSolidB,
                );
                _PolyhedralBoundedSolidSetClassifier.separateEdgeSequence(
                    ctx,
                    hb1,
                    hb2,
                    1,
                    _PolyhedralBoundedSolidSetClassifier.formatVertexVertexTraceContext(
                        op,
                        i,
                        "PARALLEL",
                        "separate",
                        1,
                    ),
                    inSolidA,
                    inSolidB,
                );
            }
            if (i === data.sectors.size()) {
                return;
            }
        }
    }

    /**
    Vertex/Vertex classifier for the set operations algorithm (big phase 2).
    Following program [MANT1988].15.6. Similar in structure to program
    [MANT1988].14.3.
    */
    private static vertexVertexClassify(
        ctx: _SetOperationContext,
        va: _PolyhedralBoundedSolidVertex,
        vb: _PolyhedralBoundedSolidVertex,
        op: number,
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
    ): void {
        const data = new _PolyhedralBoundedSolidSetVertexVertexClassifier().classify(
            va,
            vb,
            op,
            _PolyhedralBoundedSolidSetClassifier.debugFlags,
        );
        _PolyhedralBoundedSolidSetClassifier.vertexVertexInsertNullEdges(ctx, data, op, inSolidA, inSolidB);
    }

    /**
    Main control algorithm for the big phases 1 and 2. This calls the
    classifiers for vertex/face and vertex/vertex coincidences found on
    `setOpGenerate`.
    Following section [MANT1988].16.6.1. and program [MANT1988].15.5.
    */
    private static setOpClassify(
        ctx: _SetOperationContext,
        op: number,
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
    ): void {
        let i: number;
        const debugFlags = _PolyhedralBoundedSolidSetClassifier.debugFlags;

        if ((debugFlags & _PolyhedralBoundedSolidSetClassifier.DEBUG_01_STRUCTURE) !== 0x00) {
            console.log(
                "- 1.A. ----------------------------------------------------------------------------------------------------------------------------------------------------",
            );
            console.log("VERTICES OF {A} TOUCHING FACES ON {B} (sonva array of " + ctx.sonva!.size() + " matches)");
        }

        for (i = 0; i < ctx.sonva!.size(); i++) {
            new _PolyhedralBoundedSolidSetVertexFaceClassifier().classify(
                ctx.sonva!.get(i).v!,
                ctx.sonva!.get(i).f!,
                op,
                0,
                debugFlags,
                ctx.sonea!,
                ctx.soneb!,
                inSolidA,
                inSolidB,
            );
        }

        if ((debugFlags & _PolyhedralBoundedSolidSetClassifier.DEBUG_01_STRUCTURE) !== 0x00) {
            console.log(
                "- 1.B. ----------------------------------------------------------------------------------------------------------------------------------------------------",
            );
            console.log("VERTICES OF {B} TOUCHING FACES ON {A} (sonvb array of " + ctx.sonvb!.size() + " matches):");
        }

        for (i = 0; i < ctx.sonvb!.size(); i++) {
            new _PolyhedralBoundedSolidSetVertexFaceClassifier().classify(
                ctx.sonvb!.get(i).v!,
                ctx.sonvb!.get(i).f!,
                op,
                1,
                debugFlags,
                ctx.sonea!,
                ctx.soneb!,
                inSolidA,
                inSolidB,
            );
        }

        if ((debugFlags & _PolyhedralBoundedSolidSetClassifier.DEBUG_01_STRUCTURE) !== 0x00) {
            console.log(
                "- 2. ------------------------------------------------------------------------------------------------------------------------------------------------------",
            );
            console.log("VERTEX-VERTEX PAIRS (sonvv array of " + ctx.sonvv!.size() + " pairs):");
        }

        for (i = 0; i < ctx.sonvv!.size(); i++) {
            _PolyhedralBoundedSolidSetClassifier.vertexVertexClassify(
                ctx,
                ctx.sonvv!.get(i).va!,
                ctx.sonvv!.get(i).vb!,
                op,
                inSolidA,
                inSolidB,
            );
        }
    }
}
