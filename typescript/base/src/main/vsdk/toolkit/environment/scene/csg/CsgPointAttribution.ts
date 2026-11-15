import { ArrayList } from "../../../../../java/util/ArrayList.js";
import type { CsgOperandView } from "./CsgOperandView.js";
import { CsgPointWitness } from "./CsgPointWitness.js";
import { CsgVersionStamp } from "./CsgVersionStamp.js";

// Complete set of certified boundary owners. Paths borrow scene identities;
// the version stamp tells whether they are still current.
export class CsgPointAttribution {
    private readonly owners: ArrayList<CsgPointWitness>;
    private complete: boolean;
    private readonly versions: CsgVersionStamp;

    public constructor();
    /** C++ copy constructor. */
    public constructor(other: CsgPointAttribution);
    public constructor(other?: CsgPointAttribution) {
        this.owners = new ArrayList<CsgPointWitness>(4);
        this.complete = false;
        this.versions = new CsgVersionStamp();
        if (other !== undefined) this.assign(other);
    }

    /** C++ copy assignment. */
    public assign(other: CsgPointAttribution): CsgPointAttribution {
        if (this !== other) {
            this.owners.clear();
            for (let i = 0; i < other.owners.size(); ++i) this.owners.add(new CsgPointWitness(other.owners.get(i)));
            this.complete = other.complete;
            this.versions.assign(other.versions);
        }
        return this;
    }

    public getVersions(): CsgVersionStamp {
        return this.versions;
    }

    public clear(): void {
        this.owners.clear();
        this.complete = false;
        this.versions.clear();
    }

    public begin(): void {
        this.owners.clear();
        this.complete = true;
        this.versions.clear();
    }

    public add(owner: CsgPointWitness): boolean {
        if (!this.complete || !owner.isAvailable() || !this.owners.add(new CsgPointWitness(owner))) {
            this.clear();
            return false;
        }
        return true;
    }

    public append(other: CsgPointAttribution): boolean {
        if (!this.complete || !other.isComplete()) {
            this.clear();
            return false;
        }
        for (let i = 0; i < other.owners.size(); ++i)
            if (!this.owners.add(new CsgPointWitness(other.owners.get(i)))) {
                this.clear();
                return false;
            }
        return true;
    }

    public prepend(operand: CsgOperandView | null): boolean {
        if (!this.complete) return false;
        for (let i = 0; i < this.owners.size(); ++i)
            if (!this.owners.get(i).prepend(operand)) {
                this.clear();
                return false;
            }
        return true;
    }

    public reverseOrientation(): void {
        if (this.complete) for (let i = 0; i < this.owners.size(); ++i) this.owners.get(i).reverseOrientation();
    }

    public isComplete(): boolean {
        return this.complete && this.owners.size() > 0;
    }

    public getCount(): number {
        return this.isComplete() ? this.owners.size() : 0;
    }

    public getOwner(index: number): CsgPointWitness | null {
        return this.isComplete() && index >= 0 && index < this.owners.size() ? this.owners.get(index) : null;
    }
}
