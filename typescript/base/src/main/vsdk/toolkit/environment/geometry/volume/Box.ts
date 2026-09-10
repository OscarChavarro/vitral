import { VSDK } from "../../../common/VSDK.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { Ray } from "../element/Ray.js";
import { RayHit } from "../element/RayHit.js";
import { Solid } from "./Solid.js";
import { PolyhedralBoundedSolidBuilder } from "./polyhedralBoundedSolid/PolyhedralBoundedSolidBuilder.js";
import { PolyhedralBoundedSolid } from "./polyhedralBoundedSolid/PolyhedralBoundedSolid.js";

/** Axis-aligned box centered at the origin. */
export class Box extends Solid {
    private size: Vector3Dd;
    public constructor(size: Vector3Dd);
    public constructor(dx: number, dy: number, dz: number);
    public constructor(a: Vector3Dd | number, b?: number, c?: number) {
        super();
        this.size = a instanceof Vector3Dd ? new Vector3Dd(a) : new Vector3Dd(a, b!, c!);
    }
    public getSize(): Vector3Dd {
        return this.size;
    }
    public setSize(size: Vector3Dd): void;
    public setSize(dx: number, dy: number, dz: number): void;
    public setSize(a: Vector3Dd | number, b?: number, c?: number): void {
        this.size = a instanceof Vector3Dd ? new Vector3Dd(a) : new Vector3Dd(a, b!, c!);
    }
    public intersectRay(ray: Ray): Ray | null {
        const hit = new RayHit();
        return this.doIntersectionFirstHit(ray, hit) ? hit.ray() : null;
    }
    public doIntersectionFirstHit(ray: Ray, hit: RayHit): boolean {
        const h = this.hittingPlane(ray);
        if (h === null) return false;
        if (hit.shouldStoreRay() || hit.needsAnySurfaceData()) hit.setRay(ray.withT(h.t));
        else hit.setHitDistance(h.t);
        if (hit.needsAnySurfaceData()) this.setDetails(ray, h.t, h.plane, hit);
        return true;
    }
    public override doExtraInformation(ray: Ray, t: number, hit: RayHit): void {
        const p = ray.getOrigin().add(ray.getDirection().multiply(t));
        this.setDetails(ray, t, this.classifyPlane(p), hit);
    }
    public override doContainmentTest(p: Vector3Dd, t: number): number {
        const x = Math.abs(p.x()) - this.size.x() / 2,
            y = Math.abs(p.y()) - this.size.y() / 2,
            z = Math.abs(p.z()) - this.size.z() / 2;
        return x < -t && y < -t && z < -t ? Box.INSIDE : x > t || y > t || z > t ? Box.OUTSIDE : Box.LIMIT;
    }
    public getMinMax(): Float64Array {
        const x = this.size.x() / 2,
            y = this.size.y() / 2,
            z = this.size.z() / 2;
        return new Float64Array([-x, -y, -z, x, y, z]);
    }
    public override doCenterOfMass(): Vector3Dd {
        return new Vector3Dd();
    }
    public override exportToPolyhedralBoundedSolid(): PolyhedralBoundedSolid {
        const x = this.size.x() / 2,
            y = this.size.y() / 2,
            z = this.size.z() / 2;
        return PolyhedralBoundedSolidBuilder.fromPolygons(
            [
                new Vector3Dd(-x, -y, -z),
                new Vector3Dd(x, -y, -z),
                new Vector3Dd(x, y, -z),
                new Vector3Dd(-x, y, -z),
                new Vector3Dd(-x, -y, z),
                new Vector3Dd(x, -y, z),
                new Vector3Dd(x, y, z),
                new Vector3Dd(-x, y, z),
            ],
            [
                [0, 3, 2, 1],
                [4, 5, 6, 7],
                [0, 1, 5, 4],
                [1, 2, 6, 5],
                [2, 3, 7, 6],
                [3, 0, 4, 7],
            ],
        );
    }
    private hittingPlane(ray: Ray): { t: number; plane: number } | null {
        const o = ray.getOrigin(),
            d = ray.getDirection(),
            half = [this.size.x() / 2, this.size.y() / 2, this.size.z() / 2];
        let best: { t: number; plane: number } | null = null;
        const candidates: [[number, number, number], number, number][] = [
            [[0, 0, 1], half[2]!, 1],
            [[0, 0, 1], -half[2]!, 2],
            [[0, 1, 0], half[1]!, 3],
            [[0, 1, 0], -half[1]!, 4],
            [[1, 0, 0], half[0]!, 5],
            [[1, 0, 0], -half[0]!, 6],
        ];
        for (const [[nx, ny, nz], offset, plane] of candidates) {
            const den = nx * d.x() + ny * d.y() + nz * d.z();
            if (Math.abs(den) <= VSDK.EPSILON) continue;
            const t = (offset - (nx * o.x() + ny * o.y() + nz * o.z())) / den;
            if (t < -VSDK.EPSILON || (best !== null && t >= best.t)) continue;
            const p = o.add(d.multiply(t));
            if (
                Math.abs(p.x()) <= half[0]! + VSDK.EPSILON &&
                Math.abs(p.y()) <= half[1]! + VSDK.EPSILON &&
                Math.abs(p.z()) <= half[2]! + VSDK.EPSILON
            )
                best = { t, plane };
        }
        return best;
    }
    private classifyPlane(p: Vector3Dd): number {
        const x = this.size.x() / 2,
            y = this.size.y() / 2,
            z = this.size.z() / 2;
        const distances = [
            Math.abs(p.z() - z),
            Math.abs(p.z() + z),
            Math.abs(p.y() - y),
            Math.abs(p.y() + y),
            Math.abs(p.x() - x),
            Math.abs(p.x() + x),
        ];
        let best = 0;
        for (let i = 1; i < distances.length; i++) if (distances[i]! < distances[best]!) best = i;
        return best + 1;
    }
    private setDetails(ray: Ray, t: number, plane: number, hit: RayHit): void {
        const p = ray.getOrigin().add(ray.getDirection().multiply(t));
        if (hit.needsPoint()) hit.p = p;
        if (hit.needsNormal()) hit.n = this.normal(plane);
        if (hit.needsTangent()) hit.t = this.tangent(plane);
        if (hit.needsTextureCoordinates()) {
            const sx = this.size.x(),
                sy = this.size.y(),
                sz = this.size.z();
            switch (plane) {
                case 1:
                    hit.u = p.y() / sy - 0.5;
                    hit.v = 1 - (p.x() / sx - 0.5);
                    break;
                case 2:
                    hit.u = p.y() / sy - 0.5;
                    hit.v = p.x() / sx - 0.5;
                    break;
                case 3:
                    hit.u = 1 - (p.x() / sx - 0.5);
                    hit.v = p.z() / sz - 0.5;
                    break;
                case 4:
                    hit.u = p.x() / sx - 0.5;
                    hit.v = p.z() / sz - 0.5;
                    break;
                case 5:
                    hit.u = p.y() / sy - 0.5;
                    hit.v = p.z() / sz - 0.5;
                    break;
                case 6:
                    hit.u = 1 - (p.y() / sy - 0.5);
                    hit.v = p.z() / sz - 0.5;
                    break;
            }
        }
    }
    private normal(plane: number): Vector3Dd {
        return [
            new Vector3Dd(0, 0, 1),
            new Vector3Dd(0, 0, -1),
            new Vector3Dd(0, 1, 0),
            new Vector3Dd(0, -1, 0),
            new Vector3Dd(1, 0, 0),
            new Vector3Dd(-1, 0, 0),
        ][plane - 1]!;
    }
    private tangent(plane: number): Vector3Dd {
        return [
            new Vector3Dd(0, 1, 0),
            new Vector3Dd(0, 1, 0),
            new Vector3Dd(-1, 0, 0),
            new Vector3Dd(1, 0, 0),
            new Vector3Dd(0, 1, 0),
            new Vector3Dd(0, -1, 0),
        ][plane - 1]!;
    }
}
