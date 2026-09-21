package vsdk.toolkit.render.jogl.gizmo;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.gui.gizmo.ScaleGizmo;
import vsdk.toolkit.render.jogl.Jogl4LineRenderer;
import vsdk.toolkit.render.jogl.Jogl4MatrixRenderer;
import vsdk.toolkit.render.jogl.Jogl4Renderer;

/**
Renders a {@link ScaleGizmo} with the GL4 core pipeline: the axes of its
frame and a yellow cross.
*/
public class Jogl4ScaleGizmoRenderer extends Jogl4Renderer {
    /**
    Draws the gizmo.

    @param gl OpenGL context
    @param gizmo gizmo to draw
    @param position position of the gizmo in world space
    @param camera camera that views the gizmo
    */
    public static void draw(GL4 gl, ScaleGizmo gizmo, Vector3Dd position, Camera camera)
    {
        if ( gl == null || gizmo == null || camera == null ) {
            return;
        }
        Matrix4x4d frame = new Matrix4x4d(gizmo.getTransformationMatrix()).withTranslation(position);
        Matrix4x4d projection = camera.calculateProjectionMatrix();

        Jogl4MatrixRenderer.draw(gl, projection, frame);

        float[] positions = new float[] {
            -0.2f, -0.2f, 0f,  0.2f, 0.2f, 0f,
            -0.2f, 0.2f, 0f,  0.2f, -0.2f, 0f
        };
        float[] colors = new float[] {
            1f, 1f, 0f,  1f, 1f, 0f,
            1f, 1f, 0f,  1f, 1f, 0f
        };

        Jogl4LineRenderer.drawLines(gl, projection.multiply(frame), positions, colors, 1.0f);
    }
}
