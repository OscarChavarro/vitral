import { describe, expect, it } from "vitest";
import { PolygonTopologicalMerger } from "vsdk/toolkit/environment/geometry/geometricProcessing/polygonClipper/PolygonTopologicalMerger.js";
import { Polygon2D } from "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.js";

describe("PolygonTopologicalMerger", () => {
    it("removes duplicate, collinear, and cyclically equivalent contours", () => {
        const polygon = new Polygon2D();
        polygon.addVertex(0, 0);
        polygon.addVertex(1, 0);
        polygon.addVertex(2, 0);
        polygon.addVertex(2, 2);
        polygon.addVertex(0, 2);
        polygon.addVertex(0, 0);
        polygon.nextLoop();
        polygon.addVertex(2, 2);
        polygon.addVertex(2, 0);
        polygon.addVertex(0, 0);
        polygon.addVertex(0, 2);

        new PolygonTopologicalMerger().mergeInPlace(polygon);

        expect(polygon.loops).toHaveLength(1);
        expect(polygon.loops[0]!.vertices.map(({ x, y }) => [x, y])).toEqual([
            [0, 0],
            [2, 0],
            [2, 2],
            [0, 2],
        ]);
    });
});
