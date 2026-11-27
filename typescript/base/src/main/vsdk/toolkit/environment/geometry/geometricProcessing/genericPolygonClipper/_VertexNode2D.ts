import { ColorRgb } from "../../../../common/color/ColorRgb.js";
import { _DoubleLinkedListNode } from "./_DoubleLinkedListNode.js";

/** Mutable clipping vertex, including the cross-polygon intersection link. */
export class _VertexNode2D {
    public x: number;
    public y: number;
    public color: ColorRgb;
    public flags: number;
    public pairNode: _DoubleLinkedListNode<_VertexNode2D> | null;

    public constructor();
    public constructor(x: number, y: number);
    public constructor(x: number, y: number, r: number, g: number, b: number);
    public constructor(other: _VertexNode2D);
    public constructor(a: number | _VertexNode2D = 0, b = 0, r?: number, g?: number, blue?: number) {
        if (a instanceof _VertexNode2D) {
            this.x = a.x;
            this.y = a.y;
            this.color = new ColorRgb(a.color);
            this.flags = a.flags;
            this.pairNode = a.pairNode;
            return;
        }
        this.x = a;
        this.y = b;
        this.color = r === undefined ? new ColorRgb() : new ColorRgb(r, g!, blue!);
        this.flags = 0;
        this.pairNode = null;
    }
}
