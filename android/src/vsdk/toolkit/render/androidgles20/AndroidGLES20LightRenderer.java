//===========================================================================

package vsdk.toolkit.render.androidgles20;

// Java basic classes
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

// Android classes
import android.opengl.GLES20;

// VSDK classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Light;

public class AndroidGLES20LightRenderer extends AndroidGLES20Renderer
{
    public static void activate(Light l)
    {
        int i;

	for ( i = 0; i < lights.size(); i++ ) {
	    if ( lights.get(i) == l ) {
                return;
	    }
	}
        lights.add(l);
    }

    public static void draw(Light l)
    {
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        RendererConfiguration q = new RendererConfiguration();
        q.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
        q.setUseVertexColors(true);
        q.setTexture(false);
        setRendererConfiguration(q);

        //-----------------------------------------------------------------
        int index;
        ColorRgb c = l.getSpecular();
        float delta = 0.1f;
        int numVertex = 6;

        float vertexDataArray[] = new float[numVertex*8];

        Vector3D p = l.getPosition();

        index = 0;
        vertexDataArray[index] = (float)p.x - delta;    index++;
        vertexDataArray[index] = (float)p.y;    index++;
        vertexDataArray[index] = (float)p.z;    index++;
        vertexDataArray[index] = (float)c.r;    index++;
        vertexDataArray[index] = (float)c.g;    index++;
        vertexDataArray[index] = (float)c.b;    index++;
        vertexDataArray[index] = 0.0f;          index++; // u
        vertexDataArray[index] = 0.0f;          index++; // v

        vertexDataArray[index] = (float)p.x + delta;    index++;
        vertexDataArray[index] = (float)p.y;    index++;
        vertexDataArray[index] = (float)p.z;    index++;
        vertexDataArray[index] = (float)c.r;    index++;
        vertexDataArray[index] = (float)c.g;    index++;
        vertexDataArray[index] = (float)c.b;    index++;
        vertexDataArray[index] = 0.0f;          index++; // u
        vertexDataArray[index] = 0.0f;          index++; // v

        vertexDataArray[index] = (float)p.x;    index++;
        vertexDataArray[index] = (float)p.y - delta;    index++;
        vertexDataArray[index] = (float)p.z;    index++;
        vertexDataArray[index] = (float)c.r;    index++;
        vertexDataArray[index] = (float)c.g;    index++;
        vertexDataArray[index] = (float)c.b;    index++;
        vertexDataArray[index] = 0.0f;          index++; // u
        vertexDataArray[index] = 0.0f;          index++; // v

        vertexDataArray[index] = (float)p.x;    index++;
        vertexDataArray[index] = (float)p.y + delta;    index++;
        vertexDataArray[index] = (float)p.z;    index++;
        vertexDataArray[index] = (float)c.r;    index++;
        vertexDataArray[index] = (float)c.g;    index++;
        vertexDataArray[index] = (float)c.b;    index++;
        vertexDataArray[index] = 0.0f;          index++; // u
        vertexDataArray[index] = 0.0f;          index++; // v

        vertexDataArray[index] = (float)p.x;    index++;
        vertexDataArray[index] = (float)p.y;    index++;
        vertexDataArray[index] = (float)p.z - delta;    index++;
        vertexDataArray[index] = (float)c.r;    index++;
        vertexDataArray[index] = (float)c.g;    index++;
        vertexDataArray[index] = (float)c.b;    index++;
        vertexDataArray[index] = 0.0f;          index++; // u
        vertexDataArray[index] = 0.0f;          index++; // v

        vertexDataArray[index] = (float)p.x;    index++;
        vertexDataArray[index] = (float)p.y;    index++;
        vertexDataArray[index] = (float)p.z + delta;    index++;
        vertexDataArray[index] = (float)c.r;    index++;
        vertexDataArray[index] = (float)c.g;    index++;
        vertexDataArray[index] = (float)c.b;    index++;
        vertexDataArray[index] = 0.0f;          index++; // u
        vertexDataArray[index] = 0.0f;          index++; // v

        //------------------------------------------------------------
        FloatBuffer verticesBufferedArray;

        verticesBufferedArray = ByteBuffer.allocateDirect(
            vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
            ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBufferedArray.put(vertexDataArray);
        drawVertices3Position3Color2Uv(verticesBufferedArray, 
            GLES20.GL_LINES, numVertex);

    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
