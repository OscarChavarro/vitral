import * as fs from "node:fs";
import { OutputStream } from "@vitral/base";
import { File } from "./File.js";

/** Node-only synchronous local-file output stream. */
export class FileOutputStream extends OutputStream {
    private descriptor: number | undefined;

    public constructor(file: string | File, append = false) {
        super();
        const path = typeof file === "string" ? file : file.getPath().toCString();
        this.descriptor = fs.openSync(path, append ? "a" : "w");
    }

    public override write(value: number): void {
        const byte = new Uint8Array([value & 0xff]);
        fs.writeSync(this.requireOpen(), byte, 0, 1, null);
    }

    public override writeBytes(buffer: Uint8Array, offset = 0, length = buffer.length - offset): void {
        OutputStream.requireRange(buffer, offset, length);
        if (length > 0) fs.writeSync(this.requireOpen(), buffer, offset, length, null);
    }

    public override flush(): void {
        fs.fsyncSync(this.requireOpen());
    }
    public override close(): void {
        if (this.descriptor === undefined) return;
        fs.closeSync(this.descriptor);
        this.descriptor = undefined;
    }

    private requireOpen(): number {
        if (this.descriptor === undefined) throw new Error("Stream closed");
        return this.descriptor;
    }
}
