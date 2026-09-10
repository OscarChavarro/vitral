import { describe, expect, it } from "vitest";
import { MonotoneDecompositionTriangulator } from "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/MonotoneDecompositionTriangulator.js";
import { Polygon2D } from "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.js";

describe("MonotoneDecompositionTriangulator", () => {
    it("executes Java's four Seidel stages directly", () => {
        const polygon = new Polygon2D();
        polygon.addVertex(0, 0);
        polygon.addVertex(2, 0);
        polygon.addVertex(2, 2);
        polygon.addVertex(0, 2);
        const triangles: MonotoneDecompositionTriangulator.Triangle[] = [];
        const stages = new MonotoneDecompositionTriangulator() as unknown as {
            stage1PrepareAndOrder(input: Polygon2D, count: number[]): void;
            stage2BootStrap(count: number): void;
            stage3IncrementalBatchedInsertion(count: number): void;
            stage4FinalizeAndExtractTriangles(
                count: number,
                output: MonotoneDecompositionTriangulator.Triangle[],
            ): number;
        };
        const count = [0];
        stages.stage1PrepareAndOrder(polygon, count);
        stages.stage2BootStrap(count[0]!);
        stages.stage3IncrementalBatchedInsertion(count[0]!);
        expect(stages.stage4FinalizeAndExtractTriangles(count[0]!, triangles)).toBe(2);
        expect(
            triangles
                .flatMap((triangle) => [triangle.a, triangle.b, triangle.c])
                .every((index) => index >= 0 && index < 4),
        ).toBe(true);
    });

    it("triangulates a convex contour using original zero-based indices", () => {
        const polygon = new Polygon2D();
        polygon.addVertex(0, 0);
        polygon.addVertex(2, 0);
        polygon.addVertex(2, 2);
        polygon.addVertex(0, 2);
        const triangles: MonotoneDecompositionTriangulator.Triangle[] = [];
        expect(new MonotoneDecompositionTriangulator().triangulate(polygon, triangles)).toBe(2);
        expect(triangles).toEqual([
            new MonotoneDecompositionTriangulator.Triangle(3, 0, 1),
            new MonotoneDecompositionTriangulator.Triangle(1, 2, 3),
        ]);
    });

    it("triangulates an outer contour with one hole", () => {
        const polygon = new Polygon2D();
        polygon.addVertex(0, 0);
        polygon.addVertex(6, 0);
        polygon.addVertex(6, 6);
        polygon.addVertex(0, 6);
        polygon.nextLoop();
        polygon.addVertex(2, 2);
        polygon.addVertex(2, 4);
        polygon.addVertex(4, 4);
        polygon.addVertex(4, 2);
        const triangles: MonotoneDecompositionTriangulator.Triangle[] = [];
        expect(new MonotoneDecompositionTriangulator().triangulate(polygon, triangles)).toBe(8);
        expect(triangles).toEqual([
            new MonotoneDecompositionTriangulator.Triangle(0, 1, 7),
            new MonotoneDecompositionTriangulator.Triangle(0, 7, 4),
            new MonotoneDecompositionTriangulator.Triangle(3, 0, 4),
            new MonotoneDecompositionTriangulator.Triangle(3, 4, 5),
            new MonotoneDecompositionTriangulator.Triangle(3, 5, 6),
            new MonotoneDecompositionTriangulator.Triangle(6, 7, 1),
            new MonotoneDecompositionTriangulator.Triangle(6, 1, 2),
            new MonotoneDecompositionTriangulator.Triangle(6, 2, 3),
        ]);
        expect(triangles.every((triangle) => ![triangle.a, triangle.b, triangle.c].every((index) => index >= 4))).toBe(
            true,
        );
    });

    it("triangulates two disjoint holes with flattened indices", () => {
        const polygon = new Polygon2D();
        polygon.addVertex(0, 0);
        polygon.addVertex(10, 0);
        polygon.addVertex(10, 10);
        polygon.addVertex(0, 10);
        polygon.nextLoop();
        polygon.addVertex(1, 1);
        polygon.addVertex(1, 3);
        polygon.addVertex(3, 3);
        polygon.addVertex(3, 1);
        polygon.nextLoop();
        polygon.addVertex(6, 6);
        polygon.addVertex(6, 8);
        polygon.addVertex(8, 8);
        polygon.addVertex(8, 6);
        const triangles: MonotoneDecompositionTriangulator.Triangle[] = [];
        expect(new MonotoneDecompositionTriangulator().triangulate(polygon, triangles)).toBe(14);
        expect(
            triangles
                .flatMap((triangle) => [triangle.a, triangle.b, triangle.c])
                .every((index) => index >= 0 && index < 12),
        ).toBe(true);
    });

    it("preserves an island nested inside a hole", () => {
        const polygon = new Polygon2D();
        polygon.addVertex(0, 0);
        polygon.addVertex(10, 0);
        polygon.addVertex(10, 10);
        polygon.addVertex(0, 10);
        polygon.nextLoop();
        polygon.addVertex(2, 2);
        polygon.addVertex(2, 8);
        polygon.addVertex(8, 8);
        polygon.addVertex(8, 2);
        polygon.nextLoop();
        polygon.addVertex(4, 4);
        polygon.addVertex(6, 4);
        polygon.addVertex(6, 6);
        polygon.addVertex(4, 6);
        const triangles: MonotoneDecompositionTriangulator.Triangle[] = [];
        expect(new MonotoneDecompositionTriangulator().triangulate(polygon, triangles)).toBe(10);
        expect(triangles.some((triangle) => [triangle.a, triangle.b, triangle.c].every((index) => index >= 8))).toBe(
            true,
        );
    });

    it("clears output for contours with no triangulable area", () => {
        const polygon = new Polygon2D();
        polygon.addVertex(0, 0);
        polygon.addVertex(1, 0);
        polygon.addVertex(2, 0);
        const triangles = [new MonotoneDecompositionTriangulator.Triangle(4, 5, 6)];
        expect(new MonotoneDecompositionTriangulator().triangulate(polygon, triangles)).toBe(0);
        expect(triangles).toEqual([]);
    });

    it("selects a visible bridge inside a concave outer contour", () => {
        const polygon = new Polygon2D();
        for (const [x, y] of [
            [0, 0],
            [8, 0],
            [8, 8],
            [5, 8],
            [5, 3],
            [3, 3],
            [3, 8],
            [0, 8],
        ])
            polygon.addVertex(x!, y!);
        polygon.nextLoop();
        for (const [x, y] of [
            [5.5, 4],
            [5.5, 6],
            [7.5, 6],
            [7.5, 4],
        ])
            polygon.addVertex(x!, y!);
        const triangles: MonotoneDecompositionTriangulator.Triangle[] = [];
        expect(new MonotoneDecompositionTriangulator().triangulate(polygon, triangles)).toBe(12);
    });
});
