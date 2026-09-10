import { VSDK } from "../../../common/VSDK.js";
import { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { IndexedColorImageUncompressed } from "../../../media/IndexedColorImageUncompressed.js";
import { Ray } from "../element/Ray.js";
import { RayHit } from "../element/RayHit.js";
import { Solid } from "./Solid.js";

/** Eight-bit voxel grid mapped to the local cube [-1,1]^3. */
export class VoxelVolume extends Solid {
    private data: IndexedColorImageUncompressed[] | null = null;
    public getXSize(): number {
        return this.data === null || this.data.length === 0 ? 0 : this.data[0]!.getXSize();
    }
    public getYSize(): number {
        return this.data === null || this.data.length === 0 ? 0 : this.data[0]!.getYSize();
    }
    public getZSize(): number {
        return this.data?.length ?? 0;
    }
    public init(x: number, y: number, z: number): boolean {
        if (!Number.isInteger(x) || !Number.isInteger(y) || !Number.isInteger(z) || x <= 0 || y <= 0 || z <= 0)
            return false;
        const slices: IndexedColorImageUncompressed[] = [];
        for (let i = 0; i < z; i++) {
            const slice = new IndexedColorImageUncompressed();
            if (!slice.init(x, y)) return false;
            slices.push(slice);
        }
        this.data = slices;
        return true;
    }
    public putVoxel(x: number, y: number, z: number, value: number): void {
        if (x < 0 || x >= this.getXSize() || y < 0 || y >= this.getYSize() || z < 0 || z >= this.getZSize()) return;
        this.data![z]!.putPixel(x, y, value);
    }
    public getVoxel(x: number, y: number, z: number): number {
        return x < 0 || x >= this.getXSize() || y < 0 || y >= this.getYSize() || z < 0 || z >= this.getZSize()
            ? 0
            : this.data![z]!.getPixel(x, y);
    }
    public getVoxelPosition(x: number, y: number, z: number): Vector3Dd {
        return new Vector3Dd(
            ((x + 0.5) / this.getXSize()) * 2 - 1,
            ((y + 0.5) / this.getYSize()) * 2 - 1,
            ((z + 0.5) / this.getZSize()) * 2 - 1,
        );
    }
    public getNearestIFromX(x: number): number {
        return Math.trunc(((x + 1) / 2) * this.getXSize() - 0.5);
    }
    public getNearestJFromY(y: number): number {
        return Math.trunc(((y + 1) / 2) * this.getYSize() - 0.5);
    }
    public getNearestKFromZ(z: number): number {
        return Math.trunc(((z + 1) / 2) * this.getZSize() - 0.5);
    }
    public getVoxelAtPosition(p: Vector3Dd): number;
    public getVoxelAtPosition(x: number, y: number, z: number): number;
    public getVoxelAtPosition(a: Vector3Dd | number, b?: number, c?: number): number {
        const p = a instanceof Vector3Dd ? a : new Vector3Dd(a, b!, c!);
        return p.x() < -1 || p.x() > 1 || p.y() < -1 || p.y() > 1 || p.z() < -1 || p.z() > 1
            ? 0
            : this.getVoxel(this.getNearestIFromX(p.x()), this.getNearestJFromY(p.y()), this.getNearestKFromZ(p.z()));
    }
    public putVoxelAtPosition(p: Vector3Dd, value: number): void;
    public putVoxelAtPosition(x: number, y: number, z: number, value: number): void;
    public putVoxelAtPosition(a: Vector3Dd | number, b: number, c?: number, d?: number): void {
        const p = a instanceof Vector3Dd ? a : new Vector3Dd(a, b, c!);
        const value = a instanceof Vector3Dd ? b : d!;
        if (p.x() >= -1 && p.x() <= 1 && p.y() >= -1 && p.y() <= 1 && p.z() >= -1 && p.z() <= 1)
            this.putVoxel(
                this.getNearestIFromX(p.x()),
                this.getNearestJFromY(p.y()),
                this.getNearestKFromZ(p.z()),
                value,
            );
    }
    public getMinMax(): Float64Array {
        return new Float64Array([-1, -1, -1, 1, 1, 1]);
    }
    public doIntersectionFirstHit(ray: Ray, hit: RayHit): boolean {
        const d = ray.getDirection(),
            o = ray.getOrigin();
        let enter = -Infinity,
            exit = Infinity;
        for (const [origin, direction] of [
            [o.x(), d.x()],
            [o.y(), d.y()],
            [o.z(), d.z()],
        ] as const) {
            if (Math.abs(direction) < VSDK.EPSILON) {
                if (origin < -1 || origin > 1) return false;
                continue;
            }
            const a = (-1 - origin) / direction,
                b = (1 - origin) / direction;
            enter = Math.max(enter, Math.min(a, b));
            exit = Math.min(exit, Math.max(a, b));
        }
        if (exit < Math.max(enter, 0)) return false;
        const start = Math.max(enter, 0),
            step = 1 / (Math.max(this.getXSize(), this.getYSize(), this.getZSize()) * 2);
        for (let t = start; t <= exit + VSDK.EPSILON; t += step) {
            const p = o.add(d.multiply(t));
            if (this.getVoxelAtPosition(p) !== 0) {
                hit.setRay(ray.withT(t));
                if (hit.needsPoint()) hit.p = p;
                return true;
            }
        }
        return false;
    }
    public static getTransformFromVoxelFrameToMinMax(bounds: Float64Array | number[]): Matrix4x4d {
        const sx = bounds[3]! - bounds[0]!,
            sy = bounds[4]! - bounds[1]!,
            sz = bounds[5]! - bounds[2]!,
            scale = Math.max(sx, sy, sz);
        return new Matrix4x4d()
            .translation(bounds[0]! - (scale - sx) / 2, bounds[1]! - (scale - sy) / 2, bounds[2]! - (scale - sz) / 2)
            .multiply(
                new Matrix4x4d().scale(scale / 2, scale / 2, scale / 2).multiply(new Matrix4x4d().translation(1, 1, 1)),
            );
    }
    public override doCenterOfMass(): Vector3Dd {
        let mass = 0,
            x = 0,
            y = 0,
            z = 0;
        for (let i = 0; i < this.getXSize(); i++)
            for (let j = 0; j < this.getYSize(); j++)
                for (let k = 0; k < this.getZSize(); k++) {
                    const m = this.getVoxel(i, j, k) / 255,
                        p = this.getVoxelPosition(i, j, k);
                    mass += m;
                    x += m * p.x();
                    y += m * p.y();
                    z += m * p.z();
                }
        return Math.abs(mass) < VSDK.EPSILON ? new Vector3Dd() : new Vector3Dd(x / mass, y / mass, z / mass);
    }
}
