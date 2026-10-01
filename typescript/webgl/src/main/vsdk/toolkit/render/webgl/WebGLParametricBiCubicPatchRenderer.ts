import {
    identityHashCode,
    type Camera,
    type Light,
    type Matrix4x4d,
    type ParametricBiCubicPatch,
    type RendererConfiguration,
    type RGBImageUncompressed,
    type SimpleMaterial,
    type Vector3Dd,
} from "@vitral/base";
import { WebGLGeometryRenderer } from "./WebGLGeometryRenderer.js";
import { WebGLMeshBuilder } from "./WebGLMeshBuilder.js";
import type { WebGLMesh } from "./WebGLMeshRenderer.js";

/**
Port of `vsdk.toolkit.render.jogl.Jogl4ParametricBiCubicPatchRenderer`.

Renders a `ParametricBiCubicPatch` (Bezier, Hermite or Ferguson) with the
WebGL pipeline. As in `Jogl2ParametricBiCubicPatchRenderer`, the patch is
evaluated over a regular grid of (s, t) parameters with
`getApproximationSteps()` divisions in each direction, using the analytic
normals of the patch, and the texture space matches the parametric space. The
mesh is visible from both sides.
*/
export class WebGLParametricBiCubicPatchRenderer {
    private constructor() {}

    /**
    Draws the patch, with the passes selected by the configuration (see
    `WebGLMeshRenderer`).

    @param gl WebGL context
    @param patch patch to draw
    @param camera camera that views the patch
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the patch
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from patch space to world space
    */
    public static async draw(
        gl: WebGL2RenderingContext,
        patch: ParametricBiCubicPatch,
        camera: Camera,
        lights: Light | Iterable<Light | null> | null,
        material: SimpleMaterial,
        quality: RendererConfiguration,
        textureMap: RGBImageUncompressed | null,
        normalMap: RGBImageUncompressed | null,
        localTransform: Matrix4x4d | null,
    ): Promise<void> {
        await WebGLGeometryRenderer.draw(gl, patch, camera, lights, material, quality,
            textureMap, normalMap, localTransform);
    }

    /**
    @param patch a patch
    @return a string that identifies its tessellation: its geometry matrices
    and its number of approximation steps
    */
    public static meshKey(patch: ParametricBiCubicPatch): string {
        return "parametricbicubicpatch/" + identityHashCode(patch) + "/" +
            patch.getType() + "/" + patch.getApproximationSteps() + "/" +
            patch.geometryMatrixX.hashCode() + "/" + patch.geometryMatrixY.hashCode() + "/" +
            patch.geometryMatrixZ.hashCode();
    }

    /**
    @param patch patch to tessellate
    @return the mesh of the patch, or null if it can not be tessellated
    */
    public static buildMesh(patch: ParametricBiCubicPatch): WebGLMesh | null {
        const steps: number = Math.max(1, patch.getApproximationSteps());
        const n: number = steps + 1;
        const points: Vector3Dd[][] = [];
        const normals: (Vector3Dd | null)[][] = [];

        for (let i: number = 0; i < n; i++) {
            const s: number = i / steps;
            const pointRow: Vector3Dd[] = [];
            const normalRow: (Vector3Dd | null)[] = [];

            for (let j: number = 0; j < n; j++) {
                const t: number = j / steps;
                pointRow.push(patch.evaluate(s, t));
                normalRow.push(patch.evaluateNormal(s, t));
            }
            points.push(pointRow);
            normals.push(normalRow);
        }

        const minmax: ArrayLike<number> | null = patch.getMinMax();
        let size: number = 1.0;
        if (minmax !== null && minmax.length >= 6) {
            size = Math.max(Math.abs(minmax[3]! - minmax[0]!),
                Math.max(Math.abs(minmax[4]! - minmax[1]!), Math.abs(minmax[5]! - minmax[2]!)));
        }
        const builder: WebGLMeshBuilder = new WebGLMeshBuilder(size);

        builder.setDoubleSided(true);
        for (let i: number = 0; i < steps; i++) {
            for (let j: number = 0; j < steps; j++) {
                WebGLParametricBiCubicPatchRenderer.addCell(builder, points, normals, i, j, steps);
            }
        }
        return builder.build();
    }

    /**
    Adds the cell between the grid points (i, j) and (i + 1, j + 1). Normals
    that the patch can not give (degenerate points, i.e. a corner with null
    tangents) are replaced by the normal of the cell, and the cell is wound so
    its front side is the one its normals point to.
    */
    private static addCell(
        builder: WebGLMeshBuilder,
        points: Vector3Dd[][],
        normals: (Vector3Dd | null)[][],
        i: number,
        j: number,
        steps: number,
    ): void {
        const p00: Vector3Dd = points[i]![j]!;
        const p10: Vector3Dd = points[i + 1]![j]!;
        const p11: Vector3Dd = points[i + 1]![j + 1]!;
        const p01: Vector3Dd = points[i]![j + 1]!;
        const face: Vector3Dd = p11.subtract(p00).crossProduct(p01.subtract(p10));

        if (!(face.length() > 1e-15)) {
            return;
        }
        const n00: Vector3Dd = WebGLParametricBiCubicPatchRenderer.validNormal(normals[i]![j]!, face);
        const n10: Vector3Dd = WebGLParametricBiCubicPatchRenderer.validNormal(normals[i + 1]![j]!, face);
        const n11: Vector3Dd = WebGLParametricBiCubicPatchRenderer.validNormal(normals[i + 1]![j + 1]!, face);
        const n01: Vector3Dd = WebGLParametricBiCubicPatchRenderer.validNormal(normals[i]![j + 1]!, face);
        const u0: number = i / steps;
        const u1: number = (i + 1) / steps;
        const v0: number = j / steps;
        const v1: number = (j + 1) / steps;
        const average: Vector3Dd = n00.add(n10).add(n11).add(n01);

        if (average.dotProduct(face) >= 0) {
            builder.addQuad(p00, n00, u0, v0, p10, n10, u1, v0,
                p11, n11, u1, v1, p01, n01, u0, v1);
        }
        else {
            builder.addQuad(p01, n01, u0, v1, p11, n11, u1, v1,
                p10, n10, u1, v0, p00, n00, u0, v0);
        }
    }

    private static validNormal(normal: Vector3Dd | null, fallback: Vector3Dd): Vector3Dd {
        if (normal === null || !(normal.length() > 1e-12)) {
            return fallback.normalized();
        }
        return normal;
    }
}
