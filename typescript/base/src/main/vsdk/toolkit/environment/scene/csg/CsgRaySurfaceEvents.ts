import { Double } from "../../../../../java/lang/Double.js";
import { ArrayList } from "../../../../../java/util/ArrayList.js";
import { CsgRayEventReference } from "./CsgRayEventReference.js";
import { CsgRayLeafBatch } from "./CsgRayLeafBatch.js";
import { CsgVersionStamp } from "./CsgVersionStamp.js";

// Selected leaf boundary events ordered by ray parameter, then leaf order.
// This is a list of visible surface events, not a solid classification.
export class CsgRaySurfaceEvents {
    private readonly leaves: CsgRayLeafBatch;
    private readonly events: ArrayList<CsgRayEventReference>;
    private readonly versions: CsgVersionStamp;

    public constructor();
    /** C++ copy constructor. */
    public constructor(other: CsgRaySurfaceEvents);
    public constructor(other?: CsgRaySurfaceEvents) {
        this.leaves = new CsgRayLeafBatch();
        this.events = new ArrayList<CsgRayEventReference>(8);
        this.versions = new CsgVersionStamp();
        if (other !== undefined) this.assign(other);
    }

    /** C++ copy assignment. */
    public assign(other: CsgRaySurfaceEvents): CsgRaySurfaceEvents {
        if (this !== other) {
            this.leaves.assign(other.leaves);
            this.versions.assign(other.versions);
            this.events.clear();
            for (let i = 0; i < other.events.size(); ++i)
                this.events.add(new CsgRayEventReference(other.events.get(i)));
        }
        return this;
    }

    public clear(): void {
        this.leaves.clear();
        this.events.clear();
        this.versions.clear();
    }

    public getVersions(): CsgVersionStamp {
        return this.versions;
    }

    public getLeaves(): CsgRayLeafBatch {
        return this.leaves;
    }

    public size(): number {
        return this.events.size();
    }

    public get(index: number): CsgRayEventReference | null {
        return index >= 0 && index < this.events.size() ? this.events.get(index) : null;
    }

    // Parameter of event index, or NaN when out of range.
    public getT(index: number): number {
        const reference = this.get(index);
        if (reference === null) return Double.NaN;
        const leaf = this.leaves.get(reference.leafIndex);
        if (
            leaf === null ||
            reference.boundaryIndex < 0 ||
            reference.boundaryIndex >= leaf.getClassification().boundaries.size()
        )
            return Double.NaN;
        return leaf.getClassification().boundaries.get(reference.boundaryIndex).t;
    }

    // Inserts keeping the (t, leaf, boundary) order.
    public add(value: CsgRayEventReference): boolean {
        const leaf = this.leaves.get(value.leafIndex);
        if (
            leaf === null ||
            value.boundaryIndex < 0 ||
            value.boundaryIndex >= leaf.getClassification().boundaries.size()
        )
            return false;
        const reference = new CsgRayEventReference(value);
        const t = leaf.getClassification().boundaries.get(reference.boundaryIndex).t;
        let position = this.events.size();
        while (position > 0) {
            const before = this.events.get(position - 1);
            const beforeT = this.getT(position - 1);
            if (
                beforeT < t ||
                (beforeT === t &&
                    (before.leafIndex < reference.leafIndex ||
                        (before.leafIndex === reference.leafIndex && before.boundaryIndex < reference.boundaryIndex)))
            )
                break;
            --position;
        }
        if (!this.events.add(reference)) return false;
        for (let i = this.events.size() - 1; i > position; --i) this.events.set(i, this.events.get(i - 1));
        this.events.set(position, reference);
        return true;
    }
}
