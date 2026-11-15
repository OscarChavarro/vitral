import { ArrayList } from "../../../../../java/util/ArrayList.js";
import { CsgRayEventGroup } from "./CsgRayEventGroup.js";

export class CsgRayEventBatch {
    private readonly groups: ArrayList<CsgRayEventGroup>;

    public constructor();
    /** C++ copy constructor. */
    public constructor(other: CsgRayEventBatch);
    public constructor(other?: CsgRayEventBatch) {
        this.groups = new ArrayList<CsgRayEventGroup>(4);
        if (other !== undefined) this.assign(other);
    }

    /** C++ copy assignment. */
    public assign(other: CsgRayEventBatch): CsgRayEventBatch {
        if (this !== other) {
            this.groups.clear();
            for (let i = 0; i < other.groups.size(); ++i) this.groups.add(new CsgRayEventGroup(other.groups.get(i)));
        }
        return this;
    }

    public clear(): void {
        this.groups.clear();
    }

    public add(group: CsgRayEventGroup): boolean {
        return this.groups.add(new CsgRayEventGroup(group));
    }

    public size(): number {
        return this.groups.size();
    }

    public get(index: number): CsgRayEventGroup | null {
        return index >= 0 && index < this.groups.size() ? this.groups.get(index) : null;
    }
}
