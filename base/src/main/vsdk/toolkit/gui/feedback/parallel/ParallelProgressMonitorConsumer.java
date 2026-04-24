package vsdk.toolkit.gui.feedback.parallel;

import java.util.concurrent.ConcurrentLinkedQueue;
import vsdk.toolkit.gui.feedback.ProgressMonitorConsoleLongFormat;

public class ParallelProgressMonitorConsumer implements Runnable {
    private final ProgressMonitorConsoleLongFormat concreteProgressMonitor;
    private final ConcurrentLinkedQueue<ParallelProgressMonitorEvent> sharedEventQueue;
    private boolean stillProcessingEvents;
    private long totalElementsToProcess;
    private long currentProcessedElements;

    public ParallelProgressMonitorConsumer(
        ConcurrentLinkedQueue<ParallelProgressMonitorEvent> sharedEventQueue)
    {
        this.sharedEventQueue = sharedEventQueue;
        stillProcessingEvents = true;
        totalElementsToProcess = 0L;
        currentProcessedElements = 0L;
        concreteProgressMonitor = new ProgressMonitorConsoleLongFormat();
    }

    @Override
    public void run() {
        try {
            concreteProgressMonitor.begin();
            while ( stillProcessingEvents || !sharedEventQueue.isEmpty() ) {
                ParallelProgressMonitorEvent nextEvent = sharedEventQueue.poll();

                if (nextEvent == null) {
                    Thread.sleep(50);
                    continue;
                }

                switch (nextEvent.getCommandType()) {
                    case INIT:
                        totalElementsToProcess += nextEvent.getNumberOfElementsToProcess();
                        break;
                    case PROCESS_NEXT_ELEMENT:
                        currentProcessedElements++;
                        concreteProgressMonitor.update(0, totalElementsToProcess, currentProcessedElements);
                        break;
                    case FINISH:
                        stillProcessingEvents = false;
                        break;
                    default:
                        break;
                }
            }
            concreteProgressMonitor.end();
        }
        catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
        }
    }
}
