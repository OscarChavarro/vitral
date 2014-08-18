//===========================================================================

package vsdk.toolkit.render.androidgles20;

// Basic Java classes
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Stack;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

// Android classes
import android.util.Log;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.opengl.GLES20;
import android.opengl.Matrix;

// Sandbox
//import vitral.application.R;

// VSDK classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.io.PersistenceElement;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.render.RenderingElement;

public class AndroidGLES20Renderer extends RenderingElement
{
    private static final String TAG = "AndroidGLES20Renderer";

    protected static final int MODE_3POSITION = 1;
    protected static final int MODE_3POSITION_3NORMAL_2UV = 2;
    protected static final int MODE_3POSITION_3COLOR = 3;
    protected static final int MODE_3POSITION_3COLOR_3NORMAL_2UV = 4;

    // Common values
    protected static final int FLOAT_SIZE_IN_BYTES = 4;
    
    private static Object unitSquare = null;

    // OpenGL-ES2.0 state
    public static int AndroidGLES20GpuProgramConstant;
    public static int AndroidGLES20GpuProgramGouraud;
    public static int AndroidGLES20GpuProgramPhong;
    public static int AndroidGLES20GpuProgramPhongBump;
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
    private static RendererConfiguration qualitySelection;
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
    private static int lightColorsGlobalParam;
    private static int numberOfLightsParam;
    private static int withTextureParam;
    private static int withVertexColorsParam;
    private static int cameraPositionGlobalParam;
    private static int sTextureParam;
    private static int sBumpParam;

    /// Reference to all known objects and their corresponding display list data
    private static HashMap<Object, AndroidGLES20DisplayList> displayLists;
    
    protected static String errorMessage;

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
        qualitySelection.setBumpMap(false);
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
        
        displayLists = new HashMap<Object, AndroidGLES20DisplayList>();
        errorMessage = "No error detected";
    }

    public static void executeCompiledDisplayList(Object o)
    {
        AndroidGLES20DisplayList displayList;
        
        displayList = displayLists.get(o);
        
        if ( displayList == null ) {
            return;
        }
        
        int i;

        setRendererConfiguration(displayList.getCorrespondingQuality());
        
        // Draw elements on display list
        for ( i = 0; i < displayList.getVboIds().size(); i++ ) {
            Material m = displayList.getVboMaterials().get(i);
            
            if ( m != null ) {
                AndroidGLES20MaterialRenderer.activate(m);
            }

            //-----------------------------------------------------------------
            // Activate VBO
            GLES20.glBindBuffer(
                GLES20.GL_ARRAY_BUFFER, displayList.getVboIds().get(i));
            checkGlError("glBindBuffer(" + displayList.getVboIds().get(i) + ")");
            
            if ( displayList.getIndexIds().size() == 0 ) {
                // Display VBO
                switch ( displayList.getVertexMode() ) {
                  case MODE_3POSITION:
                    drawVertices3Position(
                        displayList.getVboPrimitives().get(i),
                        displayList.getVboSizes().get(i));
                    break;
                  case MODE_3POSITION_3COLOR_3NORMAL_2UV:
                  default:
                    drawVertices3Position3Color3Normal2Uv(
                        displayList.getVboPrimitives().get(i),
                        displayList.getVboSizes().get(i));
                    break;
                }
            }

            // Display indexed primitives
            if ( displayList.getIndexIds().size() != 0 ) {
                // Bind Attributes
                switch ( displayList.getVertexMode() ) {
                  case MODE_3POSITION:
                    activateVertices3Position();
                    break;
                  case MODE_3POSITION_3COLOR_3NORMAL_2UV:
                  default:
                    activateVertices3Position3Color3Normal2Uv();
                    break;
                }

                // Draw
                int ibo = displayList.getIndexIds().get(i);
                GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo);
                GLES20.glDrawElements(
                    displayList.getVboPrimitives().get(i), 
                    displayList.getIboSizes().get(i), 
                    GLES20.GL_UNSIGNED_SHORT, 0);
                checkGlError("glDrawElements()");
            }

            // Disable current VBO
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
            checkGlError("glBindBuffer(0) for vertices");
 
            // Disable current IBO
            if ( displayList.getIndexIds().size() != 0 ) {
                GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
                checkGlError("glBindBuffer(0) for indices");
            }

        }
    }

    /**
    This method should be called after every call to a texture activation
    (glBindTexture).
    */
    public static void activateDefaultTextureParameters()
    {
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, 
            GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_REPEAT);
    }
    
    protected static void activateVertices3Position3Color3Normal2Uv()
    {
        int vertexSizeInBytes = FLOAT_SIZE_IN_BYTES * 11;
        
        // glVertex3d
        GLES20.glVertexAttribPointer(PObjectParam, 3, GLES20.GL_FLOAT,
                false, vertexSizeInBytes, 0);
        checkGlError("glVertexAttribPointer PObject");
        GLES20.glEnableVertexAttribArray(PObjectParam);
        checkGlError("glEnableVertexAttribArray PObjectParam");
        
        // glColor3d
        if (emissionColorParam != -1) {
            GLES20.glVertexAttribPointer(emissionColorParam, 3, GLES20.GL_FLOAT,
                    false, vertexSizeInBytes, 3 * FLOAT_SIZE_IN_BYTES);
            checkGlError("glVertexAttribPointer emissionColorParam");
            GLES20.glEnableVertexAttribArray(emissionColorParam);
            checkGlError("glEnableVertexAttribArray emissionColorParam");
        }
        
        // glNormal3d
        if (NObjectParam != -1) {
            GLES20.glVertexAttribPointer(NObjectParam, 3, GLES20.GL_FLOAT,
                    false, vertexSizeInBytes, 6 * FLOAT_SIZE_IN_BYTES);
            checkGlError("glVertexAttribPointer NObjectParam");
            GLES20.glEnableVertexAttribArray(NObjectParam);
            checkGlError("glEnableVertexAttribArray NObjectParam");
        }
        
        // glTexCoord2d
        if (uvVertexTextureCoordinateParam != -1) {
            GLES20.glVertexAttribPointer(uvVertexTextureCoordinateParam, 2,
                    GLES20.GL_FLOAT, false, vertexSizeInBytes,
                    9 * FLOAT_SIZE_IN_BYTES);
            checkGlError(
                    "glVertexAttribPointer uvVertexTextureCoordinateParam");
            GLES20.glEnableVertexAttribArray(uvVertexTextureCoordinateParam);
            checkGlError(
                    "glEnableVertexAttribArray uvVertexTextureCoordinateParam");
        }        
    }
    
    /**
    Draws vertices from a VBO previously loaded at GPU, from vertex array
    currently binded (activated with glBindBuffer).
    @param primitive
    @param numberOfElements
    */
    protected static void drawVertices3Position3Color3Normal2Uv(
        int primitive, int numberOfElements) {
        //-----------------------------------------------------------------
        // Configure shaders parameters to process data on VBO
        activateVertices3Position3Color3Normal2Uv();
        
        //-----------------------------------------------------------------
        // Draw geometry from VBO preloaded at GPU
        GLES20.glDrawArrays(primitive, 0, numberOfElements);
        checkGlError("glDrawArrays");
    }

    protected static boolean isObjectRegisteredWithADisplayList(Object o)
    {
        return displayLists.containsKey(o);
    }
    
    protected static void registerObjectWithADisplayList(
        Object key, AndroidGLES20DisplayList value)
    {
        displayLists.put(key, value);
    }
    
    
    public static void setRendererConfiguration(RendererConfiguration source)
    {
        if ( source != null ) {
            qualitySelection.clone(source);
            activateShaders();
        }
    }

    public static void checkGlError(String op) {
        int error;
        error = GLES20.glGetError();
        String name = "UNKNOWN GL ERROR";

        while ( error != GLES20.GL_NO_ERROR ) {
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
            errorsDetected = true;
            Log.e(TAG, op + ": glError " + error + " : " + name + " Thread: " + Thread.currentThread().getName());
            errorMessage = "" + op + ": glError " + error + " : " + name;
            //throw new RuntimeException(op + ": glError " + error);
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

    public static boolean
    createDefaultAutomaticAndroidGLES20Shaders(Context ctx)
    {
        String vertexShaderSource;
        String pixelShaderSource;

        //- Create shader programs ----------------------------------------
        AssetManager loader;
        
        loader = ctx.getResources().getAssets();
        
        try {
            vertexShaderSource = loadAsStringTrimmingComments(
                loader.open("constantvertexshader.glsl"));
            pixelShaderSource = loadAsStringTrimmingComments(
                loader.open("constantpixelshader.glsl"));
            AndroidGLES20GpuProgramConstant
                = createProgram(vertexShaderSource, pixelShaderSource);
            if ( AndroidGLES20GpuProgramConstant == 0 ) {
                System.err.println("ERROR CREATING CONSTANT SHADER!");
                return false;
            }

            vertexShaderSource = loadAsStringTrimmingComments(
                loader.open("gouraudvertexshader.glsl"));
            pixelShaderSource = loadAsStringTrimmingComments(
                loader.open("gouraudpixelshader.glsl"));
            AndroidGLES20GpuProgramGouraud
                = createProgram(vertexShaderSource, pixelShaderSource);
            if ( AndroidGLES20GpuProgramGouraud == 0 ) {
                System.err.println("ERROR CREATING GOURAUD SHADER!");
                return false;
            }

            vertexShaderSource = loadAsStringTrimmingComments(
                loader.open("phongvertexshader.glsl"));
            pixelShaderSource = loadAsStringTrimmingComments(
                loader.open("phongpixelshader.glsl"));
            AndroidGLES20GpuProgramPhong
                = createProgram(vertexShaderSource, pixelShaderSource);
            if ( AndroidGLES20GpuProgramPhong == 0 ) {
                System.err.println("ERROR CREATING PHONG SHADER!");
                return false;
            }

            vertexShaderSource = loadAsStringTrimmingComments(
                loader.open("phongbumpvertexshader.glsl"));
            pixelShaderSource = loadAsStringTrimmingComments(
                loader.open("phongbumppixelshader.glsl"));
            AndroidGLES20GpuProgramPhongBump
                = createProgram(vertexShaderSource, pixelShaderSource);
            if ( AndroidGLES20GpuProgramPhong == 0 ) {
                System.err.println("ERROR CREATING PHONG BUMP SHADER!");
                return false;
            }
        }
        catch ( Resources.NotFoundException e ) {
            VSDK.reportMessageWithException(null, VSDK.FATAL_ERROR, "AndroidGLES20Renderer.createDefaultAutomaticAndroidGLES20Shaders", "Error loading assets", e);
        }
        catch ( IOException e ) {
            VSDK.reportMessageWithException(null, VSDK.FATAL_ERROR, "AndroidGLES20Renderer.createDefaultAutomaticAndroidGLES20Shaders", "Error loading assets", e);
        }
        //- Create parameters ---------------------------------------------
        activateShaders();
        return true;
    }

    protected static void activateShaders()
    {
        //- Select current shaders programs from rendering configuration --
        int shaderId; // = AndroidGLES20GpuProgramConstant;
        String selectedShaderName; // = "<invalid>";

        //System.out.print("Seleccionando shader " + qualitySelection.getShadingType() + " ... : ");

        if ( qualitySelection.getShadingType() == 
            RendererConfiguration.SHADING_TYPE_GOURAUD ||
             qualitySelection.getShadingType() == 
            RendererConfiguration.SHADING_TYPE_FLAT ) {
            shaderId = AndroidGLES20GpuProgramGouraud;
            selectedShaderName = "Gouraud";
        }
        else if ( qualitySelection.getShadingType() == 
            RendererConfiguration.SHADING_TYPE_PHONG ) {
            if ( qualitySelection.isBumpMapSet() ) {
                shaderId = AndroidGLES20GpuProgramPhongBump;
                selectedShaderName = "PhongBump";
            }
            else {
                shaderId = AndroidGLES20GpuProgramPhong; 
                selectedShaderName = "Phong";
            }
        }
        else {
            shaderId = AndroidGLES20GpuProgramConstant;
            selectedShaderName = "Constant";
        }

        //- Activate selected shader programs -----------------------------
        GLES20.glUseProgram(shaderId);
        checkGlError("glUseProgram");

        //- Activate shader parameters ------------------------------------
        modelViewProjectionLocalParam = -1;
        PObjectParam = -1;
        sTextureParam = -1;
        sBumpParam = -1;
        NObjectParam = -1;
        ambientColorParam = -1;
        diffuseColorParam = -1;
        specularColorParam = -1;
        modelViewLocalParam = -1;
        modelViewITLocalParam = -1;
        lightPositionsGlobalParam = -1;
        lightColorsGlobalParam = -1;
        numberOfLightsParam = -1;
        withTextureParam = -1;
        withVertexColorsParam = -1;
        cameraPositionGlobalParam = -1;
        phongExponentParam = -1;
        emissionColorParam = -1;
        uvVertexTextureCoordinateParam = -1;

        modelViewProjectionLocalParam = GLES20.glGetUniformLocation(
            shaderId, "modelViewProjectionLocal");
        checkGlError("glGetUniformLocation modelViewProjectionLocal");
        if ( modelViewProjectionLocalParam == -1 ) {
            throw new RuntimeException(
                "Could not get attrib location for modelViewProjectionLocal for shader " + selectedShaderName);
        }

        PObjectParam = GLES20.glGetAttribLocation(shaderId, "PObject");
        checkGlError("glGetAttribLocation PObject");
        if ( PObjectParam == -1 ) {
            throw new RuntimeException(
                "Could not get attrib location for PObject for shader " + selectedShaderName);
        }

        if ( qualitySelection.isTextureSet() ) {
            uvVertexTextureCoordinateParam = GLES20.glGetAttribLocation(
                shaderId, "uvVertexTextureCoordinate");
            checkGlError("glGetAttribLocation uvVertexTextureCoordinate");
            if ( uvVertexTextureCoordinateParam == -1 ) {
                throw new RuntimeException(
                    "Could not get attrib location for uvVertexTextureCoordinate for shader " + selectedShaderName);
            }
        }

        withTextureParam = GLES20.glGetUniformLocation(shaderId, "withTexture");
        checkGlError("glGetUniformLocation withTexture");
        if ( withTextureParam != -1 ) {
            GLES20.glUniform1i(withTextureParam, 
                qualitySelection.isTextureSet()?1:0);
        }

        withVertexColorsParam = GLES20.glGetUniformLocation(shaderId, "withVertexColors");
        checkGlError("glGetUniformLocation withVertexColors");
        if ( withVertexColorsParam != -1 ) {
            GLES20.glUniform1i(withVertexColorsParam, qualitySelection.getUseVertexColors()?1:0);
        }

        if ( qualitySelection.getShadingType() == 
            RendererConfiguration.SHADING_TYPE_NOLIGHT ||
            qualitySelection.getShadingType() == 
            RendererConfiguration.SHADING_TYPE_PHONG ) {
            emissionColorParam = 
                GLES20.glGetAttribLocation(shaderId, "emissionColor");
            checkGlError("glGetAttribLocation emissionColor");
            if ( emissionColorParam == -1 ) {
                throw new RuntimeException(
                    "Could not get attrib location for emissionColor for shader " + selectedShaderName);
            }
        }

        if ( qualitySelection.getShadingType() == 
            RendererConfiguration.SHADING_TYPE_PHONG && 
             qualitySelection.isBumpMapSet() ) {
            sTextureParam = 
                GLES20.glGetUniformLocation(shaderId, "sTexture");
            checkGlError("glGetUniformLocation sTexture");
            GLES20.glUniform1i(sTextureParam, 0);
            if ( sTextureParam == -1 ) {
                throw new RuntimeException(
                    "Could not get attrib location for sTexture for shader " + selectedShaderName);
            }

            sBumpParam = 
                GLES20.glGetUniformLocation(shaderId, "sBump");
            checkGlError("glGetUniformLocation sBump");
            GLES20.glUniform1i(sBumpParam, 1);
            if ( sBumpParam == -1 ) {
                throw new RuntimeException(
                    "Could not get attrib location for sBump for shader " + selectedShaderName);
            }
        }
        
        ColorRgb c;

        if ( qualitySelection.getShadingType() == 
            RendererConfiguration.SHADING_TYPE_GOURAUD ||
             qualitySelection.getShadingType() == 
            RendererConfiguration.SHADING_TYPE_PHONG ||
             qualitySelection.getShadingType() == 
            RendererConfiguration.SHADING_TYPE_FLAT ||
             qualitySelection.getShadingType() == 
            RendererConfiguration.SHADING_TYPE_NOLIGHT ) {
            diffuseColorParam =
                GLES20.glGetUniformLocation(shaderId, "diffuseColor");
            checkGlError("glGetUniformLocation diffuseColor");
            if ( diffuseColorParam == -1 ) {
                throw new RuntimeException(
                    "Could not get uniform location for diffuseColor for shader " + selectedShaderName);
            }
            c = currentMaterial.getDiffuse();
            GLES20.glUniform3f(diffuseColorParam,
                (float)c.r, (float)c.g, (float)c.b);
        }
    
        if ( qualitySelection.getShadingType() == 
            RendererConfiguration.SHADING_TYPE_GOURAUD ||
             qualitySelection.getShadingType() == 
            RendererConfiguration.SHADING_TYPE_PHONG ||
             qualitySelection.getShadingType() == 
            RendererConfiguration.SHADING_TYPE_FLAT ) {

            // Activate normal parameter
            NObjectParam = GLES20.glGetAttribLocation(shaderId, "NObject");
            checkGlError("glGetAttribLocation NObject");
            if ( NObjectParam == -1 ) {
                throw new RuntimeException(
                    "Could not get attrib location for NObject for shader " + selectedShaderName);
            }

            modelViewLocalParam = GLES20.glGetUniformLocation(
                shaderId, "modelViewLocal");
            checkGlError("glGetUniformLocation modelViewLocal");
            if ( modelViewLocalParam == -1 ) {
                throw new RuntimeException(
                    "Could not get attrib location for modelViewLocalParam for shader " + selectedShaderName);
            }

            modelViewITLocalParam = GLES20.glGetUniformLocation(
                shaderId, "modelViewITLocal");
            checkGlError("glGetUniformLocation modelViewITLocal");
            if ( modelViewITLocalParam == -1 ) {
                throw new RuntimeException(
                    "Could not get attrib location for modelViewITLocalParam for shader " + selectedShaderName);
            }

            // Activate material parameters
            ambientColorParam =
                GLES20.glGetUniformLocation(shaderId, "ambientColor");
            checkGlError("glGetUniformLocation ambientColor");
            if ( ambientColorParam == -1 ) {
                throw new RuntimeException(
                    "Could not get uniform location for ambientColor for shader " + selectedShaderName);
            }
            c = currentMaterial.getAmbient();
            GLES20.glUniform3f(ambientColorParam,
                (float)c.r, (float)c.g, (float)c.b);

            specularColorParam =
                GLES20.glGetUniformLocation(shaderId, "specularColor");
            checkGlError("glGetUniformLocation specularColor");
            if ( specularColorParam == -1 ) {
                throw new RuntimeException(
                    "Could not get uniform location for specularColor for shader " + selectedShaderName);
            }
            c = currentMaterial.getSpecular();
            GLES20.glUniform3f(specularColorParam,
                (float)c.r, (float)c.g, (float)c.b);

            phongExponentParam =
                GLES20.glGetUniformLocation(shaderId, "phongExponent");
            checkGlError("glGetUniformLocation phongExponent");
            if ( phongExponentParam == -1 ) {
                throw new RuntimeException(
                    "Could not get uniform location for phongExponent for shader " + selectedShaderName);
            }
            GLES20.glUniform1f(phongExponentParam,
                (float)currentMaterial.getPhongExponent());

            numberOfLightsParam =
                GLES20.glGetUniformLocation(shaderId, "numberOfLights");
            checkGlError("glGetUniformLocation numberOfLights");
            if ( numberOfLightsParam == -1 ) {
                throw new RuntimeException(
                    "Could not get uniform location for numberOfLights for shader " + selectedShaderName);
            }
            GLES20.glUniform1i(numberOfLightsParam, lights.size());

            lightPositionsGlobalParam =
                GLES20.glGetUniformLocation(shaderId, "lightPositionsGlobal");
            checkGlError("glGetUniformLocation lightPositionsGlobal");
            if ( lightPositionsGlobalParam == -1 ) {
                throw new RuntimeException(
                    "Could not get uniform location for lightPositionsGlobal for shader " + selectedShaderName);
            }

            lightColorsGlobalParam =
                GLES20.glGetUniformLocation(shaderId, "lightColorsGlobal");
            checkGlError("glGetUniformLocation lightColorsGlobal");
            if ( lightColorsGlobalParam == -1 ) {
                throw new RuntimeException(
                    "Could not get uniform location for lightColorsGlobal for shader " + selectedShaderName);
            }

            float array[] = new float[3*lights.size()];
            for ( int i = 0; i < lights.size(); i++ ) {
                Vector3D p = lights.get(i).getPosition();
                array[3*i + 0] = (float)p.x;
                array[3*i + 1] = (float)p.y;
                array[3*i + 2] = (float)p.z;
            }
            GLES20.glUniform3fv(lightPositionsGlobalParam, lights.size(), array, 0);

            array = new float[3*lights.size()];
            for ( int i = 0; i < lights.size(); i++ ) {
                c = lights.get(i).getSpecular();
                array[3*i + 0] = (float)c.r;
                array[3*i + 1] = (float)c.g;
                array[3*i + 2] = (float)c.b;
            }
            GLES20.glUniform3fv(lightColorsGlobalParam, lights.size(), array, 0);

            cameraPositionGlobalParam =
                GLES20.glGetUniformLocation(shaderId, "cameraPositionGlobal");
            checkGlError("glGetUniformLocation cameraPositionGlobal");
            if ( cameraPositionGlobalParam == -1 ) {
                throw new RuntimeException(
                    "Could not get uniform location for cameraPositionGlobal for shader " + selectedShaderName);
            }
            if ( currentCamera != null ) {
                Vector3D p = currentCamera.getPosition();
                GLES20.glUniform3f(cameraPositionGlobalParam,
                       (float)p.x, (float)p.y, (float)p.z);
            }
        }

        activateTransformationMatrices();
    }

    public static void activateTransformationMatrices()
    {
        if ( modelViewProjectionLocalParam != -1 ) {
            Matrix.multiplyMM(modelViewProjectionLocal, 0,
                projectionMatrix, 0, modelViewMatrix, 0);
            GLES20.glUniformMatrix4fv(modelViewProjectionLocalParam, 1, false,
                modelViewProjectionLocal, 0);
            checkGlError("modelViewProjectionLocalParam");
        }

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
        switch ( currentMatrixMode ) {
          case GL_MODELVIEW:
            copyMatrix(originalMatrix, modelViewMatrix);
            Matrix.multiplyMM(modelViewMatrix, 0,
                              originalMatrix, 0, transientMatrix, 0);
            break;
          case GL_PROJECTION:
            copyMatrix(originalMatrix, projectionMatrix);
            Matrix.multiplyMM(projectionMatrix, 0,
                              originalMatrix, 0, transientMatrix, 0);
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

    protected static void activateVertices3Position()
    {
        int vertexSizeInBytes = FLOAT_SIZE_IN_BYTES * 3;

        // glVertex3d
        GLES20.glVertexAttribPointer(PObjectParam, 3, GLES20.GL_FLOAT, 
                             false, vertexSizeInBytes, 0);
        checkGlError("glVertexAttribPointer PObject");
        GLES20.glEnableVertexAttribArray(PObjectParam);
        checkGlError("glEnableVertexAttribArray PObjectParam");
    }
    
    /**
    Draws vertices from a VBO previously loaded at GPU, from vertex array
    currently binded (activated with glBindBuffer).
    @param primitive
    @param numberOfElements
    */
    protected static void drawVertices3Position(
        int primitive, int numberOfElements)
    {
        //-----------------------------------------------------------------
        // Send geometry to GPU
        activateVertices3Position();
        
        //-----------------------------------------------------------------
        // Draw geometry from VBO preloaded at GPU
        GLES20.glDrawArrays(primitive, 0, numberOfElements);
        checkGlError("glDrawArrays");
    }

    protected static void drawVertices3Position(
        FloatBuffer verticesBufferedArray,
        int primitive, int numberOfElements)
    {
        int vertexSizeInBytes = FLOAT_SIZE_IN_BYTES * 3;
        
        //-----------------------------------------------------------------
        // Send geometry to GPU
        // glVertex3d
        verticesBufferedArray.position(0);
        GLES20.glVertexAttribPointer(PObjectParam, 3, GLES20.GL_FLOAT, 
                             false, vertexSizeInBytes, verticesBufferedArray);
        checkGlError("glVertexAttribPointer PObject");
        GLES20.glEnableVertexAttribArray(PObjectParam);
        checkGlError("glEnableVertexAttribArray PObjectParam");

        //-----------------------------------------------------------------
        // Draw geometry
        GLES20.glDrawArrays(primitive, 0, numberOfElements);
        checkGlError("glDrawArrays");
    }

    protected static void drawVertices3Position2Uv(
        FloatBuffer verticesBufferedArray,
        int primitive, int numberOfElements)
    {
        int vertexSizeInBytes = FLOAT_SIZE_IN_BYTES * 5;
        
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
        if ( uvVertexTextureCoordinateParam != -1 ) {
            verticesBufferedArray.position(3);
            GLES20.glVertexAttribPointer(uvVertexTextureCoordinateParam, 2, 
                GLES20.GL_FLOAT, false, vertexSizeInBytes, 
                verticesBufferedArray);
            checkGlError(
                "glVertexAttribPointer uvVertexTextureCoordinateParam");
            GLES20.glEnableVertexAttribArray(uvVertexTextureCoordinateParam);
            checkGlError(
                "glEnableVertexAttribArray uvVertexTextureCoordinateParam");
        }

        //-----------------------------------------------------------------
        // Draw geometry
        GLES20.glDrawArrays(primitive, 0, numberOfElements);
        checkGlError("glDrawArrays");
    }

    protected static void drawVertices3Position3Color2Uv(
        FloatBuffer verticesBufferedArray,
        int primitive, int numberOfElements)
    {
        int vertexSizeInBytes = FLOAT_SIZE_IN_BYTES * 8;
        //-----------------------------------------------------------------
        // Send geometry to GPU
        // glVertex3d
        verticesBufferedArray.position(0);
        GLES20.glEnableVertexAttribArray(PObjectParam);
        checkGlError("glEnableVertexAttribArray PObjectParam" + PObjectParam);
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

        // glTexCoord2d
        if ( uvVertexTextureCoordinateParam != -1 ) {
            verticesBufferedArray.position(6);
            GLES20.glEnableVertexAttribArray(uvVertexTextureCoordinateParam);
            checkGlError(
                "glEnableVertexAttribArray uvVertexTextureCoordinateParam");
            GLES20.glVertexAttribPointer(uvVertexTextureCoordinateParam, 2, 
                GLES20.GL_FLOAT, false, vertexSizeInBytes, 
                verticesBufferedArray);
            checkGlError(
                "glVertexAttribPointer uvVertexTextureCoordinateParam");
        }

        //-----------------------------------------------------------------
        // Draw geometry
        GLES20.glDrawArrays(primitive, 0, numberOfElements);
        checkGlError("glDrawArrays");
    }

    protected static void drawVertices3Position3Color3Normal(
        FloatBuffer verticesBufferedArray,
        int primitive, int numberOfElements)
    {
        int vertexSizeInBytes = FLOAT_SIZE_IN_BYTES * 9;
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

        // glNormal3d
        if ( NObjectParam != -1 ) {
            verticesBufferedArray.position(6);
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

    protected static void drawVertices3Position3Normal2Uv(
        FloatBuffer verticesBufferedArray,
        int primitive, int numberOfElements)
    {
        int vertexSizeInBytes = FLOAT_SIZE_IN_BYTES * 8;
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
        if ( uvVertexTextureCoordinateParam != -1 ) {
            verticesBufferedArray.position(6);
            GLES20.glEnableVertexAttribArray(uvVertexTextureCoordinateParam);
            checkGlError(
                "glEnableVertexAttribArray uvVertexTextureCoordinateParam");
            GLES20.glVertexAttribPointer(uvVertexTextureCoordinateParam, 2, 
                GLES20.GL_FLOAT, false, vertexSizeInBytes, 
                verticesBufferedArray);
            checkGlError(
                "glVertexAttribPointer uvVertexTextureCoordinateParam");
        }

        //-----------------------------------------------------------------
        // Draw geometry
        GLES20.glDrawArrays(primitive, 0, numberOfElements);
        checkGlError("glDrawArrays");
    }

    protected static void drawVertices3Position3Normal(
            FloatBuffer verticesBufferedArray,
            int primitive, int numberOfElements)
    {
        int vertexSizeInBytes = FLOAT_SIZE_IN_BYTES * 6;
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
            int primitive, int numberOfElements)
    {
        int vertexSizeInBytes = FLOAT_SIZE_IN_BYTES * 6;
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
    
    /**
    Draws vertices from a VBO not loaded at GPU (from a native memory float
    buffer).
    @param verticesBufferedArray
    @param primitive
    @param numberOfElements
    */
    protected static void drawVertices3Position3Color3Normal2Uv(
        FloatBuffer verticesBufferedArray,
        int primitive, int numberOfElements)
    {
        int vertexSizeInBytes = FLOAT_SIZE_IN_BYTES * 11;
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
        if ( uvVertexTextureCoordinateParam != -1 ) {
            verticesBufferedArray.position(9);
            GLES20.glEnableVertexAttribArray(uvVertexTextureCoordinateParam);
            checkGlError(
                "glEnableVertexAttribArray uvVertexTextureCoordinateParam");
            GLES20.glVertexAttribPointer(uvVertexTextureCoordinateParam, 2, 
                GLES20.GL_FLOAT, false, vertexSizeInBytes, 
                verticesBufferedArray);
            checkGlError(
                "glVertexAttribPointer uvVertexTextureCoordinateParam");
        }

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

    
    /**
    Draws an image at integer screen coordinates (x, y) in pixels from
    upper left corner. Takes into account current configured camera (viewpoint).
    
    This method could fail if caller does not set previously texture
    parameters for OpenGLES.
    @param img
    @param c
    @param x
    @param y
    */
    protected static void drawImage(Image img, Camera c, int x, int y)
    {
        RendererConfiguration q;
        double fx, fy;
        double dx, dy;

        if ( img == null ) {
            return;
        }

        if ( x < 0 ) {
            x = -x;
            x = (int)c.getViewportXSize() - img.getXSize() - x;
        }
        if ( y < 0 ) {
            y = -y;
            y = (int)c.getViewportYSize() - img.getYSize() - y;
        }
        
        fx = (((double)img.getXSize()) * 2.0) / 
             ((double)c.getViewportXSize());

        fy = (((double)img.getYSize()) * 2.0) / 
             ((double)c.getViewportYSize());

	dx = ((double)(x) * 2.0 + ((double)img.getXSize())) / 
            ((double)c.getViewportXSize());

        dy = ((double)(y) * 2.0 + ((double)img.getYSize())) / 
            ((double)c.getViewportYSize());

        q = new RendererConfiguration();
        q.setSurfaces(true);
        q.setTexture(true);
        q.setUseVertexColors(true);
        q.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glTranslated(-1.0 + dx, 1.0 - dy, 0);
        glScaled(fx, fy, 1.0);
        setRendererConfiguration(q);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        activateDefaultTextureParameters();
        AndroidGLES20ImageRenderer.activate(img);
        drawUnitSquare();
    }

    /**
    Generates OpenGL ES 2.0 primitives needed to render a 2D square of unit
    size (sides of 1.0) placed around the origin.
    */
    public synchronized static void drawUnitSquare()
    {
        if ( unitSquare != null && 
             isObjectRegisteredWithADisplayList(unitSquare) ) {
            executeCompiledDisplayList(unitSquare);
            return;
        }
        
        //-----------------------------------------------------------------
        unitSquare = new Object();
        
        AndroidGLES20DisplayList displayList;
        
        displayList = new AndroidGLES20DisplayList(null);
                
        //-----------------------------------------------------------------
        // Geometry data
        float[] vertexDataArray = {
            // X, Y, Z, R, G, B, NX, NY, NZ, U, V
            -0.5f, -0.5f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
             0.5f, -0.5f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f,
            -0.5f,  0.5f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f,
             0.5f,  0.5f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f };

        FloatBuffer vertexArray;

        vertexArray = ByteBuffer.allocateDirect(
            vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
            ByteOrder.nativeOrder()).asFloatBuffer();
        vertexArray.put(vertexDataArray);

        //-----------------------------------------------------------------
        int[] vbo;
        vbo = new int[1];
        GLES20.glGenBuffers(1, vbo, 0);
        vertexArray.position(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0]);
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            vertexArray.capacity() * FLOAT_SIZE_IN_BYTES,
            vertexArray, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        displayList.addVbo(null, vbo[0], GLES20.GL_TRIANGLE_STRIP, 4);

        registerObjectWithADisplayList(unitSquare, displayList);
        
        //-----------------------------------------------------------------
        //drawVertices3Position3Color3Normal2Uv(
        //    vertexArray,
        //    GLES20.GL_TRIANGLE_STRIP, 
        //    4);
        executeCompiledDisplayList(unitSquare);
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
