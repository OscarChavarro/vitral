#ifndef __PROGRESS_MONITOR_CONSOLE__
#define __PROGRESS_MONITOR_CONSOLE__

#include <pthread.h>
#include "vsdk/toolkit/gui/feedback/ProgressMonitor.h"
class ProgressMonitorConsole : public ProgressMonitor {
private:
    pthread_mutex_t lock;
    double currentPercent;
    double jumpPercent;
    int lastPrintedLabel;

    bool testLabelLimit(int limit);

public:
    ProgressMonitorConsole();
    virtual ~ProgressMonitorConsole();

    virtual void begin();
    virtual void end();
    virtual void update(double minValue, double maxValue, double currentValue);
    virtual double getCurrentPercent();
};

#endif
