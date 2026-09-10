import { VSDK } from "../../../../common/VSDK.js";
import { Vector2Dd } from "../../../../common/linealAlgebra/Vector2Dd.js";
import { Vector3Dd } from "../../../../common/linealAlgebra/Vector3Dd.js";
import type { PolyhedralBoundedSolid } from "./PolyhedralBoundedSolid.js";
import type { _PolyhedralBoundedSolidFace } from "./nodes/_PolyhedralBoundedSolidFace.js";

/**
 * Numerical tolerances for geometric predicates used to implement the face
 * equations and intersection predicates of chapter [MANT1988].13 and the robust
 * case distinctions required by chapter [MANT1988].15.
 */
export class PolyhedralBoundedSolidNumericPolicy {
    public static readonly BREP_EPSILON = VSDK.EPSILON;
    public static readonly BREP_BIG_EPSILON = 10 * VSDK.EPSILON;
    public static defaultContext(): ToleranceContext {
        return PolyhedralBoundedSolidNumericPolicy.fromScale(1);
    }
    public static fromScale(scale: number): ToleranceContext {
        const s = Number.isFinite(scale) && scale >= 1 ? scale : 1,
            epsilon = this.BREP_EPSILON * s,
            bigEpsilon = this.BREP_BIG_EPSILON * s;
        return new ToleranceContext(
            s,
            epsilon,
            bigEpsilon,
            this.BREP_BIG_EPSILON,
            this.BREP_BIG_EPSILON,
            10 * this.BREP_BIG_EPSILON,
            Math.min(1e-3, Math.max(this.BREP_BIG_EPSILON, bigEpsilon / s)),
        );
    }
    public static forPoints(points: readonly Vector3Dd[]): ToleranceContext {
        if (points.length < 2) return this.defaultContext();
        let minx = Infinity,
            miny = Infinity,
            minz = Infinity,
            maxx = -Infinity,
            maxy = -Infinity,
            maxz = -Infinity;
        for (const p of points) {
            minx = Math.min(minx, p.x());
            miny = Math.min(miny, p.y());
            minz = Math.min(minz, p.z());
            maxx = Math.max(maxx, p.x());
            maxy = Math.max(maxy, p.y());
            maxz = Math.max(maxz, p.z());
        }
        return this.fromScale(Math.hypot(maxx - minx, maxy - miny, maxz - minz));
    }
    public static forSolid(solid: PolyhedralBoundedSolid | null): ToleranceContext {
        return solid === null
            ? this.defaultContext()
            : this.forPoints(solid.getVerticesList().map((vertex) => vertex.position));
    }
    public static forSolids(
        first: PolyhedralBoundedSolid | null,
        second: PolyhedralBoundedSolid | null,
    ): ToleranceContext {
        return this.fromScale(Math.max(this.forSolid(first).modelScale(), this.forSolid(second).modelScale()));
    }
    public static forFace(face: _PolyhedralBoundedSolidFace | null): ToleranceContext {
        if (face === null) return this.defaultContext();
        const points: Vector3Dd[] = [];
        for (const loop of face.boundariesList)
            for (let i = 0; i < loop.halfEdgesList.size(); i++)
                points.push(loop.halfEdgesList.get(i)!.startingVertex.position);
        return this.forPoints(points);
    }
    public static compare(a: number, b: number, tolerance: number | ToleranceContext): number {
        const t = typeof tolerance === "number" ? tolerance : tolerance.epsilon();
        return a < b - t ? -1 : a > b + t ? 1 : 0;
    }
    public static compareToZero(value: number, c: ToleranceContext): number {
        return this.compare(value, 0, c.epsilon());
    }
    public static compareToZeroBig(value: number, c: ToleranceContext): number {
        return this.compare(value, 0, c.bigEpsilon());
    }
    public static isZero(value: number, c: ToleranceContext): boolean {
        return Math.abs(value) <= c.epsilon();
    }
    public static isZeroBig(value: number, c: ToleranceContext): boolean {
        return Math.abs(value) <= c.bigEpsilon();
    }
    public static pointsCoincident(a: Vector3Dd, b: Vector3Dd, c: ToleranceContext): boolean {
        return Vector3Dd.distance(a, b) <= c.bigEpsilon();
    }
    public static pointsSeparated(a: Vector3Dd, b: Vector3Dd, c: ToleranceContext): boolean {
        return !this.pointsCoincident(a, b, c);
    }
    public static testPointInside(face: _PolyhedralBoundedSolidFace, point: Vector3Dd, c: ToleranceContext): number {
        return face.testPointInside(point, c.bigEpsilon());
    }
    public static vectorsColinear(a: Vector3Dd, b: Vector3Dd, c: ToleranceContext): boolean {
        return a.crossProduct(b).length() <= c.bigEpsilon() * Math.max(1, a.length(), b.length());
    }
    public static unitVectorsParallel(a: Vector3Dd, b: Vector3Dd, c: ToleranceContext): boolean {
        return a.crossProduct(b).length() <= c.unitVectorTolerance();
    }
    public static angleIntervalsOverlap(upper: number, lower: number, c: ToleranceContext): boolean {
        return upper + c.angleTolerance() > lower - c.angleTolerance();
    }
    public static unitIntervalContainsStrictly(t: number, c: ToleranceContext): boolean {
        return t > c.unitIntervalTolerance() && t < 1 - c.unitIntervalTolerance();
    }
    public static orientationTolerance2D(a: Vector2Dd, b: Vector2Dd, c: Vector2Dd, context: ToleranceContext): number {
        const span = Math.max(
            1,
            Math.abs(a.x - b.x),
            Math.abs(a.x - c.x),
            Math.abs(b.x - c.x),
            Math.abs(a.y - b.y),
            Math.abs(a.y - c.y),
            Math.abs(b.y - c.y),
        );
        return context.bigEpsilon() * span;
    }
    public static linearTolerance2D(c: ToleranceContext): number {
        return c.bigEpsilon();
    }
    public static areaTolerance2D(c: ToleranceContext): number {
        return c.bigEpsilon() ** 2;
    }
}
export class ToleranceContext {
    public constructor(
        private readonly scale: number,
        private readonly eps: number,
        private readonly bigEps: number,
        private readonly unitTol: number,
        private readonly angleTol: number,
        private readonly coplanarTol: number,
        private readonly intervalTol: number,
    ) {}
    public modelScale(): number {
        return this.scale;
    }
    public epsilon(): number {
        return this.eps;
    }
    public bigEpsilon(): number {
        return this.bigEps;
    }
    public unitVectorTolerance(): number {
        return this.unitTol;
    }
    public angleTolerance(): number {
        return this.angleTol;
    }
    public coplanarDotTolerance(): number {
        return this.coplanarTol;
    }
    public unitIntervalTolerance(): number {
        return this.intervalTol;
    }
}
