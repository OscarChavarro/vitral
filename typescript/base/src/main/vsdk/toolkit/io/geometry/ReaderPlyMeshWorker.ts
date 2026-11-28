import type { WorkerExecutor } from "../../../../java/concurrent/WorkerProtocol.js";

export type ReaderPlyMeshRequest =
    | { readonly kind: "vertices"; readonly coordinates: number[][] }
    | { readonly kind: "triangles"; readonly indices: Int32Array; readonly vertexCount: number };

export type ReaderPlyMeshResult =
    | { readonly kind: "vertices"; readonly positions: Float64Array }
    | { readonly kind: "triangles"; readonly indices: Int32Array };

export type ReaderPlyWorkerFactory = () => WorkerExecutor<ReaderPlyMeshRequest, ReaderPlyMeshResult>;

/** The array data produced by the two mesh assembly jobs in Java's ReaderPly. */
export function assembleReaderPlyMeshPart(request: ReaderPlyMeshRequest): ReaderPlyMeshResult {
    if (request.kind === "vertices") {
        const positions = new Float64Array(request.coordinates.length * 3);
        for (let i = 0; i < request.coordinates.length; i++) {
            positions.set(request.coordinates[i]!, i * 3);
        }
        return { kind: "vertices", positions };
    }

    const accepted = new Int32Array(request.indices.length);
    let count = 0;
    for (let i = 0; i < request.indices.length; i += 3) {
        const a = request.indices[i]!;
        const b = request.indices[i + 1]!;
        const c = request.indices[i + 2]!;
        if (
            a >= 0 &&
            a < request.vertexCount &&
            b >= 0 &&
            b < request.vertexCount &&
            c >= 0 &&
            c < request.vertexCount
        ) {
            accepted[count++] = a;
            accepted[count++] = b;
            accepted[count++] = c;
        }
    }
    return { kind: "triangles", indices: accepted.slice(0, count) };
}
