//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import type { Vector3Dd } from "../../../../../../common/linealAlgebra/Vector3Dd.js";
import type { InfinitePlane } from "../../../../surface/InfinitePlane.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import { _PolyhedralBoundedSolidOperator } from "../../_PolyhedralBoundedSolidOperator.js";

/**
This class is used to store vertex / halfedge neigborhood information for the
vertex/face classifier, in a similar fashion to as presented in section
[MANT1988].14.5, and program [MANT1988].14.3., but biased for the set
operation algorithm as proposed on section [MANT1988].15..1. and problem
[MANT1988].15.4.
*/
export class _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace extends _PolyhedralBoundedSolidOperator {
    public static readonly ABOVE = 1;
    public static readonly BELOW = -1;
    public static readonly ON = 0;

    public static readonly AinB = 11;
    public static readonly AoutB = 12;
    public static readonly BinA = 13;
    public static readonly BoutA = 14;
    public static readonly AonBplus = 15;
    public static readonly AonBminus = 16;
    public static readonly BonAplus = 17;
    public static readonly BonAminus = 18;

    public static readonly COPLANAR_FACE = 10;
    public static readonly INPLANE_EDGE = 20;
    public static readonly CROSSING_EDGE = 30;
    public static readonly UNDEFINED = 40;

    public static readonly COPLANAR_UNKNOWN = 0;
    public static readonly COPLANAR_DISJOINT = 1;
    public static readonly COPLANAR_TOUCHING = 2;
    public static readonly COPLANAR_OVERLAP = 3;

    public sector: _PolyhedralBoundedSolidHalfEdge | null = null;
    public referencePlane: InfinitePlane | null = null;
    public cl = 0;

    // Following attributes are not taken from [MANT1988], and all operations
    // on them are fine tunning options aditional to original algorithm.
    public isWide = false;
    public position: Vector3Dd | null = null;
    public situation = _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.UNDEFINED;
    public reverse = false;
    public coplanarRelation = _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.COPLANAR_UNKNOWN;

    public constructor(other?: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace) {
        super();
        if (other !== undefined) {
            this.sector = other.sector;
            this.referencePlane = other.referencePlane;
            this.cl = other.cl;
            this.isWide = other.isWide;
            this.position = other.position;
            this.situation = other.situation;
            this.reverse = other.reverse;
            this.coplanarRelation = other.coplanarRelation;
        }
    }

    /**
    Current method implements the set of changes from table [MANT1988].15.3.
    for the edge reclassification rules for the third stage of a vertex/face
    classifier.
    */
    public applyRules(op: number): void {
        const C = _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace;
        if (op === _PolyhedralBoundedSolidOperator.UNION) {
            switch (this.cl) {
                case C.AonBplus:
                    this.cl = C.AoutB;
                    break;
                case C.AonBminus:
                    this.cl = C.AinB;
                    break;
                case C.BonAplus:
                    this.cl = C.BinA;
                    break;
                case C.BonAminus:
                    this.cl = C.BinA;
                    break;
            }
        } else if (op === _PolyhedralBoundedSolidOperator.INTERSECTION) {
            switch (this.cl) {
                case C.AonBplus:
                    this.cl = C.AinB;
                    break;
                case C.AonBminus:
                    this.cl = C.AoutB;
                    break;
                case C.BonAplus:
                    this.cl = C.BoutA;
                    break;
                case C.BonAminus:
                    this.cl = C.BoutA;
                    break;
            }
        } else if (op === _PolyhedralBoundedSolidOperator.SUBTRACT) {
            switch (this.cl) {
                case C.AonBplus:
                    this.cl = C.AinB;
                    break;
                case C.AonBminus:
                    this.cl = C.AoutB;
                    break;
                case C.BonAplus:
                    this.cl = C.BoutA;
                    break;
                case C.BonAminus:
                    this.cl = C.BoutA;
                    break;
            }
        }
    }

    public updateLabel(BvsA: number): void {
        const C = _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace;
        const a = this.sector!.parentLoop.parentFace.getContainingPlane()!;
        const b = this.referencePlane!;

        if (BvsA === 0) {
            switch (this.cl) {
                case C.ABOVE:
                    this.cl = C.AoutB;
                    break;
                case C.BELOW:
                    this.cl = C.AinB;
                    break;
                case C.ON:
                    if (this.coplanarRelation !== C.COPLANAR_UNKNOWN && this.coplanarRelation !== C.COPLANAR_OVERLAP) {
                        this.cl = C.AoutB;
                    } else if (a.overlapsWith(b, _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon())) {
                        this.cl = C.AonBplus;
                    } else {
                        this.cl = C.AonBminus;
                    }
                    break;
            }
        } else {
            switch (this.cl) {
                case C.ABOVE:
                    this.cl = C.BoutA;
                    break;
                case C.BELOW:
                    this.cl = C.BinA;
                    break;
                case C.ON:
                    if (this.coplanarRelation !== C.COPLANAR_UNKNOWN && this.coplanarRelation !== C.COPLANAR_OVERLAP) {
                        this.cl = C.BoutA;
                    } else if (a.overlapsWith(b, _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon())) {
                        this.cl = C.BonAplus;
                    } else {
                        this.cl = C.BonAminus;
                    }
                    break;
            }
        }
    }

    public override toString(): string {
        const C = _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace;
        let msg = "Sector(";
        msg = msg + this.sector + " | ";
        switch (this.cl) {
            case C.ABOVE:
                msg = msg + " ABOVE";
                break;
            case C.BELOW:
                msg = msg + " BELOW";
                break;
            case C.ON:
                msg = msg + " ON";
                break;
            case C.AinB:
                msg = msg + "AinB";
                break;
            case C.AoutB:
                msg = msg + "AoutB";
                break;
            case C.BinA:
                msg = msg + "BinA";
                break;
            case C.BoutA:
                msg = msg + "BoutA";
                break;
            case C.AonBplus:
                msg = msg + "AonBplus";
                break;
            case C.AonBminus:
                msg = msg + "AonBminus";
                break;
            case C.BonAplus:
                msg = msg + "BonAplus";
                break;
            case C.BonAminus:
                msg = msg + "BonAminus";
                break;
            default:
                msg = msg + "<INVALID!>";
                break;
        }
        msg = msg + " ";
        if (this.isWide) {
            msg = msg + "(W) ";
        }
        //msg = msg + ", pos: " + position;

        switch (this.situation) {
            case C.COPLANAR_FACE:
                msg = msg + "<COPLANAR_FACE>";
                break;
            case C.INPLANE_EDGE:
                msg = msg + "<INPLANE_EDGE>";
                break;
            case C.CROSSING_EDGE:
                msg = msg + "<CROSSING_EDGE>";
                break;
            default:
                msg = msg + "<UNDEFINED>";
                break;
        }

        msg = msg + ")";
        return msg;
    }
}
