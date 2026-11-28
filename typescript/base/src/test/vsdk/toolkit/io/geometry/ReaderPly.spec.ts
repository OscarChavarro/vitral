import { describe, expect, it } from "vitest";
import { ByteArrayInputStream } from "java/io/ByteArrayInputStream.js";
import { ReaderPly } from "vsdk/toolkit/io/geometry/ReaderPly.js";
import {
    assembleReaderPlyMeshPart,
    type ReaderPlyMeshRequest,
    type ReaderPlyWorkerFactory,
} from "vsdk/toolkit/io/geometry/ReaderPlyMeshWorker.js";

describe("ReaderPly", () => {
    it("preserves PLY fan order and filters out-of-range indices in the mesh job", async () => {
        const source = [
            "ply",
            "format ascii 1.0",
            "element vertex 4",
            "property float x",
            "property float y",
            "property float z",
            "element face 2",
            "property list uchar int vertex_indices",
            "end_header",
            "0 0 0",
            "1 0 0",
            "1 1 0",
            "0 1 0",
            "4 0 1 2 3",
            "3 0 1 -1",
        ].join("\n");
        const requests: ReaderPlyMeshRequest["kind"][] = [];
        let terminations = 0;
        const createWorker: ReaderPlyWorkerFactory = () => ({
            async execute(request) {
                requests.push(request.kind);
                return assembleReaderPlyMeshPart(request);
            },
            terminate() {
                terminations++;
            },
        });
        const input = new ByteArrayInputStream(new TextEncoder().encode(source));
        const result = await ReaderPly.importGeometry(input, createWorker);

        expect(requests).toEqual(["vertices", "triangles"]);
        expect(terminations).toBe(2);
        expect(result.pointCloud.size()).toBe(4);
        expect(result.pointCloud.get(2).z()).toBe(1);
        expect(result.triangleMesh?.getTriangleIndexes()).toEqual(Int32Array.of(0, 1, 2, 0, 2, 3));
    });
});
