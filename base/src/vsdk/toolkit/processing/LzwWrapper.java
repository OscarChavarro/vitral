package vsdk.toolkit.processing;

import java.io.InputStream;
import java.io.OutputStream;

public class LzwWrapper
{
    static {
        System.loadLibrary("LZW");
    }

    public static native void decompress(InputStream in, OutputStream out) throws Exception;
    //public static native void compress(InputStream in, OutputStream out) throws Exception;
}
