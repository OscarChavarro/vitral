import type { CsgOperandView } from "./CsgOperandView.js";
import type { CsgOperation } from "./CsgOperation.js";

/**
An ordered expression view. Difference subtracts operands 1..N from 0.
Structure and leaf identity are independent of any evaluation strategy.

The C++ abstract base is an interface here because `CsgTree` derives from
both `CsgExpression` and `CsgOperandView`; implementers write the C++
defaults explicitly (`isStructuralTraversalSafe` false, `queryStructureVersion`
false) and `CsgExpression.isInstance` stands for `dynamic_cast`.
*/
export interface CsgExpression {
    // Opt in only if operation and operands fully describe this solid.
    isStructuralTraversalSafe(): boolean;
    getOperation(): CsgOperation;
    getOperandCount(): number;
    getOperand(index: number): CsgOperandView | null;
    // Counter that changes whenever operation or operand list changes; false
    // when this expression does not track its edits.
    queryStructureVersion(version: number[]): boolean;
}

export namespace CsgExpression {
    export function isInstance(value: unknown): value is CsgExpression {
        if (value === null || typeof value !== "object") return false;
        const candidate = value as Partial<CsgExpression>;
        return (
            typeof candidate.getOperation === "function" &&
            typeof candidate.getOperandCount === "function" &&
            typeof candidate.getOperand === "function" &&
            typeof candidate.queryStructureVersion === "function"
        );
    }
}
