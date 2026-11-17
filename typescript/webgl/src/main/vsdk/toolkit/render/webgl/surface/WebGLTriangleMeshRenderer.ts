import {
    Vector3Dd,
    identityHashCode,
    type TriangleMesh,
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
Port of `vsdk.toolkit.render.jogl.surface.Jogl4TriangleMeshRenderer`.

Renders a `TriangleMesh` with the WebGL pipeline: its triangles are copied
by `buildMesh` with their normals and texture coordinates, and the mesh is
cached by `WebGLGeometryRenderer`, identified by the triangle mesh, as they
are not edited while shown. It is also the way to draw the triangle meshes
that other geometries are tessellated to.

Java's overloads for a single light and for a list of lights are one method
here, taken by the kind of the `lights` argument (see
`WebGLMeshRenderer.draw`); drawing is asynchronous because a GLSL source
arrives over `fetch`. `System.identityHashCode` comes from the `identityHashCode`
emulation of the base module.
*/
export class WebGLTriangleMeshRenderer {
    private constructor() {}

    /**
    Draws the mesh, with the passes selected by the configuration (see
    `WebGLMeshRenderer`).
    */
    public static async draw(
        gl: WebGL2RenderingContext,
        mesh: TriangleMesh,
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
    @param mesh a triangle mesh
    @return a string that identifies the mesh
    */
    public static meshKey(mesh: TriangleMesh): string {
        return "trianglemesh/" + identityHashCode(mesh);
    }

    /**
    Java overloads `buildMesh(TriangleMesh mesh)` (double sided as the mesh
    says) and `buildMesh(TriangleMesh mesh, boolean doubleSided)`.
    @param mesh triangle mesh to copy
    @param doubleSided true if the mesh is visible from both sides
    @return its mesh, or null if it has no vertices or triangles
    */
    public static buildMesh(mesh: TriangleMesh): WebGLMesh | null;
    public static buildMesh(mesh: TriangleMesh, doubleSided: boolean): WebGLMesh | null;
    public static buildMesh(mesh: TriangleMesh, doubleSided?: boolean): WebGLMesh | null {
        if (doubleSided === undefined) {
            return WebGLTriangleMeshRenderer.buildMesh(mesh, mesh.isDoubleSided());
        }
        if (mesh.getVertexPositions() === null || mesh.getTriangleIndexes() === null) {
            return null;
        }
        const builder: WebGLMeshBuilder = new WebGLMeshBuilder(WebGLMeshBuilder.characteristicSize(mesh.getMinMax()));
        builder.setDoubleSided(doubleSided);
        WebGLTriangleMeshRenderer.addTriangles(builder, mesh);
        return builder.build();
    }

    private static addTriangles(builder: WebGLMeshBuilder, mesh: TriangleMesh): void {
        const p: Float64Array = mesh.getVertexPositions()!;
        const n: Float64Array | null = mesh.getVertexNormals();
        const uv: Float64Array | null = mesh.getVertexUvs();
        const idx: Int32Array = mesh.getTriangleIndexes()!;

        for (let t: number = 0; t + 2 < idx.length; t += 3) {
            const pos: Vector3Dd[] = [];
            const nor: Vector3Dd[] = [];
            const u: number[] = [0, 0, 0];
            const v: number[] = [0, 0, 0];

            for (let k: number = 0; k < 3; k++) {
                const i: number = idx[t + k]!;
                pos[k] = new Vector3Dd(p[3 * i]!, p[3 * i + 1]!, p[3 * i + 2]!);
                nor[k] =
                    n !== null && n.length >= 3 * i + 3
                        ? new Vector3Dd(n[3 * i]!, n[3 * i + 1]!, n[3 * i + 2]!)
                        : new Vector3Dd(0, 0, 1);
                if (uv !== null && uv.length >= 2 * i + 2) {
                    u[k] = uv[2 * i]!;
                    v[k] = uv[2 * i + 1]!;
                }
            }
            builder.addTriangle(
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
            );
        }
    }
}
