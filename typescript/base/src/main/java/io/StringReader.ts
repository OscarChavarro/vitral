import { Reader } from "./Reader.js";
export class StringReader extends Reader {
    private offset = 0;
    private markOffset = 0;
    private closed = false;
    public constructor(private readonly text: string) {
        super();
    }
    public override read(): number {
        this.requireOpen();
        if (this.offset >= this.text.length) return -1;
        return this.text.charCodeAt(this.offset++);
    }
    public skip(count: number): number {
        this.requireOpen();
        const moved = globalThis.Math.max(0, globalThis.Math.min(count, this.text.length - this.offset));
        this.offset += moved;
        return moved;
    }
    public mark(): void {
        this.requireOpen();
        this.markOffset = this.offset;
    }
    public reset(): void {
        this.requireOpen();
        this.offset = this.markOffset;
    }
    public override close(): void {
        this.closed = true;
    }
    private requireOpen(): void {
        if (this.closed) throw new Error("Stream closed");
    }
}
