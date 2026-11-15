import { ArrayList } from "../../../../../java/util/ArrayList.js";
import type { CsgOperandView } from "./CsgOperandView.js";
import { CsgPointEvidence } from "./CsgPointEvidence.js";
import { CsgVersionStamp } from "./CsgVersionStamp.js";

// Sufficient leaf facts for a definite result, interpreted with the borrowed
// expression tree. Empty/unavailable means the result has no collected proof.
export class CsgPointProof {
    private readonly evidence: ArrayList<CsgPointEvidence>;
    private available: boolean;
    private readonly versions: CsgVersionStamp;

    public constructor();
    /** C++ implicit copy constructor. */
    public constructor(other: CsgPointProof);
    public constructor(other?: CsgPointProof) {
        this.evidence = new ArrayList<CsgPointEvidence>(4);
        this.available = false;
        this.versions = new CsgVersionStamp();
        if (other !== undefined) this.assign(other);
    }

    /** C++ implicit copy assignment. */
    public assign(other: CsgPointProof): CsgPointProof {
        if (this !== other) {
            this.evidence.clear();
            for (let i = 0; i < other.evidence.size(); ++i)
                this.evidence.add(new CsgPointEvidence(other.evidence.get(i)));
            this.available = other.available;
            this.versions.assign(other.versions);
        }
        return this;
    }

    public clear(): void {
        this.evidence.clear();
        this.available = false;
        this.versions.clear();
    }

    public begin(): void {
        this.evidence.clear();
        this.available = true;
        this.versions.clear();
    }

    // Versions of the borrowed expression, captured by the top-level query.
    public getVersions(): CsgVersionStamp {
        return this.versions;
    }

    public add(value: CsgPointEvidence): boolean {
        if (!this.available || !this.evidence.add(new CsgPointEvidence(value))) {
            this.clear();
            return false;
        }
        return true;
    }

    public append(other: CsgPointProof): boolean {
        if (!this.available || !other.available) {
            this.clear();
            return false;
        }
        for (let i = 0; i < other.evidence.size(); ++i)
            if (!this.evidence.add(new CsgPointEvidence(other.evidence.get(i)))) {
                this.clear();
                return false;
            }
        return true;
    }

    public prepend(operand: CsgOperandView | null): boolean {
        if (!this.available) return false;
        for (let i = 0; i < this.evidence.size(); ++i)
            if (!this.evidence.get(i).prepend(operand)) {
                this.clear();
                return false;
            }
        return true;
    }

    public reverseOrientation(): void {
        if (this.available) for (let i = 0; i < this.evidence.size(); ++i) this.evidence.get(i).reverseOrientation();
    }

    public isAvailable(): boolean {
        return this.available && this.evidence.size() > 0;
    }

    public getCount(): number {
        return this.isAvailable() ? this.evidence.size() : 0;
    }

    public getEvidence(index: number): CsgPointEvidence | null {
        return this.isAvailable() && index >= 0 && index < this.evidence.size() ? this.evidence.get(index) : null;
    }
}
