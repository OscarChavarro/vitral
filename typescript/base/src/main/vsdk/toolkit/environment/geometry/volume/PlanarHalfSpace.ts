//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import { Double } from "../../../../../java/lang/Double.js";
import { IllegalStateException } from "../../../../../java/lang/IllegalStateException.js";
import { Math as JavaMath } from "../../../../../java/lang/Math.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { FloatingInterval } from "../../../common/numerics/FloatingInterval.js";
import type { GeometryBounds } from "../element/GeometryBounds.js";
import { GeometryContainment } from "../element/GeometryContainment.js";
import type { Ray } from "../element/Ray.js";
import { RayBoundary } from "../element/RayBoundary.js";
import { RayCrossing } from "../element/RayCrossing.js";
import { RayCrossingKind } from "../element/RayCrossingKind.js";
import { RayCrossingQuery } from "../element/RayCrossingQuery.js";
import type { RayCrossingSink } from "../element/RayCrossingSink.js";
import { RayCrossingSupport } from "../element/RayCrossingSupport.js";
import { RayFirstHitStatus } from "../element/RayFirstHitStatus.js";
import { RayHit } from "../element/RayHit.js";
import { RaySolidBoundary } from "../element/RaySolidBoundary.js";
import type { RaySolidClassification } from "../element/RaySolidClassification.js";
import { RaySolidQueryStatus } from "../element/RaySolidQueryStatus.js";
import type { Geometry } from "../Geometry.js";
import { InfinitePlane } from "../surface/InfinitePlane.js";
import type { PolyhedralBoundedSolid } from "./polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import { Box } from "./Box.js";
import { Solid } from "./Solid.js";

// The closed solid n dot p + d <= 0. The plane owns its mathematical
// coefficients; placement and complement are independent scene operations.
export class PlanarHalfSpace extends Solid {
    private plane: InfinitePlane;

    public constructor(a: number, b: number, c: number, d: number);
    public constructor(boundary: InfinitePlane);
    public constructor(a: number | InfinitePlane, b?: number, c?: number, d?: number) {
        super();
        this.plane = a instanceof InfinitePlane ? new InfinitePlane(a) : new InfinitePlane(a, b!, c!, d!);
        this.addControlSpecifications();
    }

    /**
    The state is the equation ax + by + cz + d = 0 of the boundary plane, with
    its raw (not normalized) coefficients. Any real value is valid for each
    one, but (a, b, c) must not be all zero.
    */
    private addControlSpecifications(): void {
        this.getControlSpecifications().push("double;a;(-INFINITE, INFINITE)");
        this.getControlSpecifications().push("double;b;(-INFINITE, INFINITE)");
        this.getControlSpecifications().push("double;c;(-INFINITE, INFINITE)");
        this.getControlSpecifications().push("double;d;(-INFINITE, INFINITE)");
    }

    public getA(): number {
        return this.plane.getA();
    }

    public getB(): number {
        return this.plane.getB();
    }

    public getC(): number {
        return this.plane.getC();
    }

    public getD(): number {
        return this.plane.getD();
    }

    public setA(a: number): void {
        this.plane.setA(a);
    }

    public setB(b: number): void {
        this.plane.setB(b);
    }

    public setC(c: number): void {
        this.plane.setC(c);
    }

    public setD(d: number): void {
        this.plane.setD(d);
    }

    public override copyGeometry(): Geometry | null {
        return new PlanarHalfSpace(this.plane);
    }

    public getBoundaryPlane(): InfinitePlane {
        return this.plane;
    }

    public override getBounds(result: GeometryBounds): void {
        this.plane.getBounds(result);
    }

    public override getMinMax(): Float64Array | number[] {
        return this.plane.getMinMax();
    }

    public override classifyBoxCertified(center: Vector3Dd, halfWidths: Vector3Dd): GeometryContainment {
        const a = this.plane.getA(),
            b = this.plane.getB(),
            c = this.plane.getC(),
            d = this.plane.getD();
        if (
            !Double.isFinite(a) ||
            !Double.isFinite(b) ||
            !Double.isFinite(c) ||
            !Double.isFinite(d) ||
            (a === 0 && b === 0 && c === 0)
        )
            return GeometryContainment.InvalidInput;
        if (!FloatingInterval.isSupportedEnvironment()) return GeometryContainment.Unsupported;
        const x = FloatingInterval.around(center.x(), halfWidths.x());
        const y = FloatingInterval.around(center.y(), halfWidths.y());
        const z = FloatingInterval.around(center.z(), halfWidths.z());
        if (!x.isValid() || !y.isValid() || !z.isValid()) return GeometryContainment.InvalidInput;
        const value = FloatingInterval.exact(a)
            .multiply(x)
            .add(FloatingInterval.exact(b).multiply(y))
            .add(FloatingInterval.exact(c).multiply(z))
            .add(FloatingInterval.exact(d));
        if (!value.isValid()) return GeometryContainment.Indeterminate;
        if (value.getUpper() < 0) return GeometryContainment.Inside;
        if (value.getLower() > 0) return GeometryContainment.Outside;
        return GeometryContainment.Indeterminate;
    }

    public override classifyPointCertified(point: Vector3Dd, tolerance: number): GeometryContainment {
        const a = this.plane.getA(),
            b = this.plane.getB(),
            c = this.plane.getC(),
            d = this.plane.getD();
        if (
            !Double.isFinite(a) ||
            !Double.isFinite(b) ||
            !Double.isFinite(c) ||
            !Double.isFinite(d) ||
            (a === 0 && b === 0 && c === 0) ||
            !Double.isFinite(point.x()) ||
            !Double.isFinite(point.y()) ||
            !Double.isFinite(point.z()) ||
            !Double.isFinite(tolerance) ||
            tolerance < 0
        )
            return GeometryContainment.InvalidInput;
        if (!FloatingInterval.isSupportedEnvironment()) return GeometryContainment.Unsupported;
        const A = FloatingInterval.exact(a),
            B = FloatingInterval.exact(b),
            C = FloatingInterval.exact(c);
        const value = A.multiply(FloatingInterval.exact(point.x()))
            .add(B.multiply(FloatingInterval.exact(point.y())))
            .add(C.multiply(FloatingInterval.exact(point.z())))
            .add(FloatingInterval.exact(d));
        const norm = A.square().add(B.square()).add(C.square()).squareRoot();
        if (!value.isValid() || !norm.isValid() || !(norm.getLower() > 0)) return GeometryContainment.Indeterminate;
        const signedDistance = value.divide(norm);
        if (!signedDistance.isValid()) return GeometryContainment.Indeterminate;
        if (signedDistance.getUpper() < -tolerance) return GeometryContainment.Inside;
        if (signedDistance.getLower() > tolerance) return GeometryContainment.Outside;
        if (signedDistance.getLower() >= -tolerance && signedDistance.getUpper() <= tolerance)
            return GeometryContainment.Boundary;
        return GeometryContainment.Indeterminate;
    }

    public override classifyPoint(point: Vector3Dd, tolerance: number): GeometryContainment {
        const a = this.plane.getA(),
            b = this.plane.getB(),
            c = this.plane.getC(),
            d = this.plane.getD();
        if (
            !Double.isFinite(a) ||
            !Double.isFinite(b) ||
            !Double.isFinite(c) ||
            !Double.isFinite(d) ||
            (a === 0 && b === 0 && c === 0) ||
            !Double.isFinite(point.x()) ||
            !Double.isFinite(point.y()) ||
            !Double.isFinite(point.z()) ||
            !Double.isFinite(tolerance) ||
            tolerance < 0
        )
            return GeometryContainment.InvalidInput;
        if (tolerance > 0) return this.classifyPointCertified(point, tolerance);
        const squared = a * a + b * b + c * c;
        const value = this.plane.pointDistance(point);
        if (!Double.isFinite(squared) || squared === 0 || !Double.isFinite(value))
            return GeometryContainment.Indeterminate;
        if (
            value === 0 &&
            ((a !== 0 && point.x() !== 0 && a * point.x() === 0) ||
                (b !== 0 && point.y() !== 0 && b * point.y() === 0) ||
                (c !== 0 && point.z() !== 0 && c * point.z() === 0))
        )
            return GeometryContainment.Indeterminate;
        const signedDistance = value / JavaMath.sqrt(squared);
        if (!Double.isFinite(signedDistance) || (value !== 0 && signedDistance === 0))
            return GeometryContainment.Indeterminate;
        if (signedDistance < -tolerance) return GeometryContainment.Inside;
        if (signedDistance > tolerance) return GeometryContainment.Outside;
        return GeometryContainment.Boundary;
    }

    public override doContainmentTest(point: Vector3Dd, tolerance: number): number {
        const result = this.classifyPoint(point, tolerance);
        if (result === GeometryContainment.Inside) return PlanarHalfSpace.INSIDE;
        if (result === GeometryContainment.Outside) return PlanarHalfSpace.OUTSIDE;
        if (result === GeometryContainment.Boundary) return PlanarHalfSpace.LIMIT;
        throw new IllegalStateException("Planar half-space containment is indeterminate or invalid");
    }

    public override queryFirstHit(
        ray: Ray,
        query: RayCrossingQuery,
        hit: RayHit | null,
        boundary: RayBoundary | null = null,
    ): RayFirstHitStatus {
        if (boundary) boundary.assign(new RayBoundary());
        const planeBoundary = new RayBoundary();
        const status = this.plane.queryFirstHit(ray, query, hit, boundary ? planeBoundary : null);
        if (status === RayFirstHitStatus.Hit && boundary) boundary.assign(new RayBoundary(planeBoundary.t, this));
        return status;
    }

    public override querySurfaceDetail(ray: Ray, boundary: RayBoundary, hit: RayHit | null): RayFirstHitStatus {
        if (hit) hit.clear();
        if (!hit || boundary.geometry !== this || !Double.isFinite(boundary.t)) return RayFirstHitStatus.InvalidInput;
        if (boundary.getInstanceCount() !== 0) return RayFirstHitStatus.Unsupported;
        return this.plane.querySurfaceDetail(
            ray,
            new RayBoundary(boundary.t, this.plane, boundary.normalReversed),
            hit,
        );
    }

    public override emitRayCrossings(ray: Ray, query: RayCrossingQuery, sink: RayCrossingSink): RayCrossingSupport {
        const hit = new RayHit(RayHit.DETAIL_NONE, false);
        const status = this.plane.queryFirstHit(ray, query, hit);
        if (status === RayFirstHitStatus.Unsupported) return RayCrossingSupport.Unsupported;
        if (status === RayFirstHitStatus.InvalidInput) return RayCrossingSupport.InvalidInput;
        if (status === RayFirstHitStatus.Indeterminate) return RayCrossingSupport.Indeterminate;
        if (status === RayFirstHitStatus.Hit) {
            const normal = new Vector3Dd(this.plane.getA(), this.plane.getB(), this.plane.getC());
            const slope = normal.dotProduct(ray.getDirection());
            if (!Double.isFinite(slope) || slope === 0) return RayCrossingSupport.Indeterminate;
            sink.crossing(
                new RayCrossing(
                    hit.getHitDistance(),
                    this,
                    slope < 0 ? RayCrossingKind.Entering : RayCrossingKind.Leaving,
                ),
            );
        }
        return RayCrossingSupport.Supported;
    }

    public override classifyRay(
        ray: Ray,
        query: RayCrossingQuery,
        result: RaySolidClassification,
    ): RaySolidQueryStatus {
        result.clear();
        if (Double.isNaN(query.minT) || Double.isNaN(query.maxT) || query.minT >= query.maxT)
            return RaySolidQueryStatus.InvalidInput;
        const full = new RayCrossingQuery(
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            query.minimumChordSquared,
            query.minimumExteriorApproach,
        );
        const hit = new RayHit(RayHit.DETAIL_NONE, false);
        const status = this.plane.queryFirstHit(ray, full, hit);
        if (status === RayFirstHitStatus.Unsupported) return RaySolidQueryStatus.Unsupported;
        if (status === RayFirstHitStatus.InvalidInput) return RaySolidQueryStatus.InvalidInput;
        if (status === RayFirstHitStatus.Indeterminate) return RaySolidQueryStatus.Indeterminate;
        if (status === RayFirstHitStatus.Miss) {
            const value = this.plane.pointDistance(ray.getOrigin());
            if (!Double.isFinite(value) || value === 0) return RaySolidQueryStatus.Indeterminate;
            return result.insideSegments.add(value < 0)
                ? RaySolidQueryStatus.Supported
                : RaySolidQueryStatus.Indeterminate;
        }
        const normal = new Vector3Dd(this.plane.getA(), this.plane.getB(), this.plane.getC());
        const slope = normal.dotProduct(ray.getDirection());
        const t = hit.getHitDistance();
        if (!Double.isFinite(slope) || slope === 0 || !Double.isFinite(t)) return RaySolidQueryStatus.Indeterminate;
        const before = slope > 0;
        const initial = query.minT <= t ? before : !before;
        if (!result.insideSegments.add(initial)) return RaySolidQueryStatus.Indeterminate;
        if (t >= query.minT && t <= query.maxT) {
            if (!result.boundaries.add(new RaySolidBoundary(t, this)) || !result.insideSegments.add(!before)) {
                result.clear();
                return RaySolidQueryStatus.Indeterminate;
            }
        }
        return RaySolidQueryStatus.Supported;
    }

    public override doIntersectionFirstHit(ray: Ray, hit: RayHit): boolean;
    public override doIntersectionFirstHit(ray: Ray, hit: RayHit | null): boolean;
    public override doIntersectionFirstHit(ray: Ray, hit: RayHit | null): boolean {
        return (
            this.queryFirstHit(ray, new RayCrossingQuery(0, Double.POSITIVE_INFINITY), hit) === RayFirstHitStatus.Hit
        );
    }

    public override doExtraInformation(ray: Ray, t: number, hit: RayHit): void {
        this.plane.doExtraInformation(ray, t, hit);
    }

    //= Boundary representation =============================================

    /// Default half side of the square of the plane kept by the export
    public static readonly DEFAULT_EXPORT_HALF_SIZE = 2.0;
    /// Default thickness of the slab kept by the export
    public static readonly DEFAULT_EXPORT_DEPTH = 0.5;

    /**
    Java overloads `exportToPolyhedralBoundedSolid()` (bounded part of the
    half space with the default size) and
    `exportToPolyhedralBoundedSolid(double halfSize, double depth)`.

    The half space is unbounded, so its boundary representation is a bounded
    part of it: the slab under a square of its plane, centered at the point
    of the plane nearest to the origin. It is the boundary representation of
    a `Box` (built with Euler operators, [MANT1988].12.4) placed in the frame
    of the plane: its top face on the plane, its outward normal along the
    normal of the plane.
    @param halfSize half the side of the square of the plane
    @param depth thickness of the slab, below the plane
    @return a new polyhedral solid, or null if the plane is not valid
    */
    public override exportToPolyhedralBoundedSolid(): PolyhedralBoundedSolid | null;
    public override exportToPolyhedralBoundedSolid(halfSize: number, depth: number): PolyhedralBoundedSolid | null;
    public override exportToPolyhedralBoundedSolid(halfSize?: number, depth?: number): PolyhedralBoundedSolid | null {
        if (halfSize === undefined || depth === undefined) {
            return this.exportToPolyhedralBoundedSolid(
                PlanarHalfSpace.DEFAULT_EXPORT_HALF_SIZE,
                PlanarHalfSpace.DEFAULT_EXPORT_DEPTH,
            );
        }
        const n = new Vector3Dd(this.plane.getA(), this.plane.getB(), this.plane.getC());
        const length = n.length();
        if (
            !(length > 0) ||
            !Double.isFinite(length) ||
            !Double.isFinite(this.plane.getD()) ||
            !(halfSize > 0) ||
            !(depth > 0)
        ) {
            return null;
        }
        const u = n.multiply(1.0 / length);
        const center = u.multiply(-this.plane.getD() / length);
        // Tangents of the plane: (t1, t2, u) is a right handed frame
        const axis = Math.abs(u.x()) < 0.9 ? new Vector3Dd(1, 0, 0) : new Vector3Dd(0, 1, 0);
        const t1 = axis.subtract(u.multiply(axis.dotProduct(u))).normalized();
        const t2 = u.crossProduct(t1);
        const frame = new Matrix4x4d([
            [t1.x(), t2.x(), u.x(), center.x()],
            [t1.y(), t2.y(), u.y(), center.y()],
            [t1.z(), t2.z(), u.z(), center.z()],
            [0, 0, 0, 1],
        ]);

        const solid = new Box(
            new Vector3Dd(-halfSize, -halfSize, -depth),
            new Vector3Dd(halfSize, halfSize, 0),
        ).exportToPolyhedralBoundedSolid();
        let i: number;
        for (i = 0; i < solid.getVerticesList().size(); i++) {
            solid.getVerticesList().get(i)!.position = frame.multiply(solid.getVerticesList().get(i)!.position);
        }
        return solid;
    }
}
