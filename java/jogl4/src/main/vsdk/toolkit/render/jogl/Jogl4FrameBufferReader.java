package vsdk.toolkit.render.jogl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;

import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.ZBuffer;

/**
Reads back the contents of the current frame buffer (the area of the current
OpenGL viewport) into VSDK images.
*/
public final class Jogl4FrameBufferReader extends Jogl4Renderer {
    private Jogl4FrameBufferReader() {
    }

    /**
    @param gl OpenGL context
    @return the colors of the area of the current viewport, with the first
    row at the top
    */
    public static RGBImageUncompressed readColor(GL4 gl)
    {
        int[] view = new int[4];

        gl.glGetIntegerv(GL.GL_VIEWPORT, view, 0);
        int width = view[2];
        int height = view[3];
        ByteBuffer bb = ByteBuffer.allocateDirect(3 * width * height);

        gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, 1);
        gl.glReadPixels(view[0], view[1], width, height, GL.GL_RGB, GL.GL_UNSIGNED_BYTE, bb);
        gl.glFinish();

        RGBImageUncompressed image = new RGBImageUncompressed();
        image.init(width, height);

        int pos = 0;
        for ( int y = height - 1; y >= 0; y-- ) {
            for ( int x = 0; x < width; x++ ) {
                image.putPixel(x, y, bb.get(pos), bb.get(pos + 1), bb.get(pos + 2));
                pos += 3;
            }
        }
        return image;
    }

    /**
    @param gl OpenGL context
    @return the depths of the area of the current viewport, with the first
    row at the top
    */
    public static ZBuffer readDepth(GL4 gl)
    {
        int[] view = new int[4];

        gl.glGetIntegerv(GL.GL_VIEWPORT, view, 0);
        int width = view[2];
        int height = view[3];
        FloatBuffer bb = FloatBuffer.allocate(width * height);

        gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, 1);
        gl.glReadPixels(view[0], view[1], width, height, GL4.GL_DEPTH_COMPONENT, GL.GL_FLOAT, bb);
        gl.glFinish();

        ZBuffer result = new ZBuffer(width, height);

        int pos = 0;
        for ( int y = height - 1; y >= 0; y-- ) {
            for ( int x = 0; x < width; x++ ) {
                result.setZ(x, y, bb.get(pos));
                pos++;
            }
        }
        return result;
    }
}
