package vsdk.toolkit.io.image;

import java.nio.ByteBuffer;

public class NativeImageReaderWrapper
{
    static {
        System.loadLibrary("NativeImageReader");
    }

    public static native void readPngHeader(_NativeImageReaderWrapperHeaderInfo header, String filename) throws Exception;
    public static native void readPngDataRGB(_NativeImageReaderWrapperHeaderInfo header, ByteBuffer arr) throws Exception;
}
