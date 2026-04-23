package vsdk.toolkit.render.jogl;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4;

public class Jogl4MatrixRenderer extends Jogl4Renderer {

    public static Matrix4x4 importJOGL(GL4 gl, int matrixId)
    {
        double[] Mgl = new double[16];
        double[][] data = new double[4][4];

        int row;
        int column;
        int pos;

        gl.glGetDoublev(matrixId, Mgl, 0);
        for ( pos = 0, column = 0; column < 4; column++ ) {
            for ( row = 0; row < 4; row++, pos++ ) {
                data[row][column] = Mgl[pos];
            }
        }
        return Matrix4x4.copyOf(data);
    }

    public static float[] activate(GL4 gl, Matrix4x4 A)
    {
        return toColumnMajorFloatArray(A);
    }

    public static float[] toColumnMajorFloatArray(Matrix4x4 A)
    {
        float[] out = new float[16];
        int pos = 0;

        for ( int column = 0; column < 4; column++ ) {
            for ( int row = 0; row < 4; row++ ) {
                out[pos++] = (float)A.get(row, column);
            }
        }

        return out;
    }

    public static void draw(GL4 gl, Matrix4x4 A)
    {
        draw(gl, Matrix4x4.identityMatrix(), A);
    }

    public static void draw(GL4 gl, Matrix4x4 modelViewProjection, Matrix4x4 A)
    {
        float ox = (float)A.get(0, 3);
        float oy = (float)A.get(1, 3);
        float oz = (float)A.get(2, 3);

        float[] positions = new float[] {
            ox, oy, oz,
            ox + (float)A.get(0, 0), oy + (float)A.get(1, 0), oz + (float)A.get(2, 0),

            ox, oy, oz,
            ox + (float)A.get(0, 1), oy + (float)A.get(1, 1), oz + (float)A.get(2, 1),

            ox, oy, oz,
            ox + (float)A.get(0, 2), oy + (float)A.get(1, 2), oz + (float)A.get(2, 2)
        };

        float[] colors = new float[] {
            1f, 0f, 0f,
            1f, 0f, 0f,

            0f, 1f, 0f,
            0f, 1f, 0f,

            0f, 0f, 1f,
            0f, 0f, 1f
        };

        Jogl4LineRenderer.drawLines(gl, modelViewProjection, positions, colors, 1.0f);
    }

    static void release(GL4 gl)
    {
        Jogl4LineRenderer.release(gl);
    }
}
