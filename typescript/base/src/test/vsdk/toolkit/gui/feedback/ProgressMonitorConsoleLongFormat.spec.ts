import { describe, expect, it, vi } from "vitest";
import { ProgressMonitorConsoleLongFormat } from "vsdk/toolkit/gui/feedback/ProgressMonitorConsoleLongFormat.js";
describe("ProgressMonitorConsoleLongFormat", () =>
    it("reports precise progress", () => {
        const write = vi.spyOn(console, "log").mockImplementation(() => undefined),
            m = new ProgressMonitorConsoleLongFormat();
        m.begin();
        for (let i = 1; i <= 50; i++) m.update(0, 50, i);
        expect(m.getCurrentPercent()).toBe(100);
        m.end();
        expect(write).toHaveBeenCalledWith(expect.stringContaining("[100.00% of 50]"));
        write.mockRestore();
    }));
