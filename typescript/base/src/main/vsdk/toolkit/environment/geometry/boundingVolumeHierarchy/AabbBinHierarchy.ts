import { ArrayList } from "../../../../../java/util/ArrayList.js";
import type { Ray } from "../element/Ray.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { AxisAlignedBoundingBox } from "./AxisAlignedBoundingBox.js";
import type { RaySlabReciprocals } from "./RaySlabReciprocals.js";

// C fmin/fmax: a NaN operand is ignored (JavaScript Math.min/max propagate it).
const fmin = (a: number, b: number): number => (Number.isNaN(a) ? b : Number.isNaN(b) ? a : a < b ? a : b);
const fmax = (a: number, b: number): number => (Number.isNaN(a) ? b : Number.isNaN(b) ? a : a > b ? a : b);

/**
Two-level hierarchy of axis aligned boxes over a set of primitives, built once
before tracing. The primitives are sorted by the centroid coordinate along the
axis of widest centroid spread and cut into about the square root of their
count bins (at most 64) of consecutive primitives; each bin keeps the box that
encloses its members.

The structure is a set of flat, index-linked arrays without virtual dispatch,
in the form of a bottom-level acceleration structure over procedural boxes
(Vulkan `VK_GEOMETRY_TYPE_AABBS_KHR`): a member is identified by the caller's
primitive id, and its box is copied into the structure in bin order.

A query lists the ids of the members whose own box passes the forward slab
test, in bin order and member order inside a bin; it never omits a primitive
whose box the ray crosses.
*/
export class AabbBinHierarchy {
    private static readonly MAXIMUM_BIN_COUNT = 64;

    private binBounds = new ArrayList<AxisAlignedBoundingBox>();
    private binMemberStart = new ArrayList<number>();
    private binMemberCount = new ArrayList<number>();
    private memberIds = new ArrayList<number>();
    private memberBounds = new ArrayList<AxisAlignedBoundingBox>();

    public constructor() {}

    /** Builds the hierarchy over `boxes`, whose primitive ids are `ids`
    (same size). */
    public build(boxes: ArrayList<AxisAlignedBoundingBox>, ids: ArrayList<number>): void {
        this.binBounds.clear();
        this.binMemberStart.clear();
        this.binMemberCount.clear();
        this.memberIds.clear();
        this.memberBounds.clear();
        const total = boxes.size();
        if (total === 0) {
            return;
        }

        let lo = new Vector3Dd(1e30, 1e30, 1e30);
        let hi = new Vector3Dd(-1e30, -1e30, -1e30);
        for (let i = 0; i < total; i++) {
            const c = boxes.get(i).centroid();
            lo = new Vector3Dd(fmin(lo.x(), c.x()), fmin(lo.y(), c.y()), fmin(lo.z(), c.z()));
            hi = new Vector3Dd(fmax(hi.x(), c.x()), fmax(hi.y(), c.y()), fmax(hi.z(), c.z()));
        }
        const spreadX = hi.x() - lo.x();
        const spreadY = hi.y() - lo.y();
        const spreadZ = hi.z() - lo.z();
        let axis = 0;
        if (spreadY > spreadX && spreadY >= spreadZ) {
            axis = 1;
        } else if (spreadZ > spreadX && spreadZ > spreadY) {
            axis = 2;
        }

        // Stable insertion sort of the primitives by centroid coordinate.
        const keys = new ArrayList<number>(total);
        const order = new ArrayList<number>(total);
        for (let i = 0; i < total; i++) {
            const c = boxes.get(i).centroid();
            const key = axis === 0 ? c.x() : axis === 1 ? c.y() : c.z();
            let j = i - 1;
            keys.add(key);
            order.add(i);
            while (j >= 0 && keys.get(j) > key) {
                keys.set(j + 1, keys.get(j));
                order.set(j + 1, order.get(j));
                j--;
            }
            keys.set(j + 1, key);
            order.set(j + 1, i);
        }

        let binCount = Math.trunc(Math.sqrt(total));
        if (binCount < 1) {
            binCount = 1;
        }
        if (binCount > AabbBinHierarchy.MAXIMUM_BIN_COUNT) {
            binCount = AabbBinHierarchy.MAXIMUM_BIN_COUNT;
        }
        const base = Math.trunc(total / binCount);
        const remainder = total % binCount;
        let cursor = 0;
        for (let b = 0; b < binCount; b++) {
            const count = base + (b < remainder ? 1 : 0);
            let aggregate = AxisAlignedBoundingBox.empty();
            const start = this.memberIds.size();
            for (let k = 0; k < count; k++) {
                const primitive = order.get(cursor++);
                this.memberIds.add(ids.get(primitive));
                this.memberBounds.add(boxes.get(primitive));
                aggregate = aggregate.enclosing(boxes.get(primitive));
            }
            this.binBounds.add(aggregate);
            this.binMemberStart.add(start);
            this.binMemberCount.add(count);
        }
    }

    public getBinCount(): number {
        return this.binBounds.size();
    }

    public getMemberCount(): number {
        return this.memberIds.size();
    }

    public getBinBounds(bin: number): AxisAlignedBoundingBox {
        return this.binBounds.get(bin);
    }

    /** Writes into `outIds` the ids of the members whose box passes the
    forward slab test and returns their count, or -1 when more than
    `capacity` members pass. */
    public gatherCandidates(
        ray: Ray,
        reciprocals: RaySlabReciprocals,
        outIds: Int32Array | number[],
        capacity: number,
    ): number {
        let count = 0;
        for (let b = 0; b < this.binBounds.size(); b++) {
            if (!this.binBounds.get(b).intersectsRayForward(ray, reciprocals)) {
                continue;
            }
            const start = this.binMemberStart.get(b);
            const end = start + this.binMemberCount.get(b);
            for (let m = start; m < end; m++) {
                if (!this.memberBounds.get(m).intersectsRayForward(ray, reciprocals)) {
                    continue;
                }
                if (count >= capacity) {
                    return -1;
                }
                outIds[count++] = this.memberIds.get(m);
            }
        }
        return count;
    }
}
