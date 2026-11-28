import { describe, expect, it } from "vitest";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Triangle } from "vsdk/toolkit/environment/geometry/element/Triangle.js";
import { Containment } from "vsdk/toolkit/processing/Containment.js";

describe("Triangle.containmentTest", () => {
    const a = new Vector3Dd(0, 0, 0);
    const b = new Vector3Dd(1, 0, 0);
    const c = new Vector3Dd(0, 1, 0);

    it("measures the distance to the triangle's opposite edge", () => {
        const point = new Vector3Dd(0.6, 0.6, 0);

        expect(Triangle.containmentTest(a, b, c, point, 0.1)).toBe(Containment.OUTSIDE);
        expect(Triangle.containmentTest(a, b, c, point, 0.15)).toBe(Containment.LIMIT);
        expect(Containment.OUTSIDE).toBe(-1);
        expect(Containment.LIMIT).toBe(0);
    });

    it("retains the closest-vertex result for a triangle collapsed to a point", () => {
        expect(Triangle.containmentTest(a, a, a, a, 0)).toBe(Containment.LIMIT);
        expect(Triangle.containmentTest(a, a, a, b, 0)).toBe(Containment.OUTSIDE);
    });
});
