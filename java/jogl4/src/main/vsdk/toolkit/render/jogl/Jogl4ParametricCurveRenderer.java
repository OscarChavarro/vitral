package vsdk.toolkit.render.jogl;

import java.util.ArrayList;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.curve.ParametricCurve;
import vsdk.toolkit.environment.material.RendererConfiguration;

/**
Renders a `ParametricCurve` with the GL4 pipeline. As in
`Jogl2ParametricCurveRenderer`, each segment of the curve (segments ending at
a `BREAK` point are skipped) is approximated by the polyline given by
`ParametricCurve.calculatePoints` and drawn, unlit, with the wire color of the
configuration, whatever its surface and wire bits are (a curve has no
surface). The points bit shows the vertices of the polylines, and the
bounding volume and selection corners bits are honored as for any geometry.
*/
public class Jogl4ParametricCurveRenderer extends Jogl4Renderer {
    private static final float POINT_SIZE = 4.0f;
    private static final float[] POINT_RGB = new float[] { 1.0f, 0.0f, 0.0f };

    private Jogl4ParametricCurveRenderer() {
    }

    /**
    Draws the curve with the wire color of the configuration.

    @param gl OpenGL context
    @param curve curve to draw
    @param camera camera that views the curve
    @param quality bits of rendering configuration
    @param localTransform transformation from curve space to world space; null
    for the identity
    */
    public static void draw(
        GL4 gl,
        ParametricCurve curve,
        Camera camera,
        RendererConfiguration quality,
        Matrix4x4d localTransform)
    {
        ColorRgb color = quality != null && quality.getWireColor() != null
            ? quality.getWireColor()
            : new ColorRgb(1, 1, 1);

        draw(gl, curve, camera, quality, localTransform, color);
    }

    /**
    Draws the curve.

    @param gl OpenGL context
    @param curve curve to draw
    @param camera camera that views the curve
    @param quality bits of rendering configuration
    @param localTransform transformation from curve space to world space; null
    for the identity
    @param color color of the curve
    */
    public static void draw(
        GL4 gl,
        ParametricCurve curve,
        Camera camera,
        RendererConfiguration quality,
        Matrix4x4d localTransform,
        ColorRgb color)
    {
        if ( gl == null || curve == null || camera == null || quality == null ||
             curve.types == null || curve.types.size() < 2 ) {
            return;
        }
        Matrix4x4d local = localTransform != null ? localTransform : Matrix4x4d.identityMatrix();
        Matrix4x4d mvp = camera.calculateProjectionMatrix().multiply(local);
        ArrayList<Vector3Dd> vertices = new ArrayList<Vector3Dd>();
        float[] positions = buildSegmentPositions(curve, vertices);

        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthFunc(GL.GL_LEQUAL);
        if ( positions.length > 0 ) {
            Jogl4LineRenderer.drawLines(gl, mvp, positions,
                buildUniformColors(positions.length / 3,
                    (float)color.r(), (float)color.g(), (float)color.b()),
                1.0f);
        }
        if ( quality.isPointsSet() && !vertices.isEmpty() ) {
            drawPoints(gl, mvp, vertices);
        }
        if ( quality.isBoundingVolumeSet() ) {
            Jogl4MinMaxRenderer.draw(gl, curve, camera, local);
        }
        if ( quality.isSelectionCornersSet() ) {
            Jogl4SelectionCornersRenderer.draw(gl, curve, camera, local);
        }
        gl.glDepthFunc(GL.GL_LESS);
    }

    /**
    @param curve curve to approximate
    @param vertices receives the vertices of the polylines
    @return x,y,z of the ends of the line segments of the polylines of the
    segments of the curve
    */
    private static float[] buildSegmentPositions(ParametricCurve curve,
                                                 ArrayList<Vector3Dd> vertices)
    {
        ArrayList<Vector3Dd> ends = new ArrayList<Vector3Dd>();

        for ( int i = 1; i < curve.types.size(); i++ ) {
            if ( curve.types.get(i).intValue() == ParametricCurve.BREAK ) {
                i++;
                continue;
            }
            ArrayList<Vector3Dd> polyline = curve.calculatePoints(i, false);

            for ( int j = 0; j + 1 < polyline.size(); j++ ) {
                ends.add(polyline.get(j));
                ends.add(polyline.get(j + 1));
            }
            vertices.addAll(polyline);
        }

        float[] positions = new float[ends.size() * 3];

        for ( int k = 0; k < ends.size(); k++ ) {
            positions[3 * k] = (float)ends.get(k).x();
            positions[3 * k + 1] = (float)ends.get(k).y();
            positions[3 * k + 2] = (float)ends.get(k).z();
        }
        return positions;
    }

    /**
    Draws the vertices of the polylines as small crosses, as the points of
    `Jogl2ParametricCurveRenderer` are, sized in world units relative to the
    curve.
    */
    private static void drawPoints(GL4 gl, Matrix4x4d mvp, ArrayList<Vector3Dd> vertices)
    {
        double[] minmax = boundsOf(vertices);
        double size = Math.max(minmax[3] - minmax[0],
            Math.max(minmax[4] - minmax[1], minmax[5] - minmax[2]));
        float h = (float)(Math.max(size, 1e-6) * 0.01);
        float[] positions = new float[vertices.size() * 6 * 3];
        int k = 0;

        for ( Vector3Dd v : vertices ) {
            float x = (float)v.x();
            float y = (float)v.y();
            float z = (float)v.z();
            float[][] axes = new float[][] { { h, 0, 0 }, { 0, h, 0 }, { 0, 0, h } };

            for ( float[] a : axes ) {
                positions[k++] = x - a[0];
                positions[k++] = y - a[1];
                positions[k++] = z - a[2];
                positions[k++] = x + a[0];
                positions[k++] = y + a[1];
                positions[k++] = z + a[2];
            }
        }
        Jogl4LineRenderer.drawLines(gl, mvp, positions,
            buildUniformColors(positions.length / 3, POINT_RGB[0], POINT_RGB[1], POINT_RGB[2]),
            Math.max(1.0f, POINT_SIZE / 2));
    }

    private static double[] boundsOf(ArrayList<Vector3Dd> vertices)
    {
        double[] minmax = new double[] {
            Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE,
            -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE
        };

        for ( Vector3Dd v : vertices ) {
            minmax[0] = Math.min(minmax[0], v.x());
            minmax[1] = Math.min(minmax[1], v.y());
            minmax[2] = Math.min(minmax[2], v.z());
            minmax[3] = Math.max(minmax[3], v.x());
            minmax[4] = Math.max(minmax[4], v.y());
            minmax[5] = Math.max(minmax[5], v.z());
        }
        return minmax;
    }

    private static float[] buildUniformColors(int vertexCount, float r, float g, float b)
    {
        float[] colors = new float[vertexCount * 3];

        for ( int i = 0; i < vertexCount; i++ ) {
            colors[3 * i] = r;
            colors[3 * i + 1] = g;
            colors[3 * i + 2] = b;
        }
        return colors;
    }
}
