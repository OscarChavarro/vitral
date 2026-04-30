package vsdk.toolkit.render.jogl;

import java.util.ArrayList;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.media.RGBAImageCompressed;

public class Jogl2RGBAImageCompressedRenderer extends Jogl2Renderer
{
    private static ArrayList<_JoglRGBAImageCompressedRendererAssociation> compiledImages =
        new ArrayList<_JoglRGBAImageCompressedRendererAssociation>();

    public static int activate(GL2 gl, RGBAImageCompressed img)
    {
        if ( img == null ) {
            return -1;
        }

        _JoglRGBAImageCompressedRendererAssociation item;
        item = findCompiledImage(img);

        if ( item == null ) {
            item = upload(gl, img);
            if ( item == null ) {
                return -1;
            }
            compiledImages.add(item);
        }

        gl.glBindTexture(GL.GL_TEXTURE_2D, item.glList);
        return item.glList;
    }

    public static void deactivate(GL2 gl, RGBAImageCompressed img)
    {
        if ( img != null && findCompiledImage(img) != null ) {
            gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
        }
    }

    public static void unload(GL2 gl, RGBAImageCompressed img)
    {
        int i;
        for ( i = 0; i < compiledImages.size(); i++ ) {
            _JoglRGBAImageCompressedRendererAssociation item;
            item = compiledImages.get(i);
            if ( item.image == img ) {
                int[] tmp;
                tmp = new int[] { item.glList };
                gl.glDeleteTextures(1, tmp, 0);
                compiledImages.remove(i);
                return;
            }
        }
    }

    public static void draw(GL2 gl, RGBAImageCompressed img)
    {
        int textureId = activate(gl, img);
        if ( textureId <= 0 ) {
            return;
        }

        gl.glDisable(GL.GL_DEPTH_TEST);
        gl.glDisable(GL.GL_CULL_FACE);
        gl.glEnable(GL.GL_TEXTURE_2D);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        drawLowerLeftOverlay(gl, img.getXSize(), img.getYSize());

        gl.glDisable(GL.GL_BLEND);
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glEnable(GL.GL_DEPTH_TEST);
    }

    private static _JoglRGBAImageCompressedRendererAssociation upload(
        GL2 gl,
        RGBAImageCompressed img)
    {
        int internalFormat = toOpenGlInternalFormat(img.getCompressionFormat());
        if ( internalFormat == 0 ) {
            VSDK.reportMessage(
                null,
                VSDK.ERROR,
                "Jogl2RGBAImageCompressedRenderer.upload",
                "Unsupported compressed texture format.");
            return null;
        }

        int imageSize = img.getCompressedDataSize();
        if ( imageSize <= 0 ) {
            VSDK.reportMessage(
                null,
                VSDK.ERROR,
                "Jogl2RGBAImageCompressedRenderer.upload",
                "Invalid compressed texture size.");
            return null;
        }

        int[] tmp;
        tmp = new int[1];
        gl.glGenTextures(1, tmp, 0);

        _JoglRGBAImageCompressedRendererAssociation item;
        item = new _JoglRGBAImageCompressedRendererAssociation();
        item.image = img;
        item.glList = tmp[0];

        gl.glBindTexture(GL.GL_TEXTURE_2D, item.glList);
        gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
        gl.glCompressedTexImage2D(
            GL.GL_TEXTURE_2D,
            0,
            internalFormat,
            img.getXSize(),
            img.getYSize(),
            0,
            imageSize,
            img.getRawImageDirectBuffer());

        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);

        gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
        return item;
    }

    private static _JoglRGBAImageCompressedRendererAssociation findCompiledImage(
        RGBAImageCompressed img)
    {
        int i;
        for ( i = 0; i < compiledImages.size(); i++ ) {
            _JoglRGBAImageCompressedRendererAssociation item;
            item = compiledImages.get(i);
            if ( item.image == img ) {
                return item;
            }
        }
        return null;
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

    private static void drawLowerLeftOverlay(GL2 gl, int width, int height)
    {
        int[] viewport;
        viewport = new int[4];
        gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);

        float viewportWidth = Math.max(viewport[2], 1);
        float viewportHeight = Math.max(viewport[3], 1);
        float w = 2.0f * ((float)width / viewportWidth);
        float h = 2.0f * ((float)height / viewportHeight);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glBegin(GL2.GL_QUADS);
            gl.glColor3d(1, 1, 1);
            gl.glTexCoord2d(0, 0);
            gl.glVertex3d(-1.0, -1.0, 0.0);
            gl.glTexCoord2d(1, 0);
            gl.glVertex3d(-1.0 + w, -1.0, 0.0);
            gl.glTexCoord2d(1, 1);
            gl.glVertex3d(-1.0 + w, -1.0 + h, 0.0);
            gl.glTexCoord2d(0, 1);
            gl.glVertex3d(-1.0, -1.0 + h, 0.0);
        gl.glEnd();
    }
}
