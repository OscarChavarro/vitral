import { FundamentalEntity } from "../../../../../common/FundamentalEntity.js";
import { ColorRgb } from "../../../../../common/color/ColorRgb.js";
import { Vector3Dd } from "../../../../../common/linealAlgebra/Vector3Dd.js";
/** Minimal owner contract used while the complete solid topology is assembled. */
export interface PolyhedralBoundedSolidVertexOwner {
    getVerticesList(): _PolyhedralBoundedSolidVertex[];
}
/**
 * As noted in [MANT1988].10.2.2, a vertex contains its geometric position
 * and a reference to one half-edge emanating from it.
 */
export class _PolyhedralBoundedSolidVertex extends FundamentalEntity {
    public emanatingHalfEdge: { id: number } | null = null;
    public debugColor = new ColorRgb(1, 0, 0);
    public constructor(
        public readonly parentSolid: PolyhedralBoundedSolidVertexOwner,
        public position: Vector3Dd,
        public id: number,
    ) {
        super();
        this.position = new Vector3Dd(position);
        parentSolid.getVerticesList().push(this);
    }
    public override toString(): string {
        return `vertex id ${this.id}. Position ${this.position}. ${this.emanatingHalfEdge === null ? "No associated half-edge." : `H.E. ${this.emanatingHalfEdge.id}`}`;
    }
}
