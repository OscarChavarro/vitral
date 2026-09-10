import { PresentationElement } from "../PresentationElement.js";

/** Platform-neutral contract for reporting long-running operation progress. */
export abstract class ProgressMonitor extends PresentationElement {
    public abstract begin(): void;
    public abstract end(): void;
    public abstract update(minValue: number, maxValue: number, currentValue: number): void;
    public abstract getCurrentPercent(): number;
}
