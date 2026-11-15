package vsdk.toolkit.environment.scene.csg;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.scene.AffinePlacementStatus;

public class CsgPolicyTestOperand implements CsgOperandView {
    private final CsgExpression expression;
    private final boolean inverted;
    public CsgPolicyTestOperand(CsgExpression value, boolean invert) { expression = value; inverted = invert; }
    public boolean isInverted() { return inverted; }
    @Override public Geometry getGeometry() { return null; }
    @Override public Matrix4x4d getAffinePlacement() { return Matrix4x4d.identityMatrix(); }
    @Override public AffinePlacementStatus queryAffinePlacement(Matrix4x4d[] forward, Matrix4x4d[] inverse)
    { forward[0]=Matrix4x4d.identityMatrix(); inverse[0]=forward[0]; return AffinePlacementStatus.Valid; }
    @Override public CsgExpression getChildExpression() { return expression; }
}
