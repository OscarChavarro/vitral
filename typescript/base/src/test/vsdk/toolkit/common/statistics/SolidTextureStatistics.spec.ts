import { describe, expect, it } from "vitest";
import { SolidTextureStatistics } from "vsdk/toolkit/common/statistics/SolidTextureStatistics.js";
describe("SolidTextureStatistics", () =>
    it("aggregates and resets", () => {
        const a = new SolidTextureStatistics();
        a.callsToNoise = 4n;
        a.callsToDNoise = 2n;
        const total = new SolidTextureStatistics([a]);
        expect([total.callsToNoise, total.callsToDNoise]).toEqual([4n, 2n]);
        total.reset();
        expect([total.callsToNoise, total.callsToDNoise]).toEqual([0n, 0n]);
    }));
