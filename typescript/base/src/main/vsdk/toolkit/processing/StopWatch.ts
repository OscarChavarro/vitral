import { ProcessingElement } from "./ProcessingElement.js";

/**
`System.nanoTime()`: a monotonic clock in nanoseconds. `performance.now()`
exists in browsers and in Node, unlike the `process.hrtime` that
`java/lang/System.ts` uses.
*/
const nanoTime = (): number => performance.now() * 1_000_000;

export class StopWatch extends ProcessingElement {
    private startTime: number;
    private stopTime: number;
    private running: boolean;

    public constructor() {
        super();
        this.startTime = 0;
        this.stopTime = 1;
        this.running = false;
    }

    public start(): void {
        this.running = true;
        this.startTime = nanoTime();
    }

    public stop(): void {
        if (this.running) {
            this.stopTime = nanoTime();
        }
        this.running = false;
    }

    public getElapsedRealTime(): number {
        if (this.running) {
            this.stopTime = nanoTime();
        }
        let a: number, b: number;
        a = this.startTime;
        b = this.stopTime;
        return (b - a) / 1000000000.0;
    }
}
