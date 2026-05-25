#include "vsdk/toolkit/processing/StopWatch.h"

StopWatch::StopWatch() : running_(false), elapsedSeconds_(0.0)
{
}

void StopWatch::start()
{
    running_ = true;
    start_ = std::chrono::steady_clock::now();
}

void StopWatch::stop()
{
    if ( !running_ ) {
        return;
    }
    std::chrono::steady_clock::time_point end = std::chrono::steady_clock::now();
    elapsedSeconds_ = std::chrono::duration_cast<std::chrono::duration<double> >(end - start_).count();
    running_ = false;
}

double StopWatch::getElapsedRealTime() const
{
    return elapsedSeconds_;
}
