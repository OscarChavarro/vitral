import {
    Vector3Dd,
    type Torus,
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
Port of `vsdk.toolkit.render.jogl.volume.Jogl4TorusRenderer`.

Renders a `Torus` with the WebGL pipeline: it is tessellated by `buildMesh`
into a grid of quads along its two angles, and the mesh is cached by
`WebGLGeometryRenderer`, identified by the radii of the torus.

Java's overloads for a single light and for a list of lights are one method
here, taken by the kind of the `lights` argument (see
`WebGLMeshRenderer.draw`); drawing is asynchronous because a GLSL source
arrives over `fetch`.
*/
export class WebGLTorusRenderer {
    /// Divisions along the major circle
    public static readonly MAJOR_SEGMENTS: number = 48;
    /// Divisions along the minor circle
    public static readonly MINOR_SEGMENTS: number = 24;

    private constructor() {}

    /**
    Draws the torus, with the passes selected by the configuration (see
    `WebGLMeshRenderer`).
    */
    public static async draw(
        gl: WebGL2RenderingContext,
        torus: Torus,
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
            torus,
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
    @param torus a torus
    @return a string that identifies the mesh of the torus
    */
    public static meshKey(torus: Torus): string {
        return "torus/" + torus.getMajorRadius() + "/" + torus.getMinorRadius();
    }

    /**
    @param torus torus to tessellate
    @return the mesh of the surface of the torus
    */
    public static buildMesh(torus: Torus): WebGLMesh {
        const bigR: number = torus.getMajorRadius();
        const smallR: number = torus.getMinorRadius();
        const builder: WebGLMeshBuilder = new WebGLMeshBuilder(bigR + smallR);
        const nw: number = WebGLTorusRenderer.MAJOR_SEGMENTS;
        const nv: number = WebGLTorusRenderer.MINOR_SEGMENTS;

        for (let i: number = 0; i < nw; i++) {
            const w0: number = (2 * Math.PI * i) / nw;
            const w1: number = (2 * Math.PI * (i + 1)) / nw;

            for (let j: number = 0; j < nv; j++) {
                const v0: number = (2 * Math.PI * j) / nv;
                const v1: number = (2 * Math.PI * (j + 1)) / nv;
                const p00: Vector3Dd = WebGLTorusRenderer.point(bigR, smallR, w0, v0);
                const p01: Vector3Dd = WebGLTorusRenderer.point(bigR, smallR, w0, v1);
                const p11: Vector3Dd = WebGLTorusRenderer.point(bigR, smallR, w1, v1);
                const p10: Vector3Dd = WebGLTorusRenderer.point(bigR, smallR, w1, v0);
                const n00: Vector3Dd = WebGLTorusRenderer.normal(w0, v0);
                const n01: Vector3Dd = WebGLTorusRenderer.normal(w0, v1);
                const n11: Vector3Dd = WebGLTorusRenderer.normal(w1, v1);
                const n10: Vector3Dd = WebGLTorusRenderer.normal(w1, v0);

                builder.addQuad(
                    p00,
                    n00,
                    i / nw,
                    j / nv,
                    p10,
                    n10,
                    (i + 1) / nw,
                    j / nv,
                    p11,
                    n11,
                    (i + 1) / nw,
                    (j + 1) / nv,
                    p01,
                    n01,
                    i / nw,
                    (j + 1) / nv,
                );
            }
        }
        return builder.build();
    }

    private static point(bigR: number, smallR: number, w: number, v: number): Vector3Dd {
        return new Vector3Dd(
            (bigR + smallR * Math.cos(v)) * Math.cos(w),
            (bigR + smallR * Math.cos(v)) * Math.sin(w),
            smallR * Math.sin(v),
        );
    }

    private static normal(w: number, v: number): Vector3Dd {
        return new Vector3Dd(Math.cos(v) * Math.cos(w), Math.cos(v) * Math.sin(w), Math.sin(v));
    }
}
