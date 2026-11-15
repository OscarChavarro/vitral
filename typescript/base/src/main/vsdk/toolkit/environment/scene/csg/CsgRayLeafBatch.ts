import { ArrayList } from "../../../../../java/util/ArrayList.js";
import { CsgRayLeaf } from "./CsgRayLeaf.js";

// Borrowed leaf identities; owns snapshots of the classifications and paths.
// Leaves are pooled: clear keeps them, and acquire reuses their storage, so a
// batch reused across queries stops allocating once it has reached its size.
export class CsgRayLeafBatch {
    private readonly pool: ArrayList<CsgRayLeaf>;
    private count: number;

    public constructor();
    /** C++ copy constructor. */
    public constructor(other: CsgRayLeafBatch);
    public constructor(other?: CsgRayLeafBatch) {
        this.pool = new ArrayList<CsgRayLeaf>(4);
        this.count = 0;
        if (other !== undefined) this.assign(other);
    }

    /** C++ copy assignment. */
    public assign(other: CsgRayLeafBatch): CsgRayLeafBatch {
        if (this !== other) {
            this.clear();
            for (let i = 0; i < other.count; ++i) this.add(other.pool.get(i));
        }
        return this;
    }

    public clear(): void {
        this.count = 0;
    }

    // Appends an empty leaf, or returns null when storage is exhausted.
    public acquire(): CsgRayLeaf | null {
        if (this.count === this.pool.size()) {
            const leaf = new CsgRayLeaf();
            if (!this.pool.add(leaf)) return null;
        }
        const leaf = this.pool.get(this.count++);
        leaf.reset();
        return leaf;
    }

    public add(leaf: CsgRayLeaf): boolean {
        const slot = this.acquire();
        if (slot === null) return false;
        slot.assign(leaf);
        return true;
    }

    public size(): number {
        return this.count;
    }

    public get(index: number): CsgRayLeaf | null {
        return index >= 0 && index < this.count ? this.pool.get(index) : null;
    }
}
