import {
    type QuadricSurface,
    type Vector3Dd,
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
Port of `vsdk.toolkit.render.jogl.volume.Jogl4QuadricSurfaceRenderer`.

Renders a `QuadricSurface` with the WebGL pipeline, from the
`SolidBoundaryTessellator` mesh of its bounds (see
`_WebGLSolidBoundaryMeshBuilder`), cached by `WebGLGeometryRenderer`,
identified by the coefficients of the quadric.

Java's overloads for a single light and for a list of lights are one method
here, taken by the kind of the `lights` argument (see
`WebGLMeshRenderer.draw`); drawing is asynchronous because a GLSL source
arrives over `fetch`.
*/
export class WebGLQuadricSurfaceRenderer {
    private constructor() {}

    /**
    Draws the quadric, with the passes selected by the configuration (see
    `WebGLMeshRenderer`).
    */
    public static async draw(
        gl: WebGL2RenderingContext,
        quadric: QuadricSurface,
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
            quadric,
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
    @param quadric a quadric
    @return a string that identifies the mesh of the quadric
    */
    public static meshKey(quadric: QuadricSurface): string {
        return (
            "quadric/" +
            WebGLQuadricSurfaceRenderer.keyOf(quadric.squareCoefficients()) +
            "/" +
            WebGLQuadricSurfaceRenderer.keyOf(quadric.mixedCoefficients()) +
            "/" +
            WebGLQuadricSurfaceRenderer.keyOf(quadric.linearCoefficients()) +
            "/" +
            quadric.constantCoefficient() +
            "/" +
            _WebGLSolidBoundaryMeshBuilder.RESOLUTION
        );
    }

    /**
    @param quadric quadric to tessellate
    @return the mesh of its boundary, or null if it can not be tessellated
    */
    public static buildMesh(quadric: QuadricSurface): WebGLMesh | null {
        return _WebGLSolidBoundaryMeshBuilder.buildMesh(quadric);
    }

    /** Exact text of the coordinates, as `Vector3Dd.toString` rounds them */
    private static keyOf(v: Vector3Dd): string {
        return v.x() + "," + v.y() + "," + v.z();
    }
}
