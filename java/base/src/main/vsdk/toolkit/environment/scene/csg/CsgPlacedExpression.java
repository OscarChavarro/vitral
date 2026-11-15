package vsdk.toolkit.environment.scene.csg;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.scene.AffinePlacement;
import vsdk.toolkit.environment.scene.AffinePlacementStatus;

/**
Borrowed child expression and an independently editable child-to-parent placement.
*/
public class CsgPlacedExpression implements CsgOperandView {
    private final CsgExpression child;

    private AffinePlacement placement = new AffinePlacement();

    private long modificationVersion;

    public CsgPlacedExpression(CsgExpression expression) {
        this.child = expression;
        this.modificationVersion = 0;}

    public AffinePlacementStatus setAffinePlacement(Matrix4x4d forward, Matrix4x4d inverse) {
    AffinePlacement candidate = new AffinePlacement();
    final AffinePlacementStatus status=candidate.assign(forward,inverse);
    if (status==AffinePlacementStatus.Valid) { placement=candidate; ++modificationVersion; }
    return status;
}

    @Override
    public boolean isSameSolid(CsgOperandView other) { return CsgExpressionEquivalence.same(this,other); }

    @Override
    public Geometry getGeometry() { return null; }

    @Override
    public Matrix4x4d getAffinePlacement() { return placement.getForward(); }

    @Override
    public AffinePlacementStatus queryAffinePlacement(Matrix4x4d[] forward, Matrix4x4d[] inverse) { forward[0]=placement.getForward(); inverse[0]=placement.getInverse(); return placement.getStatus(); }

    @Override
    public CsgExpression getChildExpression() { return child; }

    @Override
    public boolean isStructuralTraversalSafe() { return getClass() == CsgPlacedExpression.class; }

    @Override
    public boolean queryModificationVersion(long[] version) { version[0]=modificationVersion; return true; }
}

