package vsdk.toolkit.environment.scene.csg;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.GeometryContainment;

/** A leaf fact and the coordinates/tolerance at which it was certified. */
public class CsgPointEvidence {
    private final CsgPointWitness path;
    private final CsgPointWitness equivalentPath;
    private boolean equivalence;
    private GeometryContainment classification;
    private Vector3Dd point;
    private double tolerance;

    public CsgPointEvidence()
    {
        path = new CsgPointWitness(); equivalentPath = new CsgPointWitness();
        equivalence = false; classification = GeometryContainment.Indeterminate; point = new Vector3Dd(); tolerance = 0;
    }

    public CsgPointEvidence(CsgPointWitness p, GeometryContainment c, Vector3Dd q, double t)
    {
        path = new CsgPointWitness(p); equivalentPath = new CsgPointWitness();
        equivalence = false; classification = c; point = q; tolerance = t;
    }

    // Records a proven identity between two sibling solid views; path is the
    // source of the certified classification, equivalentPath its recipient.
    public CsgPointEvidence(CsgPointWitness source, CsgPointWitness equivalent, Vector3Dd q, double t)
    {
        path = new CsgPointWitness(source); equivalentPath = new CsgPointWitness(equivalent);
        equivalence = true; classification = GeometryContainment.Indeterminate; point = q; tolerance = t;
    }

    /** cpp implicit copy constructor. */
    public CsgPointEvidence(CsgPointEvidence other)
    {
        path = new CsgPointWitness(other.path); equivalentPath = new CsgPointWitness(other.equivalentPath);
        equivalence = other.equivalence; classification = other.classification; point = other.point; tolerance = other.tolerance;
    }

    /** cpp implicit copy assignment. */
    public CsgPointEvidence assign(CsgPointEvidence other)
    {
        if (this != other) {
            path.assign(other.path); equivalentPath.assign(other.equivalentPath);
            equivalence = other.equivalence; classification = other.classification; point = other.point; tolerance = other.tolerance;
        }
        return this;
    }

    public boolean isEquivalence() { return equivalence; }
    public CsgPointWitness getEquivalentPath() { return equivalentPath; }

    public boolean prepend(CsgOperandView operand)
    { return path.prepend(operand) && (!equivalence || equivalentPath.prepend(operand)); }

    public void reverseOrientation()
    { path.reverseOrientation(); if (equivalence) equivalentPath.reverseOrientation(); }

    public CsgPointWitness getPath() { return path; }
    public GeometryContainment getClassification() { return classification; }
    public Vector3Dd getPoint() { return point; }
    public double getTolerance() { return tolerance; }
}
