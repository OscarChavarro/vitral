//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import { PolyhedralBoundedSolidEulerOperators } from "./polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import { VSDK } from "../../../common/VSDK.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import type { Ray } from "../element/Ray.js";
import { RayCrossing } from "../element/RayCrossing.js";
import { RayCrossingKind } from "../element/RayCrossingKind.js";
import { RayCrossingSupport } from "../element/RayCrossingSupport.js";
import { RayCrossingQuery } from "../element/RayCrossingQuery.js";
import type { RayCrossingSink } from "../element/RayCrossingSink.js";
import { RayFirstHitStatus } from "../element/RayFirstHitStatus.js";
import { RaySolidQueryStatus } from "../element/RaySolidQueryStatus.js";
import type { RaySolidClassification } from "../element/RaySolidClassification.js";
import { RaySolidBoundary } from "../element/RaySolidBoundary.js";
import { RayBoundary } from "../element/RayBoundary.js";
import type { GeometryBounds } from "../element/GeometryBounds.js";
import { GeometryContainment } from "../element/GeometryContainment.js";
import type { Geometry } from "../Geometry.js";
import { FloatingInterval } from "../../../common/numerics/FloatingInterval.js";
import { Double } from "../../../../../java/lang/Double.js";
import { Math as JavaMath } from "../../../../../java/lang/Math.js";
import { RayHit } from "../element/RayHit.js";
import { PolyhedralBoundedSolid } from "./polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { Solid } from "./Solid.js";

export class Box extends Solid {
    private static readonly NORMAL_POS_Z = new Vector3Dd(0, 0, 1);
    private static readonly NORMAL_NEG_Z = new Vector3Dd(0, 0, -1);
    private static readonly NORMAL_POS_Y = new Vector3Dd(0, 1, 0);
    private static readonly NORMAL_NEG_Y = new Vector3Dd(0, -1, 0);
    private static readonly NORMAL_POS_X = new Vector3Dd(1, 0, 0);
    private static readonly NORMAL_NEG_X = new Vector3Dd(-1, 0, 0);
    private static readonly TANGENT_POS_Y = new Vector3Dd(0, 1, 0);
    private static readonly TANGENT_NEG_Y = new Vector3Dd(0, -1, 0);
    private static readonly TANGENT_NEG_X = new Vector3Dd(-1, 0, 0);
    private static readonly TANGENT_POS_X = new Vector3Dd(1, 0, 0);
    private static readonly ZERO_VECTOR = new Vector3Dd();

    private readonly bounds: Vector3Dd[];

    public constructor(dx: number, dy: number, dz: number);
    public constructor(s: Vector3Dd);
    public constructor(minimum: Vector3Dd, maximum: Vector3Dd);
    public constructor(a: number | Vector3Dd, b?: number | Vector3Dd, dz?: number) {
        super();
        if (a instanceof Vector3Dd && b instanceof Vector3Dd) {
            this.bounds = [a, b];
            this.addControlSpecifications();
            return;
        }
        const s = a instanceof Vector3Dd ? a : new Vector3Dd(a, b as number, dz!);
        this.bounds = [
            new Vector3Dd(-s.x() / 2, -s.y() / 2, -s.z() / 2),
            new Vector3Dd(s.x() / 2, s.y() / 2, s.z() / 2),
        ];
        this.addControlSpecifications();
    }

    /**
    The state edited is the size along each axis; the center is kept (moving
    the box is a transformation of its body, not a change of the box).
    */
    private addControlSpecifications(): void {
        this.getControlSpecifications().push("double;sizeX;(0, INFINITE)");
        this.getControlSpecifications().push("double;sizeY;(0, INFINITE)");
        this.getControlSpecifications().push("double;sizeZ;(0, INFINITE)");
    }

    /** C++ returns the two-element array itself; elements may be replaced in place. */
    public getRawBounds(): Vector3Dd[] {
        return this.bounds;
    }
    public getCenter(): Vector3Dd {
        return this.bounds[0]!.multiply(0.5).add(this.bounds[1]!.multiply(0.5));
    }
    public override copyGeometry(): Geometry | null {
        return new Box(this.bounds[0]!, this.bounds[1]!);
    }

    public override getBounds(result: GeometryBounds): void {
        const size = this.getSize();
        if (
            !Double.isFinite(size.x()) ||
            !Double.isFinite(size.y()) ||
            !Double.isFinite(size.z()) ||
            size.x() <= 0 ||
            size.y() <= 0 ||
            size.z() <= 0
        ) {
            result.setIndeterminate();
            return;
        }
        result.setFinite(this.bounds[0]!, this.bounds[1]!);
    }

    public override classifyPoint(point: Vector3Dd, tolerance: number): GeometryContainment {
        if (Double.isFinite(tolerance) && tolerance > 0) return this.classifyPointCertified(point, tolerance);
        const minimum = [this.bounds[0]!.x(), this.bounds[0]!.y(), this.bounds[0]!.z()];
        const maximum = [this.bounds[1]!.x(), this.bounds[1]!.y(), this.bounds[1]!.z()];
        const coordinates = [point.x(), point.y(), point.z()];
        if (!Double.isFinite(tolerance) || tolerance < 0) return GeometryContainment.InvalidInput;
        let outside = false;
        let onFace = false;
        for (let i = 0; i < 3; ++i) {
            const dimension = maximum[i]! - minimum[i]!;
            if (!Double.isFinite(dimension) || dimension <= 0 || !Double.isFinite(coordinates[i]!))
                return GeometryContainment.InvalidInput;
            if (!Double.isFinite(minimum[i]!) || !Double.isFinite(maximum[i]!) || dimension / 2 === 0)
                return GeometryContainment.Indeterminate;
            if (coordinates[i]! < minimum[i]! || coordinates[i]! > maximum[i]!) outside = true;
            else if (coordinates[i] === minimum[i] || coordinates[i] === maximum[i]) onFace = true;
        }
        if (outside) return GeometryContainment.Outside;
        return onFace ? GeometryContainment.Boundary : GeometryContainment.Inside;
    }

    public override classifyBoxCertified(center: Vector3Dd, halfWidths: Vector3Dd): GeometryContainment {
        const minimum = [this.bounds[0]!.x(), this.bounds[0]!.y(), this.bounds[0]!.z()];
        const maximum = [this.bounds[1]!.x(), this.bounds[1]!.y(), this.bounds[1]!.z()];
        const c = [center.x(), center.y(), center.z()];
        const h = [halfWidths.x(), halfWidths.y(), halfWidths.z()];
        for (let i = 0; i < 3; ++i) {
            if (!Double.isFinite(c[i]!) || !Double.isFinite(h[i]!) || h[i]! < 0)
                return GeometryContainment.InvalidInput;
            if (!Double.isFinite(minimum[i]!) || !Double.isFinite(maximum[i]!))
                return GeometryContainment.Indeterminate;
            if (!(minimum[i]! < maximum[i]!)) return GeometryContainment.InvalidInput;
        }
        if (!FloatingInterval.isSupportedEnvironment()) return GeometryContainment.Unsupported;
        let inside = true;
        for (let i = 0; i < 3; ++i) {
            const range = FloatingInterval.around(c[i]!, h[i]!);
            if (!range.isValid()) return GeometryContainment.Indeterminate;
            if (range.getUpper() < minimum[i]! || range.getLower() > maximum[i]!) return GeometryContainment.Outside;
            if (!(range.getLower() > minimum[i]! && range.getUpper() < maximum[i]!)) inside = false;
        }
        return inside ? GeometryContainment.Inside : GeometryContainment.Indeterminate;
    }

    public override classifyPointCertified(point: Vector3Dd, tolerance: number): GeometryContainment {
        const minimum = [this.bounds[0]!.x(), this.bounds[0]!.y(), this.bounds[0]!.z()];
        const maximum = [this.bounds[1]!.x(), this.bounds[1]!.y(), this.bounds[1]!.z()];
        const coordinates = [point.x(), point.y(), point.z()];
        if (!Double.isFinite(tolerance) || tolerance < 0) return GeometryContainment.InvalidInput;
        for (let i = 0; i < 3; ++i) {
            if (!Double.isFinite(coordinates[i]!)) return GeometryContainment.InvalidInput;
            if (!Double.isFinite(minimum[i]!) || !Double.isFinite(maximum[i]!))
                return GeometryContainment.Indeterminate;
            if (!(minimum[i]! < maximum[i]!)) return GeometryContainment.InvalidInput;
        }
        if (!FloatingInterval.isSupportedEnvironment()) return GeometryContainment.Unsupported;
        let inside = true;
        for (let i = 0; i < 3; ++i) if (coordinates[i]! < minimum[i]! || coordinates[i]! > maximum[i]!) inside = false;
        if (inside) {
            // The distance to the boundary is the smallest face distance.
            let deep = true,
                near = false;
            for (let i = 0; i < 3; ++i) {
                if (coordinates[i] === minimum[i] || coordinates[i] === maximum[i]) {
                    near = true;
                    deep = false;
                    continue;
                }
                const low = FloatingInterval.exact(coordinates[i]!).subtract(FloatingInterval.exact(minimum[i]!));
                const high = FloatingInterval.exact(maximum[i]!).subtract(FloatingInterval.exact(coordinates[i]!));
                if (!low.isValid() || !high.isValid()) return GeometryContainment.Indeterminate;
                if (!(low.getLower() > tolerance) || !(high.getLower() > tolerance)) deep = false;
                if (low.getUpper() <= tolerance || high.getUpper() <= tolerance) near = true;
            }
            if (deep) return GeometryContainment.Inside;
            return near ? GeometryContainment.Boundary : GeometryContainment.Indeterminate;
        }
        let squared = FloatingInterval.exact(0);
        for (let i = 0; i < 3; ++i) {
            if (coordinates[i]! < minimum[i]!)
                squared = squared.add(
                    FloatingInterval.exact(minimum[i]!).subtract(FloatingInterval.exact(coordinates[i]!)).square(),
                );
            else if (coordinates[i]! > maximum[i]!)
                squared = squared.add(
                    FloatingInterval.exact(coordinates[i]!).subtract(FloatingInterval.exact(maximum[i]!)).square(),
                );
        }
        const distance = squared.squareRoot();
        if (!distance.isValid()) return GeometryContainment.Indeterminate;
        if (distance.getLower() > tolerance) return GeometryContainment.Outside;
        return distance.getUpper() <= tolerance ? GeometryContainment.Boundary : GeometryContainment.Indeterminate;
    }

    // Slab intersection in the original affine ray parameterization. A ray that
    // overlaps a face for an interval has no finite sequence of surface events.
    private clipRay(ray: Ray, query: RayCrossingQuery, entry: number[], exit: number[]): RayFirstHitStatus {
        const size = this.getSize();
        const dimensions = [size.x(), size.y(), size.z()];
        const origins = [ray.getOrigin().x(), ray.getOrigin().y(), ray.getOrigin().z()];
        const directions = [ray.getDirection().x(), ray.getDirection().y(), ray.getDirection().z()];
        if (
            Double.isNaN(query.minT) ||
            Double.isNaN(query.maxT) ||
            query.minT > query.maxT ||
            !Double.isFinite(query.minimumChordSquared) ||
            query.minimumChordSquared < 0 ||
            Double.isNaN(query.minimumExteriorApproach)
        )
            return RayFirstHitStatus.InvalidInput;
        if (query.minimumChordSquared !== 0 || query.minimumExteriorApproach !== Double.NEGATIVE_INFINITY)
            return RayFirstHitStatus.Unsupported;
        if (directions[0] === 0 && directions[1] === 0 && directions[2] === 0) return RayFirstHitStatus.InvalidInput;
        for (let i = 0; i < 3; ++i) {
            if (
                !Double.isFinite(dimensions[i]!) ||
                dimensions[i]! <= 0 ||
                !Double.isFinite(origins[i]!) ||
                !Double.isFinite(directions[i]!)
            )
                return RayFirstHitStatus.InvalidInput;
        }
        entry[0] = Double.NEGATIVE_INFINITY;
        exit[0] = Double.POSITIVE_INFINITY;
        const half = new Vector3Dd(size.x() / 2, size.y() / 2, size.z() / 2);
        if (half.x() === 0 || half.y() === 0 || half.z() === 0) return RayFirstHitStatus.Indeterminate;
        const faceOverlap = [false];
        const clipped = Box.intersectBounds(
            ray.getOrigin(),
            ray.getDirection(),
            this.bounds[0]!,
            this.bounds[1]!,
            0,
            true,
            entry,
            exit,
            faceOverlap,
        );
        if (clipped !== RayFirstHitStatus.Hit) return clipped;
        if (exit[0]! < query.minT || entry[0]! > query.maxT) return RayFirstHitStatus.Miss;
        if (faceOverlap[0] && entry[0]! < exit[0]!) return RayFirstHitStatus.Indeterminate;
        return RayFirstHitStatus.Hit;
    }

    public override emitRayCrossings(ray: Ray, query: RayCrossingQuery, sink: RayCrossingSink): RayCrossingSupport {
        const entryValue = [0],
            exitValue = [0];
        const status = this.clipRay(ray, query, entryValue, exitValue);
        if (status === RayFirstHitStatus.Miss) return RayCrossingSupport.Supported;
        if (status === RayFirstHitStatus.Unsupported) return RayCrossingSupport.Unsupported;
        if (status === RayFirstHitStatus.InvalidInput) return RayCrossingSupport.InvalidInput;
        if (status !== RayFirstHitStatus.Hit) return RayCrossingSupport.Indeterminate;
        const entry = entryValue[0]!,
            exit = exitValue[0]!;
        const first = ray.getOrigin().add(ray.getDirection().multiply(entry));
        const last = ray.getOrigin().add(ray.getDirection().multiply(exit));
        if (
            !Double.isFinite(first.x()) ||
            !Double.isFinite(first.y()) ||
            !Double.isFinite(first.z()) ||
            !Double.isFinite(last.x()) ||
            !Double.isFinite(last.y()) ||
            !Double.isFinite(last.z())
        )
            return RayCrossingSupport.Indeterminate;
        if (entry >= query.minT && entry <= query.maxT)
            sink.crossing(
                new RayCrossing(entry, this, entry === exit ? RayCrossingKind.Tangent : RayCrossingKind.Entering),
            );
        if (exit !== entry && exit >= query.minT && exit <= query.maxT)
            sink.crossing(new RayCrossing(exit, this, RayCrossingKind.Leaving));
        return RayCrossingSupport.Supported;
    }

    public override queryFirstHit(
        ray: Ray,
        query: RayCrossingQuery,
        result: RayHit | null,
        boundary: RayBoundary | null = null,
    ): RayFirstHitStatus {
        if (result) result.clear();
        if (boundary) boundary.assign(new RayBoundary());
        const entry = [0],
            exit = [0];
        const status = this.clipRay(ray, query, entry, exit);
        if (status !== RayFirstHitStatus.Hit) return status;
        const t = entry[0]! >= query.minT ? entry[0]! : exit[0]!;
        if (t > query.maxT) return RayFirstHitStatus.Miss;
        const point = ray.getOrigin().add(ray.getDirection().multiply(t));
        if (!Double.isFinite(point.x()) || !Double.isFinite(point.y()) || !Double.isFinite(point.z()))
            return RayFirstHitStatus.Indeterminate;
        if (boundary) boundary.assign(new RayBoundary(t, this));
        if (result) {
            result.setHitDistance(t);
            if (result.shouldStoreRay()) result.setRay(ray.withT(t));
            if (result.needsAnySurfaceData()) this.doExtraInformation(ray, t, result);
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

    public override classifyRay(
        ray: Ray,
        query: RayCrossingQuery,
        result: RaySolidClassification,
    ): RaySolidQueryStatus {
        result.clear();
        if (query.minT >= query.maxT) return RaySolidQueryStatus.InvalidInput;
        const entryValue = [0],
            exitValue = [0];
        const status = this.clipRay(ray, query, entryValue, exitValue);
        if (status === RayFirstHitStatus.Unsupported) return RaySolidQueryStatus.Unsupported;
        if (status === RayFirstHitStatus.InvalidInput) return RaySolidQueryStatus.InvalidInput;
        if (status === RayFirstHitStatus.Indeterminate) return RaySolidQueryStatus.Indeterminate;
        const entry = entryValue[0]!,
            exit = exitValue[0]!;
        const intersectsInterior = status === RayFirstHitStatus.Hit && entry < exit;
        if (!result.insideSegments.add(intersectsInterior && query.minT > entry && query.minT <= exit))
            return RaySolidQueryStatus.Indeterminate;
        if (intersectsInterior && entry >= query.minT && entry <= query.maxT) {
            if (!result.boundaries.add(new RaySolidBoundary(entry, this)) || !result.insideSegments.add(true)) {
                result.clear();
                return RaySolidQueryStatus.Indeterminate;
            }
        }
        if (intersectsInterior && exit >= query.minT && exit <= query.maxT) {
            if (!result.boundaries.add(new RaySolidBoundary(exit, this)) || !result.insideSegments.add(false)) {
                result.clear();
                return RaySolidQueryStatus.Indeterminate;
            }
        }
        return RaySolidQueryStatus.Supported;
    }

    // Mathematical operations on arbitrary axis-aligned bounds. Boundary
    // inclusion and the face selection tolerance are chosen by the caller.
    public static containsBounds(point: Vector3Dd, minimum: Vector3Dd, maximum: Vector3Dd): boolean {
        return !(
            point.x() < minimum.x() ||
            point.x() > maximum.x() ||
            point.y() < minimum.y() ||
            point.y() > maximum.y() ||
            point.z() < minimum.z() ||
            point.z() > maximum.z()
        );
    }

    public static normalForBounds(
        point: Vector3Dd,
        minimum: Vector3Dd,
        maximum: Vector3Dd,
        tolerance: number,
    ): Vector3Dd {
        if (JavaMath.abs(point.x() - maximum.x()) < tolerance) return new Vector3Dd(1, 0, 0);
        if (JavaMath.abs(point.x() - minimum.x()) < tolerance) return new Vector3Dd(-1, 0, 0);
        if (JavaMath.abs(point.y() - maximum.y()) < tolerance) return new Vector3Dd(0, 1, 0);
        if (JavaMath.abs(point.y() - minimum.y()) < tolerance) return new Vector3Dd(0, -1, 0);
        if (JavaMath.abs(point.z() - maximum.z()) < tolerance) return new Vector3Dd(0, 0, 1);
        if (JavaMath.abs(point.z() - minimum.z()) < tolerance) return new Vector3Dd(0, 0, -1);
        return new Vector3Dd(0, 0, 0);
    }

    private static clipAxis(
        origin: number,
        direction: number,
        minimum: number,
        maximum: number,
        parallelTolerance: number,
        checkArithmetic: boolean,
        entry: number[],
        exit: number[],
        faceOverlap: boolean[],
    ): RayFirstHitStatus {
        if (direction < -parallelTolerance || direction > parallelTolerance) {
            const farBound = direction < 0 ? minimum : maximum;
            const nearBound = direction < 0 ? maximum : minimum;
            const farT = (farBound - origin) / direction;
            if (!checkArithmetic && farT < entry[0]!) return RayFirstHitStatus.Miss;
            const nearT = (nearBound - origin) / direction;
            if (checkArithmetic && (!Double.isFinite(farT) || !Double.isFinite(nearT) || farT === nearT))
                return RayFirstHitStatus.Indeterminate;
            if (farT < entry[0]!) return RayFirstHitStatus.Miss;
            if (farT <= exit[0]!) exit[0] = farT;
            if (nearT >= entry[0]!) {
                if (nearT > exit[0]!) return RayFirstHitStatus.Miss;
                entry[0] = nearT;
            }
            return RayFirstHitStatus.Hit;
        }
        if (origin < minimum || origin > maximum) return RayFirstHitStatus.Miss;
        if (origin === minimum || origin === maximum) faceOverlap[0] = true;
        return RayFirstHitStatus.Hit;
    }

    // Intersect arbitrary axis-aligned bounds with an initial parameter interval.
    // The caller selects its parallel threshold and whether arithmetic failures
    // are reported; a parallel ray on a face is reported separately.
    public static intersectBounds(
        origin: Vector3Dd,
        direction: Vector3Dd,
        minimum: Vector3Dd,
        maximum: Vector3Dd,
        parallelTolerance: number,
        checkArithmetic: boolean,
        entry: number[],
        exit: number[],
        faceOverlap: boolean[],
    ): RayFirstHitStatus {
        faceOverlap[0] = false;
        if (
            checkArithmetic &&
            (!Double.isFinite(origin.x()) ||
                !Double.isFinite(origin.y()) ||
                !Double.isFinite(origin.z()) ||
                !Double.isFinite(direction.x()) ||
                !Double.isFinite(direction.y()) ||
                !Double.isFinite(direction.z()) ||
                (direction.x() === 0 && direction.y() === 0 && direction.z() === 0) ||
                !Double.isFinite(minimum.x()) ||
                !Double.isFinite(minimum.y()) ||
                !Double.isFinite(minimum.z()) ||
                !Double.isFinite(maximum.x()) ||
                !Double.isFinite(maximum.y()) ||
                !Double.isFinite(maximum.z()) ||
                minimum.x() >= maximum.x() ||
                minimum.y() >= maximum.y() ||
                minimum.z() >= maximum.z() ||
                !Double.isFinite(parallelTolerance) ||
                parallelTolerance < 0 ||
                Double.isNaN(entry[0]!) ||
                Double.isNaN(exit[0]!) ||
                entry[0]! > exit[0]!)
        )
            return RayFirstHitStatus.InvalidInput;
        let result = Box.clipAxis(
            origin.x(),
            direction.x(),
            minimum.x(),
            maximum.x(),
            parallelTolerance,
            checkArithmetic,
            entry,
            exit,
            faceOverlap,
        );
        if (result !== RayFirstHitStatus.Hit) return result;
        result = Box.clipAxis(
            origin.y(),
            direction.y(),
            minimum.y(),
            maximum.y(),
            parallelTolerance,
            checkArithmetic,
            entry,
            exit,
            faceOverlap,
        );
        if (result !== RayFirstHitStatus.Hit) return result;
        return Box.clipAxis(
            origin.z(),
            direction.z(),
            minimum.z(),
            maximum.z(),
            parallelTolerance,
            checkArithmetic,
            entry,
            exit,
            faceOverlap,
        );
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doIntersectionFirstHit.

    Java overload `doIntersectionFirstHit(Ray inOutRay)`.
    @param inOutRay
    @return true if given ray intersects current Box
    */
    public doIntersectionFirstHit(inOutRay: Ray): Ray | null;
    public override doIntersectionFirstHit(inRay: Ray, outHit: RayHit): boolean;
    public override doIntersectionFirstHit(inRay: Ray, outHit: RayHit | null): boolean;
    public override doIntersectionFirstHit(inRay: Ray, outHit?: RayHit | null): Ray | null | boolean {
        if (outHit === undefined) {
            const hit = new RayHit();
            if (this.doIntersectionFirstHit(inRay, hit)) {
                return hit.getRay();
            }
            return null;
        }

        let minT = Number.MAX_VALUE;
        let hitPlane = 0;
        const size = this.getSize();
        const center = this.getCenter();
        const x2 = size.x() / 2;
        const y2 = size.y() / 2;
        const z2 = size.z() / 2;
        const ox = inRay.getOrigin().x() - center.x();
        const oy = inRay.getOrigin().y() - center.y();
        const oz = inRay.getOrigin().z() - center.z();
        const dx = inRay.getDirection().x();
        const dy = inRay.getDirection().y();
        const dz = inRay.getDirection().z();

        if (Math.abs(dz) > VSDK.EPSILON) {
            const t = (z2 - oz) / dz;
            if (t > -VSDK.EPSILON) {
                const cx = ox + dx * t;
                const cy = oy + dy * t;
                if (cx >= -x2 && cx <= x2 && cy >= -y2 && cy <= y2) {
                    minT = t;
                    hitPlane = 1;
                }
            }
        }
        if (Math.abs(dz) > VSDK.EPSILON) {
            const t = (-z2 - oz) / dz;
            if (t > -VSDK.EPSILON && t < minT) {
                const cx = ox + dx * t;
                const cy = oy + dy * t;
                if (cx >= -x2 && cx <= x2 && cy >= -y2 && cy <= y2) {
                    minT = t;
                    hitPlane = 2;
                }
            }
        }
        if (Math.abs(dy) > VSDK.EPSILON) {
            const t = (y2 - oy) / dy;
            if (t > -VSDK.EPSILON && t < minT) {
                const cx = ox + dx * t;
                const cz = oz + dz * t;
                if (cx >= -x2 && cx <= x2 && cz >= -z2 && cz <= z2) {
                    minT = t;
                    hitPlane = 3;
                }
            }
        }
        if (Math.abs(dy) > VSDK.EPSILON) {
            const t = (-y2 - oy) / dy;
            if (t > -VSDK.EPSILON && t < minT) {
                const cx = ox + dx * t;
                const cz = oz + dz * t;
                if (cx >= -x2 && cx <= x2 && cz >= -z2 && cz <= z2) {
                    minT = t;
                    hitPlane = 4;
                }
            }
        }
        if (Math.abs(dx) > VSDK.EPSILON) {
            const t = (x2 - ox) / dx;
            if (t > -VSDK.EPSILON && t < minT) {
                const cy = oy + dy * t;
                const cz = oz + dz * t;
                if (cy >= -y2 && cy <= y2 && cz >= -z2 && cz <= z2) {
                    minT = t;
                    hitPlane = 5;
                }
            }
        }
        if (Math.abs(dx) > VSDK.EPSILON) {
            const t = (-x2 - ox) / dx;
            if (t > -VSDK.EPSILON && t < minT) {
                const cy = oy + dy * t;
                const cz = oz + dz * t;
                if (cy >= -y2 && cy <= y2 && cz >= -z2 && cz <= z2) {
                    minT = t;
                    hitPlane = 6;
                }
            }
        }

        if (minT === Number.MAX_VALUE) {
            return false;
        }

        if (outHit !== null) {
            if (outHit.shouldStoreRay() || outHit.needsAnySurfaceData()) {
                outHit.setRay(inRay.withT(minT));
            } else {
                outHit.setHitDistance(minT);
            }
            if (outHit.needsAnySurfaceData()) {
                const hitX = ox + dx * minT;
                const hitY = oy + dy * minT;
                const hitZ = oz + dz * minT;
                if (outHit.needsPoint()) {
                    outHit.point = new Vector3Dd(hitX, hitY, hitZ).add(center);
                }
                if (outHit.needsTextureCoordinates()) {
                    outHit.u = 0;
                    outHit.v = 0;
                    switch (hitPlane) {
                        case 1:
                            outHit.u = hitY / size.y() - 0.5;
                            outHit.v = 1 - (hitX / size.x() - 0.5);
                            break;
                        case 2:
                            outHit.u = hitY / size.y() - 0.5;
                            outHit.v = hitX / size.x() - 0.5;
                            break;
                        case 3:
                            outHit.u = 1 - (hitX / size.x() - 0.5);
                            outHit.v = hitZ / size.z() - 0.5;
                            break;
                        case 4:
                            outHit.u = hitX / size.x() - 0.5;
                            outHit.v = hitZ / size.z() - 0.5;
                            break;
                        case 5:
                            outHit.u = hitY / size.y() - 0.5;
                            outHit.v = hitZ / size.z() - 0.5;
                            break;
                        case 6:
                            outHit.u = 1 - (hitY / size.y() - 0.5);
                            outHit.v = hitZ / size.z() - 0.5;
                            break;
                        default:
                            break;
                    }
                }
                if (outHit.needsNormal()) {
                    outHit.normal = Box.planeNormal(hitPlane);
                }
                if (outHit.needsTangent()) {
                    outHit.tangent = Box.planeTangent(hitPlane);
                }
            }
        }
        return true;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doExtraInformation.
    @param inRay
    @param inT
    @param outData
    */
    public override doExtraInformation(inRay: Ray, inT: number, outData: RayHit): void {
        if (!outData) return;
        outData.beginDetailEvaluation();
        const hitX = inRay.getOrigin().x() + inRay.getDirection().x() * inT;
        const hitY = inRay.getOrigin().y() + inRay.getDirection().y() * inT;
        const hitZ = inRay.getOrigin().z() + inRay.getDirection().z() * inT;
        const center = this.getCenter();
        const hitPlane = this.classifyHitPlane(hitX - center.x(), hitY - center.y(), hitZ - center.z());

        if (outData.needsPoint()) {
            outData.point = new Vector3Dd(hitX, hitY, hitZ);
            outData.markDetailAvailable(RayHit.DETAIL_POINT);
        }
        if (outData.needsNormal()) {
            outData.normal = Box.planeNormal(hitPlane);
            outData.markDetailAvailable(RayHit.DETAIL_NORMAL);
        }
        if (outData.needsTangent()) {
            outData.tangent = Box.planeTangent(hitPlane);
            outData.markDetailAvailable(RayHit.DETAIL_TANGENT);
        }
        // Java-port texture coordinates, kept from this port; the C++ port does
        // not compute them here and does not report them as available.
        const size = this.getSize();
        if (outData.needsTextureCoordinates()) {
            switch (hitPlane) {
                case 1:
                    outData.u = hitY / size.y() - 0.5;
                    outData.v = 1 - (hitX / size.x() - 0.5);
                    break;
                case 2:
                    outData.u = hitY / size.y() - 0.5;
                    outData.v = hitX / size.x() - 0.5;
                    break;
                case 3:
                    outData.u = 1 - (hitX / size.x() - 0.5);
                    outData.v = hitZ / size.z() - 0.5;
                    break;
                case 4:
                    outData.u = hitX / size.x() - 0.5;
                    outData.v = hitZ / size.z() - 0.5;
                    break;
                case 5:
                    outData.u = hitY / size.y() - 0.5;
                    outData.v = hitZ / size.z() - 0.5;
                    break;
                case 6:
                    outData.u = 1 - (hitY / size.y() - 0.5);
                    outData.v = hitZ / size.z() - 0.5;
                    break;
                default:
                    outData.u = 0;
                    outData.v = 0;
                    break;
            }
        }
    }

    private static planeNormal(hitPlane: number): Vector3Dd {
        switch (hitPlane) {
            case 1:
                return Box.NORMAL_POS_Z;
            case 2:
                return Box.NORMAL_NEG_Z;
            case 3:
                return Box.NORMAL_POS_Y;
            case 4:
                return Box.NORMAL_NEG_Y;
            case 5:
                return Box.NORMAL_POS_X;
            case 6:
                return Box.NORMAL_NEG_X;
            default:
                return Box.ZERO_VECTOR;
        }
    }

    private static planeTangent(hitPlane: number): Vector3Dd {
        switch (hitPlane) {
            case 1:
            case 2:
            case 5:
                return Box.TANGENT_POS_Y;
            case 3:
                return Box.TANGENT_NEG_X;
            case 4:
                return Box.TANGENT_POS_X;
            case 6:
                return Box.TANGENT_NEG_Y;
            default:
                return Box.ZERO_VECTOR;
        }
    }

    private classifyHitPlane(x: number, y: number, z: number): number {
        const size = this.getSize();
        const x2 = size.x() / 2;
        const y2 = size.y() / 2;
        const z2 = size.z() / 2;
        const dxPlus = Math.abs(x - x2);
        const dxMinus = Math.abs(x + x2);
        const dyPlus = Math.abs(y - y2);
        const dyMinus = Math.abs(y + y2);
        const dzPlus = Math.abs(z - z2);
        const dzMinus = Math.abs(z + z2);

        let min = dzPlus;
        let plane = 1;
        if (dzMinus < min) {
            min = dzMinus;
            plane = 2;
        }
        if (dyPlus < min) {
            min = dyPlus;
            plane = 3;
        }
        if (dyMinus < min) {
            min = dyMinus;
            plane = 4;
        }
        if (dxPlus < min) {
            min = dxPlus;
            plane = 5;
        }
        if (dxMinus < min) {
            plane = 6;
        }
        return plane;
    }

    /**
    @return a new 6 valued double array containing the coordinates of a min-max
    bounding box for current geometry.
    */
    public override getMinMax(): Float64Array {
        const minmax = new Float64Array(6);

        minmax[0] = this.bounds[0]!.x();
        minmax[1] = this.bounds[0]!.y();
        minmax[2] = this.bounds[0]!.z();
        minmax[3] = this.bounds[1]!.x();
        minmax[4] = this.bounds[1]!.y();
        minmax[5] = this.bounds[1]!.z();

        return minmax;
    }

    public getSize(): Vector3Dd {
        return this.bounds[1]!.subtract(this.bounds[0]!);
    }

    public setSize(dx: number, dy: number, dz: number): void;
    public setSize(s: Vector3Dd): void;
    public setSize(a: number | Vector3Dd, dy?: number, dz?: number): void {
        if (!(a instanceof Vector3Dd)) {
            this.setSize(new Vector3Dd(a, dy!, dz!));
            return;
        }
        const center = this.getCenter();
        const half = a.multiply(0.5);
        this.bounds[0] = center.subtract(half);
        this.bounds[1] = center.add(half);
    }

    public getSizeX(): number {
        return this.bounds[1]!.x() - this.bounds[0]!.x();
    }

    public getSizeY(): number {
        return this.bounds[1]!.y() - this.bounds[0]!.y();
    }

    public getSizeZ(): number {
        return this.bounds[1]!.z() - this.bounds[0]!.z();
    }

    public setSizeX(dx: number): void {
        this.setSize(this.getSize().withX(dx));
    }

    public setSizeY(dy: number): void {
        this.setSize(this.getSize().withY(dy));
    }

    public setSizeZ(dz: number): void {
        this.setSize(this.getSize().withZ(dz));
    }

    public override exportToPolyhedralBoundedSolid(): PolyhedralBoundedSolid {
        return this.buildPolyhedralBoundedSolid();
    }

    /**
    Current method creates a polyhedral boundary representation for
    current box, following the strategy for Euler operators presented
    at sections [MANT1988].9.3., [MANT1988].12.3.1., as depicted in
    figure [MANT1988].9.11. and following the structure of the program
    [MANT1988].12.4.
    */
    private buildPolyhedralBoundedSolid(): PolyhedralBoundedSolid {
        const solid = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(
            solid,
            new Vector3Dd(this.bounds[0]!.x(), this.bounds[0]!.y(), this.bounds[0]!.z()),
            1,
            1,
        );
        PolyhedralBoundedSolidEulerOperators.smev(
            solid,
            1,
            1,
            4,
            new Vector3Dd(this.bounds[0]!.x(), this.bounds[1]!.y(), this.bounds[0]!.z()),
        );
        PolyhedralBoundedSolidEulerOperators.smev(
            solid,
            1,
            4,
            3,
            new Vector3Dd(this.bounds[1]!.x(), this.bounds[1]!.y(), this.bounds[0]!.z()),
        );
        PolyhedralBoundedSolidEulerOperators.smev(
            solid,
            1,
            3,
            2,
            new Vector3Dd(this.bounds[1]!.x(), this.bounds[0]!.y(), this.bounds[0]!.z()),
        );
        PolyhedralBoundedSolidEulerOperators.mef(solid, 1, 1, 1, 4, 2, 3, 2);

        PolyhedralBoundedSolidEulerOperators.smev(
            solid,
            1,
            1,
            5,
            new Vector3Dd(this.bounds[0]!.x(), this.bounds[0]!.y(), this.bounds[1]!.z()),
        );
        PolyhedralBoundedSolidEulerOperators.smev(
            solid,
            1,
            2,
            6,
            new Vector3Dd(this.bounds[1]!.x(), this.bounds[0]!.y(), this.bounds[1]!.z()),
        );
        PolyhedralBoundedSolidEulerOperators.mef(solid, 1, 1, 5, 1, 6, 2, 3);
        PolyhedralBoundedSolidEulerOperators.smev(
            solid,
            1,
            3,
            7,
            new Vector3Dd(this.bounds[1]!.x(), this.bounds[1]!.y(), this.bounds[1]!.z()),
        );
        PolyhedralBoundedSolidEulerOperators.mef(solid, 1, 1, 6, 2, 7, 3, 4);
        PolyhedralBoundedSolidEulerOperators.smev(
            solid,
            1,
            4,
            8,
            new Vector3Dd(this.bounds[0]!.x(), this.bounds[1]!.y(), this.bounds[1]!.z()),
        );
        PolyhedralBoundedSolidEulerOperators.mef(solid, 1, 1, 7, 3, 8, 4, 5);
        PolyhedralBoundedSolidEulerOperators.mef(solid, 1, 1, 5, 6, 8, 4, 6);
        return solid;
    }
}
