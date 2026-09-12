//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import { PolyhedralBoundedSolidEulerOperators } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";

// Java classes
import { Collections } from "../../../../../../../java/util/Collections.js";

// VitralSDK classes
import { PolyhedralBoundedSolidStatistics } from "../../../../../common/statistics/PolyhedralBoundedSolidStatistics.js";
import { Vector3Dd } from "../../../../../common/linealAlgebra/Vector3Dd.js";
import { CircularDoubleLinkedList } from "../../../../../common/dataStructures/CircularDoubleLinkedList.js";
import { Geometry } from "../../../Geometry.js";
import type { InfinitePlane } from "../../../surface/InfinitePlane.js";
import { PolyhedralBoundedSolid } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import {
    PolyhedralBoundedSolidNumericPolicy,
    type ToleranceContext,
} from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import { PolyhedralBoundedSolidValidationEngine } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import type { _PolyhedralBoundedSolidFace } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidLoop } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";
import type { _PolyhedralBoundedSolidEdge } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { _PolyhedralBoundedSolidVertex } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";
import { _PolyhedralBoundedSolidOperator } from "../_PolyhedralBoundedSolidOperator.js";

/**
This class is used to store vertex / halfedge neigborhood information, as presented
in section [MANT1988].14.5, and program [MANT1988].14.3.
*/
export class _PolyhedralBoundedSolidSplitterSectorClassification extends _PolyhedralBoundedSolidOperator {
    public static readonly ABOVE = 1;
    public static readonly BELOW = -1;
    public static readonly ON = 0;

    public static readonly COPLANAR_FACE = 10;
    public static readonly INPLANE_EDGE = 20;
    public static readonly CROSSING_EDGE = 30;
    public static readonly UNDEFINED = 40;

    public sector: _PolyhedralBoundedSolidHalfEdge | null = null;
    public cl = 0;

    // Following attributes are not taken from [MANT1988], and all operations
    // on them are fine tunning options aditional to original algorithm.
    public isWide = false;
    public position: Vector3Dd | null = null;
    public situation = _PolyhedralBoundedSolidSplitterSectorClassification.UNDEFINED;

    public override toString(): string {
        let msg = "{";
        msg = msg + this.sector;
        switch (this.cl) {
            case _PolyhedralBoundedSolidSplitterSectorClassification.ABOVE:
                msg = msg + " ABOVE";
                break;
            case _PolyhedralBoundedSolidSplitterSectorClassification.BELOW:
                msg = msg + " BELOW";
                break;
            case _PolyhedralBoundedSolidSplitterSectorClassification.ON:
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
            case _PolyhedralBoundedSolidSplitterSectorClassification.COPLANAR_FACE:
                msg = msg + "<COPLANAR_FACE>";
                break;
            case _PolyhedralBoundedSolidSplitterSectorClassification.INPLANE_EDGE:
                msg = msg + "<INPLANE_EDGE>";
                break;
            case _PolyhedralBoundedSolidSplitterSectorClassification.CROSSING_EDGE:
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
    // Java declares a private static field that hides the inherited one.
    protected static override numericContext: ToleranceContext = PolyhedralBoundedSolidNumericPolicy.defaultContext();

    public e: _PolyhedralBoundedSolidEdge;

    public constructor(e: _PolyhedralBoundedSolidEdge) {
        super();
        this.e = e;
    }

    public static override setNumericContext(context: ToleranceContext | null): void {
        if (context === null) {
            _PolyhedralBoundedSolidSplitterNullEdge.numericContext =
                PolyhedralBoundedSolidNumericPolicy.defaultContext();
        } else {
            _PolyhedralBoundedSolidSplitterNullEdge.numericContext = context;
        }
    }

    public compareTo(other: _PolyhedralBoundedSolidSplitterNullEdge): number {
        const a = this.e.rightHalf!.startingVertex.position;
        const b = other.e.rightHalf!.startingVertex.position;

        if (
            PolyhedralBoundedSolidNumericPolicy.compare(
                a.x(),
                b.x(),
                _PolyhedralBoundedSolidSplitterNullEdge.numericContext.bigEpsilon(),
            ) !== 0
        ) {
            if (a.x() < b.x()) {
                return -1;
            }
            return 1;
        } else {
            if (
                PolyhedralBoundedSolidNumericPolicy.compare(
                    a.y(),
                    b.y(),
                    _PolyhedralBoundedSolidSplitterNullEdge.numericContext.bigEpsilon(),
                ) !== 0
            ) {
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
    Following variable `soov` ("set of ON-vertices") from program [MANT1988].14.1.
    */
    private static soov: _PolyhedralBoundedSolidVertex[] | null = null;

    /**
    Following variable `sone` ("set of null edges") from program [MANT1988].14.1.
    */
    private static sone: _PolyhedralBoundedSolidSplitterNullEdge[] | null = null;

    /**
    Following variable `sonf` ("set of null faces") from program [MANT1988].14.1.
    */
    private static sonf: _PolyhedralBoundedSolidFace[] | null = null;

    private static facesToFixAbove: _PolyhedralBoundedSolidFace[] | null = null;
    private static facesToFixBelow: _PolyhedralBoundedSolidFace[] | null = null;

    /**
    Following variable `ends` from program [MANT1988].14.9.
    */
    private static ends: _PolyhedralBoundedSolidHalfEdge[] | null = null;
    private static tieds: _PolyhedralBoundedSolidHalfEdge[] | null = null;

    /**
    Implements function `addsoov` from section [MANT1988].14.4. and program
    [MANT1988].14.2.
    */
    private static addsoov(v: _PolyhedralBoundedSolidVertex): void {
        let i: number;
        const soov = _PolyhedralBoundedSolidSplitter.soov!;

        for (i = 0; i < soov.length; i++) {
            if (soov[i] === v) {
                return;
            }
        }
        soov.push(v);
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
    private static splitGenerate(inSolid: PolyhedralBoundedSolid, inSplittingPlane: InfinitePlane): void {
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

        _PolyhedralBoundedSolidSplitter.soov = [];
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
                _PolyhedralBoundedSolidSplitter.addsoov(he.previous()!.startingVertex);
            } else {
                if (s1 === 0) {
                    _PolyhedralBoundedSolidSplitter.addsoov(v1);
                }
                if (s2 === 0) {
                    _PolyhedralBoundedSolidSplitter.addsoov(v2);
                }
            }
        }

        /*
        System.out.println("-----");
        for ( i = 0; i < soov.size(); i++ ) {
            System.out.println("  - Vertex [" + i + "]: " + soov.get(i));
        }
        System.out.println("-----");
        */
    }

    /**
    Current method is the first step for the initial classification of vertex
    neighborhood for `vtx`, as indicated on section [MANT1988].14.5.2. and
    program [MANT1988].14.4.

    Vitral SDK's implementation of this procedure extends the original from
    [MANT1988] by adding extra information flags to sector classifications
    `.isWide`, `.position` and `.situation`. Those flags are an additional
    aid for debugging purposes and specifically the `situation` flag will be
    later used on `splitClassify` to correct the ordering of sectors in order
    to keep consistency with Vitral SDK's interpretation of coordinate system.
    */
    private static getNeighborhood(
        vtx: _PolyhedralBoundedSolidVertex,
        inSplittingPlane: InfinitePlane,
    ): _PolyhedralBoundedSolidSplitterSectorClassification[] {
        let he: _PolyhedralBoundedSolidHalfEdge;
        let bisect: Vector3Dd;
        let d: number;
        let c: _PolyhedralBoundedSolidSplitterSectorClassification;

        const neighborSectorsInfo: _PolyhedralBoundedSolidSplitterSectorClassification[] = [];
        he = vtx.emanatingHalfEdge!;

        do {
            c = new _PolyhedralBoundedSolidSplitterSectorClassification();
            c.sector = he;
            d = inSplittingPlane.pointDistance(he.next()!.startingVertex.position);
            c.cl = _PolyhedralBoundedSolidSplitter.compareToZero(d);
            c.isWide = false;
            c.position = new Vector3Dd(he.next()!.startingVertex.position);
            c.situation = _PolyhedralBoundedSolidSplitterSectorClassification.UNDEFINED;
            neighborSectorsInfo.push(c);
            if (_PolyhedralBoundedSolidSplitter.checkSplitterSectorWideness(he)) {
                bisect = _PolyhedralBoundedSolidOperator.bisector(he);
                c.situation = _PolyhedralBoundedSolidSplitterSectorClassification.CROSSING_EDGE;

                c = new _PolyhedralBoundedSolidSplitterSectorClassification();
                c.sector = he;
                d = inSplittingPlane.pointDistance(bisect);
                c.cl = _PolyhedralBoundedSolidSplitter.compareToZero(d);
                c.isWide = true;
                c.position = new Vector3Dd(bisect);
                c.situation = _PolyhedralBoundedSolidSplitterSectorClassification.CROSSING_EDGE;
                neighborSectorsInfo.push(c);
            }
            he = he.mirrorHalfEdge()!.next()!;
        } while (he !== vtx.emanatingHalfEdge);

        //-----------------------------------------------------------------
        // Extra pass, not from original [MANT1988] code
        let i: number;

        for (i = 0; i < neighborSectorsInfo.length; i++) {
            c = neighborSectorsInfo[i]!;
            if (
                c.cl === _PolyhedralBoundedSolidSplitterSectorClassification.ON &&
                c.situation === _PolyhedralBoundedSolidSplitterSectorClassification.UNDEFINED
            ) {
                c.situation = _PolyhedralBoundedSolidSplitterSectorClassification.INPLANE_EDGE;
            }
        }

        return neighborSectorsInfo;
    }

    /**
    The splitter needs the oriented interior angle from [MANT1988].14.5.2.
    Boolean set operations intentionally use the legacy cross-product
    predicate inherited from `_PolyhedralBoundedSolidOperator`; that
    predicate treats degenerate/straight sectors as wide and is not
    interchangeable with the splitter classification.
    */
    private static checkSplitterSectorWideness(he: _PolyhedralBoundedSolidHalfEdge | null): boolean {
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

        const numericContext = _PolyhedralBoundedSolidOperator.numericContext;
        const vertex = he.startingVertex.position;
        let incoming = vertex.subtract(he.previous()!.startingVertex.position);
        let outgoing = he.next()!.startingVertex.position.subtract(vertex);
        let faceNormal = he.parentLoop.parentFace.getContainingPlane()!.getNormal();

        if (
            incoming.length() <= numericContext.unitVectorTolerance() ||
            outgoing.length() <= numericContext.unitVectorTolerance() ||
            faceNormal.length() <= numericContext.unitVectorTolerance()
        ) {
            return true;
        }

        incoming = incoming.normalized();
        outgoing = outgoing.normalized();
        faceNormal = faceNormal.normalized();

        const signedTurn = Math.atan2(
            faceNormal.dotProduct(incoming.crossProduct(outgoing)),
            incoming.dotProduct(outgoing),
        );
        const interiorAngle = signedTurn < 0.0 ? 2.0 * Math.PI + signedTurn : signedTurn;

        return interiorAngle > Math.PI + numericContext.angleTolerance();
    }

    private static inplaneEdgesOn(nbr: readonly _PolyhedralBoundedSolidSplitterSectorClassification[]): boolean {
        let i: number;

        for (i = 0; i < nbr.length; i++) {
            if (nbr[i]!.situation === _PolyhedralBoundedSolidSplitterSectorClassification.INPLANE_EDGE) return true;
        }
        return false;
    }

    /**
    Current method applies the first reclassification rule presented at
    sections [MANT1988].14.5.1 and [MANT1988].14.5.2:
    For the given vertex neigborhood, classify each edge according to whether
    its final vertex lies above, on or below the `inSplittingPlane`. Tag
    the edge with the corresponding label ABOVE, ON or BELOW.
    Following program [MANT1988].14.5.
    */
    private static reclassifyOnSectors(
        nbr: _PolyhedralBoundedSolidSplitterSectorClassification[],
        inSplittingPlane: InfinitePlane,
    ): void {
        let f: _PolyhedralBoundedSolidFace;
        let c: Vector3Dd;
        let d: number;
        let i: number;
        let l: _PolyhedralBoundedSolidSplitterSectorClassification;

        for (i = 0; i < nbr.length; i++) {
            l = nbr[i]!;
            f = l.sector!.parentLoop.parentFace;
            c = f.getContainingPlane()!.getNormal().crossProduct(inSplittingPlane.getNormal());
            d = c.dotProduct(c);
            if (_PolyhedralBoundedSolidSplitter.compareToZero(d) === 0) {
                // Entering this means "faces are coplanar"
                d = f.getContainingPlane()!.getNormal().dotProduct(inSplittingPlane.getNormal());
                if (_PolyhedralBoundedSolidSplitter.compareToZero(d) === 1) {
                    l.cl = _PolyhedralBoundedSolidSplitterSectorClassification.BELOW;
                    l.situation = _PolyhedralBoundedSolidSplitterSectorClassification.COPLANAR_FACE;
                    nbr[(i + 1) % nbr.length]!.cl = _PolyhedralBoundedSolidSplitterSectorClassification.BELOW;
                } else {
                    l.cl = _PolyhedralBoundedSolidSplitterSectorClassification.ABOVE;
                    l.situation = _PolyhedralBoundedSolidSplitterSectorClassification.COPLANAR_FACE;
                    nbr[(i + 1) % nbr.length]!.cl = _PolyhedralBoundedSolidSplitterSectorClassification.ABOVE;
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
    */
    private static reclassifyOnEdges(nbr: _PolyhedralBoundedSolidSplitterSectorClassification[]): void {
        let l: _PolyhedralBoundedSolidSplitterSectorClassification;
        let i: number;

        for (i = 0; i < nbr.length; i++) {
            l = nbr[i]!;
            if (l.cl === _PolyhedralBoundedSolidSplitterSectorClassification.ON) {
                if (
                    nbr[(nbr.length + i - 1) % nbr.length]!.cl ===
                    _PolyhedralBoundedSolidSplitterSectorClassification.BELOW
                ) {
                    if (nbr[(i + 1) % nbr.length]!.cl === _PolyhedralBoundedSolidSplitterSectorClassification.BELOW) {
                        nbr[i]!.cl = _PolyhedralBoundedSolidSplitterSectorClassification.ABOVE;
                    } else {
                        nbr[i]!.cl = _PolyhedralBoundedSolidSplitterSectorClassification.BELOW;
                    }
                } else {
                    nbr[i]!.cl = _PolyhedralBoundedSolidSplitterSectorClassification.BELOW;
                }
            }
        }
    }

    /**
    Following section [MANT1988].14,6,2 and program [MANT1988].14.7.
    Note, this code is horrible! YUCK! :P

    With respect to the original algorithm from [MANT1988], current
    implementation adds an extra check to ensure the orientation of the
    edges from below to above.
    */
    private static insertNullEdges(
        nbr: _PolyhedralBoundedSolidSplitterSectorClassification[],
        inSolid: PolyhedralBoundedSolid,
        inSplittingPlane: InfinitePlane,
    ): void {
        let i: number;
        let tail: _PolyhedralBoundedSolidHalfEdge;
        const nnbr = nbr.length;

        if (nnbr <= 0) return;

        //- Locate the head of an ABOVE-sequence --------------------------
        i = 0;
        while (!(
            nbr[i]!.cl === _PolyhedralBoundedSolidSplitterSectorClassification.BELOW &&
            nbr[(i + 1) % nnbr]!.cl === _PolyhedralBoundedSolidSplitterSectorClassification.ABOVE
        )) {
            i++;
            if (i >= nnbr) {
                return;
            }
        }
        const start = i;
        const head = nbr[i]!.sector!;

        //-----------------------------------------------------------------
        while (true) {
            //- Locate the final sector of the sequence ------------------
            while (!(
                nbr[i]!.cl === _PolyhedralBoundedSolidSplitterSectorClassification.ABOVE &&
                nbr[(i + 1) % nnbr]!.cl === _PolyhedralBoundedSolidSplitterSectorClassification.BELOW
            )) {
                i = (i + 1) % nnbr;
            }
            tail = nbr[i]!.sector!;

            //- Insert null edge -----------------------------------------
            const d1 = inSplittingPlane.doContainmentTestHalfSpace(
                head.next()!.startingVertex.position,
                _PolyhedralBoundedSolidOperator.numericContext.epsilon(),
            );

            //System.out.println("LMEV:");
            if (d1 !== Geometry.OUTSIDE) {
                //System.out.println("  - H1: " + tail);
                //System.out.println("  - H2: " + head);
                PolyhedralBoundedSolidEulerOperators.lmev(
                    inSolid,
                    tail,
                    head,
                    inSolid.getMaxVertexId() + 1,
                    head.startingVertex.position,
                );
                _PolyhedralBoundedSolidSplitter.sone!.push(
                    new _PolyhedralBoundedSolidSplitterNullEdge(tail.previous()!.parentEdge!),
                );
            } else {
                //System.out.println("  - H1: " + head);
                //System.out.println("  - H2: " + tail);
                PolyhedralBoundedSolidEulerOperators.lmev(
                    inSolid,
                    head,
                    tail,
                    inSolid.getMaxVertexId() + 1,
                    head.startingVertex.position,
                );
                _PolyhedralBoundedSolidSplitter.sone!.push(
                    new _PolyhedralBoundedSolidSplitterNullEdge(head.previous()!.parentEdge!),
                );
            }

            //- Locate the start of the next sequence --------------------
            while (!(
                nbr[i]!.cl === _PolyhedralBoundedSolidSplitterSectorClassification.BELOW &&
                nbr[(i + 1) % nnbr]!.cl === _PolyhedralBoundedSolidSplitterSectorClassification.ABOVE
            )) {
                i = (i + 1) % nnbr;
                if (i === start) {
                    return;
                }
            }
        }
    }

    /**
    Vertex neighborhood classifier, as presented in section [MANT1988].14.5,
    and program 14.3.

    It appears that original algorithm from [MANT1988] assumes a left handed
    geometry or orientation or other difference to current Vitral SDK
    implementation of the boundary representation. That difference implies
    a reverse order in some cases, so `inplaneEdgesOn` check is added here
    to keep current implementation's consistency.
    */
    private static splitClassify(inSolid: PolyhedralBoundedSolid, inSplittingPlane: InfinitePlane): void {
        let i: number;

        _PolyhedralBoundedSolidSplitter.sone = [];

        /// Following variable `nbr` from program [MANT1988].14.3.
        let nbr: _PolyhedralBoundedSolidSplitterSectorClassification[];

        const soov = _PolyhedralBoundedSolidSplitter.soov!;
        for (i = 0; i < soov.length; i++) {
            nbr = _PolyhedralBoundedSolidSplitter.getNeighborhood(soov[i]!, inSplittingPlane);
            if (_PolyhedralBoundedSolidSplitter.inplaneEdgesOn(nbr)) {
                Collections.reverse(nbr);
            }
            _PolyhedralBoundedSolidSplitter.reclassifyOnSectors(nbr, inSplittingPlane);
            _PolyhedralBoundedSolidSplitter.reclassifyOnEdges(nbr);
            _PolyhedralBoundedSolidSplitter.insertNullEdges(nbr, inSolid, inSplittingPlane);
        }
    }

    /**
    Following section [MANT1988].14.7.2. and program [MANT1988].14.9.
    */
    private static canJoin(he: _PolyhedralBoundedSolidHalfEdge): _PolyhedralBoundedSolidHalfEdge | null {
        let ret: _PolyhedralBoundedSolidHalfEdge;
        let i: number;
        const ends = _PolyhedralBoundedSolidSplitter.ends!;

        for (i = 0; i < ends.length; i++) {
            if (_PolyhedralBoundedSolidOperator.neighbor(he, ends[i]!)) {
                ret = ends[i]!;
                ends.splice(i, 1);
                _PolyhedralBoundedSolidSplitter.tieds!.push(ret);
                return ret;
            }
        }
        ends.push(he);
        return null;
    }

    private static printNbr(neighborSectorsInfo: readonly _PolyhedralBoundedSolidSplitterSectorClassification[]): void {
        let i: number;

        for (i = 0; i < neighborSectorsInfo.length; i++) {
            console.log("  - " + neighborSectorsInfo[i]);
        }
    }

    private static printEnds(): void {
        let i: number;
        const ends = _PolyhedralBoundedSolidSplitter.ends!;

        for (i = 0; i < ends.length; i++) {
            console.log("  - ends[" + i + "]: " + ends[i]);
        }
    }

    /**
    Following section [MANT1988].14.7.2. and program [MANT1988].14.10.
    */
    private static cut(he: _PolyhedralBoundedSolidHalfEdge): void {
        const s = he.parentLoop.parentFace.parentSolid;

        if (he.parentEdge!.rightHalf!.parentLoop === he.parentEdge!.leftHalf!.parentLoop) {
            _PolyhedralBoundedSolidSplitter.sonf!.push(he.parentLoop.parentFace);
            PolyhedralBoundedSolidEulerOperators.lkemr(s, he.parentEdge!.rightHalf!, he.parentEdge!.leftHalf!);
        } else {
            PolyhedralBoundedSolidEulerOperators.lkef(s, he.parentEdge!.rightHalf!, he.parentEdge!.leftHalf!);
        }
    }

    private static isLoose(he: _PolyhedralBoundedSolidHalfEdge | null): boolean {
        let i: number;
        const tieds = _PolyhedralBoundedSolidSplitter.tieds!;

        for (i = 0; i < tieds.length; i++) {
            if (he === tieds[i]) return false;
        }

        return true;
    }

    /**
    Following section [MANT1988].14.7.2. and program [MANT1988].14.9.
    */
    private static splitConnect(): void {
        let i: number;

        _PolyhedralBoundedSolidSplitter.ends = [];
        _PolyhedralBoundedSolidSplitter.tieds = [];

        //-----------------------------------------------------------------
        let nextedge: _PolyhedralBoundedSolidEdge;
        let h1: _PolyhedralBoundedSolidHalfEdge | null;
        let h2: _PolyhedralBoundedSolidHalfEdge | null;

        _PolyhedralBoundedSolidSplitter.sonf = [];

        const sone = _PolyhedralBoundedSolidSplitter.sone!;
        Collections.sort(sone);

        //System.out.println(sone.get(0).e.rightHalf.parentLoop.parentFace.parentSolid);

        for (i = 0; i < sone.length; i++) {
            //System.out.println("- " + i + " ---------------------------------------------------------------------");
            //System.out.println(" - " + sone.get(i).e + " / " + sone.get(i).e.rightHalf.startingVertex.position);

            nextedge = sone[i]!.e;
            //System.out.println("    . edge.rightHalf: " + nextedge.rightHalf);
            h1 = _PolyhedralBoundedSolidSplitter.canJoin(nextedge.rightHalf!);

            //System.out.println("    . h1: " + h1);

            if (h1 !== null) {
                //System.out.println("    . -> JOIN H1");
                _PolyhedralBoundedSolidOperator.join(h1, nextedge.rightHalf!, false);
                _PolyhedralBoundedSolidSplitter.tieds.push(nextedge.rightHalf!);
                if (!_PolyhedralBoundedSolidSplitter.isLoose(h1.mirrorHalfEdge())) {
                    //System.out.println("    . -> CUT H1");
                    _PolyhedralBoundedSolidSplitter.cut(h1);
                }
            }
            //System.out.println("    . edge.leftHalf: " + nextedge.leftHalf);
            h2 = _PolyhedralBoundedSolidSplitter.canJoin(nextedge.leftHalf!);

            //System.out.println("    . h2: " + h2);

            if (h2 !== null) {
                //System.out.println("    . -> JOIN H2");
                _PolyhedralBoundedSolidOperator.join(h2, nextedge.leftHalf!, false);
                _PolyhedralBoundedSolidSplitter.tieds.push(nextedge.leftHalf!);
                if (!_PolyhedralBoundedSolidSplitter.isLoose(h2.mirrorHalfEdge())) {
                    //System.out.println("    . -> CUT H2");
                    _PolyhedralBoundedSolidSplitter.cut(h2);
                }
            }
            if (h1 !== null && h2 !== null) {
                //System.out.println("    . -> CUT DUAL");
                _PolyhedralBoundedSolidSplitter.cut(nextedge.rightHalf!);
            }

            //printEnds();
        }
    }

    /**
    Following section [MANT1988].14.8. and program [MANT1988].14.12.
    */
    private static classify(
        _S: PolyhedralBoundedSolid,
        Above: PolyhedralBoundedSolid,
        Below: PolyhedralBoundedSolid,
    ): void {
        let i: number;
        _PolyhedralBoundedSolidSplitter.facesToFixAbove = [];
        _PolyhedralBoundedSolidSplitter.facesToFixBelow = [];
        const sonf = _PolyhedralBoundedSolidSplitter.sonf!;
        // Java integer division `sonf.size()/2`.
        const half = Math.trunc(sonf.length / 2);

        for (i = 0; i < half; i++) {
            _PolyhedralBoundedSolidOperator.movefac(sonf[i]!, Above);
            _PolyhedralBoundedSolidSplitter.facesToFixAbove.push(sonf[i]!);
            _PolyhedralBoundedSolidOperator.movefac(sonf[i + half]!, Below);
            _PolyhedralBoundedSolidSplitter.facesToFixBelow.push(sonf[i + half]!);
        }
    }

    private static isNullFace(f: _PolyhedralBoundedSolidFace): boolean {
        let i: number;
        const sonf = _PolyhedralBoundedSolidSplitter.sonf!;

        for (i = 0; i < sonf.length; i++) {
            if (sonf[i] === f) return true;
        }
        return false;
    }

    private static destroy(inSolid: PolyhedralBoundedSolid): void {
        inSolid.setPolygonsList(new CircularDoubleLinkedList<_PolyhedralBoundedSolidFace>());
        inSolid.setEdgesList(new CircularDoubleLinkedList<_PolyhedralBoundedSolidEdge>());
        inSolid.setVerticesList(new CircularDoubleLinkedList<_PolyhedralBoundedSolidVertex>());
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

    private static fixNullFaces(l: _PolyhedralBoundedSolidFace[]): void {
        if (l.length === 1) {
            l.splice(0, 1);
            return;
        }

        let i: number;
        let j: number;

        for (i = 0; i < l.length; i++) {
            for (j = 0; j < l.length; j++) {
                if (i === j) continue;
                if (_PolyhedralBoundedSolidSplitter.faceInsideFace(l[j]!, l[i]!)) {
                    PolyhedralBoundedSolidEulerOperators.lkfmrh(l[i]!.parentSolid, l[i]!, l[j]!);
                    l.splice(j, 1);
                    // Repeat he process with remaining list
                    _PolyhedralBoundedSolidSplitter.fixNullFaces(l);
                    return;
                }
            }
        }
    }

    /**
    Following section [MANT1988].14.8. and program [MANT1988].14.11.
    */
    private static splitFinish(
        inSolid: PolyhedralBoundedSolid,
        outSolidsAbove: PolyhedralBoundedSolid[],
        outSolidsBelow: PolyhedralBoundedSolid[],
    ): void {
        let i: number;
        let newface: _PolyhedralBoundedSolidFace;
        const sonf = _PolyhedralBoundedSolidSplitter.sonf!;

        const firstHalfSize = sonf.length;
        for (i = 0; i < firstHalfSize; i++) {
            const secondLoop: _PolyhedralBoundedSolidLoop = sonf[i]!.boundariesList.get(1)!;
            newface = PolyhedralBoundedSolidEulerOperators.lmfkrh(inSolid, secondLoop, inSolid.getMaxFaceId() + 1);
            sonf.push(newface);
        }
        const newAbove = new PolyhedralBoundedSolid();
        const newBelow = new PolyhedralBoundedSolid();
        _PolyhedralBoundedSolidSplitter.classify(inSolid, newAbove, newBelow);
        _PolyhedralBoundedSolidSplitter.fixNullFaces(_PolyhedralBoundedSolidSplitter.facesToFixAbove!);
        _PolyhedralBoundedSolidSplitter.fixNullFaces(_PolyhedralBoundedSolidSplitter.facesToFixBelow!);
        _PolyhedralBoundedSolidOperator.cleanup(newAbove);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(newAbove);
        _PolyhedralBoundedSolidOperator.cleanup(newBelow);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(newBelow);
        outSolidsAbove.push(newAbove);
        outSolidsBelow.push(newBelow);
        _PolyhedralBoundedSolidSplitter.destroy(inSolid);
    }

    /**
    Given the input `inSolid` and the cutting plane `inSplittingPlane`,
    this method appends to the `outSolidsAbove` list the solids resulting
    from cutting the solid with the plane and resulting above the plane,
    similarly, `outSolidsBelow` will be appended with solid pieces
    resulting below the plane.

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
        //-----------------------------------------------------------------
        _PolyhedralBoundedSolidOperator.setNumericContext(PolyhedralBoundedSolidNumericPolicy.forSolid(inSolid));
        _PolyhedralBoundedSolidSplitterNullEdge.setNumericContext(_PolyhedralBoundedSolidOperator.numericContext);

        PolyhedralBoundedSolidValidationEngine.validateIntermediate(inSolid);
        _PolyhedralBoundedSolidSplitter.splitGenerate(inSolid, inSplittingPlane);
        _PolyhedralBoundedSolidSplitter.splitClassify(inSolid, inSplittingPlane);

        if (_PolyhedralBoundedSolidSplitter.sone!.length <= 0) {
            // Plane should be tested here before asuming this order!
            PolyhedralBoundedSolidStatistics.recordSplitNoNullEdgesCase();
            outSolidsAbove.push(inSolid);
            outSolidsBelow.push(new PolyhedralBoundedSolid());
            PolyhedralBoundedSolidStatistics.recordSplitProducedSolids(
                outSolidsAbove.length - aboveBefore,
                outSolidsBelow.length - belowBefore,
            );
            return;
        }

        _PolyhedralBoundedSolidSplitter.splitConnect();
        _PolyhedralBoundedSolidSplitter.splitFinish(inSolid, outSolidsAbove, outSolidsBelow);
        PolyhedralBoundedSolidStatistics.recordSplitProducedSolids(
            outSolidsAbove.length - aboveBefore,
            outSolidsBelow.length - belowBefore,
        );

        //-----------------------------------------------------------------
        _PolyhedralBoundedSolidSplitter.soov = null;
        _PolyhedralBoundedSolidSplitter.sone = null;
        _PolyhedralBoundedSolidSplitter.sonf = null;
    }
}
