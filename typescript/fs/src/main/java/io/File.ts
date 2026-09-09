import * as fs from "node:fs";
import * as pathModule from "node:path";
import { JavaString } from "@vitral/base";

/** Node-only implementation of the local-filesystem subset of java.io.File. */
export class File {
  private readonly path: JavaString;

  public constructor(path?: string | JavaString) {
    this.path = path instanceof JavaString ? new JavaString(path) : new JavaString(path ?? "");
  }

  public getName(): JavaString { return new JavaString(pathModule.basename(this.path.toCString())); }
  public getPath(): JavaString { return new JavaString(this.path); }
  public exists(): boolean { return this.path.toCString().length > 0 && fs.existsSync(this.path.toCString()); }
  public isDirectory(): boolean { return this.statOrUndefined()?.isDirectory() ?? false; }
  public isFile(): boolean { return this.statOrUndefined()?.isFile() ?? false; }
  public canRead(): boolean { return this.canAccess(fs.constants.R_OK); }
  public canWrite(): boolean { return this.canAccess(fs.constants.W_OK); }

  private statOrUndefined(): fs.Stats | undefined {
    try { return fs.statSync(this.path.toCString()); }
    catch { return undefined; }
  }

  private canAccess(mode: number): boolean {
    try { fs.accessSync(this.path.toCString(), mode); return true; }
    catch { return false; }
  }
}
