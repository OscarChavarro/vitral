import { _CircularDoubleLinkedList } from "./_CircularDoubleLinkedList.js";
import { _VertexNode2D } from "./_VertexNode2D.js";

/** Weiler--Atherton contour representation. */
export class _Polygon2DContourWA {
    public vertices = new _CircularDoubleLinkedList<_VertexNode2D>();
    public isClipped = false;
    public isHole = false;

    public addVertex(x: number, y: number, r?: number, g?: number, b?: number): void {
        this.vertices.add(r === undefined ? new _VertexNode2D(x, y) : new _VertexNode2D(x, y, r, g!, b!));
    }
    public removeVertex(ind: number): void {
        this.vertices.remove(ind);
    }
    public pushVertex(x: number, y: number): void {
        this.vertices.add(0, new _VertexNode2D(x, y));
    }
}
