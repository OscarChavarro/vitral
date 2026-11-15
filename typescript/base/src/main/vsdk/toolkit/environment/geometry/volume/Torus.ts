//= References:                                                             =
//= [WAGN2004] Wagner, Max. "Ray/Torus Intersection". CS400 course homework =
//= report for CS400 class                                                  =

// VSDK classes
import type { Ray } from "../element/Ray.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import type { RayHit } from "../element/RayHit.js";
import { Solid } from "./Solid.js";

/**
Current implementation is based on [WAGN2004].

The Java source of record also keeps a fully commented-out `calculateRoot`
analytic-quartic attempt ("Unused and not working yet method"); it is not
compiled Java code and therefore has no TypeScript counterpart.
*/
export class Torus extends Solid {
    /** The roots the quartic solver gives for a ray are accepted only if the
    point they define is on the surface of the torus, within this distance
    (relative to the major radius): verified roots have errors around 1e-11,
    and spurious ones (the solver gives some) errors of the order of 0.1 */
    private static readonly ROOT_VALIDATION_TOLERANCE = 1.0e-6;

    private majorRadius: number;
    private minorRadius: number;

    /**
    @param inMajorRadius
    @param inMinorRadius
    */
    public constructor(inMajorRadius: number, inMinorRadius: number) {
        super();
        this.majorRadius = inMajorRadius;
        this.minorRadius = inMinorRadius;
    }

    /**
    @return the majorRadius
    */
    public getMajorRadius(): number {
        return this.majorRadius;
    }

    /**
     * @param rMajor the majorRadius to set
     */
    public setMajorRadius(rMajor: number): void {
        this.majorRadius = rMajor;
    }

    /**
    @return the minorRadius
    */
    public getMinorRadius(): number {
        return this.minorRadius;
    }

    /**
    @param rMinor the minorRadius to set
    */
    public setMinorRadius(rMinor: number): void {
        this.minorRadius = rMinor;
    }

    public doIntersectionFirstHit(inOut_ray: Ray): Ray | null;
    public override doIntersectionFirstHit(inRay: Ray, outHit: RayHit): boolean;
    public override doIntersectionFirstHit(inRay: Ray, outHit: RayHit | null): boolean;
    public override doIntersectionFirstHit(inRay: Ray, outHit?: RayHit | null): Ray | null | boolean {
        if (outHit !== undefined) {
            const hit = this.doIntersectionFirstHit(inRay);
            if (hit === null) {
                return false;
            }
            if (outHit !== null) {
                outHit.setRay(hit);
                this.doExtraInformation(hit, hit.getT(), outHit);
                outHit.setRay(hit);
            }
            return true;
        }

        let inOut_ray = inRay;
        const origin = inOut_ray.getOrigin();

        inOut_ray = inOut_ray.withDirection(inOut_ray.getDirection().normalized());
        const d = inOut_ray.getDirection();

        // Distance from the original origin to the point nearest to the center
        const shift = -origin.dotProduct(d);
        const p = origin.add(d.multiply(shift));

        const alpha = d.dotProduct(d);
        const beta = 2 * p.dotProduct(d);
        const gama = p.dotProduct(p) - this.minorRadius * this.minorRadius - this.majorRadius * this.majorRadius;

        const a4 = alpha * alpha;
        const a3 = 2 * alpha * beta;
        const a2 = beta * beta + 2 * alpha * gama + 4 * (this.majorRadius * this.majorRadius) * (d.z() * d.z());
        const a1 = 2 * beta * gama + 8 * (this.majorRadius * this.majorRadius) * p.z() * d.z();
        const a0 =
            gama * gama +
            4 * (this.majorRadius * this.majorRadius) * (p.z() * p.z()) -
            4 * (this.majorRadius * this.majorRadius) * (this.minorRadius * this.minorRadius);

        const roots = Torus.findRealRoots([a0, a1, a2, a3, a4]);
        let mRoot = 0;
        let count = 0;

        for (const root of roots) {
            // Distance along the ray that was given
            const t = shift + root;

            if (t > 0 && this.isOnSurface(origin, d, t)) {
                if (count === 0 || t < mRoot) {
                    mRoot = t;
                    count++;
                }
            }
        }

        if (count === 0) {
            return null;
        } else {
            return inOut_ray.withT(mRoot);
        }
    }

    /**
    Finds the real roots of a polynomial, in ascending order, isolating them
    with the roots of its derivative (that separate the intervals where the
    polynomial is monotonic) and bisecting the intervals where it changes its
    sign. Roots of even multiplicity (tangencies) are found only if they are
    a root of the derivative too.
    @param polynomial coefficients, in ascending order of the powers
    @return the real roots
    */
    private static findRealRoots(polynomial: number[]): number[] {
        let degree = polynomial.length - 1;

        while (degree > 0 && polynomial[degree] === 0.0) {
            degree--;
        }
        if (degree <= 0) {
            return [];
        }
        if (degree === 1) {
            return [-polynomial[0]! / polynomial[1]!];
        }

        const derivative: number[] = new Array<number>(degree);

        for (let k = 1; k <= degree; k++) {
            derivative[k - 1] = k * polynomial[k]!;
        }
        const critical = Torus.findRealRoots(derivative);

        // Cauchy bound: every root is inside it
        let bound = 0;

        for (let k = 0; k < degree; k++) {
            bound = Math.max(bound, Math.abs(polynomial[k]! / polynomial[degree]!));
        }
        bound += 1;

        const limits: number[] = new Array<number>(critical.length + 2);

        limits[0] = -bound;
        for (let k = 0; k < critical.length; k++) {
            limits[k + 1] = Math.max(-bound, Math.min(bound, critical[k]!));
        }
        limits[limits.length - 1] = bound;

        const found: number[] = new Array<number>(degree);
        let count = 0;

        for (let k = 0; k < limits.length - 1 && count < degree; k++) {
            let low = limits[k]!;
            let high = limits[k + 1]!;
            const valueAtLow = Torus.evaluate(polynomial, degree, low);
            const valueAtHigh = Torus.evaluate(polynomial, degree, high);

            if (valueAtLow === 0.0) {
                if (count === 0 || found[count - 1] !== low) {
                    found[count++] = low;
                }
            } else if (valueAtHigh !== 0.0 && valueAtLow < 0.0 !== valueAtHigh < 0.0) {
                for (let iteration = 0; iteration < 200; iteration++) {
                    const middle = 0.5 * (low + high);

                    if (middle <= low || middle >= high) {
                        break;
                    }
                    if (Torus.evaluate(polynomial, degree, middle) < 0.0 === valueAtLow < 0.0) {
                        low = middle;
                    } else {
                        high = middle;
                    }
                }
                found[count++] = 0.5 * (low + high);
            }
        }
        if (count < degree && Torus.evaluate(polynomial, degree, bound) === 0.0) {
            found[count++] = bound;
        }
        return found.slice(0, count);
    }

    private static evaluate(polynomial: number[], degree: number, x: number): number {
        let value = 0;

        for (let k = degree; k >= 0; k--) {
            value = value * x + polynomial[k]!;
        }
        return value;
    }

    /**
    Checks a root of the intersection equation: the solver of the equation can
    give real roots that do not correspond to any point of the torus.
    @param origin origin of the ray
    @param direction unit direction of the ray
    @param t distance along the ray to check
    @return true if the point of the ray at the distance is on the surface of
    the torus
    */
    private isOnSurface(origin: Vector3Dd, direction: Vector3Dd, t: number): boolean {
        const hit = origin.add(direction.multiply(t));
        const distanceToCircle = Math.hypot(Math.hypot(hit.x(), hit.y()) - this.majorRadius, hit.z());
        const tolerance = Torus.ROOT_VALIDATION_TOLERANCE * Math.max(this.majorRadius, this.minorRadius);

        return Math.abs(distanceToCircle - this.minorRadius) <= tolerance;
    }

    public override doExtraInformation(inRay: Ray, intT: number, outData: RayHit | null): void;
    public override doExtraInformation(inRay: Ray, intT: number, outData: RayHit): void;
    public override doExtraInformation(inRay: Ray, intT: number, outData: RayHit | null): void {
        if (outData === null) {
            return;
        }
        const hitPoint = new Vector3Dd(
            inRay.getOrigin().x() + intT * inRay.getDirection().x(),
            inRay.getOrigin().y() + intT * inRay.getDirection().y(),
            inRay.getOrigin().z() + intT * inRay.getDirection().z(),
        );
        outData.point = hitPoint;
        const r2 = this.minorRadius * this.minorRadius;
        const R2 = this.majorRadius * this.majorRadius;
        const hitNormSquared = hitPoint.x() * hitPoint.x() + hitPoint.y() * hitPoint.y() + hitPoint.z() * hitPoint.z();

        outData.normal = new Vector3Dd(
            4 * hitPoint.x() * (hitNormSquared - r2 - R2),
            4 * hitPoint.y() * (hitNormSquared - r2 - R2),
            4 * hitPoint.z() * (hitNormSquared - r2 - R2) + 8 * R2 * hitPoint.z(),
        ).normalized();
    }

    /**
    @return a new 6 valued double array containing the coordinates of a min-max
    bounding box for current geometry.
    */
    public override getMinMax(): Float64Array {
        const minmax = new Float64Array(6);

        minmax[0] = -(this.majorRadius + this.minorRadius);
        minmax[1] = -(this.majorRadius + this.minorRadius);
        minmax[2] = this.minorRadius;
        minmax[3] = this.majorRadius + this.minorRadius;
        minmax[4] = this.majorRadius + this.minorRadius;
        minmax[5] = -this.minorRadius;

        return minmax;
    }
}
