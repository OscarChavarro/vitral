import { InfinitePlane } from "../../surface/InfinitePlane.js";
import { PolyhedralBoundedSolidNumericPolicy, ToleranceContext } from "./PolyhedralBoundedSolidNumericPolicy.js";
import { _PolyhedralBoundedSolidHalfEdge } from "./nodes/_PolyhedralBoundedSolidHalfEdge.js";
import { _PolyhedralBoundedSolidLoop } from "./nodes/_PolyhedralBoundedSolidLoop.js";
/** Utility predicates supporting Boolean topology operations on B-reps. */
export class _PolyhedralBoundedSolidBooleanTopologyPredicates {
    public static planesCoincidentIgnoringOrientation(
        a: InfinitePlane | null,
        b: InfinitePlane | null,
        tolerance: number,
    ): boolean {
        if (a === null || b === null) return false;
        let a1 = a.getA(),
            b1 = a.getB(),
            c1 = a.getC(),
            d1 = a.getD(),
            a2 = b.getA(),
            b2 = b.getB(),
            c2 = b.getC(),
            d2 = b.getD();
        const l1 = Math.hypot(a1, b1, c1),
            l2 = Math.hypot(a2, b2, c2);
        if (l1 <= tolerance || l2 <= tolerance) return false;
        a1 /= l1;
        b1 /= l1;
        c1 /= l1;
        d1 /= l1;
        a2 /= l2;
        b2 /= l2;
        c2 /= l2;
        d2 /= l2;
        const close = (x: number, y: number) => Math.abs(x - y) <= tolerance;
        return (
            (close(a1, a2) && close(b1, b2) && close(c1, c2) && close(d1, d2)) ||
            (close(a1, -a2) && close(b1, -b2) && close(c1, -c2) && close(d1, -d2))
        );
    }
    public static loopsCoincidentFrom(
        a: _PolyhedralBoundedSolidHalfEdge,
        b: _PolyhedralBoundedSolidHalfEdge,
        reverse: boolean,
        c: ToleranceContext,
    ): boolean {
        let x = a,
            y = b;
        do {
            if (
                !PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                    x.startingVertex.position,
                    y.startingVertex.position,
                    c,
                )
            )
                return false;
            x = x.next()!;
            y = (reverse ? y.previous() : y.next())!;
        } while (x !== a);
        return true;
    }
    public static loopsCoincident(
        a: _PolyhedralBoundedSolidLoop | null,
        b: _PolyhedralBoundedSolidLoop | null,
        c: ToleranceContext,
    ): boolean {
        if (
            a?.boundaryStartHalfEdge === null ||
            b?.boundaryStartHalfEdge === null ||
            a === null ||
            b === null ||
            a.halfEdgesList.size() !== b.halfEdgesList.size()
        )
            return false;
        for (let i = 0; i < b.halfEdgesList.size(); i++) {
            const candidate = b.halfEdgesList.get(i)!;
            if (
                PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                    a.boundaryStartHalfEdge.startingVertex.position,
                    candidate.startingVertex.position,
                    c,
                ) &&
                (this.loopsCoincidentFrom(a.boundaryStartHalfEdge, candidate, false, c) ||
                    this.loopsCoincidentFrom(a.boundaryStartHalfEdge, candidate, true, c))
            )
                return true;
        }
        return false;
    }
}
