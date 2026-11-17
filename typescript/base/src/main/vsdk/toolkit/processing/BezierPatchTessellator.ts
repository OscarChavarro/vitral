import { Double } from "../../../java/lang/Double.js";
import { ArrayList } from "../../../java/util/ArrayList.js";
import { Vector3Dd } from "../common/linealAlgebra/Vector3Dd.js";
import type { SubdividedBezierPatch } from "../environment/geometry/surface/SubdividedBezierPatch.js";
import { QuadMesh } from "../environment/geometry/surface/QuadMesh.js";
import { TriangleMesh } from "../environment/geometry/surface/TriangleMesh.js";

// Drawable triangle mesh of the Bezier surface of a patch's control net:
// (uSteps + 1) x (vSteps + 1) points of the exact surface at uniform (u, v),
// each with the unit normal S_u x S_v of the Bernstein derivatives (or, where
// that vanishes, the mean of its triangles' normals) and (u, v) as texture
// coordinates, joined by two triangles per parameter cell facing the u x v
// side. It approximates the Bezier surface, not the patch's own subdivision,
// which a ray tracer intersects instead: with the patch's uniform steps both
// share the grid points, while the adaptive subdivisions place their triangles
// elsewhere.
export class BezierPatchTessellator {
    // Cubic Bernstein polynomial k at t and its derivative.
    private static bernstein(k: number, t: number): number {
        const s = 1.0 - t;
        if (k === 0) return s * s * s;
        if (k === 1) return 3.0 * t * s * s;
        if (k === 2) return 3.0 * t * t * s;
        return t * t * t;
    }

    private static bernsteinDerivative(k: number, t: number): number {
        const s = 1.0 - t;
        if (k === 0) return -3.0 * s * s;
        if (k === 1) return 3.0 * s * s - 6.0 * t * s;
        if (k === 2) return 6.0 * t * s - 3.0 * t * t;
        return 3.0 * t * t;
    }

    // Unit S_u x S_v at (u, v), or the zero vector where it vanishes.
    public static surfaceNormal(patch: SubdividedBezierPatch, u: number, v: number): Vector3Dd {
        let su = new Vector3Dd(0, 0, 0);
        let sv = new Vector3Dd(0, 0, 0);
        for (let i = 0; i < 4; i++) {
            for (let j = 0; j < 4; j++) {
                const p = patch.getControlPoint(i, j);
                su = su.add(
                    p.multiply(
                        BezierPatchTessellator.bernsteinDerivative(i, u) * BezierPatchTessellator.bernstein(j, v),
                    ),
                );
                sv = sv.add(
                    p.multiply(
                        BezierPatchTessellator.bernstein(i, u) * BezierPatchTessellator.bernsteinDerivative(j, v),
                    ),
                );
            }
        }
        const n = su.crossProduct(sv);
        const size = n.length();
        if (!(size > 1e-300) || !Double.isFinite(size)) return new Vector3Dd(0, 0, 0);
        return n.multiply(1.0 / size);
    }

    // Largest accepted steps along u or v.
    public static readonly MAXIMUM_STEPS = 1024;

    // Returns a new mesh, or null when a step count is outside
    // 1..MAXIMUM_STEPS.
    public static tessellate(patch: SubdividedBezierPatch, uSteps: number, vSteps: number): TriangleMesh | null {
        if (
            uSteps < 1 ||
            vSteps < 1 ||
            uSteps > BezierPatchTessellator.MAXIMUM_STEPS ||
            vSteps > BezierPatchTessellator.MAXIMUM_STEPS
        )
            return null;
        const columns = vSteps + 1;
        const vertexCount = (uSteps + 1) * columns;
        const triangleCount = 2 * uSteps * vSteps;
        const mesh = new TriangleMesh();
        mesh.initVertexPositionsArray(vertexCount);
        mesh.initVertexNormalsArray();
        mesh.initVertexUvsArray();
        const positions = mesh.getVertexPositions()!;
        const normals = mesh.getVertexNormals()!;
        const uvs = mesh.getVertexUvs()!;
        for (let i = 0; i <= uSteps; i++) {
            const u = i / uSteps;
            for (let j = 0; j <= vSteps; j++) {
                const v = j / vSteps;
                const index = i * columns + j;
                const p = patch.evaluate(u, v);
                const n = BezierPatchTessellator.surfaceNormal(patch, u, v);
                positions[index * 3] = p.x();
                positions[index * 3 + 1] = p.y();
                positions[index * 3 + 2] = p.z();
                normals[index * 3] = n.x();
                normals[index * 3 + 1] = n.y();
                normals[index * 3 + 2] = n.z();
                uvs[index * 2] = u;
                uvs[index * 2 + 1] = v;
            }
        }
        mesh.initTriangleArrays(triangleCount);
        const indices = mesh.getTriangleIndexes()!;
        const faceNormals = mesh.getTriangleNormals()!;
        const faceSums = new ArrayList<Vector3Dd>();
        for (let index = 0; index < vertexCount; index++) faceSums.add(new Vector3Dd(0, 0, 0));
        let t = 0;
        for (let i = 0; i < uSteps; i++) {
            for (let j = 0; j < vSteps; j++) {
                const a = i * columns + j;
                const b = (i + 1) * columns + j;
                const c = (i + 1) * columns + j + 1;
                const d = i * columns + j + 1;
                // (a, b, c) runs along +u then +v: its normal is u x v.
                const corners = [
                    [a, b, c],
                    [a, c, d],
                ];
                for (let h = 0; h < 2; h++, t++) {
                    const p: Vector3Dd[] = [new Vector3Dd(), new Vector3Dd(), new Vector3Dd()];
                    for (let k = 0; k < 3; k++) {
                        const v = corners[h]![k]!;
                        indices[t * 3 + k] = v;
                        p[k] = new Vector3Dd(positions[v * 3]!, positions[v * 3 + 1]!, positions[v * 3 + 2]!);
                    }
                    let n = p[1]!.subtract(p[0]!).crossProduct(p[2]!.subtract(p[0]!));
                    const size = n.length();
                    if (size > 0) n = n.multiply(1.0 / size);
                    faceNormals[t * 3] = n.x();
                    faceNormals[t * 3 + 1] = n.y();
                    faceNormals[t * 3 + 2] = n.z();
                    for (let k = 0; k < 3; k++) faceSums.set(corners[h]![k]!, faceSums.get(corners[h]![k]!).add(n));
                }
            }
        }
        // Where S_u x S_v vanishes (a collapsed edge), use the triangles' normals.
        for (let index = 0; index < vertexCount; index++) {
            if (normals[index * 3] !== 0 || normals[index * 3 + 1] !== 0 || normals[index * 3 + 2] !== 0) continue;
            const size = faceSums.get(index).length();
            if (!(size > 0)) continue;
            const n = faceSums.get(index).multiply(1.0 / size);
            normals[index * 3] = n.x();
            normals[index * 3 + 1] = n.y();
            normals[index * 3 + 2] = n.z();
        }
        return mesh;
    }

    /**
    Same grid of (uSteps + 1) x (vSteps + 1) points of the Bezier surface as
    `tessellate`, joined by one quadrilateral per parameter cell (without the
    diagonals of the triangles), with positions only: the lines of its edges
    are the curves of constant u and v of the surface.
    @return a new mesh, or null when a step count is outside
    1..MAXIMUM_STEPS
    */
    public static tessellateQuads(patch: SubdividedBezierPatch, uSteps: number, vSteps: number): QuadMesh | null {
        if (
            uSteps < 1 ||
            vSteps < 1 ||
            uSteps > BezierPatchTessellator.MAXIMUM_STEPS ||
            vSteps > BezierPatchTessellator.MAXIMUM_STEPS
        )
            return null;
        const columns = vSteps + 1;
        const mesh = new QuadMesh();
        mesh.initVertexPositionsArray((uSteps + 1) * columns);
        const positions = mesh.getVertexPositions()!;
        for (let i = 0; i <= uSteps; i++) {
            const u = i / uSteps;
            for (let j = 0; j <= vSteps; j++) {
                const index = i * columns + j;
                const p = patch.evaluate(u, j / vSteps);
                positions[index * 3] = p.x();
                positions[index * 3 + 1] = p.y();
                positions[index * 3 + 2] = p.z();
            }
        }
        mesh.initQuadArrays(uSteps * vSteps);
        let q = 0;
        for (let i = 0; i < uSteps; i++) {
            for (let j = 0; j < vSteps; j++, q++) {
                mesh.setQuadAt(
                    q,
                    i * columns + j,
                    (i + 1) * columns + j,
                    (i + 1) * columns + j + 1,
                    i * columns + j + 1,
                );
            }
        }
        return mesh;
    }
}
