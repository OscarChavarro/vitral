//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import type { _PolyhedralBoundedSolidHalfEdge } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import { _PolyhedralBoundedSolidOperator } from "../../_PolyhedralBoundedSolidOperator.js";

/**
This class is used to store sector / sector neigborhood information for the
vertex/vertex classifier as proposed on section [MANT1988].15.5. and program
[MANT1988].15.6.
*/
export class _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector extends _PolyhedralBoundedSolidOperator {
    public secta = 0;
    public sectb = 0;
    public s1a = 0;
    public s2a = 0;
    public s1b = 0;
    public s2b = 0;
    public intersect = false;
    public hea: _PolyhedralBoundedSolidHalfEdge | null = null;
    public heb: _PolyhedralBoundedSolidHalfEdge | null = null;
    public wa = false;
    public wb = false;
    public static readonly ON = 0;
    public static readonly OUT = 1;
    public static readonly IN = -1;

    private label(i: number): string {
        let msg = "<Unknown>";
        switch (i) {
            case _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON:
                msg = "on";
                break;
            case _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT:
                msg = "OUT";
                break;
            case _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN:
                msg = "IN";
                break;
        }
        return msg;
    }

    public fillCases(): void {
        const ON = _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON;
        const OUT = _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT;
        const IN = _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN;

        if (this.s1a === ON) {
            switch (this.s2a) {
                case IN:
                    this.s1a = OUT;
                    break;
                case OUT:
                    this.s1a = IN;
                    break;
            }
        }
        if (this.s2a === ON) {
            switch (this.s1a) {
                case IN:
                    this.s2a = OUT;
                    break;
                case OUT:
                    this.s2a = IN;
                    break;
            }
        }
        if (this.s1b === ON) {
            switch (this.s2b) {
                case IN:
                    this.s1b = OUT;
                    break;
                case OUT:
                    this.s1b = IN;
                    break;
            }
        }
        if (this.s2b === ON) {
            switch (this.s1b) {
                case IN:
                    this.s2b = OUT;
                    break;
                case OUT:
                    this.s2b = IN;
                    break;
            }
        }
    }

    public override toString(): string {
        let msg = "Sector pair ";

        msg = msg + "A[" + (this.secta + 1) + "] / B[" + (this.sectb + 1) + "]: ";

        msg =
            msg +
            "VERTICES ( " +
            this.hea!.startingVertex.id +
            "-" +
            this.hea!.next()!.startingVertex.id +
            (this.wa ? "(W)" : "(nw)") +
            " / " +
            this.heb!.startingVertex.id +
            "-" +
            this.heb!.next()!.startingVertex.id +
            (this.wb ? "(W)" : "(nw)") +
            " ) - ";
        msg =
            msg +
            "[" +
            this.label(this.s1a) +
            "/" +
            this.label(this.s2a) +
            ", " +
            this.label(this.s1b) +
            "/" +
            this.label(this.s2b) +
            "] ";
        if (this.intersect) {
            msg = msg + "intersecting";
        } else {
            msg = msg + "(droped)";
        }

        if (this.s1a !== 0 && this.s1b !== 0 && this.s2a !== 0 && this.s2b !== 0 && this.intersect) {
            msg += " (**) ";
        }

        return msg;
    }
}
