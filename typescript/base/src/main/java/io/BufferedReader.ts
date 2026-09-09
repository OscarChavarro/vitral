import { Reader } from "./Reader.js";
export class BufferedReader extends Reader {
  public constructor(private readonly reader: Reader) { super(); }
  public override read(): number { return this.reader.read(); }
  public readLine(): string | undefined {
    let result = "";
    while (true) { const value = this.read(); if (value < 0) return result.length === 0 ? undefined : result; if (value === 10) return result; if (value !== 13) result += globalThis.String.fromCharCode(value); }
  }
  public override close(): void { this.reader.close(); }
}
