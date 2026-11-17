import {
    BezierPatchTessellator,
    identityHashCode,
    type SubdividedBezierPatch,
    type TriangleMesh,
    type Camera,
    type Light,
    type Matrix4x4d,
    type RendererConfiguration,
    type RGBImageUncompressed,
    type SimpleMaterial,
} from "@vitral/base";
import { WebGLGeometryRenderer } from "../WebGLGeometryRenderer.js";
import type { WebGLMesh } from "../WebGLMeshRenderer.js";
import { WebGLTriangleMeshRenderer } from "./WebGLTriangleMeshRenderer.js";

/**
Port of `vsdk.toolkit.render.jogl.surface.Jogl4SubdividedBezierPatchRenderer`.

Renders a `SubdividedBezierPatch` with the WebGL pipeline: its surface is
tessellated by `BezierPatchTessellator` into a grid of `TESSELLATION_STEPS`
x `TESSELLATION_STEPS` steps of its parameters, visible from both sides, and
the mesh is cached by `WebGLGeometryRenderer`, identified by the patch.

Java's overloads for a single light and for a list of lights are one method
here, taken by the kind of the `lights` argument (see
`WebGLMeshRenderer.draw`); drawing is asynchronous because a GLSL source
arrives over `fetch`.
*/
export class WebGLSubdividedBezierPatchRenderer {
    /// Parameter steps of the tessellated patch along u and v
    public static readonly TESSELLATION_STEPS: number = 24;

    private constructor() {}

    /**
    Draws the patch, with the passes selected by the configuration (see
    `WebGLMeshRenderer`).
    */
    public static async draw(
        gl: WebGL2RenderingContext,
        patch: SubdividedBezierPatch,
        camera: Camera | null,
        lights: Light | Iterable<Light | null> | null,
        material: SimpleMaterial | null,
        quality: RendererConfiguration | null,
        textureMap: RGBImageUncompressed | null,
        normalMap: RGBImageUncompressed | null,
        localTransform: Matrix4x4d | null,
    ): Promise<void> {
        await WebGLGeometryRenderer.draw(
            gl,
            patch,
            camera,
            lights,
            material,
            quality,
            textureMap,
            normalMap,
            localTransform,
        );
    }

    /**
    @param patch a patch
    @return a string that identifies the mesh of the patch
    */
    public static meshKey(patch: SubdividedBezierPatch): string {
        return (
            "subdividedbezierpatch/" +
            identityHashCode(patch) +
            "/" +
            WebGLSubdividedBezierPatchRenderer.TESSELLATION_STEPS
        );
    }

    /**
    @param patch patch to tessellate
    @return the mesh of the patch, or null if it can not be tessellated
    */
    public static buildMesh(patch: SubdividedBezierPatch): WebGLMesh | null {
        const mesh: TriangleMesh | null = BezierPatchTessellator.tessellate(
            patch,
            WebGLSubdividedBezierPatchRenderer.TESSELLATION_STEPS,
            WebGLSubdividedBezierPatchRenderer.TESSELLATION_STEPS,
        );
        if (mesh === null) {
            return null;
        }
        return WebGLTriangleMeshRenderer.buildMesh(mesh, true);
    }
}
