package vsdk.toolkit.render.jogl.volume;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.volume.Volume;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.render.jogl.Jogl4Renderer;
import vsdk.toolkit.render.jogl.volume.polyhedralBoundedSolid.Jogl4PolyhedralBoundedSolidRenderer;

/**
Renders, as the last resort for the volumes that have no direct tessellation
in the GL4 pipeline, the boundary representation of a `Volume` (see
`Volume.exportToPolyhedralBoundedSolid`) with
`Jogl4PolyhedralBoundedSolidRenderer` (without texture nor normal map). The
representation is exported once per volume, and kept while it is shown: as
triangle meshes, such volumes are not expected to be edited while shown.
*/
public class Jogl4VolumeRenderer extends Jogl4Renderer {
    private static final int MAX_CACHED_SOLIDS = 256;

    /// Boundary representations of the volumes drawn, by identity
    private static final Map<Volume, PolyhedralBoundedSolid> EXPORTED_SOLIDS =
        new IdentityHashMap<>();

    private Jogl4VolumeRenderer() {
    }

    /**
    Draws the boundary representation of the volume, if it has one.

    @param gl OpenGL context
    @param volume volume to draw
    @param camera camera that views the volume
    @param lights lights of the scene, or null for none
    @param material material of the volume
    @param quality bits of rendering configuration
    @param localTransform transformation from volume space to world space
    @return true if the volume has a boundary representation (and it was
    drawn)
    */
    public static boolean draw(
        GL4 gl,
        Volume volume,
        Camera camera,
        List<Light> lights,
        SimpleMaterial material,
        RendererConfiguration quality,
        Matrix4x4d localTransform)
    {
        PolyhedralBoundedSolid solid = exportedSolid(volume);
        if ( solid == null ) {
            return false;
        }
        Jogl4PolyhedralBoundedSolidRenderer.draw(gl, solid, camera, lights,
            material, quality, localTransform);
        return true;
    }

    /**
    @param volume a volume
    @return the boundary representation of the volume (exported once, and
    kept while the volume is shown), or null if it has none
    */
    public static PolyhedralBoundedSolid exportedSolid(Volume volume)
    {
        if ( volume instanceof PolyhedralBoundedSolid solid ) {
            return solid;
        }
        if ( EXPORTED_SOLIDS.containsKey(volume) ) {
            return EXPORTED_SOLIDS.get(volume);
        }
        PolyhedralBoundedSolid solid = volume.exportToPolyhedralBoundedSolid();
        if ( EXPORTED_SOLIDS.size() >= MAX_CACHED_SOLIDS ) {
            EXPORTED_SOLIDS.clear();
        }
        // A null export is kept too, so it is not tried again on each frame
        EXPORTED_SOLIDS.put(volume, solid);
        return solid;
    }

    /**
    Forgets the exported boundary representations.
    */
    public static void dispose()
    {
        EXPORTED_SOLIDS.clear();
    }
}
