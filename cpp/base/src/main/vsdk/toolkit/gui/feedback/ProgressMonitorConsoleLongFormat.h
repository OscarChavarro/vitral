#ifndef __VSDK_TOOLKIT_GUI_FEEDBACK_PROGRESSMONITORCONSOLELONGFORMAT_H__
#define __VSDK_TOOLKIT_GUI_FEEDBACK_PROGRESSMONITORCONSOLELONGFORMAT_H__

#include "vsdk/toolkit/gui/feedback/ProgressMonitor.h"
class ProgressMonitorConsoleLongFormat : public ProgressMonitor {
private:
    long n;
    int charactersPrintedInLastLine;
    double currentPercent;

public:
    ProgressMonitorConsoleLongFormat();
    virtual ~ProgressMonitorConsoleLongFormat() override {}

    virtual void begin() override;
    virtual void end() override;
    virtual void update(double minValue, double maxValue, double currentValue) override;
    virtual double getCurrentPercent() override;
};

#endif
