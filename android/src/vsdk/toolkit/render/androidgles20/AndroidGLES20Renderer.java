//===========================================================================

package vsdk.toolkit.render.androidgles20;

// Basic Java classes
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Stack;

// Android classes
import android.util.Log;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

// Sandbox
import vitral.application.R;

// VSDK classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.io.PersistenceElement;
import vsdk.toolkit.render.RenderingElement;

public class AndroidGLES20Renderer extends RenderingElement
{
    private static String TAG = "GLES20TriangleRenderer";

    public static int AndroidGLES20GpuProgramConstant;
    public static int AndroidGLES20GpuProgramGouraud;
    
    public static final int mode3Position3Normal2UV = 0;
    public static final int mode3Position3Color = 1;
    public static final int mode3Position3Color3Normal2UV = 2;

    // Common values
    protected static final int FLOAT_SIZE_IN_BYTES = 4;

    // OpenGL-ES2.0 state
    public static float[] transientMatrix;
    public static float[] originalMatrix;
    public static float[] modelViewMatrix;
    public static float[] modelViewITMatrix;
    public static float[] modelViewProjectionLocal;
    public static float[] projectionMatrix;
    private static Stack<float[]> matrixStack;
    public static final int GL_MODELVIEW = 1;
    public static final int GL_PROJECTION = 2;
    public static final int GL_TEXTURE_2D = 3;
    public static final int GL_LIGHTING = 4;
    public static int currentMatrixMode = GL_MODELVIEW;
    public static RendererConfiguration qualitySelection;
    protected static Material currentMaterial;
    public static boolean errorsDetected = false;
    protected static ArrayList<Light> lights;
    protected static Camera currentCamera;

    // OpenGL-ES SL parameters
    public static int modelViewProjectionLocalParam;
    public static int modelViewLocalParam;
    public static int modelViewITLocalParam;
    public static int PObjectParam;
    public static int uvVertexTextureCoordinateParam;
    public static int emissionColorParam;
    public static int NObjectParam;
    private static int ambientColorParam;
    private static int diffuseColorParam;
    private static int specularColorParam;
    private static int phongExponentParam;
    private static int lightPositionsGlobalParam;
    private static int numberOfLightsParam;
    private static int withTextureParam;
    private static int cameraPositionGlobalParam;

    public static void init(Context ctx) {
        //- Set up geometric transforms -----------------------------------
        projectionMatrix = new float[16];
        modelViewMatrix = new float[16];
        modelViewITMatrix = new float[16];
        modelViewProjectionLocal = new float[16];
        transientMatrix = new float[16];
        originalMatrix = new float[16];

        Matrix.setIdentityM(projectionMatrix, 0);
        Matrix.setIdentityM(modelViewMatrix, 0);
        Matrix.setIdentityM(modelViewITMatrix, 0);
        Matrix.setIdentityM(modelViewProjectionLocal, 0);
        Matrix.setIdentityM(transientMatrix, 0);
        Matrix.setIdentityM(originalMatrix, 0);
        matrixStack = new Stack<float[]>();

        //- Set up shaders ------------------------------------------------
        ColorRgb white = new ColorRgb(1, 1, 1);
        qualitySelection = new RendererConfiguration();
        qualitySelection.setWireColor(white);
        qualitySelection.setTexture(false);
        qualitySelection.setSurfaces(true);
        qualitySelection.setWires(false);
        qualitySelection.setPoints(false);
        qualitySelection.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
        currentMaterial = new Material();
        currentCamera = null;

        if ( !createDefaultAutomaticAndroidGLES20Shaders(ctx) == true ) {
            errorsDetected = true;
        }

        lights = new ArrayList<Light>();
    }

    public static void setRendererConfiguration(RendererConfiguration source)
    {
        qualitySelection.clone(source);
        activateShaders();
    }

    public static void setShadingType(int t)
    {
        qualitySelection.setShadingType(t);
        activateShaders();
    }

    public static void checkGlError(String op) {
        int error;
        error = GLES20.glGetError();
        String name = "UNKNOWN GL ERROR";

        while ( error != GLES20.GL_NO_ERROR) {
            switch ( error ) {
            case GLES20.GL_INVALID_ENUM:
                name = "GL_INVALID_ENUM​";
                break;
            case GLES20.GL_INVALID_VALUE:
                name = "GL_INVALID_VALUE";
                break;
            case GLES20.GL_INVALID_OPERATION:
                name = "GL_INVALID_OPERATION​";
                break;
            case GLES20.GL_OUT_OF_MEMORY:
                name = "GL_OUT_OF_MEMORY​";
                break;
            case GLES20.GL_INVALID_FRAMEBUFFER_OPERATION:
                name = "GL_INVALID_FRAMEBUFFER_OPERATION";
                break;
            //case GLES20.GL_TABLE_TOO_LARGE:
            //  name = "GL_TABLE_TOO_LARGE​"; 
            //  break;
            //case GLES20.GL_STACK_OVERFLOW: name = "GL_STACK_OVERFLOW"; break;
            //case GLES20.GL_STACK_UNDERFLOW: name = "GL_STACK_UNDERFLOW"; break;
            }
            Log.e(TAG, op + ": glError " + error + " : " + name);
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
        String vertexShaderSource;
        String pixelShaderSource;

        //- Create shader programs ----------------------------------------
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
                R.raw.gouraudvertexshader));
        pixelShaderSource = loadAsStringTrimmingComments(
            ctx.getResources().openRawResource(
                R.raw.gouraudpixelshader));
        AndroidGLES20GpuProgramGouraud = 
            createProgram(vertexShaderSource, pixelShaderSource);
        if ( AndroidGLES20GpuProgramGouraud == 0 ) {
            System.err.println("ERROR CREATING GOURAUD SHADER!");
            return false;
        }

        //- Create parameters ---------------------------------------------
        activateShaders();
        return true;
    }

    protected static void activateShaders()
    {
        //- Select current shaders programs from rendering configuration --
        int shaderId = AndroidGLES20GpuProgramConstant;

        //System.out.print("Seleccionando shader " + qualitySelection.getShadingType() + " ... :");

        if ( qualitySelection.getShadingType() == 
            RendererConfiguration.SHADING_TYPE_GOURAUD ||
             qualitySelection.getShadingType() == 
            RendererConfiguration.SHADING_TYPE_FLAT ) {
            shaderId = AndroidGLES20GpuProgramGouraud;
            //System.out.println("Gouraud");
        }
        else {
            shaderId = AndroidGLES20GpuProgramConstant;
            //System.out.println("Constante");
        }

        //- Activate selected shader programs -----------------------------
        GLES20.glUseProgram(shaderId);
        checkGlError("glUseProgram");

        //- Activate shader parameters ------------------------------------
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

        if ( qualitySelection.isTextureSet() ) {
            uvVertexTextureCoordinateParam = GLES20.glGetAttribLocation(
                shaderId, "uvVertexTextureCoordinate");
            checkGlError("glGetAttribLocation uvVertexTextureCoordinate");
            if ( uvVertexTextureCoordinateParam == -1 ) {
                throw new RuntimeException(
                    "Could not get attrib location for uvVertexTextureCoordinate");
            }
        }

        withTextureParam = GLES20.glGetUniformLocation(shaderId, "withTexture");
        checkGlError("glGetUniformLocation withTexture");
        if ( withTextureParam == -1 ) {
            //throw new RuntimeException(
            //    "Could not get uniform location for withTexture");
        }
        else {
            GLES20.glUniform1i(withTextureParam, 
                qualitySelection.isTextureSet()?1:0);
        }

        if ( qualitySelection.getShadingType() == 
            RendererConfiguration.SHADING_TYPE_NOLIGHT ) {
            emissionColorParam = 
                GLES20.glGetAttribLocation(shaderId, "emissionColor");
            checkGlError("glGetAttribLocation emissionColor");
            if ( emissionColorParam == -1 ) {
                throw new RuntimeException(
                    "Could not get attrib location for emissionColor");
            }
        }

        if ( qualitySelection.getShadingType() == 
            RendererConfiguration.SHADING_TYPE_GOURAUD ||
             qualitySelection.getShadingType() == 
            RendererConfiguration.SHADING_TYPE_FLAT ) {

            // Activate normal parameter
            NObjectParam = GLES20.glGetAttribLocation(shaderId, "NObject");
            checkGlError("glGetAttribLocation NObject");
            if ( NObjectParam == -1 ) {
                throw new RuntimeException(
                    "Could not get attrib location for NObject");
            }

            modelViewLocalParam = GLES20.glGetUniformLocation(
                shaderId, "modelViewLocal");
            checkGlError("glGetUniformLocation modelViewLocal");
            if ( modelViewLocalParam == -1 ) {
                throw new RuntimeException(
                    "Could not get attrib location for modelViewLocalParam");
            }

            modelViewITLocalParam = GLES20.glGetUniformLocation(
                shaderId, "modelViewITLocal");
            checkGlError("glGetUniformLocation modelViewITLocal");
            if ( modelViewITLocalParam == -1 ) {
                throw new RuntimeException(
                    "Could not get attrib location for modelViewITLocalParam");
            }

            // Activate material parameters
            ColorRgb c;
            ambientColorParam =
                GLES20.glGetUniformLocation(shaderId, "ambientColor");
            checkGlError("glGetUniformLocation ambientColor");
            if ( ambientColorParam == -1 ) {
                throw new RuntimeException(
                    "Could not get uniform location for ambientColor");
            }
            c = currentMaterial.getAmbient();
            GLES20.glUniform3f(ambientColorParam,
                (float)c.r, (float)c.g, (float)c.b);

            diffuseColorParam =
                GLES20.glGetUniformLocation(shaderId, "diffuseColor");
            checkGlError("glGetUniformLocation diffuseColor");
            if ( diffuseColorParam == -1 ) {
                throw new RuntimeException(
                    "Could not get uniform location for diffuseColor");
            }
            c = currentMaterial.getDiffuse();
            GLES20.glUniform3f(diffuseColorParam,
                (float)c.r, (float)c.g, (float)c.b);

            specularColorParam =
                GLES20.glGetUniformLocation(shaderId, "specularColor");
            checkGlError("glGetUniformLocation specularColor");
            if ( specularColorParam == -1 ) {
                throw new RuntimeException(
                    "Could not get uniform location for specularColor");
            }
            c = currentMaterial.getSpecular();
            GLES20.glUniform3f(specularColorParam,
                (float)c.r, (float)c.g, (float)c.b);

            phongExponentParam =
                GLES20.glGetUniformLocation(shaderId, "phongExponent");
            checkGlError("glGetUniformLocation phongExponent");
            if ( phongExponentParam == -1 ) {
                throw new RuntimeException(
                    "Could not get uniform location for phongExponent");
            }
            GLES20.glUniform1f(phongExponentParam,
                (float)currentMaterial.getPhongExponent());

            numberOfLightsParam =
                GLES20.glGetUniformLocation(shaderId, "numberOfLights");
            checkGlError("glGetUniformLocation numberOfLights");
            if ( numberOfLightsParam == -1 ) {
                throw new RuntimeException(
                    "Could not get uniform location for numberOfLights");
            }
            GLES20.glUniform1i(numberOfLightsParam, lights.size());

            lightPositionsGlobalParam =
                GLES20.glGetUniformLocation(shaderId, "lightPositionsGlobal");
            checkGlError("glGetUniformLocation lightPositionsGlobal");
            if ( lightPositionsGlobalParam == -1 ) {
                throw new RuntimeException(
                    "Could not get uniform location for lightPositionsGlobal");
            }

            float array[] = new float[3*lights.size()];
            for ( int i = 0; i < lights.size(); i++ ) {
                Vector3D p = lights.get(i).getPosition();
                array[3*i + 0] = (float)p.x;
                array[3*i + 1] = (float)p.y;
                array[3*i + 2] = (float)p.z;
            }
            GLES20.glUniform3fv(lightPositionsGlobalParam, lights.size(), array, 0);

            cameraPositionGlobalParam =
                GLES20.glGetUniformLocation(shaderId, "cameraPositionGlobal");
            checkGlError("glGetUniformLocation cameraPositionGlobal");
            if ( cameraPositionGlobalParam == -1 ) {
                throw new RuntimeException(
                    "Could not get uniform location for cameraPositionGlobal");
            }
            if ( currentCamera != null ) {
                Vector3D p = currentCamera.getPosition();
                GLES20.glUniform3f(cameraPositionGlobalParam,
                       (float)p.x, (float)p.y, (float)p.z);
            }
        }
        else {
            NObjectParam = -1;
            ambientColorParam = -1;
            diffuseColorParam = -1;

            specularColorParam = -1;
            modelViewLocalParam = -1;
            modelViewITLocalParam = -1;
            lightPositionsGlobalParam = -1;
            numberOfLightsParam = -1;
            withTextureParam = -1;
            cameraPositionGlobalParam = -1;
            phongExponentParam = -1;
        }

        activateTransformationMatrices();
    }

    public static void activateTransformationMatrices()
    {
        Matrix.multiplyMM(modelViewProjectionLocal, 0,
            projectionMatrix, 0, modelViewMatrix, 0);
        GLES20.glUniformMatrix4fv(modelViewProjectionLocalParam, 1, false, 
            modelViewProjectionLocal, 0);
        checkGlError("modelViewProjectionLocalParam");

        Matrix.invertM(transientMatrix, 0, modelViewMatrix, 0);
        Matrix.transposeM(modelViewITMatrix, 0, transientMatrix, 0);

        if ( modelViewLocalParam != -1 ) {
            GLES20.glUniformMatrix4fv(modelViewLocalParam, 
                1, false, modelViewMatrix, 0);
            checkGlError("modelViewLocalParam");
        }

        if ( modelViewITLocalParam != -1 ) {
            GLES20.glUniformMatrix4fv(modelViewITLocalParam, 1, false, 
                modelViewITMatrix, 0);
            checkGlError("modelViewITLocalParam");
        }

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
        switch ( currentMatrixMode ) {
          case GL_MODELVIEW:
            matrixStack.push(modelViewMatrix);
            break;
          case GL_PROJECTION:
            matrixStack.push(projectionMatrix);
            break;
        }
    }

    public static void glPopMatrix()
    {
        switch ( currentMatrixMode ) {
          case GL_MODELVIEW:
            modelViewMatrix = matrixStack.pop();
            break;
          case GL_PROJECTION:
            projectionMatrix = matrixStack.pop();
            break;
        }
        activateTransformationMatrices();
    }

    public static void glScaled(double sx, double sy, double sz)
    {
        Matrix.setIdentityM(transientMatrix, 0);       
        Matrix.scaleM(transientMatrix, 0, 
                      (float)sx, (float)sy, (float)sz);
        copyMatrix(originalMatrix, modelViewMatrix);
        switch ( currentMatrixMode ) {
        case GL_MODELVIEW:
            Matrix.multiplyMM(modelViewMatrix, 0,
                              originalMatrix, 0, transientMatrix, 0);
            break;
        case GL_PROJECTION:
            Matrix.multiplyMM(projectionMatrix, 0,
                              transientMatrix, 0, projectionMatrix, 0);
            break;
        }
        activateTransformationMatrices();
    }

    public static void copyMatrix(float dest[], float origin[])
    {
        int i;

        for ( i = 0; i < 16; i++ ) {
            dest[i] = origin[i];
        }
    }

    public static void glTranslated(double tx, double ty, double tz)
    {
        Matrix.setIdentityM(transientMatrix, 0);       
        Matrix.translateM(transientMatrix, 0,
                          (float)tx, (float)ty, (float)tz);
        copyMatrix(originalMatrix, modelViewMatrix);
        switch ( currentMatrixMode ) {
        case GL_MODELVIEW:
            //Matrix.multiplyMM(modelViewMatrix, 0,
            //    transientMatrix, 0, modelViewMatrix, 0);
            Matrix.multiplyMM(modelViewMatrix, 0,
                              originalMatrix, 0, transientMatrix, 0);
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
        Matrix.setIdentityM(transientMatrix, 0);       
        Matrix.setRotateM(transientMatrix, 0, 
                          (float)angleDegrees, (float)ax, (float)ay, (float)az);
        copyMatrix(originalMatrix, modelViewMatrix);
        switch ( currentMatrixMode ) {
        case GL_MODELVIEW:
            Matrix.multiplyMM(modelViewMatrix, 0,
                              originalMatrix, 0, transientMatrix, 0);
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

    protected static void drawVertices3Position2Uv(
        FloatBuffer verticesBufferedArray,
        int primitive, int numberOfElements, int vertexSizeInBytes)
    {
        //-----------------------------------------------------------------
        // Send geometry to GPU
        // glVertex3d
        verticesBufferedArray.position(0);
        GLES20.glVertexAttribPointer(PObjectParam, 3, GLES20.GL_FLOAT, 
                             false, vertexSizeInBytes, verticesBufferedArray);
        checkGlError("glVertexAttribPointer PObject");
        GLES20.glEnableVertexAttribArray(PObjectParam);
        checkGlError("glEnableVertexAttribArray PObjectParam");

        // glTexCoord2d
        verticesBufferedArray.position(3);
        GLES20.glVertexAttribPointer(uvVertexTextureCoordinateParam, 2, 
                                     GLES20.GL_FLOAT, false, vertexSizeInBytes, 
                                     verticesBufferedArray);
        checkGlError(
            "glVertexAttribPointer uvVertexTextureCoordinateParam");
        GLES20.glEnableVertexAttribArray(uvVertexTextureCoordinateParam);
        checkGlError("glEnableVertexAttribArray uvVertexTextureCoordinateParam");

        //-----------------------------------------------------------------
        // Draw geometry
        GLES20.glDrawArrays(primitive, 0, numberOfElements);
        checkGlError("glDrawArrays");
    }

    protected static void drawVertices3Position3Color2Uv(
        FloatBuffer verticesBufferedArray,
        int primitive, int numberOfElements, int vertexSizeInBytes)
    {
        //-----------------------------------------------------------------
        // Send geometry to GPU
        // glVertex3d
        verticesBufferedArray.position(0);
        GLES20.glEnableVertexAttribArray(PObjectParam);
        checkGlError("glEnableVertexAttribArray PObjectParam");
        GLES20.glVertexAttribPointer(PObjectParam, 3, GLES20.GL_FLOAT, 
                             false, vertexSizeInBytes, verticesBufferedArray);
        checkGlError("glVertexAttribPointer PObject");

        // glColor3d
        verticesBufferedArray.position(3);
        GLES20.glEnableVertexAttribArray(emissionColorParam);
        checkGlError("glEnableVertexAttribArray emissionColorParam");
        GLES20.glVertexAttribPointer(emissionColorParam, 3, GLES20.GL_FLOAT,
                             false, vertexSizeInBytes, verticesBufferedArray);
        checkGlError("glVertexAttribPointer emissionColorParam");

        // glTexCoord2d
        verticesBufferedArray.position(6);
        GLES20.glEnableVertexAttribArray(uvVertexTextureCoordinateParam);
        checkGlError("glEnableVertexAttribArray uvVertexTextureCoordinateParam");
        GLES20.glVertexAttribPointer(uvVertexTextureCoordinateParam, 2, 
                                     GLES20.GL_FLOAT, false, vertexSizeInBytes, 
                                     verticesBufferedArray);
        checkGlError("glVertexAttribPointer uvVertexTextureCoordinateParam");

        //-----------------------------------------------------------------
        // Draw geometry
        GLES20.glDrawArrays(primitive, 0, numberOfElements);
        checkGlError("glDrawArrays");
    }

    protected static void drawVertices3Position3Normal2Uv(
        FloatBuffer verticesBufferedArray,
        int primitive, int numberOfElements, int vertexSizeInBytes)
    {
        //-----------------------------------------------------------------
        // Send geometry to GPU
        // glVertex3d
        verticesBufferedArray.position(0);
        GLES20.glEnableVertexAttribArray(PObjectParam);
        checkGlError("glEnableVertexAttribArray PObjectParam");
        GLES20.glVertexAttribPointer(PObjectParam, 3, GLES20.GL_FLOAT, 
                             false, vertexSizeInBytes, verticesBufferedArray);
        checkGlError("glVertexAttribPointer PObject");

        // glNormal3d
        if ( NObjectParam != -1 ) {
            verticesBufferedArray.position(3);
            GLES20.glEnableVertexAttribArray(NObjectParam);
            checkGlError("glEnableVertexAttribArray NObjectParam");
            GLES20.glVertexAttribPointer(NObjectParam, 3, GLES20.GL_FLOAT,
                             false, vertexSizeInBytes, verticesBufferedArray);
            checkGlError("glVertexAttribPointer NObjectParam");
        }

        // glTexCoord2d
        verticesBufferedArray.position(6);
        GLES20.glEnableVertexAttribArray(uvVertexTextureCoordinateParam);
        checkGlError("glEnableVertexAttribArray uvVertexTextureCoordinateParam");
        GLES20.glVertexAttribPointer(uvVertexTextureCoordinateParam, 2, 
                                     GLES20.GL_FLOAT, false, vertexSizeInBytes, 
                                     verticesBufferedArray);
        checkGlError("glVertexAttribPointer uvVertexTextureCoordinateParam");

        //-----------------------------------------------------------------
        // Draw geometry
        GLES20.glDrawArrays(primitive, 0, numberOfElements);
        checkGlError("glDrawArrays");
    }

    protected static void drawVertices3Position3Normal(
            FloatBuffer verticesBufferedArray,
            int primitive, int numberOfElements, int vertexSizeInBytes)
    {
        //-----------------------------------------------------------------
        // Send geometry to GPU
        // glVertex3d
        verticesBufferedArray.position(0);
        GLES20.glEnableVertexAttribArray(PObjectParam);
        checkGlError("glEnableVertexAttribArray PObjectParam");
        GLES20.glVertexAttribPointer(PObjectParam, 3, GLES20.GL_FLOAT, 
                             false, vertexSizeInBytes, verticesBufferedArray);
        checkGlError("glVertexAttribPointer PObject");

        // glNormal3d
        if ( NObjectParam != -1 ) {
            verticesBufferedArray.position(3);
            GLES20.glEnableVertexAttribArray(NObjectParam);
            checkGlError("glEnableVertexAttribArray NObjectParam");
            GLES20.glVertexAttribPointer(NObjectParam, 3, GLES20.GL_FLOAT,
                             false, vertexSizeInBytes, verticesBufferedArray);
            checkGlError("glVertexAttribPointer NObjectParam");
        }

        //-----------------------------------------------------------------
        // Draw geometry
        GLES20.glDrawArrays(primitive, 0, numberOfElements);
        checkGlError("glDrawArrays");
    }

    protected static void drawVertices3Position3Color(
            FloatBuffer verticesBufferedArray,
            int primitive, int numberOfElements, int vertexSizeInBytes)
    {
        //-----------------------------------------------------------------
        // Send geometry to GPU
        // glVertex3d
        verticesBufferedArray.position(0);
        GLES20.glEnableVertexAttribArray(PObjectParam);
        checkGlError("glEnableVertexAttribArray PObjectParam");
        GLES20.glVertexAttribPointer(PObjectParam, 3, GLES20.GL_FLOAT, 
                             false, vertexSizeInBytes, verticesBufferedArray);
        checkGlError("glVertexAttribPointer PObject");

        // glColor3d
        if ( emissionColorParam != -1 ) {
            verticesBufferedArray.position(3);
            GLES20.glEnableVertexAttribArray(emissionColorParam);
            checkGlError("glEnableVertexAttribArray emissionColorParam");
            GLES20.glVertexAttribPointer(emissionColorParam, 3, GLES20.GL_FLOAT,
                             false, vertexSizeInBytes, verticesBufferedArray);
            checkGlError("glVertexAttribPointer emissionColorParam");
        }

        //-----------------------------------------------------------------
        // Draw geometry
        GLES20.glDrawArrays(primitive, 0, numberOfElements);
        checkGlError("glDrawArrays");
    }
    
    protected static void drawVertices3Position3Color3Normal2Uv(
        FloatBuffer verticesBufferedArray,
        int primitive, int numberOfElements, int vertexSizeInBytes)
    {
        //-----------------------------------------------------------------
        // Send geometry to GPU
        // glVertex3d
        verticesBufferedArray.position(0);
        GLES20.glEnableVertexAttribArray(PObjectParam);
        checkGlError("glEnableVertexAttribArray PObjectParam");
        GLES20.glVertexAttribPointer(PObjectParam, 3, GLES20.GL_FLOAT, 
                             false, vertexSizeInBytes, verticesBufferedArray);
        checkGlError("glVertexAttribPointer PObject");

        // glColor3d
        verticesBufferedArray.position(3);
        GLES20.glEnableVertexAttribArray(emissionColorParam);
        checkGlError("glEnableVertexAttribArray emissionColorParam");
        GLES20.glVertexAttribPointer(emissionColorParam, 3, GLES20.GL_FLOAT,
                             false, vertexSizeInBytes, verticesBufferedArray);
        checkGlError("glVertexAttribPointer emissionColorParam");

        // glNormal3d
        if ( NObjectParam != -1 ) {
            verticesBufferedArray.position(6);
            GLES20.glEnableVertexAttribArray(NObjectParam);
            checkGlError("glEnableVertexAttribArray NObjectParam");
            GLES20.glVertexAttribPointer(NObjectParam, 3, GLES20.GL_FLOAT,
                             false, vertexSizeInBytes, verticesBufferedArray);
            checkGlError("glVertexAttribPointer NObjectParam");
        }

        // glTexCoord2d
        verticesBufferedArray.position(9);
        GLES20.glEnableVertexAttribArray(uvVertexTextureCoordinateParam);
        checkGlError("glEnableVertexAttribArray uvVertexTextureCoordinateParam");
        GLES20.glVertexAttribPointer(uvVertexTextureCoordinateParam, 2, 
                                     GLES20.GL_FLOAT, false, vertexSizeInBytes, 
                                     verticesBufferedArray);
        checkGlError("glVertexAttribPointer uvVertexTextureCoordinateParam");

        //-----------------------------------------------------------------
        // Draw geometry
        GLES20.glDrawArrays(primitive, 0, numberOfElements);
        checkGlError("glDrawArrays");
    }

    protected static void
    vertex3Position3Color2Uv(float arr[], int index,
        double x, double y, double z,
        double r, double g, double b,
        double u, double v)
    {
        arr[index] = (float)x;    index++;
        arr[index] = (float)y;    index++;
        arr[index] = (float)z;    index++;
        arr[index] = (float)r;    index++;
        arr[index] = (float)g;    index++;
        arr[index] = (float)b;    index++;
        arr[index] = (float)u;    index++;
        arr[index] = (float)v;    index++;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
