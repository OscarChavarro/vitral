package vsdk.toolkit.render.jogl.gizmo;

import java.util.ArrayList;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.gui.gizmo.RotateGizmo;
import vsdk.toolkit.render.jogl.Jogl4LineRenderer;
import vsdk.toolkit.render.jogl.Jogl4MatrixRenderer;
import vsdk.toolkit.render.jogl.Jogl4Renderer;

/**
Renders a {@link RotateGizmo} with the GL4 core pipeline: the axes of its
frame and one circle around each axis.
*/
public class Jogl4RotateGizmoRenderer extends Jogl4Renderer {
    private static final double CIRCLE_RADIUS = 0.5;
    private static final double CIRCLE_STEP = Math.toRadians(15);
    private static final float AXES_LINE_WIDTH = 3.0f;

    /**
    Draws the gizmo.

    @param gl OpenGL context
    @param gizmo gizmo to draw
    @param position position of the gizmo in world space
    @param camera camera that views the gizmo
    */
    public static void draw(GL4 gl, RotateGizmo gizmo, Vector3Dd position, Camera camera)
    {
        if ( gl == null || gizmo == null || camera == null ) {
            return;
        }
        Matrix4x4d frame = new Matrix4x4d(gizmo.getTransformationMatrix()).withTranslation(position);
        Matrix4x4d mvp = camera.calculateProjectionMatrix().multiply(frame);

        // Axes of the frame
        Jogl4MatrixRenderer.draw(gl, camera.calculateProjectionMatrix(), frame);

        drawCircle(gl, mvp, new double[] {1, 0, 0}, 1, 0, 0, new Vector3Dd(0, CIRCLE_RADIUS, 0));
        drawCircle(gl, mvp, new double[] {0, 1, 0}, 0, 1, 0, new Vector3Dd(CIRCLE_RADIUS, 0, 0));
        drawCircle(gl, mvp, new double[] {0, 0, 1}, 0, 0, 1, new Vector3Dd(CIRCLE_RADIUS, 0, 0));
    }

    private static void drawCircle(
        GL4 gl,
        Matrix4x4d mvp,
        double[] color,
        double ax,
        double ay,
        double az,
        Vector3Dd needle)
    {
        ArrayList<Vector3Dd> points = new ArrayList<>();

        for ( double a = CIRCLE_STEP; a < Math.toRadians(360) - CIRCLE_STEP; a += CIRCLE_STEP ) {
            points.add(new Matrix4x4d().axisRotation(a, ax, ay, az).multiply(needle));
        }
        float[] positions = new float[points.size() * 2 * 3];
        float[] colors = new float[points.size() * 2 * 3];
        int out = 0;

        for ( int i = 0; i < points.size(); i++ ) {
            Vector3Dd from = points.get(i);
            Vector3Dd to = points.get((i + 1) % points.size());

            for ( Vector3Dd v : new Vector3Dd[] {from, to} ) {
                positions[3*out] = (float)v.x();
                positions[3*out + 1] = (float)v.y();
                positions[3*out + 2] = (float)v.z();
                colors[3*out] = (float)color[0];
                colors[3*out + 1] = (float)color[1];
                colors[3*out + 2] = (float)color[2];
                out++;
            }
        }
        Jogl4LineRenderer.drawLines(gl, mvp, positions, colors, 1.0f);
    }
}
