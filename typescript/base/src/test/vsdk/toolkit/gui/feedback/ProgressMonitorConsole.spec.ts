import { describe, expect, it, vi } from "vitest";
import { ProgressMonitorConsole } from "vsdk/toolkit/gui/feedback/ProgressMonitorConsole.js";
describe("ProgressMonitorConsole", () =>
    it("emits milestones", () => {
        // Java writes the bar with `System.out.print`, so on a runtime that has
        // a standard output stream the port writes there and not through
        // `console.log`, which would terminate every fragment.
        const write = vi.spyOn(process.stdout, "write").mockImplementation(() => true),
            m = new ProgressMonitorConsole();
        m.begin();
        m.update(0, 100, 27);
        m.end();
        expect(m.getCurrentPercent()).toBe(26);
        expect(write).toHaveBeenCalledWith(" 25% ");
        expect(write).toHaveBeenCalledWith("[ 0% ");
        expect(write).toHaveBeenCalledWith(" 100% ]\n");
        write.mockRestore();
    }));
