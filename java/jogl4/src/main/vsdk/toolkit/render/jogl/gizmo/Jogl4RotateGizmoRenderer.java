package vsdk.toolkit.render.jogl.gizmo;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.gui.gizmo.RotateGizmo;
import vsdk.toolkit.render.jogl.Jogl4ColoredPrimitiveRenderer;
import vsdk.toolkit.render.jogl.Jogl4Renderer;

/**
Renders a {@link RotateGizmo} with the GL4 core pipeline (see
{@link Jogl4ColoredPrimitiveRenderer}): each of its rings is a strip of quads
facing the camera, generated in world space by the gizmo (see
{@link RotateGizmo#buildRingStrip(int)}) with the width in pixels of the ring,
and drawn with the color the gizmo gives it (the one of its axis, or yellow if
the ring is under the cursor or was chosen).

While a ring is being dragged, the arc the rotation has swept is drawn over
them as a translucent sector with the color of the axis of the ring (never
yellow, see {@link RotateGizmo#buildArcFan()}).

Usage (render thread, once per frame, after the transformation matrix of the
gizmo has been set):
<pre>
    Jogl4RotateGizmoRenderer.draw(gl, gizmo, camera);
</pre>
*/
public class Jogl4RotateGizmoRenderer extends Jogl4Renderer {
    private static final float ARC_OPACITY = 0.35f;

    /**
    Draws the gizmo over the current contents of the surface.

    @param gl OpenGL context
    @param gizmo gizmo to draw; its transformation matrix must be set
    @param camera camera that views the gizmo
    */
    public static void draw(GL4 gl, RotateGizmo gizmo, Camera camera)
    {
        if ( gl == null || gizmo == null || camera == null ) {
            return;
        }
        Matrix4x4d mvp = camera.calculateProjectionMatrix();

        gl.glDisable(GL4.GL_CULL_FACE);
        gl.glEnable(GL4.GL_DEPTH_TEST);
        gl.glDepthMask(true);
        gl.glPolygonMode(GL4.GL_FRONT_AND_BACK, GL4.GL_FILL);

        for ( int ring = 0; ring < RotateGizmo.RING_COUNT; ring++ ) {
            drawRing(gl, mvp, gizmo.buildRingStrip(ring), gizmo.getRingColor(ring));
        }

        //- Translucent arc, over the rings -------------------------------
        Vector3Dd[] fan = gizmo.buildArcFan();

        if ( fan.length >= 3 ) {
            gl.glEnable(GL4.GL_BLEND);
            gl.glBlendFunc(GL4.GL_SRC_ALPHA, GL4.GL_ONE_MINUS_SRC_ALPHA);
            gl.glDepthMask(false);
            drawArc(gl, mvp, fan, gizmo.getArcColor());
            gl.glDepthMask(true);
            gl.glDisable(GL4.GL_BLEND);
        }
    }

    private static void drawArc(GL4 gl, Matrix4x4d mvp, Vector3Dd[] fan, ColorRgb c)
    {
        float[] positions = new float[fan.length * 3];
        float[] colors = new float[fan.length * 4];

        for ( int i = 0; i < fan.length; i++ ) {
            positions[3*i] = (float)fan[i].x();
            positions[3*i + 1] = (float)fan[i].y();
            positions[3*i + 2] = (float)fan[i].z();
            colors[4*i] = (float)c.r();
            colors[4*i + 1] = (float)c.g();
            colors[4*i + 2] = (float)c.b();
            colors[4*i + 3] = ARC_OPACITY;
        }
        Jogl4ColoredPrimitiveRenderer.draw(gl, mvp, GL4.GL_TRIANGLE_FAN, positions, colors);
    }

    private static void drawRing(GL4 gl, Matrix4x4d mvp, Vector3Dd[] strip, ColorRgb c)
    {
        float[] positions = new float[strip.length * 3];
        float[] colors = new float[strip.length * 4];

        for ( int i = 0; i < strip.length; i++ ) {
            positions[3*i] = (float)strip[i].x();
            positions[3*i + 1] = (float)strip[i].y();
            positions[3*i + 2] = (float)strip[i].z();
            colors[4*i] = (float)c.r();
            colors[4*i + 1] = (float)c.g();
            colors[4*i + 2] = (float)c.b();
            colors[4*i + 3] = 1.0f;
        }
        Jogl4ColoredPrimitiveRenderer.draw(gl, mvp, GL4.GL_TRIANGLE_STRIP, positions, colors);
    }
}
