package vsdk.toolkit.media;

/**
HDR version of {@link RGBAPixel} with 16-bit unsigned channel storage.
The fields are intentionally public to keep the structure aligned with the
C++ port and to avoid extra indirection in pixel-heavy code.
*/
public final class RGBAPixelHDR
{
    public char r;
    public char g;
    public char b;
    public char a;
}
