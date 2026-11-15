#ifndef __PRTCTD_286_PRGRSS_MNTR__
#define __PRTCTD_286_PRGRSS_MNTR__

#include "kernel/Protected286Types.h"
#include "vsdk/toolkit/gui/feedback/ProgressMonitor.h"

class Protected286ProgressMonitor : public ProgressMonitor {
private:
    Protected286Word lastPercent;
    BOOLEAN active;

    void draw(Protected286Word percent, const char* status);

public:
    static const Class CLASS;

    Protected286ProgressMonitor();
    virtual ~Protected286ProgressMonitor() {}
    virtual void begin();
    virtual void end();
    virtual void update(double minValue, double maxValue, double currentValue);
    virtual double getCurrentPercent();
    virtual const Class* getClass() const;
};

#endif
