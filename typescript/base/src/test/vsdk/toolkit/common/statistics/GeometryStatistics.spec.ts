import { describe, expect, it } from "vitest";
import { GeometryStatistics } from "vsdk/toolkit/common/statistics/GeometryStatistics.js";
describe("GeometryStatistics", () =>
    it("aggregates and resets exact counters", () => {
        const a = new GeometryStatistics();
        a.incrementRaySphereTests();
        a.incrementRaySphereTests();
        a.incrementRaySphereTestsSucceeded();
        a.incrementRayTriangleTests();
        const b = new GeometryStatistics();
        b.incrementRaySphereTests();
        b.incrementRayTriangleTestsSucceeded();
        const total = new GeometryStatistics([a, b]);
        expect(total.getRaySphereTests()).toBe(3n);
        expect(total.getRaySphereTestsSucceeded()).toBe(1n);
        expect(total.getRayTriangleTests()).toBe(1n);
        expect(total.getRayTriangleTestsSucceeded()).toBe(1n);
        total.reset();
        expect(total.getRaySphereTests()).toBe(0n);
    }));
