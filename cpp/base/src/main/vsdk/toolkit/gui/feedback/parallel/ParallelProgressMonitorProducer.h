#ifndef __VSDK_TOOLKIT_GUI_FEEDBACK_PARALLEL_PARALLELPROGRESSMONITORPRODUCER_H__
#define __VSDK_TOOLKIT_GUI_FEEDBACK_PARALLEL_PARALLELPROGRESSMONITORPRODUCER_H__

#include "vsdk/toolkit/gui/feedback/parallel/ParallelProgressMonitorEvent.h"
#include "vsdk/toolkit/gui/feedback/ProgressMonitor.h"
#include "java/util/concurrent/ConcurrentLinkedQueue.h"
#include "java/util/concurrent/atomic/AtomicLong.h"

class ParallelProgressMonitorProducer : public ProgressMonitor {
private:
    java::ConcurrentLinkedQueue<ParallelProgressMonitorEvent>* sharedEventQueue;
    java::AtomicLong processedElements;
    volatile long long totalElements;

public:
    explicit ParallelProgressMonitorProducer(
        java::ConcurrentLinkedQueue<ParallelProgressMonitorEvent>* sharedEventQueue);
    virtual ~ParallelProgressMonitorProducer() override {}

    void init(long long totalElementsToProcess);
    void finish();

    virtual void begin() override;
    virtual void end() override;
    virtual void update(double minimumValue, double maximumValue, double currentValue) override;
    virtual double getCurrentPercent() override;
};

#endif
