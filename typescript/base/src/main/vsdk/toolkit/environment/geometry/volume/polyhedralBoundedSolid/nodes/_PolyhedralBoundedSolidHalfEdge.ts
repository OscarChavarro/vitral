import { FundamentalEntity } from "../../../../../common/FundamentalEntity.js";
import { Vector3Dd } from "../../../../../common/linealAlgebra/Vector3Dd.js";
import { _PolyhedralBoundedSolidEdge } from "./_PolyhedralBoundedSolidEdge.js";
import { _PolyhedralBoundedSolidLoop } from "./_PolyhedralBoundedSolidLoop.js";
import { _PolyhedralBoundedSolidVertex } from "./_PolyhedralBoundedSolidVertex.js";
/** Directed segment within a loop; `mate` is represented by mirrorHalfEdge ([MANT1988].10.3). */
export class _PolyhedralBoundedSolidHalfEdge extends FundamentalEntity {
    public static readonly LEFT_SIDE = 1;
    public static readonly RIGHT_SIDE = 2;
    public static readonly NO_SIDE = 3;
    private static currentId = 1;
    public parentEdge: _PolyhedralBoundedSolidEdge | null = null;
    public id = _PolyhedralBoundedSolidHalfEdge.currentId++;
    public constructor(
        public startingVertex: _PolyhedralBoundedSolidVertex,
        public parentLoop: _PolyhedralBoundedSolidLoop,
    ) {
        super();
    }
    public previous(): _PolyhedralBoundedSolidHalfEdge | null {
        return this.parentLoop.halfEdgesList.previousOf(this);
    }
    public next(): _PolyhedralBoundedSolidHalfEdge | null {
        return this.parentLoop.halfEdgesList.nextOf(this);
    }
    public mirrorHalfEdge(): _PolyhedralBoundedSolidHalfEdge | null {
        if (this.parentEdge === null) return null;
        return this.parentEdge.rightHalf === this
            ? (this.parentEdge.leftHalf as _PolyhedralBoundedSolidHalfEdge | null)
            : (this.parentEdge.rightHalf as _PolyhedralBoundedSolidHalfEdge | null);
    }
    public vertexPositionMatch(other: _PolyhedralBoundedSolidHalfEdge, tolerance: number): boolean {
        return Vector3Dd.distance(this.startingVertex.position, other.startingVertex.position) <= tolerance;
    }
    public override toString(): string {
        const next = this.next();
        return `HalfEdge id ${this.id}. From vertex [${this.startingVertex.id}] to vertex [${next?.startingVertex.id ?? "?"}]. Parent face [${this.parentLoop.parentFace.id}].`;
    }
}
