import { _AlgebraicExpressionNode } from "./_AlgebraicExpressionNode.js";
import type { AlgebraicExpression } from "./AlgebraicExpression.js";
export class _AlgebraicExpressionVariableNode extends _AlgebraicExpressionNode {
    public constructor(
        private readonly parent: AlgebraicExpression,
        private readonly name: string,
    ) {
        super();
    }
    public eval(): number {
        return this.parent.getVariableValue(this.name);
    }
    public override toString(): string {
        return this.name;
    }
}
