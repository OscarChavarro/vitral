import {
    PolyhedralBoundedSolid,
    type Camera,
    type Light,
    type Matrix4x4d,
    type RendererConfiguration,
    type SimpleMaterial,
    type Volume,
} from "@vitral/base";
import { WebGLPolyhedralBoundedSolidRenderer } from "./polyhedralBoundedSolid/WebGLPolyhedralBoundedSolidRenderer.js";

/**
Port of `vsdk.toolkit.render.jogl.volume.Jogl4VolumeRenderer`.

Renders, as the last resort for the volumes that have no direct tessellation
in the WebGL pipeline, the boundary representation of a `Volume` (see
`Volume.exportToPolyhedralBoundedSolid`) with
`WebGLPolyhedralBoundedSolidRenderer` (without texture nor normal map). The
representation is exported once per volume, and kept while it is shown: as
triangle meshes, such volumes are not expected to be edited while shown.

Java's `IdentityHashMap` is a JavaScript `Map`, whose object keys are
compared by identity. `Volume.exportToPolyhedralBoundedSolid` is declared in
the base module to return an `object` (the base classes can not import the
solid), so its result is narrowed here. Drawing is asynchronous because a
GLSL source arrives over `fetch`, so `draw` resolves to Java's boolean.
*/
export class WebGLVolumeRenderer {
    private static readonly MAX_CACHED_SOLIDS: number = 256;
    /// Boundary representations of the volumes drawn, by identity
    private static readonly EXPORTED_SOLIDS: Map<Volume, PolyhedralBoundedSolid | null> = new Map<
        Volume,
        PolyhedralBoundedSolid | null
    >();

    private constructor() {}

    /**
    Draws the boundary representation of the volume, if it has one.
    @return true if the volume has a boundary representation (and it was
    drawn)
    */
    public static async draw(
        gl: WebGL2RenderingContext,
        volume: Volume,
        camera: Camera,
        lights: Light | Iterable<Light | null> | null,
        material: SimpleMaterial | null,
        quality: RendererConfiguration | null,
        localTransform: Matrix4x4d | null,
    ): Promise<boolean> {
        const solid: PolyhedralBoundedSolid | null = WebGLVolumeRenderer.exportedSolid(volume);
        if (solid === null) {
            return false;
        }
        await WebGLPolyhedralBoundedSolidRenderer.draw(gl, solid, camera, lights, material, quality, localTransform);
        return true;
    }

    /**
    @return the boundary representation of the volume (exported once, and
    kept while the volume is shown), or null if it has none
    */
    public static exportedSolid(volume: Volume): PolyhedralBoundedSolid | null {
        if (volume instanceof PolyhedralBoundedSolid) {
            return volume;
        }
        if (WebGLVolumeRenderer.EXPORTED_SOLIDS.has(volume)) {
            return WebGLVolumeRenderer.EXPORTED_SOLIDS.get(volume)!;
        }
        const solid: PolyhedralBoundedSolid | null =
            volume.exportToPolyhedralBoundedSolid() as PolyhedralBoundedSolid | null;
        if (WebGLVolumeRenderer.EXPORTED_SOLIDS.size >= WebGLVolumeRenderer.MAX_CACHED_SOLIDS) {
            WebGLVolumeRenderer.EXPORTED_SOLIDS.clear();
        }
        // A null export is kept too, so it is not tried again on each frame
        WebGLVolumeRenderer.EXPORTED_SOLIDS.set(volume, solid);
        return solid;
    }

    /**
    Forgets the exported boundary representations.
    */
    public static dispose(): void {
        WebGLVolumeRenderer.EXPORTED_SOLIDS.clear();
    }
}
