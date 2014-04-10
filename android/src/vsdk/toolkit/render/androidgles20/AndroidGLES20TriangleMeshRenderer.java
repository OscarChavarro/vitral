//===========================================================================
package vsdk.toolkit.render.androidgles20;

// Java basic classes
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

// Android OpenGL ES 2.0 classes
import android.opengl.GLES20;

// VSDK classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Vertex;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.geometry.TriangleMesh;

/**
Class for rendering TriangleMesh objects.
*/
public class AndroidGLES20TriangleMeshRenderer extends AndroidGLES20Renderer {

    /**
    Render surface elements.
    @param mesh
    @param q
    */
    private static void drawMeshSurface(
        TriangleMesh mesh, 
        RendererConfiguration q) {
        
        RendererConfiguration qcopy = q.clone();
        qcopy.setUseVertexColors(false);
        setRendererConfiguration(qcopy);
       
        int index;
        float vertexDataArray[];
        int vertexFloatElements = 11;
        int i;

        int vertexSizeInBytes = FLOAT_SIZE_IN_BYTES * vertexFloatElements;

        index = 0;
        vertexDataArray = 
            new float[(mesh.getNumTriangles()) * 3 * vertexSizeInBytes];

        int materialRanges[][] = mesh.getMaterialRanges();
        Material materialsArray[] = mesh.getMaterials();
        boolean flipNormals = false;
        
        if ( materialRanges == null ) {
            VSDK.reportMessage(null, VSDK.WARNING, 
                "AndroidGLES20TriangleMeshRenderer.drawMeshSurface", 
                "No material ranges found!");
            return;
        }
        
        int nt;
        nt = mesh.getNumTriangles();
        if ( nt < 1 ) {
            VSDK.reportMessage(null, VSDK.WARNING, 
                "AndroidGLES20TriangleMeshRenderer.drawMeshSurface", 
                "No triangles found!");
            return;
        }
        
        int materialIndex;
        int start = 0;
        int end = 0;
        for ( i = 0; i < materialRanges.length; i++ ) {
            end = materialRanges[i][0];
            materialIndex = materialRanges[i][1];
            if ( materialIndex >= 0 && materialIndex < materialsArray.length &&
                 start < end ) {
                AndroidGLES20MaterialRenderer.activate(
                    materialsArray[materialIndex]);
                drawRangeWithBufferedVertexArrays(mesh, start, end, flipNormals, false);
            }
            start = end;
        }
        if ( end <= nt ) {
            Material m = new Material();
            AndroidGLES20MaterialRenderer.activate(m);
            drawRangeWithBufferedVertexArrays(mesh, start, nt, flipNormals, false);
        }
        
        /*
        for ( i = 0; i < mesh.getTriangleIndexes().length; i += 3 ) {
            drawTriangle(mesh, index, i, vertexDataArray);
            index += 3 * vertexFloatElements;
        }
        
        sendVertexesToDraw(
            vertexDataArray, 
            (mesh.getNumTriangles()) * 3, 
            GLES20.GL_TRIANGLES, mode3Position3Normal2UV);
        */
    }

    /**
    Traverses mesh data structure to select three vertices corresponding to
    requested triangle.
    @param mesh    
    @param triangle_index    
    @param floatBufferedArray    
    @param trianglesArray    
    */
    public static void drawTriangle(
        TriangleMesh mesh, 
        int triangle_index, 
        FloatBuffer floatBufferedArray,
        int trianglesArray[]) {
        
        Vertex v = new Vertex(new Vector3D(0, 0, 0), new Vector3D(0, 0, 0), 0, 0);
        
        int p;
        int i;
        for ( i = 0; i < 3; i++) {
            p = mesh.getTriangleIndexes()[triangle_index + i];

            mesh.getVertexAt(trianglesArray[(3*triangle_index)+i], v);
            
            // glVertex3d(x, y, z)
            floatBufferedArray.put((float)v.position.x);
            floatBufferedArray.put((float)v.position.y);
            floatBufferedArray.put((float)v.position.z);

            // glNormal3d(nx, ny, nz)
            floatBufferedArray.put((float)v.normal.x);
            floatBufferedArray.put((float)v.normal.y);
            floatBufferedArray.put((float)v.normal.z);

            // glColor3d(r, g, b)
            floatBufferedArray.put((float)v.normal.x);
            floatBufferedArray.put((float)v.normal.y);
            floatBufferedArray.put((float)v.normal.z);

            // glTexCoord2d(u, v)
            floatBufferedArray.put((float)v.u);
            floatBufferedArray.put((float)v.v);
        }
    }

    /**
    This method controls the generation of OpenGL ES2.0 primitives needed to
    render given triangle mesh.
    @param mesh
    @param camera
    @param q
    */
    public static void draw(
        TriangleMesh mesh, 
        Camera camera, 
        RendererConfiguration q) {
        
        if (q.isPointsSet()) {
            //drawPoints(mesh, q);
        }
        if (q.isWiresSet()) {
            //drawWires(mesh, q);
        }
        if (q.isSurfacesSet()) {
            drawMeshSurface(mesh, q);
        }
        if (q.isNormalsSet()) {
            //drawNormals(mesh, q);
        }
    }

    /**
    Given a range of triangle indices between start and end, this method draws
    that range of triangles.
    @param mesh
    @param start
    @param end
    @param flipNormals
    @param withTexture
    */
    private static void drawRangeWithBufferedVertexArrays(
        TriangleMesh mesh, 
        int start, 
        int end, 
        boolean flipNormals, 
        boolean withTexture) {

        if ( start >= end ) {
            return;
        }

        int i;
        int t[];
        t = mesh.getTriangleIndexes();

        // Build buffered array
        FloatBuffer verticesBufferedArray;
        int vertexFloatElements = 11;

        verticesBufferedArray = ByteBuffer.allocateDirect(
            (vertexFloatElements*3*(end - start)) * FLOAT_SIZE_IN_BYTES).order(
                ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBufferedArray.position(0);

        int index = 0;
        for ( i = start; i < end; i++ ) {
            if ( i >= t.length/3 ) {
                break;
            }
            drawTriangle(mesh, i, verticesBufferedArray, t);
            index += 3*vertexFloatElements;
        }
        
        // Send to GPU
        drawVertices3Position3Color3Normal2Uv(
            verticesBufferedArray, GLES20.GL_TRIANGLES, (end - start) * 3);
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
