import { VSDK } from "../VSDK.js";
import { _AlgebraicExpressionNode } from "./_AlgebraicExpressionNode.js";
export class _AlgebraicExpressionConstantNode extends _AlgebraicExpressionNode {
    public constructor(private readonly value: number) {
        super();
    }
    public eval(): number {
        return this.value;
    }
    public override toString(): string {
        return VSDK.formatDouble(this.value);
    }
}
