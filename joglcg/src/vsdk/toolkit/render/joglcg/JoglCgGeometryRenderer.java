package vsdk.toolkit.render.joglcg;

// JOGL classes
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.cg.CgGL;
import com.jogamp.opengl.cg.CGprogram;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Vertex;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.Arrow;
import vsdk.toolkit.environment.geometry.Box;
import vsdk.toolkit.environment.geometry.Cone;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.ParametricCurve;
import vsdk.toolkit.environment.geometry.ParametricBiCubicPatch;
import vsdk.toolkit.environment.geometry.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.geometry.InfinitePlane;
import vsdk.toolkit.environment.geometry.FunctionalExplicitSurface;
import vsdk.toolkit.environment.geometry.TriangleMesh;
import vsdk.toolkit.environment.geometry.TriangleMeshGroup;
import vsdk.toolkit.environment.geometry.TriangleStripMesh;
import vsdk.toolkit.environment.geometry.QuadMesh;
import vsdk.toolkit.environment.geometry.VoxelVolume;
import vsdk.toolkit.render.jogl.Jogl2ArrowRenderer;
import vsdk.toolkit.render.jogl.Jogl2BoxRenderer;
import vsdk.toolkit.render.jogl.Jogl2ConeRenderer;
import vsdk.toolkit.render.jogl.Jogl2FunctionalExplicitSurfaceRenderer;
import vsdk.toolkit.render.jogl.Jogl2InfinitePlaneRenderer;
import vsdk.toolkit.render.jogl.Jogl2MatrixRenderer;
import vsdk.toolkit.render.jogl.Jogl2ParametricBiCubicPatchRenderer;
import vsdk.toolkit.render.jogl.Jogl2ParametricCurveRenderer;
import vsdk.toolkit.render.jogl.Jogl2PolyhedralBoundedSolidRenderer;
import vsdk.toolkit.render.jogl.Jogl2QuadMeshRenderer;
import vsdk.toolkit.render.jogl.Jogl2TriangleMeshGroupRenderer;
import vsdk.toolkit.render.jogl.Jogl2TriangleMeshRenderer;
import vsdk.toolkit.render.jogl.Jogl2TriangleStripMeshRenderer;
import vsdk.toolkit.render.jogl.Jogl2VoxelVolumeRenderer;
import vsdk.toolkit.render.joglcg.JoglCgRenderer;
import vsdk.toolkit.render.joglcg.JoglCgCameraRenderer;

public class JoglCgGeometryRenderer extends JoglCgRenderer 
{
    private static Vector3D p = new Vector3D();
    private static Vector3D n = new Vector3D();

    public static void activateShaders(GL2 gl, Geometry g, Camera c)
    {
        //-----------------------------------------------------------------
        if ( nvidiaCgAutomaticMode ) {
            JoglCgCameraRenderer.activateNvidiaGpuParameters(gl, c,
                currentVertexShader, currentPixelShader);
            activateNvidiaGpuParameters(gl, g, c,
                currentVertexShader, currentPixelShader);
        }
    }

    public static void prepareSurfaceQuality(GL2 gl, RendererConfiguration quality)
    {
        int shadingType = quality.getShadingType();

        switch ( shadingType ) {
          case RendererConfiguration.SHADING_TYPE_NOLIGHT:
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glShadeModel(GL2.GL_FLAT);
            // Warning: Change with configured color for ambient lightning
            gl.glColor3d(1, 1, 1);
            break;
          case RendererConfiguration.SHADING_TYPE_FLAT:
            gl.glEnable(GL2.GL_LIGHTING);
            gl.glShadeModel(GL2.GL_FLAT);
            break;
          case RendererConfiguration.SHADING_TYPE_PHONG:
          case RendererConfiguration.SHADING_TYPE_GOURAUD: default:
            gl.glEnable(GL2.GL_LIGHTING);
            gl.glShadeModel(GL2.GL_SMOOTH);
            break;
        }
    }

    public static void drawVertexNormal(GL2 gl, Vertex vertex) {
        double l = 0.2;
        p = vertex.getPosition();
        n = vertex.getNormal();

        gl.glVertex3d(p.x + (n.x * l/100),
                      p.y + (n.y * l/100),
                      p.z + (n.z * l/100));
        gl.glVertex3d(p.x + (n.x * l),
                      p.y + (n.y * l),
                      p.z + (n.z * l));
    }

    public static void drawMinMaxBox(GL2 gl, double minmax[], RendererConfiguration q)
    {
        gl.glPushAttrib(GL2.GL_LIGHTING_BIT);
        JoglCgRenderer.disableNvidiaCgProfiles();
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_TEXTURE_2D);
        // Warning: Change with configured color for bounding volume
        gl.glColor3d(1, 1, 0);
        gl.glLineWidth(1.0f);
        gl.glBegin(GL.GL_LINE_LOOP);
            gl.glVertex3d(minmax[0], minmax[1], minmax[5]); // 6
            gl.glVertex3d(minmax[3], minmax[1], minmax[5]); // 5
            gl.glVertex3d(minmax[3], minmax[4], minmax[5]); // 8
            gl.glVertex3d(minmax[0], minmax[4], minmax[5]); // 7
            gl.glVertex3d(minmax[0], minmax[1], minmax[5]); // 6
            gl.glVertex3d(minmax[0], minmax[1], minmax[2]); // 1
            gl.glVertex3d(minmax[0], minmax[4], minmax[2]); // 2
            gl.glVertex3d(minmax[0], minmax[4], minmax[5]); // 7
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
            gl.glVertex3d(minmax[3], minmax[1], minmax[2]); // 4
            gl.glVertex3d(minmax[3], minmax[4], minmax[2]); // 3
            gl.glVertex3d(minmax[0], minmax[4], minmax[2]); // 2
            gl.glVertex3d(minmax[0], minmax[1], minmax[2]); // 1
            gl.glVertex3d(minmax[3], minmax[1], minmax[2]); // 4
            gl.glVertex3d(minmax[3], minmax[1], minmax[5]); // 5
            gl.glVertex3d(minmax[3], minmax[4], minmax[5]); // 8
            gl.glVertex3d(minmax[3], minmax[4], minmax[2]); // 3
        gl.glEnd();

        gl.glPopAttrib();
    }

    public static void drawMinMaxBox(GL2 gl, Geometry g, RendererConfiguration q)
    {
        drawMinMaxBox(gl, g.getMinMax(), q);
    }

    public static void drawSelectionCorners(GL2 gl, double minmax[], RendererConfiguration q)
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

        gl.glPushAttrib(GL2.GL_LIGHTING_BIT);
        JoglCgRenderer.disableNvidiaCgProfiles();
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_TEXTURE_2D);
        // Warning: Change with configured color for bounding volume
        gl.glColor3d(1, 1, 1);
        gl.glLineWidth(1.0f);
        gl.glBegin(GL.GL_LINES);

            gl.glVertex3d(min.x, min.y, min.z);
            gl.glVertex3d(min.x+delta.x, min.y, min.z);
            gl.glVertex3d(min.x, min.y, min.z);
            gl.glVertex3d(min.x, min.y+delta.y, min.z);
            gl.glVertex3d(min.x, min.y, min.z);
            gl.glVertex3d(min.x, min.y, min.z+delta.z);

            gl.glVertex3d(max.x, min.y, min.z);
            gl.glVertex3d(max.x-delta.x, min.y, min.z);
            gl.glVertex3d(max.x, min.y, min.z);
            gl.glVertex3d(max.x, min.y+delta.y, min.z);
            gl.glVertex3d(max.x, min.y, min.z);
            gl.glVertex3d(max.x, min.y, min.z+delta.z);

            gl.glVertex3d(min.x, max.y, min.z);
            gl.glVertex3d(min.x+delta.x, max.y, min.z);
            gl.glVertex3d(min.x, max.y, min.z);
            gl.glVertex3d(min.x, max.y-delta.y, min.z);
            gl.glVertex3d(min.x, max.y, min.z);
            gl.glVertex3d(min.x, max.y, min.z+delta.z);

            gl.glVertex3d(min.x, min.y, max.z);
            gl.glVertex3d(min.x+delta.x, min.y, max.z);
            gl.glVertex3d(min.x, min.y, max.z);
            gl.glVertex3d(min.x, min.y+delta.y, max.z);
            gl.glVertex3d(min.x, min.y, max.z);
            gl.glVertex3d(min.x, min.y, max.z-delta.z);

            gl.glVertex3d(max.x, max.y, min.z);
            gl.glVertex3d(max.x-delta.x, max.y, min.z);
            gl.glVertex3d(max.x, max.y, min.z);
            gl.glVertex3d(max.x, max.y-delta.y, min.z);
            gl.glVertex3d(max.x, max.y, min.z);
            gl.glVertex3d(max.x, max.y, min.z+delta.z);

            gl.glVertex3d(max.x, min.y, max.z);
            gl.glVertex3d(max.x-delta.x, min.y, max.z);
            gl.glVertex3d(max.x, min.y, max.z);
            gl.glVertex3d(max.x, min.y+delta.y, max.z);
            gl.glVertex3d(max.x, min.y, max.z);
            gl.glVertex3d(max.x, min.y, max.z-delta.z);

            gl.glVertex3d(min.x, max.y, max.z);
            gl.glVertex3d(min.x+delta.x, max.y, max.z);
            gl.glVertex3d(min.x, max.y, max.z);
            gl.glVertex3d(min.x, max.y-delta.y, max.z);
            gl.glVertex3d(min.x, max.y, max.z);
            gl.glVertex3d(min.x, max.y, max.z-delta.z);

            gl.glVertex3d(max.x, max.y, max.z);
            gl.glVertex3d(max.x-delta.x, max.y, max.z);
            gl.glVertex3d(max.x, max.y, max.z);
            gl.glVertex3d(max.x, max.y-delta.y, max.z);
            gl.glVertex3d(max.x, max.y, max.z);
            gl.glVertex3d(max.x, max.y, max.z-delta.z);

        gl.glEnd();

        gl.glPopAttrib();
    }

    public static void drawSelectionCorners(GL2 gl, Geometry g, RendererConfiguration q)
    {
        drawSelectionCorners(gl, g.getMinMax(), q);
    }

    public static void activateNvidiaGpuParameters(GL2 gl, Geometry g,
        Camera camera, CGprogram vertexShader, CGprogram pixelShader)
    {
        Matrix4x4 MProjection;
        Matrix4x4 MModelviewGlobal;
        Matrix4x4 MModelviewLocal, MModelviewLocalIT, MCombined;
        double matrixarray[];

        MProjection = camera.calculateViewVolumeMatrix();
        MModelviewGlobal = camera.calculateTransformationMatrix();
        MModelviewLocal = MModelviewGlobal.multiply(
            Jogl2MatrixRenderer.importJOGL(gl, GL2.GL_MODELVIEW_MATRIX));
        MCombined = MProjection.multiply(MModelviewLocal);
        MModelviewLocalIT = MModelviewLocal.inverse();
        MModelviewLocalIT = MModelviewLocalIT.transpose();

        matrixarray = MCombined.exportToDoubleArrayRowOrder();
        CgGL.cgGLSetMatrixParameterdr(CgGL.cgGetNamedParameter(
            vertexShader, "modelViewProjectionLocal"),
            matrixarray, 0);

        matrixarray = MModelviewLocal.exportToDoubleArrayRowOrder();
        CgGL.cgGLSetMatrixParameterdr(CgGL.cgGetNamedParameter(
            vertexShader, "modelViewLocal"),
            matrixarray, 0);

        matrixarray = MModelviewLocalIT.exportToDoubleArrayRowOrder();
        CgGL.cgGLSetMatrixParameterdr(CgGL.cgGetNamedParameter(
            vertexShader, "modelViewLocalIT"),
            matrixarray, 0);
    }

    /**
    \todo  Homogenize all of the draw method signatures. Perhaps this code can
    be generalized to search the corresponding rendering class to a given
    Geometry via reflection, so this search should not be done explicitly.
    */
    public static void draw(GL2 gl, Geometry g, Camera c, RendererConfiguration q)
    {
        if ( g == null ) {
            VSDK.reportMessage(null, VSDK.WARNING,
                               "Jogl2GeometryRenderer.draw",
                               "null Geometry reference recieved");
            return;
        }

        if ( g instanceof Sphere ) {
            JoglCgSphereRenderer.draw(gl, (Sphere)g, c, q);
        }
        if ( g instanceof InfinitePlane ) {
            Jogl2InfinitePlaneRenderer.draw(gl, (InfinitePlane)g, c, q);
        }
        else if ( g instanceof Box ) {
            Jogl2BoxRenderer.draw(gl, (Box)g, c, q);
        }
        else if ( g instanceof Cone ) {
            Jogl2ConeRenderer.draw(gl, (Cone)g, c, q);
        }
        else if ( g instanceof Arrow ) {
            Jogl2ArrowRenderer.draw(gl, (Arrow)g, c, q);
        }
        else if ( g instanceof ParametricCurve ) {
            Jogl2ParametricCurveRenderer.draw(gl, (ParametricCurve)g, c, q);
        }
        else if ( g instanceof ParametricBiCubicPatch ) {
            Jogl2ParametricBiCubicPatchRenderer.draw(gl, (ParametricBiCubicPatch)g, c, q);
        }
        else if ( g instanceof PolyhedralBoundedSolid ) {
            Jogl2PolyhedralBoundedSolidRenderer.draw(gl, (PolyhedralBoundedSolid)g, c, q);
        }
        else if ( g instanceof TriangleMesh ) {
            Jogl2TriangleMeshRenderer.draw(gl, (TriangleMesh)g, q, false);
        }
        else if ( g instanceof QuadMesh ) {
            Jogl2QuadMeshRenderer.draw(gl, (QuadMesh)g, q, false);
        }
        else if ( g instanceof FunctionalExplicitSurface ) {
            Jogl2FunctionalExplicitSurfaceRenderer.draw(gl, (FunctionalExplicitSurface)g, c, q);
        }
        else if ( g instanceof TriangleStripMesh ) {
            Jogl2TriangleStripMeshRenderer.draw(gl, (TriangleStripMesh)g, q, false);
        }
        else if ( g instanceof TriangleMeshGroup ) {
            Jogl2TriangleMeshGroupRenderer.draw(gl, (TriangleMeshGroup)g,q);
        }
        else if ( g instanceof VoxelVolume ) {
            Jogl2VoxelVolumeRenderer.drawBinaryCubes(gl, (VoxelVolume)g, c, q);
        }
    }

    /**
    \todo  Homogenize all of the draw method signatures. Perhaps this code can
    be generalized to search the corresponding rendering class to a given
    Geometry via reflection, so this search should not be done explicitly.
    */
    public static void drawWithVertexArrays(GL2 gl, Geometry g, Camera c, RendererConfiguration q)
    {
        if ( g == null ) {
            VSDK.reportMessage(null, VSDK.WARNING,
                               "Jogl2GeometryRenderer.draw",
                               "null Geometry reference recieved");
            return;
        }

        if ( g instanceof Sphere ) {
            JoglCgSphereRenderer.draw(gl, (Sphere)g, c, q);
        }
        if ( g instanceof InfinitePlane ) {
            Jogl2InfinitePlaneRenderer.draw(gl, (InfinitePlane)g, c, q);
        }
        else if ( g instanceof Box ) {
            Jogl2BoxRenderer.draw(gl, (Box)g, c, q);
        }
        else if ( g instanceof Cone ) {
            Jogl2ConeRenderer.draw(gl, (Cone)g, c, q);
        }
        else if ( g instanceof Arrow ) {
            Jogl2ArrowRenderer.draw(gl, (Arrow)g, c, q);
        }
        else if ( g instanceof ParametricCurve ) {
            Jogl2ParametricCurveRenderer.draw(gl, (ParametricCurve)g, c, q);
        }
        else if ( g instanceof ParametricBiCubicPatch ) {
            Jogl2ParametricBiCubicPatchRenderer.draw(gl, (ParametricBiCubicPatch)g, c, q);
        }
        else if ( g instanceof PolyhedralBoundedSolid ) {
            Jogl2PolyhedralBoundedSolidRenderer.draw(gl, (PolyhedralBoundedSolid)g, c, q);
        }
        else if ( g instanceof TriangleMesh ) {
            Jogl2TriangleMeshRenderer.drawWithVertexArrays(gl, (TriangleMesh)g, q, false);
        }
        else if ( g instanceof QuadMesh ) {
            Jogl2QuadMeshRenderer.drawWithVertexArrays(gl, (QuadMesh)g, q, false);
        }
        else if ( g instanceof FunctionalExplicitSurface ) {
            Jogl2FunctionalExplicitSurfaceRenderer.draw(gl, (FunctionalExplicitSurface)g, c, q);
        }
        else if ( g instanceof TriangleStripMesh ) {
            Jogl2TriangleStripMeshRenderer.draw(gl, (TriangleStripMesh)g, q, false);
        }
        else if ( g instanceof TriangleMeshGroup ) {
            Jogl2TriangleMeshGroupRenderer.drawWithVertexArrays(gl, (TriangleMeshGroup)g,q);
        }
        else if ( g instanceof VoxelVolume ) {
            Jogl2VoxelVolumeRenderer.drawBinaryCubes(gl, (VoxelVolume)g, c, q);
        }
    }
}
