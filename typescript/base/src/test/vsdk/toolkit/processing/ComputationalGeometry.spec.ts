import { describe, expect, it } from "vitest";
import { Vector2Dd } from "vsdk/toolkit/common/linealAlgebra/Vector2Dd.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { ComputationalGeometry } from "vsdk/toolkit/processing/ComputationalGeometry.js";
import { Containment } from "vsdk/toolkit/processing/Containment.js";

describe("ComputationalGeometry", () => {
    it("clips with unordered rectangle corners and the Java tolerance inset", () => {
        const result = ComputationalGeometry.cohenSutherlandLineClipping2DResult(
            new Vector2Dd(-1, 1),
            new Vector2Dd(3, 1),
            new Vector2Dd(2, 2),
            new Vector2Dd(0, 0),
            0.125,
        );

        expect(result.accepted()).toBe(true);
        expect(result.clipped0()).toEqual(new Vector2Dd(0.25, 1));
        expect(result.clipped1()).toEqual(new Vector2Dd(1.75, 1));
    });

    it("reports an unordered point-to-segment distance as outside", () => {
        const origin = new Vector3Dd(0, 0, 0);
        const end = new Vector3Dd(1, 0, 0);
        const point = new Vector3Dd(NaN, 0, 0);

        expect(ComputationalGeometry.lineSegmentContainmentTest(origin, end, point, 1)).toBe(Containment.OUTSIDE);
        expect(ComputationalGeometry.lineSegmentContainmentTest(origin, end, origin, NaN)).toBe(Containment.OUTSIDE);
    });
});
