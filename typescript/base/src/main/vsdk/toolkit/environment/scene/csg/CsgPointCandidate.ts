import { GeometryContainment } from "../../geometry/element/GeometryContainment.js";
import { CsgPointAttribution } from "./CsgPointAttribution.js";
import { CsgPointProof } from "./CsgPointProof.js";
import { CsgPointWitness } from "./CsgPointWitness.js";

// Working value for one operand at a single point and tolerance.
export class CsgPointCandidate {
    public result: GeometryContainment;
    public readonly witness: CsgPointWitness;
    public readonly proof: CsgPointProof;
    public readonly attribution: CsgPointAttribution;

    public constructor();
    /** C++ copy constructor. */
    public constructor(other: CsgPointCandidate);
    public constructor(other?: CsgPointCandidate) {
        this.result = GeometryContainment.InvalidInput;
        this.witness = new CsgPointWitness();
        this.proof = new CsgPointProof();
        this.attribution = new CsgPointAttribution();
        if (other !== undefined) this.assign(other);
    }

    /** C++ copy assignment. */
    public assign(other: CsgPointCandidate): CsgPointCandidate {
        if (this !== other) {
            this.result = other.result;
            this.witness.assign(other.witness);
            this.proof.assign(other.proof);
            this.attribution.assign(other.attribution);
        }
        return this;
    }
}
