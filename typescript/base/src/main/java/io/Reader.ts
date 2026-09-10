/** Synchronous character reader. `read` returns a UTF-16 code unit or -1. */
export abstract class Reader {
    public abstract read(): number;
    public readChars(destination: string[], offset = 0, length = destination.length - offset): number {
        if (offset < 0 || length < 0 || offset > destination.length - length)
            throw new RangeError("Invalid character-buffer range");
        if (length === 0) return 0;
        let count = 0;
        while (count < length) {
            const character = this.read();
            if (character < 0) return count === 0 ? -1 : count;
            destination[offset + count++] = globalThis.String.fromCharCode(character);
        }
        return count;
    }
    public close(): void {
        /* Java Reader close may be a no-op. */
    }
}
