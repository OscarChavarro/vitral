import {
    type RendererConfiguration,
    type TriangleMesh,
    type TriangleMeshGroup,
    type Camera,
    type Light,
    type Matrix4x4d,
    type RGBImageUncompressed,
    type SimpleMaterial,
} from "@vitral/base";
import { WebGLGeometryRenderer } from "../WebGLGeometryRenderer.js";
import { WebGLMinMaxRenderer } from "../WebGLMinMaxRenderer.js";
import { WebGLSelectionCornersRenderer } from "../WebGLSelectionCornersRenderer.js";

/**
Port of `vsdk.toolkit.render.jogl.surface.Jogl4TriangleMeshGroupRenderer`.

Renders a `TriangleMeshGroup` with the WebGL pipeline. OBJ imports are
represented as a group of meshes (normally one per material). Raytracing
already traverses this geometry, so rasterizing every member here (each one
with `WebGLTriangleMeshRenderer`) keeps the two rendering paths equivalent.
The bounding volume and the selection corners are drawn once, around the
whole group.

Java's overloads for a single light and for a list of lights are one method
here, taken by the kind of the `lights` argument (see
`WebGLMeshRenderer.draw`); drawing is asynchronous because a GLSL source
arrives over `fetch`. Java's `quality.clone()` can not fail here, so its
`CloneNotSupportedException` branch has no counterpart.
*/
export class WebGLTriangleMeshGroupRenderer {
    private constructor() {}

    /**
    Draws the meshes of the group, with the passes selected by the
    configuration (see `WebGLMeshRenderer`).
    */
    public static async draw(
        gl: WebGL2RenderingContext,
        group: TriangleMeshGroup,
        camera: Camera | null,
        lights: Light | Iterable<Light | null> | null,
        material: SimpleMaterial | null,
        quality: RendererConfiguration | null,
        textureMap: RGBImageUncompressed | null,
        normalMap: RGBImageUncompressed | null,
        localTransform: Matrix4x4d | null,
    ): Promise<void> {
        if (quality === null) {
            return;
        }
        const meshQuality: RendererConfiguration = quality.clone() as RendererConfiguration;

        meshQuality.setBoundingVolume(false);
        meshQuality.setSelectionCorners(false);
        for (const mesh of group.getMeshes() as Iterable<TriangleMesh>) {
            await WebGLGeometryRenderer.draw(
                gl,
                mesh,
                camera,
                lights,
                material,
                meshQuality,
                textureMap,
                normalMap,
                localTransform,
            );
        }
        if (quality.isBoundingVolumeSet()) {
            await WebGLMinMaxRenderer.draw(gl, group, camera, localTransform ?? undefined);
        }
        if (quality.isSelectionCornersSet()) {
            await WebGLSelectionCornersRenderer.draw(gl, group, camera, localTransform);
        }
    }
}
