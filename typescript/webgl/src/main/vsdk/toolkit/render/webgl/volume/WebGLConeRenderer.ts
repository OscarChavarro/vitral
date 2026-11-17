import {
    type Cone,
    type Camera,
    type Light,
    type Matrix4x4d,
    type RendererConfiguration,
    type RGBImageUncompressed,
    type SimpleMaterial,
} from "@vitral/base";
import { WebGLGeometryRenderer } from "../WebGLGeometryRenderer.js";
import { WebGLMeshBuilder } from "../WebGLMeshBuilder.js";
import type { WebGLMesh } from "../WebGLMeshRenderer.js";

/**
Port of `vsdk.toolkit.render.jogl.volume.Jogl4ConeRenderer`.

Renders a `Cone` (also cylinders and truncated cones) with the WebGL
pipeline: it is tessellated by `buildMesh` into its side and its caps, and
the mesh is cached by `WebGLGeometryRenderer`, identified by the parameters
of the cone.

Java's overloads for a single light and for a list of lights are one method
here, taken by the kind of the `lights` argument (see
`WebGLMeshRenderer.draw`); drawing is asynchronous because a GLSL source
arrives over `fetch`.
*/
export class WebGLConeRenderer {
    /// Divisions around the axis
    public static readonly SLICES: number = 32;

    private constructor() {}

    /**
    Draws the cone, with the passes selected by the configuration (see
    `WebGLMeshRenderer`).
    */
    public static async draw(
        gl: WebGL2RenderingContext,
        cone: Cone,
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
            cone,
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
    @param cone a cone
    @return a string that identifies the mesh of the cone
    */
    public static meshKey(cone: Cone): string {
        return "cone/" + cone.getBottomRadius() + "/" + cone.getTopRadius() + "/" + cone.getHeight();
    }

    /**
    @param cone cone to tessellate
    @return the mesh of the side and the caps of the cone
    */
    public static buildMesh(cone: Cone): WebGLMesh {
        const r1: number = cone.getBottomRadius();
        const r2: number = cone.getTopRadius();
        const h: number = cone.getHeight();
        const builder: WebGLMeshBuilder = new WebGLMeshBuilder(Math.max(Math.max(r1, r2), h));

        builder.addFrustum(0, r1, h, r2, WebGLConeRenderer.SLICES);
        builder.addDisk(0, 0, r1, false, WebGLConeRenderer.SLICES);
        if (r2 > 0.0) {
            builder.addDisk(h, 0, r2, true, WebGLConeRenderer.SLICES);
        }
        return builder.build();
    }
}
