import { ArrayList } from "../../../../../java/util/ArrayList.js";
import { CsgRayEventReference } from "./CsgRayEventReference.js";

// Events with exactly equal binary64 ray parameters, with stable leaf order.
export class CsgRayEventGroup {
    private t: number;
    private readonly references: ArrayList<CsgRayEventReference>;

    public constructor();
    public constructor(t: number);
    /** C++ copy constructor. */
    public constructor(other: CsgRayEventGroup);
    public constructor(value?: number | CsgRayEventGroup) {
        this.t = 0;
        this.references = new ArrayList<CsgRayEventReference>(4);
        if (value instanceof CsgRayEventGroup) this.assign(value);
        else if (value !== undefined) this.t = value;
    }

    /** C++ copy assignment. */
    public assign(other: CsgRayEventGroup): CsgRayEventGroup {
        if (this !== other) {
            this.t = other.t;
            this.references.clear();
            for (let i = 0; i < other.references.size(); ++i)
                this.references.add(new CsgRayEventReference(other.references.get(i)));
        }
        return this;
    }

    public getT(): number {
        return this.t;
    }

    public add(reference: CsgRayEventReference): boolean {
        return this.references.add(new CsgRayEventReference(reference));
    }

    public size(): number {
        return this.references.size();
    }

    public get(index: number): CsgRayEventReference | null {
        return index >= 0 && index < this.references.size() ? this.references.get(index) : null;
    }
}
