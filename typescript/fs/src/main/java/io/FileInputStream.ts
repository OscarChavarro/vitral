/*
Deep module specifiers rather than the `@vitral/base` barrel: this module is on
the import path of the raytracing worker thread, and the barrel makes every
worker compile the whole library (measured: 19.7 s versus 1.3 s to boot 72
workers on a 72-core host).
*/
import * as fs from "node:fs";
import { InputStream } from "@vitral/base/java/io/InputStream";
import { File } from "./File.js";

/** Node-only synchronous local-file input stream. */
export class FileInputStream extends InputStream {
    private descriptor: number | undefined;

    public constructor(file: string | File) {
        super();
        const path = typeof file === "string" ? file : file.getPath().toCString();
        this.descriptor = fs.openSync(path, "r");
    }

    public override read(): number {
        const byte = new Uint8Array(1);
        return fs.readSync(this.requireOpen(), byte, 0, 1, null) === 0 ? -1 : (byte[0] ?? -1);
    }

    public override readBytes(buffer: Uint8Array, offset = 0, length = buffer.length - offset): number {
        InputStream.requireRange(buffer, offset, length);
        if (length === 0) return 0;
        const count = fs.readSync(this.requireOpen(), buffer, offset, length, null);
        return count === 0 ? -1 : count;
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
