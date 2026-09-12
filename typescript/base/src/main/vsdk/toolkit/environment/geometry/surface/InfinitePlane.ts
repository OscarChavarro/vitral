import { HalfSpace } from "./HalfSpace.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { VSDK } from "../../../common/VSDK.js";
import { Ray } from "../element/Ray.js";
import { RayHit } from "../element/RayHit.js";
export class InfinitePlane extends HalfSpace<Ray, RayHit> {
    private a: number;
    private b: number;
    private c: number;
    private d: number;
    public constructor(a: InfinitePlane);
    public constructor(a: number, b: number, c: number, d: number);
    public constructor(a: Vector3Dd, b: Vector3Dd);
    public constructor(a: Vector3Dd, b: Vector3Dd, c: Vector3Dd);
    public constructor(
        x: InfinitePlane | Vector3Dd | number,
        y?: Vector3Dd | number,
        z?: Vector3Dd | number,
        w?: number,
    ) {
        super();
        if (x instanceof InfinitePlane) {
            this.a = x.a;
            this.b = x.b;
            this.c = x.c;
            this.d = x.d;
        } else if (x instanceof Vector3Dd && y instanceof Vector3Dd && z instanceof Vector3Dd) {
            const n = y.subtract(x).normalized().crossProduct(z.subtract(x).normalized()).normalized();
            this.a = n.x();
            this.b = n.y();
            this.c = n.z();
            this.d = -n.dotProduct(x);
        } else if (x instanceof Vector3Dd && y instanceof Vector3Dd) {
            const n = x.normalized();
            this.a = n.x();
            this.b = n.y();
            this.c = n.z();
            this.d = -n.dotProduct(y);
        } else {
            this.a = x as number;
            this.b = y as number;
            this.c = z as number;
            this.d = w!;
        }
    }
    public override clone(other?: InfinitePlane): InfinitePlane | void {
        if (other === undefined) return new InfinitePlane(this);
        this.a = other.a;
        this.b = other.b;
        this.c = other.c;
        this.d = other.d;
    }
    public doIntersectionFirstHit(ray: Ray): Ray | null;
    public doIntersectionFirstHit(ray: Ray, hit: RayHit): boolean;
    public doIntersectionFirstHit(ray: Ray, hit?: RayHit): Ray | null | boolean {
        if (hit === undefined) {
            const denominator =
                this.a * ray.getDirection().x() + this.b * ray.getDirection().y() + this.c * ray.getDirection().z();
            if (Math.abs(denominator) < VSDK.EPSILON) return null;
            const t =
                -(this.a * ray.getOrigin().x() + this.b * ray.getOrigin().y() + this.c * ray.getOrigin().z() + this.d) /
                denominator;

            if (t < 0) return null;

            return ray.withT(t);
        }

        const r = this.doIntersectionFirstHit(ray);
        if (r === null) {
            return false;
        }
        if (hit !== null) {
            hit.setRay(r);
            this.doExtraInformation(r, r.getT(), hit);
        }
        return true;
    }
    public doIntersectionWithNegative(ray: Ray): Ray | null {
        const denominator =
            this.a * ray.getDirection().x() + this.b * ray.getDirection().y() + this.c * ray.getDirection().z();
        if (Math.abs(denominator) < VSDK.EPSILON) {
            const r = new Ray(ray.getOrigin(), ray.getDirection().multiply(-1));
            const hit = this.doIntersectionFirstHit(r);
            if (hit !== null) {
                return ray.withT(-hit.getT());
            } else {
                return null;
            }
        }
        const t =
            -(this.a * ray.getOrigin().x() + this.b * ray.getOrigin().y() + this.c * ray.getOrigin().z() + this.d) /
            denominator;

        return ray.withT(t);
    }
    public doContainmentTestHalfSpace(p: Vector3Dd, t: number) {
        const q = this.pointDistance(p);
        return q > t ? InfinitePlane.OUTSIDE : q < -t ? InfinitePlane.INSIDE : InfinitePlane.LIMIT;
    }
    public override doContainmentTest(p: Vector3Dd, t: number) {
        const q = this.pointDistance(p);
        return q > t ? InfinitePlane.OUTSIDE : q < -t ? -InfinitePlane.INSIDE : InfinitePlane.LIMIT;
    }
    public override doExtraInformation(ray: Ray, t: number, out: RayHit) {
        out.p = ray.getOrigin().add(ray.getDirection().multiply(t));
        out.n = this.getNormal();
    }
    public getMinMax() {
        return new Float64Array([-Infinity, -Infinity, -Infinity, Infinity, Infinity, Infinity]);
    }
    public getNormal() {
        return new Vector3Dd(this.a, this.b, this.c).normalized();
    }
    public getD() {
        return this.d;
    }
    public setNormal(x: Vector3Dd) {
        const n = x.normalized();
        this.a = n.x();
        this.b = n.y();
        this.c = n.z();
    }
    public setD(x: number) {
        this.d = x;
    }
    public setFromPointNormal(p: Vector3Dd, n: Vector3Dd) {
        this.setNormal(n);
        this.d = -(this.a * p.x() + this.b * p.y() + this.c * p.z());
    }
    public pointDistance(p: Vector3Dd) {
        return this.a * p.x() + this.b * p.y() + this.c * p.z() + this.d;
    }
    public projectPoint(p: Vector3Dd) {
        return p.subtract(this.getNormal().multiply(this.pointDistance(p)));
    }
    public mirrorPoint(p: Vector3Dd) {
        return p.subtract(this.getNormal().multiply(2 * this.pointDistance(p)));
    }
    public overlapsWith(o: InfinitePlane, t: number) {
        const l1 = Math.hypot(this.a, this.b, this.c),
            l2 = Math.hypot(o.a, o.b, o.c);
        return (
            Math.abs(o.a / l2 - this.a / l1) <= t &&
            Math.abs(o.b / l2 - this.b / l1) <= t &&
            Math.abs(o.c / l2 - this.c / l1) <= t &&
            Math.abs(o.d / l2 - this.d / l1) <= t
        );
    }
    public getA() {
        return this.a;
    }
    public setA(x: number) {
        this.a = x;
    }
    public getB() {
        return this.b;
    }
    public setB(x: number) {
        this.b = x;
    }
    public getC() {
        return this.c;
    }
    public setC(x: number) {
        this.c = x;
    }
    public override toString() {
        return `InfinitePlane: N=<${VSDK.formatDouble(this.a)}, ${VSDK.formatDouble(this.b)}, ${VSDK.formatDouble(this.c)}>, D=${VSDK.formatDouble(this.d)}`;
    }
}
