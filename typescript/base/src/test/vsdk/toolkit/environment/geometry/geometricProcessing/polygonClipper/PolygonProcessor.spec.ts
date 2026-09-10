import { describe, expect, it } from "vitest";
import { PolygonProcessor } from "vsdk/toolkit/environment/geometry/geometricProcessing/polygonClipper/PolygonProcessor.js";
import { Vertex2D } from "vsdk/toolkit/environment/geometry/element/Vertex2D.js";
import { Polygon2D } from "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.js";

describe("PolygonProcessor", () => {
    it("classifies points inside, outside, and on the boundary", () => {
        const square = [new Vertex2D(0, 0), new Vertex2D(4, 0), new Vertex2D(4, 4), new Vertex2D(0, 4)];

        expect(PolygonProcessor.isPointInsidePolygon2D(new Vertex2D(2, 2), square)).toBe(1);
        expect(PolygonProcessor.isPointInsidePolygon2D(new Vertex2D(5, 2), square)).toBe(-1);
        expect(PolygonProcessor.isPointInsidePolygon2D(new Vertex2D(2, 0), square)).toBe(0);
    });

    it("simplifies a nearly straight contour while preserving its endpoints", () => {
        const polygon = new Polygon2D();
        polygon.addVertex(0, 0);
        polygon.addVertex(1, 0.001);
        polygon.addVertex(2, 0);
        polygon.addVertex(3, 2);

        const simplified = PolygonProcessor.polygon2DSimplify(polygon, 0.01, true);

        expect(simplified.loops[0]!.vertices.map((vertex) => [vertex.x, vertex.y])).toEqual([
            [0, 0],
            [2, 0],
            [3, 2],
        ]);
        expect(simplified.loops[0]!.vertices[0]).not.toBe(polygon.loops[0]!.vertices[0]);
    });

    it("builds exterior-contour links for nested contours", () => {
        const polygon = new Polygon2D();
        polygon.addVertex(0, 0);
        polygon.addVertex(10, 0);
        polygon.addVertex(10, 10);
        polygon.addVertex(0, 10);
        polygon.nextLoop();
        polygon.addVertex(2, 2);
        polygon.addVertex(4, 2);
        polygon.addVertex(4, 4);
        polygon.addVertex(2, 4);

        PolygonProcessor.classifyContourHoles(polygon);

        expect(polygon.getHeadNode()).not.toBeNull();
        expect(polygon.loops[1]!.getExteriorContour()).toBe(polygon.loops[0]);
    });
});
