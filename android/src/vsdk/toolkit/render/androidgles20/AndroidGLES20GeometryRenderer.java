//===========================================================================

package vsdk.toolkit.render.androidgles20;

// Java basic classes
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

// Android classes
import android.opengl.GLES20;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Vertex;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.Arrow;
import vsdk.toolkit.environment.geometry.Box;
import vsdk.toolkit.environment.geometry.Cone;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.ParametricCurve;
import vsdk.toolkit.environment.geometry.ParametricBiCubicPatch;
import vsdk.toolkit.environment.geometry.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.geometry.Torus;
import vsdk.toolkit.environment.geometry.InfinitePlane;
import vsdk.toolkit.environment.geometry.FunctionalExplicitSurface;
import vsdk.toolkit.environment.geometry.TriangleMesh;
import vsdk.toolkit.environment.geometry.TriangleMeshGroup;
import vsdk.toolkit.environment.geometry.TriangleStripMesh;
import vsdk.toolkit.environment.geometry.QuadMesh;
import vsdk.toolkit.environment.geometry.VoxelVolume;

public class AndroidGLES20GeometryRenderer extends AndroidGLES20Renderer
{

    public static void drawMinMaxBox(double minmax[], RendererConfiguration q)
    {
        //gl.glPushAttrib(GL2.GL_LIGHTING_BIT);

        glDisable(GL_TEXTURE_2D);
        setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
        activateShaders();

        int vertexSizeInBytes = FLOAT_SIZE_IN_BYTES * 8;
        int index;
        float delta = 0.1f;
        int numVertex = 8;

        // Warning: Change with configured color for bounding volume
        ColorRgb c = new ColorRgb(1.0, 1.0, 0.0);

        //glLineWidth(1.0f);
        float vertexDataArray[] = new float[numVertex*8];
        FloatBuffer verticesBufferedArray;

        index = 0; vertex3Position3Color2Uv(vertexDataArray, index, 
            minmax[0], minmax[1], minmax[5], c.r, c.g, c.b, 0.0, 0.0); // 6
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            minmax[3], minmax[1], minmax[5], c.r, c.g, c.b, 0.0, 0.0); // 5
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            minmax[3], minmax[4], minmax[5], c.r, c.g, c.b, 0.0, 0.0); // 8
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            minmax[0], minmax[4], minmax[5], c.r, c.g, c.b, 0.0, 0.0); // 7
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            minmax[0], minmax[1], minmax[5], c.r, c.g, c.b, 0.0, 0.0); // 6
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            minmax[0], minmax[1], minmax[2], c.r, c.g, c.b, 0.0, 0.0); // 1
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            minmax[0], minmax[4], minmax[2], c.r, c.g, c.b, 0.0, 0.0); // 2
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            minmax[0], minmax[4], minmax[5], c.r, c.g, c.b, 0.0, 0.0); // 7

        verticesBufferedArray = ByteBuffer.allocateDirect(
            vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
            ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBufferedArray.put(vertexDataArray);
        drawVertices3Position3Color2Uv(verticesBufferedArray, 
            GLES20.GL_LINE_LOOP, numVertex);

        index = 0; vertex3Position3Color2Uv(vertexDataArray, index, 
            minmax[3], minmax[1], minmax[2], c.r, c.g, c.b, 0.0, 0.0); // 4
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            minmax[3], minmax[4], minmax[2], c.r, c.g, c.b, 0.0, 0.0); // 3
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            minmax[0], minmax[4], minmax[2], c.r, c.g, c.b, 0.0, 0.0); // 2
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            minmax[0], minmax[1], minmax[2], c.r, c.g, c.b, 0.0, 0.0); // 1
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            minmax[3], minmax[1], minmax[2], c.r, c.g, c.b, 0.0, 0.0); // 4
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            minmax[3], minmax[1], minmax[5], c.r, c.g, c.b, 0.0, 0.0); // 5
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            minmax[3], minmax[4], minmax[5], c.r, c.g, c.b, 0.0, 0.0); // 8
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            minmax[3], minmax[4], minmax[2], c.r, c.g, c.b, 0.0, 0.0); // 3

        verticesBufferedArray = ByteBuffer.allocateDirect(
            vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
            ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBufferedArray.put(vertexDataArray);
        drawVertices3Position3Color2Uv(verticesBufferedArray, 
            GLES20.GL_LINE_LOOP, numVertex);
        //glPopAttrib();
    }

    public static void drawMinMaxBox(Geometry g, RendererConfiguration q)
    {
        drawMinMaxBox(g.getMinMax(), q);
    }

    public static void drawSelectionCorners(double minmax[], RendererConfiguration q)
    {
        double borderPercent = 0.01;
        double linePercent = 0.25;

        Vector3D min, max, delta;
        min = new Vector3D(minmax[0], minmax[1], minmax[2]);
        max = new Vector3D(minmax[3], minmax[4], minmax[5]);
        delta = max.substract(min);
        min = min.substract(delta.multiply(borderPercent));
        max = max.add(delta.multiply(borderPercent));
        delta = delta.multiply(linePercent);

        //glPushAttrib(GL2.GL_LIGHTING_BIT);
        glDisable(GL_TEXTURE_2D);
        setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
        activateShaders();

        // Warning: Change with configured color for selection corners
        ColorRgb c = new ColorRgb(1.0, 1.0, 1.0);
        //glLineWidth(1.0f);

        int vertexSizeInBytes = FLOAT_SIZE_IN_BYTES * 8;
        int index;
        int numVertex = 48;

        float vertexDataArray[] = new float[numVertex*8];
        FloatBuffer verticesBufferedArray;

        index = 0; vertex3Position3Color2Uv(vertexDataArray, index, 
            min.x, min.y, min.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            min.x+delta.x, min.y, min.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            min.x, min.y, min.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            min.x, min.y+delta.y, min.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            min.x, min.y, min.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            min.x, min.y, min.z+delta.z, c.r, c.g, c.b, 0.0, 0.0);

        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            max.x, min.y, min.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            max.x-delta.x, min.y, min.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            max.x, min.y, min.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            max.x, min.y+delta.y, min.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            max.x, min.y, min.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            max.x, min.y, min.z+delta.z, c.r, c.g, c.b, 0.0, 0.0);

        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            min.x, max.y, min.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            min.x+delta.x, max.y, min.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            min.x, max.y, min.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            min.x, max.y-delta.y, min.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            min.x, max.y, min.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            min.x, max.y, min.z+delta.z, c.r, c.g, c.b, 0.0, 0.0);

        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            min.x, min.y, max.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            min.x+delta.x, min.y, max.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            min.x, min.y, max.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            min.x, min.y+delta.y, max.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            min.x, min.y, max.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            min.x, min.y, max.z-delta.z, c.r, c.g, c.b, 0.0, 0.0);

        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            max.x, max.y, min.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            max.x-delta.x, max.y, min.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            max.x, max.y, min.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            max.x, max.y-delta.y, min.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            max.x, max.y, min.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            max.x, max.y, min.z+delta.z, c.r, c.g, c.b, 0.0, 0.0);

        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            max.x, min.y, max.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            max.x-delta.x, min.y, max.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            max.x, min.y, max.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            max.x, min.y+delta.y, max.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            max.x, min.y, max.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            max.x, min.y, max.z-delta.z, c.r, c.g, c.b, 0.0, 0.0);

        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            min.x, max.y, max.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            min.x+delta.x, max.y, max.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            min.x, max.y, max.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            min.x, max.y-delta.y, max.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            min.x, max.y, max.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            min.x, max.y, max.z-delta.z, c.r, c.g, c.b, 0.0, 0.0);

        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            max.x, max.y, max.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            max.x-delta.x, max.y, max.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            max.x, max.y, max.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            max.x, max.y-delta.y, max.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            max.x, max.y, max.z, c.r, c.g, c.b, 0.0, 0.0);
        index += 8; vertex3Position3Color2Uv(vertexDataArray, index, 
            max.x, max.y, max.z-delta.z, c.r, c.g, c.b, 0.0, 0.0);

        verticesBufferedArray = ByteBuffer.allocateDirect(
            vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
            ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBufferedArray.put(vertexDataArray);
        drawVertices3Position3Color2Uv(verticesBufferedArray, 
            GLES20.GL_LINES, numVertex);

        //glPopAttrib();
    }

    public static void drawSelectionCorners(Geometry g, RendererConfiguration q)
    {
        drawSelectionCorners(g.getMinMax(), q);
    }

    /**
    \todo  Homogenize all of the draw method signatures. Perhaps this code can
    be generalized to search the corresponding rendering class to a given
    Geometry via reflection, so this search should not be done explicitly.
    @param g
    @param c
    @param q
    */
    public static void draw(Geometry g, Camera c, RendererConfiguration q)
    {
        if ( g == null ) {
            VSDK.reportMessage(null, VSDK.WARNING,
                               "AndroidGLES20GeometryRenderer.draw",
                               "null Geometry reference recieved");
            return;
        }

        if ( g instanceof Sphere ) {
            AndroidGLES20SphereRenderer.draw((Sphere)g, c, q);
        }
        if ( g instanceof Torus ) {
            //AndroidGLES20TorusRenderer.draw((Torus)g, c, q);
        }
        if ( g instanceof InfinitePlane ) {
            //AndroidGLES20InfinitePlaneRenderer.draw((InfinitePlane)g, c, q);
        }
        else if ( g instanceof Box ) {
            AndroidGLES20BoxRenderer.draw((Box)g, c, q);
        }
        else if ( g instanceof Cone ) {
            AndroidGLES20ConeRenderer.draw((Cone)g, c, q);
        }
        else if ( g instanceof Arrow ) {
            //AndroidGLES20ArrowRenderer.draw((Arrow)g, c, q);
        }
        else if ( g instanceof ParametricCurve ) {
            //AndroidGLES20ParametricCurveRenderer.draw((ParametricCurve)g, c, q);
        }
        else if ( g instanceof ParametricBiCubicPatch ) {
            //AndroidGLES20ParametricBiCubicPatchRenderer.draw((ParametricBiCubicPatch)g, c, q);
        }
        else if ( g instanceof PolyhedralBoundedSolid ) {
            //AndroidGLES20PolyhedralBoundedSolidRenderer.draw((PolyhedralBoundedSolid)g, c, q);
        }
        else if ( g instanceof TriangleMesh ) {
            AndroidGLES20TriangleMeshRenderer.draw((TriangleMesh)g, c, q);
        }
        else if ( g instanceof QuadMesh ) {
            //AndroidGLES20QuadMeshRenderer.draw((QuadMesh)g, q, false);
        }
        else if ( g instanceof FunctionalExplicitSurface ) {
            //AndroidGLES20FunctionalExplicitSurfaceRenderer.draw((FunctionalExplicitSurface)g, c, q);
        }
        else if ( g instanceof TriangleStripMesh ) {
            //AndroidGLES20TriangleStripMeshRenderer.draw((TriangleStripMesh)g, q, false);
        }
        else if ( g instanceof TriangleMeshGroup ) {
            //AndroidGLES20TriangleMeshGroupRenderer.draw((TriangleMeshGroup)g,q);
        }
        else if ( g instanceof VoxelVolume ) {
            //AndroidGLES20VoxelVolumeRenderer.drawBinaryCubes((VoxelVolume)g, c, q);
        }
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
