import { Double } from "../../../../../java/lang/Double.js";
import { IllegalStateException } from "../../../../../java/lang/IllegalStateException.js";
import { Math as JavaMath } from "../../../../../java/lang/Math.js";
import { ArrayList } from "../../../../../java/util/ArrayList.js";
import { Collections } from "../../../../../java/util/Collections.js";
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
import type { PolyhedralBoundedSolid } from "./polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidFieldConstructions } from "./polyhedralBoundedSolid/PolyhedralBoundedSolidFieldConstructions.js";
import { Solid } from "./Solid.js";

// Solid F(p) < 0 of a continuous scalar field F whose ray restriction a
// subclass can solve. This class turns the roots of F along a ray into the
// typed queries: first hit, crossings, ray classification, certified point and
// box containment, and surface detail with the outward normal grad F / |grad F|.
// Solver roots are refined by Newton steps on F before use.
// Ray membership between consecutive roots is decided by the sign of F at a
// point inside the segment, so tangent double roots do not split segments and a zero or
// non-finite sample is reported as Indeterminate. Queries need a finite minT.
export abstract class ImplicitSolid extends Solid {
    // Largest relative Newton correction applied to a solver root.
    public static readonly ROOT_POLISH_REACH = 1.0e-3;
    // Roots closer than this relative distance are one root.
    public static readonly ROOT_MERGE_TOLERANCE = 1.0e-12;

    // Field value; the solid is F < 0.
    public abstract evaluateField(point: Vector3Dd): number;
    // Gradient of the field, pointing out of the solid.
    public abstract evaluateFieldGradient(point: Vector3Dd): Vector3Dd;
    // Certified enclosure of F over the box x by y by z; invalid when unknown.
    public abstract encloseField(x: FloatingInterval, y: FloatingInterval, z: FloatingInterval): FloatingInterval;
    // Certified enclosures of the gradient over the box, used for the
    // mean-value form F(c) + grad F(box) . (box - c); false when unavailable.
    // Outputs gx, gy, gz are one-element arrays.
    public encloseFieldGradient(
        _x: FloatingInterval,
        _y: FloatingInterval,
        _z: FloatingInterval,
        _gx: FloatingInterval[],
        _gy: FloatingInterval[],
        _gz: FloatingInterval[],
    ): boolean {
        return false;
    }
    // Appends the distances d >= 0 at which F(origin + d*unitDirection) = 0,
    // in any order, including at least every root up to maximumDistance.
    // Returns false when the roots cannot be found.
    public abstract findRayRoots(
        origin: Vector3Dd,
        unitDirection: Vector3Dd,
        maximumDistance: number,
        distances: ArrayList<number>,
    ): boolean;

    private collectRoots(
        ray: Ray,
        query: RayCrossingQuery,
        distances: ArrayList<number>,
        start: Vector3Dd[],
        unit: Vector3Dd[],
        length: number[],
        maximumDistance: number[],
    ): RayFirstHitStatus {
        distances.clear();
        const o = ray.getOrigin();
        const d = ray.getDirection();
        if (
            !Double.isFinite(o.x()) ||
            !Double.isFinite(o.y()) ||
            !Double.isFinite(o.z()) ||
            !Double.isFinite(d.x()) ||
            !Double.isFinite(d.y()) ||
            !Double.isFinite(d.z()) ||
            Double.isNaN(query.minT) ||
            Double.isNaN(query.maxT) ||
            query.minT > query.maxT
        )
            return RayFirstHitStatus.InvalidInput;
        length[0] = d.length();
        if (!(length[0]! > 0) || !Double.isFinite(length[0]!)) return RayFirstHitStatus.InvalidInput;
        if (
            query.minimumChordSquared !== 0 ||
            query.minimumExteriorApproach !== Double.NEGATIVE_INFINITY ||
            !Double.isFinite(query.minT)
        )
            return RayFirstHitStatus.Unsupported;
        unit[0] = d.multiply(1.0 / length[0]!);
        start[0] = o.add(d.multiply(query.minT));
        maximumDistance[0] =
            query.maxT === Double.POSITIVE_INFINITY ? Double.POSITIVE_INFINITY : (query.maxT - query.minT) * length[0]!;
        if (
            !Double.isFinite(start[0].x()) ||
            !Double.isFinite(start[0].y()) ||
            !Double.isFinite(start[0].z()) ||
            Double.isNaN(maximumDistance[0])
        )
            return RayFirstHitStatus.Indeterminate;
        const found = new ArrayList<number>();
        if (!this.findRayRoots(start[0], unit[0], maximumDistance[0], found)) return RayFirstHitStatus.Indeterminate;
        for (let i = 0; i < found.size(); ++i) {
            const value = this.polishRoot(start[0], unit[0], found.get(i));
            if (!Double.isFinite(value)) return RayFirstHitStatus.Indeterminate;
            if (value >= 0 && value <= maximumDistance[0]) distances.add(value);
        }
        Collections.sort(distances, null);
        // The same root reported by two solver intervals collapses to one.
        let kept = 0;
        for (let i = 0; i < distances.size(); ++i)
            if (
                kept === 0 ||
                distances.get(i) - distances.get(kept - 1) >
                    ImplicitSolid.ROOT_MERGE_TOLERANCE * (1.0 + distances.get(i))
            )
                distances.set(kept++, distances.get(i));
        while (distances.size() > kept) distances.removeAt(distances.size() - 1);
        return distances.isEmpty() ? RayFirstHitStatus.Miss : RayFirstHitStatus.Hit;
    }

    private polishRoot(start: Vector3Dd, unit: Vector3Dd, distance: number): number {
        // Newton steps on F along the ray, kept only while they shrink |F| and stay
        // close, so solver roots reach the accuracy of the field evaluation.
        if (!Double.isFinite(distance)) return distance;
        let value = this.evaluateField(start.add(unit.multiply(distance)));
        for (let iteration = 0; iteration < 8 && Double.isFinite(value) && value !== 0; ++iteration) {
            const slope = this.evaluateFieldGradient(start.add(unit.multiply(distance))).dotProduct(unit);
            if (!Double.isFinite(slope) || slope === 0) break;
            const next = distance - value / slope;
            if (
                !Double.isFinite(next) ||
                JavaMath.abs(next - distance) > ImplicitSolid.ROOT_POLISH_REACH * (1.0 + JavaMath.abs(distance))
            )
                break;
            const nextValue = this.evaluateField(start.add(unit.multiply(next)));
            if (!(JavaMath.abs(nextValue) < JavaMath.abs(value))) break;
            distance = next;
            value = nextValue;
        }
        return distance;
    }

    private sampleSign(start: Vector3Dd, unit: Vector3Dd, distance: number): number {
        const value = this.evaluateField(start.add(unit.multiply(distance)));
        if (!Double.isFinite(value)) return 0;
        return value < 0 ? -1 : value > 0 ? 1 : 0;
    }

    private classifySegments(
        distances: ArrayList<number>,
        start: Vector3Dd,
        unit: Vector3Dd,
        maximumDistance: number,
        inside: ArrayList<boolean>,
    ): RaySolidQueryStatus {
        inside.clear();
        const n = distances.size();
        for (let i = 0; i <= n; ++i) {
            const a = i === 0 ? 0.0 : distances.get(i - 1);
            const b = i === n ? maximumDistance : distances.get(i);
            if (!(b > a)) return RaySolidQueryStatus.Indeterminate;
            // Any point of a root-free segment decides it; long segments are
            // sampled near their start to keep F finite.
            const reach = JavaMath.max(1.0, a);
            const sample = b - a > 2.0 * reach ? a + reach : 0.5 * (a + b);
            const sign = this.sampleSign(start, unit, sample);
            if (sign === 0) return RaySolidQueryStatus.Indeterminate;
            inside.add(sign < 0);
        }
        return RaySolidQueryStatus.Supported;
    }

    public override classifyRay(
        ray: Ray,
        query: RayCrossingQuery,
        result: RaySolidClassification,
    ): RaySolidQueryStatus {
        result.clear();
        if (Double.isNaN(query.minT) || Double.isNaN(query.maxT) || query.minT >= query.maxT)
            return RaySolidQueryStatus.InvalidInput;
        const distances = new ArrayList<number>();
        const start = [new Vector3Dd()],
            unit = [new Vector3Dd()];
        const length = [0],
            maximumDistance = [0];
        const status = this.collectRoots(ray, query, distances, start, unit, length, maximumDistance);
        if (status === RayFirstHitStatus.InvalidInput) return RaySolidQueryStatus.InvalidInput;
        if (status === RayFirstHitStatus.Unsupported) return RaySolidQueryStatus.Unsupported;
        if (status === RayFirstHitStatus.Indeterminate) return RaySolidQueryStatus.Indeterminate;
        const inside = new ArrayList<boolean>();
        if (
            this.classifySegments(distances, start[0]!, unit[0]!, maximumDistance[0]!, inside) !==
            RaySolidQueryStatus.Supported
        )
            return RaySolidQueryStatus.Indeterminate;
        let state = inside.get(0);
        result.insideSegments.add(state);
        for (let i = 0; i < distances.size(); ++i) {
            if (inside.get(i + 1) === state) continue;
            state = inside.get(i + 1);
            if (
                !result.boundaries.add(new RaySolidBoundary(query.minT + distances.get(i) / length[0]!, this)) ||
                !result.insideSegments.add(state)
            ) {
                result.clear();
                return RaySolidQueryStatus.Indeterminate;
            }
        }
        return RaySolidQueryStatus.Supported;
    }

    public override emitRayCrossings(ray: Ray, query: RayCrossingQuery, sink: RayCrossingSink): RayCrossingSupport {
        const distances = new ArrayList<number>();
        const start = [new Vector3Dd()],
            unit = [new Vector3Dd()];
        const length = [0],
            maximumDistance = [0];
        const status = this.collectRoots(ray, query, distances, start, unit, length, maximumDistance);
        if (status === RayFirstHitStatus.Miss) return RayCrossingSupport.Supported;
        if (status === RayFirstHitStatus.InvalidInput) return RayCrossingSupport.InvalidInput;
        if (status === RayFirstHitStatus.Unsupported) return RayCrossingSupport.Unsupported;
        if (status !== RayFirstHitStatus.Hit) return RayCrossingSupport.Indeterminate;
        const inside = new ArrayList<boolean>();
        if (
            this.classifySegments(distances, start[0]!, unit[0]!, maximumDistance[0]!, inside) !==
            RaySolidQueryStatus.Supported
        )
            return RayCrossingSupport.Indeterminate;
        for (let i = 0; i < distances.size(); ++i) {
            const kind =
                inside.get(i) === inside.get(i + 1)
                    ? RayCrossingKind.Tangent
                    : inside.get(i + 1)
                      ? RayCrossingKind.Entering
                      : RayCrossingKind.Leaving;
            sink.crossing(new RayCrossing(query.minT + distances.get(i) / length[0]!, this, kind));
        }
        return RayCrossingSupport.Supported;
    }

    public override queryFirstHit(
        ray: Ray,
        query: RayCrossingQuery,
        hit: RayHit | null,
        boundary: RayBoundary | null = null,
    ): RayFirstHitStatus {
        if (hit) hit.clear();
        if (boundary) boundary.assign(new RayBoundary());
        const distances = new ArrayList<number>();
        const start = [new Vector3Dd()],
            unit = [new Vector3Dd()];
        const length = [0],
            maximumDistance = [0];
        const status = this.collectRoots(ray, query, distances, start, unit, length, maximumDistance);
        if (status !== RayFirstHitStatus.Hit) return status;
        const t = query.minT + distances.get(0) / length[0]!;
        if (t > query.maxT) return RayFirstHitStatus.Miss;
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
        hit.setHitDistance(boundary.t);
        if (hit.shouldStoreRay()) hit.setRay(ray.withT(boundary.t));
        this.doExtraInformation(ray, boundary.t, hit);
        if (boundary.normalReversed && hit.needsNormal() && hit.getAvailableDetailMask() & RayHit.DETAIL_NORMAL)
            hit.normal = hit.normal.multiply(-1);
        return RayFirstHitStatus.Hit;
    }

    private enclosureSign(point: Vector3Dd): number {
        const value = this.encloseField(
            FloatingInterval.exact(point.x()),
            FloatingInterval.exact(point.y()),
            FloatingInterval.exact(point.z()),
        );
        if (!value.isValid()) return 0;
        return value.getUpper() < 0 ? -1 : value.getLower() > 0 ? 1 : 0;
    }

    // -1/1 when the natural or the mean-value enclosure proves F < 0 / F > 0.
    private boxSign(x: FloatingInterval, y: FloatingInterval, z: FloatingInterval, center: Vector3Dd): number {
        const range = this.encloseField(x, y, z);
        if (range.isValid() && range.getUpper() < 0) return -1;
        if (range.isValid() && range.getLower() > 0) return 1;
        const gx = [new FloatingInterval()],
            gy = [new FloatingInterval()],
            gz = [new FloatingInterval()];
        if (
            !this.encloseFieldGradient(x, y, z, gx, gy, gz) ||
            !gx[0]!.isValid() ||
            !gy[0]!.isValid() ||
            !gz[0]!.isValid()
        )
            return 0;
        const cx = FloatingInterval.exact(center.x());
        const cy = FloatingInterval.exact(center.y());
        const cz = FloatingInterval.exact(center.z());
        let value = this.encloseField(cx, cy, cz);
        value = value.add(gx[0]!.multiply(x.subtract(cx)));
        value = value.add(gy[0]!.multiply(y.subtract(cy)));
        value = value.add(gz[0]!.multiply(z.subtract(cz)));
        if (!value.isValid()) return 0;
        return value.getUpper() < 0 ? -1 : value.getLower() > 0 ? 1 : 0;
    }

    public override classifyBoxCertified(center: Vector3Dd, halfWidths: Vector3Dd): GeometryContainment {
        if (!FloatingInterval.isSupportedEnvironment()) return GeometryContainment.Unsupported;
        const x = FloatingInterval.around(center.x(), halfWidths.x());
        const y = FloatingInterval.around(center.y(), halfWidths.y());
        const z = FloatingInterval.around(center.z(), halfWidths.z());
        if (!x.isValid() || !y.isValid() || !z.isValid()) return GeometryContainment.InvalidInput;
        const sign = this.boxSign(x, y, z, center);
        return sign < 0
            ? GeometryContainment.Inside
            : sign > 0
              ? GeometryContainment.Outside
              : GeometryContainment.Indeterminate;
    }

    // Sign enclosures over the ball's bounding box prove Inside/Outside;
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
        if (!FloatingInterval.isSupportedEnvironment()) return GeometryContainment.Unsupported;
        const x = FloatingInterval.around(point.x(), tolerance);
        const y = FloatingInterval.around(point.y(), tolerance);
        const z = FloatingInterval.around(point.z(), tolerance);
        if (!x.isValid() || !y.isValid() || !z.isValid()) return GeometryContainment.Indeterminate;
        const sign = this.boxSign(x, y, z, point);
        if (sign < 0) return GeometryContainment.Inside;
        if (sign > 0) return GeometryContainment.Outside;
        if (tolerance === 0) return GeometryContainment.Indeterminate;
        // F is continuous and the ball is convex: certified opposite signs at two
        // points of the ball prove a boundary point on the segment joining them.
        const g = this.evaluateFieldGradient(point);
        const length = g.length();
        if (!(length > 0) || !Double.isFinite(length)) return GeometryContainment.Indeterminate;
        const unit = g.multiply(1.0 / length);
        const centre = this.enclosureSign(point);
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
                signs[side] = this.enclosureSign(q);
            }
            if (signs[0]! * signs[1]! < 0) return GeometryContainment.Boundary;
            if (centre !== 0 && (signs[0] === -centre || signs[1] === -centre)) return GeometryContainment.Boundary;
        }
        return GeometryContainment.Indeterminate;
    }

    public override classifyPoint(point: Vector3Dd, tolerance: number): GeometryContainment {
        if (!Double.isFinite(tolerance) || tolerance < 0) return GeometryContainment.InvalidInput;
        if (tolerance !== 0) return this.classifyPointCertified(point, tolerance);
        const value = this.evaluateField(point);
        if (!Double.isFinite(value)) return GeometryContainment.Indeterminate;
        return value < 0
            ? GeometryContainment.Inside
            : value > 0
              ? GeometryContainment.Outside
              : GeometryContainment.Boundary;
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
            const normal = this.evaluateFieldGradient(point);
            const length = normal.length();
            if (Double.isFinite(length) && length > 0) {
                hit.normal = normal.multiply(1.0 / length);
                hit.markDetailAvailable(RayHit.DETAIL_NORMAL);
            }
        }
    }

    public override doContainmentTest(point: Vector3Dd, tolerance: number): number {
        // Legacy integer adapter over the typed contract.
        const result = this.classifyPoint(point, tolerance);
        if (result === GeometryContainment.Inside) return ImplicitSolid.INSIDE;
        if (result === GeometryContainment.Boundary) return ImplicitSolid.LIMIT;
        if (result === GeometryContainment.Outside) return ImplicitSolid.OUTSIDE;
        throw new IllegalStateException(
            "Implicit solid containment cannot be represented by the legacy integer result",
        );
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

    //= Boundary representation =============================================

    /// Default number of grid cells along the largest side of the box
    /// polygonized by `exportToPolyhedralBoundedSolid`
    public static readonly DEFAULT_POLYGONIZATION_DIVISIONS = 16;
    /// Half the side of the cube, centered at the origin, that clips an
    /// unbounded solid when exported to a boundary representation
    public static readonly DEFAULT_CLIPPING_HALF_SIZE = 3.0;

    /**
    Java overloads `exportToPolyhedralBoundedSolid()` (with the default
    number of divisions), `exportToPolyhedralBoundedSolid(int divisions)` and
    `exportToPolyhedralBoundedSolid(int divisions, Vector3Dd minimum, Vector3Dd maximum)`.

    Polyhedral approximation of the solid F &lt; 0, built with Euler
    operators by `PolyhedralBoundedSolidFieldConstructions`. A bounded solid
    is polygonized inside its bounds; an unbounded one is clipped by the
    cube of half side `DEFAULT_CLIPPING_HALF_SIZE` centered at the origin.
    With a box, it is the polyhedral approximation of the part of the solid
    F &lt; 0 inside it.
    @param divisions number of grid cells along the largest side of the
    polygonized box (at least 1)
    @param minimum minimum corner of the box
    @param maximum maximum corner of the box
    @return a new polyhedral approximation, or null if the solid is empty or
    its bounds can not be determined
    */
    public override exportToPolyhedralBoundedSolid(): PolyhedralBoundedSolid | null;
    public override exportToPolyhedralBoundedSolid(divisions: number): PolyhedralBoundedSolid | null;
    public override exportToPolyhedralBoundedSolid(
        divisions: number,
        minimum: Vector3Dd,
        maximum: Vector3Dd,
    ): PolyhedralBoundedSolid | null;
    public override exportToPolyhedralBoundedSolid(
        divisions?: number,
        minimum?: Vector3Dd,
        maximum?: Vector3Dd,
    ): PolyhedralBoundedSolid | null {
        if (divisions === undefined) {
            return this.exportToPolyhedralBoundedSolid(ImplicitSolid.DEFAULT_POLYGONIZATION_DIVISIONS);
        }
        if (minimum !== undefined && maximum !== undefined) {
            return PolyhedralBoundedSolidFieldConstructions.createFromScalarField(
                (point: Vector3Dd) => this.evaluateField(point),
                minimum,
                maximum,
                Math.max(1, divisions),
            );
        }
        const bounds = new GeometryBounds();
        this.getBounds(bounds);
        if (bounds.getKind() === GeometryBounds.Kind.Finite) {
            return PolyhedralBoundedSolidFieldConstructions.createFromBoundedScalarField(
                (point: Vector3Dd) => this.evaluateField(point),
                bounds.getMinimum(),
                bounds.getMaximum(),
                Math.max(1, divisions),
            );
        }
        if (bounds.getKind() === GeometryBounds.Kind.Unbounded) {
            const c = ImplicitSolid.DEFAULT_CLIPPING_HALF_SIZE;
            return this.exportToPolyhedralBoundedSolid(divisions, new Vector3Dd(-c, -c, -c), new Vector3Dd(c, c, c));
        }
        return null;
    }
}
