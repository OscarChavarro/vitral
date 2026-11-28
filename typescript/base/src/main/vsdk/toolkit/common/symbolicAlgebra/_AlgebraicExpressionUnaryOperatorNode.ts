import { NullPointerException } from "../../../../java/lang/NullPointerException.js";
import { Math as JavaMath } from "../../../../java/lang/Math.js";
import { AlgebraicExpressionException } from "./AlgebraicExpressionException.js";
import { _AlgebraicExpressionNode } from "./_AlgebraicExpressionNode.js";

export class _AlgebraicExpressionUnaryOperatorNode extends _AlgebraicExpressionNode {
    private operand: _AlgebraicExpressionNode | null = null;

    public constructor(
        _parent: unknown,
        private readonly name: string,
    ) {
        super();
    }

    public setOperand(operand: _AlgebraicExpressionNode): void {
        this.operand = operand;
    }

    public eval(): number {
        if (this.operand === null) {
            throw new NullPointerException();
        }
        const value = this.operand.eval();
        switch (this.name) {
            case "sin":
                return Math.sin(value);
            case "cos":
                return Math.cos(value);
            case "tan":
                return Math.tan(value);
            case "asin":
                return Math.asin(value);
            case "acos":
                return Math.acos(value);
            case "atan":
                return Math.atan(value);
            case "abs":
                return Math.abs(value);
            case "cbrt":
                return Math.pow(value, 1.0 / 3.0);
            case "ceil":
                return Math.ceil(value);
            case "sinh":
                return JavaMath.sinh(value);
            case "cosh":
                return JavaMath.cosh(value);
            case "tanh":
                return Math.tanh(value);
            case "toDegrees":
                return value * (180.0 / Math.PI);
            case "toRadians":
                return value * (Math.PI / 180.0);
            case "exp":
                return Math.exp(value);
            case "floor":
                return Math.floor(value);
            case "log":
            case "ln":
                return Math.log(value);
            case "log10":
                return Math.log10(value);
            case "sqrt":
                return Math.sqrt(value);
            case "-":
                return -value;
            default:
                throw new AlgebraicExpressionException('Unknown unary operator or function "' + this.name + '"');
        }
    }

    public override toString(): string {
        if (this.operand === null) {
            throw new NullPointerException();
        }
        return this.name + "(" + this.operand.toString() + ")";
    }
}
