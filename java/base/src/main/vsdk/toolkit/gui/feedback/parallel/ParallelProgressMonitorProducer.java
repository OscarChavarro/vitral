package vsdk.toolkit.gui.feedback.parallel;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import vsdk.toolkit.gui.feedback.ProgressMonitor;

public class ParallelProgressMonitorProducer extends ProgressMonitor {
    private final ConcurrentLinkedQueue<ParallelProgressMonitorEvent> sharedEventQueue;
    private final AtomicLong processedElements;
    private volatile long totalElements;

    public ParallelProgressMonitorProducer(ConcurrentLinkedQueue<ParallelProgressMonitorEvent> sharedEventQueue) {
        this.sharedEventQueue = sharedEventQueue;
        this.processedElements = new AtomicLong(0);
        this.totalElements = 0;
    }

    public void init(long totalElementsToProcess) {
        totalElements = totalElementsToProcess;
        processedElements.set(0);
        sharedEventQueue.add(
            new ParallelProgressMonitorEvent(
                ParallelProgressMonitorCommand.INIT,
                totalElementsToProcess));
    }

    public void finish() {
        sharedEventQueue.add(
            new ParallelProgressMonitorEvent(
                ParallelProgressMonitorCommand.FINISH,
                0));
    }

    @Override
    public void begin() {
    }

    @Override
    public void end() {

    }

    @Override
    public void update(double minimumValue, double maximumValue, double currentValue) {
        processedElements.incrementAndGet();
        sharedEventQueue.add(
            new ParallelProgressMonitorEvent(
                ParallelProgressMonitorCommand.PROCESS_NEXT_ELEMENT,
                0));
    }

    @Override
    public double getCurrentPercent() {
        long total = totalElements;
        if ( total <= 0 ) {
            return 0;
        }
        return 100.0 * processedElements.get() / total;
    }
}
