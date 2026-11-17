import {
    Vector3Dd,
    type Box,
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
Port of `vsdk.toolkit.render.jogl.volume.Jogl4BoxRenderer`.

Renders a `Box` with the WebGL pipeline: it is tessellated by `buildMesh`
into its six faces, with the texture layout of `Jogl2BoxRenderer`, and the
mesh is cached by `WebGLGeometryRenderer`, identified by the size of the
box.

Java's overloads for a single light and for a list of lights are one method
here, taken by the kind of the `lights` argument (see
`WebGLMeshRenderer.draw`); drawing is asynchronous because a GLSL source
arrives over `fetch`.
*/
export class WebGLBoxRenderer {
    private constructor() {}

    /**
    Draws the box, with the passes selected by the configuration (see
    `WebGLMeshRenderer`).
    */
    public static async draw(
        gl: WebGL2RenderingContext,
        box: Box,
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
            box,
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
    @param box a box
    @return a string that identifies the mesh of the box
    */
    public static meshKey(box: Box): string {
        const s: Vector3Dd = box.getSize();
        return "box/" + s.x() + "/" + s.y() + "/" + s.z();
    }

    /**
    @param box box to tessellate
    @return the mesh of the faces of the box
    */
    public static buildMesh(box: Box): WebGLMesh {
        const size: Vector3Dd = box.getSize();
        const hx: number = size.x() / 2;
        const hy: number = size.y() / 2;
        const hz: number = size.z() / 2;
        const builder: WebGLMeshBuilder = new WebGLMeshBuilder(Math.max(hx, Math.max(hy, hz)) * 2);

        // Each face is given counterclockwise seen from outside
        WebGLBoxRenderer.addFace(
            builder,
            new Vector3Dd(0, 0, -1),
            new Vector3Dd(-hx, -hy, -hz),
            new Vector3Dd(-hx, hy, -hz),
            new Vector3Dd(hx, hy, -hz),
            new Vector3Dd(hx, -hy, -hz),
        );
        WebGLBoxRenderer.addFace(
            builder,
            new Vector3Dd(0, 0, 1),
            new Vector3Dd(-hx, -hy, hz),
            new Vector3Dd(hx, -hy, hz),
            new Vector3Dd(hx, hy, hz),
            new Vector3Dd(-hx, hy, hz),
        );
        WebGLBoxRenderer.addFace(
            builder,
            new Vector3Dd(0, -1, 0),
            new Vector3Dd(-hx, -hy, hz),
            new Vector3Dd(-hx, -hy, -hz),
            new Vector3Dd(hx, -hy, -hz),
            new Vector3Dd(hx, -hy, hz),
        );
        WebGLBoxRenderer.addFace(
            builder,
            new Vector3Dd(-1, 0, 0),
            new Vector3Dd(-hx, hy, hz),
            new Vector3Dd(-hx, hy, -hz),
            new Vector3Dd(-hx, -hy, -hz),
            new Vector3Dd(-hx, -hy, hz),
        );
        WebGLBoxRenderer.addFace(
            builder,
            new Vector3Dd(0, 1, 0),
            new Vector3Dd(hx, hy, hz),
            new Vector3Dd(hx, hy, -hz),
            new Vector3Dd(-hx, hy, -hz),
            new Vector3Dd(-hx, hy, hz),
        );
        WebGLBoxRenderer.addFace(
            builder,
            new Vector3Dd(1, 0, 0),
            new Vector3Dd(hx, -hy, hz),
            new Vector3Dd(hx, -hy, -hz),
            new Vector3Dd(hx, hy, -hz),
            new Vector3Dd(hx, hy, hz),
        );
        return builder.build();
    }

    /**
    Adds a face whose vertices are given in the order upper left, lower left,
    lower right, upper right as seen from outside (the Jogl2 texture layout),
    and which is wound so its front side looks outside.
    */
    private static addFace(
        builder: WebGLMeshBuilder,
        n: Vector3Dd,
        a: Vector3Dd,
        b: Vector3Dd,
        c: Vector3Dd,
        d: Vector3Dd,
    ): void {
        const winding: Vector3Dd = b.subtract(a).crossProduct(c.subtract(b));

        if (winding.dotProduct(n) >= 0) {
            builder.addQuad(a, n, 0, 1, b, n, 0, 0, c, n, 1, 0, d, n, 1, 1);
        } else {
            builder.addQuad(d, n, 1, 1, c, n, 1, 0, b, n, 0, 0, a, n, 0, 1);
        }
    }
}
