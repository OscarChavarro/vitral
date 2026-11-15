import { Double } from "../../../../../java/lang/Double.js";
import { IllegalStateException } from "../../../../../java/lang/IllegalStateException.js";
import { Math as JavaMath } from "../../../../../java/lang/Math.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { FloatingInterval } from "../../../common/numerics/FloatingInterval.js";
import { GeometryBounds } from "../element/GeometryBounds.js";
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
import { Solid } from "./Solid.js";

// F(x,y,z) = square dot (x*x,y*y,z*z) + mixed dot (xy,xz,yz)
//              + linear dot (x,y,z) + constant; the solid is F <= 0.
export class QuadricSurface extends Solid {
    private square: Vector3Dd;
    private mixed: Vector3Dd;
    private linear: Vector3Dd;
    private constant: number;

    public constructor(s: Vector3Dd, m: Vector3Dd, l: Vector3Dd, c: number) {
        super();
        this.square = s;
        this.mixed = m;
        this.linear = l;
        this.constant = c;
    }

    // The evaluation order matches the historical POV implementation.
    public evaluate(point: Vector3Dd): number {
        let result = point.dotProduct(this.linear) + this.constant;
        result += new Vector3Dd(point.x() * point.x(), point.y() * point.y(), point.z() * point.z()).dotProduct(
            this.square,
        );
        result +=
            this.mixed.x() * point.x() * point.y() +
            this.mixed.y() * point.x() * point.z() +
            this.mixed.z() * point.y() * point.z();
        return result;
    }

    public gradient(point: Vector3Dd): Vector3Dd {
        const doubled = this.square.multiply(2.0);
        const base = new Vector3Dd(doubled.x() * point.x(), doubled.y() * point.y(), doubled.z() * point.z()).add(
            this.linear,
        );
        return new Vector3Dd(
            base.x() + this.mixed.x() * point.y() + this.mixed.y() * point.z(),
            base.y() + this.mixed.x() * point.x() + this.mixed.z() * point.z(),
            base.z() + this.mixed.y() * point.x() + this.mixed.z() * point.y(),
        );
    }

    public override copyGeometry(): Geometry | null {
        return new QuadricSurface(this.square, this.mixed, this.linear, this.constant);
    }

    public squareCoefficients(): Vector3Dd {
        return this.square;
    }
    public setSquareCoefficients(value: Vector3Dd): void {
        this.square = value;
    }
    public mixedCoefficients(): Vector3Dd {
        return this.mixed;
    }
    public setMixedCoefficients(value: Vector3Dd): void {
        this.mixed = value;
    }
    public linearCoefficients(): Vector3Dd {
        return this.linear;
    }
    public setLinearCoefficients(value: Vector3Dd): void {
        this.linear = value;
    }
    public constantCoefficient(): number {
        return this.constant;
    }
    public setConstantCoefficient(value: number): void {
        this.constant = value;
    }

    public static solve(a: number, b: number, c: number, first: number[], second: number[]): boolean {
        if (a !== 0.0) {
            const discriminant = b * b - 4.0 * a * c;
            if (discriminant < 0.0) return false;
            const root = JavaMath.sqrt(discriminant);
            first[0] = (-b + root) / (2.0 * a);
            second[0] = (-b - root) / (2.0 * a);
        } else {
            if (b === 0.0) return false;
            first[0] = second[0] = -c / b;
        }
        return true;
    }

    private roots(ray: Ray, query: RayCrossingQuery, first: number[], second: number[]): RayFirstHitStatus {
        const o = ray.getOrigin();
        const d = ray.getDirection();
        if (
            !Double.isFinite(o.x()) ||
            !Double.isFinite(o.y()) ||
            !Double.isFinite(o.z()) ||
            !Double.isFinite(d.x()) ||
            !Double.isFinite(d.y()) ||
            !Double.isFinite(d.z()) ||
            (d.x() === 0 && d.y() === 0 && d.z() === 0) ||
            Double.isNaN(query.minT) ||
            Double.isNaN(query.maxT) ||
            query.minT > query.maxT
        )
            return RayFirstHitStatus.InvalidInput;
        if (query.minimumChordSquared !== 0 || query.minimumExteriorApproach !== Double.NEGATIVE_INFINITY)
            return RayFirstHitStatus.Unsupported;
        const square = this.square,
            mixed = this.mixed;
        const a =
            square.x() * d.x() * d.x() +
            square.y() * d.y() * d.y() +
            square.z() * d.z() * d.z() +
            mixed.x() * d.x() * d.y() +
            mixed.y() * d.x() * d.z() +
            mixed.z() * d.y() * d.z();
        const b = this.gradient(o).dotProduct(d);
        const c = this.evaluate(o);
        if (!Double.isFinite(a) || !Double.isFinite(b) || !Double.isFinite(c)) return RayFirstHitStatus.Indeterminate;
        if (a === 0 && b === 0 && c === 0) return RayFirstHitStatus.Indeterminate;
        if (a !== 0 && !Double.isFinite(b * b - 4.0 * a * c)) return RayFirstHitStatus.Indeterminate;
        if (!QuadricSurface.solve(a, b, c, first, second)) return RayFirstHitStatus.Miss;
        if (!Double.isFinite(first[0]!) || !Double.isFinite(second[0]!)) return RayFirstHitStatus.Indeterminate;
        if (first[0]! > second[0]!) {
            const tmp = first[0]!;
            first[0] = second[0]!;
            second[0] = tmp;
        }
        return RayFirstHitStatus.Hit;
    }

    public override getBounds(result: GeometryBounds): void {
        const square = this.square,
            mixed = this.mixed,
            linear = this.linear,
            constant = this.constant;
        if (
            !Double.isFinite(square.x()) ||
            !Double.isFinite(square.y()) ||
            !Double.isFinite(square.z()) ||
            !Double.isFinite(mixed.x()) ||
            !Double.isFinite(mixed.y()) ||
            !Double.isFinite(mixed.z()) ||
            !Double.isFinite(linear.x()) ||
            !Double.isFinite(linear.y()) ||
            !Double.isFinite(linear.z()) ||
            !Double.isFinite(constant)
        ) {
            result.setIndeterminate();
            return;
        }
        // POV's positive-definite ellipsoid bound. Other signs and singular or
        // poorly conditioned matrices remain conservatively unbounded.
        const a11 = square.x(),
            a22 = square.y(),
            a33 = square.z();
        const a12 = mixed.x() * 0.5,
            a13 = mixed.y() * 0.5,
            a23 = mixed.z() * 0.5;
        const minor1 = a11;
        const minor2 = a11 * a22 - a12 * a12;
        const det = a11 * (a22 * a33 - a23 * a23) - a12 * (a12 * a33 - a23 * a13) + a13 * (a12 * a23 - a22 * a13);
        if (!Double.isFinite(minor2) || !Double.isFinite(det)) {
            result.setIndeterminate();
            return;
        }
        if (minor1 <= 1e-12 || minor2 <= 1e-12 || det <= 1e-12) {
            result.setUnbounded();
            return;
        }
        const invDet = 1.0 / det;
        const inv11 = (a22 * a33 - a23 * a23) * invDet;
        const inv12 = (a13 * a23 - a12 * a33) * invDet;
        const inv13 = (a12 * a23 - a13 * a22) * invDet;
        const inv22 = (a11 * a33 - a13 * a13) * invDet;
        const inv23 = (a12 * a13 - a11 * a23) * invDet;
        const inv33 = (a11 * a22 - a12 * a12) * invDet;
        if (
            !Double.isFinite(inv11) ||
            !Double.isFinite(inv12) ||
            !Double.isFinite(inv13) ||
            !Double.isFinite(inv22) ||
            !Double.isFinite(inv23) ||
            !Double.isFinite(inv33)
        ) {
            result.setIndeterminate();
            return;
        }
        const center = new Vector3Dd(
            -0.5 * (inv11 * linear.x() + inv12 * linear.y() + inv13 * linear.z()),
            -0.5 * (inv12 * linear.x() + inv22 * linear.y() + inv23 * linear.z()),
            -0.5 * (inv13 * linear.x() + inv23 * linear.y() + inv33 * linear.z()),
        );
        const aCenter = new Vector3Dd(
            a11 * center.x() + a12 * center.y() + a13 * center.z(),
            a12 * center.x() + a22 * center.y() + a23 * center.z(),
            a13 * center.x() + a23 * center.y() + a33 * center.z(),
        );
        const radiusSquared = center.dotProduct(aCenter) - constant;
        if (!Double.isFinite(radiusSquared)) {
            result.setIndeterminate();
            return;
        }
        if (radiusSquared <= 1e-12 || inv11 <= 0 || inv22 <= 0 || inv33 <= 0) {
            result.setUnbounded();
            return;
        }
        const extent = new Vector3Dd(
            JavaMath.sqrt(radiusSquared * inv11),
            JavaMath.sqrt(radiusSquared * inv22),
            JavaMath.sqrt(radiusSquared * inv33),
        );
        result.setFinite(center.subtract(extent), center.add(extent));
    }

    private enclose(x: FloatingInterval, y: FloatingInterval, z: FloatingInterval): FloatingInterval {
        const square = this.square,
            mixed = this.mixed,
            linear = this.linear;
        let result = FloatingInterval.exact(this.constant);
        result = result.add(FloatingInterval.exact(linear.x()).multiply(x));
        result = result.add(FloatingInterval.exact(linear.y()).multiply(y));
        result = result.add(FloatingInterval.exact(linear.z()).multiply(z));
        result = result.add(FloatingInterval.exact(square.x()).multiply(x.square()));
        result = result.add(FloatingInterval.exact(square.y()).multiply(y.square()));
        result = result.add(FloatingInterval.exact(square.z()).multiply(z.square()));
        result = result.add(FloatingInterval.exact(mixed.x()).multiply(x.multiply(y)));
        result = result.add(FloatingInterval.exact(mixed.y()).multiply(x.multiply(z)));
        return result.add(FloatingInterval.exact(mixed.z()).multiply(y.multiply(z)));
    }

    private coefficientArray(): number[] {
        const square = this.square,
            mixed = this.mixed,
            linear = this.linear;
        return [
            square.x(),
            square.y(),
            square.z(),
            mixed.x(),
            mixed.y(),
            mixed.z(),
            linear.x(),
            linear.y(),
            linear.z(),
            this.constant,
        ];
    }

    public override classifyBoxCertified(center: Vector3Dd, halfWidths: Vector3Dd): GeometryContainment {
        const coefficients = this.coefficientArray();
        for (let i = 0; i < 10; ++i) if (!Double.isFinite(coefficients[i]!)) return GeometryContainment.InvalidInput;
        if (!FloatingInterval.isSupportedEnvironment()) return GeometryContainment.Unsupported;
        const x = FloatingInterval.around(center.x(), halfWidths.x());
        const y = FloatingInterval.around(center.y(), halfWidths.y());
        const z = FloatingInterval.around(center.z(), halfWidths.z());
        if (!x.isValid() || !y.isValid() || !z.isValid()) return GeometryContainment.InvalidInput;
        const range = this.enclose(x, y, z);
        if (!range.isValid()) return GeometryContainment.Indeterminate;
        if (range.getUpper() < 0) return GeometryContainment.Inside;
        if (range.getLower() > 0) return GeometryContainment.Outside;
        return GeometryContainment.Indeterminate;
    }

    // Sign enclosures of F over the ball's bounding box prove Inside/Outside;
    // Boundary requires two certified points of opposite sign within the ball.
    public override classifyPointCertified(point: Vector3Dd, tolerance: number): GeometryContainment {
        if (
            !Double.isFinite(tolerance) ||
            tolerance < 0 ||
            !Double.isFinite(point.x()) ||
            !Double.isFinite(point.y()) ||
            !Double.isFinite(point.z())
        )
            return GeometryContainment.InvalidInput;
        const coefficients = this.coefficientArray();
        for (let i = 0; i < 10; ++i) if (!Double.isFinite(coefficients[i]!)) return GeometryContainment.InvalidInput;
        if (!FloatingInterval.isSupportedEnvironment()) return GeometryContainment.Unsupported;
        const coordinates = [point.x(), point.y(), point.z()];
        const box = [new FloatingInterval(), new FloatingInterval(), new FloatingInterval()];
        for (let i = 0; i < 3; ++i) {
            box[i] = FloatingInterval.around(coordinates[i]!, tolerance);
            if (!box[i]!.isValid()) return GeometryContainment.Indeterminate;
        }
        const range = this.enclose(box[0]!, box[1]!, box[2]!);
        if (!range.isValid()) return GeometryContainment.Indeterminate;
        if (range.getUpper() < 0) return GeometryContainment.Inside;
        if (range.getLower() > 0) return GeometryContainment.Outside;
        if (tolerance === 0) return GeometryContainment.Indeterminate;
        // F is continuous and the ball is convex: certified opposite signs at two
        // points of the ball prove a boundary point on the segment joining them.
        const g = this.gradient(point);
        const length = g.length();
        if (!(length > 0) || !Double.isFinite(length)) return GeometryContainment.Indeterminate;
        const unit = g.multiply(1.0 / length);
        const steps = [tolerance * (1.0 - 1.0 / 1048576.0), tolerance * 0.5];
        for (let s = 0; s < 2; ++s) {
            const signs = [0, 0];
            for (let side = 0; side < 2; ++side) {
                const q = point.add(unit.multiply(side === 0 ? -steps[s]! : steps[s]!));
                let offset = FloatingInterval.exact(0);
                offset = offset.add(FloatingInterval.exact(q.x()).subtract(FloatingInterval.exact(point.x())).square());
                offset = offset.add(FloatingInterval.exact(q.y()).subtract(FloatingInterval.exact(point.y())).square());
                offset = offset.add(FloatingInterval.exact(q.z()).subtract(FloatingInterval.exact(point.z())).square());
                const distance = offset.squareRoot();
                if (!distance.isValid() || distance.getUpper() > tolerance) continue;
                const value = this.enclose(
                    FloatingInterval.exact(q.x()),
                    FloatingInterval.exact(q.y()),
                    FloatingInterval.exact(q.z()),
                );
                if (!value.isValid()) continue;
                signs[side] = value.getUpper() < 0 ? -1 : value.getLower() > 0 ? 1 : 0;
            }
            if (signs[0]! * signs[1]! < 0) return GeometryContainment.Boundary;
            // The centre itself may be the second point.
            const centre = this.enclose(
                FloatingInterval.exact(point.x()),
                FloatingInterval.exact(point.y()),
                FloatingInterval.exact(point.z()),
            );
            if (centre.isValid()) {
                const sign = centre.getUpper() < 0 ? -1 : centre.getLower() > 0 ? 1 : 0;
                if (sign !== 0 && (signs[0] === -sign || signs[1] === -sign)) return GeometryContainment.Boundary;
            }
        }
        return GeometryContainment.Indeterminate;
    }

    public override classifyPoint(point: Vector3Dd, tolerance: number): GeometryContainment {
        if (!Double.isFinite(tolerance) || tolerance < 0) return GeometryContainment.InvalidInput;
        if (tolerance !== 0) return this.classifyPointCertified(point, tolerance);
        const value = this.evaluate(point);
        if (!Double.isFinite(value)) return GeometryContainment.Indeterminate;
        return value < 0
            ? GeometryContainment.Inside
            : value > 0
              ? GeometryContainment.Outside
              : GeometryContainment.Boundary;
    }

    public override queryFirstHit(
        ray: Ray,
        query: RayCrossingQuery,
        hit: RayHit | null,
        boundary: RayBoundary | null = null,
    ): RayFirstHitStatus {
        if (hit) hit.clear();
        if (boundary) boundary.assign(new RayBoundary());
        const a = [0],
            b = [0];
        const status = this.roots(ray, query, a, b);
        if (status !== RayFirstHitStatus.Hit) return status;
        const t = a[0]! >= query.minT ? a[0]! : b[0]!;
        if (t < query.minT || t > query.maxT) return RayFirstHitStatus.Miss;
        if (boundary) boundary.assign(new RayBoundary(t, this));
        if (hit) {
            hit.setHitDistance(t);
            if (hit.shouldStoreRay()) hit.setRay(ray.withT(t));
            if (hit.needsAnySurfaceData()) this.doExtraInformation(ray, t, hit);
        }
        return RayFirstHitStatus.Hit;
    }

    public override querySurfaceDetail(ray: Ray, boundary: RayBoundary, hit: RayHit | null): RayFirstHitStatus {
        if (hit) hit.clear();
        if (!hit || boundary.geometry !== this || !Double.isFinite(boundary.t)) return RayFirstHitStatus.InvalidInput;
        if (boundary.getInstanceCount() !== 0) return RayFirstHitStatus.Unsupported;
        const status = this.queryFirstHit(ray, new RayCrossingQuery(boundary.t, boundary.t), null);
        if (status !== RayFirstHitStatus.Hit) return status;
        hit.setHitDistance(boundary.t);
        if (hit.shouldStoreRay()) hit.setRay(ray.withT(boundary.t));
        this.doExtraInformation(ray, boundary.t, hit);
        if (boundary.normalReversed && hit.needsNormal() && hit.getAvailableDetailMask() & RayHit.DETAIL_NORMAL)
            hit.normal = hit.normal.multiply(-1);
        return RayFirstHitStatus.Hit;
    }

    public override emitRayCrossings(ray: Ray, query: RayCrossingQuery, sink: RayCrossingSink): RayCrossingSupport {
        const aOut = [0],
            bOut = [0];
        const status = this.roots(ray, query, aOut, bOut);
        if (status === RayFirstHitStatus.Miss) return RayCrossingSupport.Supported;
        if (status === RayFirstHitStatus.InvalidInput) return RayCrossingSupport.InvalidInput;
        if (status === RayFirstHitStatus.Unsupported) return RayCrossingSupport.Unsupported;
        if (status !== RayFirstHitStatus.Hit) return RayCrossingSupport.Indeterminate;
        const a = aOut[0]!,
            b = bOut[0]!;
        const direction = ray.getDirection();
        const emitFirst = a >= query.minT && a <= query.maxT;
        const emitSecond = a !== b && b >= query.minT && b <= query.maxT;
        let slopeFirst = 0,
            slopeSecond = 0;
        if (emitFirst) {
            slopeFirst = this.gradient(ray.getOrigin().add(direction.multiply(a))).dotProduct(direction);
            if (!Double.isFinite(slopeFirst) || (a !== b && slopeFirst === 0)) return RayCrossingSupport.Indeterminate;
        }
        if (emitSecond) {
            slopeSecond = this.gradient(ray.getOrigin().add(direction.multiply(b))).dotProduct(direction);
            if (!Double.isFinite(slopeSecond) || slopeSecond === 0) return RayCrossingSupport.Indeterminate;
        }
        if (emitFirst)
            sink.crossing(
                new RayCrossing(
                    a,
                    this,
                    slopeFirst === 0
                        ? RayCrossingKind.Tangent
                        : slopeFirst < 0
                          ? RayCrossingKind.Entering
                          : RayCrossingKind.Leaving,
                ),
            );
        if (emitSecond)
            sink.crossing(
                new RayCrossing(b, this, slopeSecond < 0 ? RayCrossingKind.Entering : RayCrossingKind.Leaving),
            );
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
        const firstOut = [0],
            secondOut = [0];
        const status = this.roots(ray, query, firstOut, secondOut);
        if (status === RayFirstHitStatus.InvalidInput) return RaySolidQueryStatus.InvalidInput;
        if (status === RayFirstHitStatus.Unsupported) return RaySolidQueryStatus.Unsupported;
        if (status === RayFirstHitStatus.Indeterminate) return RaySolidQueryStatus.Indeterminate;
        if (status === RayFirstHitStatus.Miss) {
            const value = this.evaluate(ray.getOrigin());
            if (!Double.isFinite(value) || value === 0) return RaySolidQueryStatus.Indeterminate;
            return result.insideSegments.add(value < 0)
                ? RaySolidQueryStatus.Supported
                : RaySolidQueryStatus.Indeterminate;
        }
        const first = firstOut[0]!,
            second = secondOut[0]!;
        const direction = ray.getDirection();
        const slopeFirst = this.gradient(ray.getOrigin().add(direction.multiply(first))).dotProduct(direction);
        if (!Double.isFinite(slopeFirst)) return RaySolidQueryStatus.Indeterminate;
        if (first === second && slopeFirst === 0) {
            // A tangent does not change interval membership. F's quadratic term
            // determines whether the ray lies outside or inside on both sides.
            const square = this.square,
                mixed = this.mixed;
            const a =
                square.x() * direction.x() * direction.x() +
                square.y() * direction.y() * direction.y() +
                square.z() * direction.z() * direction.z() +
                mixed.x() * direction.x() * direction.y() +
                mixed.y() * direction.x() * direction.z() +
                mixed.z() * direction.y() * direction.z();
            if (!Double.isFinite(a) || a === 0) return RaySolidQueryStatus.Indeterminate;
            return result.insideSegments.add(a < 0) ? RaySolidQueryStatus.Supported : RaySolidQueryStatus.Indeterminate;
        }
        if (slopeFirst === 0) return RaySolidQueryStatus.Indeterminate;
        let slopeSecond = 0;
        if (first !== second) {
            slopeSecond = this.gradient(ray.getOrigin().add(direction.multiply(second))).dotProduct(direction);
            if (!Double.isFinite(slopeSecond) || slopeSecond === 0 || slopeFirst < 0 === slopeSecond < 0)
                return RaySolidQueryStatus.Indeterminate;
        }
        let inside = slopeFirst > 0;
        if (query.minT > first) inside = slopeFirst < 0;
        if (first !== second && query.minT > second) inside = slopeSecond < 0;
        if (!result.insideSegments.add(inside)) return RaySolidQueryStatus.Indeterminate;
        if (first >= query.minT && first <= query.maxT) {
            if (
                !result.boundaries.add(new RaySolidBoundary(first, this)) ||
                !result.insideSegments.add(slopeFirst < 0)
            ) {
                result.clear();
                return RaySolidQueryStatus.Indeterminate;
            }
        }
        if (first !== second && second >= query.minT && second <= query.maxT) {
            if (
                !result.boundaries.add(new RaySolidBoundary(second, this)) ||
                !result.insideSegments.add(slopeSecond < 0)
            ) {
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
            this.queryFirstHit(ray, new RayCrossingQuery(1e-9, Double.POSITIVE_INFINITY), hit) === RayFirstHitStatus.Hit
        );
    }

    public override doExtraInformation(ray: Ray, t: number, hit: RayHit): void;
    public override doExtraInformation(ray: Ray, t: number, hit: RayHit | null): void;
    public override doExtraInformation(ray: Ray, t: number, hit: RayHit | null): void {
        if (!hit) return;
        hit.beginDetailEvaluation();
        const point = ray.getOrigin().add(ray.getDirection().multiply(t));
        if (hit.needsPoint()) {
            hit.point = point;
            hit.markDetailAvailable(RayHit.DETAIL_POINT);
        }
        if (hit.needsNormal()) {
            const normal = this.gradient(point);
            const length = normal.length();
            if (Double.isFinite(length) && length > 0) {
                hit.normal = normal.multiply(1.0 / length);
                hit.markDetailAvailable(RayHit.DETAIL_NORMAL);
            }
        }
    }

    public override doContainmentTest(p: Vector3Dd, tolerance: number): number {
        // Legacy integer adapter over the typed world-distance contract.
        const result = this.classifyPoint(p, tolerance);
        if (result === GeometryContainment.Inside) return QuadricSurface.INSIDE;
        if (result === GeometryContainment.Boundary) return QuadricSurface.LIMIT;
        if (result === GeometryContainment.Outside) return QuadricSurface.OUTSIDE;
        throw new IllegalStateException("Quadric containment cannot be represented by the legacy integer result");
    }

    public override getMinMax(): Float64Array {
        const bounds = new GeometryBounds();
        this.getBounds(bounds);
        if (bounds.getKind() === GeometryBounds.Kind.Finite)
            return Float64Array.of(
                bounds.getMinimum().x(),
                bounds.getMinimum().y(),
                bounds.getMinimum().z(),
                bounds.getMaximum().x(),
                bounds.getMaximum().y(),
                bounds.getMaximum().z(),
            );
        return Float64Array.of(-1e308, -1e308, -1e308, 1e308, 1e308, 1e308);
    }
}
