//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import { PolyhedralBoundedSolidEulerOperators } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";

// Java classes
import { Double as JavaDouble } from "../../../../../../../java/lang/Double.js";
import { ArrayList } from "../../../../../../../java/util/ArrayList.js";
import { Collections } from "../../../../../../../java/util/Collections.js";

// VitralSDK classes
import { PolyhedralBoundedSolidStatistics } from "../../../../../common/statistics/PolyhedralBoundedSolidStatistics.js";
import { Vector3Dd } from "../../../../../common/linealAlgebra/Vector3Dd.js";
import { Geometry } from "../../../Geometry.js";
import type { InfinitePlane } from "../../../surface/InfinitePlane.js";
import { PolyhedralBoundedSolid } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import {
    PolyhedralBoundedSolidNumericPolicy,
    type ToleranceContext,
} from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import { PolyhedralBoundedSolidStructuralCopy } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidStructuralCopy.js";
import { PolyhedralBoundedSolidValidationEngine } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import type { _PolyhedralBoundedSolidFace } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidLoop } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";
import type { _PolyhedralBoundedSolidEdge } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { _PolyhedralBoundedSolidVertex } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";
import { _PolyhedralBoundedSolidOperator } from "../_PolyhedralBoundedSolidOperator.js";
import { _PolyhedralBoundedSolidSectorClassification } from "../_PolyhedralBoundedSolidSectorClassification.js";
import { _PolyhedralBoundedSolidVertexNeighborhood } from "../_PolyhedralBoundedSolidVertexNeighborhood.js";

/**
This class is used to store vertex / halfedge neigborhood information, as presented
in section [MANT1988].14.5, and program [MANT1988].14.3.
*/
export class _PolyhedralBoundedSolidSplitterSectorClassification extends _PolyhedralBoundedSolidSectorClassification {
    public override toString(): string {
        let msg = "{";
        msg = msg + this.sector;
        switch (this.classification) {
            case _PolyhedralBoundedSolidSectorClassification.ABOVE:
                msg = msg + " ABOVE";
                break;
            case _PolyhedralBoundedSolidSectorClassification.BELOW:
                msg = msg + " BELOW";
                break;
            case _PolyhedralBoundedSolidSectorClassification.ON:
                msg = msg + " ON";
                break;
            default:
                msg = msg + "<INVALID!>";
                break;
        }
        if (this.isWide) {
            msg = msg + " (W) ";
        }
        //msg = msg + ", pos: " + position;

        switch (this.situation) {
            case _PolyhedralBoundedSolidSectorClassification.COPLANAR_FACE:
                msg = msg + "<COPLANAR_FACE>";
                break;
            case _PolyhedralBoundedSolidSectorClassification.INPLANE_EDGE:
                msg = msg + "<INPLANE_EDGE>";
                break;
            case _PolyhedralBoundedSolidSectorClassification.CROSSING_EDGE:
                msg = msg + "<CROSSING_EDGE>";
                break;
            default:
                msg = msg + "<UNDEFINED>";
                break;
        }

        msg = msg + "}";
        return msg;
    }
}

/**
Class `_PolyhedralBoundedSolidSplitterNullEdge` plays a role of a decorator
design patern for class `_PolyhedralBoundedSolidEdge`, and adds sort-ability.
*/
export class _PolyhedralBoundedSolidSplitterNullEdge extends _PolyhedralBoundedSolidOperator {
    private readonly numericContext: ToleranceContext;

    public e: _PolyhedralBoundedSolidEdge;

    public constructor(e: _PolyhedralBoundedSolidEdge, numericContext: ToleranceContext | null) {
        super();
        this.e = e;
        this.numericContext =
            numericContext === null ? PolyhedralBoundedSolidNumericPolicy.defaultContext() : numericContext;
    }

    public compareTo(other: _PolyhedralBoundedSolidSplitterNullEdge): number {
        const a = this.e.rightHalf!.startingVertex.position;
        const b = other.e.rightHalf!.startingVertex.position;

        if (PolyhedralBoundedSolidNumericPolicy.compare(a.x(), b.x(), this.numericContext.bigEpsilon()) !== 0) {
            if (a.x() < b.x()) {
                return -1;
            }
            return 1;
        } else {
            if (PolyhedralBoundedSolidNumericPolicy.compare(a.y(), b.y(), this.numericContext.bigEpsilon()) !== 0) {
                if (a.y() < b.y()) {
                    return -1;
                }
                return 1;
            } else {
                if (a.z() < b.z()) {
                    return -1;
                }
                return 1;
            }
        }
    }
}

/**
Working state of one split call. The global variables of program
[MANT1988].14.1 live here instead of in static fields, so that split is
reentrant and leaves nothing behind (Fable5.2Analysis.md Stage E).
Java nests this class as `_PolyhedralBoundedSolidSplitter._SplitContext`.
*/
class SplitContext {
    /**
    Variable `soov` ("set of ON-vertices") from program [MANT1988].14.1.
    */
    public readonly soov = new ArrayList<_PolyhedralBoundedSolidVertex>();

    /**
    Variable `sone` ("set of null edges") from program [MANT1988].14.1.
    */
    public readonly sone = new ArrayList<_PolyhedralBoundedSolidSplitterNullEdge>();

    /**
    Variable `sonf` ("set of null faces") from program [MANT1988].14.1.
    */
    public readonly sonf = new ArrayList<_PolyhedralBoundedSolidFace>();

    public readonly facesToFixAbove = new ArrayList<_PolyhedralBoundedSolidFace>();
    public readonly facesToFixBelow = new ArrayList<_PolyhedralBoundedSolidFace>();

    /**
    Variable `ends` from program [MANT1988].14.9.
    */
    public readonly ends = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();
    public readonly tieds = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();

    /**
    Null edges created by lmev(he, he) (struts): both halves lie in the
    same face. Their halves are paired by geometry in `splitConnect`.
    Java uses an identity set; JavaScript `Set` compares objects by identity.
    */
    public readonly struts = new Set<_PolyhedralBoundedSolidEdge>();
}

/**
This is a utility class containing operations for implementing the boundary
representation split methods over winged-edge data structures, as presented
at chapter [MANT1988].14.

This class offers just one public method, which is supposed to be called
from GeometricModeler class.
*/
export class _PolyhedralBoundedSolidSplitter extends _PolyhedralBoundedSolidOperator {
    private static compareToZero(value: number): number {
        return PolyhedralBoundedSolidNumericPolicy.compareToZero(value, _PolyhedralBoundedSolidOperator.numericContext);
    }

    /**
    Implements function `addsoov` from section [MANT1988].14.4. and program
    [MANT1988].14.2.
    */
    private static addsoov(ctx: SplitContext, v: _PolyhedralBoundedSolidVertex): void {
        let i: number;

        for (i = 0; i < ctx.soov.size(); i++) {
            if (ctx.soov.get(i) === v) {
                return;
            }
        }
        ctx.soov.add(v);
    }

    /**
    Implements solid splitting reduction step as indicated on sections
    [MANT1988].14.2.1 and [MANT1988].14.4 and program [MANT1988].14.2.

    This method is responsible for generating the set of coplanar
    vertices of `inSolid` (with respect to `inSplittingPlane`) and store
    them on `soov` for later usage.

    This method subdivides all edges of `inSolid` that intersects
    `inSplittingPlane` at their intersection points.
    */
    private static splitGenerate(
        ctx: SplitContext,
        inSolid: PolyhedralBoundedSolid,
        inSplittingPlane: InfinitePlane,
    ): void {
        let e: _PolyhedralBoundedSolidEdge;
        let he: _PolyhedralBoundedSolidHalfEdge;
        let v1: _PolyhedralBoundedSolidVertex;
        let v2: _PolyhedralBoundedSolidVertex;
        let p: Vector3Dd;
        let d1: number;
        let d2: number;
        let t: number;
        let s1: number;
        let s2: number;
        let i: number;

        for (i = 0; i < inSolid.getEdgesList().size(); i++) {
            e = inSolid.getEdgesList().get(i)!;
            v1 = e.rightHalf!.startingVertex;
            v2 = e.leftHalf!.startingVertex;
            d1 = inSplittingPlane.pointDistance(v1.position);
            d2 = inSplittingPlane.pointDistance(v2.position);
            s1 = _PolyhedralBoundedSolidSplitter.compareToZero(d1);
            s2 = _PolyhedralBoundedSolidSplitter.compareToZero(d2);
            if ((s1 === -1 && s2 === 1) || (s1 === 1 && s2 === -1)) {
                t = d1 / (d1 - d2);
                p = v1.position.add(v2.position.subtract(v1.position).multiply(t));
                he = e.leftHalf!.next()!;
                PolyhedralBoundedSolidEulerOperators.lmev(inSolid, e.rightHalf, he, inSolid.getMaxVertexId() + 1, p);
                _PolyhedralBoundedSolidSplitter.addsoov(ctx, he.previous()!.startingVertex);
            } else {
                if (s1 === 0) {
                    _PolyhedralBoundedSolidSplitter.addsoov(ctx, v1);
                }
                if (s2 === 0) {
                    _PolyhedralBoundedSolidSplitter.addsoov(ctx, v2);
                }
            }
        }
    }

    /**
    Current method is the first step for the initial classification of vertex
    neighborhood for `vtx`, as indicated on section [MANT1988].14.5.2. and
    program [MANT1988].14.4.

    Vitral SDK's implementation of this procedure extends the original from
    [MANT1988] by adding extra information flags to sector classifications
    `.isWide`, `.position` and `.situation`, as a debugging aid.

    <p>Sector convention. Entries follow the half-edges `he_k` leaving
    `vtx`, with `he_(k+1) = mirror(he_k).next()`. Entry k classifies the
    edge `e_k` of `he_k`. The face of `he_k` lies between `e_(k-1)` and
    `e_k`, so a change of class between entries k-1 and k means the plane
    crosses the face of `he_k`: the sector of entry k. A reflex face
    corner gets an extra (wide) entry at its inward bisector, placed
    BEFORE the entry of its half-edge, i.e. between `e_(k-1)` and `e_k`
    where it geometrically belongs (problem [MANT1988].14.1). The same
    convention is used by the set operator's vertex/face classifier.</p>
    */
    private static getNeighborhood(
        vtx: _PolyhedralBoundedSolidVertex,
        inSplittingPlane: InfinitePlane,
    ): ArrayList<_PolyhedralBoundedSolidSplitterSectorClassification> {
        return _PolyhedralBoundedSolidVertexNeighborhood.classify(
            vtx,
            inSplittingPlane,
            () => new _PolyhedralBoundedSolidSplitterSectorClassification(),
        );
    }

    /**
    Current method applies the first reclassification rule presented at
    sections [MANT1988].14.5.1 and [MANT1988].14.5.2. A face lying on the
    plane spans the entry of its half-edge and the entry before it (see the
    sector convention in `getNeighborhood`); both get the class that
    leaves the face on the side opposite to its outward normal.
    For the given vertex neigborhood, classify each edge according to whether
    its final vertex lies above, on or below the `inSplittingPlane`. Tag
    the edge with the corresponding label ABOVE, ON or BELOW.
    Following program [MANT1988].14.5.
    */
    private static reclassifyOnSectors(
        nbr: ArrayList<_PolyhedralBoundedSolidSplitterSectorClassification>,
        inSplittingPlane: InfinitePlane,
    ): void {
        let f: _PolyhedralBoundedSolidFace;
        let c: Vector3Dd;
        let d: number;
        let i: number;
        let l: _PolyhedralBoundedSolidSplitterSectorClassification;

        for (i = 0; i < nbr.size(); i++) {
            l = nbr.get(i);
            f = l.sector.parentLoop.parentFace;
            c = f.getContainingPlane()!.getNormal().crossProduct(inSplittingPlane.getNormal());
            d = c.dotProduct(c);
            if (_PolyhedralBoundedSolidSplitter.compareToZero(d) === 0) {
                // Entering this means "faces are coplanar"
                d = f.getContainingPlane()!.getNormal().dotProduct(inSplittingPlane.getNormal());
                if (_PolyhedralBoundedSolidSplitter.compareToZero(d) === 1) {
                    l.classification = _PolyhedralBoundedSolidSectorClassification.BELOW;
                    l.situation = _PolyhedralBoundedSolidSectorClassification.COPLANAR_FACE;
                    nbr.get((i + nbr.size() - 1) % nbr.size()).classification =
                        _PolyhedralBoundedSolidSectorClassification.BELOW;
                } else {
                    l.classification = _PolyhedralBoundedSolidSectorClassification.ABOVE;
                    l.situation = _PolyhedralBoundedSolidSectorClassification.COPLANAR_FACE;
                    nbr.get((i + nbr.size() - 1) % nbr.size()).classification =
                        _PolyhedralBoundedSolidSectorClassification.ABOVE;
                }
            }
        }
    }

    /**
    Current method applies the second reclassification rule presented at
    sections [MANT1988].14.5.1 and [MANT1988].14.5.2:
    After applying the first rule on method `reclassifyOnSectors`, ON edges
    may appear in only four kinds of consecutive arrangements. For each
    of the following arrangements, ON edge is reclassified as ABOVE or BELOW:
      - Sequence ABOVE/ON/ABOVE -> reclassified as BELOW
      - Sequence ABOVE/ON/BELOW -> reclassified as BELOW
      - Sequence BELOW/ON/BELOW -> reclassified as ABOVE
      - Sequence BELOW/ON/ABOVE -> reclassified as BELOW
    Those 4 rules are designed so that nonmanifold results will be represented
    as disconnected models.
    Following program [MANT1988].14.6.

    <p>The two rules with equal neighbours are only right for a reflex
    (concave) edge, where the solid lies on both sides of the plane and the
    section is pinched at the edge. A convex edge with both neighbours on
    one side merely touches the plane: reclassifying it to the other side
    would insert a pair of null edges around a sector of zero width and
    produce a two-vertex null face (found with the split fuzz, Stage E). A
    convex touching edge therefore keeps the class of its neighbours.</p>
    */
    private static reclassifyOnEdges(nbr: ArrayList<_PolyhedralBoundedSolidSplitterSectorClassification>): void {
        let l: _PolyhedralBoundedSolidSplitterSectorClassification;
        let prev: number;
        let next: number;
        let i: number;
        const n = nbr.size();

        for (i = 0; i < n; i++) {
            l = nbr.get(i);
            if (l.classification !== _PolyhedralBoundedSolidSectorClassification.ON) {
                continue;
            }
            prev = nbr.get((n + i - 1) % n).classification;
            next = nbr.get((i + 1) % n).classification;
            if (prev === next && !l.isWide && _PolyhedralBoundedSolidSplitter.isConvexEdge(l.sector)) {
                l.classification = prev;
            } else if (
                prev === _PolyhedralBoundedSolidSectorClassification.BELOW &&
                next === _PolyhedralBoundedSolidSectorClassification.BELOW
            ) {
                l.classification = _PolyhedralBoundedSolidSectorClassification.ABOVE;
            } else {
                l.classification = _PolyhedralBoundedSolidSectorClassification.BELOW;
            }
        }
    }

    /**
    True when the edge of `he` is convex: with outward face normals `nA`
    (face of `he`) and `nB` (face of its mirror) and loops running
    counterclockwise about them, the edge is convex when
    (nA x nB) . t > 0, `t` being the direction of `he`. The test is local
    to the edge, so it holds for non-convex faces. Flat or undecidable
    edges report false, which keeps the [MANT1988] rule.
    */
    private static isConvexEdge(he: _PolyhedralBoundedSolidHalfEdge | null): boolean {
        let t: Vector3Dd;
        const numericContext = _PolyhedralBoundedSolidOperator.numericContext;

        if (he === null || he.parentEdge === null) {
            return false;
        }
        const pa = he.parentLoop.parentFace.getContainingPlane();
        const pb = he.mirrorHalfEdge()!.parentLoop.parentFace.getContainingPlane();
        if (pa === null || pb === null) {
            return false;
        }
        t = he.next()!.startingVertex.position.subtract(he.startingVertex.position);
        if (t.length() <= numericContext.unitVectorTolerance()) {
            return false;
        }
        const s = pa.getNormal().crossProduct(pb.getNormal()).dotProduct(t.normalized());
        return s > numericContext.unitVectorTolerance();
    }

    /**
    Following section [MANT1988].14,6,2 and program [MANT1988].14.7.
    Note, this code is horrible! YUCK! :P

    With respect to the original algorithm from [MANT1988], current
    implementation adds an extra check to ensure the orientation of the
    edges from below to above.

    <p>The null edge is inserted in the sectors where the class changes:
    the entry AFTER each BELOW/ABOVE and ABOVE/BELOW transition (the face
    of that entry's half-edge is the one the plane crosses). Taking the
    entry before the transition, as this code used to, puts the null edge
    in the wrong face whenever the ON-vertex has more than two edges, so
    no null edges could be joined and the whole solid was lost (defect
    S-01, plane through a vertex).</p>
    */
    private static insertNullEdges(
        ctx: SplitContext,
        nbr: ArrayList<_PolyhedralBoundedSolidSplitterSectorClassification>,
        inSolid: PolyhedralBoundedSolid,
        _inSplittingPlane: InfinitePlane,
    ): void {
        let i: number;
        let head: _PolyhedralBoundedSolidHalfEdge;
        let tail: _PolyhedralBoundedSolidHalfEdge;
        const nnbr = nbr.size();

        if (nnbr <= 0) return;

        //- Locate the head of an ABOVE-sequence --------------------------
        i = 0;
        while (!(
            nbr.get(i).classification === _PolyhedralBoundedSolidSectorClassification.BELOW &&
            nbr.get((i + 1) % nnbr).classification === _PolyhedralBoundedSolidSectorClassification.ABOVE
        )) {
            i++;
            if (i >= nnbr) {
                return;
            }
        }
        const start = i;
        head = nbr.get((i + 1) % nnbr).sector;

        //-----------------------------------------------------------------
        while (true) {
            //- Locate the final sector of the sequence ------------------
            while (!(
                nbr.get(i).classification === _PolyhedralBoundedSolidSectorClassification.ABOVE &&
                nbr.get((i + 1) % nnbr).classification === _PolyhedralBoundedSolidSectorClassification.BELOW
            )) {
                i = (i + 1) % nnbr;
            }
            tail = nbr.get((i + 1) % nnbr).sector;

            //- Insert null edge -----------------------------------------
            // Always the same orientation, so that the two null-edge halves
            // lying in a crossed face are a right and a left half and
            // `neighbor` can join them. Choosing it per vertex from the side
            // of head's far end (as this code used to) gave inconsistent
            // orientations at reflex ON-vertices.
            // A face crossed twice at this vertex (reflex corner) is the
            // sector of two runs. After the first null edge split the
            // vertex, the corner left for the second crossing starts at
            // the null half inserted just before the sector half-edge.
            if (tail.startingVertex !== head.startingVertex) {
                if (tail.previous()!.startingVertex === head.startingVertex) {
                    tail = tail.previous()!;
                } else if (head.previous()!.startingVertex === tail.startingVertex) {
                    head = head.previous()!;
                }
            }
            PolyhedralBoundedSolidEulerOperators.lmev(
                inSolid,
                tail,
                head,
                inSolid.getMaxVertexId() + 1,
                head.startingVertex.position,
            );
            if (head === tail) {
                // A run made of one wide (bisector) entry only: the plane
                // enters and leaves the same reflex face corner at the
                // vertex. lmev(he, he) inserts a strut edge with both
                // halves in that face ([MANT1988] Sec. 14.6.2).
                ctx.struts.add(tail.previous()!.parentEdge!);
            }
            ctx.sone.add(
                new _PolyhedralBoundedSolidSplitterNullEdge(
                    tail.previous()!.parentEdge!,
                    _PolyhedralBoundedSolidOperator.numericContext,
                ),
            );

            //- Locate the start of the next sequence --------------------
            while (!(
                nbr.get(i).classification === _PolyhedralBoundedSolidSectorClassification.BELOW &&
                nbr.get((i + 1) % nnbr).classification === _PolyhedralBoundedSolidSectorClassification.ABOVE
            )) {
                i = (i + 1) % nnbr;
                if (i === start) {
                    return;
                }
            }
            // Program 14.7 starts every ABOVE run at its own head. This
            // code used to keep the head of the first run, so a vertex
            // with two runs got its second null edge in the wrong sector.
            head = nbr.get((i + 1) % nnbr).sector;
        }
    }

    /**
    Vertex neighborhood classifier, as presented in section [MANT1988].14.5,
    and program 14.3.

    <p>Earlier versions reversed the neighborhood when it had in-plane
    edges, to compensate for taking the null-edge sectors one entry too
    early. With the sector convention documented on `getNeighborhood` no
    reversal is needed.</p>
    */
    private static splitClassify(
        ctx: SplitContext,
        inSolid: PolyhedralBoundedSolid,
        inSplittingPlane: InfinitePlane,
    ): void {
        let i: number;

        /// Following variable `nbr` from program [MANT1988].14.3.
        let nbr: ArrayList<_PolyhedralBoundedSolidSplitterSectorClassification>;

        for (i = 0; i < ctx.soov.size(); i++) {
            nbr = _PolyhedralBoundedSolidSplitter.getNeighborhood(ctx.soov.get(i), inSplittingPlane);
            _PolyhedralBoundedSolidSplitter.reclassifyOnSectors(nbr, inSplittingPlane);
            _PolyhedralBoundedSolidSplitter.reclassifyOnEdges(nbr);
            _PolyhedralBoundedSolidSplitter.insertNullEdges(ctx, nbr, inSolid, inSplittingPlane);
        }
    }

    /**
    Following section [MANT1988].14.7.2. and program [MANT1988].14.9.
    */
    private static canJoin(
        ctx: SplitContext,
        he: _PolyhedralBoundedSolidHalfEdge,
    ): _PolyhedralBoundedSolidHalfEdge | null {
        let ret: _PolyhedralBoundedSolidHalfEdge;
        let i: number;

        for (i = 0; i < ctx.ends.size(); i++) {
            if (_PolyhedralBoundedSolidOperator.neighbor(he, ctx.ends.get(i))) {
                ret = ctx.ends.get(i);
                ctx.ends.remove(i);
                ctx.tieds.add(ret);
                return ret;
            }
        }
        ctx.ends.add(he);
        return null;
    }

    private static printNbr(neighborSectorsInfo: ArrayList<_PolyhedralBoundedSolidSplitterSectorClassification>): void {
        let i: number;

        for (i = 0; i < neighborSectorsInfo.size(); i++) {
            console.log("  - " + neighborSectorsInfo.get(i));
        }
    }

    private static printEnds(ctx: SplitContext): void {
        let i: number;

        for (i = 0; i < ctx.ends.size(); i++) {
            console.log("  - ends[" + i + "]: " + ctx.ends.get(i));
        }
    }

    /**
    Following section [MANT1988].14.7.2. and program [MANT1988].14.10.
    */
    private static cut(ctx: SplitContext, he: _PolyhedralBoundedSolidHalfEdge): void {
        const s = he.parentLoop.parentFace.parentSolid;

        if (he.parentEdge!.rightHalf!.parentLoop === he.parentEdge!.leftHalf!.parentLoop) {
            ctx.sonf.add(he.parentLoop.parentFace);
            PolyhedralBoundedSolidEulerOperators.lkemr(s, he.parentEdge!.rightHalf!, he.parentEdge!.leftHalf!);
        } else {
            PolyhedralBoundedSolidEulerOperators.lkef(s, he.parentEdge!.rightHalf!, he.parentEdge!.leftHalf!);
        }
    }

    private static isLoose(ctx: SplitContext, he: _PolyhedralBoundedSolidHalfEdge | null): boolean {
        let i: number;

        for (i = 0; i < ctx.tieds.size(); i++) {
            if (he === ctx.tieds.get(i)) return false;
        }

        return true;
    }

    /**
    Following section [MANT1988].14.7.2. and program [MANT1988].14.9.
    */
    private static splitConnect(ctx: SplitContext): void {
        let i: number;
        let nextedge: _PolyhedralBoundedSolidEdge;
        let h1: _PolyhedralBoundedSolidHalfEdge | null;
        let h2: _PolyhedralBoundedSolidHalfEdge | null;

        Collections.sort(ctx.sone);

        for (i = 0; i < ctx.sone.size(); i++) {
            nextedge = ctx.sone.get(i).e;
            if (ctx.struts.has(nextedge)) {
                _PolyhedralBoundedSolidSplitter.orientStrut(ctx, nextedge);
            }
            h1 = _PolyhedralBoundedSolidSplitter.canJoin(ctx, nextedge.rightHalf!);
            if (h1 !== null) {
                _PolyhedralBoundedSolidOperator.join(h1, nextedge.rightHalf!, false);
                ctx.tieds.add(nextedge.rightHalf!);
                if (!_PolyhedralBoundedSolidSplitter.isLoose(ctx, h1.mirrorHalfEdge())) {
                    _PolyhedralBoundedSolidSplitter.cut(ctx, h1);
                }
            }
            h2 = _PolyhedralBoundedSolidSplitter.canJoin(ctx, nextedge.leftHalf!);
            if (h2 !== null) {
                _PolyhedralBoundedSolidOperator.join(h2, nextedge.leftHalf!, false);
                ctx.tieds.add(nextedge.leftHalf!);
                if (!_PolyhedralBoundedSolidSplitter.isLoose(ctx, h2.mirrorHalfEdge())) {
                    _PolyhedralBoundedSolidSplitter.cut(ctx, h2);
                }
            }
            if (h1 !== null && h2 !== null) {
                _PolyhedralBoundedSolidSplitter.cut(ctx, nextedge.rightHalf!);
            }
        }
    }

    /**
    A strut (null edge from lmev(he, he)) sits at a reflex corner of a face
    that the section line crosses straight through the vertex. Its halves
    are `s1` (from the original vertex to the new one) followed by `s2`
    in the face loop, and `s2.next()` leaves the vertex along the face
    boundary in direction `a`. Joining the earlier loose end `E` of that
    face to `s1` leaves `s2` in the part of the face that holds the
    boundary after the vertex; joining it to `s2` leaves `s1` in the part
    before. The far end of the section line must end up with the half
    that stays loose, so `s1` is joined when `E` lies on the second ray
    counterclockwise from `a`, and `s2` otherwise. `canJoin` pairs a right
    half with a left one, so the edge's right/left roles are swapped when
    needed for the chosen half to be complementary to `E`.
    */
    private static orientStrut(ctx: SplitContext, strut: _PolyhedralBoundedSolidEdge): void {
        let e: _PolyhedralBoundedSolidHalfEdge | null = null;
        let i: number;
        const numericContext = _PolyhedralBoundedSolidOperator.numericContext;

        const s1 = strut.rightHalf!.next() === strut.leftHalf ? strut.rightHalf! : strut.leftHalf!;
        const s2 = s1.next()!;
        const face = s1.parentLoop.parentFace;
        for (i = 0; i < ctx.ends.size(); i++) {
            if (ctx.ends.get(i).parentLoop.parentFace === face) {
                e = ctx.ends.get(i);
                break;
            }
        }
        if (e === null || face.getContainingPlane() === null) {
            return;
        }
        const v = s1.startingVertex.position;
        const a = s2.next()!.next()!.startingVertex.position.subtract(v);
        const d = e.startingVertex.position.subtract(v);
        const n = face.getContainingPlane()!.getNormal();
        if (d.length() <= numericContext.unitVectorTolerance()) {
            return;
        }
        const wanted =
            _PolyhedralBoundedSolidSplitter.ccwAngle(a, d, n) >
            _PolyhedralBoundedSolidSplitter.ccwAngle(a, d.multiply(-1.0), n)
                ? s1
                : s2;
        const eIsRight = e === e.parentEdge!.rightHalf;
        const wantedIsRight = wanted === strut.rightHalf;
        if (eIsRight === wantedIsRight) {
            const tmp = strut.rightHalf;
            strut.rightHalf = strut.leftHalf;
            strut.leftHalf = tmp;
        }
    }

    /** Counterclockwise angle about `n` from `a` to `b`, in [0, 2 pi). */
    private static ccwAngle(a: Vector3Dd, b: Vector3Dd, n: Vector3Dd): number {
        const angle = Math.atan2(n.dotProduct(a.crossProduct(b)), a.dotProduct(b));
        return angle < 0.0 ? angle + 2.0 * Math.PI : angle;
    }

    /**
    Following section [MANT1988].14.8. and program [MANT1988].14.12.
    */
    private static classify(ctx: SplitContext, Above: PolyhedralBoundedSolid, Below: PolyhedralBoundedSolid): void {
        let i: number;
        // Java integer division `sonf.size()/2`.
        const half = Math.trunc(ctx.sonf.size() / 2);

        for (i = 0; i < half; i++) {
            _PolyhedralBoundedSolidOperator.movefac(ctx.sonf.get(i), Above);
            ctx.facesToFixAbove.add(ctx.sonf.get(i));
            _PolyhedralBoundedSolidOperator.movefac(ctx.sonf.get(i + half), Below);
            ctx.facesToFixBelow.add(ctx.sonf.get(i + half));
        }
    }

    /**
    A face `a` is "inside" other `b` (and should be an internal loop) if
    all vertices from `b` are inside the polygon of `a`.

    PRE: given faces are coplanar.
    */
    private static faceInsideFace(a: _PolyhedralBoundedSolidFace, b: _PolyhedralBoundedSolidFace): boolean {
        let i: number;
        let he: _PolyhedralBoundedSolidHalfEdge;
        let heStart: _PolyhedralBoundedSolidHalfEdge;

        for (i = 0; i < b.boundariesList.size(); i++) {
            heStart = b.boundariesList.get(i)!.boundaryStartHalfEdge!;
            he = heStart;
            do {
                if (
                    a.testPointInside(
                        he.startingVertex.position,
                        _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon(),
                    ) === Geometry.OUTSIDE
                ) {
                    return false;
                }
                he = he.next()!;
            } while (he !== heStart);
        }
        return true;
    }

    private static fixNullFaces(l: ArrayList<_PolyhedralBoundedSolidFace>): void {
        if (l.size() === 1) {
            l.remove(0);
            return;
        }

        let i: number;
        let j: number;

        for (i = 0; i < l.size(); i++) {
            for (j = 0; j < l.size(); j++) {
                if (i === j) continue;
                if (_PolyhedralBoundedSolidSplitter.faceInsideFace(l.get(j), l.get(i))) {
                    PolyhedralBoundedSolidEulerOperators.lkfmrh(l.get(i).parentSolid, l.get(i), l.get(j));
                    l.remove(j);
                    // Repeat he process with remaining list
                    _PolyhedralBoundedSolidSplitter.fixNullFaces(l);
                    return;
                }
            }
        }
    }

    /**
    Following section [MANT1988].14.8. and program [MANT1988].14.11.

    <p>The two halves of `sonf` are not tied to a side of the plane by the
    construction, so each piece is put in the list of the side its
    vertices lie on (it used to be the opposite one for every plane).</p>
    */
    private static splitFinish(
        ctx: SplitContext,
        inSolid: PolyhedralBoundedSolid,
        inSplittingPlane: InfinitePlane,
        outSolidsAbove: PolyhedralBoundedSolid[],
        outSolidsBelow: PolyhedralBoundedSolid[],
    ): void {
        let i: number;
        let newface: _PolyhedralBoundedSolidFace;
        let first: PolyhedralBoundedSolid;
        let second: PolyhedralBoundedSolid;

        const firstHalfSize = ctx.sonf.size();
        for (i = 0; i < firstHalfSize; i++) {
            const secondLoop: _PolyhedralBoundedSolidLoop = ctx.sonf.get(i).boundariesList.get(1)!;
            newface = PolyhedralBoundedSolidEulerOperators.lmfkrh(inSolid, secondLoop, inSolid.getMaxFaceId() + 1);
            ctx.sonf.add(newface);
        }
        first = new PolyhedralBoundedSolid();
        second = new PolyhedralBoundedSolid();
        _PolyhedralBoundedSolidSplitter.classify(ctx, first, second);
        _PolyhedralBoundedSolidSplitter.fixNullFaces(ctx.facesToFixAbove);
        _PolyhedralBoundedSolidSplitter.fixNullFaces(ctx.facesToFixBelow);
        _PolyhedralBoundedSolidOperator.cleanup(first);
        _PolyhedralBoundedSolidSplitter.resolvePinchedLoops(first);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(first);
        _PolyhedralBoundedSolidOperator.cleanup(second);
        _PolyhedralBoundedSolidSplitter.resolvePinchedLoops(second);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(second);
        if (
            _PolyhedralBoundedSolidSplitter.sideOf(first, inSplittingPlane) < 0 ||
            _PolyhedralBoundedSolidSplitter.sideOf(second, inSplittingPlane) > 0
        ) {
            const swap = first;
            first = second;
            second = swap;
        }
        for (const shell of _PolyhedralBoundedSolidSplitter.separateShells(first)) {
            outSolidsAbove.push(shell);
        }
        for (const shell of _PolyhedralBoundedSolidSplitter.separateShells(second)) {
            outSolidsBelow.push(shell);
        }
    }

    /**
    A plane through a reflex vertex can cut a section made of two regions
    that touch at that vertex. Connect then closes both regions in one
    loop that passes twice through the contact point, and the section face
    is self-touching. Two forms appear, and each is resolved with Euler
    operators so that every face loop is simple:

    <ul>
    <li>The loop passes twice through the <em>same</em> vertex: the piece
    is two regions joined only at that vertex. `lmev` splits the
    vertex between the two half-edges (the new edge, of zero length, is a
    bridge of the loop), `lkemr` removes the bridge leaving two
    rings, and `lmfkrh` makes the second ring a face of its own. The
    two regions become shells that touch at a vertex position, which
    `separateShells` returns as separate solids ([MANT1988] Sec.
    14.5.1).</li>
    <li>The loop passes through two <em>distinct</em> vertices at the same
    position: the piece is one manifold solid whose section has two faces
    sharing that point. `lmef` splits the face with a zero-length
    edge between them and `lkev` merges the two vertices.</li>
    </ul>
    */
    private static resolvePinchedLoops(piece: PolyhedralBoundedSolid): void {
        let changed = true;

        while (changed) {
            changed = false;
            let f: number;
            for (f = 0; f < piece.getPolygonsList().size() && !changed; f++) {
                const face = piece.getPolygonsList().get(f)!;
                let l: number;
                for (l = 0; l < face.boundariesList.size() && !changed; l++) {
                    changed = _PolyhedralBoundedSolidSplitter.resolveOnePinch(piece, face.boundariesList.get(l)!);
                }
            }
        }
    }

    private static resolveOnePinch(piece: PolyhedralBoundedSolid, loop: _PolyhedralBoundedSolidLoop): boolean {
        const n = loop.halfEdgesList.size();
        let a: number;
        let b: number;

        if (n < 4) {
            return false;
        }
        for (a = 0; a < n; a++) {
            for (b = a + 2; b < n; b++) {
                if (a === 0 && b === n - 1) {
                    continue;
                }
                const ha = loop.halfEdgesList.get(a)!;
                const hb = loop.halfEdgesList.get(b)!;
                const va = ha.startingVertex;
                const vb = hb.startingVertex;

                if (va === vb) {
                    if (!_PolyhedralBoundedSolidSplitter.rotatesInto(ha, hb)) {
                        continue;
                    }
                    PolyhedralBoundedSolidEulerOperators.lmev(
                        piece,
                        ha,
                        hb,
                        piece.getMaxVertexId() + 1,
                        new Vector3Dd(va.position),
                    );
                    // lmev put the new edge before ha and before hb, both
                    // in this loop: it is a bridge.
                    const bridgeA = ha.previous();
                    const bridgeB = hb.previous();
                    PolyhedralBoundedSolidEulerOperators.lkemr(piece, bridgeA, bridgeB);
                    const face = loop.parentFace;
                    PolyhedralBoundedSolidEulerOperators.lmfkrh(
                        piece,
                        face.boundariesList.get(face.boundariesList.size() - 1)!,
                        piece.getMaxFaceId() + 1,
                    );
                    return true;
                }
                if (
                    PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                        va.position,
                        vb.position,
                        _PolyhedralBoundedSolidOperator.numericContext,
                    )
                ) {
                    if (PolyhedralBoundedSolidEulerOperators.lmef(piece, ha, hb, piece.getMaxFaceId() + 1) === null) {
                        continue;
                    }
                    // lmef put the half-edge from vb before ha and the one
                    // from va before hb; lkev keeps va.
                    PolyhedralBoundedSolidEulerOperators.lkev(piece, ha.previous(), hb.previous());
                    return true;
                }
            }
        }
        return false;
    }

    /**
    True when turning around the vertex of `from` (mirror, then next,
    as lmev does) reaches `to`.
    */
    private static rotatesInto(from: _PolyhedralBoundedSolidHalfEdge, to: _PolyhedralBoundedSolidHalfEdge): boolean {
        let he: _PolyhedralBoundedSolidHalfEdge = from;
        let steps: number;

        for (steps = 0; steps < 1000; steps++) {
            if (he === to) {
                return true;
            }
            const mirror = he.mirrorHalfEdge();
            if (mirror === null) {
                return false;
            }
            he = mirror.next()!;
            if (he === from) {
                return false;
            }
        }
        return false;
    }

    /**
    The pieces on one side of the plane can be several shells. Disjoint
    shells stay together in one solid, as program [MANT1988].14.12 leaves
    them. Shells that touch at a vertex or along an edge (the non-manifold
    situations [MANT1988] Sec. 14.5.1 resolves as disconnected models) are
    returned as separate solids, so that every solid in the output lists is
    a valid manifold B-Rep.
    */
    private static separateShells(piece: PolyhedralBoundedSolid): ArrayList<PolyhedralBoundedSolid> {
        const shells = new ArrayList<PolyhedralBoundedSolid>();

        if (!_PolyhedralBoundedSolidSplitter.hasTouchingShells(piece)) {
            shells.add(piece);
            return shells;
        }
        while (piece.getPolygonsList().size() > 0) {
            const shell = new PolyhedralBoundedSolid();
            _PolyhedralBoundedSolidOperator.movefac(piece.getPolygonsList().get(0)!, shell);
            _PolyhedralBoundedSolidOperator.cleanup(shell);
            _PolyhedralBoundedSolidSplitter.updateMaximumIds(shell);
            PolyhedralBoundedSolidValidationEngine.validateIntermediate(shell);
            shells.add(shell);
        }
        return shells;
    }

    /**
    Labels the shells (connected components of the face/edge adjacency
    graph) and reports whether two different shells have a vertex at the
    same position. A split creates a vertex at every contact point, so
    shells that touch always share a vertex position.
    */
    private static hasTouchingShells(solid: PolyhedralBoundedSolid): boolean {
        // Java's IdentityHashMap: JavaScript Map keys compare by identity.
        const shellOf = new Map<_PolyhedralBoundedSolidFace, number>();
        const stack = new ArrayList<_PolyhedralBoundedSolidFace>();
        const points = new ArrayList<number[]>();
        let shells = 0;
        let i: number;

        for (i = 0; i < solid.getPolygonsList().size(); i++) {
            const start = solid.getPolygonsList().get(i)!;
            if (shellOf.has(start)) {
                continue;
            }
            shellOf.set(start, shells);
            stack.add(start);
            while (!stack.isEmpty()) {
                const f = stack.remove(stack.size() - 1);
                let j: number;
                for (j = 0; j < f.boundariesList.size(); j++) {
                    const l = f.boundariesList.get(j)!;
                    let k: number;
                    for (k = 0; k < l.halfEdgesList.size(); k++) {
                        const he = l.halfEdgesList.get(k)!;
                        const p = he.startingVertex.position;
                        points.add([p.x(), p.y(), p.z(), shells]);
                        const m = he.mirrorHalfEdge();
                        if (
                            m !== null &&
                            m.parentLoop !== null &&
                            m.parentLoop.parentFace !== null &&
                            !shellOf.has(m.parentLoop.parentFace)
                        ) {
                            shellOf.set(m.parentLoop.parentFace, shells);
                            stack.add(m.parentLoop.parentFace);
                        }
                    }
                }
            }
            shells++;
        }
        if (shells <= 1) {
            return false;
        }

        const tolerance = _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon();
        Collections.sort(points, { compare: (u: number[], w: number[]): number => JavaDouble.compare(u[0]!, w[0]!) });
        for (i = 0; i < points.size(); i++) {
            const u = points.get(i);
            let j: number;
            for (j = i + 1; j < points.size() && points.get(j)[0]! - u[0]! <= tolerance; j++) {
                const w = points.get(j);
                if (u[3] !== w[3] && Math.abs(u[1]! - w[1]!) <= tolerance && Math.abs(u[2]! - w[2]!) <= tolerance) {
                    return true;
                }
            }
        }
        return false;
    }

    private static updateMaximumIds(solid: PolyhedralBoundedSolid): void {
        let maxVertex = -1;
        let maxFace = -1;
        let i: number;

        for (i = 0; i < solid.getVerticesList().size(); i++) {
            maxVertex = Math.max(maxVertex, solid.getVerticesList().get(i)!.id);
        }
        for (i = 0; i < solid.getPolygonsList().size(); i++) {
            maxFace = Math.max(maxFace, solid.getPolygonsList().get(i)!.id);
        }
        solid.setMaxVertexId(maxVertex);
        solid.setMaxFaceId(maxFace);
    }

    /**
    True when some vertex lies strictly above the plane and some strictly
    below. Otherwise the plane misses the solid or only touches it (at a
    vertex, along an edge or on a face), there is nothing to cut, and
    running the algorithm would only insert null edges along the contact
    and return a degenerate piece.
    */
    private static straddles(solid: PolyhedralBoundedSolid, plane: InfinitePlane): boolean {
        let above = false;
        let below = false;
        let i: number;

        for (i = 0; i < solid.getVerticesList().size(); i++) {
            const side = _PolyhedralBoundedSolidSplitter.compareToZero(
                plane.pointDistance(solid.getVerticesList().get(i)!.position),
            );
            above ||= side > 0;
            below ||= side < 0;
            if (above && below) {
                return true;
            }
        }
        return false;
    }

    /**
    Side of the plane a piece lies on: the sign of the signed distance of
    its farthest vertex (0 when every vertex is on the plane or the piece
    is empty).
    */
    private static sideOf(piece: PolyhedralBoundedSolid, plane: InfinitePlane): number {
        let best = 0.0;
        let i: number;

        for (i = 0; i < piece.getVerticesList().size(); i++) {
            const d = plane.pointDistance(piece.getVerticesList().get(i)!.position);
            if (Math.abs(d) > Math.abs(best)) {
                best = d;
            }
        }
        return _PolyhedralBoundedSolidSplitter.compareToZero(best);
    }

    /**
    Given the input `inSolid` and the cutting plane `inSplittingPlane`,
    this method appends to the `outSolidsAbove` list the solids resulting
    from cutting the solid with the plane and resulting above the plane
    (positive signed distance, on the side the plane normal points to),
    similarly, `outSolidsBelow` will be appended with solid pieces
    resulting below the plane.

    <p>At least one solid is appended to each list. Disjoint pieces on one
    side stay together as shells of one solid; pieces that touch at a
    vertex or along an edge come out as separate solids. When the plane does not cut the solid
    (it misses it, touches it, or contains one of its faces), a copy of the
    solid goes to the list of its side and an empty solid to the other
    one.</p>

    <p>`inSolid` is not modified: the algorithm runs on a structural copy
    (Fable5.2Analysis.md Stage E), and the output lists never receive the
    input object.</p>

    Current macro-algorithm follows the strategy outlined on sections
    [MANT1988].14.1, [MANT1988].14.2 and [MANT1988].14.3 and program
    [MANT1988].14.1.
    */
    public static split(
        inSolid: PolyhedralBoundedSolid,
        inSplittingPlane: InfinitePlane,
        outSolidsAbove: PolyhedralBoundedSolid[],
        outSolidsBelow: PolyhedralBoundedSolid[],
    ): void {
        PolyhedralBoundedSolidStatistics.recordSplitCall();
        const aboveBefore = outSolidsAbove.length;
        const belowBefore = outSolidsBelow.length;
        const ctx = new SplitContext();
        let work: PolyhedralBoundedSolid | null;

        //-----------------------------------------------------------------
        _PolyhedralBoundedSolidOperator.setNumericContext(PolyhedralBoundedSolidNumericPolicy.forSolid(inSolid));

        PolyhedralBoundedSolidValidationEngine.validateIntermediate(inSolid);
        work = null;
        if (_PolyhedralBoundedSolidSplitter.straddles(inSolid, inSplittingPlane)) {
            work = PolyhedralBoundedSolidStructuralCopy.copy(inSolid);
            _PolyhedralBoundedSolidSplitter.splitGenerate(ctx, work, inSplittingPlane);
            _PolyhedralBoundedSolidSplitter.splitClassify(ctx, work, inSplittingPlane);
        }

        if (ctx.sone.size() <= 0) {
            PolyhedralBoundedSolidStatistics.recordSplitNoNullEdgesCase();
            if (_PolyhedralBoundedSolidSplitter.sideOf(inSolid, inSplittingPlane) < 0) {
                outSolidsAbove.push(new PolyhedralBoundedSolid());
                outSolidsBelow.push(PolyhedralBoundedSolidStructuralCopy.copy(inSolid));
            } else {
                outSolidsAbove.push(PolyhedralBoundedSolidStructuralCopy.copy(inSolid));
                outSolidsBelow.push(new PolyhedralBoundedSolid());
            }
        } else {
            _PolyhedralBoundedSolidSplitter.splitConnect(ctx);
            _PolyhedralBoundedSolidSplitter.splitFinish(ctx, work!, inSplittingPlane, outSolidsAbove, outSolidsBelow);
        }
        PolyhedralBoundedSolidStatistics.recordSplitProducedSolids(
            outSolidsAbove.length - aboveBefore,
            outSolidsBelow.length - belowBefore,
        );
    }
}
