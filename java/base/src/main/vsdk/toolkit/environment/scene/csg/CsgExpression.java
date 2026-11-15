package vsdk.toolkit.environment.scene.csg;



/**
An ordered expression view. Difference subtracts operands 1..N from 0.
Structure and leaf identity are independent of any evaluation strategy.
*/
public interface CsgExpression {
    // Opt in only if operation and operands fully describe this solid.
    default boolean isStructuralTraversalSafe() { return false; }

    CsgOperation getOperation();

    long getOperandCount();

    CsgOperandView getOperand(long index);

    // Counter that changes whenever operation or operand list changes; false
    // when this expression does not track its edits.
    default boolean queryStructureVersion(long[] version) { return false; }
}

