#ifndef __PRGRSS_MNTR_CNSL_LNG_FRMT__
#define __PRGRSS_MNTR_CNSL_LNG_FRMT__

#include "vsdk/toolkit/gui/feedback/ProgressMonitor.h"
class ProgressMonitorConsoleLongFormat : public ProgressMonitor {
private:
    long updateCount;
    int charactersPrintedInLastLine;
    double currentPercent;

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    ProgressMonitorConsoleLongFormat();
    virtual ~ProgressMonitorConsoleLongFormat() {}

    virtual void begin();
    virtual void end();
    virtual void update(double minValue, double maxValue, double currentValue);
    virtual double getCurrentPercent();
};

#endif
