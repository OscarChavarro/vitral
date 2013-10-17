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
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.io.PersistenceElement;

public class AndroidGLES20Renderer
{
    private static String TAG = "GLES20TriangleRenderer";

    public static int AndroidGLES20GpuProgramConstant;
    public static int AndroidGLES20GpuProgramConstantTexture;

    // OpenGL-ES2.0 state
    public static float[] transientMatrix;
    public static float[] modelViewMatrix;
    public static float[] modelViewProjectionLocal;
    public static float[] projectionMatrix;
    public static final int GL_MODELVIEW = 1;
    public static final int GL_PROJECTION = 2;
    public static final int GL_TEXTURE_2D = 2;
    public static int currentMatrixMode = GL_MODELVIEW;
    public static RendererConfiguration qualitySelection;
    public static boolean errorsDetected = false;

    // OpenGL-ES SL parameters
    public static int modelViewProjectionLocalParam;
    public static int PObjectParam;
    public static int uvVertexTextureCoordinateParam;

    public static void init(Context ctx) {
        //- Set up geometric transforms -----------------------------------
        projectionMatrix = new float[16];
        modelViewMatrix = new float[16];
        modelViewProjectionLocal = new float[16];
        transientMatrix = new float[16];

        Matrix.setIdentityM(projectionMatrix, 0);
        Matrix.setIdentityM(modelViewMatrix, 0);
        Matrix.setIdentityM(modelViewProjectionLocal, 0);
        Matrix.setIdentityM(transientMatrix, 0);

        //- Set up shaders ------------------------------------------------
        ColorRgb white = new ColorRgb(1, 1, 1);
        qualitySelection = new RendererConfiguration();
        qualitySelection.setWireColor(white);
        qualitySelection.setTexture(false);
        qualitySelection.setSurfaces(true);
        qualitySelection.setWires(false);
        qualitySelection.setPoints(false);
        qualitySelection.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
        if ( !createDefaultAutomaticAndroidGLES20Shaders(ctx) == true ) {
            errorsDetected = true;
	}
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

    public static boolean createDefaultAutomaticAndroidGLES20Shaders(Context ctx)
    {
        //- Create shader programs ----------------------------------------
        String vertexShaderSource;
        String pixelShaderSource;

        vertexShaderSource = loadAsStringTrimmingComments(
            ctx.getResources().openRawResource(
                R.raw.constantvertexshader));
        pixelShaderSource = loadAsStringTrimmingComments(
            ctx.getResources().openRawResource(
                R.raw.constantpixelshader));

        AndroidGLES20GpuProgramConstant = 
            createProgram(vertexShaderSource, pixelShaderSource);

        if ( AndroidGLES20GpuProgramConstant == 0 ) {
	    System.err.println("ERROR CREATING CONSTANT SHADER!");
	    return false;
	}

        vertexShaderSource = loadAsStringTrimmingComments(
            ctx.getResources().openRawResource(
                R.raw.constanttexturevertexshader));
        pixelShaderSource = loadAsStringTrimmingComments(
            ctx.getResources().openRawResource(
                R.raw.constanttexturepixelshader));

        AndroidGLES20GpuProgramConstantTexture = 
            createProgram(vertexShaderSource, pixelShaderSource);

        if ( AndroidGLES20GpuProgramConstantTexture == 0 ) {
	    System.err.println("ERROR CREATING CONSTANT TEXTURE SHADER!");
	    return false;
	}

        //- Create parameters ---------------------------------------------
        activateShaders();
        return true;
    }

    private static void activateShaders()
    {
        //- Select current shaders programs from rendering configuration --
        int shaderId = AndroidGLES20GpuProgramConstant;

        if ( qualitySelection.isTextureSet() ) {
            shaderId = AndroidGLES20GpuProgramConstantTexture;
	}

        //- Activate shader parameters ------------------------------------
        if ( qualitySelection.isTextureSet() ) {
            uvVertexTextureCoordinateParam = GLES20.glGetAttribLocation(
                shaderId, "uvVertexTextureCoordinate");
            checkGlError("glGetAttribLocation uvVertexTextureCoordinate");
            if ( uvVertexTextureCoordinateParam == -1 ) {
                throw new RuntimeException(
                "Could not get attrib location for uvVertexTextureCoordinate");
            }
	}

        modelViewProjectionLocalParam = GLES20.glGetUniformLocation(
            shaderId, "modelViewProjectionLocal");
        checkGlError("glGetUniformLocation modelViewProjectionLocal");
        if ( modelViewProjectionLocalParam == -1 ) {
            throw new RuntimeException(
            "Could not get attrib location for modelViewProjectionLocal");
        }

        PObjectParam = GLES20.glGetAttribLocation(shaderId, "PObject");
        checkGlError("glGetAttribLocation PObject");
        if ( PObjectParam == -1 ) {
            throw new RuntimeException(
                "Could not get attrib location for PObject");
        }

        GLES20.glUseProgram(shaderId);
        checkGlError("glUseProgram");
    }

    public static void activateTransformationMatrices()
    {
        Matrix.multiplyMM(modelViewProjectionLocal, 0,
            projectionMatrix, 0, modelViewMatrix, 0);
        GLES20.glUniformMatrix4fv(modelViewProjectionLocalParam, 1, false, 
            modelViewProjectionLocal, 0);
    }

    public static void glEnable(int k)
    {
        switch ( k ) {
  	  case GL_TEXTURE_2D:
	    qualitySelection.setTexture(true);
            activateShaders();
	    break;
	}
    }

    public static void glDisable(int k)
    {
        switch ( k ) {
  	  case GL_TEXTURE_2D:
	    qualitySelection.setTexture(false);
            activateShaders();
	    break;
	}
    }

    public static void glLoadIdentity()
    {
        switch ( currentMatrixMode ) {
	  case GL_MODELVIEW:
            Matrix.setIdentityM(modelViewMatrix, 0);
	    break;
	  case GL_PROJECTION:
            Matrix.setIdentityM(projectionMatrix, 0);
	    break;
	}

        activateTransformationMatrices();
    }

    public static void glPushMatrix()
    {
        activateTransformationMatrices();
    }

    public static void glPopMatrix()
    {

        activateTransformationMatrices();
    }

    public static void glScaled(double sx, double sy, double sz)
    {
        Matrix.setIdentityM(transientMatrix, 0);       
        Matrix.scaleM(transientMatrix, 0, 
            (float)sx, (float)sy, (float)sz);
        switch ( currentMatrixMode ) {
	  case GL_MODELVIEW:
            Matrix.multiplyMM(modelViewMatrix, 0,
                transientMatrix, 0, modelViewMatrix, 0);
	    break;
	  case GL_PROJECTION:
            Matrix.multiplyMM(projectionMatrix, 0,
                transientMatrix, 0, projectionMatrix, 0);
	    break;
	}
        activateTransformationMatrices();
    }

    public static void glTranslated(double tx, double ty, double tz)
    {
        Matrix.setIdentityM(transientMatrix, 0);       
        Matrix.translateM(transientMatrix, 0,
            (float)tx, (float)ty, (float)tz);
        switch ( currentMatrixMode ) {
	  case GL_MODELVIEW:
            Matrix.translateM(modelViewMatrix, 0,
                (float)tx, (float)ty, (float)tz);
            //Matrix.multiplyMM(modelViewMatrix, 0,
            //    transientMatrix, 0, modelViewMatrix, 0);
	    break;
	  case GL_PROJECTION:
            Matrix.multiplyMM(projectionMatrix, 0,
                transientMatrix, 0, projectionMatrix, 0);
	    break;
	}
        activateTransformationMatrices();
    }

    public static void glRotated(double angleDegrees, 
        double ax, double ay, double az)
    {
        Matrix.setRotateM(transientMatrix, 0, 
            (float)angleDegrees, (float)ax, (float)ay, (float)az);
        switch ( currentMatrixMode ) {
	  case GL_MODELVIEW:
            Matrix.multiplyMM(modelViewMatrix, 0,
                transientMatrix, 0, modelViewMatrix, 0);
	    break;
	  case GL_PROJECTION:
            Matrix.multiplyMM(projectionMatrix, 0,
                transientMatrix, 0, projectionMatrix, 0);
	    break;
	}
        activateTransformationMatrices();
    }

    public static void glMatrixMode(int newMode)
    {
        switch ( newMode ) {
	  case GL_MODELVIEW:
	  case GL_PROJECTION:
	    currentMatrixMode = newMode;
	    break;
	}

    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
