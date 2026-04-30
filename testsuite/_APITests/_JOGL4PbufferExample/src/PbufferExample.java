// Java basic classes
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Path;

// JOGL classes
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLException;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.media.RGBImageUncompressed;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
This application example is used to test JOGL's / OpenGL's Pbuffer offline
rendering capability on OpenGL 4.1 core profile.
*/
public class PbufferExample implements GLEventListener {
    private static final int IMAGE_WIDTH = 320;
    private static final int IMAGE_HEIGHT = 240;

    private GLOffscreenAutoDrawable pbuffer;
    private int shaderProgramId;
    private int vertexArrayId;
    private int vertexBufferId;
    private boolean done;

    public PbufferExample() {
        createElements();
    }

    private void createElements() throws GLException
    {
        GLProfile profile = GLProfile.get(GLProfile.GL4);

        GLCapabilities pbCaps = new GLCapabilities(profile);
        pbCaps.setDoubleBuffered(false);

        try {
            GLDrawableFactory creator = GLDrawableFactory.getFactory(profile);
            pbuffer = creator.createOffscreenAutoDrawable(
                null, pbCaps, null, IMAGE_WIDTH, IMAGE_HEIGHT);
        }
        catch ( Exception e ) {
            VSDK.reportMessageWithException(
                this,
                VSDK.FATAL_ERROR,
                "PbufferExample.createElements",
                "Error creating OpenGL Pbuffer. This program requires a 3D "
                    + "accelerator card.",
                e);
            return;
        }

        pbuffer.addGLEventListener(this);
        pbuffer.display();
    }

    @Override
    public void init(GLAutoDrawable drawable)
    {
        GL4 gl = drawable.getGL().getGL4();

        checkOpenGLVersion(gl);
        shaderProgramId = createShaderProgram(gl);

        int[] tmp = new int[1];
        gl.glGenVertexArrays(1, tmp, 0);
        vertexArrayId = tmp[0];
        gl.glBindVertexArray(vertexArrayId);

        gl.glGenBuffers(1, tmp, 0);
        vertexBufferId = tmp[0];
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId);

        float[] vertexData = {
            -0.8f, -0.8f, 0.0f,
             0.8f,  0.8f, 0.0f
        };

        gl.glBufferData(
            GL4.GL_ARRAY_BUFFER,
            (long)vertexData.length * Float.BYTES,
            Buffers.newDirectFloatBuffer(vertexData),
            GL4.GL_STATIC_DRAW);

        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 3 * Float.BYTES, 0L);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
        gl.glBindVertexArray(0);

        gl.glUseProgram(shaderProgramId);
        setShaderUniforms(gl, shaderProgramId);
        gl.glUseProgram(0);
    }

    @Override
    public void display(GLAutoDrawable drawable)
    {
        if ( done ) {
            return;
        }

        GL4 gl = drawable.getGL().getGL4();

        gl.glViewport(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL4.GL_COLOR_BUFFER_BIT);

        gl.glUseProgram(shaderProgramId);
        gl.glBindVertexArray(vertexArrayId);
        gl.glDrawArrays(GL4.GL_LINES, 0, 2);
        gl.glBindVertexArray(0);
        gl.glUseProgram(0);
        gl.glFinish();

        RGBImageUncompressed image = captureRgbImage(gl, IMAGE_WIDTH, IMAGE_HEIGHT);
        ImagePersistence.exportJPG(new File("./output.jpg"), image);
        System.out.println("PbufferExample: exported ./output.jpg");

        done = true;
        pbuffer.destroy();
    }

    @Override
    public void dispose(GLAutoDrawable drawable)
    {
        GL4 gl = drawable.getGL().getGL4();
        int[] tmp = new int[1];

        if ( vertexBufferId != 0 ) {
            tmp[0] = vertexBufferId;
            gl.glDeleteBuffers(1, tmp, 0);
            vertexBufferId = 0;
        }

        if ( vertexArrayId != 0 ) {
            tmp[0] = vertexArrayId;
            gl.glDeleteVertexArrays(1, tmp, 0);
            vertexArrayId = 0;
        }

        if ( shaderProgramId != 0 ) {
            gl.glDeleteProgram(shaderProgramId);
            shaderProgramId = 0;
        }

        System.out.println("PbufferExample.dispose: OpenGL resources released = true");
    }

    @Override
    public void reshape(
        GLAutoDrawable drawable, int x, int y, int width, int height )
    {
        GL4 gl = drawable.getGL().getGL4();
        gl.glViewport(0, 0, width, height);
    }

    private static RGBImageUncompressed captureRgbImage(GL4 gl, int width, int height)
    {
        ByteBuffer bb = ByteBuffer.allocateDirect(3 * width * height);
        gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, 1);
        gl.glReadPixels(0, 0, width, height, GL.GL_RGB, GL.GL_UNSIGNED_BYTE, bb);

        RGBImageUncompressed image = new RGBImageUncompressed();
        image.init(width, height);

        int pos = 0;
        for ( int y = image.getYSize() - 1; y >= 0; y-- ) {
            for ( int x = 0; x < image.getXSize(); x++ ) {
                image.putPixel(x, y, bb.get(pos), bb.get(pos + 1), bb.get(pos + 2));
                pos += 3;
            }
        }

        return image;
    }

    private static void checkOpenGLVersion(GL4 gl)
    {
        int[] major = new int[1];
        int[] minor = new int[1];

        gl.glGetIntegerv(GL4.GL_MAJOR_VERSION, major, 0);
        gl.glGetIntegerv(GL4.GL_MINOR_VERSION, minor, 0);

        if ( major[0] < 4 || (major[0] == 4 && minor[0] < 1) ) {
            throw new IllegalStateException(
                "This example requires OpenGL 4.1+. Current context is "
                + major[0] + "." + minor[0]);
        }
    }

    private static int createShaderProgram(GL4 gl)
    {
        String vertexSource = readShaderSource("constantVertexShader.glsl");
        String fragmentSource = readShaderSource("constantPixelShader.glsl");

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
            String log = getShaderInfoLog(gl, shader);
            gl.glDeleteShader(shader);
            throw new IllegalStateException("Shader compile error: " + log);
        }

        return shader;
    }

    private static void setShaderUniforms(GL4 gl, int programId)
    {
        float[] identity = {
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
        };

        int modelViewProjectionLocalLoc = gl.glGetUniformLocation(programId, "modelViewProjectionLocal");
        int withTextureLoc = gl.glGetUniformLocation(programId, "withTexture");
        int withVertexColorsLoc = gl.glGetUniformLocation(programId, "withVertexColors");
        int diffuseColorLoc = gl.glGetUniformLocation(programId, "diffuseColor");

        if ( modelViewProjectionLocalLoc >= 0 ) {
            gl.glUniformMatrix4fv(modelViewProjectionLocalLoc, 1, false, identity, 0);
        }
        if ( withTextureLoc >= 0 ) {
            gl.glUniform1i(withTextureLoc, 0);
        }
        if ( withVertexColorsLoc >= 0 ) {
            gl.glUniform1i(withVertexColorsLoc, 0);
        }
        if ( diffuseColorLoc >= 0 ) {
            gl.glUniform3f(diffuseColorLoc, 1.0f, 1.0f, 1.0f);
        }
    }

    private static String readShaderSource(String shaderFileName)
    {
        Path path = Paths.get("../../../etc/glslShaders", shaderFileName);
        try {
            return Files.readString(path);
        }
        catch ( Exception ignored ) {
        }

        path = Paths.get("etc", "glslShaders", shaderFileName);
        try {
            return Files.readString(path);
        }
        catch ( Exception ignored ) {
        }

        path = Paths.get(System.getProperty("user.dir"), "etc", "glslShaders", shaderFileName);
        try {
            return Files.readString(path);
        }
        catch ( Exception e ) {
            throw new IllegalStateException("Failed to read shader: " + shaderFileName, e);
        }
    }

    private static String getShaderInfoLog(GL4 gl, int shader)
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

    private static String getProgramInfoLog(GL4 gl, int program)
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

    public static void main( String[] args )
    {
        new PbufferExample();
    }
}
