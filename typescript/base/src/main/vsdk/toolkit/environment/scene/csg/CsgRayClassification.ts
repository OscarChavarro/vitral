import { ArrayList } from "../../../../../java/util/ArrayList.js";
import { RaySolidClassification } from "../../geometry/element/RaySolidClassification.js";
import { CsgRayEventReference } from "./CsgRayEventReference.js";
import { CsgRayLeafBatch } from "./CsgRayLeafBatch.js";
import { CsgVersionStamp } from "./CsgVersionStamp.js";

// Combined membership of a whole expression along one ray. Boundary i of the
// solid is copied from its first owner. The owners of boundary i are the leaf
// events whose transition alone produces that membership change or, when none
// does, every changed leaf oriented like that change; there are none only when
// neither exists. References index the leaf snapshots kept here. The version
// stamp records the borrowed operands as they were when the result was built.
// Storage is reused across queries.
export class CsgRayClassification {
    private readonly leaves: CsgRayLeafBatch;
    private readonly solid: RaySolidClassification;
    private readonly owners: ArrayList<CsgRayEventReference>;
    private readonly ownerStarts: ArrayList<number>;
    private readonly versions: CsgVersionStamp;

    public constructor();
    /** C++ copy constructor. */
    public constructor(other: CsgRayClassification);
    public constructor(other?: CsgRayClassification) {
        this.leaves = new CsgRayLeafBatch();
        this.solid = new RaySolidClassification();
        this.owners = new ArrayList<CsgRayEventReference>(8);
        this.ownerStarts = new ArrayList<number>(8);
        this.versions = new CsgVersionStamp();
        if (other !== undefined) this.assign(other);
    }

    /** C++ copy assignment. */
    public assign(other: CsgRayClassification): CsgRayClassification {
        if (this !== other) {
            this.leaves.assign(other.leaves);
            this.solid.assign(other.solid);
            this.versions.assign(other.versions);
            this.owners.clear();
            this.ownerStarts.clear();
            for (let i = 0; i < other.owners.size(); ++i)
                this.owners.add(new CsgRayEventReference(other.owners.get(i)));
            for (let i = 0; i < other.ownerStarts.size(); ++i) this.ownerStarts.add(other.ownerStarts.get(i));
        }
        return this;
    }

    public clear(): void {
        this.leaves.clear();
        this.solid.clear();
        this.owners.clear();
        this.ownerStarts.clear();
        this.versions.clear();
    }

    public getLeaves(): CsgRayLeafBatch {
        return this.leaves;
    }

    public getSolid(): RaySolidClassification {
        return this.solid;
    }

    public getVersions(): CsgVersionStamp {
        return this.versions;
    }

    // Starts the owner list of the next boundary.
    public beginOwners(): boolean {
        return this.ownerStarts.add(this.owners.size());
    }

    public addOwner(reference: CsgRayEventReference): boolean {
        return this.ownerStarts.size() > 0 && this.owners.add(new CsgRayEventReference(reference));
    }

    public getOwnerGroupCount(): number {
        return this.ownerStarts.size();
    }

    public getOwnerCount(boundaryIndex: number): number {
        if (boundaryIndex < 0 || boundaryIndex >= this.ownerStarts.size()) return 0;
        const end =
            boundaryIndex + 1 < this.ownerStarts.size() ? this.ownerStarts.get(boundaryIndex + 1) : this.owners.size();
        return end - this.ownerStarts.get(boundaryIndex);
    }

    public getOwner(boundaryIndex: number, ownerIndex: number): CsgRayEventReference | null {
        if (ownerIndex < 0 || ownerIndex >= this.getOwnerCount(boundaryIndex)) return null;
        return this.owners.get(this.ownerStarts.get(boundaryIndex) + ownerIndex);
    }
}
