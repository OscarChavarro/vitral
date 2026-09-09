import { VSDK } from "../../common/VSDK.js";
import { ProgressMonitor } from "./ProgressMonitor.js";

/** Browser-safe console representation of the compact Java progress bar. */
export class ProgressMonitorConsole extends ProgressMonitor {
  private currentPercent = 0;
  private jumpPercent = 2;
  private lastPrintedLabel = 0;
  public begin(): void { this.currentPercent = 0; this.lastPrintedLabel = 0; this.jumpPercent = 2; this.write("[ 0% "); }
  public end(): void { this.write(" 100% ]"); }
  public update(minValue: number, maxValue: number, currentValue: number): void {
    if (maxValue - minValue < VSDK.EPSILON) return;
    const value = 100 * (currentValue - minValue) / (maxValue - minValue);
    while (this.currentPercent + this.jumpPercent < value) { this.currentPercent += this.jumpPercent; if (!this.testLabelLimit(25) && !this.testLabelLimit(50) && !this.testLabelLimit(75)) this.write("-"); }
  }
  public getCurrentPercent(): number { return this.currentPercent; }
  private testLabelLimit(limit: number): boolean { if (limit === this.lastPrintedLabel) return false; if (this.currentPercent - 6 * this.jumpPercent / 10 < limit && this.currentPercent + 6 * this.jumpPercent / 10 > limit) { this.write(` ${limit}% `); this.lastPrintedLabel = limit; return true; } return false; }
  private write(text: string): void { console.log(text); }
}
