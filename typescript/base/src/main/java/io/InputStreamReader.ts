import { InputStream } from "./InputStream.js";
import { Reader } from "./Reader.js";
/** UTF-8 decoding adapter; Java's default charset is intentionally not inferred. */
export class InputStreamReader extends Reader {
    private readonly decoder: TextDecoder;
    private pending = "";
    public constructor(
        private readonly input: InputStream,
        charset = "utf-8",
    ) {
        super();
        this.decoder = new TextDecoder(charset, { fatal: true });
    }
    public override read(): number {
        while (this.pending.length === 0) {
            const byte = this.input.read();
            if (byte < 0) return -1;
            this.pending = this.decoder.decode(Uint8Array.of(byte), { stream: true });
        }
        const value = this.pending.charCodeAt(0);
        this.pending = this.pending.slice(1);
        return value;
    }
    public override close(): void {
        this.input.close();
    }
}
