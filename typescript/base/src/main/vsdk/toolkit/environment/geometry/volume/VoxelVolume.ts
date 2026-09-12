//= References:                                                             =
//= [KAUF1987] Kaufman, Arie. "Efficient Algorithms for 3D Scan-Conversion  =
//=     of Parametric Curves, Surfaces, and Volumes", ACM SIGGRAPH Computer =
//=     Graphics, volume 21, number 4, July 1987.                           =

import { VSDK } from "../../../common/VSDK.js";
import type { Ray } from "../element/Ray.js";
import { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import type { RayHit } from "../element/RayHit.js";
import { IndexedColorImageUncompressed } from "../../../media/IndexedColorImageUncompressed.js";
import { Solid } from "./Solid.js";

/**
VoxelVolume represents a voxelized paralelogram volume in memory, as the
"cubic frame buffer" proposed in [KAUF1987]. Note that this class is intended
for simpler applications in which data volume fix into main memory. Current
class doesn't support any caching or data storage optimization. When the
x/y/z sizes of the voxel volume are equal, the corresponding space covered
by the volume is the cube from the point <-1, -1, -1> to the point <1, 1, 1>.
The voxels are always assume to be square, and when dimensions are different,
the largest dimension fits in to the <-1, 1> interval (other dimensions are
proportional to maintain voxel sizes).

As current voxel volume is based in specific data samples of 8 bits per voxel,
it is not general and not well biased to processing applications. This is
just a placeholder for regions of interest in memory, as an aid to
other algorithms.

For general representation of N-dimensional images of arbitrary data sample
format, use another toolkit, like ITK or VTK.
*/
export class VoxelVolume extends Solid {
    private data: IndexedColorImageUncompressed[] | null;

    public constructor() {
        super();
        this.data = null;
    }

    public getXSize(): number {
        if (this.data === null || this.data.length < 0) return 0;
        return VoxelVolume.listGet(this.data, 0).getXSize();
    }

    public getYSize(): number {
        if (this.data === null || this.data.length < 0) return 0;
        return VoxelVolume.listGet(this.data, 0).getXSize();
    }

    public getZSize(): number {
        if (this.data === null || this.data.length < 0) return 0;
        return this.data.length;
    }

    public init(xSize: number, ySize: number, zSize: number): boolean {
        let z: number;
        let slice: IndexedColorImageUncompressed;

        const localData: IndexedColorImageUncompressed[] = [];

        for (z = 0; z < zSize; z++) {
            slice = new IndexedColorImageUncompressed();
            if (!slice.init(xSize, ySize)) {
                return false;
            }
            localData.push(slice);
        }

        this.data = localData;
        return true;
    }

    public putVoxel(x: number, y: number, z: number, val: number): void {
        try {
            if (x < 0 || x >= this.getXSize() || y < 0 || y >= this.getYSize() || z < 0 || z >= this.getZSize()) {
                return;
            }
            const slice = VoxelVolume.listGet(this.data!, z);
            // Java calls the `putPixel(int, int, byte)` overload.
            slice.putPixelByte(x, y, val);
        } catch {
            //
        }
    }

    /**
    Given current voxel set geometric space (cube from <-1, -1, -1> to
    <1, 1, 1>), current voxel set size, and cell position to a voxel; this
    methods gives the position of the voxel center in world coordinates.
    @param x
    @param y
    @param z
    @return a new Vector3Dd with point position corresponding to given indexes
    inside the matrix of voxels
    */
    public getVoxelPosition(x: number, y: number, z: number): Vector3Dd {
        let p = new Vector3Dd();
        p = p.withX(((x + 0.5) / this.getXSize()) * 2 - 1);
        p = p.withY(((y + 0.5) / this.getYSize()) * 2 - 1);
        p = p.withZ(((z + 0.5) / this.getZSize()) * 2 - 1);
        return p;
    }

    /**
    Partial coordinate convertion (X axis) for `x` voxel coordinate to
    corresponding voxel index.
    @param x
    @return a matrix index for the given coordinate
    */
    public getNearestIFromX(x: number): number {
        return VoxelVolume.toInt(((x + 1) / 2) * this.getXSize() - 0.5);
    }

    /**
    Partial coordinate convertion (Y axis) for `y` voxel coordinate to
    corresponding voxel index.
    @param y
    @return a matrix index for the given coordinate
    */
    public getNearestJFromY(y: number): number {
        return VoxelVolume.toInt(((y + 1) / 2) * this.getYSize() - 0.5);
    }

    /**
    Partial coordinate convertion (Z axis) for `z` voxel coordinate to
    corresponding voxel index.
    @param z
    @return a matrix index for the given coordinate
    */
    public getNearestKFromZ(z: number): number {
        return VoxelVolume.toInt(((z + 1) / 2) * this.getZSize() - 0.5);
    }

    /**
    Given current voxelset geometric space (cube from <-1, -1, -1> to
    <1, 1, 1>), current voxelset size, and cell position to a voxel; this
    methods gives the voxel value with a position corresponding to coordinate
    <x, y, z> (inside voxel space cube).

    Java overloads `getVoxelAtPosition(double, double, double)` and
    `getVoxelAtPosition(Vector3Dd)`.
    @return value of voxel at specified indexed position
    */
    public getVoxelAtPosition(p: Vector3Dd): number;
    public getVoxelAtPosition(x: number, y: number, z: number): number;
    public getVoxelAtPosition(a: Vector3Dd | number, b?: number, c?: number): number {
        let i: number;
        let j: number;
        let k: number;
        if (a instanceof Vector3Dd) {
            const p = a;
            if (p.x() < -1 || p.x() > 1 || p.y() < -1 || p.y() > 1 || p.z() < -1 || p.z() > 1) return 0;

            i = VoxelVolume.toInt(((p.x() + 1) / 2) * this.getXSize() - 0.5);
            j = VoxelVolume.toInt(((p.y() + 1) / 2) * this.getYSize() - 0.5);
            k = VoxelVolume.toInt(((p.z() + 1) / 2) * this.getZSize() - 0.5);

            return this.getVoxel(i, j, k);
        }
        const x = a;
        const y = b!;
        const z = c!;
        if (x < -1 || x > 1 || y < -1 || y > 1 || z < -1 || z > 1) return 0;

        i = VoxelVolume.toInt(((x + 1) / 2) * this.getXSize() - 0.5);
        j = VoxelVolume.toInt(((y + 1) / 2) * this.getYSize() - 0.5);
        k = VoxelVolume.toInt(((z + 1) / 2) * this.getZSize() - 0.5);

        return this.getVoxel(i, j, k);
    }

    /**
    Given current voxelset geometric space (cube from <-1, -1, -1> to
    <1, 1, 1>), current voxelset size, and cell position to a voxel; this
    methods puts the voxel value with a position corresponding to coordinate
    <x, y, z> (inside voxel space cube).

    Java overloads `putVoxelAtPosition(double, double, double, byte)` and
    `putVoxelAtPosition(Vector3Dd, byte)`.
    */
    public putVoxelAtPosition(p: Vector3Dd, val: number): void;
    public putVoxelAtPosition(x: number, y: number, z: number, val: number): void;
    public putVoxelAtPosition(a: Vector3Dd | number, b: number, c?: number, d?: number): void {
        let i: number;
        let j: number;
        let k: number;
        if (a instanceof Vector3Dd) {
            const p = a;
            const val = b;
            if (p.x() < -1 || p.x() > 1 || p.y() < -1 || p.y() > 1 || p.z() < -1 || p.z() > 1) return;

            i = VoxelVolume.toInt(((p.x() + 1) / 2) * this.getXSize() - 0.5);
            j = VoxelVolume.toInt(((p.y() + 1) / 2) * this.getYSize() - 0.5);
            k = VoxelVolume.toInt(((p.z() + 1) / 2) * this.getZSize() - 0.5);

            this.putVoxel(i, j, k, val);
            return;
        }
        const x = a;
        const y = b;
        const z = c!;
        const val = d!;
        if (x < -1 || x > 1 || y < -1 || y > 1 || z < -1 || z > 1) return;

        i = VoxelVolume.toInt(((x + 1) / 2) * this.getXSize() - 0.5);
        j = VoxelVolume.toInt(((y + 1) / 2) * this.getYSize() - 0.5);
        k = VoxelVolume.toInt(((z + 1) / 2) * this.getZSize() - 0.5);

        this.putVoxel(i, j, k, val);
    }

    public getVoxel(x: number, y: number, z: number): number {
        try {
            const slice = VoxelVolume.listGet(this.data!, z);
            // Java's backing byte[] throws ArrayIndexOutOfBoundsException for
            // an index outside the slice, which this method catches; the
            // TypeScript typed array returns undefined instead, so the same
            // bound is checked explicitly.
            const index = slice.getXSize() * y + x;
            if (index < 0 || index >= slice.getXSize() * slice.getYSize()) {
                throw new RangeError("Array index out of range: " + index);
            }
            return slice.getPixel(x, y);
        } catch {
            //
        }
        return 0;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.getMinMax.
    @return a new 6 valued double array containing the coordinates of a min-max
    bounding box for current geometry.
    */
    public override getMinMax(): Float64Array {
        const minMax = new Float64Array(6);
        minMax[0] = -1.0;
        minMax[1] = -1.0;
        minMax[2] = -1.0;
        minMax[3] = 1.0;
        minMax[4] = 1.0;
        minMax[5] = 1.0;
        return minMax;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doIntersectionFirstHit.

    NOT IMPLEMENTED YET!

    Java overload `doIntersectionFirstHit(Ray inOut_Ray)` returns null.
    @return true if given ray intersects current VoxelVolume
    */
    public doIntersectionFirstHit(inOut_Ray: Ray): Ray | null;
    public override doIntersectionFirstHit(inRay: Ray, outHit: RayHit): boolean;
    public override doIntersectionFirstHit(inRay: Ray, outHit: RayHit | null): boolean;
    public override doIntersectionFirstHit(_inRay: Ray, outHit?: RayHit | null): Ray | null | boolean {
        if (outHit === undefined) {
            return null;
        }
        return false;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doExtraInformation.
    @param inRay
    @param inT
    @param outData
    */
    public override doExtraInformation(_inRay: Ray, _inT: number, _outData: RayHit): void {}

    /**
    Current method creates a transformation matrix that represent the
    coordinate change from voxel volume cube <-1, -1, -1>-<1, 1, 1> to
    the bounding box recieved in `minmax`.
    @param minmax
    @return a new Matrix4x4d for coordinate mapping between volume and
    world coordinates
    */
    public static getTransformFromVoxelFrameToMinMax(minmax: Float64Array | readonly number[]): Matrix4x4d {
        let S: Matrix4x4d;
        let T1: Matrix4x4d;
        let T2: Matrix4x4d;
        let greaterScale: number;

        const sx = minmax[3]! - minmax[0]!;
        const sy = minmax[4]! - minmax[1]!;
        const sz = minmax[5]! - minmax[2]!;
        greaterScale = sx;
        if (sy > greaterScale) {
            greaterScale = sy;
        }
        if (sz > greaterScale) {
            greaterScale = sz;
        }

        S = new Matrix4x4d();
        S = S.scale(greaterScale / 2, greaterScale / 2, greaterScale / 2);

        T1 = new Matrix4x4d();
        T1 = T1.translation(1, 1, 1);
        T2 = new Matrix4x4d();
        T2 = T2.translation(
            minmax[0]! - (greaterScale - sx) / 2,
            minmax[1]! - (greaterScale - sy) / 2,
            minmax[2]! - (greaterScale - sz) / 2,
        );

        const M = T2.multiply(S.multiply(T1));
        return M;
    }

    /**
    Check the general interface contract in superclass method
    Solid.doCenterOfMass
    @return new Vector3Dd containing volume center of mass
    */
    public override doCenterOfMass(): Vector3Dd {
        let p: Vector3Dd;
        let cmx = 0;
        let cmy = 0;
        let cmz = 0;
        let mi: number; // Maximum mass of one voxel (linear to voxel density)
        let M = 0; // Total mass for current voxel volume
        let x: number;
        let y: number;
        let z: number;

        for (x = 0; x < this.getXSize(); x++) {
            for (y = 0; y < this.getYSize(); y++) {
                for (z = 0; z < this.getZSize(); z++) {
                    // mi goes from 0 to 1
                    mi = this.getVoxel(x, y, z) / 255.0;
                    M += mi;
                    p = this.getVoxelPosition(x, y, z);
                    cmx += mi * p.x();
                    cmy += mi * p.y();
                    cmz += mi * p.z();
                }
            }
        }

        if (Math.abs(M) < VSDK.EPSILON) {
            return new Vector3Dd(0, 0, 0);
        }

        return new Vector3Dd(cmx / M, cmy / M, cmz / M);
    }

    /** Java `ArrayList.get`, which throws for an index outside the list. */
    private static listGet(
        list: readonly IndexedColorImageUncompressed[],
        index: number,
    ): IndexedColorImageUncompressed {
        if (index < 0 || index >= list.length) {
            throw new RangeError("Index " + index + " out of bounds for length " + list.length);
        }
        return list[index]!;
    }

    /** Java narrowing conversion `(int)double`. */
    private static toInt(value: number): number {
        if (Number.isNaN(value)) return 0;
        if (value >= 2147483647) return 2147483647;
        if (value <= -2147483648) return -2147483648;
        return Math.trunc(value);
    }
}
