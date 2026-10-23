#ifndef __STOP_WATCH__
#define __STOP_WATCH__

#include "vsdk/toolkit/common/VSDK.h"

class StopWatch {
private:
    BOOLEAN running;
    long long startTime;
    double elapsedSeconds;

public:
    StopWatch();
    void start();
    void stop();
    double getElapsedRealTime() const;
};

#endif
