package vsdk.toolkit.render.jogl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.SpherePolyhedralCache;

/**
Renders a `Sphere` as a tessellated mesh (see `Jogl4MeshRenderer`). The sphere
is converted into a polyhedral bounded solid by `SpherePolyhedralCache`, shared
with the renderers of the other technologies, and its triangles (with the
attributes of the parametric surface) are uploaded once. The meshes are cached
by radius and resolution, so many spheres can be drawn per frame.
*/
public class Jogl4SphereRenderer extends Jogl4Renderer {
    private static final int DEFAULT_SLICES = 32;
    private static final int DEFAULT_STACKS = 16;

    private static final Map<String, Jogl4MeshRenderer.Mesh> MESHES = new HashMap<>();

    public static void draw(
        GL4 gl,
        Sphere sphere,
        Camera camera,
        Light light,
        SimpleMaterial material,
        RendererConfiguration quality,
        RGBImageUncompressed textureMap,
        RGBImageUncompressed normalMap)
    {
        draw(
            gl,
            sphere,
            camera,
            light,
            material,
            quality,
            textureMap,
            normalMap,
            DEFAULT_SLICES,
            DEFAULT_STACKS);
    }

    public static void draw(
        GL4 gl,
        Sphere sphere,
        Camera camera,
        Light light,
        SimpleMaterial material,
        RendererConfiguration quality,
        RGBImageUncompressed textureMap,
        RGBImageUncompressed normalMap,
        int slices,
        int stacks)
    {
        draw(
            gl,
            sphere,
            camera,
            light,
            material,
            quality,
            textureMap,
            normalMap,
            Matrix4x4d.identityMatrix(),
            slices,
            stacks);
    }

    public static void draw(
        GL4 gl,
        Sphere sphere,
        Camera camera,
        Light light,
        SimpleMaterial material,
        RendererConfiguration quality,
        RGBImageUncompressed textureMap,
        RGBImageUncompressed normalMap,
        Matrix4x4d modelViewLocal,
        int slices,
        int stacks)
    {
        if ( sphere == null || camera == null || material == null || quality == null ) {
            return;
        }

        List<Light> lights = null;

        if ( light != null ) {
            lights = new ArrayList<Light>();
            lights.add(light);
        }
        draw(
            gl,
            sphere,
            camera,
            lights,
            material,
            quality,
            textureMap,
            normalMap,
            modelViewLocal,
            slices,
            stacks);
    }

    public static void draw(
        GL4 gl,
        Sphere sphere,
        Camera camera,
        List<Light> lights,
        SimpleMaterial material,
        RendererConfiguration quality,
        RGBImageUncompressed textureMap,
        RGBImageUncompressed normalMap,
        Matrix4x4d modelViewLocal,
        int slices,
        int stacks)
    {
        if ( sphere == null || camera == null || material == null || quality == null ) {
            return;
        }

        Jogl4MeshRenderer.draw(
            gl,
            obtainMesh(sphere, slices, stacks),
            sphere,
            camera,
            lights,
            material,
            quality,
            textureMap,
            normalMap,
            modelViewLocal);
    }

    public static void dispose(GL4 gl)
    {
        for ( Jogl4MeshRenderer.Mesh mesh : MESHES.values() ) {
            Jogl4MeshRenderer.release(gl, mesh);
        }
        MESHES.clear();
        Jogl4MeshRenderer.dispose(gl);
    }

    /**
    @return the mesh of the sphere for the rasterizer, built once from the
    tessellation shared by every sphere renderer (see
    `SpherePolyhedralCache`): `stacks` rings of vertices, poles included,
    are `stacks - 1` parallels of the polyhedral bounded solid
    */
    private static Jogl4MeshRenderer.Mesh obtainMesh(Sphere sphere, int requestedSlices, int requestedStacks)
    {
        int slices = Math.max(12, requestedSlices);
        int stacks = Math.max(8, requestedStacks);
        String key = sphere.getRadius() + "/" + slices + "/" + stacks;
        Jogl4MeshRenderer.Mesh mesh = MESHES.get(key);

        if ( mesh == null ) {
            SpherePolyhedralCache.Entry entry =
                SpherePolyhedralCache.obtain(sphere, slices, stacks - 1);

            mesh = new Jogl4MeshRenderer.Mesh(entry.getPositions(),
                entry.getNormals(), entry.getUvs(), entry.getTangents(),
                entry.getBiNormals(), sphere.getRadius());
            MESHES.put(key, mesh);
        }
        return mesh;
    }
}
