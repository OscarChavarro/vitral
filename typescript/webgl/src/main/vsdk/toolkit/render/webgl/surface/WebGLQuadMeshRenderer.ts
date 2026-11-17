import {
    Vector3Dd,
    identityHashCode,
    type QuadMesh,
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
Port of `vsdk.toolkit.render.jogl.surface.Jogl4QuadMeshRenderer`.

Renders a `QuadMesh` with the WebGL pipeline: its quads are copied by
`buildMesh` (as two triangles each), visible from both sides, with the
normals of its vertices or, when it has none, the normal of each quad. The
mesh is cached by `WebGLGeometryRenderer`, identified by the quad mesh, as
they are not edited while shown.

Java's overloads for a single light and for a list of lights are one method
here, taken by the kind of the `lights` argument (see
`WebGLMeshRenderer.draw`); drawing is asynchronous because a GLSL source
arrives over `fetch`. `System.identityHashCode` comes from the `identityHashCode`
emulation of the base module.
*/
export class WebGLQuadMeshRenderer {
    private constructor() {}

    /**
    Draws the mesh, with the passes selected by the configuration (see
    `WebGLMeshRenderer`).
    */
    public static async draw(
        gl: WebGL2RenderingContext,
        mesh: QuadMesh,
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
            mesh,
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
    @param mesh a quad mesh
    @return a string that identifies the mesh
    */
    public static meshKey(mesh: QuadMesh): string {
        return "quadmesh/" + identityHashCode(mesh);
    }

    /**
    @param mesh quad mesh to copy
    @return its mesh, or null if it has no vertices or quads
    */
    public static buildMesh(mesh: QuadMesh): WebGLMesh | null {
        const p: Float64Array | null = mesh.getVertexPositions();
        const n: Float64Array | null = mesh.getVertexNormals();
        const uv: Float64Array | null = mesh.getVertexUvs();
        const idx: Int32Array | null = mesh.getQuadIndices();
        const numVertices: number = p !== null ? Math.trunc(p.length / 3) : 0;

        if (p === null || idx === null || mesh.getNumQuads() <= 0) {
            return null;
        }
        const builder: WebGLMeshBuilder = new WebGLMeshBuilder(WebGLMeshBuilder.characteristicSize(mesh.getMinMax()));
        builder.setDoubleSided(true);
        for (let q: number = 0; q < mesh.getNumQuads() && 4 * q + 3 < idx.length; q++) {
            const pos: (Vector3Dd | null)[] = [null, null, null, null];
            const nor: (Vector3Dd | null)[] = [null, null, null, null];
            const u: number[] = [0, 0, 0, 0];
            const v: number[] = [0, 0, 0, 0];
            let valid: boolean = true;

            for (let k: number = 0; k < 4; k++) {
                const i: number = idx[4 * q + k]!;
                if (i < 0 || i >= numVertices) {
                    valid = false;
                    break;
                }
                pos[k] = new Vector3Dd(p[3 * i]!, p[3 * i + 1]!, p[3 * i + 2]!);
                nor[k] =
                    n !== null && n.length >= 3 * i + 3 ? new Vector3Dd(n[3 * i]!, n[3 * i + 1]!, n[3 * i + 2]!) : null;
                if (uv !== null && uv.length >= 2 * i + 2) {
                    u[k] = uv[2 * i]!;
                    v[k] = uv[2 * i + 1]!;
                }
            }
            if (!valid) {
                continue;
            }
            // Normal of the quad: cross product of its diagonals
            let face: Vector3Dd = pos[2]!.subtract(pos[0]!).crossProduct(pos[3]!.subtract(pos[1]!));
            if (!(face.length() > 0)) {
                face = new Vector3Dd(0, 0, 1);
            }
            for (let k: number = 0; k < 4; k++) {
                if (nor[k] === null || !(nor[k]!.length() > 0)) {
                    nor[k] = face;
                }
            }
            builder.addQuad(
                pos[0]!,
                nor[0]!,
                u[0]!,
                v[0]!,
                pos[1]!,
                nor[1]!,
                u[1]!,
                v[1]!,
                pos[2]!,
                nor[2]!,
                u[2]!,
                v[2]!,
                pos[3]!,
                nor[3]!,
                u[3]!,
                v[3]!,
            );
        }
        return builder.build();
    }
}
