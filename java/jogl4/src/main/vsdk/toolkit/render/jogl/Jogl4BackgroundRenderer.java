package vsdk.toolkit.render.jogl;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.background.Background;
import vsdk.toolkit.environment.background.SimpleBackground;

/**
Draws the background of a scene with the GL4 core pipeline. Only the
`SimpleBackground` (a solid color) is supported for now.
*/
public final class Jogl4BackgroundRenderer extends Jogl4Renderer {
    private Jogl4BackgroundRenderer() {
    }

    /**
    Fills the area of the current viewport with the background, without
    touching the depth buffer.

    @param gl OpenGL context
    @param background background to draw
    */
    public static void draw(GL4 gl, Background background)
    {
        if ( !(background instanceof SimpleBackground) ) {
            return;
        }
        ColorRgb c = ((SimpleBackground)background).colorInDireccion(new Vector3Dd(1, 0, 0));
        float[] positions = new float[] {
            -1, -1, 0,  1, -1, 0,  -1, 1, 0,  1, 1, 0
        };
        float[] colors = new float[16];

        for ( int i = 0; i < 4; i++ ) {
            colors[4*i] = (float)c.r();
            colors[4*i + 1] = (float)c.g();
            colors[4*i + 2] = (float)c.b();
            colors[4*i + 3] = 1.0f;
        }
        gl.glDisable(GL4.GL_DEPTH_TEST);
        gl.glDisable(GL4.GL_BLEND);
        gl.glDisable(GL4.GL_CULL_FACE);
        gl.glPolygonMode(GL4.GL_FRONT_AND_BACK, GL4.GL_FILL);
        Jogl4ColoredPrimitiveRenderer.draw(gl, Matrix4x4d.identityMatrix(),
            GL4.GL_TRIANGLE_STRIP, positions, colors);
        gl.glEnable(GL4.GL_DEPTH_TEST);
    }
}
