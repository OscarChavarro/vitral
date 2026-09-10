import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { Ray } from "../element/Ray.js";
import { RayHit } from "../element/RayHit.js";
import { Solid } from "./Solid.js";
import { Cone } from "./Cone.js";
import { PolyhedralBoundedSolid } from "./polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidBuilder } from "./polyhedralBoundedSolid/PolyhedralBoundedSolidBuilder.js";

/** Arrow aligned with +Z, built from a cylindrical shaft and conical head. */
export class Arrow extends Solid {
    private base: Cone;
    private head: Cone;
    public constructor(
        private baseLength: number,
        private headLength: number,
        private baseRadius: number,
        private headRadius: number,
    ) {
        super();
        this.base = new Cone(baseRadius, baseRadius, baseLength);
        this.head = new Cone(headRadius, 0, headLength);
    }
    public getBaseLength(): number {
        return this.baseLength;
    }
    public setBaseLength(value: number): void {
        this.baseLength = value;
        this.base.setHeight(value);
    }
    public getHeadLength(): number {
        return this.headLength;
    }
    public setHeadLength(value: number): void {
        this.headLength = value;
        this.head.setHeight(value);
    }
    public getBaseRadius(): number {
        return this.baseRadius;
    }
    public setBaseRadius(value: number): void {
        this.baseRadius = value;
        this.base.setBaseRadius(value);
        this.base.setTopRadius(value);
    }
    public getHeadRadius(): number {
        return this.headRadius;
    }
    public setHeadRadius(value: number): void {
        this.headRadius = value;
        this.head.setBaseRadius(value);
    }
    public doIntersectionFirstHit(ray: Ray, hit: RayHit): boolean {
        const a = new RayHit(hit.requiredDetailMask(), hit.shouldStoreRay()),
            b = new RayHit(hit.requiredDetailMask(), hit.shouldStoreRay()),
            hasA = this.base.doIntersectionFirstHit(ray, a),
            shifted = new Ray(
                ray.getOrigin().withZ(ray.getOrigin().z() - this.baseLength),
                ray.getDirection(),
                ray.getT(),
            ),
            hasB = this.head.doIntersectionFirstHit(shifted, b);
        if (!hasA && !hasB) return false;
        const ta = hasA ? a.hitDistance() : Infinity,
            tb = hasB ? b.hitDistance() : Infinity;
        if (ta <= tb) {
            hit.clone(a);
            return true;
        }
        hit.clone(b);
        if (hit.needsPoint()) hit.p = hit.p.withZ(hit.p.z() + this.baseLength);
        hit.setRay(ray.withT(tb));
        return true;
    }
    public getMinMax(): Float64Array {
        const radius = Math.max(this.baseRadius, this.headRadius);
        return new Float64Array([-radius, -radius, 0, radius, radius, this.baseLength + this.headLength]);
    }
    public override doContainmentTest(point: Vector3Dd, tolerance: number): number {
        return point.z() <= this.baseLength
            ? this.base.doContainmentTest(point, tolerance)
            : this.head.doContainmentTest(point.withZ(point.z() - this.baseLength), tolerance);
    }
    public override exportToPolyhedralBoundedSolid(): PolyhedralBoundedSolid;
    public override exportToPolyhedralBoundedSolid(
        circumferenceDivisions: number,
        heightDivisions?: number,
    ): PolyhedralBoundedSolid;
    /** One connected rotational profile: bottom cap, shaft, shoulder, head and apex. */
    public override exportToPolyhedralBoundedSolid(
        circumferenceDivisions = 9,
        _heightDivisions = 1,
    ): PolyhedralBoundedSolid {
        const count = Math.max(3, Math.floor(circumferenceDivisions)),
            points: Vector3Dd[] = [],
            faces: number[][] = [],
            rings: [[number, number], [number, number], [number, number]] = [
                [0, this.baseRadius],
                [this.baseLength, this.baseRadius],
                [this.baseLength, this.headRadius],
            ];
        for (const [z, radius] of rings)
            for (let i = 0; i < count; i++) {
                const angle = (2 * Math.PI * i) / count;
                points.push(new Vector3Dd(radius * Math.cos(angle), radius * Math.sin(angle), z));
            }
        const apex = points.length;
        points.push(new Vector3Dd(0, 0, this.baseLength + this.headLength));
        faces.push(Array.from({ length: count }, (_, i) => count - 1 - i));
        for (let ring = 0; ring < rings.length - 1; ring++)
            for (let i = 0; i < count; i++) {
                const base = ring * count;
                faces.push([base + i, base + ((i + 1) % count), base + count + ((i + 1) % count), base + count + i]);
            }
        const top = (rings.length - 1) * count;
        for (let i = 0; i < count; i++) faces.push([top + i, top + ((i + 1) % count), apex]);
        return PolyhedralBoundedSolidBuilder.fromPolygons(points, faces);
    }
}
