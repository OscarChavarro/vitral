import { VSDK } from "../common/VSDK.js";
import { Logger } from "../common/logging/Logger.js";
import { ProcessingElement } from "./ProcessingElement.js";

/**
Binding for the `spharmonickit` native library (the CMake project
`pkgs/SpharmonicKit27`), which computes spherical harmonics.

Java loads the C library through JNI. This edition has no native libraries
and does not port that one (about 25,000 lines of C), so the load attempt
always fails. The class follows Java's own path for a missing library: the
first call reports the error once through `Logger`, and every call returns
false.
*/
export class SpharmonicKitWrapper extends ProcessingElement {
    public static readonly LIBRARY_NAME = "spharmonickit";
    public static readonly PACKAGE_NAME = "SpharmonicKit27";

    public static loaded = false;
    public static loadAttempted = false;

    /** The `NativeLibraryLoader.load` diagnostic of this runtime. */
    private static readonly LOAD_DIAGNOSTIC = "native libraries are not available in the JavaScript runtime";

    public static calculateSphericalHarmonics(
        _inImage: Int8Array,
        _outSphericalHarmonicsR: Float64Array | number[],
        _outSphericalHarmonicsI: Float64Array | number[],
    ): boolean {
        if (!SpharmonicKitWrapper.loaded && !SpharmonicKitWrapper.loadAttempted) {
            SpharmonicKitWrapper.loadAttempted = true;

            const diagnostic: (string | null)[] = [SpharmonicKitWrapper.LOAD_DIAGNOSTIC];

            SpharmonicKitWrapper.loaded = false;

            if (!SpharmonicKitWrapper.loaded) {
                Logger.reportMessage(
                    null,
                    VSDK.ERROR,
                    "SpharmonicKitWrapper.calculateSphericalHarmonicLenghts",
                    "Native library spharmonickit not available" +
                        (diagnostic[0] !== null ? " (" + diagnostic[0] + ")" : "") +
                        ".\n" +
                        "Build it with the CMake project in pkgs/SpharmonicKit27.\n" +
                        "Further error reporting in this issue disabled.",
                );
                return false;
            }
        }
        // Java calls the native executeSphericalHarmonics here when loaded;
        // it never is in this runtime.
        return false;
    }
}
