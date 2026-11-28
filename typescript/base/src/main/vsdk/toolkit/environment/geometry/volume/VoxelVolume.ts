//= References:                                                             =
//= [KAUF1987] Kaufman, Arie. "Efficient Algorithms for 3D Scan-Conversion  =
//=     of Parametric Curves, Surfaces, and Volumes", ACM SIGGRAPH Computer =
//=     Graphics, volume 21, number 4, July 1987.                           =
//= [AMAN1987] Amanatides, John. Woo, Andrew. "A Fast Voxel Traversal      =
//=     Algorithm for Ray Tracing", Eurographics '87, 1987.                 =
//= [SNYD1987] Snyder, John. Barr, Alan. "Ray Tracing Complex Models       =
//=     Containing Surface Tessellations", SIGGRAPH '87, p. 119-128, 1987.  =

import { VSDK } from "../../../common/VSDK.js";
import type { Ray } from "../element/Ray.js";
import { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { RayHit as RayHitClass, type RayHit } from "../element/RayHit.js";
import { IndexedColorImageUncompressed } from "../../../media/IndexedColorImageUncompressed.js";
import type { PolyhedralBoundedSolid } from "./polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidFieldConstructions } from "./polyhedralBoundedSolid/PolyhedralBoundedSolidFieldConstructions.js";
import { Solid } from "./Solid.js";
import { Double } from "../../../../../java/lang/Double.js";
import { Math as JavaMath } from "../../../../../java/lang/Math.js";
import { ArrayIndexOutOfBoundsException } from "../../../../../java/lang/ArrayIndexOutOfBoundsException.js";
import { IndexOutOfBoundsException } from "../../../../../java/lang/IndexOutOfBoundsException.js";

/**
Face of a voxel entered by a ray (Java's private record
`VoxelVolume.VoxelFaceHit`).
@param t distance along the ray to the face
@param axis axis perpendicular to the face (0: x, 1: y, 2: z)
@param side sign of the outer normal of the face along its axis
*/
class _VoxelFaceHit {
    public constructor(
        private readonly tValue: number,
        private readonly axisValue: number,
        private readonly sideValue: number,
    ) {}
    public t(): number {
        return this.tValue;
    }
    public axis(): number {
        return this.axisValue;
    }
    public side(): number {
        return this.sideValue;
    }
}

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
    /** Default lowest value of a voxel considered part of the solid */
    public static readonly DEFAULT_THRESHOLD = 127;

    /** Smallest ray direction component not considered parallel to an axis */
    private static readonly PARALLEL_EPSILON = 1.0e-12;

    private data: IndexedColorImageUncompressed[] | null;
    private threshold: number;

    public constructor() {
        super();
        this.data = null;
        this.threshold = VoxelVolume.DEFAULT_THRESHOLD;
    }

    public getXSize(): number {
        if (this.data === null || this.data.length === 0) return 0;
        return VoxelVolume.listGet(this.data, 0).getXSize();
    }

    public getYSize(): number {
        if (this.data === null || this.data.length === 0) return 0;
        return VoxelVolume.listGet(this.data, 0).getYSize();
    }

    public getZSize(): number {
        if (this.data === null || this.data.length === 0) return 0;
        return this.data.length;
    }

    /**
    @return lowest value of a voxel considered part of the solid (by the ray
    intersection and by the renderers)
    */
    public getThreshold(): number {
        return this.threshold;
    }

    /**
    @param threshold lowest value of a voxel considered part of the solid
    */
    public setThreshold(threshold: number): void {
        this.threshold = threshold;
    }

    /**
    @param x voxel index along X
    @param y voxel index along Y
    @param z voxel index along Z
    @return true if the voxel is inside the volume and its value reaches the
    threshold
    */
    public isFilled(x: number, y: number, z: number): boolean {
        if (x < 0 || y < 0 || z < 0 || x >= this.getXSize() || y >= this.getYSize() || z >= this.getZSize()) {
            return false;
        }
        return this.getVoxel(x, y, z) >= this.threshold;
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
                throw new ArrayIndexOutOfBoundsException(
                    "Index " + index + " out of bounds for length " + slice.getXSize() * slice.getYSize(),
                );
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
    Geometry.doIntersectionFirstHit. The volume is seen as the union of the
    cubes of its filled voxels (see `isFilled`), and the ray hits the first
    face through which it enters one of them. A ray starting inside a filled
    voxel does not hit the voxels it leaves: as with the other solids, only
    the surface it enters counts.

    The voxels are visited in the order the ray crosses them, as the uniform
    grid traversal of [SNYD1987] (with the incremental formulation of
    [AMAN1987]), so the cost grows with the number of voxels crossed, not
    with the number of voxels of the volume.

    The one argument form (Java's `doIntersectionFirstHit(Ray inOut_Ray)`)
    returns the ray with the distance to the first filled voxel hit, or null
    if no filled voxel is hit.
    @param inRay ray in the space of the volume
    @param outHit receives the hit, or null if only the test is needed
    @return true if the ray hits a filled voxel
    */
    public doIntersectionFirstHit(inOut_Ray: Ray): Ray | null;
    public override doIntersectionFirstHit(inRay: Ray, outHit: RayHit): boolean;
    public override doIntersectionFirstHit(inRay: Ray, outHit: RayHit | null): boolean;
    public override doIntersectionFirstHit(inRay: Ray, outHit?: RayHit | null): Ray | null | boolean {
        if (outHit === undefined) {
            const hit = new RayHitClass();
            if (this.doIntersectionFirstHit(inRay, hit)) {
                return hit.getRay();
            }
            return null;
        }
        const hit = this.traceFirstFilledVoxel(inRay);

        if (hit === null) {
            return false;
        }
        if (outHit !== null) {
            if (outHit.shouldStoreRay() || outHit.needsAnySurfaceData()) {
                const hitRay = inRay.withT(hit.t());
                outHit.setRay(hitRay);
                if (outHit.needsAnySurfaceData()) {
                    VoxelVolume.fillSurfaceData(hitRay, hit, outHit);
                    outHit.setRay(hitRay);
                }
            } else {
                outHit.setHitDistance(hit.t());
            }
        }
        return true;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doExtraInformation.
    @param inRay ray (in the space of the volume) whose first hit is described
    @param inT distance to the hit
    @param outData receives the point, normal, texture coordinates and
    tangent of the hit
    */
    public override doExtraInformation(inRay: Ray, _inT: number, outData: RayHit): void {
        const hit = this.traceFirstFilledVoxel(inRay);

        if (hit !== null) {
            VoxelVolume.fillSurfaceData(inRay.withT(hit.t()), hit, outData);
        }
    }

    /**
    Finds the face of the first filled voxel entered by the ray: clips the
    ray against the cube <-1, -1, -1>-<1, 1, 1> and visits the voxels it
    crosses, as `VoxelGrid.gridIntersect` of [SNYD1987]. For each axis,
    `tNext` is the distance to the next voxel boundary along that axis and
    `tDelta` the distance between two boundaries; each step moves to the
    voxel whose boundary is nearest.
    @param ray ray in the space of the volume
    @return the face entered, or null if the ray enters no filled voxel
    */
    private traceFirstFilledVoxel(ray: Ray | null): _VoxelFaceHit | null {
        const size = [this.getXSize(), this.getYSize(), this.getZSize()];

        if (size[0]! <= 0 || size[1]! <= 0 || size[2]! <= 0 || ray === null) {
            return null;
        }

        const o = ray.getOrigin();
        const d = ray.getDirection();
        const origin = [o.x(), o.y(), o.z()];
        const direction = [d.x(), d.y(), d.z()];
        const cellSize = [0, 0, 0];
        let tEnter = Double.NEGATIVE_INFINITY;
        let tExit = Double.POSITIVE_INFINITY;
        let enterAxis = 0;
        let i: number;
        const EPS = VoxelVolume.PARALLEL_EPSILON;

        //- Clip the ray against the volume cube (slabs method) ------------
        for (i = 0; i < 3; i++) {
            cellSize[i] = 2.0 / size[i]!;
            if (JavaMath.abs(direction[i]!) < EPS) {
                if (origin[i]! < -1.0 || origin[i]! > 1.0) {
                    return null;
                }
                continue;
            }
            const t1 = (-1.0 - origin[i]!) / direction[i]!;
            const t2 = (1.0 - origin[i]!) / direction[i]!;
            const tNear = JavaMath.min(t1, t2);
            const tFar = JavaMath.max(t1, t2);
            if (tNear > tEnter) {
                tEnter = tNear;
                enterAxis = i;
            }
            tExit = JavaMath.min(tExit, tFar);
        }
        if (tExit < JavaMath.max(tEnter, 0.0)) {
            return null;
        }
        const startsInside = tEnter < 0.0;
        const t = startsInside ? 0.0 : tEnter;

        //- Setup of the traversal, from the voxel where the ray starts ----
        const g = [0, 0, 0];
        const step = [0, 0, 0];
        const tDelta = [0, 0, 0];
        const tNext = [0, 0, 0];

        for (i = 0; i < 3; i++) {
            const p = origin[i]! + t * direction[i]!;
            g[i] = VoxelVolume.toInt(JavaMath.floor((p + 1.0) / cellSize[i]!));
            if (g[i]! < 0) {
                g[i] = 0;
            }
            if (g[i]! >= size[i]!) {
                g[i] = size[i]! - 1;
            }
            if (direction[i]! > EPS) {
                step[i] = 1;
                tDelta[i] = cellSize[i]! / direction[i]!;
                tNext[i] = (-1.0 + (g[i]! + 1) * cellSize[i]! - origin[i]!) / direction[i]!;
            } else if (direction[i]! < -EPS) {
                step[i] = -1;
                tDelta[i] = cellSize[i]! / -direction[i]!;
                tNext[i] = (-1.0 + g[i]! * cellSize[i]! - origin[i]!) / direction[i]!;
            } else {
                step[i] = 0;
                tDelta[i] = Double.POSITIVE_INFINITY;
                tNext[i] = Double.POSITIVE_INFINITY;
            }
        }

        let previousFilled = this.isFilled(g[0]!, g[1]!, g[2]!);
        if (previousFilled && !startsInside) {
            return new _VoxelFaceHit(t, enterAxis, direction[enterAxis]! > 0 ? -1 : 1);
        }

        //- Visit the voxels crossed by the ray -----------------------------
        while (true) {
            let axis: number;
            if (tNext[0]! <= tNext[1]! && tNext[0]! <= tNext[2]!) {
                axis = 0;
            } else if (tNext[1]! <= tNext[2]!) {
                axis = 1;
            } else {
                axis = 2;
            }
            if (tNext[axis]! > tExit) {
                return null;
            }
            const tCell = tNext[axis]!;
            g[axis] = (g[axis]! + step[axis]!) | 0;
            tNext[axis] = tNext[axis]! + tDelta[axis]!;
            if (g[axis]! < 0 || g[axis]! >= size[axis]!) {
                return null;
            }
            const filled = this.isFilled(g[0]!, g[1]!, g[2]!);
            if (filled && !previousFilled) {
                return new _VoxelFaceHit(tCell, axis, -step[axis]!);
            }
            previousFilled = filled;
        }
    }

    /**
    Fills the surface data of a hit on a voxel face: the point, the normal of
    the face, texture coordinates spanning the volume along the two axes of
    the face, and a tangent along the first of them.
    */
    private static fillSurfaceData(hitRay: Ray, hit: _VoxelFaceHit, outData: RayHit): void {
        const p = hitRay.getOrigin().add(hitRay.getDirection().multiply(hit.t()));
        const coordinates = [p.x(), p.y(), p.z()];
        const normal = [0, 0, 0];
        const tangent = [0, 0, 0];
        const uAxis = (hit.axis() + 1) % 3;
        const vAxis = (hit.axis() + 2) % 3;

        normal[hit.axis()] = hit.side();
        tangent[uAxis] = 1.0;
        if (outData.needsPoint()) {
            outData.point = p;
        }
        if (outData.needsNormal()) {
            outData.normal = new Vector3Dd(normal[0]!, normal[1]!, normal[2]!);
        }
        if (outData.needsTextureCoordinates()) {
            outData.u = (coordinates[uAxis]! + 1.0) / 2.0;
            outData.v = (coordinates[vAxis]! + 1.0) / 2.0;
        }
        if (outData.needsTangent()) {
            outData.tangent = new Vector3Dd(tangent[0]!, tangent[1]!, tangent[2]!);
        }
    }

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

    //= Boundary representation =============================================

    /// The default export samples at most this number of voxels along each
    /// axis
    public static readonly DEFAULT_EXPORT_MAXIMUM_SAMPLES = 32;

    /**
    Without argument: polyhedral approximation sampling at most
    `DEFAULT_EXPORT_MAXIMUM_SAMPLES` voxels along each axis.

    With `step`: polyhedral approximation of the voxels that reach the
    threshold, built with Euler operators by
    `PolyhedralBoundedSolidFieldConstructions`: the voxel values are taken as
    samples at the voxel centers (the field is `threshold - 0.5 - value`,
    negative for the filled voxels), surrounded by a layer of empty samples,
    so the surface is closed and passes half way between filled and empty
    voxel centers.
    @param step sampling step (at least 1): 1 uses every voxel, 2 every other
    one along each axis, and so on, for coarser approximations
    @return a new polyhedral approximation, or null if there are no filled
    voxels
    */
    public override exportToPolyhedralBoundedSolid(): PolyhedralBoundedSolid | null;
    public override exportToPolyhedralBoundedSolid(step: number): PolyhedralBoundedSolid | null;
    public override exportToPolyhedralBoundedSolid(stepValue?: number): PolyhedralBoundedSolid | null {
        if (stepValue === undefined) {
            const largest = JavaMath.max(this.getXSize(), JavaMath.max(this.getYSize(), this.getZSize()));
            return this.exportToPolyhedralBoundedSolid(
                JavaMath.max(
                    1,
                    Math.trunc(
                        (largest + VoxelVolume.DEFAULT_EXPORT_MAXIMUM_SAMPLES - 1) /
                            VoxelVolume.DEFAULT_EXPORT_MAXIMUM_SAMPLES,
                    ),
                ),
            );
        }
        let step = stepValue;
        const xs = this.getXSize();
        const ys = this.getYSize();
        const zs = this.getZSize();
        if (xs <= 0 || ys <= 0 || zs <= 0) {
            return null;
        }
        if (step < 1) {
            step = 1;
        }
        const sx = Math.trunc((xs + step - 1) / step);
        const sy = Math.trunc((ys + step - 1) / step);
        const sz = Math.trunc((zs + step - 1) / step);
        const nx = sx + 2;
        const ny = sy + 2;
        const nz = sz + 2;
        const values = new Float64Array(nx * ny * nz);
        const level = this.threshold - 0.5;

        for (let k = 0; k < nz; k++) {
            for (let j = 0; j < ny; j++) {
                for (let i = 0; i < nx; i++) {
                    let value = 1.0;
                    if (i > 0 && j > 0 && k > 0 && i < nx - 1 && j < ny - 1 && k < nz - 1) {
                        value = level - this.getVoxel((i - 1) * step, (j - 1) * step, (k - 1) * step);
                    }
                    values[i + nx * (j + ny * k)] = value;
                }
            }
        }

        // Samples at the voxel centers (see getVoxelPosition), one step
        // before the first one for the empty layer
        const spacing = new Vector3Dd((2.0 * step) / xs, (2.0 * step) / ys, (2.0 * step) / zs);
        const origin = new Vector3Dd(
            (0.5 / xs) * 2 - 1 - spacing.x(),
            (0.5 / ys) * 2 - 1 - spacing.y(),
            (0.5 / zs) * 2 - 1 - spacing.z(),
        );
        return PolyhedralBoundedSolidFieldConstructions.createFromSampledField(nx, ny, nz, origin, spacing, values);
    }

    /** Java `ArrayList.get`, which throws for an index outside the list. */
    private static listGet(
        list: readonly IndexedColorImageUncompressed[],
        index: number,
    ): IndexedColorImageUncompressed {
        if (index < 0 || index >= list.length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + list.length);
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
