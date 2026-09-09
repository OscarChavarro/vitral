import { InputStream } from "./InputStream.js";
export class ObjectInputStream {
  public constructor(private readonly input: InputStream) {}
  public readObject(): unknown {
    const chunks: number[] = []; let value: number;
    while ((value = this.input.read()) >= 0) chunks.push(value);
    return JSON.parse(new TextDecoder().decode(Uint8Array.from(chunks)), (_key, current: unknown) => {
      if (current !== null && typeof current === "object" && "$vitralBigInt" in current) return BigInt((current as { $vitralBigInt: string }).$vitralBigInt);
      return current;
    });
  }
  public close(): void { this.input.close(); }
}
