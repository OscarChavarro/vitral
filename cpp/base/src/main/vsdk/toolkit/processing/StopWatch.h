#ifndef __STOP_WATCH__
#define __STOP_WATCH__

#include <chrono>
class StopWatch {
private:
    bool running;
    std::chrono::steady_clock::time_point startTime;
    double elapsedSeconds;

public:
    StopWatch();
    void start();
    void stop();
    double getElapsedRealTime() const;
};

#endif
