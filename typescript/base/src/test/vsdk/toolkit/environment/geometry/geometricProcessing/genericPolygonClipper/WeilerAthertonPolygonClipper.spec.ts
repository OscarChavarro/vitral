import { describe, expect, it } from "vitest";
import { Polygon2D } from "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.js";
import { WeilerAthertonPolygonClipper } from "vsdk/toolkit/environment/geometry/geometricProcessing/polygonClipper/WeilerAthertonPolygonClipper.js";
import { _VertexNode2D } from "vsdk/toolkit/environment/geometry/geometricProcessing/polygonClipper/_VertexNode2D.js";

describe("WeilerAthertonPolygonClipper", () => {
    const intersect = (p0: _VertexNode2D, p1: _VertexNode2D, p2: _VertexNode2D, p3: _VertexNode2D) => {
        const point = new _VertexNode2D();
        const coincident = [false, false, false, false];
        const hit = new WeilerAthertonPolygonClipper().intersecLineLine2D(p0, p1, p2, p3, point, coincident);
        return { hit, point, coincident };
    };

    it("finds proper and vertical segment crossings", () => {
        const diagonal = intersect(
            new _VertexNode2D(0, 0),
            new _VertexNode2D(4, 4),
            new _VertexNode2D(0, 4),
            new _VertexNode2D(4, 0),
        );
        expect(diagonal).toMatchObject({ hit: true, point: { x: 2, y: 2 }, coincident: [false, false, false, false] });
        const vertical = intersect(
            new _VertexNode2D(2, -1),
            new _VertexNode2D(2, 3),
            new _VertexNode2D(0, 1),
            new _VertexNode2D(4, 1),
        );
        expect(vertical).toMatchObject({ hit: true, point: { x: 2, y: 1 } });
    });

    it("preserves endpoint-event suppression and p0/p2 coincidence", () => {
        const p0p2 = intersect(
            new _VertexNode2D(0, 0),
            new _VertexNode2D(2, 0),
            new _VertexNode2D(0, 0),
            new _VertexNode2D(0, 2),
        );
        expect(p0p2).toMatchObject({ hit: true, coincident: [true, false, true, false] });
        const p1p3 = intersect(
            new _VertexNode2D(0, 0),
            new _VertexNode2D(2, 0),
            new _VertexNode2D(0, 2),
            new _VertexNode2D(2, 0),
        );
        expect(p1p3).toMatchObject({ hit: false, coincident: [false, true, false, true] });
        const p0p3 = intersect(
            new _VertexNode2D(0, 0),
            new _VertexNode2D(2, 0),
            new _VertexNode2D(0, 2),
            new _VertexNode2D(0, 0),
        );
        expect(p0p3).toMatchObject({ hit: false, coincident: [true, false, false, true] });
        const p1p2 = intersect(
            new _VertexNode2D(0, 0),
            new _VertexNode2D(2, 0),
            new _VertexNode2D(2, 0),
            new _VertexNode2D(2, 2),
        );
        expect(p1p2).toMatchObject({ hit: false, coincident: [false, true, true, false] });
    });

    it("recognizes collinear overlap according to clipping-event rules", () => {
        const overlap = intersect(
            new _VertexNode2D(0, 0),
            new _VertexNode2D(4, 0),
            new _VertexNode2D(0, 0),
            new _VertexNode2D(2, 0),
        );
        expect(overlap).toMatchObject({ hit: true, coincident: [true, false, true, true] });
    });

    const polygon = (...coordinates: number[]): Polygon2D => {
        const result = new Polygon2D();
        for (let i = 0; i < coordinates.length; i += 2) result.addVertex(coordinates[i]!, coordinates[i + 1]!);
        return result;
    };

    const points = (value: Polygon2D): string[][] =>
        value.loops
            .filter((loop) => loop.vertices.length > 0)
            .map((loop) => loop.vertices.map((vertex) => `${vertex.x},${vertex.y}`).sort());

    const orderedPoints = (value: Polygon2D): string[][] =>
        value.loops
            .filter((loop) => loop.vertices.length > 0)
            .map((loop) => loop.vertices.map((vertex) => `${vertex.x},${vertex.y}`));

    const addLoop = (value: Polygon2D, ...coordinates: number[]): void => {
        value.nextLoop();
        for (let i = 0; i < coordinates.length; i += 2) value.addVertex(coordinates[i]!, coordinates[i + 1]!);
    };

    it("clips a crossing rectangle into inner and outer contours", () => {
        const clip = polygon(0, 0, 4, 0, 4, 4, 0, 4);
        const subject = polygon(2, -1, 6, -1, 6, 3, 2, 3);
        const inner = new Polygon2D();
        const outer = new Polygon2D();
        new WeilerAthertonPolygonClipper().clipPolygons(clip, subject, inner, outer);
        expect(points(inner)).toEqual([["2,0", "2,3", "4,0", "4,3"]]);
        expect(points(outer)).toEqual([["2,-1", "2,0", "4,0", "4,3", "6,-1", "6,3"]]);
        expect(orderedPoints(inner)).toEqual([["4,3", "2,3", "2,0", "4,0"]]);
        expect(orderedPoints(outer)).toEqual([["2,0", "2,-1", "6,-1", "6,3", "4,3", "4,0"]]);
    });

    it("preserves disjoint and completely contained subjects", () => {
        const clip = polygon(0, 0, 4, 0, 4, 4, 0, 4);
        const outside = polygon(6, 6, 8, 6, 8, 8, 6, 8);
        const inside = polygon(1, 1, 3, 1, 3, 3, 1, 3);
        const inner = new Polygon2D();
        const outer = new Polygon2D();
        const clipper = new WeilerAthertonPolygonClipper();
        clipper.clipPolygons(clip, outside, inner, outer);
        expect(points(inner)).toEqual([]);
        expect(points(outer)).toEqual([["6,6", "6,8", "8,6", "8,8"]]);
        const containedInner = new Polygon2D();
        const containedOuter = new Polygon2D();
        clipper.clipPolygons(clip, inside, containedInner, containedOuter);
        expect(points(containedInner)).toEqual([["1,1", "1,3", "3,1", "3,3"]]);
        expect(points(containedOuter)).toEqual([]);
    });

    it("handles reverse containment and clip holes without crossings", () => {
        const clip = polygon(0, 0, 8, 0, 8, 8, 0, 8);
        addLoop(clip, 2, 2, 2, 6, 6, 6, 6, 2);
        const subject = polygon(-1, -1, 9, -1, 9, 9, -1, 9);
        const inner = new Polygon2D();
        const outer = new Polygon2D();
        new WeilerAthertonPolygonClipper().clipPolygons(clip, subject, inner, outer);
        expect(points(inner)).toEqual([
            ["0,0", "0,8", "8,0", "8,8"],
            ["2,2", "2,6", "6,2", "6,6"],
        ]);
        expect(points(outer)).toEqual([
            ["0,0", "0,8", "8,0", "8,8"],
            ["2,2", "2,6", "6,2", "6,6"],
            ["-1,-1", "-1,9", "9,-1", "9,9"],
        ]);
        expect(orderedPoints(inner)).toEqual([
            ["0,0", "8,0", "8,8", "0,8"],
            ["2,2", "2,6", "6,6", "6,2"],
        ]);
    });

    it("clips an intersecting non-convex contour into separate regions", () => {
        const clip = polygon(0, 0, 5, 0, 5, 1, 1, 1, 1, 4, 5, 4, 5, 5, 0, 5);
        const subject = polygon(2, -1, 4, -1, 4, 6, 2, 6);
        const inner = new Polygon2D();
        const outer = new Polygon2D();
        new WeilerAthertonPolygonClipper().clipPolygons(clip, subject, inner, outer);
        expect(points(inner)).toEqual([
            ["2,0", "2,1", "4,0", "4,1"],
            ["2,4", "2,5", "4,4", "4,5"],
        ]);
        expect(points(outer)).toHaveLength(3);
        expect(orderedPoints(inner)).toEqual([
            ["4,0", "4,1", "2,1", "2,0"],
            ["4,4", "4,5", "2,5", "2,4"],
        ]);
    });

    it("builds and topologically merges the union", () => {
        const a = polygon(0, 0, 3, 0, 3, 3, 0, 3);
        const b = polygon(2, 0, 5, 0, 5, 3, 2, 3);
        const union = new Polygon2D();
        new WeilerAthertonPolygonClipper().unionPolygons(a, b, union);
        const nonEmpty = union.loops.filter((loop) => loop.vertices.length > 0);
        expect(nonEmpty).toHaveLength(1);
        expect(new Set(nonEmpty[0]!.vertices.map((vertex) => `${vertex.x},${vertex.y}`))).toEqual(
            new Set(["0,0", "0,3", "2,0", "2,3", "3,0", "3,3", "5,0", "5,3"]),
        );
        expect(orderedPoints(union)).toEqual([["2,3", "0,3", "0,0", "2,0", "3,0", "5,0", "5,3", "3,3"]]);
    });
});
