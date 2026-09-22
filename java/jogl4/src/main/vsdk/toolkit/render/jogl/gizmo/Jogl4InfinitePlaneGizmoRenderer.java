package vsdk.toolkit.render.jogl.gizmo;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.gui.gizmo.GizmoVertexArrayBuilder;
import vsdk.toolkit.gui.gizmo.InfinitePlaneGizmo;
import vsdk.toolkit.render.jogl.Jogl4CameraRenderer;
import vsdk.toolkit.render.jogl.Jogl4LineRenderer;
import vsdk.toolkit.render.jogl.Jogl4Renderer;

/**
Renders an {@link InfinitePlaneGizmo} as a screen-facing square frame, with the
GL4 core pipeline.

The frame geometry (its 4 corners, sized and oriented for the camera) is
computed by the gizmo itself (see
{@link InfinitePlaneGizmo#buildFrameCorners(Camera)}); this class only packs
that geometry into the flat vertex arrays {@link Jogl4LineRenderer} consumes.

Usage (render thread, once per frame):
<pre>
    Jogl4InfinitePlaneGizmoRenderer.draw(gl, gizmo, camera);
</pre>
*/
public class Jogl4InfinitePlaneGizmoRenderer extends Jogl4Renderer {
    private static final float LINE_WIDTH = 2.0f;

    /**
    Draws the gizmo over the current contents of the surface.

    @param gl OpenGL context
    @param gizmo gizmo to draw
    @param camera camera that views the gizmo
    */
    public static void draw(GL4 gl, InfinitePlaneGizmo gizmo, Camera camera) {
        if ( gl == null || gizmo == null || camera == null || !gizmo.isVisible() ) {
            return;
        }

        Vector3Dd[] corners = gizmo.buildFrameCorners(camera);
        if ( corners == null ) {
            return;
        }

        ColorRgb color = gizmo.getFrameColor();
        if ( color == null ) {
            color = InfinitePlaneGizmo.DEFAULT_FRAME_COLOR;
        }

        float[] positions = new float[24];
        float[] colors = new float[24];
        addLine(positions, colors, 0, corners[0], corners[1], color);
        addLine(positions, colors, 2, corners[1], corners[2], color);
        addLine(positions, colors, 4, corners[2], corners[3], color);
        addLine(positions, colors, 6, corners[3], corners[0], color);

        Matrix4x4d projection = Jogl4CameraRenderer.activate(gl, camera);
        Jogl4LineRenderer.drawLines(gl, projection, positions, colors, LINE_WIDTH);
    }

    /**
    Deletes the OpenGL resources of this renderer.
    PRE: the OpenGL context that created them is current.
    @param gl OpenGL context
    */
    public static void dispose(GL4 gl) {
        Jogl4LineRenderer.release(gl);
    }

    private static void addLine(
        float[] positions,
        float[] colors,
        int vertexOffset,
        Vector3Dd a,
        Vector3Dd b,
        ColorRgb color)
    {
        GizmoVertexArrayBuilder.putVertex(positions, vertexOffset, a);
        GizmoVertexArrayBuilder.putRgb(colors, vertexOffset, color);
        GizmoVertexArrayBuilder.putVertex(positions, vertexOffset + 1, b);
        GizmoVertexArrayBuilder.putRgb(colors, vertexOffset + 1, color);
    }
}
