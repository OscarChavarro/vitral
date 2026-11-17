import {
    Vector3Dd,
    type Tube,
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
Port of `vsdk.toolkit.render.jogl.volume.Jogl4TubeRenderer`.

Renders a `Tube` with the WebGL pipeline: it is tessellated by `buildMesh`
into its external wall, its internal wall (with normals towards the axis) and
its annular caps, and the mesh is cached by `WebGLGeometryRenderer`,
identified by the parameters of the tube. A washer (height 0) is a double
sided annulus.

Java's overloads for a single light and for a list of lights are one method
here, taken by the kind of the `lights` argument (see
`WebGLMeshRenderer.draw`); drawing is asynchronous because a GLSL source
arrives over `fetch`.
*/
export class WebGLTubeRenderer {
    /// Divisions around the axis
    public static readonly SLICES: number = 32;

    private constructor() {}

    /**
    Draws the tube, with the passes selected by the configuration (see
    `WebGLMeshRenderer`).
    */
    public static async draw(
        gl: WebGL2RenderingContext,
        tube: Tube,
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
            tube,
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
    @param tube a tube
    @return a string that identifies the mesh of the tube
    */
    public static meshKey(tube: Tube): string {
        return "tube/" + tube.getInternalRadius() + "/" + tube.getExternalRadius() + "/" + tube.getHeight();
    }

    /**
    @param tube tube to tessellate
    @return the mesh of the walls and the caps of the tube
    */
    public static buildMesh(tube: Tube): WebGLMesh {
        const ri: number = tube.getInternalRadius();
        const re: number = tube.getExternalRadius();
        const h: number = tube.getHeight();
        const builder: WebGLMeshBuilder = new WebGLMeshBuilder(Math.max(re, h));

        if (tube.isWasher()) {
            builder.addDisk(0, ri, re, true, WebGLTubeRenderer.SLICES);
            builder.setDoubleSided(true);
            return builder.build();
        }
        builder.addFrustum(0, re, h, re, WebGLTubeRenderer.SLICES);
        if (ri > 0.0) {
            WebGLTubeRenderer.addInternalWall(builder, ri, h);
        }
        builder.addDisk(0, ri, re, false, WebGLTubeRenderer.SLICES);
        builder.addDisk(h, ri, re, true, WebGLTubeRenderer.SLICES);
        return builder.build();
    }

    /**
    The internal wall faces the axis: its quads go clockwise seen from
    outside the hole, with normals pointing to the axis.
    */
    private static addInternalWall(builder: WebGLMeshBuilder, r: number, h: number): void {
        const slices: number = WebGLTubeRenderer.SLICES;

        for (let i: number = 0; i < slices; i++) {
            const a0: number = (2 * Math.PI * i) / slices;
            const a1: number = (2 * Math.PI * (i + 1)) / slices;
            const c0: number = Math.cos(a0);
            const s0: number = Math.sin(a0);
            const c1: number = Math.cos(a1);
            const s1: number = Math.sin(a1);
            const n0: Vector3Dd = new Vector3Dd(-c0, -s0, 0);
            const n1: Vector3Dd = new Vector3Dd(-c1, -s1, 0);
            const u0: number = i / slices;
            const u1: number = (i + 1) / slices;

            builder.addQuad(
                new Vector3Dd(r * c1, r * s1, 0),
                n1,
                u1,
                0,
                new Vector3Dd(r * c0, r * s0, 0),
                n0,
                u0,
                0,
                new Vector3Dd(r * c0, r * s0, h),
                n0,
                u0,
                1,
                new Vector3Dd(r * c1, r * s1, h),
                n1,
                u1,
                1,
            );
        }
    }
}
