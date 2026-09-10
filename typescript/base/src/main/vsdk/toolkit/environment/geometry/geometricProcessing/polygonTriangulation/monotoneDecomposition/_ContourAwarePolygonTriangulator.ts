import { Polygon2D } from "../../../surface/polygon/Polygon2D.js";
import { MonotoneDecompositionTriangulator } from "../MonotoneDecompositionTriangulator.js";
import { _ContourData } from "./_ContourData.js";
import { _IndexedVertex } from "./_IndexedVertex.js";

export class _ContourAwarePolygonTriangulator {
    private static readonly EPSILON = 1e-9;
    private constructor() {}

    public static triangulate(input: Polygon2D, output: MonotoneDecompositionTriangulator.Triangle[]): number {
        const contours = this.flattenContours(input);
        if (contours.length === 0) {
            output.length = 0;
            return 0;
        }
        this.classifyContourHierarchy(contours);
        const triangles: MonotoneDecompositionTriangulator.Triangle[] = [];
        for (let i = 0; i < contours.length; i++) {
            const contour = contours[i]!;
            if (contour.depth % 2 === 0) continue;
            const holes = contour.childContours.map((child) => this.copyVertices(contours[child]!.vertices));
            if (!this.triangulateFilledRegion(this.copyVertices(contour.vertices), holes, triangles)) return -1;
        }
        output.length = 0;
        output.push(...triangles);
        return output.length;
    }
    private static flattenContours(input: Polygon2D): _ContourData[] {
        const result: _ContourData[] = [];
        let flattened = 0;
        for (const loop of input.loops) {
            if (loop.vertices.length === 0) continue;
            const contour = new _ContourData();
            for (const vertex of loop.vertices)
                contour.vertices.push(new _IndexedVertex(vertex.x, vertex.y, flattened++));
            contour.signedArea = this.signedArea(contour.vertices);
            if (Math.abs(contour.signedArea) > this.EPSILON) result.push(contour);
        }
        return result;
    }
    private static classifyContourHierarchy(contours: _ContourData[]): void {
        for (let i = 0; i < contours.length; i++) {
            const contour = contours[i]!,
                probe = contour.vertices[0]!;
            let best = Infinity;
            for (let j = 0; j < contours.length; j++) {
                if (i === j) continue;
                const candidate = contours[j]!,
                    area = Math.abs(candidate.signedArea);
                if (area <= Math.abs(contour.signedArea) + this.EPSILON) continue;
                if (this.containsPoint(candidate.vertices, probe.x, probe.y) && area < best) {
                    best = area;
                    contour.parentContour = j;
                }
            }
        }
        for (let i = 0; i < contours.length; i++) this.computeDepth(contours, i);
        for (let i = 0; i < contours.length; i++)
            if (contours[i]!.parentContour >= 0) contours[contours[i]!.parentContour]!.childContours.push(i);
    }
    private static computeDepth(contours: _ContourData[], index: number): number {
        const contour = contours[index]!;
        if (contour.depth >= 0) return contour.depth;
        contour.depth = contour.parentContour < 0 ? 1 : this.computeDepth(contours, contour.parentContour) + 1;
        return contour.depth;
    }
    private static triangulateFilledRegion(
        outer: _IndexedVertex[],
        holes: _IndexedVertex[][],
        output: MonotoneDecompositionTriangulator.Triangle[],
    ): boolean {
        this.normalizeOrientation(outer, true);
        for (const hole of holes) this.normalizeOrientation(hole, false);
        let polygon = this.removeConsecutiveDuplicates(outer);
        if (polygon.length < 3) return true;
        const remaining = [...holes];
        while (remaining.length > 0) {
            const index = this.findRightmostHoleIndex(remaining),
                hole = remaining.splice(index, 1)[0]!;
            const merged = this.bridgeHoleIntoPolygon(polygon, hole, remaining);
            if (merged === null) return false;
            polygon = merged;
        }
        return this.earClipSimplePolygon(polygon, output);
    }
    private static findRightmostHoleIndex(holes: _IndexedVertex[][]): number {
        let best = 0,
            vertex: _IndexedVertex | null = null;
        for (let i = 0; i < holes.length; i++) {
            const candidate = holes[i]![this.findRightmostVertexIndex(holes[i]!)]!;
            if (
                vertex === null ||
                candidate.x > vertex.x + this.EPSILON ||
                (Math.abs(candidate.x - vertex.x) <= this.EPSILON && candidate.y < vertex.y)
            ) {
                vertex = candidate;
                best = i;
            }
        }
        return best;
    }
    private static bridgeHoleIntoPolygon(
        polygon: _IndexedVertex[],
        sourceHole: _IndexedVertex[],
        remaining: _IndexedVertex[][],
    ): _IndexedVertex[] | null {
        const hole = this.removeConsecutiveDuplicates(sourceHole);
        if (hole.length < 3) return polygon;
        const h = this.findRightmostVertexIndex(hole),
            p = this.findVisibleBridgeVertex(polygon, hole, h, remaining);
        if (p < 0) return null;
        return this.removeConsecutiveDuplicates([
            ...polygon.slice(0, p + 1),
            hole[h]!,
            ...hole.slice(h + 1),
            ...hole.slice(0, h),
            hole[h]!,
            ...polygon.slice(p),
        ]);
    }
    private static findVisibleBridgeVertex(
        polygon: _IndexedVertex[],
        hole: _IndexedVertex[],
        h: number,
        remaining: _IndexedVertex[][],
    ): number {
        const a = hole[h]!;
        let best = -1,
            distance = Infinity;
        for (let i = 0; i < polygon.length; i++) {
            const b = polygon[i]!;
            if (this.segmentCrossesBoundary(a, b, polygon, i, hole, h)) continue;
            if (remaining.some((other) => this.segmentCrossesAnyEdge(a, b, other, -1))) continue;
            const x = (a.x + b.x) / 2,
                y = (a.y + b.y) / 2;
            if (!this.containsPoint(polygon, x, y) || this.containsPoint(hole, x, y)) continue;
            const d = this.squaredDistance(a, b);
            if (d < distance) {
                distance = d;
                best = i;
            }
        }
        return best;
    }
    private static segmentCrossesBoundary(
        a: _IndexedVertex,
        b: _IndexedVertex,
        polygon: _IndexedVertex[],
        p: number,
        hole: _IndexedVertex[],
        h: number,
    ): boolean {
        return this.segmentCrossesAnyEdge(a, b, polygon, p) || this.segmentCrossesAnyEdge(a, b, hole, h);
    }
    private static segmentCrossesAnyEdge(
        a: _IndexedVertex,
        b: _IndexedVertex,
        boundary: _IndexedVertex[],
        allowed: number,
    ): boolean {
        for (let i = 0; i < boundary.length; i++) {
            const start = boundary[i]!,
                end = boundary[(i + 1) % boundary.length]!;
            if (
                allowed >= 0 &&
                (start.originalIndex === boundary[allowed]!.originalIndex ||
                    end.originalIndex === boundary[allowed]!.originalIndex)
            )
                continue;
            if (this.segmentsIntersectProperly(a, b, start, end)) return true;
        }
        return false;
    }
    private static earClipSimplePolygon(
        polygon: _IndexedVertex[],
        output: MonotoneDecompositionTriangulator.Triangle[],
    ): boolean {
        const work = this.removeConsecutiveDuplicates(polygon);
        this.simplifyCollinearVertices(work);
        if (work.length < 3) return true;
        if (this.signedArea(work) < 0) work.reverse();
        let guard = work.length * work.length;
        while (work.length > 3 && guard-- > 0) {
            let clipped = false;
            for (let i = 0; i < work.length; i++) {
                const previous = work[(i + work.length - 1) % work.length]!,
                    current = work[i]!,
                    next = work[(i + 1) % work.length]!;
                if (
                    this.cross(previous, current, next) <= this.EPSILON ||
                    this.hasRepeatedVertex(previous, current, next)
                )
                    continue;
                let contains = false;
                for (let j = 0; j < work.length; j++) {
                    if (j === i || j === (i + 1) % work.length || j === (i + work.length - 1) % work.length) continue;
                    if (this.pointInTriangle(previous, current, next, work[j]!)) {
                        contains = true;
                        break;
                    }
                }
                if (contains) continue;
                output.push(
                    new MonotoneDecompositionTriangulator.Triangle(
                        previous.originalIndex,
                        current.originalIndex,
                        next.originalIndex,
                    ),
                );
                work.splice(i, 1);
                this.simplifyCollinearVertices(work);
                clipped = true;
                break;
            }
            if (!clipped) return false;
        }
        if (
            work.length === 3 &&
            !this.hasRepeatedVertex(work[0]!, work[1]!, work[2]!) &&
            Math.abs(this.cross(work[0]!, work[1]!, work[2]!)) > this.EPSILON
        ) {
            output.push(
                new MonotoneDecompositionTriangulator.Triangle(
                    work[0]!.originalIndex,
                    work[1]!.originalIndex,
                    work[2]!.originalIndex,
                ),
            );
            return true;
        }
        return work.length < 3;
    }
    private static hasRepeatedVertex(a: _IndexedVertex, b: _IndexedVertex, c: _IndexedVertex): boolean {
        return (
            a.originalIndex === b.originalIndex ||
            b.originalIndex === c.originalIndex ||
            a.originalIndex === c.originalIndex
        );
    }
    private static simplifyCollinearVertices(polygon: _IndexedVertex[]): void {
        let removed: boolean;
        do {
            removed = false;
            if (polygon.length <= 3) return;
            for (let i = 0; i < polygon.length; i++) {
                const a = polygon[(i + polygon.length - 1) % polygon.length]!,
                    b = polygon[i]!,
                    c = polygon[(i + 1) % polygon.length]!;
                if (this.samePoint(a, b) || this.samePoint(b, c)) {
                    polygon.splice(i, 1);
                    removed = true;
                    break;
                }
            }
        } while (removed);
    }
    private static removeConsecutiveDuplicates(polygon: _IndexedVertex[]): _IndexedVertex[] {
        const result: _IndexedVertex[] = [];
        for (const vertex of polygon)
            if (result.length === 0 || !this.samePoint(result[result.length - 1]!, vertex)) result.push(vertex);
        if (result.length > 1 && this.samePoint(result[0]!, result[result.length - 1]!)) result.pop();
        return result;
    }
    private static findRightmostVertexIndex(contour: _IndexedVertex[]): number {
        let best = 0;
        for (let i = 1; i < contour.length; i++)
            if (
                contour[i]!.x > contour[best]!.x + this.EPSILON ||
                (Math.abs(contour[i]!.x - contour[best]!.x) <= this.EPSILON && contour[i]!.y < contour[best]!.y)
            )
                best = i;
        return best;
    }
    private static normalizeOrientation(vertices: _IndexedVertex[], ccw: boolean): void {
        if ((ccw && this.signedArea(vertices) < 0) || (!ccw && this.signedArea(vertices) > 0)) vertices.reverse();
    }
    private static copyVertices(source: _IndexedVertex[]): _IndexedVertex[] {
        return [...source];
    }
    private static signedArea(vertices: _IndexedVertex[]): number {
        let area = 0;
        for (let i = 0; i < vertices.length; i++) {
            const next = vertices[(i + 1) % vertices.length]!;
            area += vertices[i]!.x * next.y - next.x * vertices[i]!.y;
        }
        return area / 2;
    }
    private static containsPoint(contour: _IndexedVertex[], x: number, y: number): boolean {
        let inside = false;
        for (let i = 0, j = contour.length - 1; i < contour.length; j = i++) {
            const a = contour[i]!,
                b = contour[j]!;
            if (this.pointOnSegment(x, y, a, b)) return true;
            if (a.y > y !== b.y > y && x < ((b.x - a.x) * (y - a.y)) / (b.y - a.y) + a.x) inside = !inside;
        }
        return inside;
    }
    private static pointOnSegment(x: number, y: number, a: _IndexedVertex, b: _IndexedVertex): boolean {
        const area = (b.x - a.x) * (y - a.y) - (b.y - a.y) * (x - a.x);
        return (
            Math.abs(area) <= this.EPSILON &&
            x >= Math.min(a.x, b.x) - this.EPSILON &&
            x <= Math.max(a.x, b.x) + this.EPSILON &&
            y >= Math.min(a.y, b.y) - this.EPSILON &&
            y <= Math.max(a.y, b.y) + this.EPSILON
        );
    }
    private static pointInTriangle(
        a: _IndexedVertex,
        b: _IndexedVertex,
        c: _IndexedVertex,
        p: _IndexedVertex,
    ): boolean {
        if (this.samePoint(a, p) || this.samePoint(b, p) || this.samePoint(c, p)) return false;
        const ab = this.cross(a, b, p),
            bc = this.cross(b, c, p),
            ca = this.cross(c, a, p);
        return !(
            (ab < -this.EPSILON || bc < -this.EPSILON || ca < -this.EPSILON) &&
            (ab > this.EPSILON || bc > this.EPSILON || ca > this.EPSILON)
        );
    }
    private static cross(a: _IndexedVertex, b: _IndexedVertex, c: _IndexedVertex): number {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }
    private static squaredDistance(a: _IndexedVertex, b: _IndexedVertex): number {
        const x = a.x - b.x,
            y = a.y - b.y;
        return x * x + y * y;
    }
    private static samePoint(a: _IndexedVertex, b: _IndexedVertex): boolean {
        return Math.abs(a.x - b.x) <= this.EPSILON && Math.abs(a.y - b.y) <= this.EPSILON;
    }
    private static segmentsIntersectProperly(
        a: _IndexedVertex,
        b: _IndexedVertex,
        c: _IndexedVertex,
        d: _IndexedVertex,
    ): boolean {
        if (this.samePoint(a, c) || this.samePoint(a, d) || this.samePoint(b, c) || this.samePoint(b, d)) return false;
        const ac = this.cross(a, b, c),
            ad = this.cross(a, b, d),
            ca = this.cross(c, d, a),
            cb = this.cross(c, d, b);
        if ([ac, ad, ca, cb].every((x) => Math.abs(x) <= this.EPSILON))
            return this.rangesOverlap(a.x, b.x, c.x, d.x) && this.rangesOverlap(a.y, b.y, c.y, d.y);
        return (
            ((ac > this.EPSILON && ad < -this.EPSILON) || (ac < -this.EPSILON && ad > this.EPSILON)) &&
            ((ca > this.EPSILON && cb < -this.EPSILON) || (ca < -this.EPSILON && cb > this.EPSILON))
        );
    }
    private static rangesOverlap(a0: number, a1: number, b0: number, b1: number): boolean {
        return (
            Math.max(Math.min(a0, a1), Math.min(b0, b1)) <= Math.min(Math.max(a0, a1), Math.max(b0, b1)) + this.EPSILON
        );
    }
}
