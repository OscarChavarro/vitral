// Port of cpp/base/src/test/CsgPolicyTestOperand.{h,cpp}
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import type { Geometry } from "vsdk/toolkit/environment/geometry/Geometry.js";
import { AffinePlacementStatus } from "vsdk/toolkit/environment/scene/AffinePlacementStatus.js";
import type { CsgExpression } from "vsdk/toolkit/environment/scene/csg/CsgExpression.js";
import { CsgOperandView } from "vsdk/toolkit/environment/scene/csg/CsgOperandView.js";

export class CsgPolicyTestOperand extends CsgOperandView {
    private readonly expression: CsgExpression;
    private readonly inverted: boolean;

    public constructor(value: CsgExpression, invert: boolean) {
        super();
        this.expression = value;
        this.inverted = invert;
    }
    public isInverted(): boolean {
        return this.inverted;
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
        return this.expression;
    }
}
