package vsdk.toolkit.environment.scene.csg;

public class CsgOpaqueTestExpression implements CsgExpression {
    private final CsgOperandView operand;
    public CsgOpaqueTestExpression(CsgOperandView value) { operand = value; }
    @Override public CsgOperation getOperation() { return CsgOperation.UNION; }
    @Override public long getOperandCount() { return 1; }
    @Override public CsgOperandView getOperand(long index) { return index==0 ? operand : null; }
}
