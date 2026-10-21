#ifndef __PROGRESS_MONITOR_CONSOLE_LONG_FORMAT__
#define __PROGRESS_MONITOR_CONSOLE_LONG_FORMAT__

#include "vsdk/toolkit/gui/feedback/ProgressMonitor.h"
class ProgressMonitorConsoleLongFormat : public ProgressMonitor {
private:
    long updateCount;
    int charactersPrintedInLastLine;
    double currentPercent;

public:
    ProgressMonitorConsoleLongFormat();
    virtual ~ProgressMonitorConsoleLongFormat() {}

    virtual void begin();
    virtual void end();
    virtual void update(double minValue, double maxValue, double currentValue);
    virtual double getCurrentPercent();
};

#endif
