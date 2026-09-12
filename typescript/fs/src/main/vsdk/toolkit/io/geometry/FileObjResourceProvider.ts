/*
Deep module specifiers rather than the `@vitral/base` barrel: this module is on
the import path of the raytracing worker thread, and the barrel makes every
worker compile the whole library (measured: 19.7 s versus 1.3 s to boot 72
workers on a 72-core host).
*/
import type { ObjResourceProvider } from "@vitral/base/vsdk/toolkit/io/geometry/ObjResourceProvider";
import type { Reader } from "@vitral/base/java/io/Reader";
import { File } from "../../../../java/io/File.js";
import { FileReader } from "../../../../java/io/FileReader.js";

/**
The Node half of {@link ObjResourceProvider}: the two `java.io` operations
`vsdk.toolkit.io.geometry.ReaderObj` performs in Java, `new FileReader(name)`
and `new File(name).getParentFile()`, unchanged.
*/
export class FileObjResourceProvider implements ObjResourceProvider {
    public openReader(resourceName: string): Reader {
        return new FileReader(resourceName);
    }

    public resolveSibling(resourceName: string, siblingName: string): string {
        const arc = new File(resourceName);
        const dirArc = arc.getParentFile();
        return String(dirArc) + File.separator + siblingName;
    }
}
