//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 14 2006 - Fabio Aroca / Eduardo Mendoza: Original base version  =
//= - March 14 2006 - Oscar Chavarro: quality check                         =
//===========================================================================

package vsdk.toolkit.render.jogl;

import java.nio.FloatBuffer;

// JOGL classes
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2GL3;

import vsdk.toolkit.media.ZBuffer;

public class JoglZBufferRenderer extends JoglRenderer 
{
    public static byte[] importZBuffer(GL2 gl) {
        int[] view = new int[4];
        gl.glGetIntegerv(GL.GL_VIEWPORT, view, 0);
        int width = view[2], height = view[3];
        byte[] data = new byte[4 * width * height];
        FloatBuffer bb = FloatBuffer.allocate(width * height);

        gl.glReadPixels( -1, -1, width, height, GL2ES2.GL_DEPTH_COMPONENT,
                        GL.GL_FLOAT, bb);
        gl.glFlush();

        int pos = 0;

        for (int i = 0; i < width * height; i++) {
            long entero = (long) (bb.get(i) * ((1L << 32L) - 1));
            data[pos]     = (byte) ((0x00000000FF000000L & entero) >> 24);
            data[pos + 1] = (byte) ((0x0000000000FF0000L & entero) >> 16);
            data[pos + 2] = (byte) ((0x000000000000FF00L & entero) >> 8);
            data[pos + 3] = (byte) ((0x00000000000000FFL & entero));
            pos = pos + 4;
        }

        return data;
    }

    public static ZBuffer importJOGLZBuffer(GL2 gl) {
        int[] view = new int[4];
        gl.glGetIntegerv(GL.GL_VIEWPORT, view, 0);
        int width = view[2], height = view[3];
        FloatBuffer bb = FloatBuffer.allocate(width * height);

        gl.glReadBuffer(GL2GL3.GL_FRONT_LEFT);
        gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, 1);
        gl.glReadPixels( -1, -1, width, height, GL2ES2.GL_DEPTH_COMPONENT,
                        GL.GL_FLOAT, bb);
        gl.glFlush();

        ZBuffer result = new ZBuffer(width, height);

        int pos = 0;
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                float f = bb.get(pos);
                result.setZ(x, y, f);
                pos += 1;
            }
        }        

        return result;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
