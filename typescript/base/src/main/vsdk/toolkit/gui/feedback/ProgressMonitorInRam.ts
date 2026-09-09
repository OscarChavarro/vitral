import { VSDK } from "../../common/VSDK.js";
import { ProgressMonitor } from "./ProgressMonitor.js";

/** In-memory progress monitor for UI bindings and polling clients. */
export class ProgressMonitorInRam extends ProgressMonitor {
  private currentPercent = 0;
  public begin(): void { this.currentPercent = 0; }
  public end(): void { this.currentPercent = 100; }
  public update(minValue: number, maxValue: number, currentValue: number): void { if (maxValue - minValue < VSDK.EPSILON) return; this.currentPercent = 100 * (currentValue - minValue) / (maxValue - minValue); }
  public getCurrentPercent(): number { return this.currentPercent; }
}
