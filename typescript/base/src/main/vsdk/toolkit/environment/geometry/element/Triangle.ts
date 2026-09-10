import { FundamentalEntity } from "../../../common/FundamentalEntity.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { VSDK } from "../../../common/VSDK.js";
import { Containment } from "../../../processing/Containment.js";
import { Ray } from "./Ray.js";
import { Intersection } from "./Intersection.js";
export class Triangle extends FundamentalEntity {
    private normal = new Vector3Dd();
    public constructor(
        private p0 = 0,
        private p1 = 0,
        private p2 = 0,
    ) {
        super();
    }
    public getPoint0() {
        return this.p0;
    }
    public getPoint1() {
        return this.p1;
    }
    public getPoint2() {
        return this.p2;
    }
    public setPoint0(x: number) {
        this.p0 = x;
    }
    public setPoint1(x: number) {
        this.p1 = x;
    }
    public setPoint2(x: number) {
        this.p2 = x;
    }
    public getNormal() {
        return this.normal;
    }
    public setNormal(x: Vector3Dd) {
        this.normal = x;
    }
    public static doIntersectionWithTriangle(ray: Ray, a: Vector3Dd, b: Vector3Dd, c: Vector3Dd): Intersection | null {
        const e1 = b.subtract(a),
            e2 = c.subtract(a),
            h = ray.getDirection().crossProduct(e2),
            d = e1.dotProduct(h);
        if (Math.abs(d) < VSDK.EPSILON) return null;
        const f = 1 / d,
            s = ray.getOrigin().subtract(a),
            u = f * s.dotProduct(h);
        if (u < 0 || u > 1) return null;
        const q = s.crossProduct(e1),
            v = f * ray.getDirection().dotProduct(q);
        if (v < 0 || u + v > 1) return null;
        const t = f * e2.dotProduct(q);
        return t <= VSDK.EPSILON
            ? null
            : new Intersection(
                  t,
                  ray.getOrigin().add(ray.getDirection().multiply(t)),
                  e1.crossProduct(e2).normalized(),
              );
    }
    public static containmentTest(a: Vector3Dd, b: Vector3Dd, c: Vector3Dd, p: Vector3Dd, t: number): number {
        const ab = b.subtract(a),
            ac = c.subtract(a),
            ap = p.subtract(a),
            d1 = ab.dotProduct(ap),
            d2 = ac.dotProduct(ap),
            denom = ab.dotProduct(ab) * ac.dotProduct(ac) - ab.dotProduct(ac) ** 2;
        if (Math.abs(denom) < VSDK.EPSILON) return Containment.OUTSIDE;
        const u = (d1 * ac.dotProduct(ac) - d2 * ab.dotProduct(ac)) / denom,
            v = (d2 * ab.dotProduct(ab) - d1 * ab.dotProduct(ac)) / denom,
            q = a.add(ab.multiply(Math.max(0, Math.min(1, u)))).add(ac.multiply(Math.max(0, Math.min(1, v))));
        return q.subtract(p).length() <= t ? Containment.LIMIT : Containment.OUTSIDE;
    }
    public static minMax(a: Vector3Dd, b: Vector3Dd, c: Vector3Dd, m: Float64Array | number[]): void {
        m[0] = Math.min(a.x(), b.x(), c.x());
        m[1] = Math.min(a.y(), b.y(), c.y());
        m[2] = Math.min(a.z(), b.z(), c.z());
        m[3] = Math.max(a.x(), b.x(), c.x());
        m[4] = Math.max(a.y(), b.y(), c.y());
        m[5] = Math.max(a.z(), b.z(), c.z());
    }
    public override toString() {
        return `f < ${this.p0}, ${this.p1}, ${this.p2} >`;
    }
}
