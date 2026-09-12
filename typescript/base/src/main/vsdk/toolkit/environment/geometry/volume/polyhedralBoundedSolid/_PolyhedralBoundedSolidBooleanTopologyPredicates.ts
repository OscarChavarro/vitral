//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =
//= [.wMANT2008] Mantyla Martti. "Personal Home Page", <<shar>> archive     =
//=     containing the C programs from [MANT1988]. Available at             =
//=     http://www.cs.hut.fi/~mam . Last visited April 12 / 2008.           =

import type { InfinitePlane } from "../../surface/InfinitePlane.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "./nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { _PolyhedralBoundedSolidLoop } from "./nodes/_PolyhedralBoundedSolidLoop.js";
import { PolyhedralBoundedSolidNumericPolicy, type ToleranceContext } from "./PolyhedralBoundedSolidNumericPolicy.js";

/**
Utility predicates for Boolean topology support on polyhedral bounded solids.
*/
export class _PolyhedralBoundedSolidBooleanTopologyPredicates {
    private constructor() {}

    public static planesCoincidentIgnoringOrientation(
        a: InfinitePlane | null,
        b: InfinitePlane | null,
        tolerance: number,
    ): boolean {
        let a1: number;
        let b1: number;
        let c1: number;
        let d1: number;
        let a2: number;
        let b2: number;
        let c2: number;
        let d2: number;

        if (a === null || b === null) {
            return false;
        }

        a1 = a.getA();
        b1 = a.getB();
        c1 = a.getC();
        d1 = a.getD();
        a2 = b.getA();
        b2 = b.getB();
        c2 = b.getC();
        d2 = b.getD();

        const l1 = Math.sqrt(a1 * a1 + b1 * b1 + c1 * c1);
        const l2 = Math.sqrt(a2 * a2 + b2 * b2 + c2 * c2);
        if (l1 <= tolerance || l2 <= tolerance) {
            return false;
        }

        a1 /= l1;
        b1 /= l1;
        c1 /= l1;
        d1 /= l1;
        a2 /= l2;
        b2 /= l2;
        c2 /= l2;
        d2 /= l2;

        const sameOrientation =
            Math.abs(a2 - a1) <= tolerance &&
            Math.abs(b2 - b1) <= tolerance &&
            Math.abs(c2 - c1) <= tolerance &&
            Math.abs(d2 - d1) <= tolerance;

        const oppositeOrientation =
            Math.abs(a2 + a1) <= tolerance &&
            Math.abs(b2 + b1) <= tolerance &&
            Math.abs(c2 + c1) <= tolerance &&
            Math.abs(d2 + d1) <= tolerance;

        return sameOrientation || oppositeOrientation;
    }

    public static loopsCoincidentFrom(
        startA: _PolyhedralBoundedSolidHalfEdge,
        startB: _PolyhedralBoundedSolidHalfEdge,
        reverse: boolean,
        numericContext: ToleranceContext,
    ): boolean {
        let heA: _PolyhedralBoundedSolidHalfEdge;
        let heB: _PolyhedralBoundedSolidHalfEdge;

        heA = startA;
        heB = startB;
        do {
            if (
                !PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                    heA.startingVertex.position,
                    heB.startingVertex.position,
                    numericContext,
                )
            ) {
                return false;
            }
            heA = heA.next()!;
            heB = reverse ? heB.previous()! : heB.next()!;
        } while (heA !== startA);

        return true;
    }

    public static loopsCoincident(
        a: _PolyhedralBoundedSolidLoop | null,
        b: _PolyhedralBoundedSolidLoop | null,
        numericContext: ToleranceContext,
    ): boolean {
        let i: number;
        let scanB: _PolyhedralBoundedSolidHalfEdge;

        if (a === null || b === null || a.boundaryStartHalfEdge === null || b.boundaryStartHalfEdge === null) {
            return false;
        }
        if (a.halfEdgesList.size() !== b.halfEdgesList.size()) {
            return false;
        }

        const startA = a.boundaryStartHalfEdge;
        scanB = b.boundaryStartHalfEdge;
        for (i = 0; i < b.halfEdgesList.size(); i++) {
            if (
                PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                    startA.startingVertex.position,
                    scanB.startingVertex.position,
                    numericContext,
                )
            ) {
                if (
                    _PolyhedralBoundedSolidBooleanTopologyPredicates.loopsCoincidentFrom(
                        startA,
                        scanB,
                        false,
                        numericContext,
                    ) ||
                    _PolyhedralBoundedSolidBooleanTopologyPredicates.loopsCoincidentFrom(
                        startA,
                        scanB,
                        true,
                        numericContext,
                    )
                ) {
                    return true;
                }
            }
            scanB = scanB.next()!;
        }

        return false;
    }
}
