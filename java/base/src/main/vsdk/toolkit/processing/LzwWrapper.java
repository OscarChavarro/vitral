package vsdk.toolkit.processing;

import java.io.InputStream;
import java.io.OutputStream;

import vsdk.toolkit.common.NativeLibraryLoader;

/**
Java binding for the `LZW` native library (the CMake project
`pkgs/LempelZivWelch`), used to decompress LZW streams.

The library is optional: if it can not be located or loaded, `available`
stays false instead of aborting the class initialization, so applications
not using LZW compressed data are not affected.
*/
public class LzwWrapper extends ProcessingElement
{
    public static final String LIBRARY_NAME = "LZW";
    public static final String PACKAGE_NAME = "LempelZivWelch";

    /**
    True if the native library was loaded and its methods can be called.
    */
    public static final boolean available;

    private static String loadDiagnostic = null;

    static {
        String diagnostic[] = new String[1];
        available = NativeLibraryLoader.load(LIBRARY_NAME, PACKAGE_NAME,
                                             diagnostic) != null;
        loadDiagnostic = diagnostic[0];
    }

    /**
    Returns the reason why the native library is not in use, or null if no
    loading attempt failed.
    */
    public static String getLoadDiagnostic()
    {
        return loadDiagnostic;
    }

    public static native void decompress(InputStream in, OutputStream out) throws Exception;
    public static native void decompressWithSize(InputStream in, OutputStream out, long size) throws Exception;
    //public static native void compress(InputStream in, OutputStream out) throws Exception;
}
