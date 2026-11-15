import { ArrayList } from "../../../../../java/util/ArrayList.js";
import type { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import { RaySolidClassification } from "../../geometry/element/RaySolidClassification.js";
import type { CsgOperandView } from "./CsgOperandView.js";
import { CsgPointWitness } from "./CsgPointWitness.js";
import { CsgRayPlacement } from "./CsgRayPlacement.js";

// Raw leaf membership in its parent ray's affine parameter; the path carries
// the difference parity. Boundary normals are oriented for that parity.
export class CsgRayLeaf {
    private readonly path: CsgPointWitness;
    private readonly classification: RaySolidClassification;
    private readonly placements: ArrayList<CsgRayPlacement>;

    public constructor();
    /** C++ copy constructor. */
    public constructor(other: CsgRayLeaf);
    public constructor(other?: CsgRayLeaf) {
        this.path = new CsgPointWitness();
        this.classification = new RaySolidClassification();
        this.placements = new ArrayList<CsgRayPlacement>(4);
        if (other !== undefined) this.assign(other);
    }

    /** C++ copy assignment. */
    public assign(other: CsgRayLeaf): CsgRayLeaf {
        if (this !== other) {
            this.path.assign(other.path);
            this.classification.assign(other.classification);
            this.placements.clear();
            for (let i = 0; i < other.placements.size(); ++i) {
                const placement = other.placements.get(i);
                this.placements.add(
                    new CsgRayPlacement(placement.operand, placement.childToParent, placement.parentToChild),
                );
            }
        }
        return this;
    }

    // Empties the leaf and keeps its storage for reuse.
    public reset(): void {
        this.path.clear();
        this.classification.clear();
        this.placements.clear();
    }

    public getPath(): CsgPointWitness {
        return this.path;
    }

    public getClassification(): RaySolidClassification {
        return this.classification;
    }

    public prependOperand(operand: CsgOperandView | null): boolean {
        return this.path.prepend(operand);
    }

    // Parent placements are ordered from the leaf outwards.
    public appendPlacement(operand: CsgOperandView | null, forward: Matrix4x4d, inverse: Matrix4x4d): boolean {
        return operand !== null && this.placements.add(new CsgRayPlacement(operand, forward, inverse));
    }

    public getPlacementCount(): number {
        return this.placements.size();
    }

    public getPlacement(index: number): CsgRayPlacement | null {
        return index >= 0 && index < this.placements.size() ? this.placements.get(index) : null;
    }

    public reverseOrientation(): void {
        this.path.reverseOrientation();
        for (let i = 0; i < this.classification.boundaries.size(); ++i)
            this.classification.boundaries.get(i).normalReversed =
                !this.classification.boundaries.get(i).normalReversed;
    }
}
