import { Reader } from "./Reader.js";
export class StreamTokenizer {
    public static readonly TT_EOF = -1;
    public static readonly TT_EOL = 10;
    public static readonly TT_NUMBER = -2;
    public static readonly TT_WORD = -3;
    public ttype = StreamTokenizer.TT_EOF;
    public nval = 0;
    public sval: string | undefined;
    private readonly input: string;
    private offset = 0;
    private pushed = false;
    public constructor(reader: Reader) {
        const chars: string[] = [];
        for (let c = reader.read(); c >= 0; c = reader.read()) chars.push(globalThis.String.fromCharCode(c));
        this.input = chars.join("");
    }
    public nextToken(): number {
        if (this.pushed) {
            this.pushed = false;
            return this.ttype;
        }
        this.sval = undefined;
        while (/\s/.test(this.input[this.offset] ?? "")) {
            if ((this.input[this.offset++] ?? "") === "\n") {
                this.ttype = StreamTokenizer.TT_EOL;
                return this.ttype;
            }
        }
        const start = this.offset;
        const first = this.input[this.offset];
        if (first === undefined) return (this.ttype = StreamTokenizer.TT_EOF);
        if (/[A-Za-z_]/.test(first)) {
            while (/[A-Za-z0-9_]/.test(this.input[this.offset] ?? "")) this.offset++;
            this.sval = this.input.slice(start, this.offset);
            return (this.ttype = StreamTokenizer.TT_WORD);
        }
        if (/[0-9.+-]/.test(first)) {
            while (/[0-9.eE+-]/.test(this.input[this.offset] ?? "")) this.offset++;
            const number = Number(this.input.slice(start, this.offset));
            if (!Number.isNaN(number)) {
                this.nval = number;
                return (this.ttype = StreamTokenizer.TT_NUMBER);
            }
            this.offset = start;
        }
        this.offset++;
        return (this.ttype = first.charCodeAt(0));
    }
    public pushBack(): void {
        this.pushed = true;
    }
}
