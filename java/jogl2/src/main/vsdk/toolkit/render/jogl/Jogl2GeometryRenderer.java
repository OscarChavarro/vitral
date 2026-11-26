package vsdk.toolkit.render.jogl;

// JOGL classes
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GL2;
import vsdk.toolkit.common.color.ColorRgb;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.logging.Logger;
import vsdk.toolkit.environment.geometry.element.Vertex;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.volume.Arrow;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.curve.ParametricCurve;
import vsdk.toolkit.environment.geometry.surface.ParametricBiCubicPatch;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.geometry.volume.Torus;
import vsdk.toolkit.environment.geometry.volume.Tube;
import vsdk.toolkit.environment.geometry.volume.Prism;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.surface.FunctionalExplicitSurface;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.geometry.surface.TriangleMeshGroup;
import vsdk.toolkit.environment.geometry.surface.TriangleStripMesh;
import vsdk.toolkit.environment.geometry.surface.QuadMesh;
import vsdk.toolkit.environment.geometry.volume.VoxelVolume;

import vsdk.toolkit.render.jogl.curve.Jogl2ParametricCurveRenderer;
import vsdk.toolkit.render.jogl.surface.Jogl2FunctionalExplicitSurfaceRenderer;
import vsdk.toolkit.render.jogl.surface.Jogl2InfinitePlaneRenderer;
import vsdk.toolkit.render.jogl.surface.Jogl2ParametricBiCubicPatchRenderer;
import vsdk.toolkit.render.jogl.surface.Jogl2QuadMeshRenderer;
import vsdk.toolkit.render.jogl.surface.Jogl2TriangleMeshGroupRenderer;
import vsdk.toolkit.render.jogl.surface.Jogl2TriangleMeshRenderer;
import vsdk.toolkit.render.jogl.surface.Jogl2TriangleStripMeshRenderer;
import vsdk.toolkit.render.jogl.volume.Jogl2ArrowRenderer;
import vsdk.toolkit.render.jogl.volume.Jogl2BoxRenderer;
import vsdk.toolkit.render.jogl.volume.Jogl2ConeRenderer;
import vsdk.toolkit.render.jogl.volume.Jogl2PrismRenderer;
import vsdk.toolkit.render.jogl.volume.Jogl2SphereRenderer;
import vsdk.toolkit.render.jogl.volume.Jogl2TorusRenderer;
import vsdk.toolkit.render.jogl.volume.Jogl2TubeRenderer;
import vsdk.toolkit.render.jogl.volume.Jogl2VoxelVolumeRenderer;
import vsdk.toolkit.render.jogl.volume.polyhedralBoundedSolid.Jogl2PolyhedralBoundedSolidRenderer;

public class Jogl2GeometryRenderer extends Jogl2Renderer 
{
    /// Meshes kept per context before the cache is flushed
    private static final int MAX_CACHED_MESHES = 256;

    /// Meshes of the mesh based renderers (see `keyOf`), by the context
    /// that holds their display lists and by mesh key
    private static final Map<GLContext, Map<String, Jogl2MeshRenderer.Mesh>>
        MESHES = new WeakHashMap<GLContext, Map<String, Jogl2MeshRenderer.Mesh>>();
    private static final Set<String> REPORTED_UNSUPPORTED = new HashSet<String>();


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
        Vector3Dd position = vertex.getPosition();
        Vector3Dd normal = vertex.getNormal();

        gl.glVertex3d(position.x() + (normal.x() * l/100),
                      position.y() + (normal.y() * l/100),
                      position.z() + (normal.z() * l/100));
        gl.glVertex3d(position.x() + (normal.x() * l),
                      position.y() + (normal.y() * l),
                      position.z() + (normal.z() * l));
    }

    public static void drawMinMaxBox(GL2 gl, double minmax[], RendererConfiguration q)
    {
        ColorRgb c = q.getBoundingVolumeColor();
        gl.glPushAttrib(GL2.GL_LIGHTING_BIT);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_TEXTURE_2D);
        // Warning: Change with configured color for bounding volume
        gl.glColor3d(c.r(), c.g(), c.b());
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

        Vector3Dd min, max, delta;
        min = new Vector3Dd(minmax[0], minmax[1], minmax[2]);
        max = new Vector3Dd(minmax[3], minmax[4], minmax[5]);
        delta = max.subtract(min);
        min = min.subtract(delta.multiply(borderPercent));
        max = max.add(delta.multiply(borderPercent));
        delta = delta.multiply(linePercent);

        gl.glPushAttrib(GL2.GL_LIGHTING_BIT);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_TEXTURE_2D);
        // Warning: Change with configured color for bounding volume
        gl.glColor3d(1, 1, 1);
        gl.glLineWidth(1.0f);
        gl.glBegin(GL.GL_LINES);

            gl.glVertex3d(min.x(), min.y(), min.z());
            gl.glVertex3d(min.x()+delta.x(), min.y(), min.z());
            gl.glVertex3d(min.x(), min.y(), min.z());
            gl.glVertex3d(min.x(), min.y()+delta.y(), min.z());
            gl.glVertex3d(min.x(), min.y(), min.z());
            gl.glVertex3d(min.x(), min.y(), min.z()+delta.z());

            gl.glVertex3d(max.x(), min.y(), min.z());
            gl.glVertex3d(max.x()-delta.x(), min.y(), min.z());
            gl.glVertex3d(max.x(), min.y(), min.z());
            gl.glVertex3d(max.x(), min.y()+delta.y(), min.z());
            gl.glVertex3d(max.x(), min.y(), min.z());
            gl.glVertex3d(max.x(), min.y(), min.z()+delta.z());

            gl.glVertex3d(min.x(), max.y(), min.z());
            gl.glVertex3d(min.x()+delta.x(), max.y(), min.z());
            gl.glVertex3d(min.x(), max.y(), min.z());
            gl.glVertex3d(min.x(), max.y()-delta.y(), min.z());
            gl.glVertex3d(min.x(), max.y(), min.z());
            gl.glVertex3d(min.x(), max.y(), min.z()+delta.z());

            gl.glVertex3d(min.x(), min.y(), max.z());
            gl.glVertex3d(min.x()+delta.x(), min.y(), max.z());
            gl.glVertex3d(min.x(), min.y(), max.z());
            gl.glVertex3d(min.x(), min.y()+delta.y(), max.z());
            gl.glVertex3d(min.x(), min.y(), max.z());
            gl.glVertex3d(min.x(), min.y(), max.z()-delta.z());

            gl.glVertex3d(max.x(), max.y(), min.z());
            gl.glVertex3d(max.x()-delta.x(), max.y(), min.z());
            gl.glVertex3d(max.x(), max.y(), min.z());
            gl.glVertex3d(max.x(), max.y()-delta.y(), min.z());
            gl.glVertex3d(max.x(), max.y(), min.z());
            gl.glVertex3d(max.x(), max.y(), min.z()+delta.z());

            gl.glVertex3d(max.x(), min.y(), max.z());
            gl.glVertex3d(max.x()-delta.x(), min.y(), max.z());
            gl.glVertex3d(max.x(), min.y(), max.z());
            gl.glVertex3d(max.x(), min.y()+delta.y(), max.z());
            gl.glVertex3d(max.x(), min.y(), max.z());
            gl.glVertex3d(max.x(), min.y(), max.z()-delta.z());

            gl.glVertex3d(min.x(), max.y(), max.z());
            gl.glVertex3d(min.x()+delta.x(), max.y(), max.z());
            gl.glVertex3d(min.x(), max.y(), max.z());
            gl.glVertex3d(min.x(), max.y()-delta.y(), max.z());
            gl.glVertex3d(min.x(), max.y(), max.z());
            gl.glVertex3d(min.x(), max.y(), max.z()-delta.z());

            gl.glVertex3d(max.x(), max.y(), max.z());
            gl.glVertex3d(max.x()-delta.x(), max.y(), max.z());
            gl.glVertex3d(max.x(), max.y(), max.z());
            gl.glVertex3d(max.x(), max.y()-delta.y(), max.z());
            gl.glVertex3d(max.x(), max.y(), max.z());
            gl.glVertex3d(max.x(), max.y(), max.z()-delta.z());

        gl.glEnd();

        gl.glPopAttrib();
    }

    public static void drawSelectionCorners(GL2 gl, Geometry g, RendererConfiguration q)
    {
        drawSelectionCorners(gl, g.getMinMax(), q);
    }

    /**
    \todo  Homogenize all of the draw method signatures. Perhaps this code can
    be generalized to search the corresponding rendering class to a given
    Geometry via reflection, so this search should not be done explicitly.
    */
    public static void draw(GL2 gl, Geometry g, Camera c, RendererConfiguration q)
    {
        if ( g == null ) {
            Logger.reportMessage(null, VSDK.WARNING,
                               "Jogl2GeometryRenderer.draw",
                               "null Geometry reference recieved");
            return;
        }

        if ( g instanceof Sphere ) {
            Jogl2SphereRenderer.draw(gl, (Sphere)g, c, q);
        }
        if ( g instanceof Torus ) {
            Jogl2TorusRenderer.draw(gl, (Torus)g, c, q);
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
        else if ( g instanceof Tube ) {
            Jogl2TubeRenderer.draw(gl, (Tube)g, c, q);
        }
        else if ( g instanceof Prism ) {
            Jogl2PrismRenderer.draw(gl, (Prism)g, c, q);
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
            Logger.reportMessage(null, VSDK.WARNING,
                               "Jogl2GeometryRenderer.draw",
                               "null Geometry reference recieved");
            return;
        }

        if ( g instanceof Sphere ) {
            Jogl2SphereRenderer.draw(gl, (Sphere)g, c, q);
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
        else if ( g instanceof Tube ) {
            Jogl2TubeRenderer.draw(gl, (Tube)g, c, q);
        }
        else if ( g instanceof Prism ) {
            Jogl2PrismRenderer.draw(gl, (Prism)g, c, q);
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

    //= Mesh based renderers =============================================

    /**
    @return a string that identifies the mesh of the geometry (given by the
    `meshKey` of its renderer), or null if the geometry is not tessellated to
    a mesh by this pipeline
    */
    private static String keyOf(Geometry geometry)
    {
        if ( geometry instanceof Tube t ) {
            return Jogl2TubeRenderer.meshKey(t);
        }
        if ( geometry instanceof Prism p ) {
            return Jogl2PrismRenderer.meshKey(p);
        }
        return null;
    }

    /**
    @return the mesh of the geometry, built by its renderer, or null if it
    can not be tessellated
    */
    private static Jogl2MeshRenderer.Mesh buildMesh(Geometry geometry)
    {
        if ( geometry instanceof Tube t ) {
            return Jogl2TubeRenderer.buildMesh(t);
        }
        if ( geometry instanceof Prism p ) {
            return Jogl2PrismRenderer.buildMesh(p);
        }
        return null;
    }

    private static void releaseMeshes(GL2 gl,
        Map<String, Jogl2MeshRenderer.Mesh> meshes)
    {
        for ( Jogl2MeshRenderer.Mesh mesh : meshes.values() ) {
            Jogl2MeshRenderer.release(gl, mesh);
        }
        meshes.clear();
    }

    private static Jogl2MeshRenderer.Mesh obtainMesh(GL2 gl, Geometry geometry)
    {
        String key = keyOf(geometry);

        if ( key == null ) {
            String name = geometry.getClass().getSimpleName();
            if ( REPORTED_UNSUPPORTED.add(name) ) {
                Logger.reportMessage(null, VSDK.WARNING, "Jogl2GeometryRenderer",
                    "Geometry not supported by the JOGL2 mesh pipeline: " + name);
            }
            return null;
        }
        GLContext context = gl.getContext();
        Map<String, Jogl2MeshRenderer.Mesh> meshes = MESHES.get(context);
        if ( meshes == null ) {
            meshes = new HashMap<String, Jogl2MeshRenderer.Mesh>();
            MESHES.put(context, meshes);
        }
        Jogl2MeshRenderer.Mesh mesh = meshes.get(key);

        if ( mesh == null ) {
            mesh = buildMesh(geometry);
            if ( mesh == null ) {
                return null;
            }
            if ( meshes.size() >= MAX_CACHED_MESHES ) {
                // Stale meshes (i.e. from geometries edited many times) are
                // released, and recreated on demand
                releaseMeshes(gl, meshes);
            }
            meshes.put(key, mesh);
        }
        return mesh;
    }

    /**
    Draws a geometry handled by a mesh based renderer (see `keyOf`): its
    mesh is built once per context and parameters, and drawn by
    `Jogl2MeshRenderer` with the current lights, material and texture.
    @param gl OpenGL context
    @param geometry geometry to draw
    @param camera camera that views the geometry
    @param quality bits of rendering configuration
    */
    public static void drawMesh(GL2 gl, Geometry geometry, Camera camera,
        RendererConfiguration quality)
    {
        if ( geometry == null || quality == null ) {
            return;
        }
        Jogl2MeshRenderer.Mesh mesh = obtainMesh(gl, geometry);
        if ( mesh == null ) {
            return;
        }
        Jogl2MeshRenderer.draw(gl, mesh, geometry, camera, quality);
    }

    /**
    Releases the meshes of the mesh based renderers created in the current
    context. PRE: `gl` is current.
    @param gl OpenGL context
    */
    public static void dispose(GL2 gl)
    {
        Map<String, Jogl2MeshRenderer.Mesh> meshes = MESHES.remove(gl.getContext());
        if ( meshes != null ) {
            releaseMeshes(gl, meshes);
        }
    }
}
