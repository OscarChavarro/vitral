#ifndef __VSDK_TOOLKIT_GUI_FEEDBACK_PARALLEL_PARALLELPROGRESSMONITORCONSUMER_H__
#define __VSDK_TOOLKIT_GUI_FEEDBACK_PARALLEL_PARALLELPROGRESSMONITORCONSUMER_H__

#include "ParallelProgressMonitorEvent.h"
#include "vsdk/toolkit/gui/feedback/ProgressMonitorConsoleLongFormat.h"
#include "java/util/concurrent/ConcurrentLinkedQueue.h"

class ParallelProgressMonitorConsumer {
private:
    ProgressMonitorConsoleLongFormat concreteProgressMonitor;
    java::ConcurrentLinkedQueue<ParallelProgressMonitorEvent>* sharedEventQueue;
    bool stillProcessingEvents;
    long long totalElementsToProcess;
    long long currentProcessedElements;

public:
    explicit ParallelProgressMonitorConsumer(
        java::ConcurrentLinkedQueue<ParallelProgressMonitorEvent>* sharedEventQueue);
    virtual ~ParallelProgressMonitorConsumer() {}

    void run();
};

#endif
