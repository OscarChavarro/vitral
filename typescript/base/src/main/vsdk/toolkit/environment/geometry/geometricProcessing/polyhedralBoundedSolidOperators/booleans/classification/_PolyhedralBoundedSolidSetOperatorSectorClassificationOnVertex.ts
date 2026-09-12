//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import { Vector3Dd } from "../../../../../../common/linealAlgebra/Vector3Dd.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import { _PolyhedralBoundedSolidOperator } from "../../_PolyhedralBoundedSolidOperator.js";
import { _PolyhedralBoundedSolidSetOperator } from "../_PolyhedralBoundedSolidSetOperator.js";

/**
This class is used to store vertex / halfedge neigborhood information for the
vertex/vertex classifier as proposed on section [MANT1988].15.5. and program
[MANT1988].15.6.
*/
export class _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex extends _PolyhedralBoundedSolidOperator {
    public he: _PolyhedralBoundedSolidHalfEdge | null = null;
    public ref1: Vector3Dd | null = null;
    public ref2: Vector3Dd | null = null;
    public ref12: Vector3Dd | null = null;
    public referenceLine: Vector3Dd | null;
    public referenceU: Vector3Dd | null;
    public referenceV: Vector3Dd | null;
    public wide = false;

    public constructor() {
        super();
        this.referenceLine = null;
        this.referenceU = null;
        this.referenceV = null;
    }

    public getAngle(): number {
        if (this.referenceLine === null || this.referenceU === null || this.referenceV === null) {
            return -1000;
        }

        let a = this.ref1!;

        if (_PolyhedralBoundedSolidSetOperator.colinearVectorsWithDirection(this.ref1!, this.referenceLine)) {
            a = this.ref2!;
        }

        let u: Vector3Dd;
        let v: Vector3Dd;

        u = new Vector3Dd(this.referenceU);
        u = u.normalized();
        v = new Vector3Dd(this.referenceV);
        v = v.normalized();
        a = a.normalized();

        const x = a.dotProduct(u);
        const y = a.dotProduct(v);

        let an = Math.acos(x);
        if (y < 0) an *= -1;

        return an;
    }

    public override toString(): string {
        let msg: string;

        msg =
            "R1: " +
            this.ref1 +
            " R2: " +
            this.ref2 +
            " HE " +
            this.he!.startingVertex.id +
            "/" +
            this.he!.next()!.startingVertex.id +
            (this.wide ? "(W)" : "(nw)");

        return msg;
    }

    public compareTo(other: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex): number {
        const a = this.getAngle();
        const b = other.getAngle();

        if (a > b) return 1;
        if (a < b) return -1;
        return 0;
    }
}
