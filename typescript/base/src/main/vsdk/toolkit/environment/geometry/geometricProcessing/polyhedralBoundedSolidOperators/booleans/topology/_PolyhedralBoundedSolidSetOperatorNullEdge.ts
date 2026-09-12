import { Double } from "../../../../../../../../java/lang/Double.js";
import { Vector3Dd } from "../../../../../../common/linealAlgebra/Vector3Dd.js";
import {
    PolyhedralBoundedSolidNumericPolicy,
    type ToleranceContext,
} from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import { _PolyhedralBoundedSolidOperator } from "../../_PolyhedralBoundedSolidOperator.js";
import type { _PolyhedralBoundedSolidEdge } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.js";
export class _PolyhedralBoundedSolidSetOperatorNullEdge extends _PolyhedralBoundedSolidOperator {
    protected static override numericContext: ToleranceContext = PolyhedralBoundedSolidNumericPolicy.defaultContext();
    public constructor(public e: _PolyhedralBoundedSolidEdge) {
        super();
    }
    public static override setNumericContext(context: ToleranceContext | null): void {
        this.numericContext = context ?? PolyhedralBoundedSolidNumericPolicy.defaultContext();
    }
    /**
    Exact lexicographic comparison of two 3-D points.  Using exact double
    comparison (no epsilon band) ensures a total order so that
    {@code Collections.sort} on sonea/soneb is deterministic.  After the
    post-Generate weld pass vertices that are geometrically coincident share
    the exact same double values, so the epsilon band is no longer needed for
    correctness and only introduced non-determinism.
    */
    private static comparePoint(a: Vector3Dd, b: Vector3Dd): number {
        const cmpX = Double.compare(a.x(), b.x());
        if (cmpX !== 0) {
            return cmpX;
        }
        const cmpY = Double.compare(a.y(), b.y());
        if (cmpY !== 0) {
            return cmpY;
        }
        return Double.compare(a.z(), b.z());
    }
    private static midpoint(edge: _PolyhedralBoundedSolidEdge): Vector3Dd {
        const r = edge.rightHalf!.startingVertex.position,
            l = edge.leftHalf!.startingVertex.position;
        return new Vector3Dd((r.x() + l.x()) * 0.5, (r.y() + l.y()) * 0.5, (r.z() + l.z()) * 0.5);
    }
    private static canonicalFirstEndpoint(edge: _PolyhedralBoundedSolidEdge): Vector3Dd {
        const r = edge.rightHalf!.startingVertex.position,
            l = edge.leftHalf!.startingVertex.position;
        return this.comparePoint(r, l) <= 0 ? r : l;
    }
    private static canonicalSecondEndpoint(edge: _PolyhedralBoundedSolidEdge): Vector3Dd {
        const r = edge.rightHalf!.startingVertex.position,
            l = edge.leftHalf!.startingVertex.position;
        return this.comparePoint(r, l) <= 0 ? l : r;
    }
    public compareTo(other: _PolyhedralBoundedSolidSetOperatorNullEdge): number {
        let c = _PolyhedralBoundedSolidSetOperatorNullEdge.comparePoint(
            _PolyhedralBoundedSolidSetOperatorNullEdge.canonicalFirstEndpoint(this.e),
            _PolyhedralBoundedSolidSetOperatorNullEdge.canonicalFirstEndpoint(other.e),
        );
        if (c !== 0) return c;
        c = _PolyhedralBoundedSolidSetOperatorNullEdge.comparePoint(
            _PolyhedralBoundedSolidSetOperatorNullEdge.canonicalSecondEndpoint(this.e),
            _PolyhedralBoundedSolidSetOperatorNullEdge.canonicalSecondEndpoint(other.e),
        );
        return c !== 0
            ? c
            : _PolyhedralBoundedSolidSetOperatorNullEdge.comparePoint(
                  _PolyhedralBoundedSolidSetOperatorNullEdge.midpoint(this.e),
                  _PolyhedralBoundedSolidSetOperatorNullEdge.midpoint(other.e),
              );
    }
    public override toString(): string {
        return `${this.e} (sorted with respect to segment ${_PolyhedralBoundedSolidSetOperatorNullEdge.canonicalFirstEndpoint(this.e)} -> ${_PolyhedralBoundedSolidSetOperatorNullEdge.canonicalSecondEndpoint(this.e)})`;
    }
}
