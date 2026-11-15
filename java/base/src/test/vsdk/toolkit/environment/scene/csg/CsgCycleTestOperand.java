package vsdk.toolkit.environment.scene.csg;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.scene.AffinePlacementStatus;

public class CsgCycleTestOperand implements CsgExpression, CsgOperandView {
    @Override public CsgOperation getOperation() { return CsgOperation.UNION; }
    @Override public long getOperandCount() { return 1; }
    @Override public CsgOperandView getOperand(long index) { return index==0 ? this : null; }
    @Override public Geometry getGeometry() { return null; }
    @Override public Matrix4x4d getAffinePlacement() { return Matrix4x4d.identityMatrix(); }
    @Override public AffinePlacementStatus queryAffinePlacement(Matrix4x4d[] forward, Matrix4x4d[] inverse)
    { forward[0]=Matrix4x4d.identityMatrix(); inverse[0]=forward[0]; return AffinePlacementStatus.Valid; }
    @Override public CsgExpression getChildExpression() { return this; }
    @Override public boolean isStructuralTraversalSafe() { return true; }
}
