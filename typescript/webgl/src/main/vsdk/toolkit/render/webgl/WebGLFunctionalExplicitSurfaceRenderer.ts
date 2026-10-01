import {
    identityHashCode,
    Vector3Dd,
    type Camera,
    type FunctionalExplicitSurface,
    type Light,
    type Matrix4x4d,
    type RendererConfiguration,
    type RGBImageUncompressed,
    type SimpleMaterial,
    type TriangleMesh,
} from "@vitral/base";
import { WebGLGeometryRenderer } from "./WebGLGeometryRenderer.js";
import { WebGLMeshBuilder } from "./WebGLMeshBuilder.js";
import type { WebGLMesh } from "./WebGLMeshRenderer.js";

/**
Port of `vsdk.toolkit.render.jogl.Jogl4FunctionalExplicitSurfaceRenderer`.

Renders a `FunctionalExplicitSurface` (a height field z = f(x, y)) with the
WebGL pipeline. As in `Jogl2FunctionalExplicitSurfaceRenderer`, the surface is
drawn from its internal triangle mesh, which the surface rebuilds each time its
function, bounds or tessellation change. The mesh is visible from both sides,
and its texture coordinates span the (x, y) bounds of the surface.
*/
export class WebGLFunctionalExplicitSurfaceRenderer {
    private constructor() {}

    /**
    Draws the surface, with the passes selected by the configuration (see
    `WebGLMeshRenderer`).

    @param gl WebGL context
    @param surface surface to draw
    @param camera camera that views the surface
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the surface
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from surface space to world space
    */
    public static async draw(
        gl: WebGL2RenderingContext,
        surface: FunctionalExplicitSurface,
        camera: Camera,
        lights: Light | Iterable<Light | null> | null,
        material: SimpleMaterial,
        quality: RendererConfiguration,
        textureMap: RGBImageUncompressed | null,
        normalMap: RGBImageUncompressed | null,
        localTransform: Matrix4x4d | null,
    ): Promise<void> {
        await WebGLGeometryRenderer.draw(gl, surface, camera, lights, material, quality,
            textureMap, normalMap, localTransform);
    }

    /**
    @param surface a surface
    @return a string that identifies its current tessellation: the internal
    mesh is replaced by the surface whenever it changes
    */
    public static meshKey(surface: FunctionalExplicitSurface): string {
        return "functionalexplicitsurface/" + identityHashCode(surface) + "/" +
            identityHashCode(surface.getInternalTriangleMesh());
    }

    /**
    @param surface surface to tessellate
    @return the mesh of the surface, or null if it has no valid tessellation
    (i.e. its function could not be evaluated)
    */
    public static buildMesh(surface: FunctionalExplicitSurface): WebGLMesh | null {
        const mesh: TriangleMesh | null = surface.getInternalTriangleMesh();

        if (mesh === null) {
            return null;
        }
        const p: Float64Array | null = mesh.getVertexPositions();
        const n: Float64Array | null = mesh.getVertexNormals();
        const idx: Int32Array | null = mesh.getTriangleIndexes();

        if (p === null || idx === null || idx.length < 3) {
            return null;
        }
        const sizeX: number = surface.getMaxXBound() - surface.getMinXBound();
        const sizeY: number = surface.getMaxYBound() - surface.getMinYBound();
        const sizeZ: number = surface.getMaxZBound() - surface.getMinZBound();
        const builder: WebGLMeshBuilder = new WebGLMeshBuilder(
            Math.max(Math.abs(sizeX), Math.max(Math.abs(sizeY), Math.abs(sizeZ))));
        const pos: Vector3Dd[] = new Array<Vector3Dd>(3);
        const nor: (Vector3Dd | null)[] = new Array<Vector3Dd | null>(3);
        const u: number[] = [0, 0, 0];
        const v: number[] = [0, 0, 0];

        builder.setDoubleSided(true);
        for (let t: number = 0; t + 2 < idx.length; t += 3) {
            for (let k: number = 0; k < 3; k++) {
                const i: number = idx[t + k]!;

                pos[k] = new Vector3Dd(p[3 * i]!, p[3 * i + 1]!, p[3 * i + 2]!);
                nor[k] = (n !== null && n.length >= 3 * i + 3)
                    ? new Vector3Dd(n[3 * i]!, n[3 * i + 1]!, n[3 * i + 2]!)
                    : null;
                u[k] = sizeX !== 0 ? (pos[k]!.x() - surface.getMinXBound()) / sizeX : 0;
                v[k] = sizeY !== 0 ? (pos[k]!.y() - surface.getMinYBound()) / sizeY : 0;
            }
            const face: Vector3Dd = pos[1]!.subtract(pos[0]!).crossProduct(pos[2]!.subtract(pos[0]!));

            if (face.length() < 1e-15) {
                continue;
            }
            for (let k: number = 0; k < 3; k++) {
                const normal: Vector3Dd | null = nor[k]!;
                if (normal === null || !(normal.length() > 1e-12)) {
                    nor[k] = face;
                }
            }
            const n0: Vector3Dd = nor[0]!;
            const n1: Vector3Dd = nor[1]!;
            const n2: Vector3Dd = nor[2]!;
            // The front side is the one the normals point to
            if (n0.add(n1).add(n2).dotProduct(face) >= 0) {
                builder.addTriangle(pos[0]!, n0, u[0]!, v[0]!, pos[1]!, n1, u[1]!, v[1]!,
                    pos[2]!, n2, u[2]!, v[2]!);
            }
            else {
                builder.addTriangle(pos[2]!, n2, u[2]!, v[2]!, pos[1]!, n1, u[1]!, v[1]!,
                    pos[0]!, n0, u[0]!, v[0]!);
            }
        }
        return builder.build();
    }
}
