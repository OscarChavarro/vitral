import type { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import type { Geometry } from "../../geometry/Geometry.js";
import { AffinePlacement } from "../AffinePlacement.js";
import { AffinePlacementStatus } from "../AffinePlacementStatus.js";
import type { CsgExpression } from "./CsgExpression.js";
import { CsgExpressionEquivalence } from "./CsgExpressionEquivalence.js";
import { CsgOperandView } from "./CsgOperandView.js";

// Borrowed child expression and an independently editable child-to-parent placement.
export class CsgPlacedExpression extends CsgOperandView {
    private readonly child: CsgExpression;
    private placement: AffinePlacement;
    private modificationVersion: number;

    public constructor(expression: CsgExpression) {
        super();
        this.child = expression;
        this.placement = new AffinePlacement();
        this.modificationVersion = 0;
    }

    public setAffinePlacement(forward: Matrix4x4d, inverse: Matrix4x4d): AffinePlacementStatus {
        const candidate = new AffinePlacement();
        const status = candidate.assign(forward, inverse);
        if (status === AffinePlacementStatus.Valid) {
            this.placement = candidate;
            ++this.modificationVersion;
        }
        return status;
    }

    public getGeometry(): Geometry | null {
        return null;
    }

    public getAffinePlacement(): Matrix4x4d {
        return this.placement.getForward();
    }

    public override queryAffinePlacement(forward: Matrix4x4d[], inverse: Matrix4x4d[]): AffinePlacementStatus {
        forward[0] = this.placement.getForward();
        inverse[0] = this.placement.getInverse();
        return this.placement.getStatus();
    }

    public override getChildExpression(): CsgExpression | null {
        return this.child;
    }

    public override isSameSolid(other: CsgOperandView): boolean {
        return CsgExpressionEquivalence.same(this, other);
    }

    public override queryModificationVersion(version: number[]): boolean {
        version[0] = this.modificationVersion;
        return true;
    }

    public override isStructuralTraversalSafe(): boolean {
        return this.constructor === CsgPlacedExpression;
    }
}
