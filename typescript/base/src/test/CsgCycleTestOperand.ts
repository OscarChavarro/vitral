// Port of cpp/base/src/test/CsgCycleTestOperand.{h,cpp}
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import type { Geometry } from "vsdk/toolkit/environment/geometry/Geometry.js";
import { AffinePlacementStatus } from "vsdk/toolkit/environment/scene/AffinePlacementStatus.js";
import type { CsgExpression } from "vsdk/toolkit/environment/scene/csg/CsgExpression.js";
import { CsgOperandView } from "vsdk/toolkit/environment/scene/csg/CsgOperandView.js";
import { CsgOperation } from "vsdk/toolkit/environment/scene/csg/CsgOperation.js";

export class CsgCycleTestOperand extends CsgOperandView implements CsgExpression {
    public getOperation(): CsgOperation {
        return CsgOperation.UNION;
    }
    public getOperandCount(): number {
        return 1;
    }
    public getOperand(index: number): CsgOperandView | null {
        return index === 0 ? this : null;
    }
    public getGeometry(): Geometry | null {
        return null;
    }
    public getAffinePlacement(): Matrix4x4d {
        return Matrix4x4d.identityMatrix();
    }
    public override queryAffinePlacement(forward: Matrix4x4d[], inverse: Matrix4x4d[]): AffinePlacementStatus {
        forward[0] = Matrix4x4d.identityMatrix();
        inverse[0] = forward[0];
        return AffinePlacementStatus.Valid;
    }
    public override getChildExpression(): CsgExpression | null {
        return this;
    }
    public override isStructuralTraversalSafe(): boolean {
        return true;
    }
    // C++ CsgExpression default.
    public queryStructureVersion(_version: number[]): boolean {
        return false;
    }
}
