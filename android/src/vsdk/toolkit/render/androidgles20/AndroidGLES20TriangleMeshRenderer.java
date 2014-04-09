//===========================================================================
package vsdk.toolkit.render.androidgles20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.opengl.GLES20;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.TriangleMesh;

public class AndroidGLES20TriangleMeshRenderer extends AndroidGLES20Renderer {

    private static void drawMesh(TriangleMesh nMesh, RendererConfiguration nRendererConfiguration) {
        int index;
        float vertexDataArray[];
        //we'll use vertex with 8 points of information
        int vertexFloatElements = 11;

        int vertexSizeInBytes = FLOAT_SIZE_IN_BYTES * vertexFloatElements;

        index = 0;
        vertexDataArray = new float[(nMesh.getNumTriangles()) * 3 * vertexSizeInBytes];

        int triangle = 0;
        for (int i = 0; i < nMesh.getTriangleIndexes().length; i += 3) {
            drawTriangle(nMesh, index, i, vertexDataArray);
            index += 3 * vertexFloatElements;

        }
        sendVertexesToDraw(vertexDataArray, vertexSizeInBytes, (nMesh.getNumTriangles()) * 3, GLES20.GL_TRIANGLES, mode3Position3Normal2UV);
    }

    private static void drawTriangle(TriangleMesh nMesh, int index, int triangle, float[] vertexDataArray) {
        int p;
        for (int i = 0; i < 3; i++) {
            p = nMesh.getTriangleIndexes()[triangle + i];

            vertexDataArray[index] = (float) nMesh.getVertexPositions()[p * 3];
            index++;
            vertexDataArray[index] = (float) nMesh.getVertexPositions()[(p * 3) + 1];
            index++;
            vertexDataArray[index] = (float) nMesh.getVertexPositions()[(p * 3) + 2];
            index++;

            vertexDataArray[index] = (float) nMesh.getVertexNormals()[p * 3];
            index++;
            vertexDataArray[index] = (float) nMesh.getVertexNormals()[(p * 3) + 1];
            index++;
            vertexDataArray[index] = (float) nMesh.getVertexNormals()[(p * 3) + 2];
            index++;

            vertexDataArray[index] = (float) nMesh.getVertexNormals()[p * 3];
            index++;
            vertexDataArray[index] = (float) nMesh.getVertexNormals()[(p * 3) + 1];
            index++;
            vertexDataArray[index] = (float) nMesh.getVertexNormals()[(p * 3) + 2];
            index++;

            vertexDataArray[index] = (float) nMesh.getVertexPositions()[p * 3] * 2;
            index++;
            vertexDataArray[index] = (float) nMesh.getVertexPositions()[p * 3 + 1] * 2;
            index++;
        }

    }

    /**
    Given the vertexArrayData, it sends them to be drawn
    */
    private static void sendVertexesToDraw(float[] vertexDataArray, int vertexSizeInBytes, int numberOfElements, int primitive, int drawingMode) {
        //The buffered array
        FloatBuffer verticesBufferedArray;
        //allocating the space
        verticesBufferedArray = ByteBuffer.allocateDirect(vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBufferedArray.put(vertexDataArray);
	// Send the vertices to draw with arrays of 3 points for position, 3 for 
        // normals and 2 for UV (for UV mapping) method is using according to 
        // the way they should be drawn.
        switch (drawingMode) {
            case mode3Position3Normal2UV:
                drawVertices3Position3Color3Normal2Uv(verticesBufferedArray, primitive, numberOfElements);
                break;
            case mode3Position3Color:
                drawVertices3Position3Color(verticesBufferedArray, primitive, numberOfElements);
                break;
            default:
                break;
        }

    }

    public static void draw(TriangleMesh nMesh, Camera nCamera, RendererConfiguration q) {
        if (q.isPointsSet()) {
            //drawPoints(nMesh, nRendererConfiguration);
        }
        if (q.isWiresSet()) {
            //drawWires(nMesh, nRendererConfiguration);
        }
        if (q.isSurfacesSet()) {
            drawMesh(nMesh, q);
        }
        if (q.isNormalsSet()) {
            //drawNormals(nMesh, nRendererConfiguration);
        }
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
