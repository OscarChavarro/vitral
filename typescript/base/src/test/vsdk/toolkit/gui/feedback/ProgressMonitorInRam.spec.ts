import { describe, expect, it } from "vitest";
import { ProgressMonitorInRam } from "vsdk/toolkit/gui/feedback/ProgressMonitorInRam.js";
describe("ProgressMonitorInRam", () =>
    it("updates progress", () => {
        const m = new ProgressMonitorInRam();
        m.begin();
        m.update(0, 200, 50);
        expect(m.getCurrentPercent()).toBe(25);
        m.update(3, 3, 3);
        expect(m.getCurrentPercent()).toBe(25);
        m.end();
        expect(m.getCurrentPercent()).toBe(100);
    }));
