import { describe, expect, it } from "vitest";
import { PolyhedralBoundedSolidStatistics } from "vsdk/toolkit/common/statistics/PolyhedralBoundedSolidStatistics.js";
import { RaytraceStatistics } from "vsdk/toolkit/common/statistics/RaytraceStatistics.js";
import { RenderingStatistics } from "vsdk/toolkit/common/statistics/RenderingStatistics.js";
describe("RenderingStatistics", () =>
    it("validates primitive counter keys", () => {
        expect(RaytraceStatistics.isEnabled()).toBe(false);
        expect(PolyhedralBoundedSolidStatistics.isEnabled()).toBe(false);
        RenderingStatistics.resetPrimitiveCounters();
        RenderingStatistics.accumulatePrimitiveCount(0, 2);
        expect(() => RenderingStatistics.accumulatePrimitiveCount(-1, 1)).toThrow(RangeError);
    }));
