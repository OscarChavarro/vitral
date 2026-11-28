import { Vertex2D } from "../../element/Vertex2D.js";
import { Polygon2D } from "../../surface/polygon/Polygon2D.js";
import type { _Polygon2DContour } from "../../surface/polygon/_Polygon2DContour.js";
import { Double } from "../../../../../../java/lang/Double.js";
import { Math as JavaMath } from "../../../../../../java/lang/Math.js";
import { HashMap } from "../../../../../../java/util/HashMap.js";
import { _HashMapIterationOrder } from "../../../../../../java/util/_HashMapIterationOrder.js";

/**
 * Post-processes polygon boolean outputs to reduce topological redundancy.
 *
 * This merger performs lightweight canonicalization:
 * - drops empty contours
 * - removes consecutive duplicate vertices
 * - removes collinear vertices
 * - removes duplicated contours (cyclic, either orientation)
 */
export class PolygonTopologicalMerger {
    private static readonly DEFAULT_EPSILON = 1e-9;

    /** Java overloads `mergeInPlace(Polygon2D)` and `mergeInPlace(Polygon2D, double)`. */
    public mergeInPlace(polygon: Polygon2D | null, epsilon: number = PolygonTopologicalMerger.DEFAULT_EPSILON): void {
        let i: number;
        let canonicalContours: Vertex2D[][];

        if (polygon === null) {
            return;
        }

        canonicalContours = [];
        for (i = 0; i < polygon.loops.length; ++i) {
            const contour = polygon.loops[i]!;
            const normalized = PolygonTopologicalMerger.normalizeContour(contour, epsilon);
            if (normalized.length < 3) {
                continue;
            }
            if (PolygonTopologicalMerger.containsEquivalentContour(canonicalContours, normalized, epsilon)) {
                continue;
            }
            canonicalContours.push(normalized);
        }
        canonicalContours = PolygonTopologicalMerger.weldInternalEdges(canonicalContours, epsilon);

        const merged = new Polygon2D();
        merged.loops.length = 0;
        for (i = 0; i < canonicalContours.length; ++i) {
            const contour = canonicalContours[i]!;
            merged.nextLoop();
            for (const v of contour) {
                merged.addVertex(v.x, v.y, v.color.r(), v.color.g(), v.color.b());
            }
        }

        if (merged.loops.length === 0) {
            merged.nextLoop();
        }

        polygon.loops = merged.loops;
    }

    private static normalizeContour(contour: _Polygon2DContour, epsilon: number): Vertex2D[] {
        let i: number;

        const input = contour.vertices;
        const withoutDuplicates: Vertex2D[] = [];
        for (i = 0; i < input.length; ++i) {
            const v = input[i]!;
            if (
                withoutDuplicates.length === 0 ||
                !PolygonTopologicalMerger.samePoint(withoutDuplicates[withoutDuplicates.length - 1]!, v, epsilon)
            ) {
                withoutDuplicates.push(PolygonTopologicalMerger.copyVertex(v));
            }
        }

        if (
            withoutDuplicates.length > 1 &&
            PolygonTopologicalMerger.samePoint(
                withoutDuplicates[0]!,
                withoutDuplicates[withoutDuplicates.length - 1]!,
                epsilon,
            )
        ) {
            withoutDuplicates.splice(withoutDuplicates.length - 1, 1);
        }

        const simplified: Vertex2D[] = [];
        for (i = 0; i < withoutDuplicates.length; ++i) {
            const prev = withoutDuplicates[(i - 1 + withoutDuplicates.length) % withoutDuplicates.length]!;
            const cur = withoutDuplicates[i]!;
            const next = withoutDuplicates[(i + 1) % withoutDuplicates.length]!;
            if (withoutDuplicates.length >= 3 && PolygonTopologicalMerger.areCollinear(prev, cur, next, epsilon)) {
                continue;
            }
            simplified.push(PolygonTopologicalMerger.copyVertex(cur));
        }

        return simplified;
    }

    private static containsEquivalentContour(contours: Vertex2D[][], candidate: Vertex2D[], epsilon: number): boolean {
        for (const existing of contours) {
            if (PolygonTopologicalMerger.areEquivalentContours(existing, candidate, epsilon)) {
                return true;
            }
        }
        return false;
    }

    private static areEquivalentContours(a: Vertex2D[], b: Vertex2D[], epsilon: number): boolean {
        let start: number;
        let i: number;

        if (a.length !== b.length) {
            return false;
        }
        const n = a.length;
        if (n === 0) {
            return true;
        }

        for (start = 0; start < n; ++start) {
            if (!PolygonTopologicalMerger.samePoint(a[0]!, b[start]!, epsilon)) {
                continue;
            }

            let forward = true;
            for (i = 0; i < n; ++i) {
                if (!PolygonTopologicalMerger.samePoint(a[i]!, b[(start + i) % n]!, epsilon)) {
                    forward = false;
                    break;
                }
            }
            if (forward) {
                return true;
            }

            let backward = true;
            for (i = 0; i < n; ++i) {
                let index = start - i;
                while (index < 0) {
                    index += n;
                }
                if (!PolygonTopologicalMerger.samePoint(a[i]!, b[index]!, epsilon)) {
                    backward = false;
                    break;
                }
            }
            if (backward) {
                return true;
            }
        }
        return false;
    }

    private static copyVertex(v: Vertex2D): Vertex2D {
        return new Vertex2D(v.x, v.y, v.color.r(), v.color.g(), v.color.b());
    }

    private static samePoint(a: Vertex2D, b: Vertex2D, epsilon: number): boolean {
        return Math.abs(a.x - b.x) <= epsilon && Math.abs(a.y - b.y) <= epsilon;
    }

    private static areCollinear(a: Vertex2D, b: Vertex2D, c: Vertex2D, epsilon: number): boolean {
        const abx = b.x - a.x;
        const aby = b.y - a.y;
        const bcx = c.x - b.x;
        const bcy = c.y - b.y;
        const cross = abx * bcy - aby * bcx;
        return Math.abs(cross) <= epsilon;
    }

    private static weldInternalEdges(contours: Vertex2D[][], epsilon: number): Vertex2D[][] {
        let i: number;
        let j: number;

        const segments: _Segment2D[] = [];
        for (i = 0; i < contours.length; ++i) {
            const contour = contours[i]!;
            for (j = 0; j < contour.length; ++j) {
                const a = contour[j]!;
                const b = contour[(j + 1) % contour.length]!;
                if (PolygonTopologicalMerger.samePoint(a, b, epsilon)) {
                    continue;
                }
                segments.push(
                    new _Segment2D(PolygonTopologicalMerger.copyVertex(a), PolygonTopologicalMerger.copyVertex(b)),
                );
            }
        }
        if (segments.length === 0) {
            return contours;
        }

        const signedUsage = new _SignedUsageMap();
        PolygonTopologicalMerger.splitAndAccumulateSegments(segments, epsilon, signedUsage);

        const boundaryEdges: _DirectedEdge[] = [];
        for (const e of signedUsage.entrySet()) {
            const balance = e.value;
            if (balance === 0) {
                continue;
            }
            if (balance > 0) {
                boundaryEdges.push(new _DirectedEdge(e.key.start, e.key.end));
            } else {
                boundaryEdges.push(new _DirectedEdge(e.key.end, e.key.start));
            }
        }
        if (boundaryEdges.length === 0) {
            return [];
        }

        return PolygonTopologicalMerger.extractLoopsFromBoundaryEdges(boundaryEdges, epsilon);
    }

    private static splitAndAccumulateSegments(
        segments: _Segment2D[],
        epsilon: number,
        signedUsage: _SignedUsageMap,
    ): void {
        let i: number;
        let j: number;
        for (i = 0; i < segments.length; ++i) {
            const seg = segments[i]!;
            const splitPoints: _SplitPoint[] = [];

            splitPoints.push(new _SplitPoint(0.0, seg.start));
            splitPoints.push(new _SplitPoint(1.0, seg.end));
            for (j = 0; j < segments.length; ++j) {
                const other = segments[j]!;
                PolygonTopologicalMerger.maybeAddPointOnSegment(seg, other.start, epsilon, splitPoints);
                PolygonTopologicalMerger.maybeAddPointOnSegment(seg, other.end, epsilon, splitPoints);
            }

            // Collections.sort is stable, as Array.prototype.sort.
            splitPoints.sort((a, b) => a.compareTo(b));
            const dedup = PolygonTopologicalMerger.dedupSplitPoints(splitPoints, epsilon);
            for (j = 0; j + 1 < dedup.length; ++j) {
                const p0 = dedup[j]!.point;
                const p1 = dedup[j + 1]!.point;
                if (PolygonTopologicalMerger.samePoint(p0, p1, epsilon)) {
                    continue;
                }
                const key = new _EdgeKey(p0, p1, epsilon);
                const delta = key.isForward(p0, epsilon) ? 1 : -1;
                const current = signedUsage.get(key);
                signedUsage.put(key, (current === undefined ? 0 : current) + delta);
            }
        }
    }

    private static maybeAddPointOnSegment(seg: _Segment2D, p: Vertex2D, epsilon: number, out: _SplitPoint[]): void {
        const t = PolygonTopologicalMerger.parameterOnSegment(seg, p, epsilon);
        if (t < -0.5) {
            return;
        }
        out.push(new _SplitPoint(t, PolygonTopologicalMerger.copyVertex(p)));
    }

    private static parameterOnSegment(seg: _Segment2D, p: Vertex2D, epsilon: number): number {
        const dx = seg.end.x - seg.start.x;
        const dy = seg.end.y - seg.start.y;
        const len2 = dx * dx + dy * dy;
        let t: number;

        if (len2 <= epsilon * epsilon) {
            return -1.0;
        }
        t = ((p.x - seg.start.x) * dx + (p.y - seg.start.y) * dy) / len2;
        if (t < -epsilon || t > 1.0 + epsilon) {
            return -1.0;
        }
        if (t < 0.0) {
            t = 0.0;
        } else if (t > 1.0) {
            t = 1.0;
        }
        const projx = seg.start.x + t * dx;
        const projy = seg.start.y + t * dy;
        const dist2 = (p.x - projx) * (p.x - projx) + (p.y - projy) * (p.y - projy);
        if (dist2 > epsilon * epsilon) {
            return -1.0;
        }
        return t;
    }

    private static dedupSplitPoints(input: _SplitPoint[], epsilon: number): _SplitPoint[] {
        let i: number;
        const out: _SplitPoint[] = [];
        for (i = 0; i < input.length; ++i) {
            const cur = input[i]!;
            if (
                out.length === 0 ||
                Math.abs(out[out.length - 1]!.segmentParameter - cur.segmentParameter) > epsilon ||
                !PolygonTopologicalMerger.samePoint(out[out.length - 1]!.point, cur.point, epsilon)
            ) {
                out.push(cur);
            }
        }
        return out;
    }

    private static extractLoopsFromBoundaryEdges(edges: _DirectedEdge[], epsilon: number): Vertex2D[][] {
        let i: number;
        const outgoing = new HashMap<_PointKey, number[]>();
        const used = new Set<number>();
        const loops: Vertex2D[][] = [];

        for (i = 0; i < edges.length; ++i) {
            const key = new _PointKey(edges[i]!.start, epsilon);
            if (!outgoing.containsKey(key)) {
                outgoing.put(key, []);
            }
            outgoing.get(key)!.push(i);
        }

        for (i = 0; i < edges.length; ++i) {
            if (used.has(i)) {
                continue;
            }
            const loop = PolygonTopologicalMerger.traceLoop(edges, i, outgoing, used, epsilon);
            if (loop.length >= 3) {
                loops.push(loop);
            }
        }
        return loops;
    }

    private static traceLoop(
        edges: _DirectedEdge[],
        startEdgeIndex: number,
        outgoing: HashMap<_PointKey, number[]>,
        used: Set<number>,
        epsilon: number,
    ): Vertex2D[] {
        const loop: Vertex2D[] = [];
        const startEdge = edges[startEdgeIndex]!;
        const start = startEdge.start;
        let current = startEdge.end;
        let prevDx = startEdge.end.x - startEdge.start.x;
        let prevDy = startEdge.end.y - startEdge.start.y;
        let guard = edges.length * 2 + 4;
        let currentEdge: number;

        used.add(startEdgeIndex);
        loop.push(PolygonTopologicalMerger.copyVertex(start));
        loop.push(PolygonTopologicalMerger.copyVertex(current));

        while (!PolygonTopologicalMerger.samePoint(current, start, epsilon) && guard > 0) {
            const key = new _PointKey(current, epsilon);
            const candidates = outgoing.get(key);
            let nextEdge = -1;
            if (candidates !== undefined) {
                nextEdge = PolygonTopologicalMerger.chooseNextEdge(edges, candidates, used, prevDx, prevDy);
            }
            if (nextEdge < 0) {
                break;
            }
            used.add(nextEdge);
            currentEdge = nextEdge;
            const e = edges[currentEdge]!;
            prevDx = e.end.x - e.start.x;
            prevDy = e.end.y - e.start.y;
            current = e.end;
            if (!PolygonTopologicalMerger.samePoint(current, start, epsilon)) {
                loop.push(PolygonTopologicalMerger.copyVertex(current));
            }
            guard--;
        }
        if (loop.length > 1 && PolygonTopologicalMerger.samePoint(loop[0]!, loop[loop.length - 1]!, epsilon)) {
            loop.splice(loop.length - 1, 1);
        }
        return loop;
    }

    private static chooseNextEdge(
        edges: _DirectedEdge[],
        candidates: number[],
        used: Set<number>,
        prevDx: number,
        prevDy: number,
    ): number {
        let i: number;
        let selected = -1;
        let bestAngle = Double.MAX_VALUE;

        for (i = 0; i < candidates.length; ++i) {
            const idx = candidates[i]!;
            if (used.has(idx)) {
                continue;
            }
            const e = edges[idx]!;
            const dx = e.end.x - e.start.x;
            const dy = e.end.y - e.start.y;
            const cross = prevDx * dy - prevDy * dx;
            const dot = prevDx * dx + prevDy * dy;
            let angle = Math.atan2(cross, dot);
            if (angle <= 0.0) {
                angle += JavaMath.PI * 2.0;
            }
            if (angle < bestAngle) {
                bestAngle = angle;
                selected = idx;
            }
        }
        return selected;
    }
}

class _Segment2D {
    public constructor(
        public readonly start: Vertex2D,
        public readonly end: Vertex2D,
    ) {}
}

class _SplitPoint {
    public constructor(
        public readonly segmentParameter: number,
        public readonly point: Vertex2D,
    ) {}

    public compareTo(other: _SplitPoint): number {
        return Double.compare(this.segmentParameter, other.segmentParameter);
    }
}

class _DirectedEdge {
    public constructor(
        public readonly start: Vertex2D,
        public readonly end: Vertex2D,
    ) {}
}

/** `PolygonTopologicalMerger.copyVertex`, which Java's nested `EdgeKey` calls. */
function copyVertex(v: Vertex2D): Vertex2D {
    return new Vertex2D(v.x, v.y, v.color.r(), v.color.g(), v.color.b());
}

/** Java `Long.hashCode(long)`: `(int)(value ^ (value >>> 32))`. */
function longHashCode(value: number): number {
    const v = BigInt(value);
    return Number(BigInt.asIntN(32, v ^ (BigInt.asUintN(64, v) >> 32n)));
}

class _PointKey {
    public readonly quantizedX: number;
    public readonly quantizedY: number;

    public constructor(p: Vertex2D, epsilon: number) {
        this.quantizedX = JavaMath.round(p.x / epsilon);
        this.quantizedY = JavaMath.round(p.y / epsilon);
    }

    public hashCode(): number {
        let h = 17;
        h = (Math.imul(31, h) + longHashCode(this.quantizedX)) | 0;
        h = (Math.imul(31, h) + longHashCode(this.quantizedY)) | 0;
        return h;
    }

    public equals(obj: unknown): boolean {
        if (this === obj) {
            return true;
        }
        if (!(obj instanceof _PointKey)) {
            return false;
        }
        const other = obj;
        return this.quantizedX === other.quantizedX && this.quantizedY === other.quantizedY;
    }
}

class _EdgeKey {
    public readonly start: Vertex2D;
    public readonly end: Vertex2D;
    public readonly startKey: _PointKey;
    public readonly endKey: _PointKey;

    public constructor(p0: Vertex2D, p1: Vertex2D, epsilon: number) {
        const k0 = new _PointKey(p0, epsilon);
        const k1 = new _PointKey(p1, epsilon);
        if (_EdgeKey.compareKeys(k0, k1) <= 0) {
            this.start = copyVertex(p0);
            this.end = copyVertex(p1);
            this.startKey = k0;
            this.endKey = k1;
        } else {
            this.start = copyVertex(p1);
            this.end = copyVertex(p0);
            this.startKey = k1;
            this.endKey = k0;
        }
    }

    public isForward(candidate: Vertex2D, epsilon: number): boolean {
        return Math.abs(this.start.x - candidate.x) <= epsilon && Math.abs(this.start.y - candidate.y) <= epsilon;
    }

    public hashCode(): number {
        let h = 17;
        h = (Math.imul(31, h) + this.startKey.hashCode()) | 0;
        h = (Math.imul(31, h) + this.endKey.hashCode()) | 0;
        return h;
    }

    public equals(obj: unknown): boolean {
        if (this === obj) {
            return true;
        }
        if (!(obj instanceof _EdgeKey)) {
            return false;
        }
        const other = obj;
        return this.startKey.equals(other.startKey) && this.endKey.equals(other.endKey);
    }

    private static compareKeys(a: _PointKey, b: _PointKey): number {
        if (a.quantizedX < b.quantizedX) {
            return -1;
        }
        if (a.quantizedX > b.quantizedX) {
            return 1;
        }
        if (a.quantizedY < b.quantizedY) {
            return -1;
        }
        if (a.quantizedY > b.quantizedY) {
            return 1;
        }
        return 0;
    }
}

/**
Java's `HashMap<EdgeKey, Integer> signedUsage`: lookups through the package
`HashMap` (`hashCode`/`equals` of `_EdgeKey`), and `entrySet` in the order a
JVM iterates the table, which decides the order of the boundary edges and so
the first vertex and the order of the merged contours.
*/
class _SignedUsageMap {
    private readonly map = new HashMap<_EdgeKey, number>();
    private readonly keys: _EdgeKey[] = [];
    private readonly order = new _HashMapIterationOrder();

    public get(key: _EdgeKey): number | undefined {
        return this.map.get(key);
    }

    public put(key: _EdgeKey, value: number): void {
        if (!this.map.containsKey(key)) {
            this.keys.push(key);
            this.order.add(key.hashCode());
        }
        this.map.put(key, value);
    }

    public entrySet(): { key: _EdgeKey; value: number }[] {
        return this.order.order().map((index) => {
            const key = this.keys[index]!;
            return { key, value: this.map.get(key)! };
        });
    }
}
