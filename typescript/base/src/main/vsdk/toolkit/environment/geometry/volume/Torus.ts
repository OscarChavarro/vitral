import { VSDK } from "../../../common/VSDK.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { Ray } from "../element/Ray.js";
import { RayHit } from "../element/RayHit.js";
import { Solid } from "./Solid.js";

/**
 * Origin-centered torus around the Z axis.  `majorRadius` is the distance
 * from the origin to the tube centre; `minorRadius` is the tube radius.
 *
 * Its ray equation is the quartic described by [WAGN2004].  The Java version
 * delegates its real roots to `SolverPolynomialQuarticBairstow`; here roots
 * are isolated using the derivative hierarchy and refined by bisection.  That
 * keeps the primitive available in every TypeScript runtime.
 */
export class Torus extends Solid {
    public constructor(
        private majorRadius: number,
        private minorRadius: number,
    ) {
        super();
    }

    public getMajorRadius(): number {
        return this.majorRadius;
    }
    public setMajorRadius(radius: number): void {
        this.majorRadius = radius;
    }
    public getMinorRadius(): number {
        return this.minorRadius;
    }
    public setMinorRadius(radius: number): void {
        this.minorRadius = radius;
    }

    private static evaluate(coefficients: readonly number[], x: number): number {
        return coefficients.reduce((value, coefficient) => value * x + coefficient, 0);
    }

    /** Return all real roots, including repeated roots, of a polynomial of degree <= 4. */
    private static realRoots(coefficients: readonly number[]): number[] {
        let first = 0;
        while (first < coefficients.length - 1 && Math.abs(coefficients[first]!) <= VSDK.EPSILON) first++;
        const polynomial = coefficients.slice(first);
        const degree = polynomial.length - 1;
        if (degree <= 0) return [];
        if (degree === 1) return [-polynomial[1]! / polynomial[0]!];
        const leading = Math.abs(polynomial[0]!);
        const bound = 1 + Math.max(...polynomial.slice(1).map((value) => Math.abs(value) / leading));
        const derivative = polynomial.slice(0, -1).map((value, index) => value * (degree - index));
        const critical = Torus.realRoots(derivative)
            .filter((value) => value > -bound && value < bound)
            .sort((a, b) => a - b);
        const points = [-bound, ...critical, bound];
        const roots: number[] = [];
        const add = (value: number): void => {
            if (!roots.some((root) => Math.abs(root - value) <= 1e-8 * Math.max(1, Math.abs(value)))) roots.push(value);
        };
        for (const point of points)
            if (Math.abs(Torus.evaluate(polynomial, point)) <= 1e-9 * Math.max(1, leading)) add(point);
        for (let index = 0; index + 1 < points.length; index++) {
            let left = points[index]!,
                right = points[index + 1]!;
            let leftValue = Torus.evaluate(polynomial, left),
                rightValue = Torus.evaluate(polynomial, right);
            if (leftValue * rightValue >= 0) continue;
            for (let iteration = 0; iteration < 80; iteration++) {
                const middle = (left + right) / 2;
                const middleValue = Torus.evaluate(polynomial, middle);
                if (leftValue * middleValue <= 0) {
                    right = middle;
                    rightValue = middleValue;
                } else {
                    left = middle;
                    leftValue = middleValue;
                }
            }
            add((left + right) / 2);
        }
        return roots;
    }

    public intersectRay(ray: Ray): Ray | null {
        const p = ray.getOrigin(),
            d = ray.getDirection();
        const r2 = this.minorRadius ** 2,
            R2 = this.majorRadius ** 2;
        const alpha = d.dotProduct(d),
            beta = 2 * p.dotProduct(d);
        const gamma = p.dotProduct(p) - r2 - R2;
        const roots = Torus.realRoots([
            alpha ** 2,
            2 * alpha * beta,
            beta ** 2 + 2 * alpha * gamma + 4 * R2 * d.z() ** 2,
            2 * beta * gamma + 8 * R2 * p.z() * d.z(),
            gamma ** 2 + 4 * R2 * p.z() ** 2 - 4 * R2 * r2,
        ]);
        const distance = roots.filter((root) => root > VSDK.EPSILON).sort((a, b) => a - b)[0];
        return distance === undefined ? null : ray.withT(distance);
    }

    public doIntersectionFirstHit(ray: Ray, hit: RayHit): boolean {
        const result = this.intersectRay(ray);
        if (result === null) return false;
        if (hit.shouldStoreRay() || hit.needsAnySurfaceData()) {
            hit.setRay(result);
            if (hit.needsAnySurfaceData()) this.doExtraInformation(result, result.getT(), hit);
        } else hit.setHitDistance(result.getT());
        return true;
    }

    public override doExtraInformation(ray: Ray, distance: number, hit: RayHit): void {
        const point = ray.getOrigin().add(ray.getDirection().multiply(distance));
        if (hit.needsPoint()) hit.p = point;
        if (!hit.needsNormal()) return;
        const r2 = this.minorRadius ** 2,
            R2 = this.majorRadius ** 2,
            length2 = point.dotProduct(point);
        hit.n = new Vector3Dd(
            4 * point.x() * (length2 - r2 - R2),
            4 * point.y() * (length2 - r2 - R2),
            4 * point.z() * (length2 - r2 + R2),
        ).normalized();
    }

    public override doContainmentTest(point: Vector3Dd, tolerance: number): number {
        const radialDistance = Math.hypot(point.x(), point.y());
        const tubeDistance = Math.hypot(radialDistance - this.majorRadius, point.z());
        return tubeDistance < this.minorRadius - tolerance
            ? Torus.INSIDE
            : tubeDistance > this.minorRadius + tolerance
              ? Torus.OUTSIDE
              : Torus.LIMIT;
    }

    public getMinMax(): Float64Array {
        const radius = this.majorRadius + this.minorRadius;
        return new Float64Array([-radius, -radius, -this.minorRadius, radius, radius, this.minorRadius]);
    }
}
