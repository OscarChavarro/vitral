import { describe, expect, it } from "vitest";
import { GeometryStatistics, PolyhedralBoundedSolidStatistics, RaytraceStatistics, RenderingStatistics, SolidTextureStatistics } from "../../../../index.js";

describe("Phase 10 statistics contracts", () => {
  it("counts, aggregates and resets geometry statistics using exact longs", () => {
    const first = new GeometryStatistics(); first.incrementRaySphereTests(); first.incrementRaySphereTests(); first.incrementRaySphereTestsSucceeded(); first.incrementRayTriangleTests();
    const second = new GeometryStatistics(); second.incrementRaySphereTests(); second.incrementRayTriangleTestsSucceeded();
    const total = new GeometryStatistics([first, second]);
    expect(total.getRaySphereTests()).toBe(3n); expect(total.getRaySphereTestsSucceeded()).toBe(1n); expect(total.getRayTriangleTests()).toBe(1n); expect(total.getRayTriangleTestsSucceeded()).toBe(1n);
    total.reset(); expect(total.getRaySphereTests()).toBe(0n);
  });

  it("aggregates and resets solid-texture statistics", () => {
    const first = new SolidTextureStatistics(); first.callsToNoise = 4n; first.callsToDNoise = 2n;
    const total = new SolidTextureStatistics([first]);
    expect([total.callsToNoise, total.callsToDNoise]).toEqual([4n, 2n]); total.reset(); expect([total.callsToNoise, total.callsToDNoise]).toEqual([0n, 0n]);
  });

  it("keeps instrumentation opt-in and validates rendering counter keys", () => {
    expect(RaytraceStatistics.isEnabled()).toBe(false); expect(PolyhedralBoundedSolidStatistics.isEnabled()).toBe(false);
    RaytraceStatistics.recordPrimaryRay(); PolyhedralBoundedSolidStatistics.recordSetOpCall(1);
    RenderingStatistics.resetPrimitiveCounters(); RenderingStatistics.accumulatePrimitiveCount(0, 2);
    expect(() => RenderingStatistics.accumulatePrimitiveCount(-1, 1)).toThrow(RangeError);
  });
});
