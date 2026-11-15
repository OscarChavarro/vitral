package vsdk.toolkit.environment.scene.csg;

import vsdk.toolkit.environment.geometry.element.GeometryContainment;

/** Working value for one operand at a single point and tolerance. */
public class CsgPointCandidate {
    public GeometryContainment result;
    public final CsgPointWitness witness;
    public final CsgPointProof proof;
    public final CsgPointAttribution attribution;

    public CsgPointCandidate()
    {
        result = GeometryContainment.InvalidInput;
        witness = new CsgPointWitness(); proof = new CsgPointProof(); attribution = new CsgPointAttribution();
    }

    public CsgPointCandidate(CsgPointCandidate other)
    {
        result = other.result; witness = new CsgPointWitness(other.witness);
        proof = new CsgPointProof(other.proof); attribution = new CsgPointAttribution(other.attribution);
    }

    /** cpp copy assignment. */
    public CsgPointCandidate assign(CsgPointCandidate other)
    {
        if (this!=other) { result=other.result; witness.assign(other.witness); proof.assign(other.proof); attribution.assign(other.attribution); }
        return this;
    }
}
