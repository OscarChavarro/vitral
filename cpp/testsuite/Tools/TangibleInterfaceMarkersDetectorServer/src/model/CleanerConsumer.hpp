#ifndef CLEANER_CONSUMER_HPP
#define CLEANER_CONSUMER_HPP

#include "java/util/concurrent/Callable.h"
#include "java/util/concurrent/Void.h"
#include "model/MarkerEventBus.hpp"

class CleanerConsumer : public java::Callable<java::Void> {
private:
    MarkerEventBus* bus_;
    volatile bool running_;

public:
    explicit CleanerConsumer(MarkerEventBus* bus);

    java::Void call() override;

    void stop();
};

#endif
