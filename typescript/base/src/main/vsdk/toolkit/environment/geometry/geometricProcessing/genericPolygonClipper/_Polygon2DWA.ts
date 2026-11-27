import { Vertex2D } from "../../element/Vertex2D.js";
import { Polygon2D } from "../../surface/polygon/Polygon2D.js";
import { _Polygon2DContourWA } from "./_Polygon2DContourWA.js";

/** Polygon representation consumed by the Weiler--Atherton implementation. */
export class _Polygon2DWA {
    public loops: _Polygon2DContourWA[] = [];
    private currentLoop!: _Polygon2DContourWA;

    public constructor();
    public constructor(polyToCopy: Polygon2D, copyClean: boolean);
    public constructor(polyToCopy?: Polygon2D, copyClean = false) {
        if (polyToCopy === undefined) {
            this.nextLoop();
            return;
        }
        let oneOrMoreLoops = false;
        for (const contour of polyToCopy.loops) {
            oneOrMoreLoops = true;
            this.nextLoop();
            if (contour.vertices.length === 0) continue;
            if (contour.vertices.length === 1) {
                const vertex = contour.vertices[0]!;
                this.addVertex(vertex.x, vertex.y, vertex.color.r(), vertex.color.g(), vertex.color.b());
                continue;
            }
            let previousVertex = contour.vertices[contour.vertices.length - 1]!;
            let previousPreviousVertex = contour.vertices[contour.vertices.length - 2]!;
            let removeLast = false;
            let isFirst = true;
            for (const vertex of contour.vertices) {
                if (!copyClean) {
                    this.addVertex(vertex.x, vertex.y, vertex.color.r(), vertex.color.g(), vertex.color.b());
                } else if (
                    Math.abs(previousVertex.x - vertex.x) > 0.0001 ||
                    Math.abs(previousVertex.y - vertex.y) > 0.0001
                ) {
                    this.addVertex(vertex.x, vertex.y, vertex.color.r(), vertex.color.g(), vertex.color.b());
                    if (this.areCollinearAndOposite2DVectors(previousPreviousVertex, previousVertex, vertex)) {
                        if (isFirst) removeLast = true;
                        else this.currentLoop.removeVertex(this.currentLoop.vertices.size() - 2);
                    } else previousPreviousVertex = previousVertex;
                    previousVertex = vertex;
                }
                isFirst = false;
            }
            if (removeLast) this.currentLoop.removeVertex(this.currentLoop.vertices.size() - 1);
            if (this.currentLoop.vertices.size() === 0) {
                const vertex = contour.vertices[0]!;
                this.addVertex(vertex.x, vertex.y, vertex.color.r(), vertex.color.g(), vertex.color.b());
            }
        }
        if (!oneOrMoreLoops) this.nextLoop();
    }

    public addVertex(x: number, y: number, r?: number, g?: number, b?: number): void {
        this.currentLoop.addVertex(x, y, r, g, b);
    }
    public pushVertex(x: number, y: number): void {
        this.currentLoop.pushVertex(x, y);
    }
    public nextLoop(): void {
        this.currentLoop = new _Polygon2DContourWA();
        this.loops.push(this.currentLoop);
    }

    private areCollinearAndOposite2DVectors(endA: Vertex2D, startAB: Vertex2D, endB: Vertex2D): boolean {
        let ax = endA.x - startAB.x;
        let ay = endA.y - startAB.y;
        let bx = endB.x - startAB.x;
        let by = endB.y - startAB.y;
        const aLength = Math.hypot(ax, ay);
        const bLength = Math.hypot(bx, by);
        ax /= aLength;
        ay /= aLength;
        bx /= bLength;
        by /= bLength;
        const dot = ax * bx + ay * by;
        return dot > -1.0001 && dot < -0.9999;
    }
}
