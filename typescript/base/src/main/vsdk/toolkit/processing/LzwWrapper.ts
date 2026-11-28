import type { InputStream } from "../../../java/io/InputStream.js";
import type { OutputStream } from "../../../java/io/OutputStream.js";
import { ProcessingElement } from "./ProcessingElement.js";
import { decompress as nativeDecompress, initLZW } from "./_LzwNativeLibrary.js";

/**
Binding for the `LZW` library (the CMake project `pkgs/LempelZivWelch`), used
to decompress LZW streams.

Java loads the C library through JNI and leaves `available` false when it
cannot be found. This edition has no native libraries, so the library is the
literal TypeScript port in `_LzwNativeLibrary.ts`, which is always present:
`available` is true and `getLoadDiagnostic()` is null, as in a Java runtime
that loaded the library.
*/
export class LzwWrapper extends ProcessingElement {
    public static readonly LIBRARY_NAME = "LZW";
    public static readonly PACKAGE_NAME = "LempelZivWelch";

    /** True if the library was loaded and its methods can be called. */
    public static readonly available: boolean = true;

    private static loadDiagnostic: string | null = null;

    /**
    Returns the reason why the library is not in use, or null if no loading
    attempt failed.
    */
    public static getLoadDiagnostic(): string | null {
        return LzwWrapper.loadDiagnostic;
    }

    /** `Java_vsdk_toolkit_processing_LzwWrapper_decompress` */
    public static decompress(input: InputStream, output: OutputStream): void {
        initLZW(-1);
        nativeDecompress(input, output);
    }

    /** `Java_vsdk_toolkit_processing_LzwWrapper_decompressWithSize` */
    public static decompressWithSize(input: InputStream, output: OutputStream, size: number): void {
        initLZW(size);
        nativeDecompress(input, output);
    }
}
