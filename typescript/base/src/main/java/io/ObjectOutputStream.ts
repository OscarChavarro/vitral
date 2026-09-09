import { OutputStream } from "./OutputStream.js";
/** Explicit JSON wire codec for structured-clone/portable values; cyclic graphs are rejected. */
export class ObjectOutputStream {
  public constructor(private readonly output: OutputStream) {}
  public writeObject(value: unknown): void { const text = JSON.stringify(value, ObjectOutputStream.replacer); if (text === undefined) throw new TypeError("Value is not serializable"); const bytes = new TextEncoder().encode(text); this.output.writeBytes(bytes); }
  public flush(): void { this.output.flush(); } public close(): void { this.output.close(); }
  private static replacer(_key: string, value: unknown): unknown { return typeof value === "bigint" ? { $vitralBigInt: value.toString() } : value; }
}
