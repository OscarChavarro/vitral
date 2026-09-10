import { VSDK } from "../../../common/VSDK.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { Ray } from "../element/Ray.js";
import { RayHit } from "../element/RayHit.js";
import { Solid } from "./Solid.js";
import { PolyhedralBoundedSolid } from "./polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidBuilder } from "./polyhedralBoundedSolid/PolyhedralBoundedSolidBuilder.js";

/** Closed cone/frustum along +Z: base radius r1 at z=0 and top radius r2 at z=h. */
export class Cone extends Solid {
    public constructor(
        private r1: number,
        private r2: number,
        private h: number,
    ) {
        super();
    }
    public getBaseRadius(): number {
        return this.r1;
    }
    public getTopRadius(): number {
        return this.r2;
    }
    public getHeight(): number {
        return this.h;
    }
    public setBaseRadius(value: number): void {
        this.r1 = value;
    }
    public setTopRadius(value: number): void {
        this.r2 = value;
    }
    public setHeight(value: number): void {
        this.h = value;
    }
    public doIntersectionFirstHit(ray: Ray, hit: RayHit): boolean {
        const o = ray.getOrigin(),
            d = ray.getDirection(),
            k = (this.r2 - this.r1) / this.h,
            candidates: { t: number; n: Vector3Dd }[] = [],
            q = this.r1 + k * o.z(),
            a = d.x() ** 2 + d.y() ** 2 - (k * d.z()) ** 2,
            b = 2 * (o.x() * d.x() + o.y() * d.y() - q * k * d.z()),
            c = o.x() ** 2 + o.y() ** 2 - q * q;
        if (Math.abs(a) > VSDK.EPSILON) {
            const disc = b * b - 4 * a * c;
            if (disc >= 0)
                for (const t of [(-b - Math.sqrt(disc)) / (2 * a), (-b + Math.sqrt(disc)) / (2 * a)]) {
                    const z = o.z() + d.z() * t;
                    if (t > VSDK.EPSILON && z >= 0 && z <= this.h) {
                        const p = o.add(d.multiply(t));
                        candidates.push({ t, n: new Vector3Dd(p.x(), p.y(), -k * (this.r1 + k * z)).normalized() });
                    }
                }
        }
        for (const [z, r, n] of [
            [0, this.r1, new Vector3Dd(0, 0, -1)],
            [this.h, this.r2, new Vector3Dd(0, 0, 1)],
        ] as const)
            if (Math.abs(d.z()) > VSDK.EPSILON) {
                const t = (z - o.z()) / d.z(),
                    x = o.x() + d.x() * t,
                    y = o.y() + d.y() * t;
                if (t > VSDK.EPSILON && x * x + y * y <= r * r) candidates.push({ t, n });
            }
        const best = candidates.sort((left, right) => left.t - right.t)[0];
        if (best === undefined) return false;
        hit.setRay(ray.withT(best.t));
        if (hit.needsPoint()) hit.p = o.add(d.multiply(best.t));
        if (hit.needsNormal()) hit.n = best.n;
        return true;
    }
    public override doExtraInformation(ray: Ray, _distance: number, outData: RayHit): void {
        const hit = new RayHit();
        if (this.doIntersectionFirstHit(ray.withT(Number.MAX_VALUE), hit)) outData.clone(hit);
    }
    public getMinMax(): Float64Array {
        const radius = Math.max(this.r1, this.r2);
        return new Float64Array([-radius, -radius, 0, radius, radius, this.h]);
    }
    public override doContainmentTest(point: Vector3Dd, tolerance: number): number {
        if (point.z() < -tolerance || point.z() > this.h + tolerance) return Cone.OUTSIDE;
        const radius = this.r1 + ((this.r2 - this.r1) * point.z()) / this.h,
            length = Math.hypot(point.x(), point.y());
        return length < radius - tolerance ? Cone.INSIDE : length > radius + tolerance ? Cone.OUTSIDE : Cone.LIMIT;
    }
    /** Polygonal approximation used by the Java B-rep export overload. */
    public override exportToPolyhedralBoundedSolid(): PolyhedralBoundedSolid;
    public override exportToPolyhedralBoundedSolid(
        circumferenceDivisions: number,
        heightDivisions?: number,
    ): PolyhedralBoundedSolid;
    public override exportToPolyhedralBoundedSolid(
        circumferenceDivisions = 9,
        _heightDivisions = 1,
    ): PolyhedralBoundedSolid {
        const count = Math.max(3, Math.floor(circumferenceDivisions)),
            points: Vector3Dd[] = [],
            faces: number[][] = [];
        for (let i = 0; i < count; i++) {
            const a = (2 * Math.PI * i) / count;
            points.push(new Vector3Dd(this.r1 * Math.cos(a), this.r1 * Math.sin(a), 0));
        }
        faces.push(Array.from({ length: count }, (_, i) => count - 1 - i));
        if (this.r2 <= VSDK.EPSILON) {
            const apex = points.length;
            points.push(new Vector3Dd(0, 0, this.h));
            for (let i = 0; i < count; i++) faces.push([i, (i + 1) % count, apex]);
        } else {
            for (let i = 0; i < count; i++) {
                const a = (2 * Math.PI * i) / count;
                points.push(new Vector3Dd(this.r2 * Math.cos(a), this.r2 * Math.sin(a), this.h));
            }
            faces.push(Array.from({ length: count }, (_, i) => count + i));
            for (let i = 0; i < count; i++) faces.push([i, (i + 1) % count, count + ((i + 1) % count), count + i]);
        }
        return PolyhedralBoundedSolidBuilder.fromPolygons(points, faces);
    }
}
