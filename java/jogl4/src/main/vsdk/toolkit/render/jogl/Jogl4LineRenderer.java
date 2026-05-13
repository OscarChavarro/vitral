package vsdk.toolkit.render.jogl;

import java.nio.FloatBuffer;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4;

final class Jogl4LineRenderer {
    private static final String VERTEX_SHADER_FILE = "lineVertexShader.glsl";
    private static final String FRAGMENT_SHADER_FILE = "linePixelShader.glsl";

    private static boolean initialized;
    private static int programId;
    private static int vaoId;
    private static int positionVboId;
    private static int colorVboId;
    private static int mvpLocation;
    private static int depthBiasLocation;

    private Jogl4LineRenderer() {
    }

    static void drawLines(
        GL4 gl,
        Matrix4x4 modelViewProjection,
        float[] positions,
        float[] colors,
        float lineWidth)
    {
        drawLines(gl, modelViewProjection, positions, colors, lineWidth, 0.0f);
    }

    static void drawLines(
        GL4 gl,
        Matrix4x4 modelViewProjection,
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

        gl.glLineWidth(lineWidth);
        gl.glDrawArrays(GL4.GL_LINES, 0, positions.length / 3);

        gl.glDisableVertexAttribArray(0);
        gl.glDisableVertexAttribArray(1);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
        gl.glBindVertexArray(0);
        gl.glUseProgram(0);
    }

    static void release(GL4 gl)
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
}
