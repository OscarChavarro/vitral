#ifndef __VSDK_TOOLKIT_GUI_FEEDBACK_PROGRESSMONITORINRAM_H__
#define __VSDK_TOOLKIT_GUI_FEEDBACK_PROGRESSMONITORINRAM_H__

#include "vsdk/toolkit/gui/feedback/ProgressMonitor.h"
class ProgressMonitorInRam : public ProgressMonitor {
private:
    double currentPercent;

public:
    ProgressMonitorInRam();
    virtual ~ProgressMonitorInRam() override {}

    virtual void begin() override;
    virtual void end() override;
    virtual void update(double minValue, double maxValue, double currentValue) override;
    virtual double getCurrentPercent() override;
};

#endif
