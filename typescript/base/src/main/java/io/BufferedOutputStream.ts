import { OutputStream } from "./OutputStream.js";
export class BufferedOutputStream extends OutputStream {
  private readonly buffer: number[] = [];
  public constructor(private readonly output: OutputStream, private readonly capacity = 8192) { super(); if (capacity <= 0) throw new RangeError("Buffer size must be positive"); }
  public override write(value: number): void { this.buffer.push(value & 0xff); if (this.buffer.length >= this.capacity) this.flush(); }
  public override writeBytes(bytes: Uint8Array, offset = 0, length = bytes.length - offset): void { OutputStream.requireRange(bytes, offset, length); if (length >= this.capacity) { this.flush(); this.output.writeBytes(bytes, offset, length); return; } for (let i = 0; i < length; i++) this.write(bytes[offset + i] ?? 0); }
  public override flush(): void { if (this.buffer.length > 0) { this.output.writeBytes(Uint8Array.from(this.buffer)); this.buffer.length = 0; } this.output.flush(); }
  public override close(): void { this.flush(); this.output.close(); }
}
