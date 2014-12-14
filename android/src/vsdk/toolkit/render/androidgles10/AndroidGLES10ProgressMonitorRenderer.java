//===========================================================================
package vsdk.toolkit.render.androidgles10;

// VSDK classes
import vsdk.toolkit.gui.ProgressMonitor;

// Android GLES 1.0 classes
import android.opengl.GLES10;
import android.opengl.GLES11;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.RendererConfiguration;
import static vsdk.toolkit.render.androidgles10.AndroidGLES10Renderer.FLOAT_SIZE_IN_BYTES;

/**
*/
public class AndroidGLES10ProgressMonitorRenderer extends AndroidGLES10Renderer 
{
    private static void drawFrame(
        double x0, double y0, double xSize, double ySize,
        double xBorder, double yBorder,
        double r, double g, double b)
    {
        float xa = (float)(x0 + xBorder);
        float ya = (float)(y0 + yBorder);
        float xb = (float)(x0 + xSize - xBorder);
        float yb = (float)(y0 + yBorder);
        float xc = (float)(x0 + xBorder);
        float yc = (float)(y0 + ySize - yBorder);
        float xd = (float)(x0 + xSize - xBorder);
        float yd = (float)(y0 + ySize - yBorder);
        
        GLES10.glPushMatrix();
        GLES10.glLoadIdentity();        
        //GLES10.glTranslatef(0.25f, 0.2f, 0.0f);
        //GLES10.glScalef(0.5f, 0.1f, 1.0f);
        
        //drawUnitSquare();
        

        //-----------------------------------------------------------------
        // Geometry data
        float vertexDataArray[] = {
            // X, Y, Z, R, G, B, A, NX, NY, NZ, U, V
            xa, ya, 0.0f, (float)r, (float)g, (float)b, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
            xb, yb, 0.0f, (float)r, (float)g, (float)b, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f,
            xc, yc, 0.0f, (float)r, (float)g, (float)b, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f,
            xd, yd, 0.0f, (float)r, (float)g, (float)b, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f};

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

        //-----------------------------------------------------------------
        GLES11.glBindBuffer(
                GLES11.GL_ARRAY_BUFFER, vbo[0]);

        drawVertices3Position4Color3Normal2Uv(GLES10.GL_TRIANGLE_STRIP, 4);

        GLES10.glPopMatrix();
    }

    public static void draw(
        ProgressMonitor monitor,
        double x0, double y0, double xSize, double ySize)
    {
        RendererConfiguration q = new RendererConfiguration();
        
        q.setUseVertexColors(true);
        q.setSurfaces(true);
        q.setTexture(false);
        q.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
        setRendererConfiguration(q);

        GLES10.glDisable(GLES10.GL_DEPTH_TEST);
        
        ColorRgb background = new ColorRgb();
        ColorRgb off = new ColorRgb();
        ColorRgb on = new ColorRgb();
        
        // SIGE
        background.r = 249.0/255.0;
        background.g = 235.0/255.0;
        background.b = 40.0/255.0;
        off.r = 249.0/255.0;
        off.g = 235.0/255.0;
        off.b = 40.0/255.0;
        on.r = 26.0/255.0;
        on.g = 164.0/255.0;
        on.b = 177.0/255.0;
        
        // Calleat
        //background.r = 0;
        //background.g = 0;
        //background.b = 0;
        //off.r = 0.6;
        //off.g = 0.6;
        //off.b = 0.6;
        //on.r = 0.0;
        //on.g = 1.0;
        //on.b = 0.0;

        drawFrame(x0, y0, xSize, ySize, 0.0, 0.0, background.r, background.g, background.b);
        drawFrame(x0, y0, xSize, ySize, 0.05, 0.05, off.r, off.g, off.b);
        drawFrame(
            x0 + 0.05, 
            y0 + 0.05, 
            (xSize - 2*0.05)*monitor.getCurrentPercent()/100.0, 
            ySize - 2*0.05, 0, 0,
            on.r, on.g, on.b);
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
