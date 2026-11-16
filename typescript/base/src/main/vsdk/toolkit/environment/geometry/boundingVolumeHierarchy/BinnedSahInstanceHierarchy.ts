//= References:                                                             =
//= [WALD2007] Wald, Ingo. "On fast Construction of SAH-based Bounding      =
//=     Volume Hierarchies". Proceedings of the 2007 IEEE Symposium on      =
//=     Interactive Ray Tracing, 2007, pp. 33-40.                           =

import { ArrayList } from "../../../../../java/util/ArrayList.js";
import type { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { Ray } from "../element/Ray.js";
import { BinnedSahHierarchy } from "./BinnedSahHierarchy.js";
import type { BinnedSahInstanceVisitor } from "./BinnedSahInstanceVisitor.js";
import { TriangleAccelerationStructure } from "./TriangleAccelerationStructure.js";
import { _BinnedSahInstanceAdapter } from "./_BinnedSahInstanceAdapter.js";

/**
Top level of a two level acceleration structure: a `BinnedSahHierarchy`
over instances, each one an object space box (the bounds of its bottom
level, for example a `BinnedSahTriangleHierarchy`) placed in the world by
an affine object to world matrix. It is the CPU counterpart of a Vulkan top
level acceleration structure over `VkAccelerationStructureInstanceKHR`, and
the Vulkan 1.4 compute path builds its top level with it.

Build: the world box of an instance encloses the eight transformed corners
of its object box, padded with `TriangleAccelerationStructure.padBox`; an
empty object box gives an empty world box (the instance is never reached).
The world to object matrices are computed once (`Matrix4x4d.inverse`).
Leaves hold `DEFAULT_MAXIMUM_LEAF_SIZE` (1) instance unless told otherwise,
as a hardware TLAS. The tree follows `doc/flatBinaryTreeContract.md`; its
entries are instance indices.

Rebuild it when the transforms change (the cost is that of a build over the
instance count; the bottom levels are kept).

Matrices follow the vitral convention: column vectors, translation in
column 3 (`get(row, 3)`); only the upper 3x4 part is used.
*/
export class BinnedSahInstanceHierarchy {
    public static readonly DEFAULT_MAXIMUM_LEAF_SIZE = 1;

    private readonly hierarchy: BinnedSahHierarchy = new BinnedSahHierarchy();
    private readonly objectToWorldMatrices: ArrayList<Matrix4x4d> = new ArrayList<Matrix4x4d>();
    private readonly worldToObjectMatrices: ArrayList<Matrix4x4d> = new ArrayList<Matrix4x4d>();
    /** Six values per instance: padded world box */
    private readonly worldBoxes: ArrayList<number> = new ArrayList<number>();

    public constructor() {}

    /** `m * (p, 1)`, upper 3x4 part */
    public static transformPoint(m: Matrix4x4d, p: Vector3Dd): Vector3Dd {
        return new Vector3Dd(
            m.get(0, 0) * p.x() + m.get(0, 1) * p.y() + m.get(0, 2) * p.z() + m.get(0, 3),
            m.get(1, 0) * p.x() + m.get(1, 1) * p.y() + m.get(1, 2) * p.z() + m.get(1, 3),
            m.get(2, 0) * p.x() + m.get(2, 1) * p.y() + m.get(2, 2) * p.z() + m.get(2, 3),
        );
    }

    /** `m * (v, 0)`, upper 3x3 part */
    public static transformVector(m: Matrix4x4d, v: Vector3Dd): Vector3Dd {
        return new Vector3Dd(
            m.get(0, 0) * v.x() + m.get(0, 1) * v.y() + m.get(0, 2) * v.z(),
            m.get(1, 0) * v.x() + m.get(1, 1) * v.y() + m.get(1, 2) * v.z(),
            m.get(2, 0) * v.x() + m.get(2, 1) * v.y() + m.get(2, 2) * v.z(),
        );
    }

    /** Box enclosing the eight transformed corners (unpadded); an empty box
    stays empty */
    public static transformBox(objectBox: ArrayLike<number>, m: Matrix4x4d, outWorldBox: number[]): void {
        for (let a = 0; a < 3; a++) {
            outWorldBox[a] = 1e30;
            outWorldBox[a + 3] = -1e30;
        }
        if (objectBox[0]! > objectBox[3]! || objectBox[1]! > objectBox[4]! || objectBox[2]! > objectBox[5]!) {
            return;
        }
        for (let corner = 0; corner < 8; corner++) {
            const p = new Vector3Dd(
                corner & 1 ? objectBox[3]! : objectBox[0]!,
                corner & 2 ? objectBox[4]! : objectBox[1]!,
                corner & 4 ? objectBox[5]! : objectBox[2]!,
            );
            const w = BinnedSahInstanceHierarchy.transformPoint(m, p);
            const c: number[] = [w.x(), w.y(), w.z()];
            for (let a = 0; a < 3; a++) {
                outWorldBox[a] = c[a]! < outWorldBox[a]! ? c[a]! : outWorldBox[a]!;
                outWorldBox[a + 3] = c[a]! > outWorldBox[a + 3]! ? c[a]! : outWorldBox[a + 3]!;
            }
        }
    }

    /**
    @param objectBoxes six values per instance (lo x, y, z, hi x, y, z) in
    object space; lo > hi on an axis marks an empty instance
    @param objectToWorld one matrix per instance
    @param maximumLeafSize see `BinnedSahHierarchy.build`
    */
    public build(
        objectBoxes: ArrayList<number>,
        objectToWorld: ArrayList<Matrix4x4d>,
        maximumLeafSize: number = BinnedSahInstanceHierarchy.DEFAULT_MAXIMUM_LEAF_SIZE,
    ): void {
        let count = Math.trunc(objectBoxes.size() / 6);
        if (objectToWorld.size() < count) {
            count = objectToWorld.size();
        }
        this.objectToWorldMatrices.clear();
        this.worldToObjectMatrices.clear();
        this.worldBoxes.clear();
        const objectBox: number[] = [0, 0, 0, 0, 0, 0];
        for (let i = 0; i < count; i++) {
            const m = objectToWorld.get(i);
            this.objectToWorldMatrices.add(m);
            this.worldToObjectMatrices.add(m.inverse());
            for (let k = 0; k < 6; k++) {
                objectBox[k] = objectBoxes.get(6 * i + k);
            }
            const world: number[] = [0, 0, 0, 0, 0, 0];
            BinnedSahInstanceHierarchy.transformBox(objectBox, m, world);
            if (world[0]! <= world[3]!) {
                // [WALD2007].3.2: the build only sees these boxes; padded so
                // rounding of the transform never hides a hit.
                const lo: number[] = [world[0]!, world[1]!, world[2]!];
                const hi: number[] = [world[3]!, world[4]!, world[5]!];
                TriangleAccelerationStructure.padBox(lo, hi);
                for (let a = 0; a < 3; a++) {
                    world[a] = lo[a]!;
                    world[a + 3] = hi[a]!;
                }
            }
            for (let k = 0; k < 6; k++) {
                this.worldBoxes.add(world[k]!);
            }
        }
        this.hierarchy.build(this.worldBoxes, maximumLeafSize);
    }

    /** The world ray in the object space of an instance (see the visitor) */
    public toObjectSpace(instance: number, worldRay: Ray): Ray {
        const m = this.worldToObjectMatrices.get(instance);
        return Ray.fromAffine(
            BinnedSahInstanceHierarchy.transformPoint(m, worldRay.getOrigin()),
            BinnedSahInstanceHierarchy.transformVector(m, worldRay.getDirection()),
        );
    }

    /**
    Closest hit traversal of the world ray: calls the visitor for every
    instance whose world box the ray enters before `inOutBestT[0]`, nearest
    first, with the ray in object space. Never misses an instance: when the
    traversal stack would overflow (not reachable, see
    `BinnedSahHierarchy.traverseClosest`) every instance is visited.
    @param inOutBestT one-element input/output holder
    */
    public traverseClosest(worldRay: Ray, visitor: BinnedSahInstanceVisitor, inOutBestT: number[]): void {
        const adapter = new _BinnedSahInstanceAdapter(this, worldRay, visitor);
        if (!this.hierarchy.traverseClosest(worldRay, adapter, inOutBestT)) {
            // Stack overflow: visiting every instance still gives the closest
            // hit (the visitor only lowers inOutBestT).
            for (let i = 0; i < this.getInstanceCount(); i++) {
                adapter.visit(i, inOutBestT);
            }
        }
    }

    public getInstanceCount(): number {
        return this.objectToWorldMatrices.size();
    }

    public getHierarchy(): BinnedSahHierarchy {
        return this.hierarchy;
    }

    public getObjectToWorld(instance: number): Matrix4x4d {
        return this.objectToWorldMatrices.get(instance);
    }

    public getWorldToObject(instance: number): Matrix4x4d {
        return this.worldToObjectMatrices.get(instance);
    }

    /** Padded world box of an instance: six values */
    public getWorldBox(instance: number, outBox: number[]): void {
        for (let k = 0; k < 6; k++) {
            outBox[k] = this.worldBoxes.get(6 * instance + k);
        }
    }
}
