#ifndef __CLEANER_CONSUMER__
#define __CLEANER_CONSUMER__

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
