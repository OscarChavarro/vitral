import { describe, expect, it } from "vitest";
import { IndexOutOfBoundsException } from "java/lang/IndexOutOfBoundsException.js";
import { AlgebraicExpression } from "vsdk/toolkit/common/symbolicAlgebra/AlgebraicExpression.js";
import { AlgebraicExpressionException } from "vsdk/toolkit/common/symbolicAlgebra/AlgebraicExpressionException.js";

describe("AlgebraicExpression Java parser", () => {
    it.each([
        ["(sin(PI/2) + -4)", "(sin((PI) / (2.00))) + (-4.00)", -3],
        ["2^3^2", "(2.00) ^ ((3.00) ^ (2.00))", 512],
        ["8 / 4 / 2", "(8.00) / ((4.00) / (2.00))", 4],
        ["9 - 3 - 1", "(9.00) - ((3.00) - (1.00))", 7],
        ["(-0)", "-0.00", -0],
        ["sqrt 4", "sqrt(4.00)", 2],
    ])("preserves the Java expression tree and value for %s", (source, tree, value) => {
        const expression = new AlgebraicExpression();
        expression.setExpression(source);

        expect(expression.toString()).toBe(tree);
        expect(expression.eval()).toBe(value);
    });

    it("preserves the previous expression after a failed parse", () => {
        const expression = new AlgebraicExpression();
        expression.setExpression("PI");

        expect(() => expression.setExpression("-")).toThrow("Parse error, invalid placement for operator '-'");
        expect(() => expression.setExpression("()")).toThrow(IndexOutOfBoundsException);
        expect(expression.eval()).toBe(Math.PI);
    });

    it("reports an unknown function even when its name is on Object.prototype", () => {
        const expression = new AlgebraicExpression();
        expression.setExpression("constructor(0)");

        expect(() => expression.eval()).toThrow(
            new AlgebraicExpressionException('Unknown unary operator or function "constructor"'),
        );
    });
});
