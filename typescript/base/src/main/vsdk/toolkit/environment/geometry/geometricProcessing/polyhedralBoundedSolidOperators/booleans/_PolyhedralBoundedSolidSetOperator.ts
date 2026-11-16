//= References:                                                             =
//= [MANT1986] Mantyla Martti. "Boolean Operations of 2-Manifolds through   =
//=     Vertex Neighborhood Classification". ACM Transactions on Graphics,  =
//=     Vol. 5, No. 1, January 1986, pp. 1-29.                              =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =
//= [.wMANT2008] Mantyla Martti. "Personal Home Page", <<shar>> archive     =
//=     containing the C programs from [MANT1988]. Available at             =
//=     http://www.cs.hut.fi/~mam . Last visited April 12 / 2008.           =

// Java classes
import { Double } from "../../../../../../../java/lang/Double.js";
import { StringBuilder } from "../../../../../../../java/lang/StringBuilder.js";
import { ArrayList } from "../../../../../../../java/util/ArrayList.js";
import { HashSet } from "../../../../../../../java/util/HashSet.js";

// VitralSDK classes
import { VSDK } from "../../../../../common/VSDK.js";
import { Logger } from "../../../../../common/logging/Logger.js";
import type { Vector3Dd } from "../../../../../common/linealAlgebra/Vector3Dd.js";
import { Vector3Dd as Vector3DdClass } from "../../../../../common/linealAlgebra/Vector3Dd.js";
import { PolyhedralBoundedSolid } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidStructuralCopy } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidStructuralCopy.js";
import { PolyhedralBoundedSolidGeometricValidator } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.js";
import { PolyhedralBoundedSolidNumericPolicy } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import { PolyhedralBoundedSolidTopologyEditing } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologyEditing.js";
import { PolyhedralBoundedSolidTopologySummary } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.js";
import { PolyhedralBoundedSolidValidationEngine } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import type { _PolyhedralBoundedSolidEdge } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.js";
import type { _PolyhedralBoundedSolidFace } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { _PolyhedralBoundedSolidLoop } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";
import { PolyhedralBoundedSolidEulerOperators } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import { _PolyhedralBoundedSolidOperator } from "../_PolyhedralBoundedSolidOperator.js";
import { InvalidOperandException } from "../InvalidOperandException.js";
import { InvalidSolidResultException } from "../InvalidSolidResultException.js";
import { NonManifoldBooleanResultException } from "../NonManifoldBooleanResultException.js";
import { SolidOperationFailedException } from "../SolidOperationFailedException.js";
import { _PolyhedralBoundedSolidIdNamespace } from "../fallbacks/_PolyhedralBoundedSolidIdNamespace.js";
import { _PolyhedralBoundedSolidSetOperandAligner } from "./_PolyhedralBoundedSolidSetOperandAligner.js";
import { _SetOperationContext } from "./_SetOperationContext.js";
import { _SetOperationTrace } from "./_SetOperationTrace.js";
import { _PolyhedralBoundedSolidSetClassifier } from "./classification/_PolyhedralBoundedSolidSetClassifier.js";
import { _PolyhedralBoundedSolidSetNonIntersectingClassifier } from "./classification/_PolyhedralBoundedSolidSetNonIntersectingClassifier.js";
import type { _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace } from "./classification/_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.js";
import type { _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex } from "./classification/_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex.js";
import type { _PolyhedralBoundedSolidSetOperatorVertexFace } from "./classification/_PolyhedralBoundedSolidSetOperatorVertexFace.js";
import { _PolyhedralBoundedSolidSetGeometricPredicateProcessor } from "./intersection/_PolyhedralBoundedSolidSetGeometricPredicateProcessor.js";
import { _PolyhedralBoundedSolidSetIntersector } from "./intersection/_PolyhedralBoundedSolidSetIntersector.js";
import { _PolyhedralBoundedSolidSetFinisher } from "./topology/_PolyhedralBoundedSolidSetFinisher.js";
import { _PolyhedralBoundedSolidSetNullEdgesConnector } from "./topology/_PolyhedralBoundedSolidSetNullEdgesConnector.js";
import { _PolyhedralBoundedSolidSetOperatorNullEdge } from "./topology/_PolyhedralBoundedSolidSetOperatorNullEdge.js";

const isPipelineSummaryTraceEnabled = (): boolean => _SetOperationTrace.isPipelineSummaryTraceEnabled();
const tracePipelineSummary = (message: string): void => _SetOperationTrace.tracePipelineSummary(message);

/**
Java's `System.identityHashCode` has no direct equivalent on this runtime;
identity keys are handed out lazily from this table so that the
`separateEdgeSequence` cycle detector keeps comparing object identities (and
not values), exactly as the Java code does.
*/
const identityHashCodes = new WeakMap<object, number>();
let nextIdentityHashCode = 1;

function identityHashCode(value: object | null): number {
    if (value === null) {
        return 0;
    }
    let code = identityHashCodes.get(value);
    if (code === undefined) {
        code = nextIdentityHashCode;
        nextIdentityHashCode++;
        identityHashCodes.set(value, code);
    }
    return code;
}

/**
Optional callback used to export internal state in graphical form.
*/
export interface DebugSolidExporter {
    export(solid: PolyhedralBoundedSolid, pattern: string): void;
}

/**
Outcome of the endpoint-pairing recovery loop in `separateEdgeSequence`.
Distinguishes successful pairing from each failure mode so the caller can
report or react specifically instead of relying on a generic fatal log.
*/
export enum SeparateEdgeSequenceResult {
    OK,
    FAILED_NULL_INPUT,
    FAILED_DIFFERENT_SOLIDS,
    FAILED_CYCLE_DETECTED,
    FAILED_NO_PAIRING_REACHED,
}

/**
This class encapsulates the set operations algorithms for boundary
representation solids in VitralSDK. Basically, this class implements the
original algorithm published in the paper [MANT1986] and in the second
part of the book [MANT1988].
The algorithm is structured in 5 big phases:
  0. Calculate vertex/face and vertex/vertex crossings.
  1. Classify and split for vertex/face cases.
  2. Classify and split for vertex/vertex cases.
  3. Connect.
  4. Finish.
Note that each big phase is controlled in a method (mark as "big phase" in
its documentation).
*/
export class _PolyhedralBoundedSolidSetOperator extends _PolyhedralBoundedSolidOperator {
    /**
    Debug flags.
    */
    private static readonly DEBUG_01_STRUCTURE = 0x01;
    private static readonly DEBUG_02_GENERATOR = 0x02;
    private static readonly DEBUG_03_VERTEXFACECLASIFFIER = 0x04;
    private static readonly DEBUG_04_VERTEXVERTEXCLASIFFIER = 0x08;
    private static readonly DEBUG_05_CONNECT = 0x10;
    private static readonly DEBUG_06_FINISH = 0x20;
    private static readonly DEBUG_99_SHOWOPERATIONS = 0x40;
    private static debugFlags = 0;

    private static debugSolidExporter: DebugSolidExporter | null = null;

    public static setDebugSolidExporter(exporter: DebugSolidExporter | null): void {
        _PolyhedralBoundedSolidSetOperator.debugSolidExporter = exporter;
    }

    // Retained as a god-class delegation surface for the reflection-based
    // PolyhedralBoundedSolidSetOperatorCoplanarPredicateTest; the production
    // call sites use _PolyhedralBoundedSolidSetGeometricPredicateProcessor
    // directly. The sibling compareToZero/pointInFace/resolveCoplanarVertexVertexClass
    // wrappers were removed in Stage 7 R3 (no remaining callers).
    private static classifyCoplanarSectorRelation(
        sectorInfo: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace,
        referenceFace: _PolyhedralBoundedSolidFace,
    ): number {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.classifyCoplanarSectorRelation(
            sectorInfo,
            referenceFace,
        );
    }

    // The Mantyla son* globals ([MANT1988].15.1: sonvv, sonva, sonvb, sonea,
    // soneb, sonfa, sonfb) are no longer static fields; they live in a
    // per-invocation _SetOperationContext threaded through the pipeline
    // (Stage 7 R5).

    /**
    Procedure `updmaxnames` functionality is described on section
    [MANT1988].15.4. This method increments the face and vertex
    identifiers of `solidToUpdate` so that they do not overlap with
    `referenceSolid` identifiers.
    */
    public static updmaxnames(solidToUpdate: PolyhedralBoundedSolid, referenceSolid: PolyhedralBoundedSolid): void {
        _PolyhedralBoundedSolidIdNamespace.updmaxnames(solidToUpdate, referenceSolid);
    }

    private static nextVertexId(current: PolyhedralBoundedSolid, other: PolyhedralBoundedSolid): number {
        return _PolyhedralBoundedSolidIdNamespace.nextVertexId(
            current,
            other,
            _PolyhedralBoundedSolidOperator.idNamespace,
        );
    }

    /**
    Initial vertex intersection detector for the set operations algorithm
    (big phase 0).
    Following program [MANT1988].15.2.
    After generation, coincident intersection vertices are welded in both
    solids and stale entries are pruned from sonva/sonvb.
    */
    private static setOpGenerate(
        ctx: _SetOperationContext,
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
    ): void {
        const generation = _PolyhedralBoundedSolidSetIntersector.setOpGenerate(inSolidA, inSolidB);
        ctx.sonvv = generation.sonvv();
        ctx.sonva = generation.sonva();
        ctx.sonvb = generation.sonvb();

        _PolyhedralBoundedSolidSetOperator.weldIntersectionVertices(ctx, inSolidA, inSolidB);
    }

    /**
    Post-Generate weld pass: collapses spatially coincident vertices introduced
    during setOpGenerate in each solid, then removes from sonva/sonvb any entry
    whose vertex was merged away by lkev.  This prevents duplicate-position
    vertices from propagating into the Classify and Connect phases.
    */
    private static weldIntersectionVertices(
        ctx: _SetOperationContext,
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
    ): void {
        const weldedA = PolyhedralBoundedSolidTopologyEditing.weldCoincidentVertices(
            inSolidA,
            _PolyhedralBoundedSolidOperator.numericContext,
        );
        const weldedB = PolyhedralBoundedSolidTopologyEditing.weldCoincidentVertices(
            inSolidB,
            _PolyhedralBoundedSolidOperator.numericContext,
        );

        if (weldedA > 0) {
            Logger.reportMessage(
                null,
                VSDK.DEBUG,
                "weldIntersectionVertices",
                "setOpGenerate weld: " + weldedA + " vertex pair(s) collapsed in solidA",
            );
            _PolyhedralBoundedSolidSetOperator.pruneStaleVertexFaceEntries(ctx.sonva!, inSolidA);
        }
        if (weldedB > 0) {
            Logger.reportMessage(
                null,
                VSDK.DEBUG,
                "weldIntersectionVertices",
                "setOpGenerate weld: " + weldedB + " vertex pair(s) collapsed in solidB",
            );
            _PolyhedralBoundedSolidSetOperator.pruneStaleVertexFaceEntries(ctx.sonvb!, inSolidB);
        }
    }

    /**
    Removes entries from `list` whose vertex is no longer present in
    `solid`.  After lkev merges two vertices the removed vertex object
    is detached from the solid's verticesList; any sonva/sonvb entry still
    pointing to it would reference a dangling node.
    */
    private static pruneStaleVertexFaceEntries(
        list: ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace>,
        solid: PolyhedralBoundedSolid,
    ): void {
        let i: number;

        i = 0;
        while (i < list.size()) {
            const entry = list.get(i);
            if (!solid.getVerticesList().locateWindowAtElem(entry.v!)) {
                list.remove(i);
            } else {
                i++;
            }
        }
    }

    /**
    Constructs a vector along the bisector of the sector defined by `he`.
    that points inward the he's containing face.
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

    /**
    Checks if two coplanar sectors overlaps, by doing a "sector within" test
    for coplanar sectors: If the two given sectors are coplanar and with
    overlaping faces:
      - If sectors only intersects in one point returns false.
      - If sectors intersects on a line or area returns true.

    Following section [MANT1988].15.6.2. Note that this operation is not
    elaborated on [MANT1988], but left as an excercise.

    PRE: Given sectors are "coplanar".
    */
    private static sectoroverlap(
        na: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex,
        nb: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex,
    ): boolean {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.sectoroverlap(
            na,
            nb,
            (_PolyhedralBoundedSolidSetOperator.debugFlags &
                _PolyhedralBoundedSolidSetOperator.DEBUG_04_VERTEXVERTEXCLASIFFIER) !==
                0,
        );
    }

    public static colinearVectorsWithDirection(a: Vector3Dd, b: Vector3Dd): boolean {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.colinearVectorsWithDirection(a, b);
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
            !_PolyhedralBoundedSolidSetOperator.nulledge(prev) ||
            !_PolyhedralBoundedSolidSetOperator.strutnulledge(prev)
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
    Following program [MANT1988].15.12. Adapts the wMANT2008 recovery
    extensions for null-edge endpoints (cases A-E) using strict cycle
    detection — every iteration must produce an unseen (from, to)
    configuration, otherwise we abort and report
    `SeparateEdgeSequenceResult.FAILED_CYCLE_DETECTED`. This is the
    convergence proof requested in plan-csg-boolean-fix-stage2 §5.3:
    progress is measured as "new configurations visited", which is bounded
    by the (finite) product of half-edges in both loops, so the loop
    necessarily terminates.

    @return diagnostic result; `SeparateEdgeSequenceResult.OK` only
        when `from` and `to` share starting vertex and the LMEV
        split has been applied. Any other value indicates the LMEV was
        skipped to avoid corrupting the B-rep.
    */
    public static separateEdgeSequence(
        from: _PolyhedralBoundedSolidHalfEdge | null,
        to: _PolyhedralBoundedSolidHalfEdge | null,
        type: number,
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
    ): SeparateEdgeSequenceResult {
        const debugFlags = _PolyhedralBoundedSolidSetOperator.debugFlags;
        //-----------------------------------------------------------------
        if ((debugFlags & _PolyhedralBoundedSolidSetOperator.DEBUG_04_VERTEXVERTEXCLASIFFIER) !== 0x00) {
            console.log("      SEPARATEEDGESEQUENCE " + type);
            console.log("        From: " + from);
            console.log("        To: " + to);
        }

        if (from === null || to === null) {
            Logger.reportMessage(
                null,
                VSDK.WARNING,
                "separateEdgeSequence",
                "Unexpected case: null halfedges; skipping LMEV.",
            );
            return SeparateEdgeSequenceResult.FAILED_NULL_INPUT;
        }

        const s = from.parentLoop.parentFace.parentSolid;

        if (s !== to.parentLoop.parentFace.parentSolid) {
            Logger.reportMessage(
                null,
                VSDK.WARNING,
                "separateEdgeSequence",
                "Unexpected case: halfedges on different solids; skipping LMEV.",
            );
            return SeparateEdgeSequenceResult.FAILED_DIFFERENT_SOLIDS;
        }

        //-----------------------------------------------------------------
        // Recover from null edges already inserted.
        // Cases A/B follow null-edge struts inserted previously; cases C/D/E
        // step backwards in the loop until the two starts coincide. Each
        // iteration must produce an unseen (from, to) pair — repeating a pair
        // proves divergence and is reported as a bug instead of looping
        // forever or aborting silently after a magic count.
        const visitedConfigurations = new HashSet<string>();
        let changed: boolean;
        do {
            const configurationKey = identityHashCode(from) + ":" + identityHashCode(to);
            if (!visitedConfigurations.add(configurationKey)) {
                Logger.reportMessage(
                    null,
                    VSDK.WARNING,
                    "separateEdgeSequence",
                    "Cycle detected in endpoint recovery (cases A-E did not converge); skipping LMEV to keep B-rep valid.",
                );
                return SeparateEdgeSequenceResult.FAILED_CYCLE_DETECTED;
            }

            changed = false;

            const recoveredFrom = _PolyhedralBoundedSolidSetOperator.recoverEdgeSequenceEndpointFromStrut(from, true);
            if (recoveredFrom !== from) {
                from = recoveredFrom;
                changed = true;
                if ((debugFlags & _PolyhedralBoundedSolidSetOperator.DEBUG_04_VERTEXVERTEXCLASIFFIER) !== 0x00) {
                    console.log("        Recovered edge sequence case A");
                }
            }

            const recoveredTo = _PolyhedralBoundedSolidSetOperator.recoverEdgeSequenceEndpointFromStrut(to, false);
            if (recoveredTo !== to) {
                to = recoveredTo;
                changed = true;
                if ((debugFlags & _PolyhedralBoundedSolidSetOperator.DEBUG_04_VERTEXVERTEXCLASIFFIER) !== 0x00) {
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
                    if ((debugFlags & _PolyhedralBoundedSolidSetOperator.DEBUG_04_VERTEXVERTEXCLASIFFIER) !== 0x00) {
                        console.log("        Recovered edge sequence case C");
                    }
                } else if (fromPrev !== null && fromPrev.startingVertex === to!.startingVertex) {
                    from = fromPrev;
                    changed = true;
                    if ((debugFlags & _PolyhedralBoundedSolidSetOperator.DEBUG_04_VERTEXVERTEXCLASIFFIER) !== 0x00) {
                        console.log("        Recovered edge sequence case D");
                    }
                } else if (toPrev !== null && toPrev.startingVertex === from!.startingVertex) {
                    to = toPrev;
                    changed = true;
                    if ((debugFlags & _PolyhedralBoundedSolidSetOperator.DEBUG_04_VERTEXVERTEXCLASIFFIER) !== 0x00) {
                        console.log("        Recovered edge sequence case E");
                    }
                }
            }
        } while (changed);

        if (from!.startingVertex !== to!.startingVertex) {
            Logger.reportMessage(
                null,
                VSDK.WARNING,
                "separateEdgeSequence",
                "Unable to recover endpoint pairing after A-E normalization; skipping LMEV.",
            );
            return SeparateEdgeSequenceResult.FAILED_NO_PAIRING_REACHED;
        }

        //-----------------------------------------------------------------
        if (
            (debugFlags & _PolyhedralBoundedSolidSetOperator.DEBUG_04_VERTEXVERTEXCLASIFFIER) !== 0x00 &&
            (debugFlags & _PolyhedralBoundedSolidSetOperator.DEBUG_99_SHOWOPERATIONS) !== 0x00
        ) {
            console.log("       -> LMEV (Separate edge sequence):");
            console.log("          . H1: " + to);
            console.log("          . H2: " + from);
            //from.startingVertex.debugColor = new ColorRgb(1, 0, 1);
        }

        const id = _PolyhedralBoundedSolidSetOperator.nextVertexId(inSolidA, inSolidB);

        PolyhedralBoundedSolidEulerOperators.lmev(s, to, from, id, to!.startingVertex.position);

        if (
            (debugFlags & _PolyhedralBoundedSolidSetOperator.DEBUG_04_VERTEXVERTEXCLASIFFIER) !== 0x00 &&
            (debugFlags & _PolyhedralBoundedSolidSetOperator.DEBUG_99_SHOWOPERATIONS) !== 0x00
        ) {
            console.log("          . New vertex: " + id);
        }

        // The live pipeline records the created null edge into the per-call
        // sonea/soneb lists from within _PolyhedralBoundedSolidSetClassifier
        // (see its own separateEdgeSequence). This god-class overload is
        // retained only to validate the cycle-detection failure modes /
        // result enum via VertexVertexEndpointRecoveryTest and is not part of
        // the live path, so it no longer touches the (now per-call) son*
        // state. Stage 7 R5.

        return SeparateEdgeSequenceResult.OK;
    }

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
        _PolyhedralBoundedSolidSetClassifier.runSetOpClassify(
            op,
            inSolidA,
            inSolidB,
            _PolyhedralBoundedSolidSetOperator.debugFlags,
            ctx,
        );
    }

    // TEMP moon-diagnostic: scans a solid for boundary loops whose vertices are
    // coincident-but-distinct (self-touching / figure-8) — the signature of a
    // broken double/cut face that Generate produces on a concave cap. Gated
    // behind the pipeline-summary trace property; no effect in normal runs.
    private static traceSelfTouchingLoops(solid: PolyhedralBoundedSolid | null, label: string): void {
        if (!_SetOperationTrace.isPipelineSummaryTraceEnabled()) {
            return;
        }
        if (solid === null || solid.getPolygonsList() === null) {
            return;
        }
        const tol = PolyhedralBoundedSolidNumericPolicy.forSolid(solid);
        let fi: number;
        for (fi = 0; fi < solid.getPolygonsList().size(); fi++) {
            const face = solid.getPolygonsList().get(fi)!;
            let li: number;
            for (li = 0; li < face.boundariesList.size(); li++) {
                const loop = face.boundariesList.get(li)!;
                const sz = loop.halfEdgesList.size();
                let a: number;
                for (a = 0; a < sz; a++) {
                    let b: number;
                    for (b = a + 1; b < sz; b++) {
                        const ha = loop.halfEdgesList.get(a)!;
                        const hb = loop.halfEdgesList.get(b)!;
                        if (
                            ha.startingVertex.id !== hb.startingVertex.id &&
                            PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                                ha.startingVertex.position,
                                hb.startingVertex.position,
                                tol,
                            )
                        ) {
                            const gap = b - a;
                            const adjacent = gap === 1 || gap === sz - 1;
                            console.log(
                                "[SelfTouch] " +
                                    label +
                                    " face=" +
                                    face.id +
                                    " loop=" +
                                    li +
                                    " size=" +
                                    sz +
                                    " idx[" +
                                    a +
                                    "]v" +
                                    ha.startingVertex.id +
                                    "==idx[" +
                                    b +
                                    "]v" +
                                    hb.startingVertex.id +
                                    (adjacent ? " ADJACENT(zero-len-edge)" : " NON-ADJACENT(pinch)") +
                                    " at (" +
                                    ha.startingVertex.position.x().toFixed(4) +
                                    "," +
                                    ha.startingVertex.position.y().toFixed(4) +
                                    "," +
                                    ha.startingVertex.position.z().toFixed(4) +
                                    ")",
                            );
                        }
                    }
                }
            }
        }
    }

    /**
    Splits every boundary loop that is self-touching (pinched) — i.e. has two
    non-adjacent half-edges whose start vertices are geometrically coincident —
    into simple loops via `lmef`.

    <p>This is run on each operand right after `setOpGenerate` and before
    `setOpClassify`. At that point, the only coincident-vertex pairs that
    exist in real boundary loops (size&gt;2) are genuine pinches introduced by
    the intersector on concave faces (e.g. a crescent-shaped cap). Size-2 strut
    loops (the normal null-edge form) are deliberately excluded.</p>

    <p>For a pinch at loop positions `a` and `b`, `lmef(he_a, he_b)`
    splits the loop into two simple loops. If the original loop had
    multiple pinches, the outer restartable scan re-examines the face until all
    pinches are resolved. This handles both nested and interleaved pinch pairs.</p>

    <p>Tolerance: uses the pipeline's `numericContext` (scaled
    `bigEpsilon`), consistent with other coincidence tests.</p>
    */
    private static splitSelfTouchingLoops(solid: PolyhedralBoundedSolid | null): void {
        if (solid === null || solid.getPolygonsList() === null) {
            return;
        }
        const tracing = isPipelineSummaryTraceEnabled();
        let splitsFired = 0;
        let fi = 0;
        while (fi < solid.getPolygonsList().size()) {
            const face = solid.getPolygonsList().get(fi)!;
            let splitDone = false;
            let li = 0;
            while (li < face.boundariesList.size() && !splitDone) {
                const loop = face.boundariesList.get(li)!;
                const sz = loop.halfEdgesList.size();
                if (sz <= 2) {
                    li++;
                    continue;
                }
                let a: number;
                for (a = 0; a < sz && !splitDone; a++) {
                    let b: number;
                    for (b = a + 2; b < sz; b++) {
                        if (a === 0 && b === sz - 1) {
                            continue; // adjacent via wrap-around
                        }
                        const ha = loop.halfEdgesList.get(a)!;
                        const hb = loop.halfEdgesList.get(b)!;
                        if (
                            ha.startingVertex.id !== hb.startingVertex.id &&
                            PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                                ha.startingVertex.position,
                                hb.startingVertex.position,
                                _PolyhedralBoundedSolidOperator.numericContext,
                            )
                        ) {
                            const newId =
                                _PolyhedralBoundedSolidOperator.idNamespace !== null
                                    ? _PolyhedralBoundedSolidOperator.idNamespace.nextFaceId(solid)
                                    : solid.getMaxFaceId() + 1;
                            const newFace = PolyhedralBoundedSolidEulerOperators.lmef(solid, ha, hb, newId);
                            if (newFace !== null) {
                                splitsFired++;
                                if (tracing) {
                                    tracePipelineSummary(
                                        "splitSelfTouchingLoops #" +
                                            splitsFired +
                                            " face=" +
                                            face.id +
                                            " loop=" +
                                            li +
                                            " sz=" +
                                            sz +
                                            " idx[" +
                                            a +
                                            "]v" +
                                            ha.startingVertex.id +
                                            "==idx[" +
                                            b +
                                            "]v" +
                                            hb.startingVertex.id +
                                            " -> newFace=" +
                                            newFace.id,
                                    );
                                }
                                splitDone = true;
                            }
                            break;
                        }
                    }
                }
                if (!splitDone) {
                    li++;
                }
            }
            if (!splitDone) {
                fi++;
            }
            // If splitDone: stay at the same fi so the modified face is
            // re-examined for any remaining pinches.
        }
        if (tracing && splitsFired > 0) {
            tracePipelineSummary(
                "splitSelfTouchingLoops total=" + splitsFired + " faces-after=" + solid.getPolygonsList().size(),
            );
        }
    }

    private static setOpConnect(ctx: _SetOperationContext, op: number): void {
        const result = new _PolyhedralBoundedSolidSetNullEdgesConnector().connect(
            op,
            _PolyhedralBoundedSolidSetOperator.debugFlags,
            ctx.sonea!,
            ctx.soneb!,
        );
        ctx.sonfa = result.sonfa();
        ctx.sonfb = result.sonfb();
        ctx.connected = result;
    }

    /**
    Answer integrator for the set operations algorithm (big phase 4).
    Following program [MANT1988].15.15.
    */
    private static setOpFinish(
        ctx: _SetOperationContext,
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
        outRes: PolyhedralBoundedSolid,
        op: number,
    ): void {
        _PolyhedralBoundedSolidSetFinisher.finish(
            inSolidA,
            inSolidB,
            outRes,
            op,
            _PolyhedralBoundedSolidSetOperator.debugFlags,
            ctx.sonfa!,
            ctx.sonfb!,
            ctx.connected,
        );
    }

    private static weldCoincidentVerticesOfOperand(solid: PolyhedralBoundedSolid, name: string): void {
        const context = PolyhedralBoundedSolidNumericPolicy.forSolid(solid);

        if (
            PolyhedralBoundedSolidGeometricValidator.validateNoCoincidentVertices(solid, context, new StringBuilder())
        ) {
            return;
        }
        const welded = PolyhedralBoundedSolidTopologyEditing.weldCoincidentVertices(solid, context);
        tracePipelineSummary("setOp operand " + name + " had coincident vertices; welded " + welded + " pair(s)");
    }

    private static isTouchingOnlyPreflightCase(
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
        cache?: _PolyhedralBoundedSolidSetNonIntersectingClassifier._PreflightCache,
    ): boolean {
        if (cache === undefined) {
            return _PolyhedralBoundedSolidSetNonIntersectingClassifier.runTouchingOnlyPreflightCase(inSolidA, inSolidB);
        }
        return _PolyhedralBoundedSolidSetNonIntersectingClassifier.runTouchingOnlyPreflightCase(
            inSolidA,
            inSolidB,
            cache,
        );
    }

    private static isContainmentOnlyPreflightCase(
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
        cache?: _PolyhedralBoundedSolidSetNonIntersectingClassifier._PreflightCache,
    ): boolean {
        if (cache === undefined) {
            return _PolyhedralBoundedSolidSetNonIntersectingClassifier.runContainmentOnlyPreflightCase(
                inSolidA,
                inSolidB,
            );
        }
        return _PolyhedralBoundedSolidSetNonIntersectingClassifier.runContainmentOnlyPreflightCase(
            inSolidA,
            inSolidB,
            cache,
        );
    }

    /**
    Handles no-intersection cases (book problem [MANT1988].15.1). Without a
    cache it builds a fresh classification: use that overload after the
    generate/classify stages, where the operands have already been mutated and
    the preflight memo is stale. With a cache it reuses the per-setOp preflight
    memo, only valid while the operands are still unmutated.
    */
    private static setOpNoIntersectionCase(
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
        outRes: PolyhedralBoundedSolid,
        op: number,
        cache?: _PolyhedralBoundedSolidSetNonIntersectingClassifier._PreflightCache,
    ): PolyhedralBoundedSolid {
        try {
            if (cache === undefined) {
                return _PolyhedralBoundedSolidSetNonIntersectingClassifier.runSetOpNoIntersectionCase(
                    inSolidA,
                    inSolidB,
                    outRes,
                    op,
                );
            }
            return _PolyhedralBoundedSolidSetNonIntersectingClassifier.runSetOpNoIntersectionCase(
                inSolidA,
                inSolidB,
                outRes,
                op,
                cache,
            );
        } catch (e) {
            if (e instanceof NonManifoldBooleanResultException) {
                _PolyhedralBoundedSolidSetOperator.lastResultPath =
                    _PolyhedralBoundedSolidSetOperator.REJECTED_NON_MANIFOLD_PATH;
            }
            throw e;
        }
    }

    private static debugSolid(solid: PolyhedralBoundedSolid, pattern: string): void {
        console.log("**** DEBUGGING SOLID INFORMATION WRITEN TO FILES " + pattern + " ****");
        try {
            // Runtime boundary: the Java version also writes `pattern + ".txt"`
            // with `PersistenceElement.writeAsciiLine`. The base TypeScript
            // package has no filesystem port, so the textual dump is delegated
            // to the debug exporter callback when one is installed.
            if (_PolyhedralBoundedSolidSetOperator.debugSolidExporter !== null) {
                _PolyhedralBoundedSolidSetOperator.debugSolidExporter.export(solid, pattern);
            }
        } catch (e) {
            console.log(String(e));
        }
    }

    /**
    Following program [MANT1988].15.1.
    */
    private static postProcessResult(res: PolyhedralBoundedSolid, maximizeResultFaces: boolean): void {
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(res);
        PolyhedralBoundedSolidTopologyEditing.compactIds(res);
        if (maximizeResultFaces) {
            PolyhedralBoundedSolidTopologyEditing.maximizeFaces(res);
            _PolyhedralBoundedSolidSetFinisher.triangulateNonPlanarFaces(res);
            PolyhedralBoundedSolidTopologyEditing.compactIds(res);
        }
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(res);
    }

    /**
    Name of the path that produced the last setOp result on the calling
    thread: "normal-pipeline" for the [MANT1988] Ch. 15 pipeline, or the name
    of the preflight that answered instead. Recorded before the
    strict postcondition runs, so it is also available after a strict
    validation failure. Fable5.2Analysis.md Stage A step 4.
    */
    /**
    Result path recorded when the operation is rejected with
    `NonManifoldBooleanResultException`.
    */
    public static readonly REJECTED_NON_MANIFOLD_PATH = "rejected-non-manifold-union";

    /**
    Java keeps this in a `ThreadLocal`; this runtime is single-threaded, so a
    static field holds the same per-caller value.
    */
    private static lastResultPath: string | null = null;

    public static getLastResultPath(): string | null {
        return _PolyhedralBoundedSolidSetOperator.lastResultPath;
    }

    /**
    Violations of the post-connect invariant found on the last setOp of the
    calling thread that reached connect (empty list: none; null: connect was
    not reached). See `checkConnectInvariant`. Java keeps this in a
    `ThreadLocal`; see `lastResultPath`.
    */
    private static lastConnectInvariantViolations: ArrayList<string> | null = null;

    /** @internal Java package-private. */
    public static getLastConnectInvariantViolations(): ArrayList<string> | null {
        return _PolyhedralBoundedSolidSetOperator.lastConnectInvariantViolations;
    }

    /**
    Post-connect invariant ([MANT1988] Sec. 14.7 and 15.7; Fable5.2Analysis.md
    Stage C step 1), checked when connect left no loose ends:
    - every null face recorded for finish (sonfa, sonfb) has exactly two
      loops, the two copies of its section curve;
    - no other face carries two loops that coincide vertex by vertex. A
      section ring strictly inside a face must leave that face with the
      ring as a hole and the disk as a separate face; two coincident
      loops in one face mean the hole was moved into the disk (B-01).
    */
    private static checkConnectInvariant(
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
        ctx: _SetOperationContext,
    ): ArrayList<string> {
        const violations = new ArrayList<string>();

        if (_PolyhedralBoundedSolidSetOperator.hasIncompleteConnectState()) {
            return violations;
        }
        _PolyhedralBoundedSolidSetOperator.collectNullFaceViolations("A", ctx.sonfa, violations);
        _PolyhedralBoundedSolidSetOperator.collectNullFaceViolations("B", ctx.sonfb, violations);
        _PolyhedralBoundedSolidSetOperator.collectCoincidentLoopViolations("A", inSolidA, ctx.sonfa, violations);
        _PolyhedralBoundedSolidSetOperator.collectCoincidentLoopViolations("B", inSolidB, ctx.sonfb, violations);
        return violations;
    }

    private static collectNullFaceViolations(
        label: string,
        nullFaces: ArrayList<_PolyhedralBoundedSolidFace> | null,
        violations: ArrayList<string>,
    ): void {
        let i: number;

        if (nullFaces === null) {
            return;
        }
        for (i = 0; i < nullFaces.size(); i++) {
            const f = nullFaces.get(i);
            // A null face collapsed to a single point (only zero-length
            // strut rings, left where the section curve runs straight
            // through a vertex of one face) bounds no section and is
            // discarded by finish; it is not a section-curve face.
            if (f.boundariesList.size() !== 2 && !_PolyhedralBoundedSolidSetOperator.isPointLikeFace(f)) {
                violations.add(
                    label + ": null face " + f.id + " has " + f.boundariesList.size() + " loops instead of 2",
                );
            }
        }
    }

    private static isPointLikeFace(f: _PolyhedralBoundedSolidFace): boolean {
        let first: Vector3Dd | null = null;
        let i: number;

        for (i = 0; i < f.boundariesList.size(); i++) {
            const start = f.boundariesList.get(i)!.boundaryStartHalfEdge;
            let he = start;
            if (start === null) {
                continue;
            }
            do {
                const p = he!.startingVertex.position;
                if (first === null) {
                    first = p;
                } else if (
                    Vector3DdClass.distance(first, p) > _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon()
                ) {
                    return false;
                }
                he = he!.next();
            } while (he !== start);
        }
        return true;
    }

    private static collectCoincidentLoopViolations(
        label: string,
        solid: PolyhedralBoundedSolid,
        nullFaces: ArrayList<_PolyhedralBoundedSolidFace> | null,
        violations: ArrayList<string>,
    ): void {
        let i: number;
        let j: number;
        let k: number;

        for (i = 0; i < solid.getPolygonsList().size(); i++) {
            const f = solid.getPolygonsList().get(i)!;
            if (f.boundariesList.size() < 2 || (nullFaces !== null && nullFaces.contains(f))) {
                continue;
            }
            for (j = 0; j < f.boundariesList.size(); j++) {
                for (k = j + 1; k < f.boundariesList.size(); k++) {
                    if (
                        _PolyhedralBoundedSolidSetOperator.loopsCoincide(
                            f.boundariesList.get(j)!,
                            f.boundariesList.get(k)!,
                        )
                    ) {
                        violations.add(label + ": face " + f.id + " has coincident loops " + j + " and " + k);
                    }
                }
            }
        }
    }

    private static loopsCoincide(a: _PolyhedralBoundedSolidLoop, b: _PolyhedralBoundedSolidLoop): boolean {
        let i: number;
        let j: number;

        if (a.halfEdgesList.size() !== b.halfEdgesList.size() || a.halfEdgesList.size() < 3) {
            return false;
        }
        for (i = 0; i < a.halfEdgesList.size(); i++) {
            const p = a.halfEdgesList.get(i)!.startingVertex.position;
            let found = false;
            for (j = 0; j < b.halfEdgesList.size() && !found; j++) {
                found = PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                    p,
                    b.halfEdgesList.get(j)!.startingVertex.position,
                    _PolyhedralBoundedSolidOperator.numericContext,
                );
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    /**
    Detects result pieces that touch only along an edge or at a vertex: two
    distinct vertices at the same point, or two distinct edges joining the
    same two points. A manifold boundary has neither. Returns a short
    description of the first contact found, or null.
    @internal Java package-private.
    */
    public static findNonManifoldContact(result: PolyhedralBoundedSolid): string | null {
        const tolerance = _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon();
        const nv = result.getVerticesList().size();
        const ne = result.getEdgesList().size();
        let i: number;
        let j: number;

        for (i = 0; i < nv; i++) {
            const p = result.getVerticesList().get(i)!.position;
            for (j = i + 1; j < nv; j++) {
                if (Vector3DdClass.distance(p, result.getVerticesList().get(j)!.position) <= tolerance) {
                    return (
                        "vertices " +
                        result.getVerticesList().get(i)!.id +
                        " and " +
                        result.getVerticesList().get(j)!.id +
                        " coincide at " +
                        p
                    );
                }
            }
        }
        for (i = 0; i < ne; i++) {
            const e = result.getEdgesList().get(i)!;
            for (j = i + 1; j < ne; j++) {
                const f = result.getEdgesList().get(j)!;
                if (_PolyhedralBoundedSolidSetOperator.sameEndpoints(e, f)) {
                    return (
                        "edges " +
                        e.id +
                        " and " +
                        f.id +
                        " join the same points " +
                        e.rightHalf!.startingVertex.position +
                        " - " +
                        e.leftHalf!.startingVertex.position
                    );
                }
            }
        }
        return null;
    }

    private static sameEndpoints(e: _PolyhedralBoundedSolidEdge, f: _PolyhedralBoundedSolidEdge): boolean {
        const e1 = e.rightHalf!.startingVertex;
        const e2 = e.leftHalf!.startingVertex;
        const f1 = f.rightHalf!.startingVertex;
        const f2 = f.leftHalf!.startingVertex;

        return (e1 === f1 && e2 === f2) || (e1 === f2 && e2 === f1);
    }

    private static completeSetOpResult(
        result: PolyhedralBoundedSolid | null,
        operandADiagnostics: string,
        operandBDiagnostics: string,
        op: number,
        resultPath: string,
        maximizeResultFaces: boolean,
        doStrictValidation: boolean,
    ): PolyhedralBoundedSolid {
        _PolyhedralBoundedSolidSetOperator.lastResultPath = resultPath;
        if (result === null) {
            throw new SolidOperationFailedException(
                "Boolean operation returned null: op=" +
                    _PolyhedralBoundedSolidSetOperator.operationName(op) +
                    ", path=" +
                    resultPath,
            );
        }
        if (result.getPolygonsList().size() > 0) {
            _PolyhedralBoundedSolidSetOperator.postProcessResult(result, maximizeResultFaces);
        }
        const contact = _PolyhedralBoundedSolidSetOperator.findNonManifoldContact(result);
        if (contact !== null) {
            // Same policy as B-04: pieces of the result that touch only
            // along an edge or at a vertex form a non-manifold solid.
            throw new NonManifoldBooleanResultException(
                _PolyhedralBoundedSolidSetOperator.operationName(op) +
                    " result is non-manifold (" +
                    contact +
                    "); rejected (path=" +
                    resultPath +
                    ")",
            );
        }
        if (doStrictValidation) {
            const strictMessage = new StringBuilder();
            if (!PolyhedralBoundedSolidValidationEngine.validateStrict(result, strictMessage)) {
                const topology = PolyhedralBoundedSolidTopologySummary.from(result);
                throw new InvalidSolidResultException(
                    "Strict boolean result validation failed: op=" +
                        _PolyhedralBoundedSolidSetOperator.operationName(op) +
                        ", path=" +
                        resultPath +
                        ", operandA=" +
                        operandADiagnostics +
                        ", operandB=" +
                        operandBDiagnostics +
                        ", result=" +
                        _PolyhedralBoundedSolidSetOperator.cardinalitiesAndBounds(result) +
                        ", " +
                        topology +
                        "\n" +
                        strictMessage,
                );
            }
        }
        return result;
    }

    private static operationName(op: number): string {
        if (op === _PolyhedralBoundedSolidOperator.UNION) {
            return "UNION";
        }
        if (op === _PolyhedralBoundedSolidOperator.INTERSECTION) {
            return "INTERSECTION";
        }
        if (op === _PolyhedralBoundedSolidOperator.SUBTRACT) {
            return "SUBTRACT";
        }
        return "UNKNOWN(" + op + ")";
    }

    private static cardinalitiesAndBounds(solid: PolyhedralBoundedSolid | null): string {
        if (solid === null) {
            return "null";
        }
        return (
            "{faces=" +
            solid.getPolygonsList().size() +
            ", edges=" +
            solid.getEdgesList().size() +
            ", vertices=" +
            solid.getVerticesList().size() +
            ", bounds=" +
            // Java's `Arrays.toString(double[])`, whose elements go through
            // `String.valueOf(double)`.
            "[" +
            Array.from(solid.getMinMax(), (value) => Double.toString(value)).join(", ") +
            "]" +
            "}"
        );
    }

    /**
    Independent copy of `solid` for the recovery paths. It used to be a
    Java serialization round trip, which recursed over the linked structure
    and could overflow the stack on large solids; it is now the linear
    structural copy of [MANT1988] Sec. 15.8 (`scopy`, Stage F). The label
    is kept for the callers' diagnostics.
    */
    private static deepCloneSolid(solid: PolyhedralBoundedSolid, _solidLabel: string): PolyhedralBoundedSolid {
        return PolyhedralBoundedSolidStructuralCopy.copy(solid);
    }

    private static hasIncompleteConnectState(): boolean {
        return (
            _PolyhedralBoundedSolidSetNullEdgesConnector.getLastLooseACount() > 0 ||
            _PolyhedralBoundedSolidSetNullEdgesConnector.getLastLooseBCount() > 0
        );
    }

    /**
    Following program [MANT1988].15.1. Java overloads `setOp(a, b, op)`,
    `setOp(a, b, op, withDebug)`, `setOp(a, b, op, withDebug,
    maximizeResultFaces)` and `setOp(a, b, op, withDebug,
    maximizeResultFaces, doStrictValidation)`; the shorter forms enable
    face maximization and strict validation by default.
    *
    * @throws IllegalStateException when `doStrictValidation` is true
    *     and the completed result fails strict validation
    */
    public static setOp(
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
        op: number,
        withDebug = false,
        maximizeResultFaces = true,
        doStrictValidation = true,
    ): PolyhedralBoundedSolid {
        _PolyhedralBoundedSolidSetOperator.lastResultPath = null;
        _PolyhedralBoundedSolidSetOperator.lastConnectInvariantViolations = null;
        _PolyhedralBoundedSolidOperator.setNumericContext(
            PolyhedralBoundedSolidNumericPolicy.forSolids(inSolidA, inSolidB),
        );
        _PolyhedralBoundedSolidSetOperatorNullEdge.setNumericContext(_PolyhedralBoundedSolidOperator.numericContext);

        if (withDebug) {
            _PolyhedralBoundedSolidSetOperator.debugFlags =
                0 |
                _PolyhedralBoundedSolidSetOperator.DEBUG_01_STRUCTURE |
                _PolyhedralBoundedSolidSetOperator.DEBUG_02_GENERATOR |
                _PolyhedralBoundedSolidSetOperator.DEBUG_03_VERTEXFACECLASIFFIER |
                _PolyhedralBoundedSolidSetOperator.DEBUG_04_VERTEXVERTEXCLASIFFIER |
                _PolyhedralBoundedSolidSetOperator.DEBUG_05_CONNECT |
                _PolyhedralBoundedSolidSetOperator.DEBUG_06_FINISH |
                _PolyhedralBoundedSolidSetOperator.DEBUG_99_SHOWOPERATIONS;
        } else {
            _PolyhedralBoundedSolidSetOperator.debugFlags = 0;
        }

        if (
            (_PolyhedralBoundedSolidSetOperator.debugFlags & _PolyhedralBoundedSolidSetOperator.DEBUG_01_STRUCTURE) !==
            0x00
        ) {
            console.log(
                "= [START OF SETOP REPORT] =================================================================================================================================",
            );
            console.log("Dumping debug log for _PolyhedralBoundedSolidSetOperator.setOp.");
            console.log("The algorithm structure is:");
            console.log("  0. Calculate vertex/face and vertex/vertex crossings.");
            console.log("  1. Classify and split for vertex/face cases.");
            console.log("  2. Classify and split for vertex/vertex cases.");
            console.log("  3. Connect.");
            console.log("  4. Finish.");
        }

        //-----------------------------------------------------------------
        let res: PolyhedralBoundedSolid = new PolyhedralBoundedSolid();
        let resultPath: string;
        let operandADiagnostics: string;
        let operandBDiagnostics: string;

        const ctx = new _SetOperationContext();
        ctx.sonea = new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>();
        ctx.soneb = new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>();
        resultPath = "normal-pipeline";
        operandADiagnostics = _PolyhedralBoundedSolidSetOperator.cardinalitiesAndBounds(inSolidA);
        operandBDiagnostics = _PolyhedralBoundedSolidSetOperator.cardinalitiesAndBounds(inSolidB);

        //-----------------------------------------------------------------
        if (withDebug) {
            _PolyhedralBoundedSolidSetOperator.debugSolid(inSolidA, "outputA_stage00");
            _PolyhedralBoundedSolidSetOperator.debugSolid(inSolidB, "outputB_stage00");
        }

        PolyhedralBoundedSolidTopologyEditing.compactIds(inSolidA);
        PolyhedralBoundedSolidTopologyEditing.compactIds(inSolidB);
        PolyhedralBoundedSolidTopologyEditing.maximizeFaces(inSolidA);
        PolyhedralBoundedSolidTopologyEditing.maximizeFaces(inSolidB);
        PolyhedralBoundedSolidTopologyEditing.compactIds(inSolidA);
        PolyhedralBoundedSolidTopologyEditing.compactIds(inSolidB);
        // Operand preparation, on the copies this operation owns: edges
        // shorter than the tolerance are collapsed, so that generate does
        // not meet two vertices at one point. Validation, below, only
        // reads (plan-stage11 Phase C).
        _PolyhedralBoundedSolidSetOperator.weldCoincidentVerticesOfOperand(inSolidA, "A");
        _PolyhedralBoundedSolidSetOperator.weldCoincidentVerticesOfOperand(inSolidB, "B");
        // Faces of B inside the tolerance band of a parallel face of A are
        // put on its plane, so that every stage below takes the same
        // decision about them (see the aligner).
        const alignment = _PolyhedralBoundedSolidSetOperandAligner.align(inSolidA, inSolidB);
        if (alignment !== null) {
            tracePipelineSummary("setOp operand B moved by " + alignment + " to align faces inside the tolerance band");
        }
        const booleanInputMsg = new StringBuilder();
        const validInputs = PolyhedralBoundedSolidValidationEngine.validateBooleanInputs(
            inSolidA,
            inSolidB,
            booleanInputMsg,
        );
        if (!validInputs) {
            throw new InvalidOperandException("Boolean operands are not valid solids:\n" + booleanInputMsg);
        }
        if (booleanInputMsg.length() > 0) {
            Logger.reportMessage(null, VSDK.DEBUG, "setOp", "Boolean input pre-processing:\n" + booleanInputMsg);
        }
        PolyhedralBoundedSolidTopologyEditing.compactIds(inSolidA);
        PolyhedralBoundedSolidTopologyEditing.compactIds(inSolidB);
        _PolyhedralBoundedSolidSetOperator.updmaxnames(inSolidB, inSolidA);
        _PolyhedralBoundedSolidOperator.setIdNamespace(new _PolyhedralBoundedSolidIdNamespace(inSolidA, inSolidB));
        _PolyhedralBoundedSolidOperator.setNumericContext(
            PolyhedralBoundedSolidNumericPolicy.forSolids(inSolidA, inSolidB),
        );
        _PolyhedralBoundedSolidSetOperatorNullEdge.setNumericContext(_PolyhedralBoundedSolidOperator.numericContext);
        if (withDebug) {
            _PolyhedralBoundedSolidSetOperator.debugSolid(inSolidA, "outputA_stage01");
            _PolyhedralBoundedSolidSetOperator.debugSolid(inSolidB, "outputB_stage01");
        }

        // Per-setOp memo shared across all "no real intersection" preflights:
        // operands are not mutated until setOpGenerate runs (below), so the
        // heavy classification/overlap/edge-face predicates are computed once
        // and reused. See _PreflightCache.
        const preflightCache = _PolyhedralBoundedSolidSetNonIntersectingClassifier.newPreflightCache(
            inSolidA,
            inSolidB,
        );

        // A union of solids that touch along a face is one solid, glued
        // along the AonB- faces ([MANT1988] Eq. 15.2): leave it to the
        // pipeline. Edge or vertex contact would leave two shells sharing
        // a boundary point: rejected as non-manifold (B-04).
        const unionGluesAlongAFace =
            op === _PolyhedralBoundedSolidOperator.UNION &&
            _PolyhedralBoundedSolidSetNonIntersectingClassifier.hasOppositeCoplanarFaceAreaContact(inSolidA, inSolidB);
        if (
            !unionGluesAlongAFace &&
            _PolyhedralBoundedSolidSetOperator.isTouchingOnlyPreflightCase(inSolidA, inSolidB, preflightCache)
        ) {
            res = _PolyhedralBoundedSolidSetOperator.setOpNoIntersectionCase(
                inSolidA,
                inSolidB,
                res,
                op,
                preflightCache,
            );
            return _PolyhedralBoundedSolidSetOperator.completeSetOpResult(
                res,
                operandADiagnostics,
                operandBDiagnostics,
                op,
                "touching-only-preflight",
                maximizeResultFaces,
                doStrictValidation,
            );
        }

        // §7.3.1.A — Degenerate identity preflight (algebraic identity
        // detector): when A ≡ B geometrically (e.g. tests of idempotence
        // A∪A = A on cloned operands), the classifier marks every face of
        // both as "inside the other" and the regular pipeline collapses
        // to ∅, breaking A∪A and A∩A. Detect and dispatch directly per
        // set-theoretic identity. MUST run before the containment
        // preflight (which would also accept A ≡ B but produce slightly
        // different topology via merge() instead of deepClone()).
        if (
            PolyhedralBoundedSolidValidationEngine.areGeometricallyIdentical(
                inSolidA,
                inSolidB,
                _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon(),
            )
        ) {
            tracePipelineSummary("setOp identity-preflight A≡B op=" + op);
            if (op === _PolyhedralBoundedSolidOperator.UNION || op === _PolyhedralBoundedSolidOperator.INTERSECTION) {
                res = _PolyhedralBoundedSolidSetOperator.deepCloneSolid(inSolidA, "identity-preflight clone");
                if (res === null) {
                    res = new PolyhedralBoundedSolid();
                }
            }
            // SUBTRACT case: A − A = ∅; res remains the empty PolyhedralBoundedSolid
            // already initialised at function entry.
            return _PolyhedralBoundedSolidSetOperator.completeSetOpResult(
                res,
                operandADiagnostics,
                operandBDiagnostics,
                op,
                "geometric-identity-preflight",
                maximizeResultFaces,
                doStrictValidation,
            );
        }

        // §7.3.1.D — Containment-only preflight: when one solid is
        // contained inside the other (strictly or with tangent
        // boundary) without real edge/face intersections (e.g., the
        // second step of absorption identities A ∪ (A ∩ B) where
        // A ∩ B ⊂ A), the regular pipeline produces ∅. Dispatch to a
        // dedicated containment table per Mäntylä Ch.15.1.
        // A solid inside the other and touching its boundary is not a
        // plain containment for the difference: A - B is then A with a
        // pocket open to the outside, not A with a cavity, and only the
        // pipeline builds it.
        if (
            _PolyhedralBoundedSolidSetOperator.isContainmentOnlyPreflightCase(inSolidA, inSolidB, preflightCache) &&
            !(
                op === _PolyhedralBoundedSolidOperator.SUBTRACT &&
                _PolyhedralBoundedSolidSetNonIntersectingClassifier.touchesBoundaryFromInside(inSolidB, inSolidA)
            )
        ) {
            res = _PolyhedralBoundedSolidSetOperator.setOpNoIntersectionCase(
                inSolidA,
                inSolidB,
                res,
                op,
                preflightCache,
            );
            return _PolyhedralBoundedSolidSetOperator.completeSetOpResult(
                res,
                operandADiagnostics,
                operandBDiagnostics,
                op,
                "containment-only-preflight",
                maximizeResultFaces,
                doStrictValidation,
            );
        }

        // Generate and classify edit the operands (vertices on edges and
        // faces, null edges). When they find no intersection the answer is
        // one of the operands, or their contact: it is taken from these
        // copies, made before any edit (plan-stage11 Phase E). Taken from
        // the edited operands it carried their extra vertices, and failed
        // strict validation for contacts inside the tolerance band.
        const untouchedA = PolyhedralBoundedSolidStructuralCopy.copy(inSolidA);
        const untouchedB = PolyhedralBoundedSolidStructuralCopy.copy(inSolidB);

        _PolyhedralBoundedSolidSetOperator.setOpGenerate(ctx, inSolidA, inSolidB);

        if (withDebug) {
            _PolyhedralBoundedSolidSetOperator.debugSolid(inSolidA, "outputA_stage02");
            _PolyhedralBoundedSolidSetOperator.debugSolid(inSolidB, "outputB_stage02");
        }
        _PolyhedralBoundedSolidSetOperator.traceSelfTouchingLoops(inSolidA, "A-after-generate");
        _PolyhedralBoundedSolidSetOperator.traceSelfTouchingLoops(inSolidB, "B-after-generate");
        _PolyhedralBoundedSolidSetOperator.splitSelfTouchingLoops(inSolidA);
        _PolyhedralBoundedSolidSetOperator.splitSelfTouchingLoops(inSolidB);

        _PolyhedralBoundedSolidSetOperator.setOpClassify(ctx, op, inSolidA, inSolidB);

        if (withDebug) {
            _PolyhedralBoundedSolidSetOperator.debugSolid(inSolidA, "outputA_stage03");
            _PolyhedralBoundedSolidSetOperator.debugSolid(inSolidB, "outputB_stage03");
        }
        // NOTE: after Classify the algorithm has (by design) inserted null-edge
        // struts (size-2 loops with coincident endpoints); scanning here would
        // flag those normal struts. Genuine self-touch is only meaningful
        // after Generate (above), so we deliberately do not scan post-Classify.

        // [MANT1988] Sec. 15.4: what matters is whether classification
        // produced null edges. Vertex/vertex contacts that yield none (a
        // solid tangent inside the other along whole faces, for instance)
        // leave nothing to connect, so the result follows from containment
        // as in the no-intersection case; running connect and finish on
        // them returned an empty solid.
        if (ctx.sonea!.isEmpty() && ctx.soneb!.isEmpty()) {
            // No intersections found
            res = _PolyhedralBoundedSolidSetOperator.setOpNoIntersectionCase(untouchedA, untouchedB, res, op);
            return _PolyhedralBoundedSolidSetOperator.completeSetOpResult(
                res,
                operandADiagnostics,
                operandBDiagnostics,
                op,
                "no-intersection-after-classify",
                maximizeResultFaces,
                doStrictValidation,
            );
        }

        if (withDebug) {
            _PolyhedralBoundedSolidSetOperator.debugSolid(inSolidA, "outputA_stage04");
            _PolyhedralBoundedSolidSetOperator.debugSolid(inSolidB, "outputB_stage04");
        }

        _PolyhedralBoundedSolidSetOperator.setOpConnect(ctx, op);
        _PolyhedralBoundedSolidSetOperator.lastConnectInvariantViolations =
            _PolyhedralBoundedSolidSetOperator.checkConnectInvariant(inSolidA, inSolidB, ctx);
        // [MANT1988] Program 15.14: connect ends with every null edge
        // joined. Loose ends mean that part of the intersection curve was
        // not cut: a crossing was missed or misclassified, or two ends that
        // belong together sat in different faces. The null faces found so
        // far then describe only part of the result. Finishing with them
        // returned a solid with a piece missing and no error (plan-stage11
        // Phase B, near-degenerate fuzz), or a solid with coincident,
        // unjoined vertices that the non-manifold check rejected for the
        // wrong reason.
        const looseA = _PolyhedralBoundedSolidSetNullEdgesConnector.getLastLooseACount();
        const looseB = _PolyhedralBoundedSolidSetNullEdgesConnector.getLastLooseBCount();
        const curveReport = _PolyhedralBoundedSolidSetNullEdgesConnector.getLastCurveReport();
        if (looseA > 0 || looseB > 0) {
            _PolyhedralBoundedSolidSetOperator.lastResultPath = "normal-pipeline";
            throw new SolidOperationFailedException(
                "Boolean operation could not close the intersection curve: " +
                    "op=" +
                    _PolyhedralBoundedSolidSetOperator.operationName(op) +
                    ", " +
                    looseA +
                    " loose null edge end(s) on A and " +
                    looseB +
                    " on B " +
                    "after connect (" +
                    curveReport +
                    ")",
            );
        }

        if (withDebug) {
            _PolyhedralBoundedSolidSetOperator.debugSolid(inSolidA, "outputA_stage05");
            _PolyhedralBoundedSolidSetOperator.debugSolid(inSolidB, "outputB_stage05");
        }

        // [MANT1988] Sec. 15.8: finish builds the result from the null
        // faces. Every answer comes from this pipeline; there is no
        // shape-specific fallback behind it.
        _PolyhedralBoundedSolidSetOperator.setOpFinish(ctx, inSolidA, inSolidB, res, op);

        if (withDebug) {
            _PolyhedralBoundedSolidSetOperator.debugSolid(inSolidA, "outputA_stage06");
            _PolyhedralBoundedSolidSetOperator.debugSolid(inSolidB, "outputB_stage06");
            _PolyhedralBoundedSolidSetOperator.debugSolid(res, "outputR_stage06");
        }

        res = _PolyhedralBoundedSolidSetOperator.completeSetOpResult(
            res,
            operandADiagnostics,
            operandBDiagnostics,
            op,
            resultPath,
            maximizeResultFaces,
            doStrictValidation,
        );

        if (withDebug) {
            _PolyhedralBoundedSolidSetOperator.debugSolid(res, "outputR_stage07");
        }

        if (
            (_PolyhedralBoundedSolidSetOperator.debugFlags & _PolyhedralBoundedSolidSetOperator.DEBUG_01_STRUCTURE) !==
            0x00
        ) {
            console.log(
                "= [END OF SETOP REPORT] ===================================================================================================================================",
            );
        }

        return res;
    }
}

export namespace _PolyhedralBoundedSolidSetOperator {
    export type DebugSolidExporter = import("./_PolyhedralBoundedSolidSetOperator.js").DebugSolidExporter;
}
