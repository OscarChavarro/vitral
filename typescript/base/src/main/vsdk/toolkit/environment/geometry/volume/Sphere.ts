import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { VSDK } from "../../../common/VSDK.js";
import { Ray } from "../element/Ray.js";
import { RayHit } from "../element/RayHit.js";
import { Solid } from "./Solid.js";
import { PolyhedralBoundedSolid } from "./polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidBuilder } from "./polyhedralBoundedSolid/PolyhedralBoundedSolidBuilder.js";

/** Sphere centered at the origin. */
export class Sphere extends Solid {
    private radius: number;
    private radiusSquared: number;
    public constructor(radius: number) {
        super();
        this.radius = radius;
        this.radiusSquared = radius * radius;
    }
    public getRadius(): number {
        return this.radius;
    }
    public getRadiusSquared(): number {
        return this.radiusSquared;
    }
    public setRadius(radius: number): void {
        this.radius = radius;
        this.radiusSquared = radius * radius;
    }
    public intersectRay(ray: Ray): Ray | null {
        const dx = -ray.getOrigin().x(),
            dy = -ray.getOrigin().y(),
            dz = -ray.getOrigin().z(),
            direction = ray.getDirection(),
            projection = direction.x() * dx + direction.y() * dy + direction.z() * dz,
            discriminant = this.radiusSquared + projection * projection - dx * dx - dy * dy - dz * dz;
        if (discriminant < 0) return null;
        const t = projection - Math.sqrt(discriminant);
        return t < 0 ? null : ray.withT(t);
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
    public override doExtraInformation(ray: Ray, t: number, hit: RayHit): void {
        const point = ray.getOrigin().add(ray.getDirection().multiply(t));
        if (hit.needsPoint()) hit.p = point;
        const needsNormal = hit.needsNormal() || hit.needsTextureCoordinates() || hit.needsTangent();
        if (!needsNormal) return;
        const normal = point.normalized();
        if (hit.needsNormal()) hit.n = normal;
        if (!hit.needsTextureCoordinates() && !hit.needsTangent()) return;
        let theta = 0;
        if (Math.abs(normal.x()) > VSDK.EPSILON) {
            theta = Math.atan(normal.y() / normal.x()) + (3 * Math.PI) / 2;
            if (normal.x() < 0) {
                theta += Math.PI;
                if (theta > 2 * Math.PI) theta -= 2 * Math.PI;
            }
        }
        const phi = Math.acos(normal.z());
        if (hit.needsTextureCoordinates()) {
            hit.u = (theta + Math.PI / 2) / (2 * Math.PI);
            hit.v = 1 - phi / Math.PI;
        }
        if (hit.needsTangent()) hit.t = new Vector3Dd(Math.sin(theta - Math.PI / 2), -Math.cos(theta - Math.PI / 2), 0);
    }
    public override doContainmentTest(point: Vector3Dd, tolerance: number): number {
        const length = point.length();
        return length < this.radius - tolerance
            ? Sphere.INSIDE
            : length > this.radius + tolerance
              ? Sphere.OUTSIDE
              : Sphere.LIMIT;
    }
    public getMinMax(): Float64Array {
        return new Float64Array([-this.radius, -this.radius, -this.radius, this.radius, this.radius, this.radius]);
    }
    public override doCenterOfMass(): Vector3Dd {
        return new Vector3Dd();
    }
    public override exportToPolyhedralBoundedSolid(): PolyhedralBoundedSolid;
    public override exportToPolyhedralBoundedSolid(meridians: number, parallels?: number): PolyhedralBoundedSolid;
    /** Closed latitude/longitude approximation; poles are shared vertices, never degenerate rings. */
    public override exportToPolyhedralBoundedSolid(meridians = 16, parallels = 8): PolyhedralBoundedSolid {
        const longitude = Math.max(3, Math.floor(meridians)),
            latitude = Math.max(3, Math.floor(parallels)),
            points: Vector3Dd[] = [new Vector3Dd(0, 0, -this.radius)],
            faces: number[][] = [];
        for (let row = 1; row < latitude; row++) {
            const phi = -Math.PI / 2 + (Math.PI * row) / latitude;
            for (let column = 0; column < longitude; column++) {
                const theta = (2 * Math.PI * column) / longitude;
                points.push(
                    new Vector3Dd(
                        this.radius * Math.cos(phi) * Math.cos(theta),
                        this.radius * Math.cos(phi) * Math.sin(theta),
                        this.radius * Math.sin(phi),
                    ),
                );
            }
        }
        const north = points.length;
        points.push(new Vector3Dd(0, 0, this.radius));
        for (let column = 0; column < longitude; column++) faces.push([0, 1 + ((column + 1) % longitude), 1 + column]);
        for (let row = 0; row < latitude - 2; row++)
            for (let column = 0; column < longitude; column++) {
                const base = 1 + row * longitude,
                    next = 1 + (row + 1) * longitude;
                faces.push([
                    base + column,
                    base + ((column + 1) % longitude),
                    next + ((column + 1) % longitude),
                    next + column,
                ]);
            }
        const last = 1 + (latitude - 2) * longitude;
        for (let column = 0; column < longitude; column++)
            faces.push([last + column, last + ((column + 1) % longitude), north]);
        return PolyhedralBoundedSolidBuilder.fromPolygons(points, faces);
    }
}
