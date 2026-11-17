import {
    InfinitePlane,
    type PlanarHalfSpace,
    type Camera,
    type Light,
    type Matrix4x4d,
    type RendererConfiguration,
    type RGBImageUncompressed,
    type SimpleMaterial,
} from "@vitral/base";
import { WebGLInfinitePlaneRenderer } from "../surface/WebGLInfinitePlaneRenderer.js";

/**
Port of `vsdk.toolkit.render.jogl.volume.Jogl4PlanarHalfSpaceRenderer`.

Renders a `PlanarHalfSpace` with the WebGL pipeline. The half space is
unbounded: it is shown by its boundary plane, as the square that
`WebGLInfinitePlaneRenderer` shows for it.

Java's overloads for a single light and for a list of lights are one method
here, taken by the kind of the `lights` argument (see
`WebGLMeshRenderer.draw`); drawing is asynchronous because a GLSL source
arrives over `fetch`.
*/
export class WebGLPlanarHalfSpaceRenderer {
    private constructor() {}

    /**
    Draws the boundary of the half space, with the passes selected by the
    configuration (see `WebGLMeshRenderer`).
    */
    public static async draw(
        gl: WebGL2RenderingContext,
        halfSpace: PlanarHalfSpace,
        camera: Camera | null,
        lights: Light | Iterable<Light | null> | null,
        material: SimpleMaterial | null,
        quality: RendererConfiguration | null,
        textureMap: RGBImageUncompressed | null,
        normalMap: RGBImageUncompressed | null,
        localTransform: Matrix4x4d | null,
    ): Promise<void> {
        await WebGLInfinitePlaneRenderer.draw(
            gl,
            WebGLPlanarHalfSpaceRenderer.boundary(halfSpace),
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
    @param halfSpace a half space
    @return a new plane with the equation of its boundary
    */
    public static boundary(halfSpace: PlanarHalfSpace): InfinitePlane {
        return new InfinitePlane(halfSpace.getA(), halfSpace.getB(), halfSpace.getC(), halfSpace.getD());
    }
}
