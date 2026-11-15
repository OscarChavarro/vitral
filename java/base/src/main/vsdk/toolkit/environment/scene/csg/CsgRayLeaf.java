package vsdk.toolkit.environment.scene.csg;

import java.util.ArrayList;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.geometry.element.RaySolidClassification;

/**
Raw leaf membership in its parent ray's affine parameter; the path carries
the difference parity. Boundary normals are oriented for that parity.
*/
public class CsgRayLeaf {
    private final CsgPointWitness path;
    private final RaySolidClassification classification;
    // CsgRayPlacement values are never mutated after construction, so the
    // list may share them (cpp copies the values).
    private final ArrayList<CsgRayPlacement> placements;

    public CsgRayLeaf() { path = new CsgPointWitness(); classification = new RaySolidClassification(); placements = new ArrayList<CsgRayPlacement>(4); }

    public CsgRayLeaf(CsgRayLeaf other)
    {
        path = new CsgPointWitness(other.path);
        classification = new RaySolidClassification(other.classification);
        placements = new ArrayList<CsgRayPlacement>(other.placements);
    }

    public CsgRayLeaf assign(CsgRayLeaf other)
    {
        if (this!=other) {
            path.assign(other.path); classification.assign(other.classification);
            placements.clear();
            for (int i=0;i<other.placements.size();++i) placements.add(other.placements.get(i));
        }
        return this;
    }

    /** Empties the leaf and keeps its storage for reuse. */
    public void reset() { path.clear(); classification.clear(); placements.clear(); }

    public CsgPointWitness getPath() { return path; }
    public RaySolidClassification getClassification() { return classification; }
    public boolean prependOperand(CsgOperandView operand) { return path.prepend(operand); }

    /** Parent placements are ordered from the leaf outwards. */
    public boolean appendPlacement(CsgOperandView operand, Matrix4x4d forward, Matrix4x4d inverse)
    { return operand != null && placements.add(new CsgRayPlacement(operand,forward,inverse)); }

    public long getPlacementCount() { return placements.size(); }

    public CsgRayPlacement getPlacement(long index)
    { return index>=0 && index<placements.size() ? placements.get((int)index) : null; }

    public void reverseOrientation()
    {
        path.reverseOrientation();
        for (int i=0;i<classification.boundaries.size();++i)
            classification.boundaries.get(i).normalReversed=!classification.boundaries.get(i).normalReversed;
    }
}
