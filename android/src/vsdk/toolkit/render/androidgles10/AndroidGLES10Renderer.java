//===========================================================================
package vsdk.toolkit.render.androidgles10;

// Java basic classes
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;

// Android GLES classes
import android.opengl.GLES10;
import android.opengl.GLES11;
import android.opengl.GLES20;
import android.util.Log;
import vsdk.toolkit.common.RendererConfiguration;

// VSDK classes
import vsdk.toolkit.render.RenderingElement;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.gui.AndroidSystem;
import vsdk.toolkit.gui.TextVisualConfiguration;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBAImage;

/**
*/
public class AndroidGLES10Renderer extends RenderingElement {
    private static final String TAG = "AndroidGLES10Renderer";

    protected static final int MODE_3POSITION = 1;
    protected static final int MODE_3POSITION_3NORMAL_2UV = 2;
    protected static final int MODE_3POSITION_3COLOR = 3;
    protected static final int MODE_3POSITION_3COLOR_3NORMAL_2UV = 4;    
    protected static final int MODE_3POSITION_4COLOR = 3;
    protected static final int MODE_3POSITION_4COLOR_3NORMAL_2UV = 4;   

    protected static final int FLOAT_SIZE_IN_BYTES = 4;
    private static Object unitSquare = null;
    public static boolean errorsDetected = false;
    private static String errorMessage;
    
    /// Reference to all known objects and their corresponding display list data
    private static HashMap<Object, AndroidGLES10DisplayList> displayLists;

    static {
        init();
    }
    
    public static void init()
    {
        displayLists = new HashMap<Object, AndroidGLES10DisplayList>();
        errorMessage = "No error detected";
    }
    
    public static void checkGlError(String op) {
        int error;
        error = GLES10.glGetError();
        String name = "UNKNOWN GL ERROR";

        while ( error != GLES10.GL_NO_ERROR ) {
            switch ( error ) {
              case GLES10.GL_INVALID_ENUM:
                name = "GL_INVALID_ENUM​";
                break;
              case GLES10.GL_INVALID_VALUE:
                name = "GL_INVALID_VALUE";
                break;
              case GLES10.GL_INVALID_OPERATION:
                name = "GL_INVALID_OPERATION​";
                break;
              case GLES10.GL_OUT_OF_MEMORY:
                name = "GL_OUT_OF_MEMORY​";
                break;
            //case GLES10.GL_INVALID_FRAMEBUFFER_OPERATION:
            //  name = "GL_INVALID_FRAMEBUFFER_OPERATION";
            //  break;
            //case GLES10.GL_TABLE_TOO_LARGE:
            //  name = "GL_TABLE_TOO_LARGE​"; 
            //  break;
            //case GLES10.GL_STACK_OVERFLOW: name = "GL_STACK_OVERFLOW"; break;
            //case GLES10.GL_STACK_UNDERFLOW: name = "GL_STACK_UNDERFLOW"; break;
            }

            if ( !errorsDetected ) {
                Log.e(TAG, op + ": glError " + error + " : " + name
                    + " Thread: " + Thread.currentThread().getName());
                
                VSDK.reportMessage(
                        null, VSDK.WARNING, "checkGLError",
                        "OpenGL ES 1.0 error");
                /*
                try {
                    //throw new RuntimeException(op + ": glError " + error);
                }
                catch ( RuntimeException e ) {
                    VSDK.reportMessageWithException(
                        null, VSDK.WARNING, "checkGLError",
                        "OpenGL ES 1.0 error", e);
                }
                */
            }
            
            errorsDetected = true;
            errorMessage = "" + op + ": glError " + error + " : " + name;
        }
    }

    public static void executeCompiledDisplayList(Object o)
    {
        AndroidGLES10DisplayList displayList;
        
        displayList = displayLists.get(o);
        
        if ( displayList == null ) {
            return;
        }
        
        int i;

        if ( displayList.getCorrespondingQuality() != null ) {
            setRendererConfiguration(displayList.getCorrespondingQuality());
        }
        
        // Draw elements on display list
        for ( i = 0; i < displayList.getVboIds().size(); i++ ) {
            Material m = displayList.getVboMaterials().get(i);
            
            if ( m != null ) {
                //AndroidGLES10MaterialRenderer.activate(m);
            }

            //-----------------------------------------------------------------
            // Activate VBO
            GLES11.glBindBuffer(
                GLES11.GL_ARRAY_BUFFER, displayList.getVboIds().get(i));
            checkGlError("glBindBuffer(" + displayList.getVboIds().get(i) + ")");

            if ( displayList.getIndexIds().size() == 0 ) {
                // Display VBO
                switch ( displayList.getVertexMode() ) {
                  case MODE_3POSITION:
                    drawVertices3Position(
                        displayList.getVboPrimitives().get(i),
                        displayList.getVboSizes().get(i));
                    break;
                  case MODE_3POSITION_4COLOR_3NORMAL_2UV:
                    drawVertices3Position4Color3Normal2Uv(
                        displayList.getVboPrimitives().get(i),
                        displayList.getVboSizes().get(i));
                    break;
                  default:
                    errorMessage = "DISPLAY LIST TYPE NOT SUPPORTED";
                    errorsDetected = true;
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
                    //activateVertices3Position3Color3Normal2Uv();
                    break;
                  default:
                    errorMessage = "DISPLAY LIST TYPE NOT SUPPORTED";
                    errorsDetected = true;
                    break;
                }

                // Draw
                int ibo = displayList.getIndexIds().get(i);
                GLES11.glBindBuffer(GLES11.GL_ELEMENT_ARRAY_BUFFER, ibo);
                GLES11.glDrawElements(
                    displayList.getVboPrimitives().get(i), 
                    displayList.getIboSizes().get(i), 
                    GLES11.GL_UNSIGNED_SHORT, 0);
                checkGlError("glDrawElements()");
            }

            // Disable current VBO
            GLES11.glBindBuffer(GLES11.GL_ARRAY_BUFFER, 0);
            checkGlError("glBindBuffer(0) for vertices");
 
            // Disable current IBO
            if ( displayList.getIndexIds().size() != 0 ) {
                GLES11.glBindBuffer(GLES11.GL_ELEMENT_ARRAY_BUFFER, 0);
                checkGlError("glBindBuffer(0) for indices");
            }

        }
    }

    protected static void registerObjectWithADisplayList(
        Object key, AndroidGLES10DisplayList value)
    {
        displayLists.put(key, value);
    }
    
    protected static boolean isObjectRegisteredWithADisplayList(Object o)
    {
        return displayLists.containsKey(o);
    }

    /**
    Generates OpenGL ES 1.0 primitives needed to render a 2D square of unit
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

        AndroidGLES10DisplayList displayList;

        displayList = new AndroidGLES10DisplayList(null);

        //-----------------------------------------------------------------
        // Geometry data
        float vertexDataArray[] = {
            // X, Y, Z, R, G, B, A, NX, NY, NZ, U, V
            -0.5f, -0.5f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
            0.5f, -0.5f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f,
            -0.5f, 0.5f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f,
            0.5f, 0.5f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f};

        FloatBuffer vertexArray;
        vertexArray = ByteBuffer.allocateDirect(
            vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
                ByteOrder.nativeOrder()).asFloatBuffer();
        vertexArray.put(vertexDataArray);
        vertexArray.position(0);

        //-----------------------------------------------------------------
        int vbo[] = new int[1];
        GLES11.glGenBuffers(1, vbo, 0);

        GLES11.glBindBuffer(GLES11.GL_ARRAY_BUFFER, vbo[0]);
        GLES11.glBufferData(
            GLES11.GL_ARRAY_BUFFER,
            vertexArray.capacity() * FLOAT_SIZE_IN_BYTES,
            vertexArray,
            GLES11.GL_STATIC_DRAW);
        GLES11.glBindBuffer(GLES11.GL_ARRAY_BUFFER, 0);
        
        displayList.addVbo(null, vbo[0], GLES20.GL_TRIANGLE_STRIP, 4);
        registerObjectWithADisplayList(unitSquare, displayList);
        
        //-----------------------------------------------------------------
        executeCompiledDisplayList(unitSquare);
    }

    protected static void activateVertices3Position()
    {
        // A
        GLES10.glEnableClientState(GLES10.GL_VERTEX_ARRAY);

        // B
        GLES11.glVertexPointer(3, GLES11.GL_FLOAT, 0, 0);        
        checkGlError("glVertexPointer");
    }

    protected static void activateVertices3Position4Color3Normal2Uv()
    {
        // A
        GLES10.glEnableClientState(GLES10.GL_VERTEX_ARRAY);
        GLES10.glEnableClientState(GLES10.GL_COLOR_ARRAY);
        GLES10.glEnableClientState(GLES10.GL_NORMAL_ARRAY);
        GLES10.glEnableClientState(GLES10.GL_TEXTURE_COORD_ARRAY);

        // B
        int vertexSizeInBytes = 12*FLOAT_SIZE_IN_BYTES;
        
        GLES11.glVertexPointer(
            3, GLES11.GL_FLOAT, vertexSizeInBytes, 0);        
        checkGlError("glVertexPointer");
        
        GLES11.glColorPointer(
            4, GLES11.GL_FLOAT, vertexSizeInBytes, 3*FLOAT_SIZE_IN_BYTES);
        checkGlError("glColorPointer");
        
        GLES11.glNormalPointer(
            GLES11.GL_FLOAT, vertexSizeInBytes, 7*FLOAT_SIZE_IN_BYTES);
        checkGlError("glNormalPointer");
        
        GLES11.glTexCoordPointer(
            2, GLES11.GL_FLOAT, vertexSizeInBytes, 10*FLOAT_SIZE_IN_BYTES);
        checkGlError("glTexCoordPointer");
    }

    /**
    Draws vertices from a VBO previously loaded at GPU, from vertex array
    currently binded (activated with glBindBuffer).
    @param primitive
    @param numberOfElements
    */
    protected static void drawVertices3Position(
        int primitive, int numberOfElements) {
        //-----------------------------------------------------------------
        // Configure shaders parameters to process data on VBO
        activateVertices3Position();
        
        //-----------------------------------------------------------------
        // Draw geometry from VBO preloaded at GPU
        // C
        GLES10.glDrawArrays(primitive, 0, numberOfElements);
        checkGlError("glDrawArrays");

        // D
        GLES10.glDisableClientState(GLES10.GL_VERTEX_ARRAY);
    }    

    /**
    Draws vertices from a VBO previously loaded at GPU, from vertex array
    currently binded (activated with glBindBuffer).
    @param primitive
    @param numberOfElements
    */
    protected static void drawVertices3Position4Color3Normal2Uv(
        int primitive, int numberOfElements) {
        //-----------------------------------------------------------------
        // Configure shaders parameters to process data on VBO
        activateVertices3Position4Color3Normal2Uv();
        
        //-----------------------------------------------------------------
        // Draw geometry from VBO preloaded at GPU
        // C
        GLES10.glDrawArrays(primitive, 0, numberOfElements);
        checkGlError("glDrawArrays");

        // D
        GLES10.glDisableClientState(GLES10.GL_VERTEX_ARRAY);
        GLES10.glDisableClientState(GLES10.GL_COLOR_ARRAY);
        GLES10.glDisableClientState(GLES10.GL_NORMAL_ARRAY);
        GLES10.glDisableClientState(GLES10.GL_TEXTURE_COORD_ARRAY);
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
    @param useWhiteColor
    */
    protected static void drawImage(Image img, Camera c, int x, int y, 
        boolean useWhiteColor)
    {
        RendererConfiguration q;
        float fx, fy;
        float dx, dy;

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
        
        fx = (((float)img.getXSize()) * 2.0f) / 
             ((float)c.getViewportXSize());

        fy = (((float)img.getYSize()) * 2.0f) / 
             ((float)c.getViewportYSize());

	dx = ((float)(x) * 2.0f + ((float)img.getXSize())) / 
            ((float)c.getViewportXSize());

        dy = ((float)(y) * 2.0f + ((float)img.getYSize())) / 
            ((float)c.getViewportYSize());

        q = new RendererConfiguration();
        q.setSurfaces(true);
        q.setTexture(true);
        q.setUseVertexColors(useWhiteColor);
        q.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
        
        GLES10.glMatrixMode(GLES10.GL_PROJECTION);
        GLES10.glLoadIdentity();
        GLES10.glMatrixMode(GLES10.GL_MODELVIEW);
        GLES10.glLoadIdentity();
        GLES10.glTranslatef(-1.0f + dx, 1.0f - dy, 0);
        GLES10.glScalef(fx, fy, 1.0f);
        setRendererConfiguration(q);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        activateDefaultTextureParameters();
        AndroidGLES10ImageRenderer.activate(img);
        drawUnitSquare();
    }
    
    /**
    @param characterStyle
    @param msg
    @param c
    @param x0
    @param y0
    @param useWhiteColor
    */
    public static void
    drawText(
        TextVisualConfiguration characterStyle,
        String msg, Camera c, int x0, int y0, 
        boolean useWhiteColor)
    {
        int x = x0, y = y0;
        int i;
        String key;
        RGBAImage img;

        HashMap<String, RGBAImage> characterSpriteCaches;
        characterSpriteCaches = characterStyle.getCharacterSprites();
        
        for ( i = 0; i < msg.length(); i++ ) {
            key = "" + msg.charAt(i);
            if ( !characterSpriteCaches.containsKey(key) ) {
                img = AndroidSystem.calculateLabelImage(
                    key, 
                    characterStyle.getForegroundColor(), 
                    characterStyle.getBackgroundColor(), 
                    characterStyle.getFontSize());
                characterSpriteCaches.put(key, img);
            }
            if ( characterSpriteCaches.containsKey(key) ) {
                img = characterSpriteCaches.get(key);
                drawImage(img, c, x, y, useWhiteColor);
                x += img.getXSize();
            }
        }
    }

    /**
    @return the errorMessage
    */
    public static String getErrorMessage() {
        return errorMessage;
    }

    /**
    This method should be called after every call to a texture activation
    (glBindTexture).
    */
    public static void activateDefaultTextureParameters()
    {
        GLES10.glTexParameterf(GLES10.GL_TEXTURE_2D, 
            GLES10.GL_TEXTURE_MIN_FILTER, GLES10.GL_NEAREST);
        GLES10.glTexParameterf(GLES10.GL_TEXTURE_2D,
                GLES10.GL_TEXTURE_MAG_FILTER,
                GLES10.GL_LINEAR);
        GLES11.glTexParameteri(GLES11.GL_TEXTURE_2D, GLES11.GL_TEXTURE_WRAP_S,
                GLES10.GL_REPEAT);
        GLES11.glTexParameteri(GLES11.GL_TEXTURE_2D, GLES11.GL_TEXTURE_WRAP_T,
                GLES10.GL_REPEAT);
    }

    public static void setRendererConfiguration(
        RendererConfiguration inRendererConfiguration) {
        
        if ( inRendererConfiguration == null ) {
            return;
        }
        
        if ( inRendererConfiguration.isTextureSet() ) {
            GLES10.glEnable(GLES10.GL_TEXTURE_2D);
        }
        else {
            GLES10.glDisable(GLES10.GL_TEXTURE_2D);
        }
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
