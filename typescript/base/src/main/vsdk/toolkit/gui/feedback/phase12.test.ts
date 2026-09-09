import { describe, expect, it, vi } from "vitest";
import { ProgressMonitorConsole, ProgressMonitorConsoleLongFormat, ProgressMonitorInRam } from "../../../../index.js";

describe("Phase 12 progress-monitor contracts", () => {
  it("updates in-memory progress and honors degenerate ranges", () => {
    const monitor = new ProgressMonitorInRam();
    monitor.begin(); monitor.update(0, 200, 50); expect(monitor.getCurrentPercent()).toBe(25);
    monitor.update(3, 3, 3); expect(monitor.getCurrentPercent()).toBe(25);
    monitor.end(); expect(monitor.getCurrentPercent()).toBe(100);
  });

  it("emits the compact console milestones and keeps its stepped percentage", () => {
    const write = vi.spyOn(console, "log").mockImplementation(() => undefined);
    const monitor = new ProgressMonitorConsole();
    monitor.begin(); monitor.update(0, 100, 27); monitor.end();
    expect(monitor.getCurrentPercent()).toBe(26);
    expect(write).toHaveBeenCalledWith(" 25% ");
    write.mockRestore();
  });

  it("reports precise long-format progress and finishes at 100 percent", () => {
    const write = vi.spyOn(console, "log").mockImplementation(() => undefined);
    const monitor = new ProgressMonitorConsoleLongFormat();
    monitor.begin(); for (let i = 1; i <= 50; i++) monitor.update(0, 50, i);
    expect(monitor.getCurrentPercent()).toBe(100);
    monitor.end(); expect(monitor.getCurrentPercent()).toBe(100);
    expect(write).toHaveBeenCalledWith(expect.stringContaining("[100.00% of 50]"));
    write.mockRestore();
  });
});
