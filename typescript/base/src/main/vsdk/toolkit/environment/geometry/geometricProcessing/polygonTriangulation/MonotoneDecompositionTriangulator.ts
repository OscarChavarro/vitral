import { Polygon2D } from "../../surface/polygon/Polygon2D.js";
import { _Construct } from "./monotoneDecomposition/_Construct.js";
import { _ContourAwarePolygonTriangulator } from "./monotoneDecomposition/_ContourAwarePolygonTriangulator.js";
import { _Monotone } from "./monotoneDecomposition/_Monotone.js";
import { _RandomSegmentOrder } from "./monotoneDecomposition/_RandomSegmentOrder.js";
import { _SegmentTableBuilder } from "./monotoneDecomposition/_SegmentTableBuilder.js";

export class MonotoneDecompositionTriangulator {
    private stage1PrepareAndOrder(input: Polygon2D, numVertices: number[]): void {
        if (input.loops.length <= 0) {
            throw new Error("Polygon input must contain at least one contour");
        }

        const contourSizes: number[] = [];
        const vertices: number[] = [];
        for (const contour of input.loops) {
            const pointCount = contour.vertices.length;
            if (pointCount <= 0) continue;
            contourSizes.push(pointCount);
            for (const vertex of contour.vertices) {
                vertices.push(vertex.x);
                vertices.push(vertex.y);
            }
        }

        if (contourSizes.length === 0 || vertices.length === 0) {
            throw new Error("Polygon input must contain at least one vertex");
        }

        const pointPairs = vertices.length / 2;
        const vertexArray = new Float64Array(vertices);
        const contourArray = new Int32Array(contourSizes);
        numVertices[0] = _SegmentTableBuilder.prepareSegments(
            vertexArray,
            pointPairs,
            contourArray,
            contourArray.length,
        );
    }

    private stage2BootStrap(numVertices: number): void {
        for (let i = 1; i <= numVertices; ++i) {
            _Construct.setSegmentInserted(i, false);
        }
        _RandomSegmentOrder.generateRandomOrdering(numVertices);
    }

    private stage3IncrementalBatchedInsertion(numVertices: number): void {
        _Construct.constructTrapezoids(numVertices);
    }

    private stage4FinalizeAndExtractTriangles(
        numVertices: number,
        out: MonotoneDecompositionTriangulator.Triangle[],
    ): number {
        const op = Array.from({ length: _Construct.SEGMENT_SIZE }, () => [0, 0, 0]);
        const nmonpoly = _Monotone.monotonateTrapezoids(numVertices);
        const ntriangles = _Monotone.triangulateMonotonePolygons(numVertices, nmonpoly, op);

        out.length = 0;
        for (let i = 0; i < ntriangles; ++i) {
            out.push(new MonotoneDecompositionTriangulator.Triangle(op[i]![0]! - 1, op[i]![1]! - 1, op[i]![2]! - 1));
        }

        return ntriangles;
    }

    public triangulate(input: Polygon2D, triangles: MonotoneDecompositionTriangulator.Triangle[]): number {
        const contourAwareTriangles = _ContourAwarePolygonTriangulator.triangulate(input, triangles);
        if (contourAwareTriangles >= 0) {
            return contourAwareTriangles;
        }

        const numVertices = [0];
        this.stage1PrepareAndOrder(input, numVertices);
        this.stage2BootStrap(numVertices[0]!);
        this.stage3IncrementalBatchedInsertion(numVertices[0]!);
        return this.stage4FinalizeAndExtractTriangles(numVertices[0]!, triangles);
    }
}

export namespace MonotoneDecompositionTriangulator {
    export class Triangle {
        public constructor(
            public readonly a: number,
            public readonly b: number,
            public readonly c: number,
        ) {}
    }
}
