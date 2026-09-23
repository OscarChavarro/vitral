#include <cstdio>

#include "java/util/ArrayList.txx"
#include "model/CleanerConsumer.hpp"
#include <unistd.h>

CleanerConsumer::CleanerConsumer(MarkerEventBus* bus)
    : bus(bus), running(true) {}

java::Void CleanerConsumer::call() {
    while (running) {
        if (bus->getTotalSize() > 1000) {
            bus->drainAll();
        }
        usleep(50000);
    }
    return java::Void();
}

void CleanerConsumer::stop() {
    running = false;
}
