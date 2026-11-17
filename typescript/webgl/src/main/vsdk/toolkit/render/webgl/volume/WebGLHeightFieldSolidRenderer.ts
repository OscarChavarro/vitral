import {
    identityHashCode,
    type HeightFieldSolid,
    type Camera,
    type Light,
    type Matrix4x4d,
    type RendererConfiguration,
    type RGBImageUncompressed,
    type SimpleMaterial,
} from "@vitral/base";
import { WebGLGeometryRenderer } from "../WebGLGeometryRenderer.js";
import type { WebGLMesh } from "../WebGLMeshRenderer.js";
import { _WebGLSolidBoundaryMeshBuilder } from "./_WebGLSolidBoundaryMeshBuilder.js";

/**
Port of `vsdk.toolkit.render.jogl.volume.Jogl4HeightFieldSolidRenderer`.

Renders a `HeightFieldSolid` with the WebGL pipeline, from the
`SolidBoundaryTessellator` mesh of its bounds (see
`_WebGLSolidBoundaryMeshBuilder`), cached by `WebGLGeometryRenderer`,
identified by the height field, as they are not edited while shown.

Java's overloads for a single light and for a list of lights are one method
here, taken by the kind of the `lights` argument (see
`WebGLMeshRenderer.draw`); drawing is asynchronous because a GLSL source
arrives over `fetch`. `System.identityHashCode` comes from the `identityHashCode`
emulation of the base module.
*/
export class WebGLHeightFieldSolidRenderer {
    private constructor() {}

    /**
    Draws the solid, with the passes selected by the configuration (see
    `WebGLMeshRenderer`).
    */
    public static async draw(
        gl: WebGL2RenderingContext,
        solid: HeightFieldSolid,
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
            solid,
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
    @param solid a height field
    @return a string that identifies the mesh of the height field
    */
    public static meshKey(solid: HeightFieldSolid): string {
        return "tessellatedsolid/" + identityHashCode(solid) + "/" + _WebGLSolidBoundaryMeshBuilder.RESOLUTION;
    }

    /**
    @param solid solid to tessellate
    @return the mesh of its boundary, or null if it can not be tessellated
    */
    public static buildMesh(solid: HeightFieldSolid): WebGLMesh | null {
        return _WebGLSolidBoundaryMeshBuilder.buildMesh(solid);
    }
}
