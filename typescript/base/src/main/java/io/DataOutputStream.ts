import { OutputStream } from "./OutputStream.js";
export class DataOutputStream {
  public constructor(private readonly output: OutputStream) {}
  public writeByte(value: number): void { this.output.write(value); }
  public writeBoolean(value: boolean): void { this.writeByte(value ? 1 : 0); }
  public writeInt(value: number): void { for (let shift = 24; shift >= 0; shift -= 8) this.writeByte(value >>> shift); }
  public writeLong(value: bigint): void { const unsigned = BigInt.asUintN(64, value); for (let shift = 56n; shift >= 0n; shift -= 8n) this.writeByte(Number((unsigned >> shift) & 0xffn)); }
  public flush(): void { this.output.flush(); }
  public close(): void { this.output.close(); }
}
