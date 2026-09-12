/*
Deep module specifiers rather than the `@vitral/base` barrel: this module is on
the import path of the raytracing worker thread, and the barrel makes every
worker compile the whole library (measured: 19.7 s versus 1.3 s to boot 72
workers on a 72-core host).
*/
import { ReaderObj as ReaderObjParser } from "@vitral/base/vsdk/toolkit/io/geometry/ReaderObj";
import type { SimpleScene } from "@vitral/base/vsdk/toolkit/environment/scene/SimpleScene";
import { File } from "../../../../java/io/File.js";
import { FileObjResourceProvider } from "./FileObjResourceProvider.js";

/**
The `java.io.File` entry point of `vsdk.toolkit.io.geometry.ReaderObj`.

The reader itself is one class in Java and one class here, in `@vitral/base`
under its Java package; what stays in `@vitral/fs` is the signature that names
its resource with a `File` and the {@link FileObjResourceProvider} that opens
it. See `ObjResourceProvider` for why the parser was given that seam.
*/
export class ReaderObj {
    private static readonly provider = new FileObjResourceProvider();

    public static importEnvironment(inSceneFileFd: File, inoutSimpleScene: SimpleScene): void {
        ReaderObjParser.importEnvironment(
            inSceneFileFd.getAbsolutePath().toCString(),
            inoutSimpleScene,
            ReaderObj.provider,
        );
    }
}
