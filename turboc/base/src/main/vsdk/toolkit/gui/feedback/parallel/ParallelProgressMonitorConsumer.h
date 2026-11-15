#ifndef __PRLLL_PRGRSS_MNTR_CNSMR__
#define __PRLLL_PRGRSS_MNTR_CNSMR__

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
