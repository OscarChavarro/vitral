import { Double } from "../../../../../java/lang/Double.js";
import { Math as JavaMath } from "../../../../../java/lang/Math.js";
import { ArrayList } from "../../../../../java/util/ArrayList.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { FloatingInterval } from "../../../common/numerics/FloatingInterval.js";
import { PolynomialSolver } from "../../../numericalAnalysis/polynomial/PolynomialSolver.js";
import { QuadraticSolver } from "../../../numericalAnalysis/polynomial/QuadraticSolver.js";
import { QuarticSolver } from "../../../numericalAnalysis/polynomial/QuarticSolver.js";
import type { GeometryBounds } from "../element/GeometryBounds.js";
import type { Geometry } from "../Geometry.js";
import type { BlobComponent } from "./BlobComponent.js";
import { BlobInfluenceInterval } from "./BlobInfluenceInterval.js";
import { ImplicitSolid } from "./ImplicitSolid.js";

// Blob of spherical field components (PovRay 1.0 blob): the solid where the
// summed component density exceeds the threshold, F = threshold - density.
// Along a ray each set of overlapping influence spheres gives a quartic in the
// ray distance, solved by the closed-form quartic solver or, when requested, by
// Sturm sequences. Certified containment encloses the exact field
// strength * (1 - d^2/radius^2)^2; bounds are the boxes of positive components
// for a nonnegative threshold, and unbounded for a negative one.
export class BlobSolid extends ImplicitSolid {
    public static readonly COEFFICIENT_LIMIT = 1.0e-20;
    public static readonly SOLVER_EPSILON = 1.0e-10;
    // Rays grazing an influence sphere closer than this are ignored, and
    // roots with a squared gradient below it are rejected as spurious.
    public static readonly GRAZING_LIMIT = 1.0e-5;

    private readonly components: ArrayList<BlobComponent> = new ArrayList<BlobComponent>();
    private readonly threshold: number;
    private readonly sturm: boolean;

    public constructor(threshold: number, useSturmSequences: boolean);
    /** cpp implicit copy constructor; BlobComponent values are immutable and shared. */
    public constructor(other: BlobSolid);
    public constructor(thresholdOrOther: number | BlobSolid, useSturmSequences?: boolean) {
        super();
        if (thresholdOrOther instanceof BlobSolid) {
            const other = thresholdOrOther;
            for (let i = 0; i < other.components.size(); i++) this.components.add(other.components.get(i));
            this.threshold = other.threshold;
            this.sturm = other.sturm;
            return;
        }
        this.threshold = thresholdOrOther;
        this.sturm = useSturmSequences as boolean;
    }

    public override copyGeometry(): Geometry {
        return new BlobSolid(this);
    }

    public addComponent(component: BlobComponent): void {
        this.components.add(component);
    }

    public getComponentCount(): number {
        return this.components.size();
    }

    public getComponent(index: number): BlobComponent {
        return this.components.get(index);
    }

    public getThreshold(): number {
        return this.threshold;
    }

    public usesSturmSequences(): boolean {
        return this.sturm;
    }

    /**
    Sorted list of the distances along the ray where the blob components start
    and stop adding their influence. It would take a very complex blob (with many
    components along the current ray) to warrant a faster sort technique.
    */
    private determineInfluences(
        p: Vector3Dd,
        d: Vector3Dd,
        influenceStart: number,
        intervals: ArrayList<BlobInfluenceInterval>,
    ): number {
        intervals.clear();
        for (let i = 0; i < this.components.size(); i++) {
            // Standard sphere intersection with the volume of influence.
            const v = this.components.get(i).getCenter().subtract(p);
            const b = v.dotProduct(d);
            const t = v.dotProduct(v);
            let disc = b * b - t + this.components.get(i).getRadiusSquared();
            if (disc < BlobSolid.GRAZING_LIMIT) {
                continue;
            }
            disc = JavaMath.sqrt(disc);
            let t1 = b + disc;
            if (t1 < influenceStart) {
                t1 = 0.0;
            }
            let t0 = b - disc;
            if (t0 < influenceStart) {
                t0 = 0.0;
            }
            if (t1 === t0) {
                continue;
            }
            if (t1 < t0) {
                disc = t0;
                t0 = t1;
                t1 = disc;
            }
            let k: number;
            for (k = 0; k < intervals.size() && t0 > intervals.get(k).getBound(); k++) {
                // empty
            }
            if (k < intervals.size()) {
                // Smaller than an existing bound: insert it here.
                intervals.add(k, new BlobInfluenceInterval(true, i, t0));
                for (k = k + 1; k < intervals.size() && t1 > intervals.get(k).getBound(); k++) {
                    // empty
                }
                if (k < intervals.size()) {
                    intervals.add(k, new BlobInfluenceInterval(false, i, t1));
                } else {
                    intervals.add(new BlobInfluenceInterval(false, i, t1));
                }
            } else {
                intervals.add(new BlobInfluenceInterval(true, i, t0));
                intervals.add(new BlobInfluenceInterval(false, i, t1));
            }
        }
        return intervals.size();
    }

    // Summed component density at the point.
    public evaluateDensity(pos: Vector3Dd): number {
        let density = 0.0;
        for (let i = 0; i < this.components.size(); i++) {
            const component = this.components.get(i);
            const v = component.getCenter().subtract(pos);
            const len = v.dotProduct(v);
            if (len < component.getRadiusSquared()) {
                // Inside the radius of influence of this component.
                density +=
                    len * (len * component.getCoefficients()[0]! + component.getCoefficients()[1]!) +
                    component.getCoefficients()[2]!;
            }
        }
        return density;
    }

    // Gradient of threshold - density, pointing out of the solid.
    public gradient(point: Vector3Dd): Vector3Dd {
        let result = new Vector3Dd(0.0, 0.0, 0.0);
        for (let i = 0; i < this.components.size(); i++) {
            const component = this.components.get(i);
            const v = new Vector3Dd(
                point.x() - component.getCenter().x(),
                point.y() - component.getCenter().y(),
                point.z() - component.getCenter().z(),
            );
            const dist = v.x() * v.x() + v.y() * v.y() + v.z() * v.z();
            if (dist <= component.getRadiusSquared()) {
                const val = -2.0 * (2.0 * component.getCoefficients()[0]! * dist + component.getCoefficients()[1]!);
                result = new Vector3Dd(result.x() + val * v.x(), result.y() + val * v.y(), result.z() + val * v.z());
            }
        }
        return result;
    }

    /**
    With r^2 = t^2 + 2 t t1 + t0 along the ray, where t0 = (P - C).(P - C) and
    t1 = D.(P - C) for the unit direction D, each component density
    c0 r^4 + c1 r^2 + c2 is the quartic
        t^4 c0 + t^3 4 c0 t1 + t^2 (c1 + 2 c0 t0 + 4 c0 t1^2)
        + t 2 (c1 t1 + 2 c0 t0 t1) + c2 + c1 t0 + c0 t0^2,
    summed over the components whose influence intervals overlap.
    Appends the PovRay ray roots: distances d >= minimumDistance along the
    unit direction, inside the influence intervals that start no earlier than
    influenceStart (earlier starts are moved to 0), whose gradient is usable.
    */
    public intersect(
        p: Vector3Dd,
        d: Vector3Dd,
        minimumDistance: number,
        influenceStart: number,
        distances: ArrayList<number>,
    ): void {
        const intervals = new ArrayList<BlobInfluenceInterval>(2 * this.components.size());
        const cnt = this.determineInfluences(p, d, influenceStart, intervals);
        if (cnt === 0) {
            return;
        }
        // cpp java::ArrayList<double> addressed through a pointer; a plain array here.
        const componentTerms: number[] = new Array<number>(this.components.size() * 5).fill(0.0);
        const coefficients: number[] = [0, 0, 0, 0, 0];
        const roots: number[] = [0, 0, 0, 0];
        for (let i = 0; i < 4; i++) {
            coefficients[i] = 0.0;
        }
        coefficients[4] = -this.threshold;
        let inFlag = 0;
        for (let i = 0; i < cnt; i++) {
            const terms = intervals.get(i).getComponent() * 5;
            if (intervals.get(i).isStart()) {
                // A component starts influencing the ray: add its terms.
                inFlag++;
                const component = this.components.get(intervals.get(i).getComponent());
                const v = p.subtract(component.getCenter());
                const c0 = component.getCoefficients()[0]!;
                const c1 = component.getCoefficients()[1]!;
                const c2 = component.getCoefficients()[2]!;
                const t0 = v.dotProduct(v);
                const t1 = v.dotProduct(d);
                componentTerms[terms + 0] = c0;
                componentTerms[terms + 1] = 4.0 * c0 * t1;
                componentTerms[terms + 2] = 2.0 * c0 * (2.0 * t1 * t1 + t0) + c1;
                componentTerms[terms + 3] = 2.0 * t1 * (2.0 * c0 * t0 + c1);
                componentTerms[terms + 4] = c0 * t0 * t0 + c1 * t0 + c2;
                for (let j = 0; j < 5; j++) {
                    coefficients[j]! += componentTerms[terms + j]!;
                }
            } else {
                // A component stops influencing the ray: subtract its terms.
                for (let j = 0; j < 5; j++) {
                    coefficients[j]! -= componentTerms[terms + j]!;
                }
                if (--inFlag === 0) {
                    // No component affects the ray until the next start.
                    continue;
                }
            }
            let rootCount: number;
            if (!this.sturm) {
                rootCount = QuarticSolver.solve(coefficients, roots, minimumDistance, BlobSolid.SOLVER_EPSILON);
            } else if (JavaMath.abs(coefficients[0]!) < BlobSolid.COEFFICIENT_LIMIT) {
                if (JavaMath.abs(coefficients[1]!) < BlobSolid.COEFFICIENT_LIMIT) {
                    rootCount = QuadraticSolver.solve(coefficients, 2, roots);
                } else {
                    const cubicSolver = new PolynomialSolver(3, coefficients, 1);
                    rootCount = cubicSolver.solve(roots, minimumDistance, BlobSolid.SOLVER_EPSILON);
                }
            } else {
                const quarticSolver = new PolynomialSolver(4, coefficients);
                rootCount = quarticSolver.solve(roots, minimumDistance, BlobSolid.SOLVER_EPSILON);
            }
            for (let j = 0; j < rootCount; j++) {
                const dist = roots[j]!;
                // Keep roots inside the interval of the active components whose
                // gradient shows a real crossing rather than numerical noise.
                if (dist >= intervals.get(i).getBound() && dist <= intervals.get(i + 1).getBound()) {
                    const point = d.multiply(dist).add(p);
                    const n = this.gradient(point);
                    if (n.dotProduct(n) >= BlobSolid.GRAZING_LIMIT) {
                        distances.add(dist);
                    }
                }
            }
        }
    }

    private clampedFalloff(
        index: number,
        x: FloatingInterval,
        y: FloatingInterval,
        z: FloatingInterval,
    ): FloatingInterval {
        const component = this.components.get(index);
        const dx = x.subtract(FloatingInterval.exact(component.getCenter().x()));
        const dy = y.subtract(FloatingInterval.exact(component.getCenter().y()));
        const dz = z.subtract(FloatingInterval.exact(component.getCenter().z()));
        const q = dx
            .square()
            .add(dy.square())
            .add(dz.square())
            .divide(FloatingInterval.exact(component.getRadius()).square());
        if (!q.isValid()) return q;
        if (q.getLower() >= 1.0) return FloatingInterval.exact(0.0);
        // 1 - min(q, 1): the exact falloff is continuous and zero beyond the radius.
        return FloatingInterval.exact(1.0).subtract(
            FloatingInterval.spanning(JavaMath.max(q.getLower(), 0.0), JavaMath.min(q.getUpper(), 1.0)),
        );
    }

    public override evaluateField(point: Vector3Dd): number {
        return this.threshold - this.evaluateDensity(point);
    }

    public override evaluateFieldGradient(point: Vector3Dd): Vector3Dd {
        return this.gradient(point);
    }

    public override encloseField(x: FloatingInterval, y: FloatingInterval, z: FloatingInterval): FloatingInterval {
        if (!x.isValid() || !y.isValid() || !z.isValid() || !Double.isFinite(this.threshold))
            return new FloatingInterval();
        let result = FloatingInterval.exact(this.threshold);
        for (let i = 0; i < this.components.size(); i++) {
            if (this.components.get(i).isDegenerate()) return new FloatingInterval();
            const e = this.clampedFalloff(i, x, y, z);
            result = result.subtract(FloatingInterval.exact(this.components.get(i).getStrength()).multiply(e.square()));
        }
        return result;
    }

    public override encloseFieldGradient(
        x: FloatingInterval,
        y: FloatingInterval,
        z: FloatingInterval,
        gx: FloatingInterval[],
        gy: FloatingInterval[],
        gz: FloatingInterval[],
    ): boolean {
        if (!x.isValid() || !y.isValid() || !z.isValid()) return false;
        gx[0] = gy[0] = gz[0] = FloatingInterval.exact(0.0);
        for (let i = 0; i < this.components.size(); i++) {
            const component = this.components.get(i);
            if (component.isDegenerate()) return false;
            // grad (threshold - s (1 - q)^2) = 4 s (1 - q) (p - c) / radius^2
            const scale = FloatingInterval.exact(4.0 * component.getStrength())
                .multiply(this.clampedFalloff(i, x, y, z))
                .divide(FloatingInterval.exact(component.getRadius()).square());
            gx[0] = gx[0]!.add(scale.multiply(x.subtract(FloatingInterval.exact(component.getCenter().x()))));
            gy[0] = gy[0]!.add(scale.multiply(y.subtract(FloatingInterval.exact(component.getCenter().y()))));
            gz[0] = gz[0]!.add(scale.multiply(z.subtract(FloatingInterval.exact(component.getCenter().z()))));
        }
        return gx[0]!.isValid() && gy[0]!.isValid() && gz[0]!.isValid();
    }

    public override findRayRoots(
        origin: Vector3Dd,
        unitDirection: Vector3Dd,
        _maximumDistance: number,
        distances: ArrayList<number>,
    ): boolean {
        for (let i = 0; i < this.components.size(); i++) if (this.components.get(i).isDegenerate()) return false;
        if (!Double.isFinite(this.threshold)) return false;
        this.intersect(origin, unitDirection, 0.0, 0.0, distances);
        return true;
    }

    public override getBounds(result: GeometryBounds): void {
        if (!Double.isFinite(this.threshold)) {
            result.setIndeterminate();
            return;
        }
        for (let i = 0; i < this.components.size(); i++)
            if (this.components.get(i).isDegenerate()) {
                result.setIndeterminate();
                return;
            }
        // Density above a nonnegative threshold needs a positive component.
        if (this.threshold < 0) {
            result.setUnbounded();
            return;
        }
        let any = false;
        let minimum = new Vector3Dd();
        let maximum = new Vector3Dd();
        for (let i = 0; i < this.components.size(); i++) {
            const component = this.components.get(i);
            if (component.getStrength() <= 0) continue;
            const r = FloatingInterval.exact(component.getRadius());
            const c: number[] = [component.getCenter().x(), component.getCenter().y(), component.getCenter().z()];
            const low: number[] = [0, 0, 0];
            const high: number[] = [0, 0, 0];
            for (let k = 0; k < 3; k++) {
                low[k] = FloatingInterval.exact(c[k]!).subtract(r).getLower();
                high[k] = FloatingInterval.exact(c[k]!).add(r).getUpper();
            }
            const a = new Vector3Dd(low[0]!, low[1]!, low[2]!);
            const b = new Vector3Dd(high[0]!, high[1]!, high[2]!);
            if (!any) {
                minimum = a;
                maximum = b;
                any = true;
                continue;
            }
            minimum = new Vector3Dd(
                JavaMath.min(minimum.x(), a.x()),
                JavaMath.min(minimum.y(), a.y()),
                JavaMath.min(minimum.z(), a.z()),
            );
            maximum = new Vector3Dd(
                JavaMath.max(maximum.x(), b.x()),
                JavaMath.max(maximum.y(), b.y()),
                JavaMath.max(maximum.z(), b.z()),
            );
        }
        if (!any) {
            result.setEmpty();
            return;
        }
        if (!result.setFinite(minimum, maximum)) result.setIndeterminate();
    }
}
