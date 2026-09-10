import { describe, expect, it } from "vitest";
import { _InsertionBatchSchedule } from "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_InsertionBatchSchedule.js";
import { _RandomSegmentOrder } from "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_RandomSegmentOrder.js";
import { _TriangulationSegment } from "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_TriangulationSegment.js";

describe("monotone-decomposition scheduling primitives", () => {
    it("uses the Java Park-Miller ordering sequence", () => {
        _RandomSegmentOrder.generateRandomOrdering(6);
        expect(Array.from({ length: 6 }, () => _RandomSegmentOrder.chooseSegment())).toEqual([1, 6, 2, 4, 5, 3]);
    });

    it("keeps the log-star insertion schedule", () => {
        expect(_InsertionBatchSchedule.mathLogStarN(1)).toBe(0);
        expect(_InsertionBatchSchedule.mathLogStarN(16)).toBe(3);
        expect(_InsertionBatchSchedule.mathN(16, 0)).toBe(1);
        expect(_InsertionBatchSchedule.mathN(16, 2)).toBe(8);
    });

    it("initializes a segment in the reserved zero-index compatible state", () => {
        const segment = new _TriangulationSegment();
        expect(segment.hasBeenInserted).toBe(false);
        expect(segment.nextSegmentIndex).toBe(0);
        expect(segment.previousSegmentIndex).toBe(0);
        expect(segment.startPoint.x).toBe(0);
        expect(segment.endPoint.y).toBe(0);
    });
});
