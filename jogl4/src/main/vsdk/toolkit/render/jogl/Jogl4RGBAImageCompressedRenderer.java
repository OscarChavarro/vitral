package vsdk.toolkit.render.jogl;

import java.util.IdentityHashMap;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.VSDK;
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
        int internalFormat = toOpenGlInternalFormat(img.getCompressionFormat());
        if ( internalFormat == 0 ) {
            VSDK.reportMessage(
                null,
                VSDK.ERROR,
                "Jogl4RGBAImageCompressedRenderer.upload",
                "Unsupported compressed texture format.");
            return -1;
        }

        int imageSize = img.getCompressedDataSize();
        if ( imageSize <= 0 ) {
            VSDK.reportMessage(
                null,
                VSDK.ERROR,
                "Jogl4RGBAImageCompressedRenderer.upload",
                "Invalid compressed texture size.");
            return -1;
        }

        int[] tmp = new int[1];
        gl.glGenTextures(1, tmp, 0);
        int textureId = tmp[0];

        gl.glActiveTexture(GL4.GL_TEXTURE0);
        gl.glBindTexture(GL4.GL_TEXTURE_2D, textureId);

        gl.glPixelStorei(GL4.GL_UNPACK_ALIGNMENT, 1);
        gl.glCompressedTexImage2D(
            GL4.GL_TEXTURE_2D,
            0,
            internalFormat,
            img.getXSize(),
            img.getYSize(),
            0,
            imageSize,
            img.getRawImageDirectBuffer());

        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_LINEAR);
        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_LINEAR);
        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_S, GL4.GL_REPEAT);
        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_T, GL4.GL_REPEAT);

        gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);

        return textureId;
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
