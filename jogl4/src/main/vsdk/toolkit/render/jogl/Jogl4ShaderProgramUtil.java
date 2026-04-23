package vsdk.toolkit.render.jogl;

import com.jogamp.opengl.GL4;

public final class Jogl4ShaderProgramUtil {
    private Jogl4ShaderProgramUtil() {
    }

    public static int createProgramFromFiles(GL4 gl, String vertexShaderFile, String fragmentShaderFile)
    {
        String vertexSource = Jogl4ShaderLoader.readShaderSource(vertexShaderFile);
        String fragmentSource = Jogl4ShaderLoader.readShaderSource(fragmentShaderFile);

        int vertexShader = compileShader(gl, GL4.GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = compileShader(gl, GL4.GL_FRAGMENT_SHADER, fragmentSource);

        int program = gl.glCreateProgram();
        gl.glAttachShader(program, vertexShader);
        gl.glAttachShader(program, fragmentShader);
        gl.glBindFragDataLocation(program, 0, "fragColor");
        gl.glLinkProgram(program);

        int[] linkStatus = new int[1];
        gl.glGetProgramiv(program, GL4.GL_LINK_STATUS, linkStatus, 0);
        if ( linkStatus[0] == GL4.GL_FALSE ) {
            String log = getProgramInfoLog(gl, program);
            gl.glDeleteShader(vertexShader);
            gl.glDeleteShader(fragmentShader);
            gl.glDeleteProgram(program);
            throw new IllegalStateException("Program link error: " + log);
        }

        gl.glDetachShader(program, vertexShader);
        gl.glDetachShader(program, fragmentShader);
        gl.glDeleteShader(vertexShader);
        gl.glDeleteShader(fragmentShader);

        return program;
    }

    public static int compileShader(GL4 gl, int shaderType, String source)
    {
        int shader = gl.glCreateShader(shaderType);
        String[] sources = new String[] { source };
        int[] lengths = new int[] { source.length() };
        gl.glShaderSource(shader, 1, sources, lengths, 0);
        gl.glCompileShader(shader);

        int[] compileStatus = new int[1];
        gl.glGetShaderiv(shader, GL4.GL_COMPILE_STATUS, compileStatus, 0);
        if ( compileStatus[0] == GL4.GL_FALSE ) {
            String log = getShaderInfoLog(gl, shader);
            gl.glDeleteShader(shader);
            throw new IllegalStateException("Shader compile error: " + log);
        }

        return shader;
    }

    public static String getShaderInfoLog(GL4 gl, int shader)
    {
        int[] length = new int[1];
        gl.glGetShaderiv(shader, GL4.GL_INFO_LOG_LENGTH, length, 0);
        if ( length[0] <= 1 ) {
            return "(no log)";
        }
        byte[] data = new byte[length[0]];
        gl.glGetShaderInfoLog(shader, data.length, length, 0, data, 0);
        return new String(data, 0, Math.max(0, length[0] - 1));
    }

    public static String getProgramInfoLog(GL4 gl, int program)
    {
        int[] length = new int[1];
        gl.glGetProgramiv(program, GL4.GL_INFO_LOG_LENGTH, length, 0);
        if ( length[0] <= 1 ) {
            return "(no log)";
        }
        byte[] data = new byte[length[0]];
        gl.glGetProgramInfoLog(program, data.length, length, 0, data, 0);
        return new String(data, 0, Math.max(0, length[0] - 1));
    }
}
