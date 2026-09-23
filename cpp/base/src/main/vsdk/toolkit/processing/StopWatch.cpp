#include "vsdk/toolkit/processing/StopWatch.h"
StopWatch::StopWatch() : running(false), elapsedSeconds(0.0)
{
}

void StopWatch::start()
{
    running = true;
    startTime = std::chrono::steady_clock::now();
}

void StopWatch::stop()
{
    if ( !running ) {
        return;
    }
    std::chrono::steady_clock::time_point end = std::chrono::steady_clock::now();
    elapsedSeconds = std::chrono::duration_cast<std::chrono::duration<double> >(end - startTime).count();
    running = false;
}

double StopWatch::getElapsedRealTime() const
{
    return elapsedSeconds;
}
