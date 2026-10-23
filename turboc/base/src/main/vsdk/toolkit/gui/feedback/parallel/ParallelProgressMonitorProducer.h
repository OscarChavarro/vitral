#ifndef __PARALLEL_PROGRESS_MONITOR_PRODUCER__
#define __PARALLEL_PROGRESS_MONITOR_PRODUCER__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/concurrent/ConcurrentLinkedQueue.h"
#include "java/util/concurrent/atomic/AtomicLong.h"
#include "vsdk/toolkit/gui/feedback/ProgressMonitor.h"
#include "vsdk/toolkit/gui/feedback/parallel/ParallelProgressMonitorEvent.h"
class ParallelProgressMonitorProducer : public ProgressMonitor {
private:
    ConcurrentLinkedQueue<ParallelProgressMonitorEvent>* sharedEventQueue;
    AtomicLong processedElements;
    volatile long long totalElements;

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    explicit ParallelProgressMonitorProducer(
        ConcurrentLinkedQueue<ParallelProgressMonitorEvent>* sharedEventQueue);
    virtual ~ParallelProgressMonitorProducer() {}

    void init(long long totalElementsToProcess);
    void finish();

    virtual void begin();
    virtual void end();
    virtual void update(double minimumValue, double maximumValue, double currentValue);
    virtual double getCurrentPercent();
};

#endif
