// Port of cpp/base/src/test/CsgOpaqueTestExpression.{h,cpp}
import type { CsgExpression } from "vsdk/toolkit/environment/scene/csg/CsgExpression.js";
import type { CsgOperandView } from "vsdk/toolkit/environment/scene/csg/CsgOperandView.js";
import { CsgOperation } from "vsdk/toolkit/environment/scene/csg/CsgOperation.js";

export class CsgOpaqueTestExpression implements CsgExpression {
    private readonly operand: CsgOperandView;

    public constructor(value: CsgOperandView) {
        this.operand = value;
    }
    public getOperation(): CsgOperation {
        return CsgOperation.UNION;
    }
    public getOperandCount(): number {
        return 1;
    }
    public getOperand(index: number): CsgOperandView | null {
        return index === 0 ? this.operand : null;
    }
    // C++ CsgExpression defaults.
    public isStructuralTraversalSafe(): boolean {
        return false;
    }
    public queryStructureVersion(_version: number[]): boolean {
        return false;
    }
}
