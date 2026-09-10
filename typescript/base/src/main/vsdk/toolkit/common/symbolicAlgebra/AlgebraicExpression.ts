import { FundamentalEntity } from "../FundamentalEntity.js";
import { AlgebraicExpressionException } from "./AlgebraicExpressionException.js";
import { _AlgebraicExpressionNode } from "./_AlgebraicExpressionNode.js";
import { _AlgebraicExpressionConstantNode } from "./_AlgebraicExpressionConstantNode.js";
import { _AlgebraicExpressionVariableNode } from "./_AlgebraicExpressionVariableNode.js";
import { _AlgebraicExpressionBinaryOperatorNode } from "./_AlgebraicExpressionBinaryOperatorNode.js";
import { _AlgebraicExpressionUnaryOperatorNode } from "./_AlgebraicExpressionUnaryOperatorNode.js";

export class AlgebraicExpression extends FundamentalEntity {
    private root: _AlgebraicExpressionNode | undefined;
    private readonly values = new Map<string, number>([
        ["PI", Math.PI],
        ["E", Math.E],
    ]);
    public defineValue(name: string, value: number): void {
        this.values.set(name, value);
    }
    public getVariableValue(name: string): number {
        const value = this.values.get(name);
        if (value === undefined)
            throw new AlgebraicExpressionException(
                `AlgebraicExpression.getVariableValue: Variable "${name}" not defined`,
            );
        return value;
    }
    public setExpression(expression: string): void {
        const tokens =
            expression.match(/(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?|[A-Za-z_][A-Za-z_0-9]*|[()+\-*/^]/g) ?? [];
        if (tokens.length === 0) throw new AlgebraicExpressionException("Parse error, empty expression");
        this.root = this.parse(tokens);
    }
    public eval(): number {
        if (!this.root) throw new AlgebraicExpressionException("Null expression, can not evaluate.");
        return this.root.eval();
    }
    public override toString(): string {
        return this.root?.toString() ?? "<Invalid Expression>";
    }
    private parse(tokens: string[]): _AlgebraicExpressionNode {
        let index = 0;
        const parsePrimary = (): _AlgebraicExpressionNode => {
            const t = tokens[index++];
            if (t === undefined) throw new AlgebraicExpressionException("Parse error, unexpected end of expression");
            if (t === "(") {
                const n = parseBinary(0);
                if (tokens[index++] !== ")")
                    throw new AlgebraicExpressionException("Parse error, missing closing parenthesis");
                return n;
            }
            if (t === "-") {
                const n = new _AlgebraicExpressionUnaryOperatorNode(this, "-");
                n.setOperand(parsePrimary());
                return n;
            }
            if (/^[A-Za-z_]/.test(t) && tokens[index] === "(") {
                index++;
                const n = new _AlgebraicExpressionUnaryOperatorNode(this, t);
                n.setOperand(parseBinary(0));
                if (tokens[index++] !== ")")
                    throw new AlgebraicExpressionException("Parse error, missing closing parenthesis");
                return n;
            }
            if (/^(?:\d|\.)/.test(t)) return new _AlgebraicExpressionConstantNode(Number(t));
            return new _AlgebraicExpressionVariableNode(this, t);
        };
        const precedence: Record<string, number> = { "+": 1, "-": 1, "*": 2, "/": 2, "^": 3 };
        const parseBinary = (minimum: number): _AlgebraicExpressionNode => {
            let left = parsePrimary();
            while (true) {
                const op = tokens[index],
                    p = op === undefined ? -1 : (precedence[op] ?? -1);
                if (p < minimum) break;
                index++;
                const right = parseBinary(p + (op === "^" ? 0 : 1));
                const node = new _AlgebraicExpressionBinaryOperatorNode(this, op!);
                node.setLeftOperand(left);
                node.setRightOperand(right);
                left = node;
            }
            return left;
        };
        const result = parseBinary(0);
        if (index !== tokens.length) throw new AlgebraicExpressionException(`Parse error near "${tokens[index]}"`);
        return result;
    }
}
