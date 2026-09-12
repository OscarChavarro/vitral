import type { ConcurrentLinkedQueue } from "../../../../../java/util/concurrent/ConcurrentLinkedQueue.js";
import { AtomicLong } from "../../../../../java/util/concurrent/atomic/AtomicLong.js";

import { ProgressMonitor } from "../ProgressMonitor.js";
import { ParallelProgressMonitorCommand } from "./ParallelProgressMonitorCommand.js";
import { ParallelProgressMonitorEvent } from "./ParallelProgressMonitorEvent.js";

export class ParallelProgressMonitorProducer extends ProgressMonitor {
    private readonly sharedEventQueue: ConcurrentLinkedQueue<ParallelProgressMonitorEvent>;
    private readonly processedElements: AtomicLong;
    private totalElements: bigint;

    public constructor(sharedEventQueue: ConcurrentLinkedQueue<ParallelProgressMonitorEvent>) {
        super();
        this.sharedEventQueue = sharedEventQueue;
        this.processedElements = new AtomicLong(0n);
        this.totalElements = 0n;
    }

    public init(totalElementsToProcess: bigint): void {
        this.totalElements = totalElementsToProcess;
        this.processedElements.set(0n);
        this.sharedEventQueue.add(
            new ParallelProgressMonitorEvent(ParallelProgressMonitorCommand.INIT, totalElementsToProcess),
        );
    }

    public finish(): void {
        this.sharedEventQueue.add(new ParallelProgressMonitorEvent(ParallelProgressMonitorCommand.FINISH, 0n));
    }

    public begin(): void {}

    public end(): void {}

    public update(_minimumValue: number, _maximumValue: number, _currentValue: number): void {
        this.processedElements.incrementAndGet();
        this.sharedEventQueue.add(
            new ParallelProgressMonitorEvent(ParallelProgressMonitorCommand.PROCESS_NEXT_ELEMENT, 0n),
        );
    }

    public getCurrentPercent(): number {
        const total: bigint = this.totalElements;
        if (total <= 0n) {
            return 0;
        }
        return (100.0 * Number(this.processedElements.get())) / Number(total);
    }
}
