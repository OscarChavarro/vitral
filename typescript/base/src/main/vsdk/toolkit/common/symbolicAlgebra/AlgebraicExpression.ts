import { IOException } from "../../../../java/io/IOException.js";
import { StreamTokenizer } from "../../../../java/io/StreamTokenizer.js";
import { StringReader } from "../../../../java/io/StringReader.js";
import { Double } from "../../../../java/lang/Double.js";
import { ArrayList } from "../../../../java/util/ArrayList.js";
import { FundamentalEntity } from "../FundamentalEntity.js";
import { AlgebraicExpressionException } from "./AlgebraicExpressionException.js";
import { _AlgebraicExpressionNode } from "./_AlgebraicExpressionNode.js";
import { _AlgebraicExpressionConstantNode } from "./_AlgebraicExpressionConstantNode.js";
import { _AlgebraicExpressionVariableNode } from "./_AlgebraicExpressionVariableNode.js";
import { _AlgebraicExpressionBinaryOperatorNode } from "./_AlgebraicExpressionBinaryOperatorNode.js";
import { _AlgebraicExpressionUnaryOperatorNode } from "./_AlgebraicExpressionUnaryOperatorNode.js";

/** Parses algebraic expressions into a tree that evaluates with the current variable bindings. */
export class AlgebraicExpression extends FundamentalEntity {
    private root: _AlgebraicExpressionNode | null = null;
    private readonly values = new Map<string, number>();

    public constructor() {
        super();
        this.defineValue("PI", Math.PI);
        this.defineValue("E", Math.E);
    }

    public defineValue(name: string, value: number): void {
        this.values.set(name, value);
    }

    public override toString(): string {
        return this.root === null ? "<Invalid Expression>" : this.root.toString();
    }

    public getVariableValue(name: string): number {
        const value = this.values.get(name);
        if (value === undefined) {
            throw new AlgebraicExpressionException(
                'AlgebraicExpression.getVariableValue: Variable "' + name + '" not defined',
            );
        }
        return value;
    }

    private isOperator(token: string): boolean {
        switch (token) {
            case "+":
            case "-":
            case "*":
            case "/":
            case "^":
                return true;
            default:
                return false;
        }
    }

    private buildExpressionTree(tokens: ArrayList<string>): _AlgebraicExpressionNode {
        // Trim enclosing parentheses only when they enclose the entire expression.
        while (tokens.get(0) === "(" && tokens.get(tokens.size() - 1) === ")") {
            let level = 0;
            let trim = true;
            for (let i = 0; i < tokens.size(); i++) {
                const c = tokens.get(i).charAt(0);
                if (c === "(") {
                    level++;
                } else if (c === ")") {
                    level--;
                }
                if (level === 0 && i < tokens.size() - 1) {
                    trim = false;
                }
            }
            if (!trim) {
                break;
            }
            tokens.remove(tokens.size() - 1);
            tokens.remove(0);
        }

        if (tokens.size() === 1) {
            const token = tokens.get(0);
            const c = token.charAt(0);
            if (this.isOperator(token)) {
                throw new AlgebraicExpressionException("Parse error, invalid placement for operator '" + c + "'");
            }
            // Java Character.isDigit(char) recognizes Unicode decimal digits.
            if (/\p{Nd}/u.test(c) || c === "-") {
                return new _AlgebraicExpressionConstantNode(Double.parseDouble(token));
            }
            return new _AlgebraicExpressionVariableNode(this, token);
        }

        const topLevelConnectorIndexes = new ArrayList<number>();
        let level = 0;
        for (let i = 0; i < tokens.size(); i++) {
            const token = tokens.get(i);
            const c = token.charAt(0);
            if (c === "(") {
                level++;
                continue;
            } else if (c === ")") {
                level--;
                continue;
            }
            if (level === 0 && this.isOperator(token)) {
                topLevelConnectorIndexes.add(i);
            }
        }

        if (topLevelConnectorIndexes.isEmpty()) {
            const unary = new _AlgebraicExpressionUnaryOperatorNode(this, tokens.get(0));
            tokens.remove(0);
            unary.setOperand(this.buildExpressionTree(tokens));
            return unary;
        }

        // Within each precedence group, Java selects the first connector.
        // The resulting binary chains group from the right.
        let mainConnector = 0;
        for (const index of topLevelConnectorIndexes) {
            if (tokens.get(index).charAt(0) === "^") {
                mainConnector = index;
                break;
            }
        }
        for (const index of topLevelConnectorIndexes) {
            const c = tokens.get(index).charAt(0);
            if (c === "*" || c === "/") {
                mainConnector = index;
                break;
            }
        }
        for (const index of topLevelConnectorIndexes) {
            const c = tokens.get(index).charAt(0);
            if (c === "+" || c === "-") {
                mainConnector = index;
                break;
            }
        }

        const operator = tokens.get(mainConnector).charAt(0);
        if (operator === "-" && mainConnector === 0) {
            const unary = new _AlgebraicExpressionUnaryOperatorNode(this, "-");
            tokens.remove(0);
            unary.setOperand(this.buildExpressionTree(tokens));
            return unary;
        }

        const leftTokens = new ArrayList<string>();
        for (let i = 0; i < mainConnector; i++) {
            leftTokens.add(tokens.get(i));
        }
        const rightTokens = new ArrayList<string>();
        for (let i = mainConnector + 1; i < tokens.size(); i++) {
            rightTokens.add(tokens.get(i));
        }

        const binary = new _AlgebraicExpressionBinaryOperatorNode(this, operator);
        binary.setLeftOperand(this.buildExpressionTree(leftTokens));
        binary.setRightOperand(this.buildExpressionTree(rightTokens));
        return binary;
    }

    public setExpression(expression: string): void {
        const stream = new StringReader(expression);
        const parser = new StreamTokenizer(stream);
        const tokens = new ArrayList<string>();
        parser.resetSyntax();
        parser.eolIsSignificant(false);
        parser.slashSlashComments(false);
        parser.slashStarComments(false);
        parser.whitespaceChars(32, 32);
        parser.wordChars(65, 90);
        parser.wordChars(97, 122);
        parser.wordChars(48, 57);
        parser.wordChars(95, 95);
        for (const operator of "+-*/()^") {
            parser.ordinaryChar(operator.charCodeAt(0));
        }
        parser.parseNumbers();

        let tokenType: number;
        do {
            try {
                tokenType = parser.nextToken();
            } catch (error) {
                if (error instanceof IOException) {
                    break;
                }
                throw error;
            }
            switch (tokenType) {
                case StreamTokenizer.TT_EOL:
                case StreamTokenizer.TT_EOF:
                    break;
                case StreamTokenizer.TT_NUMBER:
                    tokens.add(Double.toString(parser.nval));
                    break;
                case StreamTokenizer.TT_WORD:
                    // The tokenizer sets sval before returning TT_WORD.
                    tokens.add(parser.sval!);
                    break;
                default:
                    if (parser.ttype !== 34) {
                        tokens.add(parser.toString().charAt(7));
                    }
                    break;
            }
        } while (tokenType !== StreamTokenizer.TT_EOF);

        this.root = this.buildExpressionTree(tokens);
    }

    public eval(): number {
        if (this.root === null) {
            throw new AlgebraicExpressionException("Null expression, can not evaluate.");
        }
        return this.root.eval();
    }
}
