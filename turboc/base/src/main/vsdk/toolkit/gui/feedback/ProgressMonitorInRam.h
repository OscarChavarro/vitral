#ifndef __PROGRESS_MONITOR_IN_RAM__
#define __PROGRESS_MONITOR_IN_RAM__

#include "vsdk/toolkit/gui/feedback/ProgressMonitor.h"
class ProgressMonitorInRam : public ProgressMonitor {
private:
    double currentPercent;

public:
    ProgressMonitorInRam();
    virtual ~ProgressMonitorInRam() {}

    virtual void begin();
    virtual void end();
    virtual void update(double minValue, double maxValue, double currentValue);
    virtual double getCurrentPercent();
};

#endif
