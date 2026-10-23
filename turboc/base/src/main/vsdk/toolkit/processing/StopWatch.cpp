#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/System.h"
#include "vsdk/toolkit/processing/StopWatch.h"
StopWatch::StopWatch() : running(FALSE), startTime(0), elapsedSeconds(0.0)
{
}

void StopWatch::start()
{
    running = TRUE;
    startTime = System::nanoTime();
}

void StopWatch::stop()
{
    if ( !running ) {
        return;
    }
    long long end = System::nanoTime();
    elapsedSeconds = (double)(end - startTime) / 1.0e9;
    running = FALSE;
}

double StopWatch::getElapsedRealTime() const
{
    return elapsedSeconds;
}
