import { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import type { CsgOperandView } from "./CsgOperandView.js";

// Borrowed identity and owned child-to-parent and inverse snapshots.
export class CsgRayPlacement {
    public operand: CsgOperandView | null;
    public childToParent: Matrix4x4d;
    public parentToChild: Matrix4x4d;

    public constructor();
    public constructor(operand: CsgOperandView | null, forward: Matrix4x4d, inverse: Matrix4x4d);
    public constructor(value?: CsgOperandView | null, forward?: Matrix4x4d, inverse?: Matrix4x4d) {
        this.operand = value ?? null;
        this.childToParent = forward ?? new Matrix4x4d();
        this.parentToChild = inverse ?? new Matrix4x4d();
    }
}
