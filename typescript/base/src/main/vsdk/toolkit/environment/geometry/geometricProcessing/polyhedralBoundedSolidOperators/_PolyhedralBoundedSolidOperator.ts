//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

// VitralSDK classes
import { Boolean as JavaBoolean } from "../../../../../../java/lang/Boolean.js";
import { Double } from "../../../../../../java/lang/Double.js";
import type { Vector3Dd } from "../../../../common/linealAlgebra/Vector3Dd.js";
import type { InfinitePlane } from "../../surface/InfinitePlane.js";
import type { CircularArrayList } from "../../../../common/dataStructures/CircularArrayList.js";
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
        l: CircularArrayList<_PolyhedralBoundedSolidEdge>,
        e: _PolyhedralBoundedSolidEdge | null,
    ): boolean {
        let i: number;

        for (i = 0; i < l.size(); i++) {
            if (l.get(i) === e) return true;
        }
        return false;
    }

    private static searchForVertex(
        l: CircularArrayList<_PolyhedralBoundedSolidVertex>,
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
    private static laringmv(f1: _PolyhedralBoundedSolidFace, f2: _PolyhedralBoundedSolidFace): void;
    /**
    As {@link #laringmv(_PolyhedralBoundedSolidFace, _PolyhedralBoundedSolidFace)}
    but never moves `keep`. `join` passes the remainder of an inner ring it
    has just divided: that remainder is the hole the section polygon leaves
    in `f1`, while the divided-off part became the outer loop of `f2` (the
    disk). Both coincide geometrically, so no containment test can tell them
    apart; topology does (Fable5.2Analysis.md defect B-01).
    */
    private static laringmv(
        f1: _PolyhedralBoundedSolidFace,
        f2: _PolyhedralBoundedSolidFace,
        keep: _PolyhedralBoundedSolidLoop | null,
    ): void;
    private static laringmv(
        f1: _PolyhedralBoundedSolidFace,
        f2: _PolyhedralBoundedSolidFace,
        keep: _PolyhedralBoundedSolidLoop | null = null,
    ): void {
        let l: _PolyhedralBoundedSolidLoop;
        let i: number;

        // [MANT1988].13.5 moves the rings that are not inside the outer loop
        // of `f1`. A ring clearly inside `f1` only (clearRingSide) stays;
        // every other ring follows the legacy rule and moves to `f2`. A
        // containment test for every ring was tried (mythosPlan Phase 3) and
        // regressed reference flows: mid-connect loops are not simple
        // regions and pending strut rings usually sit on the dividing chord,
        // so only unambiguous geometry overrides the legacy rule.
        const sides = new _RingSideTest(f1, f2);
        for (i = 1; i < f1.boundariesList.size(); i++) {
            l = f1.boundariesList.get(i)!;
            if (l === keep) {
                continue;
            }
            if (JavaBoolean.getBoolean("vsdk.setop.tracePipelineSummary")) {
                _PolyhedralBoundedSolidOperator.traceRingMoveShadowDecision(
                    f1,
                    f2,
                    l,
                    "laringmv MOVE wouldMove=" + _PolyhedralBoundedSolidOperator.ringBelongsToOtherHalf(f1, f2, l),
                );
            }
            if (sides.side(l) === 1) {
                continue;
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
    Side of a face division on which a ring clearly lies: 1 when its
    representative point is inside the outer loop of `f1` only, 2 when
    inside that of `f2` only, both times farther than the tolerance from
    either outer loop; 0 otherwise (ambiguous: the caller keeps its legacy
    rule). A ring clearly inside one region cannot belong to the other,
    whatever the shape of the loops in the middle of the connect stage.
    */
    private static clearRingSide(
        f1: _PolyhedralBoundedSolidFace | null,
        f2: _PolyhedralBoundedSolidFace | null,
        l: _PolyhedralBoundedSolidLoop | null,
    ): number {
        if (f1 === null || f2 === null) {
            return 0;
        }
        return new _RingSideTest(f1, f2).side(l);
    }

    /** Distance from `point` to the outer loop of `f` (3D segments). */
    private static outerLoopClearance(f: _PolyhedralBoundedSolidFace, point: Vector3Dd): number {
        const outer = f.boundariesList.get(0)!;
        const start = outer.boundaryStartHalfEdge;
        if (start === null) {
            return 0.0;
        }
        let best = Double.MAX_VALUE;
        let he: _PolyhedralBoundedSolidHalfEdge = start;
        let guard = 0;
        do {
            const next = he.next();
            if (next === null) {
                return 0.0;
            }
            const a = he.startingVertex.position;
            const ab = next.startingVertex.position.subtract(a);
            const len2 = ab.dotProduct(ab);
            let t = len2 > 0.0 ? point.subtract(a).dotProduct(ab) / len2 : 0.0;
            t = Math.max(0.0, Math.min(1.0, t));
            best = Math.min(best, point.subtract(a.add(ab.multiply(t))).length());
            he = next;
            guard++;
        } while (he !== start && guard < 100000);
        return best;
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

    /** @internal Private in Java; read by the nested `_OuterLoopShadow`. */
    public static shadowProjectedU(p: Vector3Dd, dropAxis: number): number {
        if (dropAxis === 0) {
            return p.y();
        }
        return p.x();
    }

    /** @internal Private in Java; read by the nested `_OuterLoopShadow`. */
    public static shadowProjectedV(p: Vector3Dd, dropAxis: number): number {
        if (dropAxis === 1 || dropAxis === 0) {
            return p.z();
        }
        return p.y();
    }

    /**
    After `join` divides an inner ring of `oldf` with `lmef`, one of the two
    coincident copies of the section polygon must stay in `oldf` as its hole
    and the other must be the outer loop of `newf` (the disk). `lmef` puts
    the h1 side in `newf` whatever its winding, so when the remainder left in
    `oldf` is wound as a disk and `newf`'s outer loop is wound as a hole, the
    two loops are exchanged. Returns the loop that is now the hole of `oldf`
    (and must not be moved by laringmv), or null when the division does not
    have that shape (Fable5.2Analysis.md defect B-01).
    */
    private static holeLeftByDividedRing(
        oldf: _PolyhedralBoundedSolidFace,
        newf: _PolyhedralBoundedSolidFace | null,
        dividedInnerRing: _PolyhedralBoundedSolidLoop | null,
    ): _PolyhedralBoundedSolidLoop | null {
        if (
            dividedInnerRing === null ||
            newf === null ||
            dividedInnerRing.parentFace !== oldf ||
            newf.boundariesList.size() < 1
        ) {
            return null;
        }
        const newOuter = newf.boundariesList.get(0)!;
        if (JavaBoolean.getBoolean("vsdk.setop.tracePipelineSummary")) {
            console.log(
                "[RINGDIV] oldf=" +
                    oldf.id +
                    " newf=" +
                    newf.id +
                    " remainder=" +
                    dividedInnerRing.halfEdgesList.size() +
                    " dot=" +
                    Double.toString(_PolyhedralBoundedSolidOperator.windingDot(dividedInnerRing, oldf)) +
                    " newfLoops=" +
                    newf.boundariesList.size() +
                    " newOuter=" +
                    newOuter.halfEdgesList.size() +
                    " dotVsOldf=" +
                    Double.toString(_PolyhedralBoundedSolidOperator.windingDot(newOuter, oldf)),
            );
        }
        if (_PolyhedralBoundedSolidOperator.isWoundAsHole(dividedInnerRing, oldf)) {
            return dividedInnerRing;
        }
        // Both copies are measured against oldf's normal: newf's own plane
        // follows whatever winding its outer loop got, so it cannot tell.
        if (
            newf.boundariesList.size() !== 1 ||
            !_PolyhedralBoundedSolidOperator.isWoundAsHole(newOuter, oldf) ||
            !_PolyhedralBoundedSolidOperator.isWoundAsDisk(dividedInnerRing, oldf)
        ) {
            return null;
        }
        const s = oldf.parentSolid;
        // newf keeps one loop only while the exchange happens: first move the
        // disk in as outer loop, then send the hole piece to oldf.
        if (!PolyhedralBoundedSolidEulerOperators.lringmv(s, dividedInnerRing, newf, true)) {
            return null;
        }
        if (!PolyhedralBoundedSolidEulerOperators.lringmv(s, newOuter, oldf, false)) {
            return null;
        }
        return newOuter;
    }

    private static isWoundAsDisk(loop: _PolyhedralBoundedSolidLoop, face: _PolyhedralBoundedSolidFace): boolean {
        return _PolyhedralBoundedSolidOperator.windingDot(loop, face) > VSDK.EPSILON * VSDK.EPSILON;
    }

    /**
    True when `loop` is a loop of `face` whose winding (Newell normal) is
    opposite to the face normal, i.e. it is wound as a hole. A loop with no
    measurable area answers false.
    */
    private static isWoundAsHole(loop: _PolyhedralBoundedSolidLoop, face: _PolyhedralBoundedSolidFace): boolean {
        return _PolyhedralBoundedSolidOperator.windingDot(loop, face) < -VSDK.EPSILON * VSDK.EPSILON;
    }

    /** Newell normal of `loop` dotted with `face`'s normal (0 if unknown). */
    private static windingDot(
        loop: _PolyhedralBoundedSolidLoop | null,
        face: _PolyhedralBoundedSolidFace | null,
    ): number {
        if (loop === null || face === null || face.getContainingPlane() === null || loop.halfEdgesList.size() < 3) {
            return 0.0;
        }
        let nx = 0.0;
        let ny = 0.0;
        let nz = 0.0;
        let i: number;
        const n = loop.halfEdgesList.size();
        for (i = 0; i < n; i++) {
            const a = loop.halfEdgesList.get(i)!.startingVertex.position;
            const b = loop.halfEdgesList.get((i + 1) % n)!.startingVertex.position;
            nx += (a.y() - b.y()) * (a.z() + b.z());
            ny += (a.z() - b.z()) * (a.x() + b.x());
            nz += (a.x() - b.x()) * (a.y() + b.y());
        }
        const normal = face.getContainingPlane()!.getNormal();
        return nx * normal.x() + ny * normal.y() + nz * normal.z();
    }

    /**
    Following section [MANT1988].14.7.2. and program [MANT1988].14.10.
    */
    protected static join(
        h1: _PolyhedralBoundedSolidHalfEdge,
        h2: _PolyhedralBoundedSolidHalfEdge,
        withDebug: boolean,
    ): void {
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
        let dividedInnerRing: _PolyhedralBoundedSolidLoop | null = null;
        if (h1.parentLoop === h2.parentLoop) {
            if (oldf.boundariesList.size() > 1 && oldf.boundariesList.get(0) !== h1.parentLoop) {
                dividedInnerRing = h1.parentLoop;
            }
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
            // The second division used to leave every ring in the face it
            // splits. That stranded strut rings far from their junction
            // partners, which the connector then had to rescue (pending item
            // 3 of Fable5.2Analysis.md). Moving every ring regressed
            // MOON_BLOCK; moving only rings that clearly lie in the new half
            // (below) removed every rescue of the reference corpus and the
            // box fuzz.
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
            // Rings of the face divided by the second lmef that clearly lie
            // in the new half follow it ([MANT1988].13.5).
            if (splitFace2 !== null && newFace2 !== null) {
                let k: number;
                const sides = new _RingSideTest(splitFace2, newFace2);
                for (k = 1; k < splitFace2.boundariesList.size(); k++) {
                    const ring = splitFace2.boundariesList.get(k)!;
                    if (
                        sides.side(ring) === 2 &&
                        PolyhedralBoundedSolidEulerOperators.lringmv(s, ring, newFace2, false)
                    ) {
                        k--;
                    }
                }
            }
            if (newf !== null && oldf.boundariesList.size() >= 2) {
                // Rings are redistributed on both solids and for every
                // operation. Connect used to skip this on solid A unless
                // the operation was an intersection; the rings that then
                // stayed in the wrong half were what the ring rescue of
                // connect moved back later, and a hole already cut by
                // another curve was never moved back (plan-stage11 Phase
                // F).
                _PolyhedralBoundedSolidOperator.laringmv(
                    oldf,
                    newf,
                    _PolyhedralBoundedSolidOperator.holeLeftByDividedRing(oldf, newf, dividedInnerRing),
                );
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
        // Collinear edges: |ref1 x ref2| is |ref1| |ref2| sin(angle), so the
        // test compares the sine, not an area that shrinks with the model.
        if (ref12.length() < VSDK.EPSILON * ref1.length() * ref2.length()) {
            return true;
        }
        return ref12.dotProduct(he.parentLoop.parentFace.getContainingPlane()!.getNormal()) <= 0.0;
    }
}

/**
The outer loop of a face as {@link #clearRingSide} needs it: its
vertices in order and the coordinate dropped to project it. Built once
per face, so that testing many rings against the same division walks
the loop and computes the plane once (performance.md P-04). The answers
are those of {@link #outerLoopClearance} and
{@link #outerLoopParityContains}.
*/
class _OuterLoopShadow {
    /** False when the loop has no start half-edge or is not closed. */
    private readonly walkable: boolean;
    /** False when the face has no plane. */
    private readonly planar: boolean;
    private readonly dropAxis: number;
    private readonly points: Vector3Dd[];
    private readonly u: number[];
    private readonly v: number[];

    public constructor(f: _PolyhedralBoundedSolidFace) {
        const found: Vector3Dd[] = [];
        const outer = f.boundariesList.get(0);
        const start = outer === null ? null : outer.boundaryStartHalfEdge;
        let he = start;
        let closed = start !== null;
        let guard = 0;

        while (closed) {
            if (he!.startingVertex === null) {
                closed = false;
                break;
            }
            found.push(he!.startingVertex.position);
            he = he!.next();
            guard++;
            if (he === null) {
                closed = false;
            } else if (he === start || guard >= 100000) {
                break;
            }
        }
        this.walkable = closed;
        this.points = found.slice();
        const plane: InfinitePlane | null = f.getContainingPlane();
        this.planar = plane !== null;
        let axis = 2;
        if (this.planar) {
            const normal = plane!.getNormal();
            const ax = Math.abs(normal.x());
            const ay = Math.abs(normal.y());
            const az = Math.abs(normal.z());
            if (ax >= ay && ax >= az) {
                axis = 0;
            } else if (ay >= ax && ay >= az) {
                axis = 1;
            }
        }
        this.dropAxis = axis;
        this.u = new Array<number>(this.points.length).fill(0.0);
        this.v = new Array<number>(this.points.length).fill(0.0);
        for (guard = 0; guard < this.points.length; guard++) {
            this.u[guard] = _PolyhedralBoundedSolidOperator.shadowProjectedU(this.points[guard]!, this.dropAxis);
            this.v[guard] = _PolyhedralBoundedSolidOperator.shadowProjectedV(this.points[guard]!, this.dropAxis);
        }
    }

    /**
    As {@link #outerLoopClearance}, with the same arithmetic on plain
    numbers: no vector is allocated per segment.
    */
    public clearance(point: Vector3Dd): number {
        let best = Double.MAX_VALUE;
        const px = point.x();
        const py = point.y();
        const pz = point.z();
        let i: number;

        if (!this.walkable) {
            return 0.0;
        }
        for (i = 0; i < this.points.length; i++) {
            const a = this.points[i]!;
            const b = this.points[(i + 1) % this.points.length]!;
            const abx = b.x() - a.x();
            const aby = b.y() - a.y();
            const abz = b.z() - a.z();
            const len2 = abx * abx + aby * aby + abz * abz;
            let t = len2 > 0.0 ? ((px - a.x()) * abx + (py - a.y()) * aby + (pz - a.z()) * abz) / len2 : 0.0;
            t = Math.max(0.0, Math.min(1.0, t));
            const dx = px - (a.x() + abx * t);
            const dy = py - (a.y() + aby * t);
            const dz = pz - (a.z() + abz * t);
            best = Math.min(best, Math.sqrt(dx * dx + dy * dy + dz * dz));
        }
        return best;
    }

    /** As {@link #outerLoopParityContains}. */
    public contains(point: Vector3Dd | null): boolean | null {
        let pu: number;
        let pv: number;
        let inside = false;
        let i: number;

        if (!this.planar || !this.walkable || point === null) {
            return null;
        }
        pu = _PolyhedralBoundedSolidOperator.shadowProjectedU(point, this.dropAxis);
        pv = _PolyhedralBoundedSolidOperator.shadowProjectedV(point, this.dropAxis);
        for (i = 0; i < this.points.length; i++) {
            const next = (i + 1) % this.points.length;
            if (this.v[i]! > pv !== this.v[next]! > pv) {
                const crossingU =
                    ((this.u[next]! - this.u[i]!) * (pv - this.v[i]!)) / (this.v[next]! - this.v[i]!) + this.u[i]!;
                if (pu < crossingU) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }
}

/**
{@link #clearRingSide} for many rings of the same division: the two
outer loops and the tolerance are prepared once. Moving a ring from one
face to the other does not change their outer loops, so one instance
serves a whole pass over the rings.
*/
class _RingSideTest {
    private readonly firstFace: _PolyhedralBoundedSolidFace;
    private readonly secondFace: _PolyhedralBoundedSolidFace;
    private shadow1: _OuterLoopShadow | null = null;
    private shadow2: _OuterLoopShadow | null = null;
    private tolerance = 0.0;

    public constructor(f1: _PolyhedralBoundedSolidFace, f2: _PolyhedralBoundedSolidFace) {
        this.firstFace = f1;
        this.secondFace = f2;
    }

    /** See {@link #clearRingSide}. */
    public side(l: _PolyhedralBoundedSolidLoop | null): number {
        if (
            l === null ||
            l.boundaryStartHalfEdge === null ||
            l.boundaryStartHalfEdge.startingVertex === null ||
            this.firstFace.boundariesList.size() === 0 ||
            this.secondFace.boundariesList.size() === 0 ||
            this.firstFace.boundariesList.get(0) === l ||
            this.secondFace.boundariesList.get(0) === l
        ) {
            return 0;
        }
        if (this.shadow1 === null) {
            this.shadow1 = new _OuterLoopShadow(this.firstFace);
            this.shadow2 = new _OuterLoopShadow(this.secondFace);
            this.tolerance = PolyhedralBoundedSolidNumericPolicy.forSolid(this.firstFace.parentSolid).bigEpsilon();
        }
        const point = l.boundaryStartHalfEdge.startingVertex.position;
        if (this.shadow1.clearance(point) <= this.tolerance || this.shadow2!.clearance(point) <= this.tolerance) {
            return 0;
        }
        const in1 = this.shadow1.contains(point);
        const in2 = this.shadow2!.contains(point);
        if (in1 === null || in2 === null || in1 === in2) {
            return 0;
        }
        return in1 ? 1 : 2;
    }
}
