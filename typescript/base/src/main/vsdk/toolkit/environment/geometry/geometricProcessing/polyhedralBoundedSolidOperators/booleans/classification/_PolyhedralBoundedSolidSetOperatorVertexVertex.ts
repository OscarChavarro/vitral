import { _PolyhedralBoundedSolidOperator } from "../../_PolyhedralBoundedSolidOperator.js";
import type { _PolyhedralBoundedSolidVertex } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";
export class _PolyhedralBoundedSolidSetOperatorVertexVertex extends _PolyhedralBoundedSolidOperator {
    public va: _PolyhedralBoundedSolidVertex | null = null;
    public vb: _PolyhedralBoundedSolidVertex | null = null;
    public override toString(): string {
        return `(${this.va}) / (${this.vb}}`;
    }
}
