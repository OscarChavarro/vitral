/**
 * Synchronous counterpart of Java's InputStream. Java byte streams return an
 * unsigned byte (0..255) or -1 at EOF.
 */
export abstract class InputStream {
  public abstract read(): number;

  public readBytes(buffer: Uint8Array, offset = 0, length = buffer.length - offset): number {
    InputStream.requireRange(buffer, offset, length);
    if (length === 0) return 0;
    const first = this.read();
    if (first < 0) return -1;
    buffer[offset] = first;
    let count = 1;
    while (count < length) {
      const value = this.read();
      if (value < 0) break;
      buffer[offset + count] = value;
      count++;
    }
    return count;
  }

  public skip(count: number): number {
    if (count <= 0) return 0;
    const scratch = new Uint8Array(globalThis.Math.min(2_048, count));
    let skipped = 0;
    while (skipped < count) {
      const read = this.readBytes(scratch, 0, globalThis.Math.min(scratch.length, count - skipped));
      if (read < 0) break;
      skipped += read;
    }
    return skipped;
  }

  public available(): number { return 0; }
  public close(): void { /* Java InputStream.close is a no-op by default. */ }
  public mark(_readLimit: number): void { /* unsupported marks are ignored by Java. */ }
  public reset(): void { throw new Error("mark/reset not supported"); }
  public markSupported(): boolean { return false; }
  public dispose(): void { this.close(); }

  public static requireRange(buffer: Uint8Array, offset: number, length: number): void {
    if (!Number.isInteger(offset) || !Number.isInteger(length) || offset < 0 || length < 0 || offset > buffer.length - length) {
      throw new RangeError("Invalid byte-buffer range");
    }
  }
}
