import { ArrayList } from "../../../../../java/util/ArrayList.js";
import { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import type { Geometry } from "../../geometry/Geometry.js";
import { AffinePlacementStatus } from "../AffinePlacementStatus.js";
import type { CsgExpression } from "./CsgExpression.js";
import { CsgExpressionEquivalence } from "./CsgExpressionEquivalence.js";
import { CsgOperandView } from "./CsgOperandView.js";
import type { CsgOperation } from "./CsgOperation.js";

// An application-built ordered node. Operands are borrowed and must remain
// alive. addOperand rejects cycles visible through the current expression graph.
// A nested tree is identity-placed; evaluators and ownership are separate.
export class CsgTree extends CsgOperandView implements CsgExpression {
    private operation: CsgOperation;
    private readonly operands: ArrayList<CsgOperandView>;
    private structureVersion: number;

    public constructor(value: CsgOperation) {
        super();
        this.operation = value;
        this.operands = new ArrayList<CsgOperandView>(4);
        this.structureVersion = 0;
    }

    public getOperation(): CsgOperation {
        return this.operation;
    }

    public getOperandCount(): number {
        return this.operands.size();
    }

    public getOperand(index: number): CsgOperandView | null {
        return index < 0 || index >= this.operands.size() ? null : this.operands.get(index);
    }

    private canAttach(operand: CsgOperandView): boolean {
        const child = operand.getChildExpression();
        if (child === null) return true;
        const pending = new ArrayList<CsgExpression>();
        const visited = new ArrayList<CsgExpression>();
        if (!pending.add(child)) return false;
        for (let i = 0; i < pending.size(); ++i) {
            const current = pending.get(i);
            if (current === this) return false;
            let known = false;
            for (let j = 0; j < visited.size(); ++j) {
                if (visited.get(j) === current) {
                    known = true;
                    break;
                }
            }
            if (known) continue;
            if (!visited.add(current)) return false;
            for (let j = 0; j < current.getOperandCount(); ++j) {
                const nested = current.getOperand(j);
                if (
                    nested !== null &&
                    nested.getChildExpression() !== null &&
                    !pending.add(nested.getChildExpression()!)
                )
                    return false;
            }
        }
        return true;
    }

    public addOperand(operand: CsgOperandView): boolean {
        if (!this.canAttach(operand) || !this.operands.add(operand)) return false;
        ++this.structureVersion;
        return true;
    }

    // Replaces the operation (e.g. a De Morgan complement); a structure edit.
    public setOperation(value: CsgOperation): void {
        this.operation = value;
        ++this.structureVersion;
    }

    public queryStructureVersion(version: number[]): boolean {
        version[0] = this.structureVersion;
        return true;
    }

    public override queryModificationVersion(version: number[]): boolean {
        version[0] = this.structureVersion;
        return true;
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

    public override isSameSolid(other: CsgOperandView): boolean {
        return CsgExpressionEquivalence.same(this, other);
    }

    public override isStructuralTraversalSafe(): boolean {
        return this.constructor === CsgTree;
    }
}
