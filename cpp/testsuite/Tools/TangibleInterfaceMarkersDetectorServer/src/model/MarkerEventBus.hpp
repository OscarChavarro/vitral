#ifndef __MARKER_EVENT_BUS__
#define __MARKER_EVENT_BUS__

#include "java/util/ArrayList.h"
#include "java/util/concurrent/ConcurrentLinkedQueue.h"
#include "java/util/concurrent/atomic/AtomicLong.h"
#include "webservice/Protocol.hpp"

class MarkerEventBus {
private:
    java::ConcurrentLinkedQueue<java::ArrayList<MarkerGroupPose>> visualizationQueue_;
    java::ConcurrentLinkedQueue<java::ArrayList<MarkerGroupPose>> networkQueue_;
    java::AtomicLong totalSize_;

public:
    MarkerEventBus() {}

    void publish(const java::ArrayList<MarkerGroupPose>& event) {
        visualizationQueue_.add(event);
        networkQueue_.add(event);
        totalSize_.incrementAndGet();
        totalSize_.incrementAndGet();
    }

    bool pollVisualization(java::ArrayList<MarkerGroupPose>* out) {
        if (visualizationQueue_.poll(out)) {
            totalSize_.set(totalSize_.get() - 1);
            return true;
        }
        return false;
    }

    bool pollNetwork(java::ArrayList<MarkerGroupPose>* out) {
        if (networkQueue_.poll(out)) {
            totalSize_.set(totalSize_.get() - 1);
            return true;
        }
        return false;
    }

    long totalSize() const { return totalSize_.get(); }

    void drainAll() {
        java::ArrayList<MarkerGroupPose> dummy;
        while (visualizationQueue_.poll(&dummy)) {}
        while (networkQueue_.poll(&dummy)) {}
        totalSize_.set(0);
    }
};

#endif
