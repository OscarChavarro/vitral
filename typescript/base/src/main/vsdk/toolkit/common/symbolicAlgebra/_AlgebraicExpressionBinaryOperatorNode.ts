import { NullPointerException } from "../../../../java/lang/NullPointerException.js";
import { AlgebraicExpressionException } from "./AlgebraicExpressionException.js";
import { _AlgebraicExpressionNode } from "./_AlgebraicExpressionNode.js";

export class _AlgebraicExpressionBinaryOperatorNode extends _AlgebraicExpressionNode {
    private leftOperand: _AlgebraicExpressionNode | null = null;
    private rightOperand: _AlgebraicExpressionNode | null = null;
    public constructor(
        _parent: unknown,
        private readonly operator: string,
    ) {
        super();
    }
    public setLeftOperand(operand: _AlgebraicExpressionNode): void {
        this.leftOperand = operand;
    }
    public setRightOperand(operand: _AlgebraicExpressionNode): void {
        this.rightOperand = operand;
    }

    public eval(): number {
        if (this.leftOperand === null) {
            throw new NullPointerException();
        }
        const left = this.leftOperand.eval();
        if (this.rightOperand === null) {
            throw new NullPointerException();
        }
        const right = this.rightOperand.eval();
        switch (this.operator) {
            case "+":
                return left + right;
            case "-":
                return left - right;
            case "*":
                return left * right;
            case "/":
                return left / right;
            case "^":
                return Math.pow(left, right);
            default:
                throw new AlgebraicExpressionException(`Unknown binary operator "${this.operator}"`);
        }
    }

    public override toString(): string {
        if (this.leftOperand === null) {
            throw new NullPointerException();
        }
        const left = this.leftOperand.toString();
        if (this.rightOperand === null) {
            throw new NullPointerException();
        }
        return `(${left}) ${this.operator} (${this.rightOperand.toString()})`;
    }
}
