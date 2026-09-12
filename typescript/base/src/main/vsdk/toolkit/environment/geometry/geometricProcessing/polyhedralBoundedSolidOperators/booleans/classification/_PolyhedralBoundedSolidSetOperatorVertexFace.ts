import { _PolyhedralBoundedSolidOperator } from "../../_PolyhedralBoundedSolidOperator.js";
import type { _PolyhedralBoundedSolidFace } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidVertex } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";
export class _PolyhedralBoundedSolidSetOperatorVertexFace extends _PolyhedralBoundedSolidOperator {
    public v: _PolyhedralBoundedSolidVertex | null = null;
    public f: _PolyhedralBoundedSolidFace | null = null;
    public override toString(): string {
        return `{${this.v} / ${this.f}}`;
    }
}
