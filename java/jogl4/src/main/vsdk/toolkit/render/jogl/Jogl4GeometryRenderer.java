package vsdk.toolkit.render.jogl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.logging.Logger;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.curve.ParametricCurve;
import vsdk.toolkit.environment.geometry.surface.FunctionalExplicitSurface;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.surface.ParametricBiCubicPatch;
import vsdk.toolkit.environment.geometry.surface.QuadMesh;
import vsdk.toolkit.environment.geometry.surface.SubdividedBezierPatch;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.geometry.surface.TriangleMeshGroup;
import vsdk.toolkit.environment.geometry.surface.TriangleStripMesh;
import vsdk.toolkit.environment.geometry.volume.Arrow;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.geometry.volume.HeightFieldSolid;
import vsdk.toolkit.environment.geometry.volume.ImplicitSolid;
import vsdk.toolkit.environment.geometry.volume.PlanarHalfSpace;
import vsdk.toolkit.environment.geometry.volume.QuadricSurface;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.geometry.volume.Torus;
import vsdk.toolkit.environment.geometry.volume.Volume;
import vsdk.toolkit.environment.geometry.volume.VoxelVolume;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.jogl.curve.Jogl4ParametricCurveRenderer;
import vsdk.toolkit.render.jogl.surface.Jogl4FunctionalExplicitSurfaceRenderer;
import vsdk.toolkit.render.jogl.surface.Jogl4InfinitePlaneRenderer;
import vsdk.toolkit.render.jogl.surface.Jogl4ParametricBiCubicPatchRenderer;
import vsdk.toolkit.render.jogl.surface.Jogl4QuadMeshRenderer;
import vsdk.toolkit.render.jogl.surface.Jogl4SubdividedBezierPatchRenderer;
import vsdk.toolkit.render.jogl.surface.Jogl4TriangleMeshGroupRenderer;
import vsdk.toolkit.render.jogl.surface.Jogl4TriangleMeshRenderer;
import vsdk.toolkit.render.jogl.surface.Jogl4TriangleStripMeshRenderer;
import vsdk.toolkit.render.jogl.volume.Jogl4ArrowRenderer;
import vsdk.toolkit.render.jogl.volume.Jogl4BoxRenderer;
import vsdk.toolkit.render.jogl.volume.Jogl4ConeRenderer;
import vsdk.toolkit.render.jogl.volume.Jogl4HeightFieldSolidRenderer;
import vsdk.toolkit.render.jogl.volume.Jogl4ImplicitSolidRenderer;
import vsdk.toolkit.render.jogl.volume.Jogl4PlanarHalfSpaceRenderer;
import vsdk.toolkit.render.jogl.volume.Jogl4QuadricSurfaceRenderer;
import vsdk.toolkit.render.jogl.volume.Jogl4SphereRenderer;
import vsdk.toolkit.render.jogl.volume.Jogl4TorusRenderer;
import vsdk.toolkit.render.jogl.volume.Jogl4VolumeRenderer;
import vsdk.toolkit.render.jogl.volume.Jogl4VoxelVolumeRenderer;
import vsdk.toolkit.render.jogl.volume.polyhedralBoundedSolid.Jogl4PolyhedralBoundedSolidRenderer;

/**
Renders the geometries of the toolkit with the GL4 pipeline. Each geometry
has its own `Jogl4*Renderer`, in the package that mirrors the package of the
geometry (`curve`, `surface`, `volume`), that tessellates it into a mesh
(`meshKey` and `buildMesh`). This class dispatches the drawing to them, and
keeps the meshes (cached by their keys), that are drawn by
`Jogl4MeshRenderer`, so every geometry honors every bit of the
`RendererConfiguration` in the same way (surfaces, wires, points, normals,
bounding volume, texture, bump map and shading).

Some geometries are drawn by their renderers in their own way: a `Sphere` by
`Jogl4SphereRenderer`, a `ParametricCurve` (it has no surface) as lines by
`Jogl4ParametricCurveRenderer`, a `TriangleMeshGroup` mesh by mesh, a
`PlanarHalfSpace` by its boundary plane, and a `PolyhedralBoundedSolid` by
`Jogl4PolyhedralBoundedSolidRenderer` (without texture nor normal map). Any
other volume is drawn, as the last resort, from its boundary representation
(see `Jogl4VolumeRenderer`).
*/
public class Jogl4GeometryRenderer extends Jogl4Renderer {
    private static final int MAX_CACHED_MESHES = 256;

    private static final Map<String, Jogl4MeshRenderer.Mesh> MESHES = new HashMap<>();
    private static final Set<String> REPORTED_UNSUPPORTED = new HashSet<>();

    private Jogl4GeometryRenderer() {
    }

    /**
    @param geometry a geometry
    @return true if this renderer can draw the geometry
    */
    public static boolean isSupported(Geometry geometry)
    {
        return geometry instanceof Sphere ||
            geometry instanceof ParametricCurve ||
            geometry instanceof TriangleMeshGroup ||
            geometry instanceof PlanarHalfSpace ||
            geometry instanceof PolyhedralBoundedSolid ||
            keyOf(geometry) != null ||
            (geometry instanceof Volume volume &&
             Jogl4VolumeRenderer.exportedSolid(volume) != null);
    }

    /**
    Draws a geometry lit by a single light.
    See the overload taking a list of lights.

    @param gl OpenGL context
    @param geometry geometry to draw
    @param camera camera that views the geometry
    @param light light of the scene, or null to use a light at the camera
    @param material material of the geometry
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from geometry space to world space
    */
    public static void draw(
        GL4 gl,
        Geometry geometry,
        Camera camera,
        Light light,
        SimpleMaterial material,
        RendererConfiguration quality,
        RGBImageUncompressed textureMap,
        RGBImageUncompressed normalMap,
        Matrix4x4d localTransform)
    {
        List<Light> lights = null;

        if ( light != null ) {
            lights = new ArrayList<Light>();
            lights.add(light);
        }
        draw(gl, geometry, camera, lights, material, quality, textureMap,
            normalMap, localTransform);
    }

    /**
    Draws a geometry.

    @param gl OpenGL context
    @param geometry geometry to draw
    @param camera camera that views the geometry
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the geometry
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from geometry space to world space
    */
    public static void draw(
        GL4 gl,
        Geometry geometry,
        Camera camera,
        List<Light> lights,
        SimpleMaterial material,
        RendererConfiguration quality,
        RGBImageUncompressed textureMap,
        RGBImageUncompressed normalMap,
        Matrix4x4d localTransform)
    {
        if ( geometry == null ) {
            return;
        }
        if ( geometry instanceof Sphere sphere ) {
            Jogl4SphereRenderer.draw(gl, sphere, camera, lights, material, quality,
                textureMap, normalMap, localTransform,
                Jogl4SphereRenderer.SLICES, Jogl4SphereRenderer.SLICES / 2);
            return;
        }
        if ( geometry instanceof ParametricCurve curve ) {
            Jogl4ParametricCurveRenderer.draw(gl, curve, camera, quality, localTransform);
            return;
        }
        if ( geometry instanceof TriangleMeshGroup group ) {
            Jogl4TriangleMeshGroupRenderer.draw(gl, group, camera, lights, material,
                quality, textureMap, normalMap, localTransform);
            return;
        }
        if ( geometry instanceof PlanarHalfSpace halfSpace ) {
            Jogl4PlanarHalfSpaceRenderer.draw(gl, halfSpace, camera, lights,
                material, quality, textureMap, normalMap, localTransform);
            return;
        }
        if ( geometry instanceof PolyhedralBoundedSolid solid ) {
            Jogl4PolyhedralBoundedSolidRenderer.draw(gl, solid, camera, lights,
                material, quality, localTransform);
            return;
        }
        if ( keyOf(geometry) == null && geometry instanceof Volume volume &&
             Jogl4VolumeRenderer.draw(gl, volume, camera, lights, material,
                 quality, localTransform) ) {
            return;
        }
        Jogl4MeshRenderer.Mesh mesh = obtainMesh(gl, geometry);
        if ( mesh == null ) {
            return;
        }
        if ( geometry instanceof InfinitePlane plane && quality != null ) {
            Jogl4InfinitePlaneRenderer.drawMesh(gl, mesh, plane, camera, lights,
                material, quality, textureMap, normalMap, localTransform);
            return;
        }
        Jogl4MeshRenderer.draw(gl, mesh, geometry, camera, lights, material, quality,
            textureMap, normalMap, localTransform);
    }

    /**
    Releases the OpenGL resources of the meshes. PRE: the OpenGL context that
    created them is current.
    @param gl OpenGL context
    */
    public static void dispose(GL4 gl)
    {
        releaseMeshes(gl);
        Jogl4VolumeRenderer.dispose();
        Jogl4SphereRenderer.dispose(gl);
    }

    private static void releaseMeshes(GL4 gl)
    {
        for ( Jogl4MeshRenderer.Mesh mesh : MESHES.values() ) {
            Jogl4MeshRenderer.release(gl, mesh);
        }
        MESHES.clear();
    }

    private static Jogl4MeshRenderer.Mesh obtainMesh(GL4 gl, Geometry geometry)
    {
        String key = keyOf(geometry);

        if ( key == null ) {
            String name = geometry.getClass().getSimpleName();
            if ( REPORTED_UNSUPPORTED.add(name) ) {
                Logger.reportMessage(null, VSDK.WARNING, "Jogl4GeometryRenderer",
                    "Geometry not supported by the GL4 pipeline yet: " + name);
            }
            return null;
        }
        Jogl4MeshRenderer.Mesh mesh = MESHES.get(key);

        if ( mesh == null ) {
            mesh = buildMesh(geometry);
            if ( mesh == null ) {
                return null;
            }
            if ( MESHES.size() >= MAX_CACHED_MESHES ) {
                // Stale meshes (i.e. from geometries edited many times) are
                // released, and recreated on demand
                releaseMeshes(gl);
            }
            MESHES.put(key, mesh);
        }
        return mesh;
    }

    /**
    @return a string that identifies the mesh of the geometry (given by the
    `meshKey` of its renderer), or null if the geometry is not tessellated to
    a mesh
    */
    private static String keyOf(Geometry geometry)
    {
        if ( geometry instanceof Cone c ) {
            return Jogl4ConeRenderer.meshKey(c);
        }
        if ( geometry instanceof Arrow a ) {
            return Jogl4ArrowRenderer.meshKey(a);
        }
        if ( geometry instanceof Box b ) {
            return Jogl4BoxRenderer.meshKey(b);
        }
        if ( geometry instanceof Torus t ) {
            return Jogl4TorusRenderer.meshKey(t);
        }
        if ( geometry instanceof TriangleMesh m ) {
            return Jogl4TriangleMeshRenderer.meshKey(m);
        }
        if ( geometry instanceof QuadMesh m ) {
            return Jogl4QuadMeshRenderer.meshKey(m);
        }
        if ( geometry instanceof TriangleStripMesh m ) {
            return Jogl4TriangleStripMeshRenderer.meshKey(m);
        }
        if ( geometry instanceof FunctionalExplicitSurface f ) {
            return Jogl4FunctionalExplicitSurfaceRenderer.meshKey(f);
        }
        if ( geometry instanceof ParametricBiCubicPatch p ) {
            return Jogl4ParametricBiCubicPatchRenderer.meshKey(p);
        }
        if ( geometry instanceof SubdividedBezierPatch p ) {
            return Jogl4SubdividedBezierPatchRenderer.meshKey(p);
        }
        if ( geometry instanceof InfinitePlane p ) {
            return Jogl4InfinitePlaneRenderer.meshKey(p);
        }
        if ( geometry instanceof VoxelVolume v ) {
            return Jogl4VoxelVolumeRenderer.meshKey(v);
        }
        if ( geometry instanceof QuadricSurface q ) {
            return Jogl4QuadricSurfaceRenderer.meshKey(q);
        }
        if ( geometry instanceof ImplicitSolid s ) {
            return Jogl4ImplicitSolidRenderer.meshKey(s);
        }
        if ( geometry instanceof HeightFieldSolid h ) {
            return Jogl4HeightFieldSolidRenderer.meshKey(h);
        }
        return null;
    }

    /**
    @return the mesh of the geometry, built by its renderer, or null if it
    can not be tessellated
    */
    private static Jogl4MeshRenderer.Mesh buildMesh(Geometry geometry)
    {
        if ( geometry instanceof Cone c ) {
            return Jogl4ConeRenderer.buildMesh(c);
        }
        if ( geometry instanceof Arrow a ) {
            return Jogl4ArrowRenderer.buildMesh(a);
        }
        if ( geometry instanceof Box b ) {
            return Jogl4BoxRenderer.buildMesh(b);
        }
        if ( geometry instanceof Torus t ) {
            return Jogl4TorusRenderer.buildMesh(t);
        }
        if ( geometry instanceof TriangleMesh m ) {
            return Jogl4TriangleMeshRenderer.buildMesh(m);
        }
        if ( geometry instanceof QuadMesh m ) {
            return Jogl4QuadMeshRenderer.buildMesh(m);
        }
        if ( geometry instanceof TriangleStripMesh m ) {
            return Jogl4TriangleStripMeshRenderer.buildMesh(m);
        }
        if ( geometry instanceof FunctionalExplicitSurface f ) {
            return Jogl4FunctionalExplicitSurfaceRenderer.buildMesh(f);
        }
        if ( geometry instanceof ParametricBiCubicPatch p ) {
            return Jogl4ParametricBiCubicPatchRenderer.buildMesh(p);
        }
        if ( geometry instanceof SubdividedBezierPatch p ) {
            return Jogl4SubdividedBezierPatchRenderer.buildMesh(p);
        }
        if ( geometry instanceof InfinitePlane p ) {
            return Jogl4InfinitePlaneRenderer.buildMesh(p);
        }
        if ( geometry instanceof VoxelVolume v ) {
            return Jogl4VoxelVolumeRenderer.buildMesh(v);
        }
        if ( geometry instanceof QuadricSurface q ) {
            return Jogl4QuadricSurfaceRenderer.buildMesh(q);
        }
        if ( geometry instanceof ImplicitSolid s ) {
            return Jogl4ImplicitSolidRenderer.buildMesh(s);
        }
        if ( geometry instanceof HeightFieldSolid h ) {
            return Jogl4HeightFieldSolidRenderer.buildMesh(h);
        }
        return null;
    }
}
