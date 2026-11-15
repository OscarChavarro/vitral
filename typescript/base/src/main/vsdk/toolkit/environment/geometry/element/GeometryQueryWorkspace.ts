import { ArrayList } from "../../../../../java/util/ArrayList.js";
import { GeometryBounds } from "./GeometryBounds.js";
import { RayHit } from "./RayHit.js";
import { RaySolidClassification } from "./RaySolidClassification.js";

// Caller-owned, reusable per worker/query; never share during concurrent queries.
export class GeometryQueryWorkspace {
    private readonly solid = new RaySolidClassification();
    private readonly bounds = new GeometryBounds();
    private readonly hit = new RayHit();
    private readonly scratch = new ArrayList<RaySolidClassification>();
    private scratchDepth = 0;
    public solidResult(): RaySolidClassification {
        return this.solid;
    }
    public boundsResult(): GeometryBounds {
        return this.bounds;
    }
    public hitResult(): RayHit {
        return this.hit;
    }
    // Acquired references remain stable across pool growth; release in reverse order.
    public acquireSolidScratch(): RaySolidClassification | null {
        if (this.scratchDepth === this.scratch.size()) {
            const value = new RaySolidClassification();
            if (!this.scratch.add(value)) return null;
        }
        const result = this.scratch.get(this.scratchDepth++);
        result.clear();
        return result;
    }
    public releaseSolidScratch(value: RaySolidClassification | null): boolean {
        if (this.scratchDepth === 0 || value !== this.scratch.get(this.scratchDepth - 1)) return false;
        value.clear();
        this.scratchDepth--;
        return true;
    }
    public clear(): void {
        this.solid.clear();
        this.bounds.setUnsupported();
        this.hit.clear();
        for (let i = 0; i < this.scratchDepth; i++) this.scratch.get(i).clear();
        this.scratchDepth = 0;
    }
}
