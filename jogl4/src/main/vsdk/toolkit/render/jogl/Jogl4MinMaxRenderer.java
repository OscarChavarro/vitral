package vsdk.toolkit.render.jogl;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.Geometry;

public class Jogl4MinMaxRenderer extends Jogl4Renderer {
    private static final float[] YELLOW_RGB = new float[] { 1.0f, 1.0f, 0.0f };

    public static void draw(GL4 gl, Geometry geometry, Camera camera)
    {
        draw(gl, geometry, camera, Matrix4x4.identityMatrix());
    }

    public static void draw(GL4 gl, Geometry geometry, Camera camera, Matrix4x4 modelViewLocal)
    {
        if ( geometry == null ) {
            return;
        }
        draw(gl, geometry.getMinMax(), camera, modelViewLocal);
    }

    public static void draw(GL4 gl, double[] minmax, Camera camera)
    {
        draw(gl, minmax, camera, Matrix4x4.identityMatrix());
    }

    public static void draw(GL4 gl, double[] minmax, Camera camera, Matrix4x4 modelViewLocal)
    {
        if ( gl == null || minmax == null || minmax.length < 6 || camera == null ) {
            return;
        }

        float[] positions = buildMinMaxLinePositions(minmax);
        float[] colors = buildUniformColors(positions.length / 3, YELLOW_RGB);

        Matrix4x4 local = (modelViewLocal != null)
            ? modelViewLocal
            : Matrix4x4.identityMatrix();
        Matrix4x4 mvp = camera.calculateProjectionMatrix().multiply(local);

        Jogl4LineRenderer.drawLines(gl, mvp, positions, colors, 1.0f);
    }

    public static void dispose(GL4 gl)
    {
        Jogl4LineRenderer.release(gl);
    }

    private static float[] buildUniformColors(int vertexCount, float[] rgb)
    {
        float[] colors = new float[vertexCount * 3];
        for ( int i = 0; i < vertexCount; i++ ) {
            int base = i * 3;
            colors[base] = rgb[0];
            colors[base + 1] = rgb[1];
            colors[base + 2] = rgb[2];
        }
        return colors;
    }

    private static float[] buildMinMaxLinePositions(double[] mm)
    {
        float x0 = (float)mm[0];
        float y0 = (float)mm[1];
        float z0 = (float)mm[2];
        float x1 = (float)mm[3];
        float y1 = (float)mm[4];
        float z1 = (float)mm[5];

        // 12 axis-aligned edges, each expressed as 2 vertices (GL_LINES)
        return new float[] {
            x0, y0, z0,   x1, y0, z0,
            x0, y0, z1,   x1, y0, z1,
            x0, y1, z0,   x1, y1, z0,
            x0, y1, z1,   x1, y1, z1,

            x0, y0, z0,   x0, y1, z0,
            x1, y0, z0,   x1, y1, z0,
            x0, y0, z1,   x0, y1, z1,
            x1, y0, z1,   x1, y1, z1,

            x0, y0, z0,   x0, y0, z1,
            x1, y0, z0,   x1, y0, z1,
            x0, y1, z0,   x0, y1, z1,
            x1, y1, z0,   x1, y1, z1
        };
    }
}
