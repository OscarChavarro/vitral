#include <cstdio>

#include "java/util/ArrayList.txx"
#include "model/CleanerConsumer.hpp"
#include <unistd.h>
CleanerConsumer::CleanerConsumer(MarkerEventBus* bus)
    : bus_(bus), running_(true) {}

java::Void CleanerConsumer::call() {
    std::printf("[cleaner] consumer started\n");
    while (running_) {
        if (bus_->totalSize() > 1000) {
            std::printf("[cleaner] draining queues (total size: %ld)\n", bus_->totalSize());
            bus_->drainAll();
        }
        usleep(50000);
    }
    std::printf("[cleaner] consumer stopped\n");
    return java::Void();
}

void CleanerConsumer::stop() {
    running_ = false;
}
