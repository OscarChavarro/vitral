import type { ConcurrentLinkedQueue } from "../../../../../java/util/concurrent/ConcurrentLinkedQueue.js";
import type { Runnable } from "../../../../../java/lang/Runnable.js";

import { ProgressMonitorConsoleLongFormat } from "../ProgressMonitorConsoleLongFormat.js";
import { ParallelProgressMonitorCommand } from "./ParallelProgressMonitorCommand.js";
import type { ParallelProgressMonitorEvent } from "./ParallelProgressMonitorEvent.js";

/**
Drains the progress event queue that the worker-side producers feed and writes
the resulting bar on the console, as the Java class does.

Runtime boundary: Java runs this loop on a dedicated `Thread` and blocks it
with `Thread.sleep(50)` whenever the queue runs dry. JavaScript has a single
event loop per runtime and no blocking sleep, so `run()` is `async` and awaits
a 50 ms timer instead. The waiting behavior, the event ordering and the
console output are the same; what differs is that the caller must `await` the
returned promise where Java would `join()` the consumer thread.
*/
export class ParallelProgressMonitorConsumer implements Runnable {
    private readonly concreteProgressMonitor: ProgressMonitorConsoleLongFormat;
    private readonly sharedEventQueue: ConcurrentLinkedQueue<ParallelProgressMonitorEvent>;
    private stillProcessingEvents: boolean;
    private totalElementsToProcess: bigint;
    private currentProcessedElements: bigint;

    public constructor(sharedEventQueue: ConcurrentLinkedQueue<ParallelProgressMonitorEvent>) {
        this.sharedEventQueue = sharedEventQueue;
        this.stillProcessingEvents = true;
        this.totalElementsToProcess = 0n;
        this.currentProcessedElements = 0n;
        this.concreteProgressMonitor = new ProgressMonitorConsoleLongFormat();
    }

    public async run(): Promise<void> {
        this.concreteProgressMonitor.begin();
        while (this.stillProcessingEvents || !this.sharedEventQueue.isEmpty()) {
            const nextEvent: ParallelProgressMonitorEvent | undefined = this.sharedEventQueue.poll();

            if (nextEvent === undefined) {
                await ParallelProgressMonitorConsumer.sleep(50);
                continue;
            }

            switch (nextEvent.getCommandType()) {
                case ParallelProgressMonitorCommand.INIT:
                    this.totalElementsToProcess += nextEvent.getNumberOfElementsToProcess();
                    break;
                case ParallelProgressMonitorCommand.PROCESS_NEXT_ELEMENT:
                    this.currentProcessedElements++;
                    this.concreteProgressMonitor.update(
                        0,
                        Number(this.totalElementsToProcess),
                        Number(this.currentProcessedElements),
                    );
                    break;
                case ParallelProgressMonitorCommand.FINISH:
                    this.stillProcessingEvents = false;
                    break;
                default:
                    break;
            }
        }
        this.concreteProgressMonitor.end();
    }

    private static sleep(milliseconds: number): Promise<void> {
        return new Promise<void>((resolve) => globalThis.setTimeout(resolve, milliseconds));
    }
}
