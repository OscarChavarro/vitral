package vsdk.toolkit.render.jogl;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector4Dd;

public final class Jogl4LineRenderer {
    private static final String VERTEX_SHADER_FILE = "lineVertexShader.glsl";
    private static final String FRAGMENT_SHADER_FILE = "linePixelShader.glsl";
    private static final double[][] CLIP_PLANES = new double[][] {
        { 1.0, 0.0, 0.0, 1.0 },
        { -1.0, 0.0, 0.0, 1.0 },
        { 0.0, 1.0, 0.0, 1.0 },
        { 0.0, -1.0, 0.0, 1.0 },
        { 0.0, 0.0, 1.0, 1.0 },
        { 0.0, 0.0, -1.0, 1.0 }
    };

    private static boolean initialized;
    private static int programId;
    private static int vaoId;
    private static int positionVboId;
    private static int colorVboId;
    private static int mvpLocation;
    private static int depthBiasLocation;

    private Jogl4LineRenderer() {
    }

    public static void drawLines(
        GL4 gl,
        Matrix4x4d modelViewProjection,
        float[] positions,
        float[] colors,
        float lineWidth)
    {
        drawLines(gl, modelViewProjection, positions, colors, lineWidth, 0.0f);
    }

    public static void drawLines(
        GL4 gl,
        Matrix4x4d modelViewProjection,
        float[] positions,
        float[] colors,
        float lineWidth,
        float depthBiasNdc)
    {
        if ( positions == null || colors == null || positions.length == 0 ) {
            return;
        }
        if ( positions.length != colors.length ) {
            throw new IllegalArgumentException("positions/colors length mismatch");
        }
        if ( lineWidth <= 1.0f ) {
            drawThinLines(gl, modelViewProjection, positions, colors, lineWidth,
                depthBiasNdc);
            return;
        }

        ThickLineMesh thickLines = buildThickLineMesh(gl, modelViewProjection,
            positions, colors, lineWidth);
        if ( thickLines.positions.length == 0 ) {
            return;
        }

        drawPrimitives(gl, Matrix4x4d.identityMatrix(), thickLines.positions,
            thickLines.colors, GL4.GL_TRIANGLES, depthBiasNdc);
    }

    private static void drawThinLines(
        GL4 gl,
        Matrix4x4d modelViewProjection,
        float[] positions,
        float[] colors,
        float lineWidth,
        float depthBiasNdc)
    {
        drawPrimitives(gl, modelViewProjection, positions, colors, GL4.GL_LINES,
            depthBiasNdc, lineWidth);
    }

    private static void drawPrimitives(
        GL4 gl,
        Matrix4x4d modelViewProjection,
        float[] positions,
        float[] colors,
        int primitiveType,
        float depthBiasNdc)
    {
        drawPrimitives(gl, modelViewProjection, positions, colors, primitiveType,
            depthBiasNdc, 1.0f);
    }

    private static void drawPrimitives(
        GL4 gl,
        Matrix4x4d modelViewProjection,
        float[] positions,
        float[] colors,
        int primitiveType,
        float depthBiasNdc,
        float lineWidth)
    {
        if ( positions == null || colors == null || positions.length == 0 ) {
            return;
        }

        ensureInitialized(gl);
        disableTextureBindings(gl);

        gl.glUseProgram(programId);
        gl.glUniformMatrix4fv(
            mvpLocation,
            1,
            false,
            Jogl4MatrixRenderer.toColumnMajorFloatArray(modelViewProjection),
            0);
        gl.glUniform1f(depthBiasLocation, depthBiasNdc);

        gl.glBindVertexArray(vaoId);

        FloatBuffer posBuffer = Buffers.newDirectFloatBuffer(positions);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, positionVboId);
        gl.glBufferData(
            GL4.GL_ARRAY_BUFFER,
            (long)positions.length * Float.BYTES,
            posBuffer,
            GL4.GL_STREAM_DRAW);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0L);

        FloatBuffer colorBuffer = Buffers.newDirectFloatBuffer(colors);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, colorVboId);
        gl.glBufferData(
            GL4.GL_ARRAY_BUFFER,
            (long)colors.length * Float.BYTES,
            colorBuffer,
            GL4.GL_STREAM_DRAW);
        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, 0, 0L);

        if ( primitiveType == GL4.GL_LINES ) {
            gl.glLineWidth(lineWidth);
        }
        gl.glDrawArrays(primitiveType, 0, positions.length / 3);

        gl.glDisableVertexAttribArray(0);
        gl.glDisableVertexAttribArray(1);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
        gl.glBindVertexArray(0);
        gl.glUseProgram(0);
    }

    public static void release(GL4 gl)
    {
        if ( !initialized ) {
            return;
        }

        int[] tmp = new int[1];

        if ( positionVboId != 0 ) {
            tmp[0] = positionVboId;
            gl.glDeleteBuffers(1, tmp, 0);
            positionVboId = 0;
        }

        if ( colorVboId != 0 ) {
            tmp[0] = colorVboId;
            gl.glDeleteBuffers(1, tmp, 0);
            colorVboId = 0;
        }

        if ( vaoId != 0 ) {
            tmp[0] = vaoId;
            gl.glDeleteVertexArrays(1, tmp, 0);
            vaoId = 0;
        }

        if ( programId != 0 ) {
            gl.glDeleteProgram(programId);
            programId = 0;
        }

        initialized = false;
        mvpLocation = -1;
        depthBiasLocation = -1;
    }

    private static void ensureInitialized(GL4 gl)
    {
        if ( initialized ) {
            return;
        }

        programId = Jogl4ShaderProgramUtil.createProgramFromFiles(
            gl,
            VERTEX_SHADER_FILE,
            FRAGMENT_SHADER_FILE);

        mvpLocation = gl.glGetUniformLocation(programId, "modelViewProjectionLocal");
        if ( mvpLocation < 0 ) {
            throw new IllegalStateException("Missing uniform modelViewProjectionLocal");
        }
        depthBiasLocation = gl.glGetUniformLocation(programId, "depthBiasNdc");
        if ( depthBiasLocation < 0 ) {
            throw new IllegalStateException("Missing uniform depthBiasNdc");
        }

        int[] tmp = new int[1];

        gl.glGenVertexArrays(1, tmp, 0);
        vaoId = tmp[0];

        gl.glGenBuffers(1, tmp, 0);
        positionVboId = tmp[0];

        gl.glGenBuffers(1, tmp, 0);
        colorVboId = tmp[0];

        initialized = true;
    }

    private static void disableTextureBindings(GL4 gl)
    {
        gl.glActiveTexture(GL4.GL_TEXTURE1);
        gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);
        gl.glActiveTexture(GL4.GL_TEXTURE0);
        gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);
    }

    private static ThickLineMesh buildThickLineMesh(
        GL4 gl,
        Matrix4x4d modelViewProjection,
        float[] positions,
        float[] colors,
        float lineWidth)
    {
        ArrayList<Float> trianglePositions = new ArrayList<Float>();
        ArrayList<Float> triangleColors = new ArrayList<Float>();
        int[] viewport = new int[4];
        gl.glGetIntegerv(GL4.GL_VIEWPORT, viewport, 0);
        double viewportWidth = Math.max(1.0, viewport[2]);
        double viewportHeight = Math.max(1.0, viewport[3]);
        double halfWidth = lineWidth / 2.0;

        for ( int i = 0; i + 5 < positions.length; i += 6 ) {
            Vector4Dd clip0 = modelViewProjection.multiply(new Vector4Dd(
                positions[i], positions[i + 1], positions[i + 2], 1.0));
            Vector4Dd clip1 = modelViewProjection.multiply(new Vector4Dd(
                positions[i + 3], positions[i + 4], positions[i + 5], 1.0));
            Vector4Dd[] clipped = clipLineToClipVolume(clip0, clip1);
            if ( clipped == null ) {
                continue;
            }

            Vector4Dd ndc0 = clipped[0].dividedByW();
            Vector4Dd ndc1 = clipped[1].dividedByW();
            double dxPixels = (ndc1.x() - ndc0.x()) * viewportWidth / 2.0;
            double dyPixels = (ndc1.y() - ndc0.y()) * viewportHeight / 2.0;
            double lengthPixels = Math.hypot(dxPixels, dyPixels);
            if ( lengthPixels <= 1.0e-9 ) {
                continue;
            }

            double perpX = -dyPixels / lengthPixels;
            double perpY = dxPixels / lengthPixels;
            double offsetNdcX = perpX * halfWidth * 2.0 / viewportWidth;
            double offsetNdcY = perpY * halfWidth * 2.0 / viewportHeight;

            float[] p0Plus = new float[] {
                (float)(ndc0.x() + offsetNdcX),
                (float)(ndc0.y() + offsetNdcY),
                (float)ndc0.z()
            };
            float[] p0Minus = new float[] {
                (float)(ndc0.x() - offsetNdcX),
                (float)(ndc0.y() - offsetNdcY),
                (float)ndc0.z()
            };
            float[] p1Plus = new float[] {
                (float)(ndc1.x() + offsetNdcX),
                (float)(ndc1.y() + offsetNdcY),
                (float)ndc1.z()
            };
            float[] p1Minus = new float[] {
                (float)(ndc1.x() - offsetNdcX),
                (float)(ndc1.y() - offsetNdcY),
                (float)ndc1.z()
            };

            float[] c0 = new float[] { colors[i], colors[i + 1], colors[i + 2] };
            float[] c1 = new float[] {
                colors[i + 3], colors[i + 4], colors[i + 5]
            };

            addVertex(trianglePositions, triangleColors, p0Plus, c0);
            addVertex(trianglePositions, triangleColors, p0Minus, c0);
            addVertex(trianglePositions, triangleColors, p1Plus, c1);

            addVertex(trianglePositions, triangleColors, p1Plus, c1);
            addVertex(trianglePositions, triangleColors, p0Minus, c0);
            addVertex(trianglePositions, triangleColors, p1Minus, c1);
        }

        return new ThickLineMesh(toArray(trianglePositions), toArray(triangleColors));
    }

    private static void addVertex(ArrayList<Float> positions,
                                  ArrayList<Float> colors,
                                  float[] point,
                                  float[] color)
    {
        positions.add(point[0]);
        positions.add(point[1]);
        positions.add(point[2]);
        colors.add(color[0]);
        colors.add(color[1]);
        colors.add(color[2]);
    }

    private static float[] toArray(ArrayList<Float> values)
    {
        float[] out = new float[values.size()];
        for ( int i = 0; i < values.size(); i++ ) {
            out[i] = values.get(i);
        }
        return out;
    }

    private static double evaluateClipPlane(double[] plane, Vector4Dd point)
    {
        return plane[0] * point.x() + plane[1] * point.y() +
            plane[2] * point.z() + plane[3] * point.w();
    }

    private static Vector4Dd interpolate(Vector4Dd start,
                                         Vector4Dd end,
                                         double t)
    {
        return start.multiply(1.0 - t).add(end.multiply(t));
    }

    private static Vector4Dd[] clipLineToClipVolume(Vector4Dd start,
                                                    Vector4Dd end)
    {
        Vector4Dd clippedStart = start;
        Vector4Dd clippedEnd = end;

        for ( int i = 0; i < CLIP_PLANES.length; i++ ) {
            double[] plane = CLIP_PLANES[i];
            double d0 = evaluateClipPlane(plane, clippedStart);
            double d1 = evaluateClipPlane(plane, clippedEnd);

            if ( d0 < 0.0 && d1 < 0.0 ) {
                return null;
            }
            if ( d0 < 0.0 || d1 < 0.0 ) {
                double denominator = d0 - d1;
                if ( Math.abs(denominator) < 1.0e-12 ) {
                    return null;
                }
                double t = d0 / denominator;
                Vector4Dd intersection = interpolate(clippedStart, clippedEnd,
                    t);
                if ( d0 < 0.0 ) {
                    clippedStart = intersection;
                }
                else {
                    clippedEnd = intersection;
                }
            }
        }
        return new Vector4Dd[] { clippedStart, clippedEnd };
    }

    private record ThickLineMesh(float[] positions, float[] colors) {
    }
}
