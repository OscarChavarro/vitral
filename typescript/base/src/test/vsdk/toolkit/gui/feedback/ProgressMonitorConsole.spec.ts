import { describe, expect, it, vi } from "vitest";
import { ProgressMonitorConsole } from "vsdk/toolkit/gui/feedback/ProgressMonitorConsole.js";
describe("ProgressMonitorConsole", () =>
    it("emits milestones", () => {
        const write = vi.spyOn(console, "log").mockImplementation(() => undefined),
            m = new ProgressMonitorConsole();
        m.begin();
        m.update(0, 100, 27);
        m.end();
        expect(m.getCurrentPercent()).toBe(26);
        expect(write).toHaveBeenCalledWith(" 25% ");
        write.mockRestore();
    }));
