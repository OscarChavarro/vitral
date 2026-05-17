package vsdk.toolkit.render.jogl;

import java.util.IdentityHashMap;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.logging.Logger;
import vsdk.toolkit.media.RGBAImageCompressed;

public class Jogl4RGBAImageCompressedRenderer extends Jogl4Renderer {
    private static final IdentityHashMap<RGBAImageCompressed, Integer> COMPILED_IMAGES =
        new IdentityHashMap<>();

    public static int activate(GL4 gl, RGBAImageCompressed img)
    {
        if ( img == null ) {
            return -1;
        }

        Integer textureId = COMPILED_IMAGES.get(img);
        if ( textureId == null ) {
            textureId = upload(gl, img);
            if ( textureId <= 0 ) {
                return -1;
            }
            COMPILED_IMAGES.put(img, textureId);
        }

        gl.glActiveTexture(GL4.GL_TEXTURE0);
        gl.glBindTexture(GL4.GL_TEXTURE_2D, textureId);
        return textureId;
    }

    public static void deactivate(GL4 gl, RGBAImageCompressed img)
    {
        if ( img != null && COMPILED_IMAGES.containsKey(img) ) {
            gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);
        }
    }

    public static void unload(GL4 gl, RGBAImageCompressed img)
    {
        Integer textureId = COMPILED_IMAGES.remove(img);
        if ( textureId == null ) {
            return;
        }

        int[] tmp = new int[] { textureId };
        gl.glDeleteTextures(1, tmp, 0);
    }

    public static void draw(GL4 gl, RGBAImageCompressed img)
    {
        int textureId = activate(gl, img);
        if ( textureId <= 0 ) {
            return;
        }

        gl.glDisable(GL4.GL_DEPTH_TEST);
        gl.glDisable(GL4.GL_CULL_FACE);
        gl.glEnable(GL4.GL_BLEND);
        gl.glBlendFunc(GL4.GL_SRC_ALPHA, GL4.GL_ONE_MINUS_SRC_ALPHA);

        Jogl4ImageRenderer.drawLowerLeftOverlay(gl, textureId, img.getXSize(), img.getYSize());

        gl.glDisable(GL4.GL_BLEND);
        gl.glEnable(GL4.GL_DEPTH_TEST);
    }

    private static int upload(GL4 gl, RGBAImageCompressed img)
    {
        int imageSize = img.getCompressedDataSize();
        if ( imageSize <= 0 ) {
            Logger.reportMessage(
                null,
                VSDK.ERROR,
                "Jogl4RGBAImageCompressedRenderer.upload",
                "Invalid compressed texture size.");
            return -1;
        }

        boolean s3tcAvailable =
            gl.isExtensionAvailable("GL_EXT_texture_compression_s3tc") ||
            gl.isExtensionAvailable("GL_ANGLE_texture_compression_dxt1") ||
            gl.isExtensionAvailable("GL_ANGLE_texture_compression_dxt3") ||
            gl.isExtensionAvailable("GL_ANGLE_texture_compression_dxt5");

        int internalFormat = toOpenGlInternalFormat(img.getCompressionFormat());

        int[] tmp = new int[1];
        gl.glGenTextures(1, tmp, 0);
        int textureId = tmp[0];

        gl.glActiveTexture(GL4.GL_TEXTURE0);
        gl.glBindTexture(GL4.GL_TEXTURE_2D, textureId);
        gl.glPixelStorei(GL4.GL_UNPACK_ALIGNMENT, 1);

        if ( s3tcAvailable && internalFormat != 0 ) {
            gl.glCompressedTexImage2D(
                GL4.GL_TEXTURE_2D,
                0,
                internalFormat,
                img.getXSize(),
                img.getYSize(),
                0,
                imageSize,
                img.getRawImageDirectBuffer());
            gl.glGenerateMipmap(GL4.GL_TEXTURE_2D);
        }
        else {
            // S3TC not available (e.g. macOS Core Profile) — decode in CPU
            Logger.reportMessage(
                null,
                VSDK.WARNING,
                "Jogl4RGBAImageCompressedRenderer.upload",
                "S3TC extension not available; decoding compressed texture in CPU.");
            byte[] rgba = decompressToRGBA(img);
            if ( rgba == null ) {
                gl.glDeleteTextures(1, tmp, 0);
                gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);
                return -1;
            }
            java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(rgba);
            gl.glTexImage2D(
                GL4.GL_TEXTURE_2D,
                0,
                GL4.GL_RGBA8,
                img.getXSize(),
                img.getYSize(),
                0,
                GL4.GL_RGBA,
                GL4.GL_UNSIGNED_BYTE,
                buf);
            gl.glGenerateMipmap(GL4.GL_TEXTURE_2D);
        }

        gl.glTexParameteri(
            GL4.GL_TEXTURE_2D,
            GL4.GL_TEXTURE_MAG_FILTER,
            Jogl4ImageRenderer.magFilterParam());
        gl.glTexParameteri(
            GL4.GL_TEXTURE_2D,
            GL4.GL_TEXTURE_MIN_FILTER,
            Jogl4ImageRenderer.minFilterParam());
        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_S, GL4.GL_REPEAT);
        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_T, GL4.GL_REPEAT);

        gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);

        return textureId;
    }

    /**
     * Decodes a DXT1/DXT3/DXT5 compressed image to a flat RGBA byte array
     * (top-left origin, row-major). Returns null on unsupported format.
     */
    private static byte[] decompressToRGBA(RGBAImageCompressed img)
    {
        int fmt = img.getCompressionFormat();
        if ( fmt != RGBAImageCompressed.COMPRESSION_DXT1 &&
             fmt != RGBAImageCompressed.COMPRESSION_DXT3 &&
             fmt != RGBAImageCompressed.COMPRESSION_DXT5 ) {
            Logger.reportMessage(
                null,
                VSDK.ERROR,
                "Jogl4RGBAImageCompressedRenderer.decompressToRGBA",
                "Cannot decompress format: " + fmt);
            return null;
        }

        int width = img.getXSize();
        int height = img.getYSize();
        byte[] src = img.getRawImage();
        byte[] rgba = new byte[width * height * 4];

        int blockSize = (fmt == RGBAImageCompressed.COMPRESSION_DXT1) ? 8 : 16;
        int blockCountX = Math.max(1, (width  + 3) / 4);
        int blockCountY = Math.max(1, (height + 3) / 4);
        int srcOffset = 0;

        for ( int by = 0; by < blockCountY; by++ ) {
            for ( int bx = 0; bx < blockCountX; bx++ ) {
                int alphaOffset = srcOffset;
                int colorOffset = (fmt == RGBAImageCompressed.COMPRESSION_DXT1)
                    ? srcOffset
                    : srcOffset + 8;

                // Decode the 4x4 colour block (DXT1 core)
                int c0 = readUShort(src, colorOffset);
                int c1 = readUShort(src, colorOffset + 2);
                int lookup = readInt(src, colorOffset + 4);

                int[] cr = new int[4];
                int[] cg = new int[4];
                int[] cb = new int[4];
                int[] ca = new int[4];
                decodeRgb565(c0, cr, cg, cb, 0);
                decodeRgb565(c1, cr, cg, cb, 1);
                ca[0] = 255; ca[1] = 255; ca[2] = 255; ca[3] = 255;

                if ( fmt == RGBAImageCompressed.COMPRESSION_DXT1 ) {
                    if ( c0 > c1 ) {
                        cr[2] = (2*cr[0] + cr[1]) / 3;
                        cg[2] = (2*cg[0] + cg[1]) / 3;
                        cb[2] = (2*cb[0] + cb[1]) / 3;
                        cr[3] = (cr[0] + 2*cr[1]) / 3;
                        cg[3] = (cg[0] + 2*cg[1]) / 3;
                        cb[3] = (cb[0] + 2*cb[1]) / 3;
                    }
                    else {
                        cr[2] = (cr[0] + cr[1]) / 2;
                        cg[2] = (cg[0] + cg[1]) / 2;
                        cb[2] = (cb[0] + cb[1]) / 2;
                        cr[3] = 0; cg[3] = 0; cb[3] = 0; ca[3] = 0;
                    }
                }
                else {
                    cr[2] = (2*cr[0] + cr[1]) / 3;
                    cg[2] = (2*cg[0] + cg[1]) / 3;
                    cb[2] = (2*cb[0] + cb[1]) / 3;
                    cr[3] = (cr[0] + 2*cr[1]) / 3;
                    cg[3] = (cg[0] + 2*cg[1]) / 3;
                    cb[3] = (cb[0] + 2*cb[1]) / 3;
                }

                for ( int py = 0; py < 4; py++ ) {
                    for ( int px = 0; px < 4; px++ ) {
                        int pixX = bx * 4 + px;
                        int pixY = by * 4 + py;
                        if ( pixX >= width || pixY >= height ) {
                            continue;
                        }
                        int idx = (lookup >> (2 * (py * 4 + px))) & 0x3;
                        int dstBase = (pixY * width + pixX) * 4;

                        rgba[dstBase + 0] = (byte)cr[idx];
                        rgba[dstBase + 1] = (byte)cg[idx];
                        rgba[dstBase + 2] = (byte)cb[idx];

                        if ( fmt == RGBAImageCompressed.COMPRESSION_DXT1 ) {
                            rgba[dstBase + 3] = (byte)ca[idx];
                        }
                        else if ( fmt == RGBAImageCompressed.COMPRESSION_DXT3 ) {
                            // 4-bit alpha per pixel packed in first 8 bytes
                            int alphaShift = (py * 4 + px) * 4;
                            int alphaVal = (src[alphaOffset + alphaShift / 8] >> (alphaShift % 8)) & 0xF;
                            rgba[dstBase + 3] = (byte)((alphaVal << 4) | alphaVal);
                        }
                        else {
                            // DXT5: interpolated alpha
                            int a0 = src[alphaOffset] & 0xFF;
                            int a1 = src[alphaOffset + 1] & 0xFF;
                            int[] aTable = buildAlphaTable(a0, a1);
                            long alphaBits = readAlphaBits(src, alphaOffset + 2);
                            int aIdx = (int)((alphaBits >> (3 * (py * 4 + px))) & 0x7);
                            rgba[dstBase + 3] = (byte)aTable[aIdx];
                        }
                    }
                }

                srcOffset += blockSize;
            }
        }

        return rgba;
    }

    private static int readUShort(byte[] data, int offset)
    {
        return ((data[offset] & 0xFF)) | ((data[offset + 1] & 0xFF) << 8);
    }

    private static int readInt(byte[] data, int offset)
    {
        return (data[offset] & 0xFF)
            | ((data[offset + 1] & 0xFF) << 8)
            | ((data[offset + 2] & 0xFF) << 16)
            | ((data[offset + 3] & 0xFF) << 24);
    }

    private static long readAlphaBits(byte[] data, int offset)
    {
        long v = 0;
        for ( int i = 0; i < 6; i++ ) {
            v |= ((long)(data[offset + i] & 0xFF)) << (8 * i);
        }
        return v;
    }

    private static void decodeRgb565(int packed, int[] r, int[] g, int[] b, int idx)
    {
        r[idx] = ((packed >> 11) & 0x1F) * 255 / 31;
        g[idx] = ((packed >>  5) & 0x3F) * 255 / 63;
        b[idx] = ( packed        & 0x1F) * 255 / 31;
    }

    private static int[] buildAlphaTable(int a0, int a1)
    {
        int[] t = new int[8];
        t[0] = a0;
        t[1] = a1;
        if ( a0 > a1 ) {
            t[2] = (6*a0 + 1*a1) / 7;
            t[3] = (5*a0 + 2*a1) / 7;
            t[4] = (4*a0 + 3*a1) / 7;
            t[5] = (3*a0 + 4*a1) / 7;
            t[6] = (2*a0 + 5*a1) / 7;
            t[7] = (1*a0 + 6*a1) / 7;
        }
        else {
            t[2] = (4*a0 + 1*a1) / 5;
            t[3] = (3*a0 + 2*a1) / 5;
            t[4] = (2*a0 + 3*a1) / 5;
            t[5] = (1*a0 + 4*a1) / 5;
            t[6] = 0;
            t[7] = 255;
        }
        return t;
    }

    private static int toOpenGlInternalFormat(int compressionFormat)
    {
        if ( compressionFormat == RGBAImageCompressed.COMPRESSION_DXT1 ) {
            return GL.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
        }
        if ( compressionFormat == RGBAImageCompressed.COMPRESSION_DXT3 ) {
            return GL.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
        }
        if ( compressionFormat == RGBAImageCompressed.COMPRESSION_DXT5 ) {
            return GL.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
        }
        return 0;
    }
}
