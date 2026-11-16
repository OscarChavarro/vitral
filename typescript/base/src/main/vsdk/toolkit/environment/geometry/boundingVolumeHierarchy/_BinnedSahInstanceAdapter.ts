import type { Ray } from "../element/Ray.js";
import type { BinnedSahInstanceHierarchy } from "./BinnedSahInstanceHierarchy.js";
import type { BinnedSahInstanceVisitor } from "./BinnedSahInstanceVisitor.js";
import type { BinnedSahLeafVisitor } from "./BinnedSahLeafVisitor.js";

/** Leaf test of the inner hierarchy: takes the ray to object space */
export class _BinnedSahInstanceAdapter implements BinnedSahLeafVisitor {
    public readonly owner: BinnedSahInstanceHierarchy;
    public readonly worldRay: Ray;
    public readonly visitor: BinnedSahInstanceVisitor;

    public constructor(inOwner: BinnedSahInstanceHierarchy, inWorldRay: Ray, inVisitor: BinnedSahInstanceVisitor) {
        this.owner = inOwner;
        this.worldRay = inWorldRay;
        this.visitor = inVisitor;
    }

    public visit(instance: number, inOutBestT: number[]): void {
        this.visitor.visit(instance, this.owner.toObjectSpace(instance, this.worldRay), inOutBestT);
    }
}
