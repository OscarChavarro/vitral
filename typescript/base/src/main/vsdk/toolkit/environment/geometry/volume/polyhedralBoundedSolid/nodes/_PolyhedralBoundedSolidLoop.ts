import { FundamentalEntity } from "../../../../../common/FundamentalEntity.js";
import { CircularDoubleLinkedList } from "../../../../../common/dataStructures/CircularDoubleLinkedList.js";
import { _PolyhedralBoundedSolidHalfEdge } from "./_PolyhedralBoundedSolidHalfEdge.js";
/** Structural face contract; the concrete face is introduced after loop connectivity. */
export interface PolyhedralBoundedSolidLoopFace {
    id: number;
    boundariesList: _PolyhedralBoundedSolidLoop[];
    getContainingPlane: () => {
        getNormal: () => import("../../../../../common/linealAlgebra/Vector3Dd.js").Vector3Dd;
    } | null;
}
/** A connected boundary of a face, represented by a circular half-edge list ([MANT1988].10.2.1). */
export class _PolyhedralBoundedSolidLoop extends FundamentalEntity {
    public boundaryStartHalfEdge: _PolyhedralBoundedSolidHalfEdge | null = null;
    public halfEdgesList = new CircularDoubleLinkedList<_PolyhedralBoundedSolidHalfEdge>();
    public constructor(public parentFace: PolyhedralBoundedSolidLoopFace) {
        super();
        parentFace.boundariesList.push(this);
    }
    public unlistHalfEdge(edge: _PolyhedralBoundedSolidHalfEdge): void {
        if (this.halfEdgesList.locateWindowAtElem(edge)) this.halfEdgesList.removeElemAtWindow();
        this.boundaryStartHalfEdge = this.halfEdgesList.size() > 0 ? this.halfEdgesList.get(0) : null;
    }
    public halfEdgeVertices(a: number, b: number): _PolyhedralBoundedSolidHalfEdge | null {
        return this.find((edge) => edge.startingVertex.id === a && edge.next()?.startingVertex.id === b);
    }
    public firstHalfEdgeAtVertex(id: number): _PolyhedralBoundedSolidHalfEdge | null {
        return this.find((edge) => edge.startingVertex.id === id);
    }
    public delhe(edge: _PolyhedralBoundedSolidHalfEdge): void {
        this.unlistHalfEdge(edge);
    }
    public revert(): void {
        if (this.halfEdgesList.size() <= 1) return;
        const edges: _PolyhedralBoundedSolidHalfEdge["parentEdge"][] = [];
        for (let i = 0; i < this.halfEdgesList.size(); i++) {
            const edge = this.halfEdgesList.get(i)!;
            if (edge.parentEdge === null) return;
            edges.push(edge.parentEdge);
        }
        for (let i = 0; i < this.halfEdgesList.size(); i++) {
            const edge = this.halfEdgesList.get(i)!;
            const parent = edges[(i + edges.length - 1) % edges.length]!;
            edge.parentEdge = parent;
            if (parent.rightHalf === edge) parent.rightHalf = edge;
            else parent.leftHalf = edge;
        }
        this.halfEdgesList.reverse();
        this.boundaryStartHalfEdge = this.halfEdgesList.get(0);
    }
    public override toString(): string {
        return `Loop, parent face ${this.parentFace.id}`;
    }
    private find(
        predicate: (edge: _PolyhedralBoundedSolidHalfEdge) => boolean,
    ): _PolyhedralBoundedSolidHalfEdge | null {
        for (let i = 0; i < this.halfEdgesList.size(); i++) {
            const edge = this.halfEdgesList.get(i)!;
            if (predicate(edge)) return edge;
        }
        return null;
    }
}
