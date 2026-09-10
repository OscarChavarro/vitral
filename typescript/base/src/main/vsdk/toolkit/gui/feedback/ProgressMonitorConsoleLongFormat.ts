import { VSDK } from "../../common/VSDK.js";
import { ProgressMonitor } from "./ProgressMonitor.js";

/** Single-runtime console monitor; it deliberately has no browser-worker mode. */
export class ProgressMonitorConsoleLongFormat extends ProgressMonitor {
    private n = 0n;
    private charactersPrintedInLastLine = 0;
    private currentPercent = 0;
    public begin(): void {
        this.n = 0n;
        this.currentPercent = 0;
        this.charactersPrintedInLastLine = 0;
        this.write("    ");
    }
    public end(): void {
        this.currentPercent = 100;
        this.write(`${" ".repeat(Math.max(0, 55 - this.charactersPrintedInLastLine))} - [100% / Operation finished!]`);
    }
    public update(minValue: number, maxValue: number, currentValue: number): void {
        if (Math.abs(maxValue - minValue) < VSDK.EPSILON) return;
        const value = (100 * (currentValue - minValue)) / (maxValue - minValue);
        this.currentPercent = value;
        this.n++;
        this.write(".");
        this.charactersPrintedInLastLine++;
        if (this.n % 10n === 0n) {
            this.write(" ");
            this.charactersPrintedInLastLine++;
        }
        if (this.n % 50n === 0n) {
            this.write(` - [${VSDK.formatDouble(value)}% of ${Math.round(maxValue)}]\n    `);
            this.charactersPrintedInLastLine = 0;
        }
    }
    public getCurrentPercent(): number {
        return this.currentPercent;
    }
    private write(text: string): void {
        console.log(text);
    }
}
