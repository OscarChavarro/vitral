import { FundamentalEntity } from "../../../../../common/FundamentalEntity.js";
import { Vector3Dd } from "../../../../../common/linealAlgebra/Vector3Dd.js";
import { InfinitePlane } from "../../../surface/InfinitePlane.js";
import { PolyhedralBoundedSolidNumericPolicy } from "../PolyhedralBoundedSolidNumericPolicy.js";
import { _PolyhedralBoundedSolidHalfEdge } from "./_PolyhedralBoundedSolidHalfEdge.js";
import { _PolyhedralBoundedSolidLoop } from "./_PolyhedralBoundedSolidLoop.js";
import { _PolyhedralBoundedSolidVertex } from "./_PolyhedralBoundedSolidVertex.js";
export interface PolyhedralBoundedSolidFaceOwner {
    getPolygonsList(): _PolyhedralBoundedSolidFace[];
}
/** Planar, possibly concave face with an outer loop followed by hole loops ([MANT1988].10.2.1). */
export class _PolyhedralBoundedSolidFace extends FundamentalEntity {
    public readonly boundariesList: _PolyhedralBoundedSolidLoop[] = [];
    public constructor(
        public parentSolid: PolyhedralBoundedSolidFaceOwner,
        public id: number,
    ) {
        super();
        parentSolid.getPolygonsList().push(this);
    }
    public findHalfEdge(a: number, b?: number): _PolyhedralBoundedSolidHalfEdge | null {
        for (const loop of this.boundariesList) {
            const hit = b === undefined ? loop.firstHalfEdgeAtVertex(a) : loop.halfEdgeVertices(a, b);
            if (hit !== null) return hit;
        }
        return null;
    }
    public calculatePlane(): boolean {
        return this.getContainingPlane() === null;
    }
    public getContainingPlane(): InfinitePlane | null {
        const loop = this.largestLoop();
        if (loop === null || loop.halfEdgesList.size() < 3) return null;
        let nx = 0,
            ny = 0,
            nz = 0,
            cx = 0,
            cy = 0,
            cz = 0;
        for (let i = 0; i < loop.halfEdgesList.size(); i++) {
            const p = loop.halfEdgesList.get(i)!.startingVertex.position,
                q = loop.halfEdgesList.get((i + 1) % loop.halfEdgesList.size())!.startingVertex.position;
            nx += (p.y() - q.y()) * (p.z() + q.z());
            ny += (p.z() - q.z()) * (p.x() + q.x());
            nz += (p.x() - q.x()) * (p.y() + q.y());
            cx += p.x();
            cy += p.y();
            cz += p.z();
        }
        const n = new Vector3Dd(nx, ny, nz),
            centroid = new Vector3Dd(
                cx / loop.halfEdgesList.size(),
                cy / loop.halfEdgesList.size(),
                cz / loop.halfEdgesList.size(),
            ),
            tolerance = PolyhedralBoundedSolidNumericPolicy.forFace(this).bigEpsilon();
        if (n.length() > tolerance) return new InfinitePlane(n, centroid);

        /**
         * Newell normals cancel on an open-wire representation that traverses
         * every segment forward and backward. Java falls back to its corner
         * construction in this case; retain that behavior so lmef/lkef inverse
         * sequences still recover their containing plane.
         */
        const points = Array.from(
            { length: loop.halfEdgesList.size() },
            (_, index) => loop.halfEdgesList.get(index)!.startingVertex.position,
        );
        for (let first = 0; first < points.length; first++)
            for (let second = first + 1; second < points.length; second++)
                for (let third = second + 1; third < points.length; third++) {
                    const cornerNormal = points[second]!.subtract(points[first]!).crossProduct(
                        points[third]!.subtract(points[first]!),
                    );
                    if (cornerNormal.length() > tolerance) return new InfinitePlane(cornerNormal, points[first]!);
                }
        return null;
    }
    public testPointInside(
        point: Vector3Dd,
        tolerance: number,
        plane: InfinitePlane | null = this.getContainingPlane(),
    ): number {
        return this.testPointInsideDetailed(point, tolerance, plane).status();
    }
    public testPointInsideDetailed(
        point: Vector3Dd,
        tolerance: number,
        plane: InfinitePlane | null = this.getContainingPlane(),
    ): PointInsideResult {
        if (plane === null) return new PointInsideResult(0, null, null);
        const n = plane.getNormal();
        const drop =
            Math.abs(n.x()) >= Math.abs(n.y()) && Math.abs(n.x()) >= Math.abs(n.z())
                ? 0
                : Math.abs(n.y()) >= Math.abs(n.z())
                  ? 1
                  : 2;
        let crossings = 0;
        for (const loop of this.boundariesList) {
            const count = loop.halfEdgesList.size();
            if (count === 0) return new PointInsideResult(0, null, null);
            for (let i = 0; i < count; i++) {
                const edge = loop.halfEdgesList.get(i)!,
                    next = loop.halfEdgesList.get((i + 1) % count)!,
                    a = this.project(edge.startingVertex.position, drop),
                    b = this.project(next.startingVertex.position, drop),
                    p = this.project(point, drop);
                if (Vector3Dd.distance(point, edge.startingVertex.position) < 2 * tolerance)
                    return new PointInsideResult(2, null, edge.startingVertex);
                const cross = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x);
                const dot = (p.x - a.x) * (p.x - b.x) + (p.y - a.y) * (p.y - b.y);
                if (Math.abs(cross) <= tolerance && dot <= tolerance) return new PointInsideResult(2, edge, null);
                if (a.y > p.y !== b.y > p.y && p.x < ((b.x - a.x) * (p.y - a.y)) / (b.y - a.y) + a.x) crossings++;
            }
        }
        return new PointInsideResult(crossings % 2 === 1 ? 1 : 0, null, null);
    }
    public revert(): void {
        for (const loop of this.boundariesList) loop.revert();
    }
    public override toString(): string {
        return `Face id [${this.id}], ${this.boundariesList.length} loops.`;
    }
    private project(p: Vector3Dd, drop: number): { x: number; y: number } {
        return drop === 0 ? { x: p.y(), y: p.z() } : drop === 1 ? { x: p.x(), y: p.z() } : { x: p.x(), y: p.y() };
    }
    private largestLoop(): _PolyhedralBoundedSolidLoop | null {
        let best: _PolyhedralBoundedSolidLoop | null = null,
            area = -1;
        for (const loop of this.boundariesList) {
            let sum = 0;
            for (let i = 0; i < loop.halfEdgesList.size(); i++) {
                const a = loop.halfEdgesList.get(i)!.startingVertex.position,
                    b = loop.halfEdgesList.get((i + 1) % loop.halfEdgesList.size())!.startingVertex.position;
                sum += a.crossProduct(b).length();
            }
            if (sum > area) {
                area = sum;
                best = loop;
            }
        }
        return best;
    }
}
export class PointInsideResult {
    public constructor(
        private readonly result: number,
        private readonly edge: _PolyhedralBoundedSolidHalfEdge | null,
        private readonly vertex: _PolyhedralBoundedSolidVertex | null,
    ) {}
    public status(): number {
        return this.result;
    }
    public intersectedHalfedge(): _PolyhedralBoundedSolidHalfEdge | null {
        return this.edge;
    }
    public intersectedVertex(): _PolyhedralBoundedSolidVertex | null {
        return this.vertex;
    }
}
