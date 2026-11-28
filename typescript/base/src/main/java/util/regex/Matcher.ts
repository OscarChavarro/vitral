import { IllegalStateException } from "../../lang/IllegalStateException.js";
import { IndexOutOfBoundsException } from "../../lang/IndexOutOfBoundsException.js";

export class Matcher {
    private last: RegExpExecArray | undefined;
    private searchOffset = 0;
    public constructor(
        private readonly expression: string,
        private readonly flags: string,
        private input: string,
    ) {}
    public matches(): boolean {
        const expression = new RegExp(`(?:${this.expression})(?![\\s\\S])`, `${this.flags}dy`);
        const match = expression.exec(this.input);
        this.recordMatch(match);
        return match !== null;
    }
    public find(start?: number): boolean {
        if (start !== undefined) {
            if (!Number.isInteger(start) || start < 0 || start > this.input.length)
                throw new IndexOutOfBoundsException(`Illegal start index: ${start}`);
            this.reset();
            this.searchOffset = start;
        }
        const expression = new RegExp(this.expression, `${this.flags}dg`);
        expression.lastIndex = this.searchOffset;
        const result = expression.exec(this.input);
        // Unicode matching can rewind a zero-length search into a surrogate pair.
        if (result !== null && result.index < this.searchOffset) {
            expression.lastIndex = this.searchOffset + 1;
            const match = expression.exec(this.input);
            this.recordMatch(match);
            return match !== null;
        }
        this.recordMatch(result);
        return result !== null;
    }
    public group(index = 0): string | null {
        return this.getMatch(index)[index] ?? null;
    }
    public groupCount(): number {
        // The empty alternative lets RegExp expose the number of captures before a match.
        return this.last
            ? this.last.length - 1
            : new RegExp(`(?:${this.expression})|`, this.flags).exec("")!.length - 1;
    }
    public start(index = 0): number {
        return this.getMatch(index).indices![index]?.[0] ?? -1;
    }
    public end(index = 0): number {
        return this.getMatch(index).indices![index]?.[1] ?? -1;
    }
    public reset(input = this.input): Matcher {
        this.input = input;
        this.last = undefined;
        this.searchOffset = 0;
        return this;
    }
    private recordMatch(match: RegExpExecArray | null): void {
        this.last = match ?? undefined;
        if (match !== null) this.searchOffset = match.index + Math.max(match[0].length, 1);
    }
    private getMatch(index: number): RegExpExecArray {
        if (this.last === undefined) throw new IllegalStateException("No match available");
        if (!Number.isInteger(index) || index < 0 || index >= this.last.length)
            throw new IndexOutOfBoundsException(`No group ${index}`);
        return this.last;
    }
}
