// Port of cpp/base/src/test/TessellationMeshCheck.{h,cpp}
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import type { TriangleMesh } from "vsdk/toolkit/environment/geometry/surface/TriangleMesh.js";

// Properties of tessellation results checked by the tests. The cpp
// `java::TreeMap<long int, int>` key sets are JavaScript sets here: only key
// membership and count are used.
export class TessellationMeshCheck {
    public static vertex(mesh: TriangleMesh, index: number): Vector3Dd {
        const p = mesh.getVertexPositionsView()!;
        return new Vector3Dd(p[index * 3]!, p[index * 3 + 1]!, p[index * 3 + 2]!);
    }

    // Every directed edge appears once and its reverse once: the mesh is
    // closed and its triangles are consistently wound.
    public static isClosedAndOriented(mesh: TriangleMesh): boolean {
        const t = mesh.getTriangleIndexesView() ?? new Int32Array(0);
        const n = Math.trunc((mesh.getVertexPositionsView()?.length ?? 0) / 3);
        const directed = new Set<number>();
        for (let i = 0; i < t.length; i += 3) {
            for (let k = 0; k < 3; k++) {
                const a = t[i + k]!;
                const b = t[i + ((k + 1) % 3)]!;
                if (a === b) return false;
                // A repeated directed edge is a fold or a third sheet on the edge.
                const key = a * n + b;
                if (directed.has(key)) return false;
                directed.add(key);
            }
        }
        for (const key of directed) {
            if (!directed.has((key % n) * n + Math.trunc(key / n))) return false;
        }
        return directed.size !== 0;
    }

    // Signed volume enclosed by the triangles (positive when they face out).
    public static enclosedVolume(mesh: TriangleMesh): number {
        const t = mesh.getTriangleIndexesView() ?? new Int32Array(0);
        let volume = 0;
        for (let i = 0; i < t.length; i += 3) {
            const a = TessellationMeshCheck.vertex(mesh, t[i]!);
            const b = TessellationMeshCheck.vertex(mesh, t[i + 1]!);
            const c = TessellationMeshCheck.vertex(mesh, t[i + 2]!);
            volume += a.dotProduct(b.crossProduct(c)) / 6.0;
        }
        return volume;
    }

    public static area(mesh: TriangleMesh): number {
        const t = mesh.getTriangleIndexesView() ?? new Int32Array(0);
        let total = 0;
        for (let i = 0; i < t.length; i += 3) {
            const a = TessellationMeshCheck.vertex(mesh, t[i]!);
            const b = TessellationMeshCheck.vertex(mesh, t[i + 1]!);
            const c = TessellationMeshCheck.vertex(mesh, t[i + 2]!);
            total += b.subtract(a).crossProduct(c.subtract(a)).length() / 2.0;
        }
        return total;
    }

    // Vertices minus edges plus triangles.
    public static eulerCharacteristic(mesh: TriangleMesh): number {
        const t = mesh.getTriangleIndexesView() ?? new Int32Array(0);
        const n = Math.trunc((mesh.getVertexPositionsView()?.length ?? 0) / 3);
        const edges = new Set<number>();
        for (let i = 0; i < t.length; i += 3) {
            for (let k = 0; k < 3; k++) {
                let a = t[i + k]!;
                let b = t[i + ((k + 1) % 3)]!;
                if (a > b) {
                    const s = a;
                    a = b;
                    b = s;
                }
                edges.add(a * n + b);
            }
        }
        return n - edges.size + Math.trunc(t.length / 3);
    }
}
