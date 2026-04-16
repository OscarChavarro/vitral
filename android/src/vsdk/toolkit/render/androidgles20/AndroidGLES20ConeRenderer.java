package vsdk.toolkit.render.androidgles20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.opengl.GLES20;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.Cone;

public class AndroidGLES20ConeRenderer extends AndroidGLES20Renderer {

    private static final int BODY = 0;
    private static final int TOP_CAP = 1;
    private static final int BOTTOM_CAP = 2;

    /**
     * This method draws the two-radius cylinder surfaces according to the
     * number of slices desired
     */
    private static void drawCone(Cone nCone, RendererConfiguration nRendererConfiguration, int slices) {
        //three quad_strips for the body
        VSDK.acumulatePrimitiveCount(VSDK.QUAD_STRIP, 3);
        //number of slices covering the cone
        VSDK.acumulatePrimitiveCount(VSDK.QUAD, slices * 3);

        //index on the vertex array
        int index;
        //This array will contain the information of every vertex to draw them later
        float vertexDataArray[];
        //THe size of a vertex with all its information
        int vertexFloatElements = 11;

        //Size of the vertex in byte
        int vertexSizeInBytes = FLOAT_SIZE_IN_BYTES * vertexFloatElements;
                                //Defining the size of the vertex array

        double theta;

                                //------------------------------------------------------------------------
        //  CONE MAIN BODY
        //------------------------------------------------------------------------
                                //The main body is composed by an enveloping triangle strip
        //reserving the space on the vertexArray. 
        vertexDataArray = new float[(slices + 1) * 2 * vertexFloatElements];
        index = 0;

        //dividing the circle in the number indicated by slices
        double f = 2 * Math.PI / slices;

        for (int i = 0; i <= slices; i++) {
            //calculating the angle
            theta = i * f;
            drawVertex(nCone, nCone.getBaseRadius(), 0, theta, vertexDataArray, index, nRendererConfiguration, BODY);
            index += vertexFloatElements;
            drawVertex(nCone, nCone.getTopRadius(), nCone.getHeight(), theta, vertexDataArray, index, nRendererConfiguration, BODY);
            index += vertexFloatElements;
        }

        //Sending the data to the renderer
        sendVertexesToDraw(vertexDataArray, vertexSizeInBytes, (slices + 1) * 2, GLES20.GL_TRIANGLE_STRIP, MODE_3POSITION_3COLOR_3NORMAL_2UV);

                                //------------------------------------------------------------------------
        //  CONE CAPS
        //------------------------------------------------------------------------
                                //Base Cap
        vertexDataArray = new float[(slices + 2) * vertexFloatElements];
        index = 0;

        vertexDataArray[index] = 0.0f;
        index++;
        vertexDataArray[index] = 0.0f;
        index++;
        vertexDataArray[index] = 0.0f;
        index++;

        vertexDataArray[index] = 0.0f;
        index++;
        vertexDataArray[index] = 0.0f;
        index++;
        vertexDataArray[index] = -1.0f;
        index++;

        vertexDataArray[index] = 0.0f;
        index++;
        vertexDataArray[index] = 0.0f;
        index++;
        vertexDataArray[index] = -1.0f;
        index++;

        vertexDataArray[index] = 0.0f;
        index++;
        vertexDataArray[index] = 0.0f;
        index++;

        for (int i = 0; i <= slices; i++) {
            //calculating the angle
            theta = i * f;
            drawVertex(nCone, nCone.getBaseRadius(), 0, theta, vertexDataArray, index, nRendererConfiguration, BOTTOM_CAP);
            index += vertexFloatElements;

        }

        sendVertexesToDraw(vertexDataArray, vertexSizeInBytes, slices + 2, GLES20.GL_TRIANGLE_FAN, MODE_3POSITION_3COLOR_3NORMAL_2UV);

                                //Top Cap
        vertexDataArray = new float[(slices + 2) * vertexFloatElements];
        index = 0;

        vertexDataArray[index] = 0.0f;
        index++;
        vertexDataArray[index] = 0.0f;
        index++;
        vertexDataArray[index] = (float) (nCone.getHeight());
        index++;

        vertexDataArray[index] = 0.0f;
        index++;
        vertexDataArray[index] = 0.0f;
        index++;
        vertexDataArray[index] = 1.0f;
        index++;

        vertexDataArray[index] = 0.0f;
        index++;
        vertexDataArray[index] = 0.0f;
        index++;
        vertexDataArray[index] = 1.0f;
        index++;

        vertexDataArray[index] = 1.0f;
        index++;
        vertexDataArray[index] = 1.0f;
        index++;

        for (int i = 0; i <= slices; i++) {
            //calculating the angle
            theta = i * f;
            drawVertex(nCone, nCone.getTopRadius(), nCone.getHeight(), theta, vertexDataArray, index, nRendererConfiguration, TOP_CAP);
            index += vertexFloatElements;

        }

        sendVertexesToDraw(vertexDataArray, vertexSizeInBytes, slices + 2, GLES20.GL_TRIANGLE_FAN, MODE_3POSITION_3COLOR_3NORMAL_2UV);
    }

    /**
     * This method puts the vertex on the array in order to build the surface
     * TODO: Change the name. Suggestion: CalculateVertex
     */
    private static void drawVertex(Cone nCone, double radius, double height, double theta, float[] vertexDataArray, int index, RendererConfiguration nRendererConfiguration, int element) {

        double cosTheta = Math.cos(theta);
        double sinTheta = Math.sin(theta);

        //Putting the coordinates
        vertexDataArray[index] = (float) (radius * cosTheta);
        index++;
        vertexDataArray[index] = (float) (radius * sinTheta);
        index++;
        vertexDataArray[index] = (float) (height);
        index++;

        Vector3D normal;
        if (element == BODY) {
            normal = calculateVectorBodyNormal(nCone, cosTheta, sinTheta);
        } else if (element == TOP_CAP) {
            normal = new Vector3D(0.0f, 0.0f, 1.0f);
        } else {
            normal = new Vector3D(0.0f, 0.0f, -1.0f);
        }

                //Putting the colors
        vertexDataArray[index] = (float) normal.x;
        index++;
        vertexDataArray[index] = (float) normal.y;
        index++;
        vertexDataArray[index] = (float) normal.z;
        index++;

                //Putting the normals   
        vertexDataArray[index] = (float) normal.x;
        index++;
        vertexDataArray[index] = (float) normal.y;
        index++;
        vertexDataArray[index] = (float) normal.z;
        index++;

        //Mapping the texture coordinates
        vertexDataArray[index] = (float) (theta / (2 * Math.PI));
        index++;
        int val = (int) (height / nCone.getHeight());
        vertexDataArray[index] = (float) val;

    }

    private static void drawSimpleVertex(Cone nCone, Vector3D color, double radius, double height, double theta, float[] vertexDataArray, int index, RendererConfiguration nRendererConfiguration, int element) {

        double cosTheta = Math.cos(theta);
        double sinTheta = Math.sin(theta);

        //Putting the coordinates
        vertexDataArray[index] = (float) (radius * cosTheta);
        index++;
        vertexDataArray[index] = (float) (radius * sinTheta);
        index++;
        vertexDataArray[index] = (float) (height);
        index++;

        Vector3D normal;
        if (element == BODY) {
            normal = calculateVectorBodyNormal(nCone, cosTheta, sinTheta);
        } else if (element == TOP_CAP) {
            normal = new Vector3D(0.0f, 0.0f, 1.0f);
        } else {
            normal = new Vector3D(0.0f, 0.0f, -1.0f);
        }

                //Putting the colors
        vertexDataArray[index] = (float) color.x;
        index++;
        vertexDataArray[index] = (float) color.y;
        index++;
        vertexDataArray[index] = (float) color.z;
        index++;

    }

    /**
     * This     method draws the wire framed representation of the cone
     */
    private static void drawWires(Cone nCone, RendererConfiguration nRendererConfiguration, int slices) {

        //index on the vertex array
        int index;
        //This array will contain the information of every vertex to draw them later
        float vertexDataArray[];
        //THe size of a vertex with all its information
        int vertexFloatElements = 6;

        Vector3D color = new Vector3D(1, 0, 0);

        //Size of the vertex in byte
        int vertexSizeInBytes = FLOAT_SIZE_IN_BYTES * vertexFloatElements;
                //Defining the size of the vertex array

        double theta;

                //WIRING THE BASE CAP
        //reserving the space on the vertexArray. 
        vertexDataArray = new float[slices * vertexFloatElements];
        index = 0;

        //dividing the circle in the number indicated by slices
        double f = 2 * Math.PI / slices;

        for (int i = 0; i < slices; i++) {
            //calculating the angle
            theta = i * f;
            drawSimpleVertex(nCone, color, nCone.getBaseRadius(), 0, theta, vertexDataArray, index, nRendererConfiguration, BOTTOM_CAP);
            index += vertexFloatElements;
        }

        //Sending the data to the renderer
        sendVertexesToDraw(vertexDataArray, vertexSizeInBytes, slices, GLES20.GL_LINE_LOOP, MODE_3POSITION_3COLOR);

        //drawing the lines from the center to the edge
        vertexDataArray = new float[slices * 2 * vertexFloatElements];
        index = 0;

        for (int i = 0; i < slices; i++) {
            //the center goes first on the vertex data array
            vertexDataArray[index] = 0;
            index++;
            vertexDataArray[index] = 0;
            index++;
            vertexDataArray[index] = 0;
            index++;

            vertexDataArray[index] = (float) color.x;
            index++;
            vertexDataArray[index] = (float) color.y;
            index++;
            vertexDataArray[index] = (float) color.z;
            index++;

            theta = i * f;
            drawSimpleVertex(nCone, color, nCone.getBaseRadius(), 0, theta, vertexDataArray, index, nRendererConfiguration, BOTTOM_CAP);
            index += vertexFloatElements;
        }

        sendVertexesToDraw(vertexDataArray, vertexSizeInBytes, slices * 2, GLES20.GL_LINES, MODE_3POSITION_3COLOR);

                //WIRING THE TOP CAP
        vertexDataArray = new float[slices * vertexFloatElements];
        index = 0;

        for (int i = 0; i < slices; i++) {
            //calculating the angle
            theta = i * f;
            drawSimpleVertex(nCone, color, nCone.getTopRadius(), nCone.getHeight(), theta, vertexDataArray, index, nRendererConfiguration, TOP_CAP);
            index += vertexFloatElements;
        }

        //Sending the data to the renderer
        sendVertexesToDraw(vertexDataArray, vertexSizeInBytes, slices, GLES20.GL_LINE_LOOP, MODE_3POSITION_3COLOR);

        //drawing the lines from the center to the edge
        vertexDataArray = new float[2 * slices * vertexFloatElements];
        index = 0;

        for (int i = 0; i < slices; i++) {
            //the center goes first on the vertex data array
            vertexDataArray[index] = 0.0f;
            index++;
            vertexDataArray[index] = 0.0f;
            index++;
            vertexDataArray[index] = (float) nCone.getHeight();
            index++;

            vertexDataArray[index] = (float) color.x;
            index++;
            vertexDataArray[index] = (float) color.y;
            index++;
            vertexDataArray[index] = (float) color.z;
            index++;

            theta = i * f;
            drawSimpleVertex(nCone, color, nCone.getTopRadius(), nCone.getHeight(), theta, vertexDataArray, index, nRendererConfiguration, TOP_CAP);
            index += vertexFloatElements;
        }
        sendVertexesToDraw(vertexDataArray, vertexSizeInBytes, 2 * slices, GLES20.GL_LINES, MODE_3POSITION_3COLOR);

                //THIS IS THE BODY IN WIRE FRAME
        vertexDataArray = new float[(slices) * 2 * vertexFloatElements];
        index = 0;

                //dividing the circle in the number indicated by slices
        for (int i = 0; i < slices; i++) {
            //calculating the angle
            theta = i * f;
            drawSimpleVertex(nCone, color, nCone.getBaseRadius(), 0, theta, vertexDataArray, index, nRendererConfiguration, BODY);
            index += vertexFloatElements;
            drawSimpleVertex(nCone, color, nCone.getTopRadius(), nCone.getHeight(), theta, vertexDataArray, index, nRendererConfiguration, BODY);
            index += vertexFloatElements;
        }

        sendVertexesToDraw(vertexDataArray, vertexSizeInBytes, slices * 2, GLES20.GL_LINE_STRIP, MODE_3POSITION_3COLOR);

    }

    /**
     * Method that draws the vertices of the cone
     */
    private static void drawPoints(Cone nCone, RendererConfiguration nRendererConfiguration, int slices) {

        //index on the vertex array
        int index;
        //This array will contain the information of every vertex to draw them later
        float vertexDataArray[];
        //THe size of a vertex with all its information
        int vertexFloatElements = 6;

        Vector3D color = new Vector3D(1, 0, 0);

        //Size of the vertex in byte
        int vertexSizeInBytes = FLOAT_SIZE_IN_BYTES * vertexFloatElements;
                //Defining the size of the vertex array

        double theta;

                //------------------------------------------------------------------------
        //  CONE MAIN BODY
        //------------------------------------------------------------------------
                //The main body is composed by an enveloping triangle strip
        //reserving the space on the vertexArray. 
        vertexDataArray = new float[(slices) * 2 * vertexFloatElements];
        index = 0;

        //dividing the circle in the number indicated by slices
        double f = 2 * Math.PI / slices;

        for (int i = 0; i < slices; i++) {
            //calculating the angle
            theta = i * f;
            drawSimpleVertex(nCone, color, nCone.getBaseRadius(), 0, theta, vertexDataArray, index, nRendererConfiguration, BODY);
            index += vertexFloatElements;
            drawSimpleVertex(nCone, color, nCone.getTopRadius(), nCone.getHeight(), theta, vertexDataArray, index, nRendererConfiguration, BODY);
            index += vertexFloatElements;
        }

        //Sending the data to the renderer
        sendVertexesToDraw(vertexDataArray, vertexSizeInBytes, slices * 2, GLES20.GL_POINTS, MODE_3POSITION_3COLOR);

    }

    /**
     * Method that draws only the normals to each vertex for the cone
     */
    private static void drawNormals(Cone nCone, RendererConfiguration nRendererConfiguration, int slices) {

        //index on the vertex array
        int index;
        //This array will contain the information of every vertex to draw them later
        float vertexDataArray[];
        //THe size of a vertex with all its information
        int vertexFloatElements = 6;

        //Size of the vertex in byte
        int vertexSizeInBytes = FLOAT_SIZE_IN_BYTES * vertexFloatElements;
        //Defining the size of the vertex array

        double theta;

        //------------------------------------------------------------------------
        //  CONE MAIN BODY
        //------------------------------------------------------------------------
        //dividing the circle in the number indicated by slices
        double f = 2 * Math.PI / slices;

        vertexDataArray = new float[4 * slices * vertexFloatElements];
        index = 0;

        for (int i = 0; i < slices; i++) {
            //calculating the angle
            theta = i * f;

            //Sending the lower vertex on the body
            drawNormal(nCone, nCone.getBaseRadius(), 0, theta, vertexDataArray, index, nRendererConfiguration, BODY);
            index += 2 * vertexFloatElements;

            //Sending the upper vertex on the body
            drawNormal(nCone, nCone.getTopRadius(), nCone.getHeight(), theta, vertexDataArray, index, nRendererConfiguration, BODY);
            index += 2 * vertexFloatElements;
        }
        sendVertexesToDraw(vertexDataArray, vertexSizeInBytes, slices * 4, GLES20.GL_LINES, MODE_3POSITION_3COLOR);

        //Sending the top and bottom cap normals
        vertexDataArray = new float[((slices * 4) + 4) * vertexFloatElements];
        index = 0;

        drawCenterNormal(nCone, vertexDataArray, index, nRendererConfiguration, BOTTOM_CAP);
        index += vertexFloatElements * 2;
        drawCenterNormal(nCone, vertexDataArray, index, nRendererConfiguration, TOP_CAP);
        index += vertexFloatElements * 2;

        for (int i = 0; i < slices; i++) {
            theta = i * f;

            drawNormal(nCone, nCone.getBaseRadius(), 0, theta, vertexDataArray, index, nRendererConfiguration, BOTTOM_CAP);
            index += 2 * vertexFloatElements;

            drawNormal(nCone, nCone.getTopRadius(), nCone.getHeight(), theta, vertexDataArray, index, nRendererConfiguration, TOP_CAP);
            index += 2 * vertexFloatElements;

        }
        sendVertexesToDraw(vertexDataArray, vertexSizeInBytes, ((slices * 4) + 4), GLES20.GL_LINES, MODE_3POSITION_3COLOR);

    }

    private static void drawCenterNormal(Cone nCone, float[] vertexDataArray, int index, RendererConfiguration nRendererConfiguration, int capElement) {

        int direction;
        if (capElement == TOP_CAP) {
            direction = 1;
        } else {
            direction = -1;
        }

        //Putting the coordinates
        vertexDataArray[index] = (float) 0.0f;
        index++;
        vertexDataArray[index] = (float) 0.0f;
        index++;
        vertexDataArray[index] = (capElement == BOTTOM_CAP) ? 0.0f : (float) (nCone.getHeight());
        index++;

        vertexDataArray[index] = (float) 1.0f;
        index++;
        vertexDataArray[index] = (float) 0.0f;
        index++;
        vertexDataArray[index] = (float) 0.0f;
        index++;

        //Putting the normals
        vertexDataArray[index] = vertexDataArray[index - 6];
        index++;
        vertexDataArray[index] = vertexDataArray[index - 6];
        index++;
        vertexDataArray[index] = vertexDataArray[index - 6] + (float) direction / 10.0f;
        index++;

        vertexDataArray[index] = (float) 1.0f;
        index++;
        vertexDataArray[index] = (float) 0.0f;
        index++;
        vertexDataArray[index] = (float) 0.0f;
        index++;

    }

    /**
     * Calculate the value of a single normal to put them on the vertexArray
     * TODO: Change name. Suggestion: CalculateNormal
     */
    private static void drawNormal(Cone nCone, double radius, double height, double theta, float[] vertexDataArray, int index, RendererConfiguration nRendererConfiguration, int element) {
        if (element == BODY) {
            drawBodyNormal(nCone, radius, height, theta, vertexDataArray, index, nRendererConfiguration);
        } else {
            drawCapNormal(nCone, radius, height, theta, vertexDataArray, index, nRendererConfiguration, element);
        }

    }

    /**
     * auxiliary method that calculates the normal for a vertex on the body
     * surface TODO: renaming. Suggestion calculateBodyNormal
     */
    private static void drawBodyNormal(Cone nCone, double radius, double height, double theta, float[] vertexDataArray, int index, RendererConfiguration nRendererConfiguration) {

        double cosTheta = Math.cos(theta);
        double sinTheta = Math.sin(theta);

        Vector3D normal = calculateVectorBodyNormal(nCone, cosTheta, sinTheta);

        //Putting the coordinates
        vertexDataArray[index] = (float) (radius * cosTheta);
        index++;
        vertexDataArray[index] = (float) (radius * sinTheta);
        index++;
        vertexDataArray[index] = (float) (height);
        index++;

        vertexDataArray[index] = (float) 1.0f;
        index++;
        vertexDataArray[index] = (float) 0.0f;
        index++;
        vertexDataArray[index] = (float) 0.0f;
        index++;

        //Putting the normals
        vertexDataArray[index] = vertexDataArray[index - 6] + (float) normal.x / 10;
        index++;
        vertexDataArray[index] = vertexDataArray[index - 6] + (float) normal.y / 10;
        index++;
        vertexDataArray[index] = vertexDataArray[index - 6] + (float) normal.z / 10;
        index++;

        vertexDataArray[index] = (float) 1.0f;
        index++;
        vertexDataArray[index] = (float) 0.0f;
        index++;
        vertexDataArray[index] = (float) 0.0f;
        index++;

    }

    /**
     * Auxiliary method that calculates the normal of a vertex on the cone cap
     * TODO: Renaming. Suggestion: calculateCapNormal
     */
    private static void drawCapNormal(Cone nCone, double radius, double height, double theta, float[] vertexDataArray, int index, RendererConfiguration nRendererConfiguration, int capElement) {

        double cosTheta = Math.cos(theta);
        double sinTheta = Math.sin(theta);
        int direction;

        //Direction depends on the cap. If it's the top cap, the normal vector goes up, otherwise it goes down
        if (capElement == TOP_CAP) {
            direction = 1;
        } else {
            direction = -1;
        }

        //Putting the coordinates
        vertexDataArray[index] = (float) (radius * cosTheta);
        index++;
        vertexDataArray[index] = (float) (radius * sinTheta);
        index++;
        vertexDataArray[index] = (float) (height);
        index++;

        vertexDataArray[index] = (float) 1.0f;
        index++;
        vertexDataArray[index] = (float) 0.0f;
        index++;
        vertexDataArray[index] = (float) 0.0f;
        index++;

        //Putting the normals
        vertexDataArray[index] = vertexDataArray[index - 6];
        index++;
        vertexDataArray[index] = vertexDataArray[index - 6];
        index++;
        vertexDataArray[index] = vertexDataArray[index - 6] + (float) direction / 10.0f;
        index++;

        vertexDataArray[index] = (float) 1.0f;
        index++;
        vertexDataArray[index] = (float) 0.0f;
        index++;
        vertexDataArray[index] = (float) 0.0f;
        index++;

    }

    /**
     * Method that calculates the vector for a normal on the body
     */
    private static Vector3D calculateVectorBodyNormal(Cone nCone, double cosTheta, double sinTheta) {

        Vector3D r1 = new Vector3D((nCone.getBaseRadius() * cosTheta), (nCone.getBaseRadius() * sinTheta), 0);
        Vector3D r2 = new Vector3D((nCone.getTopRadius() * cosTheta), (nCone.getTopRadius() * sinTheta), nCone.getHeight());
        Vector3D tangent = r2.substract(r1);

        tangent.normalize();
        r1.normalize();

        Vector3D binormal;
        //Avoid cross products with the zero vector.
        if (nCone.getBaseRadius() > 0) {
            binormal = tangent.crossProduct(r1);
        } else {
            //An auxiliary vector to make the crossProducts
            Vector3D rAux = new Vector3D((nCone.getTopRadius() * cosTheta), (nCone.getTopRadius() * sinTheta), 0);
            rAux.normalize();
            binormal = tangent.crossProduct(rAux);
        }
        Vector3D normal = binormal.crossProduct(tangent);

        return normal;
    }

    /**
     * Given the vertexArrayData, it sends it to be drawn
     */
    private static void sendVertexesToDraw(float[] vertexDataArray, int vertexSizeInBytes, int slices, int primitive, int drawingMode) {
        //The buffered array
        FloatBuffer verticesBufferedArray;
        //allocating the space
        verticesBufferedArray = ByteBuffer.allocateDirect(vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBufferedArray.put(vertexDataArray);
        //send the vertices to draw with arrays of 3 points for position, 3 for normals and 2 for UV (for UV mapping)
        //method is using according to the way they should be drawn.
        switch (drawingMode) {
            case MODE_3POSITION_3COLOR_3NORMAL_2UV:
                drawVertices3Position3Color3Normal2Uv(verticesBufferedArray, primitive, slices);
                break;
            case MODE_3POSITION_3NORMAL_2UV:
                drawVertices3Position3Normal2Uv(verticesBufferedArray, primitive, slices);
                break;
            case MODE_3POSITION_3COLOR:
                drawVertices3Position3Color(verticesBufferedArray, primitive, slices);
                break;
            default:
                break;
        }

    }

    public static void draw(Cone nCone, Camera nCamera, RendererConfiguration nRendererConfiguration) {
        draw(nCone, nCamera, nRendererConfiguration, 10);
    }

    public static void draw(Cone nCone, Camera nCamera, RendererConfiguration q, int slices) {
        //Ilumination IFs
        if (q.isPointsSet()) {
            drawPoints(nCone, q, slices);
        }
        if (q.isWiresSet()) {
            drawWires(nCone, q, slices);
        }
        if (q.isSurfacesSet()) {
            drawCone(nCone, q, slices);
        }

    }
}
