import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { SolverPolynomialQuarticBairstow } from "../../../processing/SolverPolynomialQuarticBairstow.js";
import { Ray } from "../element/Ray.js";
import { RayHit } from "../element/RayHit.js";
import { Solid } from "./Solid.js";

/**
 * Origin-centered torus around the Z axis.  `majorRadius` is the distance
 * from the origin to the tube centre; `minorRadius` is the tube radius.
 *
 * Current implementation is based on [WAGN2004].
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

    public doIntersectionFirstHit(ray: Ray): Ray | null;
    public doIntersectionFirstHit(ray: Ray, hit: RayHit): boolean;
    public doIntersectionFirstHit(ray: Ray, hit?: RayHit): Ray | null | boolean {
        const normalizedRay = ray.withDirection(ray.getDirection().normalized());
        const p = normalizedRay.getOrigin(),
            d = normalizedRay.getDirection();
        const r2 = this.minorRadius ** 2,
            R2 = this.majorRadius ** 2;
        const alpha = d.dotProduct(d),
            beta = 2 * p.dotProduct(d);
        const gamma = p.dotProduct(p) - r2 - R2;
        const solver = new SolverPolynomialQuarticBairstow(
            alpha * alpha,
            2 * alpha * beta,
            beta * beta + 2 * alpha * gamma + 4 * R2 * d.z() * d.z(),
            2 * beta * gamma + 8 * R2 * p.z() * d.z(),
            gamma * gamma + 4 * R2 * p.z() * p.z() - 4 * R2 * r2,
        );
        const real = solver.getReal(),
            imaginary = solver.getImg();
        let minimumRoot = 0,
            count = 0;
        for (let i = 0; i < 4; i++)
            if (imaginary[i] === 0 && real[i]! > 0) {
                if (count === 0) {
                    minimumRoot = real[i]!;
                    count++;
                } else if (real[i]! < minimumRoot) minimumRoot = real[i]!;
            }
        const result = count === 0 ? null : normalizedRay.withT(minimumRoot);
        if (hit === undefined) return result;
        if (result === null) return false;
        hit.setRay(result);
        this.doExtraInformation(result, result.getT(), hit);
        hit.setRay(result);
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

    public getMinMax(): Float64Array {
        const radius = this.majorRadius + this.minorRadius;
        return new Float64Array([-radius, -radius, -this.minorRadius, radius, radius, this.minorRadius]);
    }
}
