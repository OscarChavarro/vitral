#include "ParallelProgressMonitorConsumer.h"

#include <unistd.h>

ParallelProgressMonitorConsumer::ParallelProgressMonitorConsumer(
    java::concurrent::ConcurrentLinkedQueue<ParallelProgressMonitorEvent>* queue)
    : concreteProgressMonitor(),
      sharedEventQueue(queue),
      stillProcessingEvents(true),
      totalElementsToProcess(0),
      currentProcessedElements(0)
{
}

void ParallelProgressMonitorConsumer::run()
{
    concreteProgressMonitor.begin();
    while ( stillProcessingEvents || !sharedEventQueue->isEmpty() ) {
        ParallelProgressMonitorEvent nextEvent(FINISH, 0);
        bool hasEvent = sharedEventQueue->poll(&nextEvent);

        if ( !hasEvent ) {
            usleep(50 * 1000);
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
