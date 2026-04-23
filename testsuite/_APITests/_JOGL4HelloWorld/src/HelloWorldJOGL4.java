// Java Awt/Swing classes
import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

// Java base classes
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

// JOGL classes
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.common.nio.Buffers;

/**
Simple example program to show how to program OpenGL programs in Java using
JOGL and OpenGL 4.1 core profile. Current program does not make use of
interaction events, and does not respond to mouse neither keyboard events.
*/
public class HelloWorldJOGL4 implements GLEventListener
{
    private static final String SHADER_BASE_PATH = "../../../etc/glslShaders";

    private JFrame mainWindowWidget;
    private GLCanvas canvas;

    private int shaderProgramId;
    private int vertexArrayId;
    private int vertexBufferId;
    private boolean ready;
    private boolean closing;
    private boolean glResourcesReleased;

    public HelloWorldJOGL4()
    {
        createElements();
    }

    public final void createElements()
    {
        GLProfile glp = GLProfile.get(GLProfile.GL4);
        GLCapabilities caps = new GLCapabilities(glp);
        canvas = new GLCanvas(caps);
        canvas.addGLEventListener(this);
        canvas.setFocusable(true);

        mainWindowWidget = new JFrame("VITRAL concept test - JOGL4 Hello World");
        mainWindowWidget.add(canvas, BorderLayout.CENTER);
        mainWindowWidget.pack();
        mainWindowWidget.setSize(640, 480);
        mainWindowWidget.setLocationRelativeTo(null);
        mainWindowWidget.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        KeyAdapter escKeyHandler = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) {
                    requestClose();
                }
            }
        };

        canvas.addKeyListener(escKeyHandler);
        mainWindowWidget.addKeyListener(escKeyHandler);
        mainWindowWidget.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                requestClose();
            }
        });
    }

    public final void runApp()
    {
        mainWindowWidget.setVisible(true);
        canvas.requestFocusInWindow();
    }

    @Override
    public void init(GLAutoDrawable drawable)
    {
        ready = false;
        try {
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
            setShaderUniforms(gl);
            gl.glUseProgram(0);
            ready = true;
        }
        catch (Exception e) {
            System.err.println("JOGL4 init failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void display(GLAutoDrawable drawable)
    {
        if ( !ready ) return;
        GL4 gl = drawable.getGL().getGL4();

        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL4.GL_COLOR_BUFFER_BIT);

        gl.glUseProgram(shaderProgramId);
        gl.glBindVertexArray(vertexArrayId);
        gl.glLineWidth(1.0f);
        gl.glDrawArrays(GL4.GL_LINES, 0, 2);
        gl.glBindVertexArray(0);
        gl.glUseProgram(0);
    }

    @Override
    public void dispose(GLAutoDrawable drawable)
    {
        GL4 gl = drawable.getGL().getGL4();
        ready = false;

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

        glResourcesReleased = true;
    }

    @Override
    public void reshape(
        GLAutoDrawable drawable,
        int x,
        int y,
        int width,
        int height)
    {
        GL4 gl = drawable.getGL().getGL4();
        gl.glViewport(0, 0, width, height);
    }

    private void checkOpenGLVersion(GL4 gl)
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

    private int createShaderProgram(GL4 gl)
    {
        String vertexSource = readShaderSource("constantvertexshader.glsl");
        String fragmentSource = readShaderSource("constantpixelshader.glsl");

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

    private int compileShader(GL4 gl, int shaderType, String source)
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

    private void setShaderUniforms(GL4 gl)
    {
        float[] identity = {
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
        };

        int modelViewProjectionLocalLoc = gl.glGetUniformLocation(shaderProgramId, "modelViewProjectionLocal");
        int withTextureLoc = gl.glGetUniformLocation(shaderProgramId, "withTexture");
        int withVertexColorsLoc = gl.glGetUniformLocation(shaderProgramId, "withVertexColors");
        int diffuseColorLoc = gl.glGetUniformLocation(shaderProgramId, "diffuseColor");

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
        List<Path> candidates = new ArrayList<>();
        candidates.add(Paths.get(SHADER_BASE_PATH, shaderFileName));
        candidates.add(Paths.get("etc", "glslShaders", shaderFileName));
        candidates.add(Paths.get(System.getProperty("user.dir"), "etc", "glslShaders", shaderFileName));

        for ( Path path : candidates ) {
            if ( Files.exists(path) ) {
                try {
                    return Files.readString(path);
                }
                catch ( IOException e ) {
                    throw new IllegalStateException("Failed to read shader: " + path, e);
                }
            }
        }
        throw new IllegalStateException("Shader not found: " + shaderFileName + " (searched " + candidates + ")");
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

    public static void main(String[] args)
    {
        HelloWorldJOGL4 instance = new HelloWorldJOGL4();
        instance.runApp();
    }

    private void requestClose()
    {
        if ( closing ) return;
        closing = true;

        Runnable closeAction = () -> {
            if ( canvas != null ) {
                canvas.destroy();
            }
            if ( mainWindowWidget != null ) {
                mainWindowWidget.dispose();
            }
        };

        if ( SwingUtilities.isEventDispatchThread() ) {
            closeAction.run();
        }
        else {
            SwingUtilities.invokeLater(closeAction);
        }
        System.exit(0);
    }
}
