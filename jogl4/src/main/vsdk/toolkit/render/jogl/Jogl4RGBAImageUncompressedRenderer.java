package vsdk.toolkit.render.jogl;

import java.util.IdentityHashMap;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.media.RGBAImageUncompressed;

public class Jogl4RGBAImageUncompressedRenderer extends Jogl4Renderer {
    private static final IdentityHashMap<RGBAImageUncompressed, Integer> COMPILED_IMAGES =
        new IdentityHashMap<>();

    public static int activate(GL4 gl, RGBAImageUncompressed img)
    {
        if ( img == null ) {
            return -1;
        }

        Integer textureId = COMPILED_IMAGES.get(img);
        if ( textureId == null ) {
            textureId = upload(gl, img);
            COMPILED_IMAGES.put(img, textureId);
        }

        gl.glActiveTexture(GL4.GL_TEXTURE0);
        gl.glBindTexture(GL4.GL_TEXTURE_2D, textureId);
        return textureId;
    }

    public static void deactivate(GL4 gl, RGBAImageUncompressed img)
    {
        if ( img != null && COMPILED_IMAGES.containsKey(img) ) {
            gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);
        }
    }

    public static void unload(GL4 gl, RGBAImageUncompressed img)
    {
        Integer textureId = COMPILED_IMAGES.remove(img);
        if ( textureId == null ) {
            return;
        }

        int[] tmp = new int[] { textureId };
        gl.glDeleteTextures(1, tmp, 0);
    }

    public static void draw(GL4 gl, RGBAImageUncompressed img)
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

    private static int upload(GL4 gl, RGBAImageUncompressed img)
    {
        int[] tmp = new int[1];
        gl.glGenTextures(1, tmp, 0);
        int textureId = tmp[0];

        gl.glActiveTexture(GL4.GL_TEXTURE0);
        gl.glBindTexture(GL4.GL_TEXTURE_2D, textureId);

        gl.glPixelStorei(GL4.GL_UNPACK_ALIGNMENT, 1);
        gl.glTexImage2D(
            GL4.GL_TEXTURE_2D,
            0,
            GL4.GL_RGBA8,
            img.getXSize(),
            img.getYSize(),
            0,
            GL4.GL_RGBA,
            GL4.GL_UNSIGNED_BYTE,
            img.getRawImageDirectBuffer());
        gl.glGenerateMipmap(GL4.GL_TEXTURE_2D);

        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_LINEAR);
        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_LINEAR_MIPMAP_LINEAR);
        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_S, GL4.GL_REPEAT);
        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_T, GL4.GL_REPEAT);

        gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);

        return textureId;
    }
}
