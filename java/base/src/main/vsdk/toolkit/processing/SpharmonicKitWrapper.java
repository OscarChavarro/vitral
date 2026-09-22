package vsdk.toolkit.processing;

import vsdk.toolkit.common.NativeLibraryLoader;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.logging.Logger;

public class SpharmonicKitWrapper extends ProcessingElement {

    public static final String LIBRARY_NAME = "spharmonickit";
    public static final String PACKAGE_NAME = "SpharmonicKit27";

    static boolean loaded = false;
    static boolean loadAttempted = false;

    private static native boolean
    executeSphericalHarmonics(
        byte inImage[],
        double outSphericalHarmonicsR[],
        double outSphericalHarmonicsI[]);

    public static boolean
    calculateSphericalHarmonics(
        byte inImage[],
        double outSphericalHarmonicsR[],
        double outSphericalHarmonicsI[])
    {
        if ( !loaded && !loadAttempted ) {
            loadAttempted = true;

            String diagnostic[] = new String[1];

            loaded = NativeLibraryLoader.load(LIBRARY_NAME, PACKAGE_NAME,
                                              diagnostic) != null;

            if ( !loaded ) {
                Logger.reportMessage(null, VSDK.ERROR,
                  "SpharmonicKitWrapper.calculateSphericalHarmonicLenghts",
"Native library spharmonickit not available" +
(diagnostic[0] != null ? " (" + diagnostic[0] + ")" : "") + ".\n" +
"Build it with the CMake project in pkgs/SpharmonicKit27.\n" +
"Further error reporting in this issue disabled.");
                return false;
            }
        }
        if ( loaded == true ) {
            return executeSphericalHarmonics(inImage,
                outSphericalHarmonicsR, outSphericalHarmonicsI);
        }
        else {
            return false;
        }
    }

}
