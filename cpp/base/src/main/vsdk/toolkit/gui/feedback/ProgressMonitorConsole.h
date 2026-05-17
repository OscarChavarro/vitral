#ifndef __VSDK_TOOLKIT_GUI_FEEDBACK_PROGRESSMONITORCONSOLE_H__
#define __VSDK_TOOLKIT_GUI_FEEDBACK_PROGRESSMONITORCONSOLE_H__

#include "ProgressMonitor.h"

#include <pthread.h>

class ProgressMonitorConsole : public ProgressMonitor {
private:
    pthread_mutex_t lock;
    double currentPercent;
    double jumpPercent;
    int lastPrintedLabel;

    bool testLabelLimit(int limit);

public:
    ProgressMonitorConsole();
    virtual ~ProgressMonitorConsole() override;

    virtual void begin() override;
    virtual void end() override;
    virtual void update(double minValue, double maxValue, double currentValue) override;
    virtual double getCurrentPercent() override;
};

#endif
