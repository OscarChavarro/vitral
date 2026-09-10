import { Vertex2D } from "../../element/Vertex2D.js";
import { Polygon2D } from "../../surface/polygon/Polygon2D.js";
import { _Polygon2DContour } from "../../surface/polygon/_Polygon2DContour.js";

/** Canonicalizes polygon-boolean output and welds internal shared edges. */
export class PolygonTopologicalMerger {
    private static readonly DEFAULT_EPSILON = 1e-9;

    public mergeInPlace(polygon: Polygon2D, epsilon = PolygonTopologicalMerger.DEFAULT_EPSILON): void {
        const contours: Vertex2D[][] = [];
        for (const contour of polygon.loops) {
            const normalized = PolygonTopologicalMerger.normalizeContour(contour, epsilon);
            if (
                normalized.length >= 3 &&
                !PolygonTopologicalMerger.containsEquivalentContour(contours, normalized, epsilon)
            )
                contours.push(normalized);
        }
        const welded = PolygonTopologicalMerger.weldInternalEdges(contours, epsilon);
        const merged = new Polygon2D();
        merged.loops.length = 0;
        for (const contour of welded) {
            merged.nextLoop();
            for (const vertex of contour)
                merged.addVertex(vertex.x, vertex.y, vertex.color.r(), vertex.color.g(), vertex.color.b());
        }
        if (merged.loops.length === 0) merged.nextLoop();
        polygon.loops = merged.loops;
    }

    private static normalizeContour(contour: _Polygon2DContour, epsilon: number): Vertex2D[] {
        const withoutDuplicates: Vertex2D[] = [];
        for (const vertex of contour.vertices) {
            if (withoutDuplicates.length === 0 || !this.samePoint(withoutDuplicates.at(-1)!, vertex, epsilon))
                withoutDuplicates.push(this.copyVertex(vertex));
        }
        if (withoutDuplicates.length > 1 && this.samePoint(withoutDuplicates[0]!, withoutDuplicates.at(-1)!, epsilon))
            withoutDuplicates.pop();
        return withoutDuplicates
            .filter((vertex, i, vertices) => {
                if (vertices.length < 3) return true;
                return !this.areCollinear(
                    vertices[(i - 1 + vertices.length) % vertices.length]!,
                    vertex,
                    vertices[(i + 1) % vertices.length]!,
                    epsilon,
                );
            })
            .map(this.copyVertex);
    }
    private static containsEquivalentContour(
        contours: readonly Vertex2D[][],
        candidate: Vertex2D[],
        epsilon: number,
    ): boolean {
        return contours.some((contour) => this.areEquivalentContours(contour, candidate, epsilon));
    }
    private static areEquivalentContours(a: readonly Vertex2D[], b: readonly Vertex2D[], epsilon: number): boolean {
        if (a.length !== b.length) return false;
        if (a.length === 0) return true;
        for (let start = 0; start < a.length; start++) {
            if (!this.samePoint(a[0]!, b[start]!, epsilon)) continue;
            if (a.every((point, i) => this.samePoint(point, b[(start + i) % a.length]!, epsilon))) return true;
            if (a.every((point, i) => this.samePoint(point, b[(start - i + a.length) % a.length]!, epsilon)))
                return true;
        }
        return false;
    }
    private static copyVertex(vertex: Vertex2D): Vertex2D {
        return new Vertex2D(vertex.x, vertex.y, vertex.color.r(), vertex.color.g(), vertex.color.b());
    }
    private static samePoint(a: Vertex2D, b: Vertex2D, epsilon: number): boolean {
        return Math.abs(a.x - b.x) <= epsilon && Math.abs(a.y - b.y) <= epsilon;
    }
    private static areCollinear(a: Vertex2D, b: Vertex2D, c: Vertex2D, epsilon: number): boolean {
        return Math.abs((b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x)) <= epsilon;
    }
    private static weldInternalEdges(contours: Vertex2D[][], epsilon: number): Vertex2D[][] {
        const segments: Segment2D[] = [];
        for (const contour of contours)
            for (let i = 0; i < contour.length; i++) {
                const a = contour[i]!;
                const b = contour[(i + 1) % contour.length]!;
                if (!this.samePoint(a, b, epsilon)) segments.push({ a: this.copyVertex(a), b: this.copyVertex(b) });
            }
        if (segments.length === 0) return contours;
        const signedUsage = new Map<string, { key: EdgeKey; balance: number }>();
        for (const segment of segments) {
            const splits: SplitPoint[] = [
                { t: 0, p: segment.a },
                { t: 1, p: segment.b },
            ];
            for (const other of segments) {
                this.maybeAddPointOnSegment(segment, other.a, epsilon, splits);
                this.maybeAddPointOnSegment(segment, other.b, epsilon, splits);
            }
            splits.sort((a, b) => a.t - b.t);
            const dedup = this.dedupSplitPoints(splits, epsilon);
            for (let i = 0; i + 1 < dedup.length; i++) {
                const p0 = dedup[i]!.p;
                const p1 = dedup[i + 1]!.p;
                if (this.samePoint(p0, p1, epsilon)) continue;
                const key = new EdgeKey(p0, p1, epsilon);
                const id = key.id;
                const entry = signedUsage.get(id) ?? { key, balance: 0 };
                entry.balance += key.isForward(p0, epsilon) ? 1 : -1;
                signedUsage.set(id, entry);
            }
        }
        const edges: DirectedEdge[] = [];
        for (const { key, balance } of signedUsage.values()) {
            if (balance > 0) edges.push({ start: key.a, end: key.b });
            else if (balance < 0) edges.push({ start: key.b, end: key.a });
        }
        return edges.length === 0 ? [] : this.extractLoopsFromBoundaryEdges(edges, epsilon);
    }
    private static maybeAddPointOnSegment(
        segment: Segment2D,
        point: Vertex2D,
        epsilon: number,
        output: SplitPoint[],
    ): void {
        const t = this.parameterOnSegment(segment, point, epsilon);
        if (t >= -0.5) output.push({ t, p: this.copyVertex(point) });
    }
    private static parameterOnSegment(segment: Segment2D, point: Vertex2D, epsilon: number): number {
        const dx = segment.b.x - segment.a.x,
            dy = segment.b.y - segment.a.y,
            len2 = dx * dx + dy * dy;
        if (len2 <= epsilon * epsilon) return -1;
        let t = ((point.x - segment.a.x) * dx + (point.y - segment.a.y) * dy) / len2;
        if (t < -epsilon || t > 1 + epsilon) return -1;
        t = Math.max(0, Math.min(1, t));
        const x = segment.a.x + t * dx,
            y = segment.a.y + t * dy;
        return (point.x - x) ** 2 + (point.y - y) ** 2 > epsilon * epsilon ? -1 : t;
    }
    private static dedupSplitPoints(points: SplitPoint[], epsilon: number): SplitPoint[] {
        return points.filter(
            (point, i) =>
                i === 0 ||
                Math.abs(points[i - 1]!.t - point.t) > epsilon ||
                !this.samePoint(points[i - 1]!.p, point.p, epsilon),
        );
    }
    private static extractLoopsFromBoundaryEdges(edges: DirectedEdge[], epsilon: number): Vertex2D[][] {
        const outgoing = new Map<string, number[]>();
        edges.forEach((edge, i) => {
            const key = PointKey.of(edge.start, epsilon);
            outgoing.set(key, [...(outgoing.get(key) ?? []), i]);
        });
        const used = new Set<number>();
        const loops: Vertex2D[][] = [];
        for (let i = 0; i < edges.length; i++)
            if (!used.has(i)) {
                const loop = this.traceLoop(edges, i, outgoing, used, epsilon);
                if (loop.length >= 3) loops.push(loop);
            }
        return loops;
    }
    private static traceLoop(
        edges: DirectedEdge[],
        startIndex: number,
        outgoing: Map<string, number[]>,
        used: Set<number>,
        epsilon: number,
    ): Vertex2D[] {
        const startEdge = edges[startIndex]!;
        const start = startEdge.start;
        let current = startEdge.end,
            previousDx = current.x - start.x,
            previousDy = current.y - start.y,
            guard = edges.length * 2 + 4;
        const loop = [this.copyVertex(start), this.copyVertex(current)];
        used.add(startIndex);
        while (!this.samePoint(current, start, epsilon) && guard-- > 0) {
            const next = this.chooseNextEdge(
                edges,
                outgoing.get(PointKey.of(current, epsilon)) ?? [],
                used,
                previousDx,
                previousDy,
            );
            if (next < 0) break;
            used.add(next);
            const edge = edges[next]!;
            previousDx = edge.end.x - edge.start.x;
            previousDy = edge.end.y - edge.start.y;
            current = edge.end;
            if (!this.samePoint(current, start, epsilon)) loop.push(this.copyVertex(current));
        }
        if (loop.length > 1 && this.samePoint(loop[0]!, loop.at(-1)!, epsilon)) loop.pop();
        return loop;
    }
    private static chooseNextEdge(
        edges: DirectedEdge[],
        candidates: number[],
        used: Set<number>,
        previousDx: number,
        previousDy: number,
    ): number {
        let selected = -1,
            bestAngle = Number.MAX_VALUE;
        for (const index of candidates)
            if (!used.has(index)) {
                const edge = edges[index]!;
                const cross = previousDx * (edge.end.y - edge.start.y) - previousDy * (edge.end.x - edge.start.x);
                const dot = previousDx * (edge.end.x - edge.start.x) + previousDy * (edge.end.y - edge.start.y);
                let angle = Math.atan2(cross, dot);
                if (angle <= 0) angle += Math.PI * 2;
                if (angle < bestAngle) {
                    bestAngle = angle;
                    selected = index;
                }
            }
        return selected;
    }
}
interface Segment2D {
    a: Vertex2D;
    b: Vertex2D;
}
interface SplitPoint {
    t: number;
    p: Vertex2D;
}
interface DirectedEdge {
    start: Vertex2D;
    end: Vertex2D;
}
class PointKey {
    public static of(point: Vertex2D, epsilon: number): string {
        return `${Math.round(point.x / epsilon)},${Math.round(point.y / epsilon)}`;
    }
}
class EdgeKey {
    public readonly a: Vertex2D;
    public readonly b: Vertex2D;
    public readonly id: string;
    public constructor(p0: Vertex2D, p1: Vertex2D, epsilon: number) {
        const k0 = PointKey.of(p0, epsilon),
            k1 = PointKey.of(p1, epsilon);
        [this.a, this.b, this.id] =
            k0 <= k1
                ? [
                      new Vertex2D(p0.x, p0.y, p0.color.r(), p0.color.g(), p0.color.b()),
                      new Vertex2D(p1.x, p1.y, p1.color.r(), p1.color.g(), p1.color.b()),
                      `${k0}|${k1}`,
                  ]
                : [
                      new Vertex2D(p1.x, p1.y, p1.color.r(), p1.color.g(), p1.color.b()),
                      new Vertex2D(p0.x, p0.y, p0.color.r(), p0.color.g(), p0.color.b()),
                      `${k1}|${k0}`,
                  ];
    }
    public isForward(start: Vertex2D, epsilon: number): boolean {
        return Math.abs(this.a.x - start.x) <= epsilon && Math.abs(this.a.y - start.y) <= epsilon;
    }
}
