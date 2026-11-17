import {
    Vector3Dd,
    identityHashCode,
    type TriangleMesh,
    type TriangleStripMesh,
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
Port of `vsdk.toolkit.render.jogl.surface.Jogl4TriangleStripMeshRenderer`.

Renders a `TriangleStripMesh` with the WebGL pipeline: the triangles of its
strips (see `TriangleStripMesh.exportToTriangleMesh`) are copied by
`buildMesh`, visible from both sides, with the normals of its vertexes or,
when they have none, the normal of each triangle. The mesh is cached by
`WebGLGeometryRenderer`, identified by the strip mesh, as they are not edited
while shown.

Java's overloads for a single light and for a list of lights are one method
here, taken by the kind of the `lights` argument (see
`WebGLMeshRenderer.draw`); drawing is asynchronous because a GLSL source
arrives over `fetch`. `System.identityHashCode` comes from the `identityHashCode`
emulation of the base module.
*/
export class WebGLTriangleStripMeshRenderer {
    private constructor() {}

    /**
    Draws the strip mesh, with the passes selected by the configuration (see
    `WebGLMeshRenderer`).
    */
    public static async draw(
        gl: WebGL2RenderingContext,
        strips: TriangleStripMesh,
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
            strips,
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
    @param strips a strip mesh
    @return a string that identifies the mesh
    */
    public static meshKey(strips: TriangleStripMesh): string {
        return "trianglestripmesh/" + identityHashCode(strips);
    }

    /**
    @param strips strip mesh to copy
    @return its mesh, or null if it has no triangles
    */
    public static buildMesh(strips: TriangleStripMesh): WebGLMesh | null {
        const mesh: TriangleMesh | null = strips.exportToTriangleMesh();
        if (mesh === null || mesh.getNumTriangles() <= 0) {
            return null;
        }
        const p: Float64Array = mesh.getVertexPositions()!;
        const idx: Int32Array = mesh.getTriangleIndexes()!;
        const builder: WebGLMeshBuilder = new WebGLMeshBuilder(WebGLMeshBuilder.characteristicSize(strips.getMinMax()));

        builder.setDoubleSided(true);
        for (let t: number = 0; t + 2 < idx.length; t += 3) {
            const pos: Vector3Dd[] = [];
            const nor: (Vector3Dd | null)[] = [null, null, null];

            for (let k: number = 0; k < 3; k++) {
                const i: number = idx[t + k]!;
                pos[k] = new Vector3Dd(p[3 * i]!, p[3 * i + 1]!, p[3 * i + 2]!);
                nor[k] = strips.getVertexAt(i).getNormal();
            }
            const face: Vector3Dd = pos[1]!.subtract(pos[0]!).crossProduct(pos[2]!.subtract(pos[0]!));
            if (!(face.length() > 0)) {
                continue;
            }
            for (let k: number = 0; k < 3; k++) {
                if (nor[k] === null || !(nor[k]!.length() > 0)) {
                    nor[k] = face;
                }
            }
            builder.addTriangle(pos[0]!, nor[0]!, 0, 0, pos[1]!, nor[1]!, 0, 0, pos[2]!, nor[2]!, 0, 0);
        }
        return builder.build();
    }
}
