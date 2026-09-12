import { describe, expect, it, vi } from "vitest";
import { ProgressMonitorConsoleLongFormat } from "vsdk/toolkit/gui/feedback/ProgressMonitorConsoleLongFormat.js";
describe("ProgressMonitorConsoleLongFormat", () =>
    it("reports precise progress", () => {
        // See the note in ProgressMonitorConsole.spec.ts: Java's
        // `System.out.print` maps onto the standard output stream, never onto
        // `console.log`.
        const write = vi.spyOn(process.stdout, "write").mockImplementation(() => true),
            m = new ProgressMonitorConsoleLongFormat();
        m.begin();
        for (let i = 1; i <= 50; i++) m.update(0, 50, i);
        expect(m.getCurrentPercent()).toBe(100);
        m.end();
        expect(write).toHaveBeenCalledWith(expect.stringContaining("[100.00% of 50]"));
        expect(write).toHaveBeenCalledWith(expect.stringContaining("[100% / Operation finished!] \n"));
        write.mockRestore();
    }));
