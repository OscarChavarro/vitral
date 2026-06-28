#include <cstdio>

#include "java/util/ArrayList.txx"
#include "model/CleanerConsumer.hpp"
#include <unistd.h>

CleanerConsumer::CleanerConsumer(MarkerEventBus* bus)
    : bus_(bus), running_(true) {}

java::Void CleanerConsumer::call() {
    while (running_) {
        if (bus_->totalSize() > 1000) {
            bus_->drainAll();
        }
        usleep(50000);
    }
    return java::Void();
}

void CleanerConsumer::stop() {
    running_ = false;
}
