#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Thread.h"
#include "vsdk/toolkit/gui/feedback/parallel/ParallelProgressMonitorConsumer.h"
ParallelProgressMonitorConsumer::ParallelProgressMonitorConsumer(
    ConcurrentLinkedQueue<ParallelProgressMonitorEvent>* queue)
    : concreteProgressMonitor(),
      sharedEventQueue(queue),
      stillProcessingEvents(TRUE),
      totalElementsToProcess(0),
      currentProcessedElements(0)
{
}

void ParallelProgressMonitorConsumer::run()
{
    concreteProgressMonitor.begin();
    while ( stillProcessingEvents || !sharedEventQueue->isEmpty() ) {
        ParallelProgressMonitorEvent nextEvent(FINISH, 0);
        BOOLEAN hasEvent = sharedEventQueue->poll(&nextEvent);

        if ( !hasEvent ) {
            Thread::sleep(50);
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
                stillProcessingEvents = FALSE;
                break;
            default:
                break;
        }
    }
    concreteProgressMonitor.end();
}
