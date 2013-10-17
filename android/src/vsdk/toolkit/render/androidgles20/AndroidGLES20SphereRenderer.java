//===========================================================================

package vsdk.toolkit.render.androidgles20;

// Java basic classes
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

// Android classes
import android.opengl.GLES20;

// VSDK classes
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.Sphere;

public class AndroidGLES20SphereRenderer extends AndroidGLES20Renderer
{
    private static void drawPoints(Sphere sphere, int slices, int stacks)
    {
        //-----------------------------------------------------------------
        int n;
        n = stacks * slices;
        int vertexSizeInBytes = 5 * FLOAT_SIZE_IN_BYTES;

        float vertexDataArray[] = new float[n*5];

        //-----------------------------------------------------------------
        Vector3D p = new Vector3D();

        int index = 0;

        for( int i = 0; i < stacks; i++ ) {
            double t1 = ((double)i)/((double)(stacks)-1.0);
            double phi1 = Math.PI * t1 - Math.PI / 2;
    
            for( int j = 0; j < slices; j++ ) {
                double s = ((double)j) / (((double)slices)-1.0);
                double theta = 2 * Math.PI * s;

                sphere.spherePosition(p, theta, phi1);

                vertexDataArray[index] = (float)p.x;    index++;
                vertexDataArray[index] = (float)p.y;    index++;
                vertexDataArray[index] = (float)p.z;    index++;
                vertexDataArray[index] = 0.0f;          index++; // u
                vertexDataArray[index] = 0.0f;          index++; // v
            }
        }

        //-----------------------------------------------------------------
        FloatBuffer verticesBufferedArray;

        verticesBufferedArray = ByteBuffer.allocateDirect(
            vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
            ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBufferedArray.put(vertexDataArray);

        drawVertices3Position2Uv(verticesBufferedArray, GLES20.GL_POINTS,
				 n, vertexSizeInBytes);
    }

    public static void draw(Sphere s)
    {
        drawPoints(s, 20, 10);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
