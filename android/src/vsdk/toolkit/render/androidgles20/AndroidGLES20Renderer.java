//===========================================================================

package vsdk.toolkit.render.androidgles20;

// Basic Java classes
import java.io.InputStream;

// Android classes
import android.util.Log;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

// Sandbox
import vitral.application.R;

// VSDK classes
import vsdk.toolkit.io.PersistenceElement;

public class AndroidGLES20Renderer
{
    private static String TAG = "GLES20TriangleRenderer";

    public static int AndroidGLES20GpuProgramConstant;

    // OpenGL-ES2.0 state
    public static float[] projectionMatrix;
    protected static float[] modelViewMatrix;

    // OpenGL-ES SL parameters
    public static int modelViewProjectionLocalParam;
    public static int PObjectParam;
    public static int uvVertexTextureCoordinateParam;

    public static void init(Context ctx) {
        projectionMatrix = new float[16];
        modelViewMatrix = new float[16];

        Matrix.setIdentityM(projectionMatrix, 0);
        Matrix.setIdentityM(modelViewMatrix, 0);

        createDefaultAutomaticAndroidGLES20Shaders(ctx);
    }

    public static void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    private static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader " + shaderType + ":");
                Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    private static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }

        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            checkGlError("glAttachShader");
            GLES20.glAttachShader(program, pixelShader);
            checkGlError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: ");
                Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    private static String loadAsStringTrimmingComments(InputStream is)
    {
        String s = "";
        String line;
        String line2;

        try {
            while ( is.available() > 0 ) {
                line = PersistenceElement.readAsciiLine(is);
                line2 = "";
                boolean trimmed = false;
                for ( int i = 0; line.length() > 2 && i < line.length()-1; i++ ) {
		    char ci = line.charAt(i);
		    char cj = line.charAt(i+1);

                    if ( ci == '/' && cj == '/' ) {
                        trimmed = true;
			break;
		    }
                    line2 = line2 + ci;
		}
                if ( !trimmed && line.length() > 0 ) {
                    line2 = line2 + line.charAt(line.length()-1);
		}
                s = s + line2 + '\n';
            }
        }
        catch ( Exception e ) {
            System.out.println(e);
        }

        return s;
    }

    public static void createDefaultAutomaticAndroidGLES20Shaders(Context ctx)
    {
        //- Create shader programs ----------------------------------------
        String vertexShaderSource;
        String pixelShaderSource;

        vertexShaderSource = loadAsStringTrimmingComments(
            ctx.getResources().openRawResource(
                R.raw.constanttexturevertexshader));
        pixelShaderSource = loadAsStringTrimmingComments(
            ctx.getResources().openRawResource(
                R.raw.constanttexturepixelshader));

        AndroidGLES20GpuProgramConstant = 
            createProgram(vertexShaderSource, pixelShaderSource);

        //- Create parameters ---------------------------------------------
        PObjectParam = GLES20.glGetAttribLocation(
            AndroidGLES20GpuProgramConstant, "PObject");
        checkGlError("glGetAttribLocation PObject");
        if ( PObjectParam == -1 ) {
            throw new RuntimeException(
                "Could not get attrib location for PObject");
        }

        uvVertexTextureCoordinateParam = GLES20.glGetAttribLocation(
            AndroidGLES20GpuProgramConstant, "uvVertexTextureCoordinate");
        checkGlError("glGetAttribLocation uvVertexTextureCoordinate");
        if ( uvVertexTextureCoordinateParam == -1 ) {
            throw new RuntimeException(
                "Could not get attrib location for uvVertexTextureCoordinate");
        }

        modelViewProjectionLocalParam = GLES20.glGetUniformLocation(
            AndroidGLES20GpuProgramConstant, "modelViewProjectionLocal");
        checkGlError("glGetUniformLocation modelViewProjectionLocal");
        if ( modelViewProjectionLocalParam == -1 ) {
            throw new RuntimeException(
            "Could not get attrib location for modelViewProjectionLocal");
        }

    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
