//= References:                                                             =
//= [WAGN2004] Wagner, Max. "Ray/Torus Intersection". CS400 course homework =
//= report for CS400 class                                                  =

// VSDK classes
import type { Ray } from "../element/Ray.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import type { RayHit } from "../element/RayHit.js";
import { SolverPolynomialQuarticBairstow } from "../../../processing/SolverPolynomialQuarticBairstow.js";
import { Solid } from "./Solid.js";

/**
Current implementation is based on [WAGN2004].

The Java source of record also keeps a fully commented-out `calculateRoot`
analytic-quartic attempt ("Unused and not working yet method"); it is not
compiled Java code and therefore has no TypeScript counterpart.
*/
export class Torus extends Solid {
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
        const p = inOut_ray.getOrigin();

        inOut_ray = inOut_ray.withDirection(inOut_ray.getDirection().normalized());
        const d = inOut_ray.getDirection();

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

        const q = new SolverPolynomialQuarticBairstow(a4, a3, a2, a1, a0);

        const root = q.getReal();
        const rootImg = q.getImg();
        let mRoot = 0;
        let count = 0;

        for (let i = 0; i < 4; i++) {
            if (rootImg[i] === 0 && root[i]! > 0) {
                if (count === 0) {
                    mRoot = root[i]!;
                    count++;
                } else if (root[i]! < mRoot) {
                    mRoot = root[i]!;
                }
            }
        }

        if (count === 0) {
            return null;
        } else {
            return inOut_ray.withT(mRoot); //calculateRoot(a4, a3, a2,  a1, a0, inOut_ray);
        }
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
        outData.p = hitPoint;
        const r2 = this.minorRadius * this.minorRadius;
        const R2 = this.majorRadius * this.majorRadius;
        const hitNormSquared = hitPoint.x() * hitPoint.x() + hitPoint.y() * hitPoint.y() + hitPoint.z() * hitPoint.z();

        outData.n = new Vector3Dd(
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
