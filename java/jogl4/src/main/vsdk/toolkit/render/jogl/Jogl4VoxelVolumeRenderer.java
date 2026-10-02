package vsdk.toolkit.render.jogl;

import java.util.List;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.volume.VoxelVolume;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.media.RGBImageUncompressed;

/**
Renders a `VoxelVolume` with the GL4 pipeline, as the binary cubes of
`Jogl2VoxelVolumeRenderer`: every filled voxel (whose value reaches the
threshold of the volume, see `VoxelVolume.isFilled`) is shown as a cube, inside the volume space (the cube from <-1, -1, -1> to
<1, 1, 1>). Instead of drawing one cube per voxel, the volume is tessellated
once into a mesh with only the faces of the voxels that border an empty voxel
(or the limits of the volume), so the result looks the same with a fraction
of the triangles. The mesh is cached by `Jogl4GeometryRenderer`, identified by
the volume and its threshold: as triangle meshes, volumes are not expected
to be edited while shown.
*/
public class Jogl4VoxelVolumeRenderer extends Jogl4Renderer {
    private Jogl4VoxelVolumeRenderer() {
    }

    /**
    Draws the volume, with the passes selected by the configuration (see
    `Jogl4MeshRenderer`).

    @param gl OpenGL context
    @param volume volume to draw
    @param camera camera that views the volume
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the volume
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from volume space to world space
    */
    public static void draw(
        GL4 gl,
        VoxelVolume volume,
        Camera camera,
        List<Light> lights,
        SimpleMaterial material,
        RendererConfiguration quality,
        RGBImageUncompressed textureMap,
        RGBImageUncompressed normalMap,
        Matrix4x4d localTransform)
    {
        Jogl4GeometryRenderer.draw(gl, volume, camera, lights, material, quality,
            textureMap, normalMap, localTransform);
    }

    /**
    @param volume a volume
    @return a string that identifies the mesh of the volume with its current
    threshold
    */
    static String meshKey(VoxelVolume volume)
    {
        return "voxelvolume/" + System.identityHashCode(volume) + "/" +
            volume.getThreshold();
    }

    /**
    @param volume volume to tessellate
    @return the mesh of the boundary faces of the filled voxels, or null if the
    volume has no filled voxels
    */
    static Jogl4MeshRenderer.Mesh buildMesh(VoxelVolume volume)
    {
        int[] size = { volume.getXSize(), volume.getYSize(), volume.getZSize() };
        double[] step = { 2.0 / size[0], 2.0 / size[1], 2.0 / size[2] };
        Jogl4MeshBuilder builder = new Jogl4MeshBuilder(2.0);
        int faceCount = 0;
        int x;
        int y;
        int z;

        for ( z = 0; z < size[2]; z++ ) {
            for ( y = 0; y < size[1]; y++ ) {
                for ( x = 0; x < size[0]; x++ ) {
                    if ( !volume.isFilled(x, y, z) ) {
                        continue;
                    }
                    int[] cell = { x, y, z };
                    for ( int axis = 0; axis < 3; axis++ ) {
                        for ( int side = -1; side <= 1; side += 2 ) {
                            int[] neighbor = { x, y, z };
                            neighbor[axis] += side;
                            if ( !volume.isFilled(neighbor[0], neighbor[1], neighbor[2]) ) {
                                addVoxelFace(builder, cell, step, axis, side > 0);
                                faceCount++;
                            }
                        }
                    }
                }
            }
        }
        if ( faceCount == 0 ) {
            return null;
        }
        return builder.build();
    }

    /**
    Adds the face of a voxel perpendicular to an axis, counterclockwise seen
    from outside the voxel.

    @param builder builder receiving the face
    @param cell indexes of the voxel
    @param step size of a voxel along each axis
    @param axis axis perpendicular to the face (0: x, 1: y, 2: z)
    @param positive true for the face on the positive side of the axis
    */
    private static void addVoxelFace(Jogl4MeshBuilder builder, int[] cell,
        double[] step, int axis, boolean positive)
    {
        // The other two axes, in cyclic order, so u x v points along +axis
        int uAxis = (axis + 1) % 3;
        int vAxis = (axis + 2) % 3;
        double[] base = new double[3];
        double[] u = new double[3];
        double[] v = new double[3];
        double[] n = new double[3];

        for ( int i = 0; i < 3; i++ ) {
            base[i] = cell[i] * step[i] - 1;
        }
        if ( positive ) {
            base[axis] += step[axis];
        }
        u[uAxis] = step[uAxis];
        v[vAxis] = step[vAxis];
        n[axis] = positive ? 1 : -1;

        Vector3Dd p0 = new Vector3Dd(base[0], base[1], base[2]);
        Vector3Dd p1 = new Vector3Dd(base[0] + u[0], base[1] + u[1], base[2] + u[2]);
        Vector3Dd p2 = new Vector3Dd(base[0] + u[0] + v[0], base[1] + u[1] + v[1],
            base[2] + u[2] + v[2]);
        Vector3Dd p3 = new Vector3Dd(base[0] + v[0], base[1] + v[1], base[2] + v[2]);
        Vector3Dd normal = new Vector3Dd(n[0], n[1], n[2]);

        if ( positive ) {
            builder.addQuad(p0, normal, 0, 0, p1, normal, 1, 0,
                p2, normal, 1, 1, p3, normal, 0, 1);
        }
        else {
            builder.addQuad(p0, normal, 0, 0, p3, normal, 0, 1,
                p2, normal, 1, 1, p1, normal, 1, 0);
        }
    }
}
