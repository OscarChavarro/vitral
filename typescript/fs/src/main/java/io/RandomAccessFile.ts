import * as fs from "node:fs";
import { File } from "./File.js";
export class RandomAccessFile {
  private readonly descriptor: number;
  private offset = 0;
  public constructor(file: string | File, mode: "r" | "rw") { this.descriptor = fs.openSync(typeof file === "string" ? file : file.getPath().toCString(), mode === "r" ? "r" : "r+"); }
  public getFilePointer(): number { return this.offset; } public seek(position: number): void { if (!Number.isInteger(position) || position < 0) throw new RangeError("Negative seek offset"); this.offset = position; }
  public length(): number { return fs.fstatSync(this.descriptor).size; }
  public read(): number { const byte = new Uint8Array(1); const count = fs.readSync(this.descriptor, byte, 0, 1, this.offset); if (count === 0) return -1; this.offset++; return byte[0] ?? -1; }
  public readFully(target: Uint8Array): void { const count = fs.readSync(this.descriptor, target, 0, target.length, this.offset); if (count !== target.length) throw new RangeError("Unexpected end of file"); this.offset += count; }
  public write(value: number): void { const byte = Uint8Array.of(value & 0xff); fs.writeSync(this.descriptor, byte, 0, 1, this.offset); this.offset++; }
  public writeBytes(bytes: Uint8Array): void { const count = fs.writeSync(this.descriptor, bytes, 0, bytes.length, this.offset); this.offset += count; }
  public setLength(length: number): void { fs.ftruncateSync(this.descriptor, length); if (this.offset > length) this.offset = length; }
  public close(): void { fs.closeSync(this.descriptor); }
}
