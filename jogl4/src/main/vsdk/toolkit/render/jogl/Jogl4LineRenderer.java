package vsdk.toolkit.render.jogl;

import java.nio.FloatBuffer;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4;

final class Jogl4LineRenderer {
    private static final String VERTEX_SHADER =
        "#version 410 core\n"
        + "layout(location = 0) in vec3 PObject;\n"
        + "layout(location = 1) in vec3 emissionColor;\n"
        + "uniform mat4 modelViewProjectionLocal;\n"
        + "out vec3 vertexColor;\n"
        + "void main() {\n"
        + "    gl_Position = modelViewProjectionLocal * vec4(PObject, 1.0);\n"
        + "    vertexColor = emissionColor;\n"
        + "}\n";

    private static final String FRAGMENT_SHADER =
        "#version 410 core\n"
        + "in vec3 vertexColor;\n"
        + "layout(location = 0) out vec4 fragColor;\n"
        + "void main() {\n"
        + "    fragColor = vec4(vertexColor, 1.0);\n"
        + "}\n";

    private static boolean initialized;
    private static int programId;
    private static int vaoId;
    private static int positionVboId;
    private static int colorVboId;
    private static int mvpLocation;

    private Jogl4LineRenderer() {
    }

    static void drawLines(
        GL4 gl,
        Matrix4x4 modelViewProjection,
        float[] positions,
        float[] colors,
        float lineWidth)
    {
        if ( positions == null || colors == null || positions.length == 0 ) {
            return;
        }
        if ( positions.length != colors.length ) {
            throw new IllegalArgumentException("positions/colors length mismatch");
        }

        ensureInitialized(gl);

        gl.glUseProgram(programId);
        gl.glUniformMatrix4fv(
            mvpLocation,
            1,
            false,
            Jogl4MatrixRenderer.toColumnMajorFloatArray(modelViewProjection),
            0);

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
    }

    private static void ensureInitialized(GL4 gl)
    {
        if ( initialized ) {
            return;
        }

        int vertexShader = compileShader(gl, GL4.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = compileShader(gl, GL4.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        programId = gl.glCreateProgram();
        gl.glAttachShader(programId, vertexShader);
        gl.glAttachShader(programId, fragmentShader);
        gl.glLinkProgram(programId);

        int[] linkStatus = new int[1];
        gl.glGetProgramiv(programId, GL4.GL_LINK_STATUS, linkStatus, 0);
        if ( linkStatus[0] == GL4.GL_FALSE ) {
            String log = Jogl4ShaderProgramUtil.getProgramInfoLog(gl, programId);
            gl.glDeleteShader(vertexShader);
            gl.glDeleteShader(fragmentShader);
            gl.glDeleteProgram(programId);
            programId = 0;
            throw new IllegalStateException("Jogl4LineRenderer link error: " + log);
        }

        gl.glDetachShader(programId, vertexShader);
        gl.glDetachShader(programId, fragmentShader);
        gl.glDeleteShader(vertexShader);
        gl.glDeleteShader(fragmentShader);

        mvpLocation = gl.glGetUniformLocation(programId, "modelViewProjectionLocal");
        if ( mvpLocation < 0 ) {
            throw new IllegalStateException("Missing uniform modelViewProjectionLocal");
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

    private static int compileShader(GL4 gl, int shaderType, String source)
    {
        int shader = gl.glCreateShader(shaderType);
        String[] sources = new String[] { source };
        int[] lengths = new int[] { source.length() };
        gl.glShaderSource(shader, 1, sources, lengths, 0);
        gl.glCompileShader(shader);

        int[] compileStatus = new int[1];
        gl.glGetShaderiv(shader, GL4.GL_COMPILE_STATUS, compileStatus, 0);
        if ( compileStatus[0] == GL4.GL_FALSE ) {
            String log = Jogl4ShaderProgramUtil.getShaderInfoLog(gl, shader);
            gl.glDeleteShader(shader);
            throw new IllegalStateException("Jogl4LineRenderer compile error: " + log);
        }

        return shader;
    }
}
