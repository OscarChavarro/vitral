#ifndef __STOP_WATCH__
#define __STOP_WATCH__

#include <chrono>
class StopWatch {
private:
    bool running_;
    std::chrono::steady_clock::time_point start_;
    double elapsedSeconds_;

public:
    StopWatch();
    void start();
    void stop();
    double getElapsedRealTime() const;
};

#endif
