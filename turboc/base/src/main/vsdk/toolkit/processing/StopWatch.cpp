#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/System.h"
#include "vsdk/toolkit/processing/StopWatch.h"
StopWatch::StopWatch() : running(FALSE), startTime(0), elapsedSeconds(0.0)
{
}

void StopWatch::start()
{
    running = TRUE;
#ifdef TURBO_C_286_ENABLED
    startTime = System::currentTimeMillis();
#else
    startTime = System::nanoTime();
#endif
}

void StopWatch::stop()
{
    if ( !running ) {
        return;
    }
#ifdef TURBO_C_286_ENABLED
    long long end = System::currentTimeMillis();
    elapsedSeconds = (double)(end - startTime) / 1000.0;
#else
    long long end = System::nanoTime();
    elapsedSeconds = (double)(end - startTime) / 1.0e9;
#endif
    running = FALSE;
}

double StopWatch::getElapsedRealTime() const
{
    return elapsedSeconds;
}
