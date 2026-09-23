import { ProcessingElement } from "./ProcessingElement.js";
export class StopWatch extends ProcessingElement {
    private startTime = 0;
    private stopTime = 0;
    private running = false;
    public start(): void {
        this.startTime = Date.now();
        this.running = true;
    }
    public stop(): void {
        this.stopTime = Date.now();
        this.running = false;
    }
    public getElapsedRealTime(): number {
        return ((this.running ? Date.now() : this.stopTime) - this.startTime) / 1000;
    }
}
