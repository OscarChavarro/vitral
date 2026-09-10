export class Matcher {
    private last: RegExpExecArray | undefined;
    private searchOffset = 0;
    public constructor(
        private readonly expression: string,
        private readonly flags: string,
        private input: string,
    ) {}
    public matches(): boolean {
        const match = new RegExp(`^(?:${this.expression})$`, this.flags).exec(this.input);
        this.last = match ?? undefined;
        return match !== null;
    }
    public find(): boolean {
        const expression = new RegExp(this.expression, this.flags.includes("g") ? this.flags : `${this.flags}g`);
        expression.lastIndex = this.searchOffset;
        const match = expression.exec(this.input);
        this.last = match ?? undefined;
        if (match === null) return false;
        this.searchOffset = match.index + globalThis.Math.max(match[0].length, 1);
        return true;
    }
    public group(index = 0): string {
        if (this.last === undefined) throw new Error("No match available");
        const value = this.last[index];
        if (value === undefined) throw new RangeError(`No group ${index}`);
        return value;
    }
    public groupCount(): number {
        return this.last === undefined ? this.captureCount() : this.last.length - 1;
    }
    public start(index = 0): number {
        if (this.last === undefined || index !== 0)
            throw new Error("Only full-match position is available before a match");
        return this.last.index;
    }
    public end(index = 0): number {
        return this.start(index) + this.group(index).length;
    }
    public reset(input = this.input): Matcher {
        this.input = input;
        this.last = undefined;
        this.searchOffset = 0;
        return this;
    }
    private captureCount(): number {
        return new RegExp(this.expression, this.flags).exec("")?.length ? 0 : 0;
    }
}
