import { InputStream } from "./InputStream.js";
export class DataInputStream {
  public constructor(private readonly input: InputStream) {}
  public readUnsignedByte(): number { const value = this.input.read(); if (value < 0) throw new RangeError("Unexpected end of stream"); return value; }
  public readByte(): number { const value = this.readUnsignedByte(); return value > 127 ? value - 256 : value; }
  public readBoolean(): boolean { return this.readUnsignedByte() !== 0; }
  public readInt(): number { return (this.readUnsignedByte() << 24) | (this.readUnsignedByte() << 16) | (this.readUnsignedByte() << 8) | this.readUnsignedByte(); }
  public readLong(): bigint { let value = 0n; for (let i = 0; i < 8; i++) value = (value << 8n) | BigInt(this.readUnsignedByte()); return BigInt.asIntN(64, value); }
  public readFully(target: Uint8Array): void { let offset = 0; while (offset < target.length) { const count = this.input.readBytes(target, offset); if (count < 0) throw new RangeError("Unexpected end of stream"); offset += count; } }
  public close(): void { this.input.close(); }
}
