#ifndef __PARALLEL_PROGRESS_MONITOR_CONSUMER__
#define __PARALLEL_PROGRESS_MONITOR_CONSUMER__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/concurrent/ConcurrentLinkedQueue.h"
#include "vsdk/toolkit/gui/feedback/ProgressMonitorConsoleLongFormat.h"
#include "vsdk/toolkit/gui/feedback/parallel/ParallelProgressMonitorEvent.h"
class ParallelProgressMonitorConsumer {
private:
    ProgressMonitorConsoleLongFormat concreteProgressMonitor;
    ConcurrentLinkedQueue<ParallelProgressMonitorEvent>* sharedEventQueue;
    BOOLEAN stillProcessingEvents;
    long long totalElementsToProcess;
    long long currentProcessedElements;

public:
    explicit ParallelProgressMonitorConsumer(
        ConcurrentLinkedQueue<ParallelProgressMonitorEvent>* sharedEventQueue);
    virtual ~ParallelProgressMonitorConsumer() {}

    void run();
};

#endif
