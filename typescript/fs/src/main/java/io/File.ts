/*
Deep module specifiers rather than the `@vitral/base` barrel: this module is on
the import path of the raytracing worker thread, and the barrel makes every
worker compile the whole library (measured: 19.7 s versus 1.3 s to boot 72
workers on a 72-core host).
*/
import * as fs from "node:fs";
import * as pathModule from "node:path";
import { String as JavaString } from "@vitral/base/java/lang/String";

/** Node-only implementation of the local-filesystem subset of java.io.File. */
export class File {
    /** Java's `File.separator`: the local name-separator character. */
    public static readonly separator: string = pathModule.sep;
    /** Java's `File.pathSeparator`: the local path-list separator character. */
    public static readonly pathSeparator: string = pathModule.delimiter;

    private readonly path: JavaString;

    public constructor(path?: string | JavaString) {
        this.path = path instanceof JavaString ? new JavaString(path) : new JavaString(path ?? "");
    }

    public getName(): JavaString {
        return new JavaString(pathModule.basename(this.path.toCString()));
    }
    public getPath(): JavaString {
        return new JavaString(this.path);
    }
    public getAbsolutePath(): JavaString {
        return new JavaString(pathModule.resolve(this.path.toCString()));
    }
    public getAbsoluteFile(): File {
        return new File(this.getAbsolutePath());
    }
    /**
    Java returns null when the pathname names no parent directory. Node's
    `dirname` answers the path itself at the filesystem root and "." for a
    bare name, so both of those are mapped back to Java's null.
    */
    public getParentFile(): File | null {
        const here = this.path.toCString();
        const parent = pathModule.dirname(here);
        if (parent === here || (parent === "." && !here.startsWith("."))) {
            return null;
        }
        return new File(parent);
    }
    public toString(): string {
        return this.path.toCString();
    }
    public exists(): boolean {
        return this.path.toCString().length > 0 && fs.existsSync(this.path.toCString());
    }
    public isDirectory(): boolean {
        return this.statOrUndefined()?.isDirectory() ?? false;
    }
    public isFile(): boolean {
        return this.statOrUndefined()?.isFile() ?? false;
    }
    public canRead(): boolean {
        return this.canAccess(fs.constants.R_OK);
    }
    public canWrite(): boolean {
        return this.canAccess(fs.constants.W_OK);
    }

    private statOrUndefined(): fs.Stats | undefined {
        try {
            return fs.statSync(this.path.toCString());
        } catch {
            return undefined;
        }
    }

    private canAccess(mode: number): boolean {
        try {
            fs.accessSync(this.path.toCString(), mode);
            return true;
        } catch {
            return false;
        }
    }
}
