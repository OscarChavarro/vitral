package vsdk.toolkit.render.jogl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.media.RGBImageUncompressed;

/**
Renders a `Sphere` as a tessellated mesh (see `Jogl4MeshRenderer`). The meshes
are cached by radius and resolution, so many spheres can be drawn per frame.
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

    private static Jogl4MeshRenderer.Mesh obtainMesh(Sphere sphere, int requestedSlices, int requestedStacks)
    {
        int slices = Math.max(12, requestedSlices);
        int stacks = Math.max(8, requestedStacks);
        String key = sphere.getRadius() + "/" + slices + "/" + stacks;
        Jogl4MeshRenderer.Mesh mesh = MESHES.get(key);

        if ( mesh == null ) {
            mesh = buildMesh(sphere, slices, stacks);
            MESHES.put(key, mesh);
        }
        return mesh;
    }

    private static Jogl4MeshRenderer.Mesh buildMesh(Sphere sphere, int slices, int stacks)
    {
        int triangles = (stacks - 1) * slices * 2;
        int vertices = triangles * 3;

        float[] positions = new float[vertices * 3];
        float[] normals = new float[vertices * 3];
        float[] uvs = new float[vertices * 2];
        float[] tangents = new float[vertices * 3];
        float[] biNormals = new float[vertices * 3];

        int posIndex = 0;
        int uvIndex = 0;

        for ( int i = 0; i < stacks - 1; i++ ) {
            double t0 = i / (double)(stacks - 1);
            double t1 = (i + 1) / (double)(stacks - 1);
            double phi0 = Math.PI * t0 - Math.PI / 2;
            double phi1 = Math.PI * t1 - Math.PI / 2;

            for ( int j = 0; j < slices; j++ ) {
                double s0 = j / (double)slices;
                double s1 = (j + 1) / (double)slices;
                double theta0 = 2 * Math.PI * s0;
                double theta1 = 2 * Math.PI * s1;

                posIndex = addVertex(sphere, theta0, phi0, positions, normals, tangents, biNormals, posIndex);
                uvIndex = addUv(s0, t0, uvs, uvIndex);

                posIndex = addVertex(sphere, theta0, phi1, positions, normals, tangents, biNormals, posIndex);
                uvIndex = addUv(s0, t1, uvs, uvIndex);

                posIndex = addVertex(sphere, theta1, phi1, positions, normals, tangents, biNormals, posIndex);
                uvIndex = addUv(s1, t1, uvs, uvIndex);

                posIndex = addVertex(sphere, theta0, phi0, positions, normals, tangents, biNormals, posIndex);
                uvIndex = addUv(s0, t0, uvs, uvIndex);

                posIndex = addVertex(sphere, theta1, phi1, positions, normals, tangents, biNormals, posIndex);
                uvIndex = addUv(s1, t1, uvs, uvIndex);

                posIndex = addVertex(sphere, theta1, phi0, positions, normals, tangents, biNormals, posIndex);
                uvIndex = addUv(s1, t0, uvs, uvIndex);
            }
        }

        return new Jogl4MeshRenderer.Mesh(positions, normals, uvs, tangents,
            biNormals, sphere.getRadius());
    }

    private static int addVertex(
        Sphere sphere,
        double theta,
        double phi,
        float[] positions,
        float[] normals,
        float[] tangents,
        float[] biNormals,
        int index)
    {
        Vector3Dd p = sphere.spherePosition(theta, phi);
        Vector3Dd n = sphere.sphereNormal(theta, phi);
        Vector3Dd tangent = sphere.sphereTangent(theta, phi);
        Vector3Dd biNormal = sphere.sphereBinormal(theta, phi);

        positions[index] = (float)p.x();
        normals[index] = (float)n.x();
        tangents[index] = (float)tangent.x();
        biNormals[index] = (float)biNormal.x();
        index++;

        positions[index] = (float)p.y();
        normals[index] = (float)n.y();
        tangents[index] = (float)tangent.y();
        biNormals[index] = (float)biNormal.y();
        index++;

        positions[index] = (float)p.z();
        normals[index] = (float)n.z();
        tangents[index] = (float)tangent.z();
        biNormals[index] = (float)biNormal.z();
        index++;

        return index;
    }

    private static int addUv(double s, double t, float[] uvs, int index)
    {
        uvs[index++] = (float)(1.0 - s);
        uvs[index++] = (float)t;
        return index;
    }
}
