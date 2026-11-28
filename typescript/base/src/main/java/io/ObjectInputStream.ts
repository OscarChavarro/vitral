import { InputStream } from "./InputStream.js";
import { IOException } from "./IOException.js";

/** Reads one portable JSON record at a time, including legacy unframed records. */
export class ObjectInputStream {
    public constructor(private readonly input: InputStream) {}
    public readObject(): unknown {
        const chunks: number[] = [];
        let value: number;
        while ((value = this.input.read()) >= 0 && value !== 0x0a) chunks.push(value);
        if (chunks.length === 0 && value < 0) throw new IOException("End of object stream");
        return JSON.parse(new TextDecoder().decode(Uint8Array.from(chunks)), (_key, current: unknown) => {
            if (current !== null && typeof current === "object" && "$vitralBigInt" in current)
                return BigInt((current as { $vitralBigInt: string }).$vitralBigInt);
            return current;
        });
    }
    public close(): void {
        this.input.close();
    }
}
