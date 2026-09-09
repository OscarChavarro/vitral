import { InputStream } from "./InputStream.js";

export class ByteArrayInputStream extends InputStream {
  private position: number;
  private markPosition: number;
  private readonly limit: number;

  public constructor(private readonly bytes: Uint8Array, offset = 0, length = bytes.length - offset) {
    super();
    InputStream.requireRange(bytes, offset, length);
    this.position = offset;
    this.markPosition = offset;
    this.limit = offset + length;
  }

  public override read(): number { return this.position >= this.limit ? -1 : (this.bytes[this.position++] ?? -1); }
  public override readBytes(buffer: Uint8Array, offset = 0, length = buffer.length - offset): number {
    InputStream.requireRange(buffer, offset, length);
    const available = this.limit - this.position;
    if (available <= 0) return -1;
    const amount = globalThis.Math.min(length, available);
    if (amount === 0) return 0;
    buffer.set(this.bytes.subarray(this.position, this.position + amount), offset);
    this.position += amount;
    return amount;
  }
  public override skip(count: number): number { const amount = globalThis.Math.min(globalThis.Math.max(0, count), this.limit - this.position); this.position += amount; return amount; }
  public override available(): number { return this.limit - this.position; }
  public override mark(_readLimit: number): void { this.markPosition = this.position; }
  public override reset(): void { this.position = this.markPosition; }
  public override markSupported(): boolean { return true; }
}
