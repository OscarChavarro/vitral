import { ArrayList } from "../../../../../java/util/ArrayList.js";
import type { CsgOperandView } from "./CsgOperandView.js";

// Borrowed root-to-leaf path for a single operand proving a point result.
// Reversal records difference orientation, not a certified boundary hit.
export class CsgPointWitness {
    private readonly path: ArrayList<CsgOperandView>;
    private reversed: boolean;

    public constructor();
    /** C++ copy constructor. */
    public constructor(other: CsgPointWitness);
    public constructor(other?: CsgPointWitness) {
        this.path = new ArrayList<CsgOperandView>(4);
        this.reversed = false;
        if (other !== undefined) this.assign(other);
    }

    /** C++ copy assignment. */
    public assign(other: CsgPointWitness): CsgPointWitness {
        // Reuses the existing storage, see RayInstancePath.
        if (this !== other) {
            this.path.clear();
            for (let i = 0; i < other.path.size(); ++i) this.path.add(other.path.get(i));
            this.reversed = other.reversed;
        }
        return this;
    }

    public clear(): void {
        this.path.clear();
        this.reversed = false;
    }

    public prepend(operand: CsgOperandView | null): boolean {
        if (operand === null || !this.path.add(operand)) return false;
        for (let i = this.path.size() - 1; i > 0; --i) this.path.set(i, this.path.get(i - 1));
        this.path.set(0, operand);
        return true;
    }

    public reverseOrientation(): void {
        this.reversed = !this.reversed;
    }

    public isAvailable(): boolean {
        return this.path.size() > 0;
    }

    public isReversed(): boolean {
        return this.reversed;
    }

    public getLength(): number {
        return this.path.size();
    }

    public getOperand(index: number): CsgOperandView | null {
        return index >= 0 && index < this.path.size() ? this.path.get(index) : null;
    }
}
