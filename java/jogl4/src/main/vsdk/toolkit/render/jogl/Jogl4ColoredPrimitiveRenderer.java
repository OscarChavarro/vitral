package vsdk.toolkit.render.jogl;

import java.nio.FloatBuffer;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;

/**
Draws batches of primitives (triangles, triangle strips and fans, lines,
points) with a color (and transparency) per vertex and no lighting: the GL4
replacement of immediate mode (`glBegin`, `glColor`, `glVertex`, `glEnd`)
used to draw gizmos, grids, borders and overlays.

The caller sets the depth test, blending, culling and polygon mode; this
class only binds its program and vertex buffers. The batch is uploaded on each
call, so it is meant for small amounts of geometry.
*/
public final class Jogl4ColoredPrimitiveRenderer {
    private static final String VERTEX_SHADER_FILE = "coloredPrimitiveVertexShader.glsl";
    private static final String FRAGMENT_SHADER_FILE = "coloredPrimitivePixelShader.glsl";

    private static boolean initialized;
    private static int programId;
    private static int vaoId;
    private static int positionVboId;
    private static int colorVboId;
    private static int mvpLocation;
    private static int depthBiasLocation;

    private Jogl4ColoredPrimitiveRenderer() {
    }

    /**
    Draws a batch of primitives.

    @param gl OpenGL context
    @param modelViewProjection matrix that takes the positions to clip space
    @param primitiveType `GL4.GL_TRIANGLES`, `GL4.GL_TRIANGLE_STRIP`,
    `GL4.GL_TRIANGLE_FAN`, `GL4.GL_LINES`, `GL4.GL_LINE_LOOP`, `GL4.GL_POINTS`...
    @param positions x, y, z of each vertex
    @param colors r, g, b, a of each vertex
    */
    public static void draw(
        GL4 gl,
        Matrix4x4d modelViewProjection,
        int primitiveType,
        float[] positions,
        float[] colors)
    {
        draw(gl, modelViewProjection, primitiveType, positions, colors, 0.0f);
    }

    /**
    Draws a batch of primitives, moving it towards (negative) or away from
    (positive) the viewer by a bias in normalized device coordinates.

    @param gl OpenGL context
    @param modelViewProjection matrix that takes the positions to clip space
    @param primitiveType see the overload without bias
    @param positions x, y, z of each vertex
    @param colors r, g, b, a of each vertex
    @param depthBiasNdc depth bias, in normalized device coordinates
    */
    public static void draw(
        GL4 gl,
        Matrix4x4d modelViewProjection,
        int primitiveType,
        float[] positions,
        float[] colors,
        float depthBiasNdc)
    {
        if ( positions == null || colors == null || positions.length == 0 ) {
            return;
        }
        int vertexCount = positions.length / 3;
        if ( colors.length != vertexCount * 4 ) {
            throw new IllegalArgumentException("positions/colors length mismatch");
        }

        ensureInitialized(gl);

        gl.glUseProgram(programId);
        gl.glUniformMatrix4fv(mvpLocation, 1, false,
            Jogl4MatrixRenderer.toColumnMajorFloatArray(modelViewProjection), 0);
        gl.glUniform1f(depthBiasLocation, depthBiasNdc);

        gl.glBindVertexArray(vaoId);
        upload(gl, positionVboId, 0, 3, positions);
        upload(gl, colorVboId, 1, 4, colors);

        gl.glDrawArrays(primitiveType, 0, vertexCount);

        gl.glDisableVertexAttribArray(0);
        gl.glDisableVertexAttribArray(1);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
        gl.glBindVertexArray(0);
        gl.glUseProgram(0);
    }

    /**
    Releases the OpenGL objects of this renderer. PRE: the context that
    created them is current.
    @param gl OpenGL context
    */
    public static void release(GL4 gl)
    {
        if ( !initialized ) {
            return;
        }
        int[] tmp = new int[1];

        tmp[0] = positionVboId;
        gl.glDeleteBuffers(1, tmp, 0);
        tmp[0] = colorVboId;
        gl.glDeleteBuffers(1, tmp, 0);
        tmp[0] = vaoId;
        gl.glDeleteVertexArrays(1, tmp, 0);
        gl.glDeleteProgram(programId);
        initialized = false;
    }

    private static void upload(GL4 gl, int vbo, int attribute, int size, float[] data)
    {
        FloatBuffer buffer = Buffers.newDirectFloatBuffer(data);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vbo);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long)data.length * Float.BYTES, buffer,
            GL4.GL_STREAM_DRAW);
        gl.glEnableVertexAttribArray(attribute);
        gl.glVertexAttribPointer(attribute, size, GL4.GL_FLOAT, false, 0, 0L);
    }

    private static void ensureInitialized(GL4 gl)
    {
        if ( initialized ) {
            return;
        }
        programId = Jogl4ShaderProgramUtil.createProgramFromFiles(gl,
            VERTEX_SHADER_FILE, FRAGMENT_SHADER_FILE);
        mvpLocation = gl.glGetUniformLocation(programId, "modelViewProjectionLocal");
        depthBiasLocation = gl.glGetUniformLocation(programId, "depthBiasNdc");

        int[] tmp = new int[1];

        gl.glGenVertexArrays(1, tmp, 0);
        vaoId = tmp[0];
        gl.glGenBuffers(1, tmp, 0);
        positionVboId = tmp[0];
        gl.glGenBuffers(1, tmp, 0);
        colorVboId = tmp[0];
        initialized = true;
    }
}
