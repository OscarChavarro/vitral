import { FundamentalEntity } from "../../../../../common/FundamentalEntity.js";
import { ColorRgb } from "../../../../../common/color/ColorRgb.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "./_PolyhedralBoundedSolidHalfEdge.js";
/** Owner protocol retained to avoid creating topology nodes outside their solid. */
export interface PolyhedralBoundedSolidEdgeOwner {
    getEdgesList(): _PolyhedralBoundedSolidEdge[];
}
/** Face-to-face identification of the two half-edge representations of a line segment ([MANT1988].10.2.2). */
export class _PolyhedralBoundedSolidEdge extends FundamentalEntity {
    private static currentId = 1;
    public rightHalf: _PolyhedralBoundedSolidHalfEdge | null = null;
    public leftHalf: _PolyhedralBoundedSolidHalfEdge | null = null;
    public id = _PolyhedralBoundedSolidEdge.currentId++;
    public debugColor = new ColorRgb(1, 1, 1);
    public constructor(parentSolid: PolyhedralBoundedSolidEdgeOwner) {
        super();
        parentSolid.getEdgesList().push(this);
    }
    public getEndingVertexId(): number {
        return this.leftHalf?.startingVertex.id ?? -1;
    }
    public getStartingVertexId(): number {
        return this.rightHalf?.startingVertex.id ?? -1;
    }
    public override toString(): string {
        return `Edge id ${this.id}. Half1: ${this.leftHalf === null ? "null." : `vertex ${this.leftHalf.startingVertex.id}`} / Half2: ${this.rightHalf === null ? "null." : `vertex ${this.rightHalf.startingVertex.id}`}`;
    }
}
