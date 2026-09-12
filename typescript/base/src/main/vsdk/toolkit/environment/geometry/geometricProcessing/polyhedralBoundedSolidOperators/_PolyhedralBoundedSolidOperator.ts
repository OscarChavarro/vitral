//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

// VitralSDK classes
import { Boolean as JavaBoolean } from "../../../../../../java/lang/Boolean.js";
import type { Vector3Dd } from "../../../../common/linealAlgebra/Vector3Dd.js";
import type { CircularDoubleLinkedList } from "../../../../common/dataStructures/CircularDoubleLinkedList.js";
import { PolyhedralBoundedSolidStatistics } from "../../../../common/statistics/PolyhedralBoundedSolidStatistics.js";
import { VSDK } from "../../../../common/VSDK.js";
import type { PolyhedralBoundedSolid } from "../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import {
    PolyhedralBoundedSolidNumericPolicy,
    type ToleranceContext,
} from "../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import type { _PolyhedralBoundedSolidFace } from "../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidLoop } from "../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";
import type { _PolyhedralBoundedSolidEdge } from "../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { _PolyhedralBoundedSolidVertex } from "../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";
import { PolyhedralBoundedSolidEulerOperators } from "../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import { ProcessingElement } from "../../../../processing/ProcessingElement.js";
import type { _PolyhedralBoundedSolidIdNamespace } from "./fallbacks/_PolyhedralBoundedSolidIdNamespace.js";

/**
Shared low-level services for B-Rep operators. This class is not intended to
be used directly; it provides common behavior for slicing and boolean
operators.
*/
export class _PolyhedralBoundedSolidOperator extends ProcessingElement {
    // Java initializes these from `PolyhedralBoundedSolidModeler.UNION`,
    // `INTERSECTION` and `SUBTRACT` (1, 2 and 3). The literal values avoid a
    // module initialization cycle, since the modeler imports the splitter
    // subclass of this class.
    public static readonly UNION = 1;
    public static readonly INTERSECTION = 2;
    public static readonly SUBTRACT = 3;
    public static readonly DIFFERENCE = _PolyhedralBoundedSolidOperator.SUBTRACT;

    protected static numericContext: ToleranceContext = PolyhedralBoundedSolidNumericPolicy.defaultContext();

    protected static setNumericContext(context: ToleranceContext | null): void {
        if (context === null) {
            _PolyhedralBoundedSolidOperator.numericContext = PolyhedralBoundedSolidNumericPolicy.defaultContext();
        } else {
            _PolyhedralBoundedSolidOperator.numericContext = context;
        }
    }

    protected static idNamespace: _PolyhedralBoundedSolidIdNamespace | null = null;

    protected static setIdNamespace(ns: _PolyhedralBoundedSolidIdNamespace | null): void {
        _PolyhedralBoundedSolidOperator.idNamespace = ns;
    }

    private static searchForEdge(
        l: CircularDoubleLinkedList<_PolyhedralBoundedSolidEdge>,
        e: _PolyhedralBoundedSolidEdge | null,
    ): boolean {
        let i: number;

        for (i = 0; i < l.size(); i++) {
            if (l.get(i) === e) return true;
        }
        return false;
    }

    private static searchForVertex(
        l: CircularDoubleLinkedList<_PolyhedralBoundedSolidVertex>,
        v: _PolyhedralBoundedSolidVertex,
    ): boolean {
        let i: number;

        for (i = 0; i < l.size(); i++) {
            if (l.get(i) === v) return true;
        }
        return false;
    }

    /**
    Following section [MANT1988].14.7.1 and program [MANT1988].14.8.
    */
    protected static neighbor(h1: _PolyhedralBoundedSolidHalfEdge, h2: _PolyhedralBoundedSolidHalfEdge): boolean {
        return (
            h1.parentLoop.parentFace === h2.parentLoop.parentFace &&
            ((h1 === h1.parentEdge!.rightHalf && h2 === h2.parentEdge!.leftHalf) ||
                (h1 === h1.parentEdge!.leftHalf && h2 === h2.parentEdge!.rightHalf))
        );
    }

    /**
    This is the answer to problem [MANT1988].14.2.

    \todo  Check for consistency of `emanatingHalfEdge` pointers for vertices.
    */
    protected static cleanup(s: PolyhedralBoundedSolid): void {
        let i: number;
        let j: number;
        let f: _PolyhedralBoundedSolidFace;
        let l: _PolyhedralBoundedSolidLoop;
        let he: _PolyhedralBoundedSolidHalfEdge;

        for (i = 0; i < s.getPolygonsList().size(); i++) {
            f = s.getPolygonsList().get(i)!;
            for (j = 0; j < f.boundariesList.size(); j++) {
                l = f.boundariesList.get(j)!;
                he = l.boundaryStartHalfEdge!;
                do {
                    //
                    if (!_PolyhedralBoundedSolidOperator.searchForEdge(s.getEdgesList(), he.parentEdge)) {
                        s.getEdgesList().add(he.parentEdge!);
                    }
                    if (!_PolyhedralBoundedSolidOperator.searchForVertex(s.getVerticesList(), he.startingVertex)) {
                        s.getVerticesList().add(he.startingVertex);
                        he.startingVertex.emanatingHalfEdge = he;
                    }
                    //
                    he = he.next()!;
                } while (he !== l.boundaryStartHalfEdge);
            }
        }
    }

    /**
    Following section [MANT1988].14.8. and program [MANT1988].14.12.
    */
    protected static movefac(f: _PolyhedralBoundedSolidFace, s: PolyhedralBoundedSolid): void {
        let l: _PolyhedralBoundedSolidLoop;
        let he: _PolyhedralBoundedSolidHalfEdge;
        let f2: _PolyhedralBoundedSolidFace;
        let i: number;

        if (!_PolyhedralBoundedSolidOperator.canMoveFace(f)) {
            return;
        }

        f.parentSolid.getPolygonsList().locateWindowAtElem(f);
        f.parentSolid.getPolygonsList().removeElemAtWindow();
        s.getPolygonsList().add(f);
        f.parentSolid = s;

        for (i = 0; i < f.boundariesList.size(); i++) {
            l = f.boundariesList.get(i)!;
            he = l.boundaryStartHalfEdge!;
            do {
                const mirror = he.mirrorHalfEdge();
                if (mirror === null || mirror.parentLoop === null || mirror.parentLoop.parentFace === null) {
                    he = he.next()!;
                    continue;
                }
                f2 = mirror.parentLoop.parentFace;
                if (f2.parentSolid !== s && _PolyhedralBoundedSolidOperator.canMoveFace(f2)) {
                    _PolyhedralBoundedSolidOperator.movefac(f2, s);
                }
                he = he.next()!;
            } while (he !== l.boundaryStartHalfEdge);
        }
    }

    private static canMoveFace(f: _PolyhedralBoundedSolidFace | null): boolean {
        let i: number;

        if (f === null || f.boundariesList === null) {
            return false;
        }
        for (i = 0; i < f.boundariesList.size(); i++) {
            const l = f.boundariesList.get(i);
            let he: _PolyhedralBoundedSolidHalfEdge | null;
            let guard: number;

            if (l === null || l.boundaryStartHalfEdge === null || l.halfEdgesList === null) {
                return false;
            }
            const start = l.boundaryStartHalfEdge;
            he = start;
            guard = 0;
            do {
                if (
                    he === null ||
                    he.parentEdge === null ||
                    he.parentLoop === null ||
                    he.startingVertex === null ||
                    he.mirrorHalfEdge() === null ||
                    he.next() === null
                ) {
                    return false;
                }
                he = he.next();
                guard++;
            } while (he !== start && guard <= l.halfEdgesList.size() + 1);
            if (he !== start) {
                return false;
            }
        }
        return true;
    }

    /**
    Constructs a vector along the bisector of the sector defined by `he`.
    Answer to problem [MANT1988].14.1.

    Current implementation assumes the following interpretation:
    Given a vertex of interest `he.startingVertex`, one can measure the angle
    of incidence of loop `he.parentLoop` on vertex of interest by measuring the
    angle between the halfedges `he` (direction `a`) and `he.previous`
    (direction `b`).  The bisector vector is the one having its tail on the
    vertex of interest position `he.startingVertex.position` and its end
    pointing in the middle of `a` and `b` directions.

    This is the answer to problem [MANT1988].14.1.

    \todo : check current assumptions!

    This protected method is here for exclusive use of subclasses
    `_PolyhedralBoundedSolidSplitter` and `_PolyhedralBoundedSolidSetOperator`.
    */
    protected static bisector(he: _PolyhedralBoundedSolidHalfEdge): Vector3Dd {
        let a: Vector3Dd;
        let b: Vector3Dd;

        a = he.next()!.startingVertex.position.subtract(he.startingVertex.position);
        b = he.previous()!.startingVertex.position.subtract(he.startingVertex.position);
        a = a.normalized();
        b = b.normalized();

        const middle = he.startingVertex.position.add(a.add(b).multiply(0.5));

        return middle;
    }

    /**
    Moves those rings of `f1` that do not lie within its outer loop to
    `f2`.
    This procedure is used on the splitter and set operator algorithms to
    ensure that after a face has been divided by a MEF, all loops will end up
    in the correct halves.
    This is an answer to problem [MANT1988].13.5. Its use in the context of
    the splitter algorithm is briefly described on section [MANT1988].14.7.2.
    */
    private static laringmv(f1: _PolyhedralBoundedSolidFace, f2: _PolyhedralBoundedSolidFace): void {
        let l: _PolyhedralBoundedSolidLoop;
        let i: number;

        // It is supposed to move all (internal) rings from `f1` to `f2`
        // using PolyhedralBoundedSolid.lringmv
        // Legacy rule: move every ring. A geometric two-face containment
        // rule was tried here (mythosPlan Phase 3) and regressed reference
        // flows: mid-connect loops are not simple regions (bridge edges,
        // spikes, half-built chains), so parity containment is unreliable
        // at this point of the pipeline. The shadow trace below records
        // what the geometric rule would have decided, for diagnosis only.
        for (i = 1; i < f1.boundariesList.size(); i++) {
            l = f1.boundariesList.get(i)!;
            if (JavaBoolean.getBoolean("vsdk.setop.tracePipelineSummary")) {
                _PolyhedralBoundedSolidOperator.traceRingMoveShadowDecision(
                    f1,
                    f2,
                    l,
                    "laringmv MOVE wouldMove=" + _PolyhedralBoundedSolidOperator.ringBelongsToOtherHalf(f1, f2, l),
                );
            }
            if (PolyhedralBoundedSolidEulerOperators.lringmv(f1.parentSolid, l, f2, false)) {
                i--;
            }
        }
    }

    /**
    Decides on which side of a face division a pending ring belongs
    ([MANT1988].13.5, completed): after a {@code lmef} divides a face into
    `f1` (keeping the old outer loop side) and `f2`, a ring belongs to the
    half whose region geometrically contains its representative point.
    Containment is evaluated by 2D parity against each half's outer loop on
    the dominant projection plane. The three observed regimes (mythosPlan
    Phase 3 shadow study):
    <ul>
    <li>Chord division (regions disjoint): exactly one parity test is true
        — the ring follows it. This keeps cusp-region strut rings with
        their junction partners instead of stranding them.</li>
    <li>Interior closed curve (one region nested in the other): both tests
        are true for rings in the nested region; the nested half wins
        (decided by testing one vertex of f2's outer loop against f1's).</li>
    <li>Ring on the dividing boundary (zero clearance: the common
        single-chord pending strut): both tests are unreliable/false —
        the legacy behavior (move to `f2`) is preserved.</li>
    </ul>
    @param f1 face that kept the original outer loop side
    @param f2 face created by the division
    @param l pending ring currently parented by {@code f1}
    @return true when the ring must move to {@code f2}
    */
    private static ringBelongsToOtherHalf(
        f1: _PolyhedralBoundedSolidFace | null,
        f2: _PolyhedralBoundedSolidFace | null,
        l: _PolyhedralBoundedSolidLoop | null,
    ): boolean {
        if (
            f1 === null ||
            f2 === null ||
            l === null ||
            l.boundaryStartHalfEdge === null ||
            l.boundaryStartHalfEdge.startingVertex === null
        ) {
            return true;
        }
        const point = l.boundaryStartHalfEdge.startingVertex.position;
        const insideF1 = _PolyhedralBoundedSolidOperator.outerLoopParityContains(f1, point);
        const insideF2 = _PolyhedralBoundedSolidOperator.outerLoopParityContains(f2, point);
        if (insideF1 === null || insideF2 === null) {
            return true;
        }
        if (insideF1 !== insideF2) {
            return insideF2;
        }
        if (insideF1) {
            // Nested halves: the ring belongs to the inner region. Decide
            // which outer loop is nested by testing a representative vertex
            // of f2's outer loop against f1's outer loop.
            const f2Outer = f2.boundariesList.size() > 0 ? f2.boundariesList.get(0) : null;
            if (
                f2Outer === null ||
                f2Outer.boundaryStartHalfEdge === null ||
                f2Outer.boundaryStartHalfEdge.startingVertex === null
            ) {
                return true;
            }
            const f2NestedInF1 = _PolyhedralBoundedSolidOperator.outerLoopParityContains(
                f1,
                f2Outer.boundaryStartHalfEdge.startingVertex.position,
            );
            if (f2NestedInF1 === null) {
                return true;
            }
            return f2NestedInF1;
        }
        // Outside both (on the dividing boundary or degenerate): legacy.
        return true;
    }

    /**
    2D parity (ray crossing) containment of a point against the outer loop
    of a face, on the dominant projection plane of the face normal.
    @param f face whose outer loop is tested
    @param point point to classify
    @return TRUE/FALSE parity result, or null when not computable
    */
    private static outerLoopParityContains(
        f: _PolyhedralBoundedSolidFace | null,
        point: Vector3Dd | null,
    ): boolean | null {
        if (f === null || point === null || f.boundariesList.size() === 0 || f.getContainingPlane() === null) {
            return null;
        }
        const outerLoop = f.boundariesList.get(0);
        if (outerLoop === null || outerLoop.boundaryStartHalfEdge === null) {
            return null;
        }

        const normal = f.getContainingPlane()!.getNormal();
        const ax = Math.abs(normal.x());
        const ay = Math.abs(normal.y());
        const az = Math.abs(normal.z());
        let dropAxis: number;
        if (ax >= ay && ax >= az) {
            dropAxis = 0;
        } else if (ay >= ax && ay >= az) {
            dropAxis = 1;
        } else {
            dropAxis = 2;
        }

        const pu = _PolyhedralBoundedSolidOperator.shadowProjectedU(point, dropAxis);
        const pv = _PolyhedralBoundedSolidOperator.shadowProjectedV(point, dropAxis);
        let inside = false;
        let guard = 0;
        const start = outerLoop.boundaryStartHalfEdge;
        let he: _PolyhedralBoundedSolidHalfEdge = start;
        do {
            const next: _PolyhedralBoundedSolidHalfEdge | null = he.next();
            if (next === null || he.startingVertex === null || next.startingVertex === null) {
                return null;
            }
            const au = _PolyhedralBoundedSolidOperator.shadowProjectedU(he.startingVertex.position, dropAxis);
            const av = _PolyhedralBoundedSolidOperator.shadowProjectedV(he.startingVertex.position, dropAxis);
            const bu = _PolyhedralBoundedSolidOperator.shadowProjectedU(next.startingVertex.position, dropAxis);
            const bv = _PolyhedralBoundedSolidOperator.shadowProjectedV(next.startingVertex.position, dropAxis);
            if (av > pv !== bv > pv) {
                const crossingU = ((bu - au) * (pv - av)) / (bv - av) + au;
                if (pu < crossingU) {
                    inside = !inside;
                }
            }
            he = next;
            guard++;
        } while (he !== start && guard < 100000);
        return inside;
    }

    /**
    Diagnostic shadow for the ring redistribution decision (mythosPlan
    Phase 3): computes — without changing behavior — whether the ring's
    representative point lies inside `f1`'s outer loop (2D parity on the
    dominant projection plane) and its clearance from the outer-loop edges,
    then logs one line when the pipeline trace property is set. The legacy
    behavior (move every ring to `f2`) is preserved by the caller; this
    trace exists to map where that behavior is load-bearing versus where it
    mis-parents pending null-edge strut rings (crescent-cusp faces).
    @param f1 face whose rings are being redistributed
    @param f2 destination face of the legacy unconditional move
    @param l ring about to be moved
    */
    private static traceRingMoveShadowDecision(
        f1: _PolyhedralBoundedSolidFace | null,
        f2: _PolyhedralBoundedSolidFace | null,
        l: _PolyhedralBoundedSolidLoop | null,
        site: string,
    ): void {
        if (!JavaBoolean.getBoolean("vsdk.setop.tracePipelineSummary")) {
            return;
        }
        if (
            f1 === null ||
            f2 === null ||
            l === null ||
            f1.boundariesList.size() === 0 ||
            l.boundaryStartHalfEdge === null ||
            l.boundaryStartHalfEdge.startingVertex === null ||
            f1.getContainingPlane() === null
        ) {
            console.log(
                "[LARINGMV] f1=" +
                    (f1 === null ? "?" : f1.id) +
                    " f2=" +
                    (f2 === null ? "?" : f2.id) +
                    " ring=untestable site=" +
                    site,
            );
            return;
        }
        const outerLoop = f1.boundariesList.get(0)!;
        if (outerLoop === l || outerLoop.boundaryStartHalfEdge === null) {
            console.log("[LARINGMV] f1=" + f1.id + " f2=" + f2.id + " ring=outer? site=" + site);
            return;
        }

        const normal = f1.getContainingPlane()!.getNormal();
        const ax = Math.abs(normal.x());
        const ay = Math.abs(normal.y());
        const az = Math.abs(normal.z());
        let dropAxis: number;
        if (ax >= ay && ax >= az) {
            dropAxis = 0;
        } else if (ay >= ax && ay >= az) {
            dropAxis = 1;
        } else {
            dropAxis = 2;
        }

        const point = l.boundaryStartHalfEdge.startingVertex.position;
        const pu = _PolyhedralBoundedSolidOperator.shadowProjectedU(point, dropAxis);
        const pv = _PolyhedralBoundedSolidOperator.shadowProjectedV(point, dropAxis);

        let inside = false;
        let minClearance = Number.MAX_VALUE;
        let outerSize = 0;
        const start = outerLoop.boundaryStartHalfEdge;
        let he: _PolyhedralBoundedSolidHalfEdge = start;
        let walkable = true;
        do {
            const next: _PolyhedralBoundedSolidHalfEdge | null = he.next();
            if (next === null || he.startingVertex === null || next.startingVertex === null) {
                walkable = false;
                break;
            }
            const au = _PolyhedralBoundedSolidOperator.shadowProjectedU(he.startingVertex.position, dropAxis);
            const av = _PolyhedralBoundedSolidOperator.shadowProjectedV(he.startingVertex.position, dropAxis);
            const bu = _PolyhedralBoundedSolidOperator.shadowProjectedU(next.startingVertex.position, dropAxis);
            const bv = _PolyhedralBoundedSolidOperator.shadowProjectedV(next.startingVertex.position, dropAxis);

            const eu = bu - au;
            const ev = bv - av;
            const lenSq = eu * eu + ev * ev;
            let t = 0.0;
            if (lenSq > 0.0) {
                t = ((pu - au) * eu + (pv - av) * ev) / lenSq;
                if (t < 0.0) {
                    t = 0.0;
                } else if (t > 1.0) {
                    t = 1.0;
                }
            }
            const du = pu - (au + eu * t);
            const dv = pv - (av + ev * t);
            const clearance = Math.sqrt(du * du + dv * dv);
            if (clearance < minClearance) {
                minClearance = clearance;
            }

            if (av > pv !== bv > pv) {
                const crossingU = ((bu - au) * (pv - av)) / (bv - av) + au;
                if (pu < crossingU) {
                    inside = !inside;
                }
            }
            outerSize++;
            he = next;
        } while (he !== start && outerSize < 100000);

        const ringSize = l.halfEdgesList === null ? -1 : l.halfEdgesList.size();
        console.log(
            "[LARINGMV] f1=" +
                f1.id +
                " f2=" +
                f2.id +
                " ringV=" +
                l.boundaryStartHalfEdge.startingVertex.id +
                " ringSize=" +
                ringSize +
                " outerSize=" +
                outerSize +
                " walkable=" +
                walkable +
                " inside=" +
                inside +
                " clearance=" +
                minClearance.toFixed(6) +
                " p=" +
                point +
                " site=" +
                site,
        );
    }

    private static shadowProjectedU(p: Vector3Dd, dropAxis: number): number {
        if (dropAxis === 0) {
            return p.y();
        }
        return p.x();
    }

    private static shadowProjectedV(p: Vector3Dd, dropAxis: number): number {
        if (dropAxis === 1 || dropAxis === 0) {
            return p.z();
        }
        return p.y();
    }

    /**
    Following section [MANT1988].14.7.2. and program [MANT1988].14.10.

    Java overloads `join(h1, h2, withDebug)` (ring moves allowed) and
    `join(h1, h2, withDebug, allowRingMove)`.
    */
    protected static join(
        h1: _PolyhedralBoundedSolidHalfEdge,
        h2: _PolyhedralBoundedSolidHalfEdge,
        withDebug: boolean,
    ): void;
    protected static join(
        h1: _PolyhedralBoundedSolidHalfEdge,
        h2: _PolyhedralBoundedSolidHalfEdge,
        withDebug: boolean,
        allowRingMove: boolean,
    ): void;
    protected static join(
        h1: _PolyhedralBoundedSolidHalfEdge,
        h2: _PolyhedralBoundedSolidHalfEdge,
        withDebug: boolean,
        allowRingMove?: boolean,
    ): void {
        if (allowRingMove === undefined) {
            _PolyhedralBoundedSolidOperator.join(h1, h2, withDebug, true);
            return;
        }
        PolyhedralBoundedSolidStatistics.recordJoinCall();
        let newf: _PolyhedralBoundedSolidFace | null;

        if (withDebug) {
            console.log("       -> JOIN:");
            console.log("          . H1: " + h1);
            console.log("          . H2: " + h2);
        }

        const oldf = h1.parentLoop.parentFace;
        newf = null;
        const s = oldf.parentSolid;
        if (h1.parentLoop === h2.parentLoop) {
            if (h1.previous()!.previous() !== h2) {
                const fid1 =
                    _PolyhedralBoundedSolidOperator.idNamespace !== null
                        ? _PolyhedralBoundedSolidOperator.idNamespace.nextFaceId(s)
                        : s.getMaxFaceId() + 1;
                newf = PolyhedralBoundedSolidEulerOperators.lmef(s, h1, h2.next(), fid1);
                if (withDebug) {
                    //h1.next().parentEdge.debugColor = new ColorRgb(1, 0, 0);
                }
            }
        } else {
            PolyhedralBoundedSolidEulerOperators.lmekr(s, h1, h2.next()!);
            if (withDebug) {
                //h1.next().parentEdge.debugColor = new ColorRgb(0, 1, 0);
            }
        }

        if (h1.next()!.next() !== h2) {
            const fid2 =
                _PolyhedralBoundedSolidOperator.idNamespace !== null
                    ? _PolyhedralBoundedSolidOperator.idNamespace.nextFaceId(s)
                    : s.getMaxFaceId() + 1;
            // Shadow diagnostics (mythosPlan Phase 3): capture which face
            // the second division splits and the face it creates, so the
            // rings that are never redistributed across this division stay
            // observable. Applying laringmv here was tried and regressed
            // MOON_BLOCK reference cases — behavior stays legacy.
            const splitFace2 = h2.parentLoop.parentFace;
            const newFace2 = PolyhedralBoundedSolidEulerOperators.lmef(s, h2, h1.next(), fid2);
            if (withDebug) {
                //h2.next().parentEdge.debugColor = new ColorRgb(0, 0, 1);
            }
            if (
                JavaBoolean.getBoolean("vsdk.setop.tracePipelineSummary") &&
                splitFace2 !== null &&
                splitFace2.boundariesList.size() >= 2
            ) {
                let shadowI: number;
                for (shadowI = 1; shadowI < splitFace2.boundariesList.size(); shadowI++) {
                    _PolyhedralBoundedSolidOperator.traceRingMoveShadowDecision(
                        splitFace2,
                        newFace2,
                        splitFace2.boundariesList.get(shadowI),
                        "lmef2 KEEP wouldMove=" +
                            _PolyhedralBoundedSolidOperator.ringBelongsToOtherHalf(
                                splitFace2,
                                newFace2,
                                splitFace2.boundariesList.get(shadowI),
                            ),
                    );
                }
            }
            if (newf !== null && oldf.boundariesList.size() >= 2) {
                if (allowRingMove) {
                    _PolyhedralBoundedSolidOperator.laringmv(oldf, newf);
                }
            }
        }
    }

    /**
    This method checks whether the edges `he.previous().parentEdge` and
    `he.parentEdge` make a convex (less than 180 degrees) or concave
    (larger than 180 degrees) angle. In the first case the method returns
    `false` and `true` for the second case.
    This is an answer to problem [MANT1988].13.6.
    Current implementation intentionally follows the legacy boolean-kernel
    predicate: the sector is wide when the cross product of its two boundary
    vectors is degenerate or points opposite to the parent face normal.

    PRE: Parent solid should be previously validated to contain correct
    face equations.

    This protected method is here for exclusive use of subclasses
    `_PolyhedralBoundedSolidSplitter` and `_PolyhedralBoundedSolidSetOperator`.
    */
    protected static checkWideness(he: _PolyhedralBoundedSolidHalfEdge | null): boolean {
        if (
            he === null ||
            he.parentLoop === null ||
            he.parentLoop.parentFace === null ||
            he.parentLoop.parentFace.getContainingPlane() === null ||
            he.previous() === null ||
            he.next() === null
        ) {
            return true;
        }

        const ref1 = he.previous()!.startingVertex.position.subtract(he.startingVertex.position);
        const ref2 = he.next()!.startingVertex.position.subtract(he.startingVertex.position);
        const ref12 = ref1.crossProduct(ref2);
        if (ref12.length() < VSDK.EPSILON) {
            return true;
        }
        return ref12.dotProduct(he.parentLoop.parentFace.getContainingPlane()!.getNormal()) <= 0.0;
    }
}
