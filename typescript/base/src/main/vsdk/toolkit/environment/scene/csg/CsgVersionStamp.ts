import { ArrayList } from "../../../../../java/util/ArrayList.js";
import type { CsgExpression } from "./CsgExpression.js";
import type { CsgOperandView } from "./CsgOperandView.js";

// Modification versions of every expression node and operand reachable from
// a root, recorded when a result that borrows them is built. A result is known
// to be current only when the stamp is complete (every node and operand
// reports a version) and every version is unchanged. Versions cover edits made
// through the scene and CSG APIs; geometry parameters changed behind a body's
// back are not observed. Storage is reused across captures.
export class CsgVersionStamp {
    private readonly expressions: ArrayList<CsgExpression>;
    private readonly expressionVersions: ArrayList<number>;
    private readonly operands: ArrayList<CsgOperandView>;
    private readonly operandVersions: ArrayList<number>;
    private readonly path: ArrayList<CsgExpression>;
    private complete: boolean;
    private captured: boolean;

    public constructor();
    /** C++ copy constructor. */
    public constructor(other: CsgVersionStamp);
    public constructor(other?: CsgVersionStamp) {
        this.expressions = new ArrayList<CsgExpression>(4);
        this.expressionVersions = new ArrayList<number>(4);
        this.operands = new ArrayList<CsgOperandView>(8);
        this.operandVersions = new ArrayList<number>(8);
        this.path = new ArrayList<CsgExpression>(4);
        this.complete = false;
        this.captured = false;
        if (other !== undefined) this.assign(other);
    }

    /** C++ copy assignment. */
    public assign(other: CsgVersionStamp): CsgVersionStamp {
        if (this !== other) {
            this.clear();
            for (let i = 0; i < other.expressions.size(); ++i) {
                this.expressions.add(other.expressions.get(i));
                this.expressionVersions.add(other.expressionVersions.get(i));
            }
            for (let i = 0; i < other.operands.size(); ++i) {
                this.operands.add(other.operands.get(i));
                this.operandVersions.add(other.operandVersions.get(i));
            }
            this.complete = other.complete;
            this.captured = other.captured;
        }
        return this;
    }

    public clear(): void {
        this.expressions.clear();
        this.expressionVersions.clear();
        this.operands.clear();
        this.operandVersions.clear();
        this.path.clear();
        this.complete = false;
        this.captured = false;
    }

    private record(expression: CsgExpression): boolean {
        if (this.path.contains(expression) || !this.path.add(expression)) return false;
        const version: number[] = [0];
        if (!expression.queryStructureVersion(version)) this.complete = false;
        if (!this.expressions.add(expression) || !this.expressionVersions.add(version[0]!)) return false;
        for (let i = 0; i < expression.getOperandCount(); ++i) {
            const operand = expression.getOperand(i);
            if (operand === null) {
                this.complete = false;
                continue;
            }
            const operandVersion: number[] = [0];
            if (!operand.queryModificationVersion(operandVersion)) this.complete = false;
            if (!this.operands.add(operand) || !this.operandVersions.add(operandVersion[0]!)) return false;
            const child = operand.getChildExpression();
            if (child !== null && !this.record(child)) return false;
        }
        this.path.remove(this.path.size() - 1);
        return true;
    }

    // False only for a cyclic expression or exhausted storage; the stamp is
    // then cleared.
    public capture(root: CsgExpression): boolean {
        this.clear();
        this.complete = true;
        if (!this.record(root)) {
            this.clear();
            return false;
        }
        this.path.clear();
        this.captured = true;
        return true;
    }

    public isCaptured(): boolean {
        return this.captured;
    }

    public isComplete(): boolean {
        return this.captured && this.complete;
    }

    public isCurrent(): boolean {
        if (!this.isComplete()) return false;
        for (let i = 0; i < this.expressions.size(); ++i) {
            const version: number[] = [0];
            if (
                !this.expressions.get(i).queryStructureVersion(version) ||
                version[0] !== this.expressionVersions.get(i)
            )
                return false;
        }
        for (let i = 0; i < this.operands.size(); ++i) {
            const version: number[] = [0];
            if (!this.operands.get(i).queryModificationVersion(version) || version[0] !== this.operandVersions.get(i))
                return false;
        }
        return true;
    }
}
