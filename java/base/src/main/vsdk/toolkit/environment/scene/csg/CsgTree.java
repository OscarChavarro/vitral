package vsdk.toolkit.environment.scene.csg;

import java.util.ArrayList;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.scene.AffinePlacementStatus;

/**
An application-built ordered node. Operands are borrowed and must remain
alive. addOperand rejects cycles visible through the current expression graph.
A nested tree is identity-placed; evaluators and ownership are separate.
*/
public class CsgTree implements CsgExpression, CsgOperandView {
    private CsgOperation operation;

    private final ArrayList<CsgOperandView> operands = new ArrayList<CsgOperandView>();

    private long structureVersion;

    private boolean canAttach(CsgOperandView operand) {
    final CsgExpression child = operand.getChildExpression();
    if (child == null) return true;
    ArrayList<CsgExpression> pending = new ArrayList<CsgExpression>();
    ArrayList<CsgExpression> visited = new ArrayList<CsgExpression>();
    if (!pending.add(child)) return false;
    for (long i = 0; i < pending.size(); ++i) {
        final CsgExpression current = pending.get((int)(i));
        if (current == this) return false;
        boolean known = false;
        for (long j = 0; j < visited.size(); ++j) {
            if (visited.get((int)(j)) == current) { known = true; break; }
        }
        if (known) continue;
        if (!visited.add(current)) return false;
        for (long j = 0; j < current.getOperandCount(); ++j) {
            final CsgOperandView nested = current.getOperand(j);
            if (nested != null && nested.getChildExpression() != null && !pending.add(nested.getChildExpression()))
                return false;
        }
    }
    return true;
}

    public CsgTree(CsgOperation value) {
        this.operation = value;
        this.structureVersion = 0;}

    @Override
    public CsgOperation getOperation() { return operation; }

    @Override
    public long getOperandCount() { return operands.size(); }

    @Override
    public CsgOperandView getOperand(long index) { return index < 0 || index >= operands.size() ? null : operands.get((int)(index)); }

    public boolean addOperand(CsgOperandView operand) {
    if (!canAttach(operand) || !operands.add(operand)) return false;
    ++structureVersion;
    return true;
}

    // Replaces the operation (e.g. a De Morgan complement); a structure edit.
    public void setOperation(CsgOperation value) {
    operation = value;
    ++structureVersion;
}

    @Override
    public boolean isSameSolid(CsgOperandView other) { return CsgExpressionEquivalence.same(this,other); }

    @Override
    public Geometry getGeometry() { return null; }

    @Override
    public Matrix4x4d getAffinePlacement() { return Matrix4x4d.identityMatrix(); }

    @Override
    public AffinePlacementStatus queryAffinePlacement(Matrix4x4d[] forward, Matrix4x4d[] inverse) { forward[0] = Matrix4x4d.identityMatrix(); inverse[0] = forward[0]; return AffinePlacementStatus.Valid; }

    @Override
    public CsgExpression getChildExpression() { return this; }

    @Override
    public boolean isStructuralTraversalSafe() { return getClass() == CsgTree.class; }

    @Override
    public boolean queryStructureVersion(long[] version) { version[0]=structureVersion; return true; }

    @Override
    public boolean queryModificationVersion(long[] version) { version[0]=structureVersion; return true; }
}

