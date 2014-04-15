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
    Render surface elements. If using display list compiling, all display list
    fragments are added to given display list. 
    @param mesh
    @param q
    @param displayList if it is not null, generates the compilation of
    fragments inside the displayList.
    */
    private static void drawMeshSurfaceFragments(
        TriangleMesh mesh, 
        RendererConfiguration q,
        AndroidGLES20DisplayList displayList) {

        RendererConfiguration qcopy = q.clone();
        qcopy.setUseVertexColors(false);
        setRendererConfiguration(qcopy);
       
        int i;

        int materialRanges[][] = mesh.getMaterialRanges();
        Material materialsArray[] = mesh.getMaterials();
        boolean flipNormals = false;

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
        FloatBuffer vertexArray;
        int vbo[];
        
        for ( i = 0; materialRanges != null && i < materialRanges.length; 
              i++ ) {
            end = materialRanges[i][0];
            materialIndex = materialRanges[i][1];
            if ( materialIndex >= 0 && materialIndex < materialsArray.length &&
                 start < end ) {
                AndroidGLES20MaterialRenderer.activate(
                    materialsArray[materialIndex]);
                vertexArray = drawRangeWithBufferedVertexArrays(
                    mesh, start, end, flipNormals, false);
                if ( displayList != null && vertexArray != null ) {
                    compileDisplayListForTriangleRange(
                       vertexArray, displayList, materialsArray[materialIndex]);
                }
            }
            start = end;
        }

        if ( end <= nt ) {
            Material m = new Material();
            AndroidGLES20MaterialRenderer.activate(m);
            vertexArray = drawRangeWithBufferedVertexArrays(
                mesh, start, nt, flipNormals, false);
            if ( displayList != null && vertexArray != null ) {
                compileDisplayListForTriangleRange(
                    vertexArray, displayList, m);
            }
        }
    }

    private static void compileDisplayListForTriangleRange(
        FloatBuffer vertexArray, AndroidGLES20DisplayList displayList, 
        Material m) {
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
        displayList.addVbo(
            m,
            vbo[0],
            vertexArray.capacity()/11);
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
        
        Vertex v;
        
        v = new Vertex(new Vector3D(0, 0, 0), new Vector3D(0, 0, 0), 0, 0);
        
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
        
        if ( q.isPointsSet() ) {
            //drawPoints(mesh, q);
        }
        if ( q.isWiresSet() ) {
            //drawWires(mesh, q);
        }
        if ( q.isSurfacesSet() ) {
            drawMeshSurfaceFragments(mesh, q, null);
        }
        if ( q.isNormalsSet() ) {
            //drawNormals(mesh, q);
        }
    }

    /**
    This method controls the generation of OpenGL ES2.0 primitives needed to
    render given triangle mesh.
    @param mesh
    @param camera
    @param q
    @param displayList
    */
    public static void drawWithDisplayListCompiling(
        TriangleMesh mesh, 
        Camera camera, 
        RendererConfiguration q,
        AndroidGLES20DisplayList displayList) {
        
        if ( q.isPointsSet() ) {
            //drawPoints(mesh, q);
        }
        if ( q.isWiresSet() ) {
            //drawWires(mesh, q);
        }
        if ( q.isSurfacesSet() ) {
            drawMeshSurfaceFragments(mesh, q, displayList);
        }
        if ( q.isNormalsSet() ) {
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
    @return if null,there is no display list, so should not be added to
    display lists set.
    */
    private static FloatBuffer drawRangeWithBufferedVertexArrays(
        TriangleMesh mesh, 
        int start, 
        int end, 
        boolean flipNormals, 
        boolean withTexture) {

        if ( start >= end ) {
            return null;
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

        for ( i = start; i < end; i++ ) {
            if ( i >= t.length/3 ) {
                break;
            }
            drawTriangle(mesh, i, verticesBufferedArray, t);
        }
        
        // Send to GPU
        drawVertices3Position3Color3Normal2Uv(
            verticesBufferedArray, GLES20.GL_TRIANGLES, (end - start) * 3);

        return verticesBufferedArray;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
