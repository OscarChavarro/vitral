#ifndef __MARKER_EVENT_BUS__
#define __MARKER_EVENT_BUS__

#include "java/util/ArrayList.h"
#include "java/util/concurrent/ConcurrentLinkedQueue.h"
#include "java/util/concurrent/atomic/AtomicLong.h"
#include "webservice/Protocol.hpp"

class MarkerEventBus {
private:
    java::ConcurrentLinkedQueue<java::ArrayList<MarkerGroupPose>> visualizationQueue;
    java::ConcurrentLinkedQueue<java::ArrayList<MarkerGroupPose>> networkQueue;
    java::AtomicLong totalSize;

public:
    MarkerEventBus() {}

    void publish(const java::ArrayList<MarkerGroupPose>& event) {
        visualizationQueue.add(event);
        networkQueue.add(event);
        totalSize.incrementAndGet();
        totalSize.incrementAndGet();
    }

    bool pollVisualization(java::ArrayList<MarkerGroupPose>* out) {
        if (visualizationQueue.poll(out)) {
            totalSize.set(totalSize.get() - 1);
            return true;
        }
        return false;
    }

    bool pollNetwork(java::ArrayList<MarkerGroupPose>* out) {
        if (networkQueue.poll(out)) {
            totalSize.set(totalSize.get() - 1);
            return true;
        }
        return false;
    }

    long getTotalSize() const { return totalSize.get(); }

    void drainAll() {
        java::ArrayList<MarkerGroupPose> dummy;
        while (visualizationQueue.poll(&dummy)) {}
        while (networkQueue.poll(&dummy)) {}
        totalSize.set(0);
    }
};

#endif
